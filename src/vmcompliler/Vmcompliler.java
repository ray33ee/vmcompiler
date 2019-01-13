/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vmcompliler;

import java.io.*;
import java.util.HashMap;
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
    static final Pattern LABEL_REG = Pattern.compile("(?<=:)([a-z]?[A-Z]?)*"); //Matches label identifiers
    
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
    static final HashMap<String, Integer> table;
    
    static
    {
        
        table = new HashMap<>(80);
        
        /* Zero argument instructions */
        table.put("nop", 0x0000);
        table.put("halt", 0x0001); 
        table.put("ret", 0x0002);
        
        /* Single argument instruction */
        table.put("jmp_li", 0x0100);
        table.put("je_li", 0x0101);
        table.put("jne_li", 0x0102);
        table.put("jl_li", 0x0103);
        table.put("jle_li", 0x0104);
        
        table.put("external_li", 0x0105);
        table.put("elapsed_vi", 0x0106);
        
        table.put("dec_vi", 0x0107);
        table.put("inc_vi", 0x0108);
       
        table.put("random_vi", 0x0109);
        table.put("random_vf", 0x0109);
        table.put("random_vv", 0x0109);
        
        //NOTE: all three peeks should map to the same instruction, since type safety is not forced on peek function.
        //Any signatures with the * wildcard will have multiple signatures map to the same instruction. FUnctions
        //Marked with the * comment must share the same bytecode
        table.put("peek_vi", 0x010A); //*
        table.put("peek_vf", 0x010A);
        table.put("peek_vv", 0x010A);
        
        table.put("push_vi", 0x010B); //*
        table.put("push_vf", 0x010B);
        table.put("push_vv", 0x010B);
        
        table.put("push_li", 0x010B); //*
        table.put("push_lf", 0x010B);
        
        table.put("pop_vi", 0x010E); //*
        table.put("pop_vf", 0x010E);
        table.put("pop_vv", 0x010E);
        
        table.put("start_vi", 0x0);
        
        /* Dual argument math instructions */
        table.put("sin_vf_vf", 0x0200);
        table.put("cos_vf_vf", 0x0201);
        table.put("tan_vf_vf", 0x0202);
        
        table.put("abs_vi_vi", 0x0203);
        table.put("abs_vf_vf", 0x0204);
        
        table.put("norm_vi_vi", 0x0205);
        table.put("norm_vf_vf", 0x0206);
        
        table.put("ln_vf_vf", 0x0207);
        table.put("exp_vf_vf", 0x0208);
        table.put("floor_vf_vf", 0x0209);
        table.put("ceil_vf_vf", 0x020A);
        table.put("sqrt_vf_vf", 0x020B);
        table.put("neg_vf_vf", 0x020C);
        
        /* Dual argument non-math instructions */
        table.put("store_vi_li", 0x0240);
        table.put("store_vi_lf", 0x0241);
        
        table.put("store_vi_vi", 0x0242);
        table.put("store_vi_vf", 0x0242);
        table.put("store_vf_vi", 0x0242);
        table.put("store_vf_vf", 0x0242);
        table.put("store_vi_ii", 0x0242);
        table.put("store_vf_if", 0x0242);
        
        table.put("store_v*_p", 0x0242); //Need to figure out if i really want/need pointers...
        
        table.put("test_vv_vv", 0x0);
        
        table.put("cmp_vi_vi", 0x0243);
        table.put("cmp_vi_vf", 0x0243);
        table.put("cmp_vf_vi", 0x0243);
        table.put("cmp_vf_vf", 0x0243);
        
        table.put("cmp_vi_li", 0x0244);
        table.put("cmp_vf_li", 0x0244);
        table.put("cmp_vi_lf", 0x0245);
        table.put("cmp_vf_lf", 0x0245);
        
        table.put("stop_vi_vi", 0x0);
       
        
        /* Triple argument math instructions */
        table.put("add_vi_vi_vi", 0x0300); //vari <-- vari + vari
        table.put("add_vf_vf_vf", 0x0300); //vari <-- vari + varf
        table.put("add_vv_vv_vv", 0x0300); //varv <-- varv + varv
        
        table.put("add_vi_vi_li", 0x0300);
        table.put("add_vf_vf_lf", 0x0300);
        
        table.put("sub_vi_vi_vi", 0x0300);
        table.put("sub_vf_vf_vf", 0x0300);
        table.put("sub_vv_vv_vv", 0x0300);
        
        table.put("sub_vi_vi_li", 0x0300);
        table.put("sub_vf_vf_lf", 0x0300);
        
        table.put("sub_vi_li_vi", 0x0300);
        table.put("sub_vf_lf_vf", 0x0300);     
        
        table.put("mul_vi_vi_vi", 0x0300);
        table.put("mul_vf_vf_vf", 0x0300);
        
        table.put("mul_vv_vv_vf", 0x0300);
        table.put("mul_vv_vv_lf", 0x0300);
        
        table.put("mul_vi_vi_li", 0x0300);
        table.put("mul_vf_vf_lf", 0x0300);
        
        table.put("div_vi_vi_vi", 0x030B);
        table.put("div_vi_vi_li", 0x030C);
        table.put("div_vi_li_vi", 0x030C);
        
        table.put("div_vf_vf_vf", 0x030D);
        table.put("div_vf_vf_lf", 0x030E);
        table.put("div_vf_lf_vf", 0x030E);
        
        table.put("div_vv_vv_vf", 0x030D);
        table.put("div_vv_vv_lf", 0x030E);
        
        table.put("pow_vf_vf_vf", 0x0310);
        table.put("pow_vf_vf_lf", 0x0312);
        table.put("pow_vf_lf_vf", 0x0312);
        
        /* Triple argument non-math functions */
        table.put("store_vv_lf_lf", 0x0340);
        
        
    }
    
    /**
     * Tests whether line is a variable declaration
     * @param line a line of source code
     * @return true if the line is a variable declaration
     */
    public static boolean isIdentifier(String line)
    {
        Matcher m = IDENTIFIER_REG.matcher(line);
        return m.find();
    }
    
    /**
     * Tests whether line is a label declaration
     * @param line a line of source code
     * @return true if the line is a label declaration
     */
    public static boolean isLabel(String line)
    {
        Matcher m = LABEL_REG.matcher(line);
        return m.find();
        
    }
    
    /**
     * Tests whether the line is an instruction
     * @param line a line of source code
     * @return true if the line is a command
     */
    public static boolean isInstruction(String line)
    {
        Matcher m = INSCTUCTION_REG.matcher(line);
        return m.find();
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
        return m.group();
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
     * Parses the line and returns a list of arguments
     * NOTE: This function MUST be used on an instruction line. Test with 
     * isInstruction first.
     * @param line a line of source code
     * @return a list of arguments
     */
    public static String[] getArgs(String command, String line)
    {
        String s = line.split(command + "\\s*")[1]; //Get string after command (instruction/identifier)
        
        s = s.replaceAll("\\s", ""); //Remove whitespace
        
        String[] args = ARGS_DELIM_REG.split(s); //Separate by comma  
        
        /*for (int i = 0; i < args.length; ++i)
            System.out.print("'" + args[i] + "' ");*/
        
        return args;
    }
    
    /**
     * Get the index of the variable, i.e. "$3" would return 3.
     * @param variable the variable string
     * @return the index of the variable
     */
    public static int getVarIndex(String variable)
    {
        return Integer.parseInt(variable.substring(1, variable.length()));
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
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        
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
        
        int byteCount = 0; //Cumulative total number of bytes used by opcodes, used when addressing labels
        
        String[] typeTable = new String[30];
        
        while (true)
        {
            line = buff.readLine();
            
            if (line == null)
                break;
            
            if (isIdentifier(line))
            {
                String identifier = getIdentifier(line);
                
                //Get variable number
                String[] arguments = getArgs(identifier, line);
                
                if (arguments.length != 1)
                {
                    System.err.println("Too many/few arguments in declaration. Declaration takes exactly one argument.");
                    return;
                }
                
                String variable = arguments[0];
                
                if (getType(variable) != "v")
                {
                    System.err.println("Argument must be a variable.");
                    return;
                }
                
                int index = getVarIndex(variable);
                
                System.out.println("Declaration: " + identifier + " at index " + index);
                
                if (typeTable[index] != null)
                {
                    System.out.println("ERROR: $" + index + " has already been defined as " + typeTable[index]);
                    return;
                }
                
                typeTable[index] = identifier;
                
                continue;
            }
            
            if (isLabel(line))
            {
                
                System.out.println("Label: " + getLabel(line));
                continue;
            }
            
            if (isInstruction(line))
            {
                String instr = getInstruction(line);
                String[] arguments = getArgs(instr, line);
                
                String signature = instr;
                
                for (int i = 0; i < arguments.length; ++i)
                {
                    String type = getType(arguments[i]);
                    
                    if (type.equals("v"))
                        signature += "_v" + getCharIdentifier(typeTable[getVarIndex(arguments[i])]);
                    else
                        signature += "_l" + type;
                }
                
                System.out.println("Signature: " + signature);
            }
            
        }
            
        reader.close();
    }
    
}
