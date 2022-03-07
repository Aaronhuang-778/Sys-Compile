import java.util.ArrayList;
import java.util.HashMap;

public class Lexical {
    public static int indexs = 0;
    public static int oldIndex = 0;
    public static int line = 1;
    public static String token = "";
    public static String symbol = "";
    public static int num;
    public static char ch;
    public static ArrayList<String> output = new ArrayList<>();
    public static HashMap<String, String> reservedMap = new HashMap<>();
    public static HashMap<Integer, Integer> formatNumber = new HashMap<>();
    public static int getSym() {
        oldIndex = indexs;  //最初的index
        reservedMap = Global.reservedMap;
        //初始化
        clearToken();
        get_ch();
        //跳过空白字符
//        while (isBlank()) {
//            get_ch();
//        }
        if (isEOF()) {
            return -1;
        }
        //a-z, A-Z, _   字母开头：保留字、标识符
        if (isLetter()) {
            while (isLetter() || Character.isDigit(ch)) {
                catToken();
                get_ch();
            }
            reTrack();
            if (!reserved()) {  //是标识符
                symbol = "IDENFR";
            }
            output.add(symbol + " " + token + " " + line);
        }
        //数字开头
        else if (Character.isDigit(ch)) {
            while (Character.isDigit(ch)) {
                catToken();
                get_ch();
            }
            reTrack();
            num = Integer.parseInt(token);
            if (token.charAt(0) == '0' && token.length() > 1) {
                System.out.println("前导0错误!");
            }
            symbol = "INTCON";
            output.add(symbol + " " + num + " " + line);
        }
        else if (ch == '/') {
            get_ch();
            if (ch == '*') {
                while (true) {
                    get_ch();
                    if (ch == '*') {
                        get_ch();
                        if (ch == '/') break;
                        else reTrack();
                    }
                }
            }
            else if (ch == '/') {
                get_ch();
                while (ch != '\n') {
                    get_ch();
                }
            }
            else {
                reTrack();
                symbol = "DIV";
                token = "/";
                output.add(symbol + " " + token + " " + line);
            }
        }

        //双引号
        else if (isDquo()) {
            formatNumber.put(line, 0);
            int flag = 0;
            catToken();
            get_ch();
            while (isNormalChar() || isFormatChar()) {
                catToken();
                get_ch();
            }
            while (!isDquo()) {
                if (!isNormalChar() && !isFormatChar()) {
                    flag = 1;
                }
                catToken();
                get_ch();
            }
            symbol = "STRCON";
            catToken();
            output.add(symbol + " " + token + " " + line);
            if (flag==1){
                System.out.println("不符合词法" + ch);
                Global.error_output.set(line, line + " " + "a");
            }
        }
        else if (isLss()) {
            catToken();
            get_ch();
            if (isAssign()) {
                catToken();
                symbol = "LEQ";
            }
            else {
                symbol = "LSS";
                reTrack();
            }
            output.add(symbol + " " + token + " " + line);
        }
        else if (isGre()) {
            catToken();
            get_ch();
            if (isAssign()) {
                catToken();
                symbol = "GEQ";
            }
            else {
                symbol = "GRE";
                reTrack();
            }
            output.add(symbol + " " + token + " " + line);
        }
        else if (isNot()) {
            catToken();
            get_ch();
            if (isAssign()) {
                catToken();
                symbol = "NEQ";
            }
            else {
                symbol = "NOT";
                reTrack();
            }
            output.add(symbol + " " + token + " " + line);
        }
        else if (isAssign()) {
            catToken();
            get_ch();
            if (isAssign()) {
                symbol = "EQL";
                catToken();
            }
            else {
                symbol = "ASSIGN";
                reTrack();
            }
            output.add(symbol + " " + token + " " + line);
        }
        else if (isAnd()) {
            catToken();
            get_ch();
            if (isAnd()) {
                symbol = "AND";
                catToken();
                output.add(symbol + " " + token + " " + line);
            }
            else {
                System.out.println("错误的&符号!");
                reTrack();
            }

        }
        else if (isOr()) {
            catToken();
            get_ch();
            if (isOr()) {
                symbol = "OR";
                catToken();
                output.add(symbol + " " + token + " " + line);
            }
            else {
                System.out.println("错误的|符号!");
                reTrack();
            }
        }
        else if (reservedS()){
            token += ch;
            output.add(symbol + " " + ch + " " + line);
        }
        return 1;
    }
    //
    //
    //
    //功能函数
    private static boolean reservedS() {
        symbol = Global.singleMap.get(ch);
        if (symbol != null) {
            return true;
        }
        return false;
    }
    private static boolean reserved() {
        symbol = reservedMap.get(token);
        if (symbol != null) {
            return true;
        }
        return false;
    }
    //
    //
    //
    //读取处理
    private static void catToken() {
        token += ch;
    }

    private static void reTrack() {
        if (ch == '\n') line--;
        indexs--;
    }

    private static void clearToken() {
        token = "";
    }

    public static void get_ch() {
        ch = Global.inputContent.charAt(indexs);
        indexs ++;
        if (ch == '\n') {
            line++;
        }
    }
    //判断字符
    public static boolean isBlank() {
        return (ch == ' ' || ch == '\n' || ch == '\t'
                || ch == '\r' || ch == '\f' );
    }

    public static boolean isEOF() {
        return indexs >= Global.inputContent.length();
    }

    public static boolean isLetter() {
        return ch=='_' || Character.isLetter(ch);
    }
    public static boolean isDquo() {
        return ch=='\"';
    }
    public static boolean isLss() {
        return ch=='<';
    }
    public static boolean isGre() {
        return ch=='>';
    }
    public static boolean isAssign() {
        return ch=='=';
    }
    public static boolean isNot() {
        return ch=='!';
    }
    public static boolean isAnd() {
        return ch=='&';
    }
    public static boolean isOr() {
        return ch=='|';
    }
    public static boolean isNormalChar() {
        int ascll = ch;
        return (ascll == 32 || ascll == 33|| (ascll >= 40 && ascll <=126 && ascll != 92));
    }
    public static boolean isFormatChar() {
        int ascll = ch;
        int ascll1 = Global.inputContent.charAt(indexs);
        if ((ascll == 37 && ascll1 == 100) || (ascll == 92 && ascll1 == 110) ) {
            if (ascll == 37 && ascll1 == 100) {
                int num = formatNumber.get(line);
                num ++;
                formatNumber.put(line, num);
            }
            catToken();
            get_ch();
            return true;
        }
        return false;
    }

    public static boolean isNewLine() {
        return ch=='\n';
    }

}
