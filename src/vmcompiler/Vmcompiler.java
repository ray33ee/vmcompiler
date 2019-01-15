/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vmcompiler;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Will
 */
public class Vmcompiler {
    
    /** Regex to match label declarations */
    static final Pattern LABEL_REG = Pattern.compile("([a-zA-Z_])*:"); //Matches label identifiers
    
    /** Regex to match preprocessor directives */
    static final Pattern DIRECTIVE_REG = Pattern.compile("\\s*(?<=[.])([a-zA-Z_])+");
    
    /** Regex to match preprocessor directive arguments */
    static final Pattern DIRECTIVE_ARG_REG = Pattern.compile("(?<=\\s)([0-9a-zA-Z.e+-_$])*");
    
    /** Regex to match instructions */
    static final Pattern INSCTUCTION_REG = Pattern.compile("([a-z]?[A-Z]?)*\\b"); //Matches instructions
    
    /** Regex to match literal integers */
    static final Pattern LITERAL_INT_REG = Pattern.compile("(?<!\\S)\\d++(?!\\S)"); //Matches literal integers
    
    /** Regex to match literal floating point numbers */
    static final Pattern LITERAL_FLOAT_REG = Pattern.compile("[-+]?\\b[0-9]*\\.[0-9]+(?:[eE][-+]?[0-9]+)?\\b"); // Matches literal floating point numbers
    
    /** Regex to match variables */
    static final Pattern VARIABLE_REG = Pattern.compile("[$]\\d++"); //Matches variables
    
    /** Regex to match type identifiers in variable declarations */
    static final Pattern IDENTIFIER_REG = Pattern.compile("([a-zA-Z_])+(?=\\s)"); // Matches variable identifiers
    
    /** Regex to match the argument delimiter character, ',' */
    static final Pattern ARGS_DELIM_REG = Pattern.compile(",");
    
    /** Map function signatures to corresponding bytecodes */
    static final HashMap<String, Integer> BYTECODE_TABLE;
    
    /** Map long type string (i.e. float) to short type string (i.e. f) */
    static final HashMap<String, String> TYPE_TABLE;
    
    /** The total number of available registers */
    static final int REGISTER_COUNT = 30;
    
    /** Simple lookup table containing the types of all declared variables */
    static String[] typeTable = new String[REGISTER_COUNT];
    
    static
    {
        BYTECODE_TABLE = new HashMap<>(90);
        TYPE_TABLE = new HashMap<>(10);
        
        TYPE_TABLE.put("int", "i");
        TYPE_TABLE.put("float", "f");
        
        try 
        {
            InputStream stream = vmcompiler.Vmcompiler.class.getResourceAsStream("bytecodes.xml");

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("Instruction");

            for (int temp = 0; temp < nList.getLength(); temp++) 
            {
                Node nNode = nList.item(temp);
                
                if (nNode.getNodeType() == Node.ELEMENT_NODE) 
                {
                   Element eElement = (Element) nNode;
                   
                   String command = eElement.getElementsByTagName("Signature").item(0).getTextContent();
                   Integer bytecode = Integer.parseInt(eElement.getElementsByTagName("Bytecode").item(0).getTextContent(), 16);
                   
                   BYTECODE_TABLE.put(command, bytecode);
                }
            }
            
            nList = doc.getElementsByTagName("Type");

            for (int temp = 0; temp < nList.getLength(); temp++) 
            {
                Node nNode = nList.item(temp);
                
                if (nNode.getNodeType() == Node.ELEMENT_NODE) 
                {
                   Element eElement = (Element) nNode;
                   
                   String type = eElement.getElementsByTagName("Signature").item(0).getTextContent();
                   String abvr = eElement.getElementsByTagName("Short").item(0).getTextContent();
                   
                   TYPE_TABLE.put(type, abvr);
                }
            }
            
        } 
        catch (IOException | NumberFormatException | ParserConfigurationException | DOMException | SAXException e) 
        {
            System.out.println("ERROR: Problem while parsing bytecodes.xml " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Tests whether line is a variable declaration
     * @param line a line of source code
     * @return true if the line is a variable declaration
     */
    public static boolean isIdentifier(String line)
    {
        Set<String> values = TYPE_TABLE.keySet();
        
        for (String s : values)
            if (line.contains(s))
                return true;
        
        return false;
    }
    
    /**
     * Tests whether line is a label declaration
     * @param line a line of source code
     * @return true if the line is a label declaration
     */
    public static boolean isLabel(String line)
    {
        return LABEL_REG.matcher(line).find();
    }
    
    /**
     * Tests whether the line is an instruction
     * @param line a line of source code
     * @return true if the line is a command
     */
    public static boolean isInstruction(String line)
    {
        return INSCTUCTION_REG.matcher(line).find();
    }
    
    /**
     * Tests whether the line is a preprocessor directive
     * @param line
     * @return 
     */
    public static boolean isDirective(String line)
    {
        return DIRECTIVE_REG.matcher(line).find();
    }
    
    /**
     * Parses the line and returns the type identifier (int, float, etc.)
     * NOTE: This function MUST be used on a variable declaration line. Test with 
     * isIdentifier first.
     * @param line a line of source code
     * @return the identifier as a string
     */
    public static String getIdentifier(String line)
    {
        Matcher m = IDENTIFIER_REG.matcher(line);
        m.find();
        return m.group();
    }
    
    /**
     * Parses the line and returns the label string
     * NOTE: This function MUST be used on a label declaration line. Test with 
     * isLabel first.
     * @param line a line of source code
     * @return the label string
     */
    public static String getLabel(String line)
    {
        Matcher m = LABEL_REG.matcher(line);
        m.find();
        String label = m.group();
        return label.substring(0, label.length()-1);
    }
    
    /**
     * Parses the line and returns the instruction string
     * NOTE: This function MUST be used on an instruction line. Test with 
     * isInstruction first.
     * @param line a line of source code
     * @return the instruction string
     */
    public static String getInstruction(String line)
    {
        Matcher m = INSCTUCTION_REG.matcher(line);
        m.find();
        return m.group();
    }
    
    /**
     * Parses the line and returns the directive string
     * NOTE: This function MUST be used on a directive line. Test with 
     * isDiurective first.
     * @param line a line of source code
     * @return the direective string
     */
    public static String getDirective(String line)
    {
        Matcher m = DIRECTIVE_REG.matcher(line);
        m.find();
        return m.group();
    }
    
    /**
     * Parses the line and returns the string of arguments
     * NOTE: This function MUST be used on an instruction line. Test with 
     * isInstruction first.
     * @param command the string command for this instruction
     * @param line a line of source code
     * @return a list of arguments
     */
    public static String getArgs(String command, String line)
    {
        String argstring = line.replaceAll("\\s", ""); //Remove whitespace
        
        if (command.equals(argstring))
            return "";
        
        argstring = argstring.split(command + "\\s*")[1]; //Get string after command (instruction/identifier)
        
        return argstring;
    }
    
    /**
     * Splits the string of arguments into a list of arguments
     * @param argstring the argument string
     * @param definitionTable the table used to replace preprocessor .defines
     * @return list of arguments
     */
    public static String[] splitArgs(String argstring, HashMap<String, String> definitionTable)
    {
        if (argstring.isEmpty())
            return new String[0];
        
        String[] args = ARGS_DELIM_REG.split(argstring);
        
        for (int i = 0; i < args.length; ++i)
        {
            if (definitionTable.containsKey(args[i]))
            {
                args[i] = definitionTable.get(args[i]);
            }
        }
        
        return args;
    }
    
    /**
     * Get the index of the variable, i.e. "$3" would return 3.
     * @param variable the variable string
     * @return the index of the variable or null if the conversion was unsuccessful
     */
    public static Integer getVarIndex(String variable) throws NumberFormatException
    {
        Integer number;
        
        try
        {
            number = Integer.parseInt(variable.substring(1));
        }
        catch (NumberFormatException e)
        {
            throw new NumberFormatException();
        }
        
        return number;
    }
    
    /**
     * Gets the type of the argument, variable, literal float or literal integer
     * @param arg the argument to test
     * @return a single character, "v", "f" or "i".
     */
    public static String getType(String arg)
    {
        if (arg.contains("$"))
            return "v";
        else if (arg.contains("."))
            return "f";
        else
            return "i";
    }
    
    /**
     * Get the single char from the string representation of the type.
     * @param identifier the string identifier
     * @return a single char identifier
     */
    public static String getCharIdentifier(String identifier)
    {
        return TYPE_TABLE.get(identifier);
    }
    
    /**
     * Get the argtag of the argument, used in the function signature. For example,
     * The argument $1 (where $1 is a variable containing an integer) would have the 
     * argtag 'vi'.
     * @param arg the argument to convert
     * @return the argtag used in the signature
     */
    public static String getArgTag(String arg) throws NumberFormatException, IllegalArgumentException
    {
        String type = getType(arg);
        
        if (type.equals("v"))
        {
            String typ = typeTable[getVarIndex(arg)];
            
            if (typ == null) //if typ is null, there is no entry in the table for the variable, which means it wasn't declared
                throw new IllegalArgumentException();
            
            return "_v" + getCharIdentifier(typ);
        }
        else
            return "_l" + type;
    }
    
    /**
     * Determines whether an instruction uses an address, i.e. a label. If it does,
     * on the second parse the label string is replaced with an address to the label location
     * @param bytecode the instruction to test
     * @return true if the instruction is a jump
     */
    public static boolean usesAddress(int bytecode)
    {
        return bytecode >= 0x0100 && bytecode < 0x0105;
    }
    
    //It was around here I realised why I hate java...
    /**
     * Perform 2's complement on negative values to convert from signed to unsigned
     * @param val
     * @return 
     */
    public static int unsign(byte val)
    {
        if (val < 0) //2's complement if needed
            val += 256;
        return val & 0xff; //Shave off overflow bit
    }
    
    /**
     * Convert float to int. This is done by converting a float to an
     * array of bytes, then combines them into an int. 
     * Equivalent to 'return *(int*)&number' in C.
     * @param number
     * @return 
     */
    public static int floatToInt(float number)
    {
        byte[] arr = ByteBuffer.allocate(4).putFloat(number).array();
        return unsign(arr[3]) + unsign(arr[2]) * 0x100 + unsign(arr[1]) * 0x10000 + unsign(arr[0]) * 0x1000000;
    }
    
    /**
     * Converts the list of bytecodes into a formatted hex string.
     * @param binary list of bytecodes
     * @return string representation of final code
     */
    public static String display(ArrayList<Integer> binary)
    {
        String format = "0x%08X";
        String ans = "[";
        
        for (int i = 0; i < binary.size() - 1; ++i)
            ans += String.format(format, binary.get(i)) + ", ";
        
        ans += String.format(format, binary.get(binary.size() - 1)) + "]";
        
        return ans;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        BufferedReader buff;
        FileReader reader;
        
        String line;
        
        if (args.length == 0)
        {
            System.out.println("ERROR: No command line argument supplied.");
            return;
        }
        
        try
        {
            reader = new FileReader(args[0]);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("ERROR: Could not find the specified file " + e.getMessage());
            return;
        }
        
        buff = new BufferedReader(reader);
        
        HashMap<String, Integer> addressTable = new HashMap<>(20); //Mapping label strings to corresponding address
        ArrayList<Jumps> locationTable = new ArrayList<>(40); //Maps the location of the jump addresses to the string, so they can be found and replaced on the second parse
        
        ArrayList<Integer> binary = new ArrayList(); //List of opcodes
        
        HashMap<String, String> definitionTable = new HashMap<>(); //Maps preprocessor definitions to values
        Set<String> definitionList = definitionTable.keySet(); //Set of preprocessor definitions 
        
        int wordCount = 0; //Cumulative total number of words used by opcodes, used when addressing labels
        
        System.out.println("First parse...");
        
        while (true)
        {
            line = buff.readLine();
            
            if (line == null)
                break;
            
            if (line.contains("#")) //Ignore comments
                line = line.substring(0, line.indexOf("#"));
            
            if (isDirective(line))
            {
                String directive = getDirective(line);
                
                if (directive.equals("define"))
                {
                    Matcher m = DIRECTIVE_ARG_REG.matcher(line);
                    
                    //Get first argument, op1
                    if (!m.find())
                    {
                        System.out.println("ERROR: Invalid preprocessor arguments in line '" + line + "'.");
                        return;
                    }
                    
                    String op1 = m.group();
                    
                    //Get second argument, op2
                    if (!m.find())
                    {
                        System.out.println("ERROR: Invalid preprocessor arguments in line '" + line + "'.");
                        return;
                    }
                    
                    String op2 = m.group();
                    
                    //Add entry to table. Any ocurence of op1 within command arguments will be replaced with op2
                    definitionTable.put(op1, op2);
                    
                    System.out.println("    Directive:   " + directive + " " + op1 + " " + op2);
                }
                else
                {
                    System.out.println("ERROR: Unknown preprocessor directive " + directive + " on line '" + line + "'.");
                    return;
                }
                
                
            }
            else if (isIdentifier(line))
            {
                String identifier = getIdentifier(line);
                
                
                
                if (!TYPE_TABLE.containsKey(identifier))
                {
                    System.out.println("ERROR: Unrecognised type '" + identifier + "' in line '" + line + "'.");
                    return;
                }
                
                //Get variable number
                String[] arguments = splitArgs(getArgs(identifier, line), definitionTable);
                
                if (arguments.length != 1) //If the user has displayed too many args 
                {
                    System.err.println("ERROR: Too many/few arguments in declaration. Declaration takes exactly one argument. Error in line '" + line + "'.");
                    return;
                }
                
                String variable = arguments[0];
                
                /*for (String s : definitionList)
                    if (variable.contains(s))
                        variable = variable.replace(s, definitionTable.get(s));*/
                
                if (!getType(variable).equals("v")) //If argument supplied is not a variable
                {
                    System.err.println("ERROR: Argument must be a variable in line '" + line + "'.");
                    return;
                }
                
                int index;
                
                try
                {
                    index = getVarIndex(variable);
                }
                catch (NumberFormatException e)
                {
                    System.out.println("ERROR: " + variable + " is not a valid number, in line '" + line + "'.");
                    return;
                }
                
                if (typeTable[index] != null)
                {
                    System.out.println("ERROR: $" + index + " has already been defined as " + typeTable[index]);
                    return;
                }
                
                System.out.println("    Declaration: " + identifier + " at register " + index);
                
                typeTable[index] = identifier;
            } 
            else if (isLabel(line))
            {
                String label = getLabel(line);
                
                addressTable.put(label, wordCount);
                
                System.out.println("    Label:       " + getLabel(line));
            } 
            else if (isInstruction(line))
            {
                String instr = getInstruction(line);
                String argumentString = getArgs(instr, line);
                
                String[] arguments = splitArgs(argumentString, definitionTable);
                
                String signature = instr; //Add the instruction name to the signature
                
                for (int i = 0; i < arguments.length; ++i) //Complete the signature by populating with argument tags
                {
                    try
                    {
                        String argtag;
                        
                        try
                        {
                            argtag = getArgTag(arguments[i]);
                        }
                        catch (IllegalArgumentException e)
                        {
                            System.out.println("ERROR: No declaration for variable '" + arguments[i] + "'.");
                            return;
                        }
                        
                        signature += argtag;
                    }
                    catch (NumberFormatException e)
                    {
                        System.out.println("ERROR: " + arguments[i] + " is not a valid number, in line '" + line + "'.");
                        return;
                    }
                }
                
                if (!BYTECODE_TABLE.containsKey(signature))
                {
                    System.out.println("ERROR: Unknown command signature (" + signature + ") in line '" + line + "'.");
                    return;
                }
                
                int bytecode = BYTECODE_TABLE.get(signature);
                
                binary.add(bytecode);
                
                if (usesAddress(bytecode)) //If the instruction is a jump, add a placeholder argument which will be filled on the second parse
                {
                    binary.add(0xFFFFFFFF);   

                    locationTable.add(new Jumps(wordCount + 1, arguments[0]));
                }
                else
                {
                    for (int i = 0; i < arguments.length; ++i)
                    {
                        String type = getType(arguments[i]);

                        switch (type) {
                            case "v":
                                binary.add(getVarIndex(arguments[i]));
                                break;
                            case "i":
                                {Integer number;
                                try
                                {
                                    number = Integer.parseInt(arguments[i]);
                                }
                                catch (NumberFormatException e)
                                {
                                    System.out.println("ERROR: '" + arguments[i] + "' is not a valid number in line '" + line + "'.");
                                    return;
                                }
                                binary.add(number);
                                break;}
                            case "f":
                                {Float number;
                                try
                                {
                                    number = Float.parseFloat(arguments[i]);
                                }
                                catch (NumberFormatException e)
                                {
                                    System.out.println("ERROR: '" + arguments[i] + "' is not a valid number in line '" + line + "'.");
                                    return;
                                }
                                binary.add(floatToInt(number));
                                break;}
                            default:
                                break;
                        }
                    }
                }
                wordCount += (arguments.length + 1);
                System.out.println("    Command:     " + signature + ", bytecode: " + String.format("0x%x", bytecode) + ", Size: " + (arguments.length + 1));
                
            }
            
        }
        
        reader.close();
        
        System.out.println("First parse complete, replacing jump labels with addresses...");
        
        System.out.println("    Location table: " + locationTable);
        System.out.println("    Label table: " + addressTable);
        
        //Through all of locationTable and fill jump addresses
        for (int i = 0; i < locationTable.size(); ++i)
        {
            String label = locationTable.get(i)._label;
            if (!addressTable.containsKey(label))
            {
                System.out.println("ERROR: Cannot find label " + label + ".");
                return;
            }
            binary.set(locationTable.get(i)._position, addressTable.get(label));
        }
        
        System.out.println("Second parse complete.");
        
        OutputStream outStream = new FileOutputStream(".\\code.bin");
        
        for (Integer i : binary)
            outStream.write(ByteBuffer.allocate(4).putInt(i).array());
        
        outStream.close();
        
        System.out.println("Bytecode output: " + display(binary));
            
    }
    
}
