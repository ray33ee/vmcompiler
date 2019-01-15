/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vmcompliler;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Will
 */
public class Vmcompliler {

    /** Path to source file. Replace me with command line args later. */
    static final String source = "E:\\Software Projects\\Java\\vmcompliler\\code.v";
    
    /** Regex to match label declarations */
    static final Pattern LABEL_REG = Pattern.compile("([a-zA-Z_$])*:"); //Matches label identifiers
    
    /** Regex to match preprocessor directives */
    static final Pattern DIRECTIVE_REG = Pattern.compile("\\s*(?<=[.])([a-zA-Z_$])*");
    
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
    static final Pattern IDENTIFIER_REG = Pattern.compile("int|float|vec|ptr"); // Matches variable identifiers
    
    /** Regex to match the argument delimiter character, ',' */
    static final Pattern ARGS_DELIM_REG = Pattern.compile(",");
    
    /** Map function signatures to corresponding bytecodes */
    static final HashMap<String, Integer> BYTECODE_TABLE;
    
    /** The total number of available registers */
    static final int REGISTER_COUNT = 30;
    
    /** Simple lookup table containing the types of all declared variables */
    static String[] typeTable = new String[REGISTER_COUNT];
    
    static
    {
        BYTECODE_TABLE = new HashMap<>(90);
        
        /* Zero argument instructions */
        BYTECODE_TABLE.put("nop", 0x0000);
        BYTECODE_TABLE.put("halt", 0x0001); 
        BYTECODE_TABLE.put("ret", 0x0002);
        
        /* Single argument instruction */
        BYTECODE_TABLE.put("jmp_li", 0x0100);
        BYTECODE_TABLE.put("je_li", 0x0101);
        BYTECODE_TABLE.put("jne_li", 0x0102);
        BYTECODE_TABLE.put("jl_li", 0x0103);
        BYTECODE_TABLE.put("jle_li", 0x0104);
        
        BYTECODE_TABLE.put("external_li", 0x0105);
        BYTECODE_TABLE.put("elapsed_vi", 0x0106);
        
        BYTECODE_TABLE.put("dec_vi", 0x0107);
        BYTECODE_TABLE.put("inc_vi", 0x0108);
       
        BYTECODE_TABLE.put("random_vi", 0x0109);
        BYTECODE_TABLE.put("random_vf", 0x0109);
        BYTECODE_TABLE.put("random_vv", 0x0109);
        
        //NOTE: all three peeks should map to the same instruction, since type safety is not forced on peek function.
        //Any signatures with the * wildcard will have multiple signatures map to the same instruction. FUnctions
        //Marked with the * comment must share the same bytecode
        BYTECODE_TABLE.put("peek_vi", 0x010A); //*
        BYTECODE_TABLE.put("peek_vf", 0x010A);
        BYTECODE_TABLE.put("peek_vv", 0x010A);
        
        BYTECODE_TABLE.put("push_vi", 0x010B); //*
        BYTECODE_TABLE.put("push_vf", 0x010B);
        BYTECODE_TABLE.put("push_vv", 0x010B);
        
        BYTECODE_TABLE.put("push_li", 0x010B); //*
        BYTECODE_TABLE.put("push_lf", 0x010B);
        
        BYTECODE_TABLE.put("pop_vi", 0x010E); //*
        BYTECODE_TABLE.put("pop_vf", 0x010E);
        BYTECODE_TABLE.put("pop_vv", 0x010E);
        
        BYTECODE_TABLE.put("start_vi", 0x0);
        
        /* Dual argument math instructions */
        BYTECODE_TABLE.put("sin_vf_vf", 0x0200);
        BYTECODE_TABLE.put("cos_vf_vf", 0x0201);
        BYTECODE_TABLE.put("tan_vf_vf", 0x0202);
        
        BYTECODE_TABLE.put("abs_vi_vi", 0x0203);
        BYTECODE_TABLE.put("abs_vf_vf", 0x0204);
        
        BYTECODE_TABLE.put("norm_vi_vi", 0x0205);
        BYTECODE_TABLE.put("norm_vf_vf", 0x0206);
        
        BYTECODE_TABLE.put("ln_vf_vf", 0x0207);
        BYTECODE_TABLE.put("exp_vf_vf", 0x0208);
        BYTECODE_TABLE.put("floor_vf_vf", 0x0209);
        BYTECODE_TABLE.put("ceil_vf_vf", 0x020A);
        BYTECODE_TABLE.put("sqrt_vf_vf", 0x020B);
        BYTECODE_TABLE.put("neg_vf_vf", 0x020C);
        
        /* Dual argument non-math instructions */
        BYTECODE_TABLE.put("store_vi_li", 0x0240);
        BYTECODE_TABLE.put("store_vi_lf", 0x0241);
        
        BYTECODE_TABLE.put("store_vi_vi", 0x0242);
        BYTECODE_TABLE.put("store_vi_vf", 0x0242);
        BYTECODE_TABLE.put("store_vf_vi", 0x0242);
        BYTECODE_TABLE.put("store_vf_vf", 0x0242);
        BYTECODE_TABLE.put("store_vi_ii", 0x0242);
        BYTECODE_TABLE.put("store_vf_if", 0x0242);
        
        BYTECODE_TABLE.put("store_v*_p", 0x0242); //Need to figure out if i really want/need pointers...
        
        BYTECODE_TABLE.put("test_vv_vv", 0x0);
        
        BYTECODE_TABLE.put("cmp_vi_vi", 0x0243);
        BYTECODE_TABLE.put("cmp_vi_vf", 0x0243);
        BYTECODE_TABLE.put("cmp_vf_vi", 0x0243);
        BYTECODE_TABLE.put("cmp_vf_vf", 0x0243);
        
        BYTECODE_TABLE.put("cmp_vi_li", 0x0244);
        BYTECODE_TABLE.put("cmp_vf_li", 0x0244);
        BYTECODE_TABLE.put("cmp_vi_lf", 0x0245);
        BYTECODE_TABLE.put("cmp_vf_lf", 0x0245);
        
        BYTECODE_TABLE.put("stop_vi_vi", 0x0);
       
        
        /* Triple argument math instructions */
        BYTECODE_TABLE.put("add_vi_vi_vi", 0x0300); //vari <-- vari + vari
        BYTECODE_TABLE.put("add_vf_vf_vf", 0x0300); //vari <-- vari + varf
        BYTECODE_TABLE.put("add_vv_vv_vv", 0x0300); //varv <-- varv + varv
        
        BYTECODE_TABLE.put("add_vi_vi_li", 0x0300);
        BYTECODE_TABLE.put("add_vf_vf_lf", 0x0300);
        
        BYTECODE_TABLE.put("sub_vi_vi_vi", 0x0300);
        BYTECODE_TABLE.put("sub_vf_vf_vf", 0x0300);
        BYTECODE_TABLE.put("sub_vv_vv_vv", 0x0300);
        
        BYTECODE_TABLE.put("sub_vi_vi_li", 0x0300);
        BYTECODE_TABLE.put("sub_vf_vf_lf", 0x0300);
        
        BYTECODE_TABLE.put("sub_vi_li_vi", 0x0300);
        BYTECODE_TABLE.put("sub_vf_lf_vf", 0x0300);     
        
        BYTECODE_TABLE.put("mul_vi_vi_vi", 0x0300);
        BYTECODE_TABLE.put("mul_vf_vf_vf", 0x0300);
        
        BYTECODE_TABLE.put("mul_vv_vv_vf", 0x0300);
        BYTECODE_TABLE.put("mul_vv_vv_lf", 0x0300);
        
        BYTECODE_TABLE.put("mul_vi_vi_li", 0x0300);
        BYTECODE_TABLE.put("mul_vf_vf_lf", 0x0300);
        
        BYTECODE_TABLE.put("div_vi_vi_vi", 0x030B);
        BYTECODE_TABLE.put("div_vi_vi_li", 0x030C);
        BYTECODE_TABLE.put("div_vi_li_vi", 0x030C);
        
        BYTECODE_TABLE.put("div_vf_vf_vf", 0x030D);
        BYTECODE_TABLE.put("div_vf_vf_lf", 0x030E);
        BYTECODE_TABLE.put("div_vf_lf_vf", 0x030E);
        
        BYTECODE_TABLE.put("div_vv_vv_vf", 0x030D);
        BYTECODE_TABLE.put("div_vv_vv_lf", 0x030E);
        
        BYTECODE_TABLE.put("pow_vf_vf_vf", 0x0310);
        BYTECODE_TABLE.put("pow_vf_vf_lf", 0x0312);
        BYTECODE_TABLE.put("pow_vf_lf_vf", 0x0312);
        
        /* Triple argument non-math functions */
        BYTECODE_TABLE.put("store_vv_lf_lf", 0x0340);
        
        
    }
    
    /**
     * Tests whether line is a variable declaration
     * @param line a line of source code
     * @return true if the line is a variable declaration
     */
    public static boolean isIdentifier(String line)
    {
        return IDENTIFIER_REG.matcher(line).find();
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
        String s = line.replaceAll("\\s", ""); //Remove whitespace
        
        if (command.equals(s))
            return "";
        
        s = s.split(command + "\\s*")[1]; //Get string after command (instruction/identifier)
        
        return s;
    }
    
    /**
     * Splits the string of arguments into a list of arguments
     * @param argstring the argument string
     * @return list of arguments
     */
    public static String[] splitArgs(String argstring)
    {
        if (argstring.isEmpty())
            return new String[0];
        
        return ARGS_DELIM_REG.split(argstring);
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
        switch (identifier) {
            case "int":
                return "i";
            case "float":
                return "f";
            case "vector":
                return "v";
            default:
                return "|";
        }
    }
    
    /**
     * Get the argtag of the argument, used in the function signature. For example,
     * The argument $1 (where $1 is a variable containing an integer) would have the 
     * argtag 'vi'.
     * @param arg the argument to convert
     * @return the argtag used in the signature
     */
    public static String getArgTag(String arg) throws NumberFormatException
    {
        String type = getType(arg);
                    
        if (type.equals("v"))
            return "_v" + getCharIdentifier(typeTable[getVarIndex(arg)]);
        else
            return "_l" + type;
    }
    
    /**
     * Determines whether an instruction is a jump or not
     * @param bytecode the instruction to test
     * @return true if the instruction is a jump
     */
    public static boolean isJump(int bytecode)
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
        
        try
        {
            reader = new FileReader(source);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Could not find the specified file " + e.getMessage());
            return;
        }
        
        buff = new BufferedReader(reader);
        
        HashMap<String, Integer> addressTable = new HashMap<>(20); //Mapping label strings to corresponding address
        ArrayList<Jumps> locationTable = new ArrayList<>(40); //Maps the location of the jump addresses to the string, so they can be found and replaced on the second parse
        
        ArrayList<Integer> binary = new ArrayList(); //List of opcodes
        
        HashMap<String, String> definitionTable = new HashMap<>(); //Maps preprocessor definitions to values
        Set<String> definitionList = definitionTable.keySet(); //Set of preprocessor definitions 
        
        int wordCount = 0; //Cumulative total number of words used by opcodes, used when addressing labels
        
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
                    
                    System.out.println("Directive: " + directive + " " + op1 + " " + op2);
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
                
                //Get variable number
                String[] arguments = splitArgs(getArgs(identifier, line));
                
                if (arguments.length != 1) //If the user has displayed too many args 
                {
                    System.err.println("ERROR: Too many/few arguments in declaration. Declaration takes exactly one argument.");
                    return;
                }
                
                String variable = arguments[0];
                
                if (!getType(variable).equals("v")) //If argument supplied is not a variable
                {
                    System.err.println("ERROR: Argument must be a variable.");
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
                
                System.out.println("Declaration: " + identifier + " at index " + index);
                
                if (typeTable[index] != null)
                {
                    System.out.println("ERROR: $" + index + " has already been defined as " + typeTable[index]);
                    return;
                }
                
                typeTable[index] = identifier;
            } 
            else if (isLabel(line))
            {
                String label = getLabel(line);
                
                addressTable.put(label, wordCount);
                
                System.out.println("Label:       " + getLabel(line));
            } 
            else if (isInstruction(line))
            {
                String instr = getInstruction(line);
                String argumentString = getArgs(instr, line);
                
                
                for (String s : definitionList)
                {
                    if (argumentString.contains(s))
                    {
                        argumentString = argumentString.replace(s, definitionTable.get(s));
                    }

                }
                
                String[] arguments = splitArgs(argumentString);
                
                String signature = instr; //Add the instruction name to the signature
                
                for (int i = 0; i < arguments.length; ++i) //Complete the signature by populating with argument tags
                {
                    try
                    {
                        signature += getArgTag(arguments[i]);
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
                
                if (isJump(bytecode)) //If the instruction is a jump, add a placeholder argument which will be filled on the second parse
                {
                    binary.add(0x0);   

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
                System.out.println("Command:     " + signature + ", bytecode: " + String.format("0x%x", bytecode) + ", Size: " + (arguments.length + 1));
                
            }
            
        }
        
        reader.close();
        
        System.out.println(locationTable);
        
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
        
        System.out.println(display(binary));
            
    }
    
}
