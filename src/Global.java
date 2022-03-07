import AST.TreeNode;

import java.util.ArrayList;
import java.util.HashMap;

public class Global {
    public static HashMap<String, String> reservedMap = new HashMap<>();
    public static HashMap<Character, String> singleMap = new HashMap<>();
    public static String inputContent = "";
    public static ArrayList<String> error_output = new ArrayList<>();
    public static HashMap<String, SymbolItem> globalTable = new HashMap<>();
    public static HashMap<String, SymbolItem> globalFuncTable = new HashMap<>();
    public static HashMap<String, HashMap<String, SymbolItem>> LayerTable = new HashMap<>();
    public static TreeNode head;
    public static void setSingleMap() {
        singleMap.put('+', "PLUS");
        singleMap.put('-', "MINU");
        singleMap.put('*', "MULT");
        singleMap.put('/', "DIV");
        singleMap.put('%', "MOD");
        singleMap.put(';', "SEMICN");
        singleMap.put(',', "COMMA");
        singleMap.put('(', "LPARENT");
        singleMap.put(')', "RPARENT");
        singleMap.put('[', "LBRACK");
        singleMap.put(']', "RBRACK");
        singleMap.put('{', "LBRACE");
        singleMap.put('}', "RBRACE");
    }

    public static void setReservedMap(){
        reservedMap.put("main", "MAINTK");
        reservedMap.put("const", "CONSTTK");
        reservedMap.put("int", "INTTK");
        reservedMap.put("break", "BREAKTK");
        reservedMap.put("continue", "CONTINUETK");
        reservedMap.put("if", "IFTK");
        reservedMap.put("else", "ELSETK");
        reservedMap.put("while", "WHILETK");
        reservedMap.put("getint", "GETINTTK");
        reservedMap.put("printf", "PRINTFTK");
        reservedMap.put("return", "RETURNTK");
        reservedMap.put("void", "VOIDTK");
    }

}
