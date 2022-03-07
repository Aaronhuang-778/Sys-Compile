
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Grammer {
    public static ArrayList<String> output = new ArrayList<>();
    public static int index = -1;
    public static String line = "";
    public static String preline = "";
    public static String symbol = "";
    public static String token = "";
    public static ArrayList<HashMap<String, SymbolItem>> localLayer = new ArrayList<>(); //语句块中的return
    public static int layerSize = -1;
    public static ArrayList<ArrayList<Integer>> rp = new ArrayList<>(); //函数的调用参数
    public static ArrayList<Integer> cut_prase = new ArrayList<>(); //while 语句
    //编译单元：CompUnit->{Decl} {FuncD      ef} MainFuncDef
    public static boolean CompUnit() {
        int re = getOne();
        //Decl
        while(Decl(true)){

        }
        while (FuncDef()) {

        }
        if (!MainFuncDef()) {
            return false;
        }
        output.add("<CompUnit>");
        return true;
    }
    // MainFuncDef → 'int' 'main' '(' ')' Block
    private static boolean MainFuncDef() {
        String mname;
        if (Objects.equals(symbol, "INTTK")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "MAINTK") ) {
                mname = token;
                output.add(symbol + " " + token);
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                SymbolItem sb = new SymbolItem(mname,-1,2,
                        0);
                Global.globalFuncTable.put(mname, sb);
                HashMap<String, SymbolItem> localTable = new HashMap<>();
                localTable.put(mname, sb);
                localLayer.add(localTable);
                layerSize ++;
                if (Objects.equals(symbol, "LPARENT")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    if (!Objects.equals(symbol, "RPARENT")) {
                        System.out.println("函数缺少)！");
                        Global.error_output.set(Integer.parseInt(preline), preline + " " + "j");
                    }
                    else {
                        output.add(symbol + " " + token);
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                    }
                    Block(0);
                }
                System.out.println("本层符号表： ");
                for (String item :localLayer.get(layerSize).keySet()) {
                    System.out.println(item);
                }
                localLayer.remove(layerSize);
                layerSize --;
            }
        }
        else {
            System.out.println("main函数类型定义错误");
            return false;
        }
        output.add("<MainFuncDef>");
        return true;
    }

    //  FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    private static boolean FuncDef() {
        //FuncType
        String fname;
        int pnum = 0;
        String funcline = "";
        int type = 0;
        //type = 0:int 函数
        //type = 1:void 函数
        // type = 2:int 非函数
        if (Objects.equals(symbol, "VOIDTK") || Objects.equals(symbol, "INTTK")) {
            if (Objects.equals(symbol, "VOIDTK")) {
                type = 1;
            }
            int re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "MAINTK")) {
                reTrack();
                return false;
            }
            else {
                reTrack();
                output.add(symbol + " " + token);
                output.add("<FuncType>");
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                //Ident
                if (Objects.equals(symbol, "IDENFR")) {
                    fname = token;
                    funcline = line;
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    //判断参数表
                    if ( Global.globalFuncTable.containsKey(fname)) {
                        Global.error_output.set(Integer.parseInt(funcline), funcline + " " + "b");
                        System.out.println("与全局定义冲突！");
                    }
                    SymbolItem sb = new SymbolItem(fname,-1,2,
                            type*3);
                    Global.globalFuncTable.put(fname, sb);
                    HashMap<String, SymbolItem> localTable = new HashMap<>();
                    localTable.put(fname, sb);
                    localLayer.add(localTable);
                    layerSize ++;

                    //(
                    if (Objects.equals(symbol, "LPARENT")) {
                        output.add(symbol + " " + token);
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                        //[FuncRParams]
                        FuncFParams(fname);
                        //)
                        if (!Objects.equals(symbol, "RPARENT")) {
                            System.out.println("函数缺少)！");
                            Global.error_output.set(Integer.parseInt(preline), preline + " " + "j");
                        }
                        else {
                            output.add(symbol + " " + token);
                            re = getOne();  //读下一个 然后进入常量定义
                            if (re < 0) {  //没有下一个 出错
                                return false;
                            }
                        }

                        Block(type);

                        System.out.println("本层符号表： ");
                        for (String item :localLayer.get(layerSize).keySet()) {
                            System.out.println(item);
                        }
                        localLayer.remove(layerSize);
                        layerSize --;
                    }
                    else {
                        System.out.println("函数缺少(！");
                        return false;
                    }
                }
                else {
                    System.out.println("函数缺少名称！");
                    return false;
                }
            }
        }
        else {
            System.out.println("函数开头错误！");
            return false;
        }
        output.add("<FuncDef>");
        return true;
    }
    //Block → '{' { BlockItem } '}'
    private static boolean Block(int type) {
        String rline = "";
        if (Objects.equals(symbol, "LBRACE")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (type == 21 || type == 20) {
                HashMap<String, SymbolItem> localTable = new HashMap<>();
                localLayer.add(localTable);
                layerSize ++;
            }
            while (!Objects.equals(symbol, "RBRACE")) {
                BlockItem(type);
            }
            if (Objects.equals(symbol, "RBRACE")) {
                rline = line;
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    if (index < Lexical.output.size()) {
                        return false;
                    }
                }
            }
            if (type == 21 || type == 20) {
                System.out.println("本层符号表： ");
                for (String item :localLayer.get(layerSize).keySet()) {
                    System.out.println(item);
                }
                localLayer.remove(layerSize);
                layerSize --;
            }
            else if (type == 0) {
                for (Map.Entry<String, SymbolItem> entry : localLayer.get(layerSize).entrySet()) {
                    SymbolItem funitem = entry.getValue();
                    System.out.println(entry.getKey());
                    if (funitem.kind == 2 && funitem.alreadyReturn == 0) {
                        System.out.println("int 类型函数最终缺少返回语句");
                        Global.error_output.set(Integer.parseInt(rline), rline + " " + "g");
                    }
                }
            }
        }
        else {
            System.out.println("Block没有使用{开头!");
            return false;
        }
        output.add("<Block>");
        return true;
    }

    private static boolean BlockItem(int type) {

        if (Decl(false)){

        }
        else if (Stmt(type)){

        }
        else {
            System.out.println("BlockItem 为空！");
            return false;
        }
        return true;
    }
    /*
    Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
    | [Exp] ';' //有⽆Exp两种情况
    | Block
    | LVal = 'getint''('')'';'

    | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.⽆else
    | 'while' '(' Cond ')' Stmt
    | 'break' ';' | 'continue' ';'
    | 'return' [Exp] ';' // 1.有Exp 2.⽆Exp
    | 'printf''('FormatString{,Exp}')'';'
    */
    private static boolean Stmt(int type) {
        //'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        int next_type = 0;
        if (type == 0) {
            next_type = 20;
        }
        else if (type == 1) {
            next_type = 21;
        }
        else {
            next_type = type;
        }
        int re;

        if (Objects.equals(symbol, "IFTK")) {
            output.add(symbol + " " + token);
            re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "LPARENT")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!Cond())  {
                    System.out.println("if 语句缺少Cond条件判断!");
                }//预读一个词
                if (!Objects.equals(symbol, "RPARENT")) {
                    System.out.println("if 语句缺少)!");
                    Global.error_output.set(Integer.parseInt(preline), preline + " " + "j");

                }
                else {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                }

                if(!Stmt(next_type)) {
                    System.out.println("if中Stmt语句错误！");
                }

                if (Objects.equals(symbol, "ELSETK")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }

                    if (!Stmt(next_type)) {
                        System.out.println("if中Stmt语句错误！");
                    }

                }
            }
            else {
                System.out.println("if 语句缺少(!");
                return false;
            }
            output.add("<Stmt>");
            return true;
        }
        //'while' '(' Cond ')' Stmt
        if (Objects.equals(symbol, "WHILETK")) {
            output.add(symbol + " " + token);
            re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "LPARENT")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!Cond())  {
                    System.out.println("while 语句缺少Cond条件判断!");
                }//预读一个词
                if (Objects.equals(symbol, "RPARENT")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    //while语句设计一个判断堆栈，堆栈内容不为空则可以进行break和continue，否则报错；
                    cut_prase.add(1);
                    if(!Stmt(next_type)) {
                        System.out.println("while中Stmt语句错误！");
                    }

                    cut_prase.remove(cut_prase.size() - 1);
                }
                else {
                    System.out.println("while 语句缺少)!");
                    Global.error_output.set(Integer.parseInt(preline), preline + " " + "j");
                }
            }
            else {
                System.out.println("while 语句缺少(!");
                return false;
            }
            output.add("<Stmt>");
            return true;
        }
        // 'break' ';' | 'continue' ';'
        if (Objects.equals(symbol, "BREAKTK") || Objects.equals(symbol, "CONTINUETK")){
            String cutline = line;
            if (cut_prase.isEmpty()) {
                System.out.println("中断语句出现位置不合理!");
                Global.error_output.set(Integer.parseInt(line), line + " " + "m");
            }
            output.add(symbol + " " + token);
            re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "SEMICN")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            } else {
                System.out.println("break|continue语句缺少;");
                Global.error_output.set(Integer.parseInt(cutline), cutline + " " + "i");
            }
            output.add("<Stmt>");
            return true;
        }
        //'return' [Exp] ';'
        if (Objects.equals(symbol, "RETURNTK")) {
            String returnline = line;
            output.add(symbol + " " + token);
            re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            //Exp,可有可无
            if (Exp()) {
                if (type == 1 || type == 21) {
                    System.out.println("无返回语句块中出现return表达式!");
                    Global.error_output.set(Integer.parseInt(returnline), returnline + " " + "f");
                }
                else if (layerSize == 0) {
                    String symbol1 = symbol;
                    re = getOne();
                    String symbol2 = symbol;
                    if (Objects.equals(symbol1, "RBRACE") || Objects.equals(symbol2, "RBRACE")) {
                        for (Map.Entry<String, SymbolItem> entry : localLayer.get(layerSize).entrySet()) {
                            SymbolItem funitem = entry.getValue();
                            if (funitem.kind == 2 && funitem.alreadyReturn == 0) {
                                funitem.alreadyReturn = 1;
                            }
                        }
                    }
                    reTrack();
                }
            }


            if (Objects.equals(symbol, "SEMICN")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            } else {
                System.out.println("return语句缺少;");
                Global.error_output.set(Integer.parseInt(returnline), returnline + " " + "i");
            }
            output.add("<Stmt>");
            return true;
        }
        //'printf''('FormatString{,Exp}')'';'
        if (Objects.equals(symbol, "PRINTFTK")) {
            String pline = line;
            int fnum = 0;
            int pnum = -1;
            output.add(symbol + " " + token);
            re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "LPARENT")) {
                String lline = line;
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (Objects.equals(symbol, "STRCON")) {
                    pnum = Lexical.formatNumber.get(Integer.parseInt(line));
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                }
                while (Objects.equals(symbol, "COMMA")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (!Exp()) {
                        System.out.println("printf输出语句缺少Exp！");
                    }
                    else {
                        fnum ++;
                    }
                }
                if (pnum != fnum) {
                    System.out.println();
                    Global.error_output.set(Integer.parseInt(pline), pline + " " + "l");
                }
                if (Objects.equals(symbol, "RPARENT")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                }
                else {
                    System.out.println("printf语句缺少)!");
                    Global.error_output.set(Integer.parseInt(preline), preline + " " + "j");
                }
                if (Objects.equals(symbol, "SEMICN")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                } else {
                    System.out.println("printf语句缺少;");
                    Global.error_output.set(Integer.parseInt(preline), preline + " " + "i");
                }
            }
            else {
                System.out.println("printf语句缺少(!");
                return false;
            }
            output.add("<Stmt>");
            return true;
        }
        // LVal '=' Exp ';'
        // LVal '=' 'getint''('')'';'
        int pre_lexical = index;
        int pre_grammer = Grammer.output.size();
        int flag = 0;
        if (LVal()) {
            if (Objects.equals(symbol, "ASSIGN")){
                flag = 1;
            }
        }
        index = pre_lexical;
        if (Grammer.output.size() > pre_grammer) {
            Grammer.output.subList(pre_grammer, Grammer.output.size()).clear();
        }
        reTrack();
        getOne();

        if (Objects.equals(symbol, "IDENFR")) {
            String lvalName = token;
            re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (flag == 1) {
                int kind = 0;
                int have = 0;
                if (Global.globalTable.containsKey(lvalName)) {
                    kind = Global.globalTable.get(lvalName).kind;
                    have = 1;
                }
                else {
                    for (HashMap<String, SymbolItem> map : localLayer) {
                        if (map.containsKey(lvalName)) {
                            have = 1;
                            kind = map.get(lvalName).kind;
                            break;
                        }
                    }
                }
                if (have != 0) {
                    if (kind == 1) {
                        System.out.println("这是个常量无法改变！");
                        Global.error_output.set(Integer.parseInt(preline), preline + " " + "h");
                    }
                }

                reTrack();
                LVal();

                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (Objects.equals(symbol, "GETINTTK")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    if (Objects.equals(symbol, "LPARENT")) {
                        output.add(symbol + " " + token);
                        re = getOne();  //读下一个
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                        if (Objects.equals(symbol, "RPARENT")) {
                            output.add(symbol + " " + token);
                            re = getOne();  //读下一个
                            if (re < 0) {  //没有下一个 出错
                                return false;
                            }
                        } else {
                            System.out.println("getint缺少')'!");
                            Global.error_output.set(Integer.parseInt(preline), preline + " " + "j");
                        }
                    }
                    else {
                        System.out.println("getint缺少'('!");
                        return false;
                    }
                }
                else if (!Exp()) {
                    System.out.println("LVal赋值语句缺少Exp！");
                    return false;
                }

                if (Objects.equals(symbol, "SEMICN")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                } else {
                    System.out.println("LVal右边语句缺少;");
                    Global.error_output.set(Integer.parseInt(preline), preline + " " + "i");
                }
                output.add("<Stmt>");
                return true;
            }
            else {
                reTrack();
            }
        }
        pre_lexical = index;
        pre_grammer = Grammer.output.size();
        //[Exp] ';'
        if (Exp()) {
            if (Objects.equals(symbol, "SEMICN")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            } else {
                System.out.println("EXP缺少;");
                Global.error_output.set(Integer.parseInt(preline), preline + " " + "i");
            }
            output.add("<Stmt>");
            return true;
        }
        else {
            index = pre_lexical;
            if (Grammer.output.size() > pre_grammer) {
                Grammer.output.subList(pre_grammer, Grammer.output.size()).clear();
            }
            reTrack();
            getOne();
            if (Objects.equals(symbol, "SEMICN")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                output.add("<Stmt>");
                return true;
            }
        }
        //Block
        if (Block(next_type));


        output.add("<Stmt>");
        return true;
    }


    private static boolean Cond() {
        if (!LOrExp()) {
            System.out.println("Cond中没有LOrExp!");
            return false;
        }
        output.add("<Cond>");
        return true;
    }
    // LOrExp → LAndExp | LOrExp '||' LAndExp
    // LOrExp → LAndExp {'||' LAndExp}
    private static boolean LOrExp() {
        if (LAndExp()) {
            while (Objects.equals(symbol, "OR")) {
                output.add("<LOrExp>");
                output.add(symbol + " " + token);
                int re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!LAndExp()) {
                    System.out.println("LOrExp当中||之后缺少表达式!");
                }
            }
        }
        else {
            System.out.println("不是LOrExp！");
            return false;
        }
        output.add("<LOrExp>");
        return true;
    }
    // LAndExp → EqExp | LAndExp '&&' EqExp
    // LAndExp → EqExp {'&&' EqExp}
    private static boolean LAndExp() {
        if (EqExp()) {
            while (Objects.equals(symbol, "AND")) {
                output.add("<LAndExp>");
                output.add(symbol + " " + token);
                int re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!EqExp()) {
                    System.out.println("LAndExp当中&&之后缺少表达式!");
                }
            }
        }
        else {
            System.out.println("不是LAndExp！");
            return false;
        }
        output.add("<LAndExp>");
        return true;
    }
    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    //    // EqExp → RelExp { ('==' | '!=') RelExp }
    private static boolean EqExp() {
        if (RelExp()) {
            while (Objects.equals(symbol, "EQL") || Objects.equals(symbol, "NEQ")) {
                output.add("<EqExp>");
                output.add(symbol + " " + token);
                int re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!RelExp()) {
                    System.out.println("EqExp缺少右边RelExp表达式!");
                }
            }
        }
        else {
            System.out.println("不是EqExp！");
            return false;
        }
        output.add("<EqExp>");
        return true;
    }
    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    // RelExp → AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    private static boolean RelExp() {
        if (AddExp()) {
            while (Objects.equals(symbol, "LEQ") || Objects.equals(symbol, "LSS")
                    || Objects.equals(symbol, "GEQ") || Objects.equals(symbol, "GRE")) {
                output.add("<RelExp>");
                output.add(symbol + " " + token);
                int re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!AddExp()) {
                    System.out.println("RelExp缺少右边AddExp表达式!");
                }
            }
        }
        else {
            System.out.println("不是RelExp!");
            return false;
        }
        output.add("<RelExp>");
        return true;
    }
    // FuncRParams → Exp { ',' Exp }
    private static int FuncRParams(String iname) {
        ArrayList<Integer> ar = new ArrayList<>();
        ar.add(-1);
        rp.add(ar);
        boolean flag ;
        if (Exp()){
            while (Objects.equals(symbol, "COMMA")) {
                rp.get(rp.size() - 1).add(-1);
                output.add(symbol + " " + token);
                int re = getOne();  //读下一个
//                if (re < 0) {  //没有下一个 出错
//                    return false;
//                }
                if (!Exp()) {
                    System.out.println("，后缺少函数参数！");
                }
            }
            System.out.println("gogogo" + rp.get(rp.size() - 1).size());
        }

        //
        output.add("<FuncRParams>");
        return 1;
    }

    //FuncFParams → FuncFParam { ',' FuncFParam }
    private static boolean FuncFParams(String fname) {
        if (FuncFParam(fname)) {
            while (Objects.equals(symbol, "COMMA")) {
                output.add(symbol + " " + token);
                int re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!FuncFParam(fname)) {
                    System.out.println("，后缺少函数参数！");
                    return false;
                }
            }
        }
        else {
            System.out.println("FuncFParams 无参数");
            return false;
        }
        output.add("<FuncFParams>");
        return true;
    }
    //FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    private static boolean FuncFParam(String fname) {
        String fpname;
        int num = 0;
        //BType
        int type;
        if (Objects.equals(symbol, "INTTK")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }

            //Ident
            if (Objects.equals(symbol, "IDENFR")) {
                fpname = token;
                output.add(symbol + " " + token);
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                int flag = 0;
                //['[' ']' { '[' ConstExp ']' }]
                if (Objects.equals(symbol, "LBRACK")) {
                    String preline = line;
                    num += 1;
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    if (!Objects.equals(symbol, "RBRACK")) {
                        System.out.println("数组传值缺少]！");
                        Global.error_output.set(Integer.parseInt(preline), preline + " " + "k");
                    }
                    else {
                        output.add(symbol + " " + token);
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                    }

                    //{ '[' ConstExp ']' }
                    if (Objects.equals(symbol, "LBRACK")) {
                        num +=1;
                        while (Objects.equals(symbol, "LBRACK")) {
                            output.add(symbol + " " + token);
                            re = getOne();  //读下一个 然后进入常量定义
                            if (re < 0) {  //没有下一个 出错
                                return false;
                            }
                            if (!ConstExp()) {
                                System.out.println("二维数组第二维度缺少长度！");
                                return false;
                            }//预读一个词
                            if (Objects.equals(symbol, "RBRACK")) {
                                output.add(symbol + " " + token);
                                //继续进入ConsDef
                                re = getOne();  //读下一个 然后进入常量定义
                                if (re < 0) {  //没有下一个 出错
                                    return false;
                                }
                            } else {
                                System.out.println("数组传值缺少]！");
                                Global.error_output.set(Integer.parseInt(preline), preline + " " + "k");
                                break;
                            }
                        }
                    }
                }
            }
            else {
                System.out.println("函数参数明缺失！");
                return false;
            }
        }
        else {
            System.out.println("函数参数类型错误！");
            return false;
        }
        int l = localLayer.size() - 1;
        if (!localLayer.get(l).containsKey(fpname)) {
            SymbolItem sb = new SymbolItem(fpname, -1, 0,
                    num);
            localLayer.get(l).put(fpname, sb);
            Global.globalFuncTable.get(fname).insertPar(num);
        }
        else {
            Global.error_output.set(Integer.parseInt(line), line + " " + "b");
            System.out.println(fpname);
            System.out.println("函数定义参数与函数定义参数冲突！");
        }

        output.add("<FuncFParam>");
        return true;
    }

    private static boolean Decl(boolean global) {
        int re;
        if (Objects.equals(symbol, "CONSTTK")) {
            ConstDecl(global);
        } else if (Objects.equals(symbol, "INTTK")) {
            re = getOne();
            re = getOne();
            if (Objects.equals(symbol, "LPARENT")) {
                reTrack();
                reTrack();
                return false;
            } else {
                reTrack();
                reTrack();
                VarDecl(global);
            }
        } else {
            return false;
        }
        return true;
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private static boolean ConstDecl(boolean global) {
        //ConstDDecl
        if (Objects.equals(symbol, "CONSTTK")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "INTTK")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!ConstDef(global)) {

                }//需要预读一个词
                if (Objects.equals(symbol, "COMMA")) {
                    while (Objects.equals(symbol, "COMMA")) {
                        output.add(symbol + " " + token);
                        //继续进入ConsDef
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                        if (!ConstDef(global)) {

                        }
                    }
                }
                if (Objects.equals(symbol, "SEMICN")) {
                    output.add(symbol + " " + token);
                    output.add("<ConstDecl>");
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                } else {
                    Global.error_output.set(Integer.parseInt(preline), preline + " " + "i");
                }
            }
        } else {
            return false;
        }
        return true;
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    private static boolean VarDecl(boolean global) {
        if (Objects.equals(symbol, "INTTK")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (!VarDef(global)) {

            }//需要预读一个词
            if (Objects.equals(symbol, "COMMA")) {
                while (Objects.equals(symbol, "COMMA")) {
                    output.add(symbol + " " + token);
                    //继续进入ConsDef
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    if (!VarDef(global)) {

                    }
                }
            }
            if (Objects.equals(symbol, "SEMICN")) {
                output.add(symbol + " " + token);
                output.add("<VarDecl>");
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            } else {
                System.out.println("变量定义缺少;");
                Global.error_output.set(Integer.parseInt(preline), preline + " " + "i");
            }
        }
        else {
            return false;
        }
        return true;
    }

    //VarDef → Ident { '[' ConstExp ']' }
    // | Ident { '[' ConstExp ']' } '=' InitVal
    // 包含普通变量、⼀维数组、⼆维数组定义
    private static boolean VarDef(boolean global) {
        String vname;
        int dem = 0;
        int re;
        String idline = "";
        if (Objects.equals(symbol, "IDENFR")) {
            idline = line;
            vname = token;
            output.add(symbol + " " + token);
            re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (Objects.equals(symbol, "LBRACK")) {
                while (Objects.equals(symbol, "LBRACK")) {
                    System.out.println("1111111");
                    dem ++;
                    output.add(symbol + " " + token);
                    //继续进入ConsDef
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    if (!ConstExp()) {
                        return false;
                    }//预读一个词
                    if (Objects.equals(symbol, "RBRACK")) {
                        output.add(symbol + " " + token);
                        //继续进入ConsDef
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                    } else {
                        Global.error_output.set(Integer.parseInt(line), line + " " + "k");
                        if (dem == 2) {
                            break;
                        }
                    }
                }
            }
            if (Objects.equals(symbol, "ASSIGN")) {
                output.add(symbol + " " + token);
                //继续进入ConsDef
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!InitVal()) {
                    reTrack();
                    return false;
                }//预读一个词
            }
            //符号表管理
            if (global) {
                if (Global.globalTable.containsKey(vname)) {
                    Global.error_output.set(Integer.parseInt(idline), idline + " " + "b");
                    System.out.println("与全局定义冲突！");
                }
                else {
                    SymbolItem sb = new SymbolItem(vname,-1,0,
                            dem);
                    Global.globalTable.put(vname, sb);
                }
            }
            else {
                HashMap<String, SymbolItem> temp = localLayer.get(layerSize);
                if (temp.containsKey(vname)) {
                    if (temp.get(vname).kind != 2) {
                        Global.error_output.set(Integer.parseInt(idline), idline + " " + "b");
                        System.out.println("与局部定义冲突！");
                    }
                }
                else {
                    SymbolItem sb = new SymbolItem(vname,-1,0,
                            dem);
                    localLayer.get(layerSize).put(vname, sb);
                }
            }

            output.add("<VarDef>");
            return true;
        }
        else {
            return false;
        }
    }

    //ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    //
    private static boolean ConstDef(boolean global) {
        String cname;
        int dem = 0;
        int re;
        String constline = "";
        if (Objects.equals(symbol, "IDENFR")) {
            constline = line;
            cname = token;
            output.add(symbol + " " + token);
            re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }

            if (Objects.equals(symbol, "LBRACK")) {
                while (Objects.equals(symbol, "LBRACK")) {
                    dem ++;
                    output.add(symbol + " " + token);
                    //继续进入ConsDef
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    if (!ConstExp()) {
                        return false;
                    }//预读一个词
                    if (Objects.equals(symbol, "RBRACK")) {
                        output.add(symbol + " " + token);
                        //继续进入ConsDef
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                    } else {
                        Global.error_output.set(Integer.parseInt(line), line + " " + "k");
                        if (dem == 2) {
                            break;
                        }
                    }
                }
            }
            if (Objects.equals(symbol, "ASSIGN")) {
                output.add(symbol + " " + token);
                //继续进入ConsDef
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!ConstInitVal()) {
                    return false;
                }//预读一个词

            } else {
                System.out.println("常量说明缺少等号！");
            }
            //符号表管理
            if (global) {
                if (Global.globalTable.containsKey(cname)) {
                    Global.error_output.set(Integer.parseInt(constline), constline + " " + "b");
                    System.out.println("与全局定义冲突！");
                }
                else {
                    SymbolItem sb = new SymbolItem(cname,-1,1,
                            dem);
                    Global.globalTable.put(cname, sb);
                }
            }
            else {
                HashMap<String, SymbolItem> temp = localLayer.get(layerSize);
                if (temp.containsKey(cname)) {
                    if (temp.get(cname).kind != 2) {
                        Global.error_output.set(Integer.parseInt(constline), constline + " " + "b");
                        System.out.println("与局部定义冲突！");
                    }
                }
                else {
                    SymbolItem sb = new SymbolItem(cname,-1,1,
                            dem);
                    localLayer.get(layerSize).put(cname, sb);
                }
            }

            output.add("<ConstDef>");
            return true;
        } else {
            return false;
        }
    }

    //ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private static boolean ConstInitVal() {
        if (Objects.equals(symbol, "LBRACE")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (ConstInitVal()) {
                if (Objects.equals(symbol, "COMMA")) {
                    while (Objects.equals(symbol, "COMMA")) {
                        output.add(symbol + " " + token);
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                        if (!ConstInitVal()) {
                            return false;
                        }
                    }
                }
            }
            if (Objects.equals(symbol, "RBRACE")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            } else {
                System.out.println("ConstInitVal 缺少}!");
            }

        }
        else if (ConstExp()) {

        }
        else {
//            reTrack();
            return false;
        }

        output.add("<ConstInitVal>");
        return true;
    }
    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private static boolean InitVal() {
        if (Objects.equals(symbol, "LBRACE")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个 然后进入常量定义
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (InitVal()) {
                if (Objects.equals(symbol, "COMMA")) {
                    while (Objects.equals(symbol, "COMMA")) {
                        output.add(symbol + " " + token);
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                        if (!InitVal()) {
                            return false;
                        }
                    }
                }
            }
            if (Objects.equals(symbol, "RBRACE")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            } else {
                System.out.println("InitVal 缺少}!");
            }
        }
        else if (!Exp()){
            return false;
        }

        output.add("<InitVal>");
        return true;
    }

    private static boolean ConstExp() {
        //已经预读了一个词
        if (!AddExp()) {
            return false;
        }
        output.add("<ConstExp>");
        return true;
    }
    //AddExp→ MulExp { ('+' | '−') MulExp}
    private static boolean AddExp() {
        if (MulExp()) {
            while (Objects.equals(symbol, "PLUS") || Objects.equals(symbol, "MINU")) {
                output.add("<AddExp>");
                output.add(symbol + " " + token);
                int re = getOne();  //读下一个 然后进入常量定义
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
                if (!MulExp()) {
                    System.out.println("Add Exp 第二种情况出现错误！");
                    return false;
                }
            }
        }
        else {
            return false;
        }
        output.add("<AddExp>");
        return true;
    }

    //MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    //MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
    private static boolean MulExp() {
        if (UnaryExp()) {
            if (Objects.equals(symbol, "MULT") || Objects.equals(symbol, "MOD")
                    || Objects.equals(symbol, "DIV")) {
                while (Objects.equals(symbol, "MULT") || Objects.equals(symbol, "MOD")
                        || Objects.equals(symbol, "DIV")) {
                    output.add("<MulExp>");
                    output.add(symbol + " " + token);
                    int re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    if (!UnaryExp()) {
                        System.out.println("MulExp 第二种情况出现错误！");
                        return false;
                    }
                }
            }
        }
        else {
            return false;
        }

        output.add("<MulExp>");
        return true;
    }

    //UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp // 存在即可
    private static boolean UnaryExp() {
        int flag = 0;
        String iname;
        String idenline = "";
        if (Objects.equals(symbol, "IDENFR")) {
            iname = token;
            idenline = line;
            int re = getOne();
            if (re < 0) {
                return false;
            }
            if (!Objects.equals(symbol, "LPARENT")){
                reTrack();
            }
            else {
                if (!Global.globalFuncTable.containsKey(iname)) {
                    flag = 1;
                    Global.error_output.set(Integer.parseInt(idenline), idenline + " " + "c");
                    System.out.println("使用未定义名字！");
                }
                //判断是否是一个函数参数
                if (flag == 0){
                    int type = Global.globalFuncTable.get(iname).type;
                    if (rp.size() != 0 ) {
                        ArrayList<Integer> a1 = rp.get(rp.size() - 1);
                        if (a1.get(a1.size() - 1) == -1){
                            int l = rp.size();
                            rp.get(rp.size()-1).set(a1.size() - 1, type);

                        }
                    }
                }
                //
                else {
                    reTrack();
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                }
                if (Objects.equals(symbol, "LPARENT")) {
                    output.add(symbol + " " + token);
                    re = getOne();  //读下一个 然后进入常量定义
                    if (re < 0) {  //没有下一个 出错
                        return false;
                    }
                    FuncRParams(iname);

                    //判断调用参数是否合理
                    int l = rp.size();
                    if (flag == 0) {
                        ArrayList<Integer> fpt = Global.globalFuncTable.get(iname).parameterTable;
                        System.out.println(fpt.size());
                        for (int i : rp.get(l - 1)) {
                            System.out.println(i);
                        }
                        int psize = 0;
                        if (!rp.get(l-1).contains(-1)) {
                            psize = rp.get(l - 1).size();
                        }
                        if (fpt.size() != psize) {
                            System.out.println("函数参数个数不匹配！");
                            Global.error_output.set(Integer.parseInt(idenline), idenline + " " + "d");
                        } else {
                            for (int i = 0; i < fpt.size(); i++) {
                                if (!Objects.equals(fpt.get(i), rp.get(l - 1).get(i))) {
                                    System.out.println("函数参数类型不匹配！");
                                    Global.error_output.set(Integer.parseInt(idenline), idenline + " " + "e");
                                }
                            }
                        }
                    }
                    rp.remove(l - 1);

                    if (!Objects.equals(symbol, "RPARENT")){
                        System.out.println("UnaryExp中的func缺少)!");
                        Global.error_output.set(Integer.parseInt(line), line + " " + "j");
                    }
                    else {
                        output.add(symbol + " " + token);
                        re = getOne();  //读下一个 然后进入常量定义
                        if (re < 0) {  //没有下一个 出错
                            return false;
                        }
                    }
                    output.add("<UnaryExp>");
                    return true;
                }
            }
        }

        if (PrimaryExp()) {

        }else if (UnaryOp()) {
            if (!UnaryExp()) {
                return false;
            }
        } else {
            return false;
        }
        output.add("<UnaryExp>");
        return true;
    }
    // UnaryOp → '+' | '−' | '!'
    private static boolean UnaryOp() {
        if (Objects.equals(symbol, "PLUS")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
        }
        else if (Objects.equals(symbol, "MINU")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
        }
        else if (Objects.equals(symbol, "NOT")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
        }
        else {
            return false;
        }
        output.add("<UnaryOp>");
        return true;
    }

    //PrimaryExp → '(' Exp ')' | LVal | Number
    private static boolean PrimaryExp() {
        if (Objects.equals(symbol, "LPARENT")) {
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (!Exp()) {
                System.out.println("PrimaryExp 当中的Exp错误！");
            }
            if (!Objects.equals(symbol, "RPARENT")) {
                System.out.println("缺少右括号！");
                Global.error_output.set(Integer.parseInt(preline), preline + " " + "j");
            } else {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            }
        } else if (LVal()) {

        } else if (number()) {
            if (rp.size() != 0 ) {
                ArrayList<Integer> a1 = rp.get(rp.size() - 1);
                if (a1.get(a1.size() - 1) == -1) {
                    int l = rp.size();
                    rp.get(l-1).set(a1.size() - 1, 0);
                }
            }
        }
        else {
            return false;
        }
        output.add("<PrimaryExp>");
        return true;
    }

    //Number → IntConst
    public static boolean number() {
        if (Objects.equals(symbol, "INTCON")) {
            output.add(symbol + " " + token);
            getOne();
        } else {
            return false;
        }
        output.add("<Number>");
        return true;
    }

    //LVal → Ident {'[' Exp ']'}
    private static boolean LVal() {
        String iname;
        int lnum = 0;
        int flag = 0;
        String lvalline = "";
        if (Objects.equals(symbol, "IDENFR")) {
            iname = token;
            lvalline = line;
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (rp.size() != 0) {
                ArrayList<Integer> a1 = rp.get(rp.size() - 1);
                if (a1.get(a1.size() - 1) == -1) {
                    rp.get(rp.size()-1).set(a1.size() - 1, -2);
                    flag = 1;
                }
            }
        }
        else {
            return false;
        }

        if (Objects.equals(symbol, "LPARENT")) {
            return false;
        }
        while (Objects.equals(symbol, "LBRACK")) {
            lnum ++;
            output.add(symbol + " " + token);
            int re = getOne();  //读下一个
            if (re < 0) {  //没有下一个 出错
                return false;
            }
            if (!Exp()) {
                System.out.println("Exp in LVal wrong!");
                return false;
            }
            else if (Objects.equals(symbol, "RBRACK")) {
                output.add(symbol + " " + token);
                re = getOne();  //读下一个
                if (re < 0) {  //没有下一个 出错
                    return false;
                }
            }
            else if (!Objects.equals(symbol, "RBRACK")){
                System.out.println("LVal 缺少]!");
                Global.error_output.set(Integer.parseInt(line), line + " " + "k");
            }
        }
        int have = 0;
        int dnum = 0;
        for (HashMap<String, SymbolItem> map : localLayer) {
            if (map.containsKey(iname)) {
                have ++;
                dnum = map.get(iname).type;
                break;
            }
        }
        //局部符号表中没有
        if (have == 0) {
            if (Global.globalTable.containsKey(iname)) {
                if (rp.size() != 0 ) {
                    ArrayList<Integer> a1 = rp.get(rp.size() - 1);
                    if (a1.get(a1.size() - 1) == -2) {
                        int dnum1 = Global.globalTable.get(iname).type;
                        int l = rp.size();
                        if ((dnum1 == 1 && lnum == 1) ||
                                (dnum1 == 2 && lnum == 2)) {
                            rp.get(l-1).set(a1.size() - 1, 0);
                        }
                        else if (dnum1 == 2 && lnum == 1) {
                            rp.get(l-1).set(a1.size() - 1, 1);
                        }
                        else {
                            rp.get(l-1).set(a1.size() - 1, dnum1);
                        }
                    }
                }
            }
            else {
                Global.error_output.set(Integer.parseInt(lvalline), lvalline + " " + "c");
                System.out.println("使用未定义名字！");
            }
        }
        //局部符号表中有，而且是函数调用中的
        else {
            if (rp.size() != 0) {
                ArrayList<Integer> a1 = rp.get(rp.size() - 1);
                if (a1.get(a1.size() - 1) == -2 && flag == 1) {
                    int l = rp.size();
                    if ((dnum == 1 && lnum == 1) ||
                            (dnum == 2 && lnum == 2)) {
                        rp.get(l-1).set(a1.size() - 1, 0);
                    }
                    else if (dnum == 2 && lnum == 1) {
                        rp.get(l-1).set(a1.size() - 1, 1);
                    }
                    else {
                        rp.get(l-1).set(a1.size() - 1, dnum);
                    }
                }
            }
        }
        output.add("<LVal>");
        return true;
    }

    //Exp → AddExp
    private static boolean Exp() {
        if (!AddExp()) {
            return false;
        }
        output.add("<Exp>");
        return true;
    }


    public static int getOne() {
        index++;
        if (index >= Lexical.output.size()) {
            return -1;
        }
        preline = line;
        String s = Lexical.output.get(index);
        int space = s.indexOf(' ');
        int last = s.lastIndexOf(' ');
        symbol = s.substring(0, space);
        token =  s.substring(space+1, last);
        line = s.substring(last + 1);
//        System.out.println("This is: " + symbol + " and: " + token);
        return 1;
    }

    public static void reTrack() {
        index--;
        preline = line;
        String s = Lexical.output.get(index);
        int space = s.indexOf(' ');
        int last = s.lastIndexOf(' ');
        symbol = s.substring(0, space);
        token =  s.substring(space+1, last);
        line = s.substring(last + 1);
//        System.out.println(index + ":" + "error is: " + symbol + " and: " + token);
    }


}
