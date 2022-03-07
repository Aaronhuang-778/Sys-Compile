package AST;

import java.util.ArrayList;
import java.util.Objects;

public class MakeTree {
    public static int index = -1;
    public static String line = "";
    public static String preline = "";
    public static String symbol = "";
    public static String token = "";
    public static int wrongExp = 0;
    public static ArrayList<String> lexicalOutput = new ArrayList<>();


    public static TreeNode ParseTree(ArrayList<String> lexical) {
        lexicalOutput = lexical;
        //CompUnit
        return  CompUnit();
    }

    private static TreeNode CompUnit() {
        TreeNode compUnit = new TreeNode(NodeType.COMPUNIT);
        // {Decl}
        getOne();
        while (true) {
            if (Objects.equals(symbol, "CONSTTK")) {
                compUnit.addTree(ConstDecl());
                continue;
            }
            if (Objects.equals(symbol, "INTTK")) {
                getOne();
                getOne();
                if (Objects.equals(symbol, "LPARENT")) {
                    reTrack();
                    reTrack();
                } else {
                    reTrack();
                    reTrack();
                    compUnit.addTree(VarDecl());
                    continue;
                }
            }
            break;
        }
        // {FuncDef}
        while (true) {
            if (Objects.equals(symbol, "VOIDTK") || Objects.equals(symbol, "INTTK")) {
                getOne();
                if (Objects.equals(symbol, "IDENFR")) {
                    getOne();
                    if (!Objects.equals(symbol, "LPARENT")) {
                        reTrack();
                        reTrack();
                    } else {
                        reTrack();
                        reTrack();
                        compUnit.addTree(FuncDef());
                    }
                    continue;
                }
                reTrack();
                break;
            }
        }

        compUnit.addTree(MainFuncDef());
        return compUnit;
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private static TreeNode ConstDecl() {
        TreeNode constDecl = new TreeNode(NodeType.CONSTDECL);
        //const
        constDecl.addTree(Terminal());
        //Btype
        constDecl.addTree(Terminal());
        //ConstDef
        constDecl.addTree(ConstDef());
        // { ',' ConstDef } ';'
        while (true) {
            if (Objects.equals(symbol, "COMMA")) {
                //','
                constDecl.addTree(Terminal());
                // ConstDef
                constDecl.addTree(ConstDef());
                continue;
            }
            break;
        }
        if (Objects.equals(symbol, "SEMICN")) {
            //';'
            constDecl.addTree(Terminal());
        }
        return constDecl;
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    private static TreeNode VarDecl() {
        TreeNode varDecl = new TreeNode(NodeType.VARDECL);
        //Btype
        varDecl.addTree(Terminal());
        //ConstDef
        varDecl.addTree(VarDef());
        // { ',' VarDef } ';'
        while (true) {
            if (Objects.equals(symbol, "COMMA")) {
                //','
                varDecl.addTree(Terminal());
                // ConstDef
                varDecl.addTree(VarDef());
                continue;
            }
            break;
        }
        //';'
        varDecl.addTree(Terminal());
        return varDecl;
    }

    //  FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public static TreeNode FuncDef() {
        TreeNode funcDef = new TreeNode(NodeType.FUNCDEF);
        //FuncType
        funcDef.addTree(FuncType());
        //Ident
        funcDef.addTree(Terminal());
        //'('
        funcDef.addTree(Terminal());
        // FuncFParams
        if (Objects.equals(symbol, "INTTK")) {
            funcDef.addTree(FuncFParams());
        }
        //')'
        funcDef.addTree(Terminal());
        //Block
        funcDef.addTree(Block());

        return funcDef;
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    public static TreeNode MainFuncDef() {
        TreeNode mainFuncDef = new TreeNode(NodeType.MAINFUNCDEF);
        // int
        mainFuncDef.addTree(Terminal());
        //'main'
        mainFuncDef.addTree(Terminal());
        //(
        mainFuncDef.addTree(Terminal());
        //)
        mainFuncDef.addTree(Terminal());
        //Block
        mainFuncDef.addTree(Block());
        return mainFuncDef;
    }
    //ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    public static TreeNode ConstDef() {
        TreeNode constDef = new TreeNode(NodeType.CONSTDEF);
        // Ident
        constDef.addTree(Terminal());
        // { '[' ConstExp ']' }
        while (true) {
            if (Objects.equals(symbol, "LBRACK")) {
                //'['
                constDef.addTree(Terminal());
                //ConstExp
                constDef.addTree(ConstExp());
                // '['
                constDef.addTree(Terminal());
                continue;
            }
            break;
        }
        // '='
        constDef.addTree(Terminal());
        // ConstInitVal
        constDef.addTree(ConstInitVal());
        return constDef;
    }

    //VarDef → Ident { '[' ConstExp ']' }
    // | Ident { '[' ConstExp ']' } '=' InitVal
    // 包含普通变量、⼀维数组、⼆维数组定义
    public static TreeNode VarDef() {
        TreeNode varDef = new TreeNode(NodeType.VARDEF);
        //Ident
        varDef.addTree(Terminal());
        //{ '[' ConstExp ']' }
        while (true) {
            if (Objects.equals(symbol, "LBRACK")) {
                // '['
                varDef.addTree(Terminal());
                // ConstExp
                varDef.addTree(ConstExp());
                //']'
                varDef.addTree(Terminal());
                continue;
            }
            break;
        }
        if (Objects.equals(symbol, "ASSIGN")) {
            // '='
            varDef.addTree(Terminal());
            //InitVal
            varDef.addTree(InitVal());
        }
        return varDef;
    }

    //FuncFParams → FuncFParam { ',' FuncFParam }
    public static TreeNode FuncFParams() {
       TreeNode funcFParam = new TreeNode(NodeType.FUNCFPARAMS);
       //FuncFParam
        funcFParam.addTree(FuncFParam());
        while (true) {
            if (Objects.equals(symbol, "COMMA")) {
                // ','
                funcFParam.addTree(Terminal());
                // FuncFParam
                funcFParam.addTree(FuncFParam());
                continue;
            }
            break;
        }
        return funcFParam;
    }

    //Block → '{' { BlockItem } '}'
    public static TreeNode Block() {
        TreeNode block = new TreeNode(NodeType.BLOCK);
        //'{'
        block.addTree(Terminal());
        //{ BlockItem }
        while (true) {
            if (!Objects.equals(symbol, "RBRACE")) {
                if (Objects.equals(symbol, "CONSTTK")) {
                    //Decl->ConstDecl
                    block.addTree(ConstDecl());
                    continue;
                }
                else if (Objects.equals(symbol, "INTTK")) {
                    block.addTree(VarDecl());
                    continue;
                }
                else {
                    block.addTree(Stmt());
                    continue;
                }
            }
            break;
        }
        //'}'
        block.addTree(Terminal());
        return block;
    }

    public static TreeNode ConstExp() {
        TreeNode constExp = new TreeNode(NodeType.CONSTEXP);
        constExp.addTree(AddExp());
        return constExp;
    }

    //ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'

    public static TreeNode ConstInitVal() {
        TreeNode constInitVal = new TreeNode(NodeType.CONSTINITVAL);
        if (Objects.equals(symbol, "LBRACE")) {
            // '{'
            constInitVal.addTree(Terminal());
            // ConstInitVal
            if (!Objects.equals(symbol, "RBRACE")) {
                constInitVal.addTree(ConstInitVal());
                while (true) {
                    if (Objects.equals(symbol, "COMMA")) {
                        // ','
                        constInitVal.addTree(Terminal());
                        // ConstInitVal
                        constInitVal.addTree(ConstInitVal());
                        continue;
                    }
                    break;
                }
            }
            // '}'
            constInitVal.addTree(Terminal());
        }
        else {
            // ConstExp
            constInitVal.addTree(ConstExp());
        }
        return constInitVal;
    }

    //InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    public static TreeNode InitVal() {
        TreeNode initVal = new TreeNode(NodeType.INITVAL);
        if (Objects.equals(symbol, "LBRACE")) {
            // '{'
            initVal.addTree(Terminal());
            // InitVal
            if (!Objects.equals(symbol, "RBRACE")) {
                initVal.addTree(InitVal());
                while (true) {
                    if (Objects.equals(symbol, "COMMA")) {
                        // ','
                        initVal.addTree(Terminal());
                        // ConstInitVal
                        initVal.addTree(InitVal());
                        continue;
                    }
                    break;
                }
            }
            // '}'
            initVal.addTree(Terminal());
        }
        else {
            // ConstExp
            initVal.addTree(Exp());
        }
        return initVal;
    }

    //FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    public static TreeNode FuncFParam() {
        TreeNode funcFParam = new TreeNode(NodeType.FUNCFPARAM);
        //BType
        funcFParam.addTree(Terminal());
        //Ident
        funcFParam.addTree(Terminal());

        if (Objects.equals(symbol, "LBRACK")) {
            //['[' ']' { '[' ConstExp ']' }]
            //'['
            funcFParam.addTree(Terminal());
            //']'
            funcFParam.addTree(Terminal());
            //{ '[' ConstExp ']' }
            while (true) {
                if (Objects.equals(symbol, "LBRACK")) {
                    // '['
                    funcFParam.addTree(Terminal());
                    //ConstExp
                    funcFParam.addTree(ConstExp());
                    //']'
                    funcFParam.addTree(Terminal());
                    continue;
                }
                break;
            }
        }
        return funcFParam;
    }

    //AddExp→ MulExp { ('+' | '−') MulExp}
    public static TreeNode  AddExp() {
        TreeNode addExp = new TreeNode(NodeType.ADDEXP);
        ArrayList<TreeNode> temp = new ArrayList<>();
        //MulExp
        temp.add(MulExp());
        // {('+' | '−') MulExp}
        while (true) {
            if (Objects.equals(symbol, "PLUS") ||
                    Objects.equals(symbol, "MINU")) {
                // '+'
                temp.add(Terminal());
                //MulExp
                temp.add(MulExp());
                continue;
            }
            break;
        }
        int calPos = 1;
        int length = temp.size();
        int cur = 0;
        while (calPos <= length - 2) {
            TreeNode addExp_temp = new TreeNode(NodeType.ADDEXP);
            while (cur < calPos) {
                addExp_temp.addTree(temp.get(cur));
                cur ++;
            }
            cur --;
            temp.set(cur, addExp_temp);
            calPos += 2;
        }
        if (length >= 3) {
            addExp.addTree(temp.get(length - 3));
            addExp.addTree(temp.get(length - 2));
            addExp.addTree(temp.get(length - 1));
        }else {
            addExp.addTree(temp.get(0));
        }
        return addExp;
    }

    //Exp → AddExp
    public static TreeNode Exp() {
        TreeNode exp = new TreeNode(NodeType.EXP);
        // AddExp
        exp.addTree(AddExp());
        return exp;
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
    public static TreeNode Stmt() {
        TreeNode stmt = new TreeNode(NodeType.STMT);
        if (Objects.equals(symbol, "LBRACE")) {
            //Block
            stmt.addTree(Block());
        }
        else if (Objects.equals(symbol, "IFTK")) {
            //'if'
            stmt.addTree(Terminal());
            //'('
            stmt.addTree(Terminal());
            //Cond
            stmt.addTree(Cond());
            //')'
            stmt.addTree(Terminal());
            //Stmt
            stmt.addTree(Stmt());
            if (Objects.equals(symbol, "ELSETK")) {
                // 'else'
                stmt.addTree(Terminal());
                //Stmt
                stmt.addTree(Stmt());
            }
        }
        else if (Objects.equals(symbol, "WHILETK")) {
            //'while'
            stmt.addTree(Terminal());
            //'('
            stmt.addTree(Terminal());
            //Cond
            stmt.addTree(Cond());
            //')'
            stmt.addTree(Terminal());
            //Stmt
            stmt.addTree(Stmt());
        }
        else if (Objects.equals(symbol, "BREAKTK")) {
            //'break'
            stmt.addTree(Terminal());
            //';'
            stmt.addTree(Terminal());
        }
        else if (Objects.equals(symbol, "CONTINUETK")) {
            //'continue'
            stmt.addTree(Terminal());
            //';'
            stmt.addTree(Terminal());
        }
        else if (Objects.equals(symbol, "RETURNTK")) {
            //'return'
            stmt.addTree(Terminal());
            int preIndex = index;
            //Exp
            TreeNode treeNode = Exp();
            if (wrongExp == 1) {
                wrongExp = 0;
                index = preIndex;
                stmt.addTree(Terminal());
                return stmt;
            }
            // add exp to tree
            stmt.addTree(treeNode);
            //';'
            stmt.addTree(Terminal());
        }
        else if (Objects.equals(symbol, "PRINTFTK")) {
            //'printf'
            stmt.addTree(Terminal());
            // '('
            stmt.addTree(Terminal());
            // FormatString
            stmt.addTree(Terminal());
            //{',' Exp}
            while (true) {
                if (Objects.equals(symbol, "COMMA")) {
                    //','
                    stmt.addTree(Terminal());
                    //Exp
                    stmt.addTree(Exp());
                    continue;
                }
                break;
            }
            // ')'
            stmt.addTree(Terminal());
            // ';'
            stmt.addTree(Terminal());
        }
        else if (Objects.equals(symbol, "IDENFR")) {
            //LVal '=' Exp ';' | LVal '=' 'getint' '(' ')' ';' | [Exp] ';'
            int preIndex = index;
            //Lval
            int tempWrong = wrongExp;
            TreeNode temp = LVal();
            wrongExp = tempWrong;
            if (!Objects.equals(symbol, "ASSIGN")) {
                index = preIndex - 1;
                getOne();
                if (!Objects.equals(symbol, "SEMICN")) {
                    stmt.addTree(Exp());
                }
                // ';'
                stmt.addTree(Terminal());
                return stmt;
            }
            stmt.addTree(temp);
            //'='
            stmt.addTree(Terminal());
            if (Objects.equals(symbol, "GETINTTK")) {
                //'getint'
                stmt.addTree(Terminal());
                //'('
                stmt.addTree(Terminal());
                //')'
                stmt.addTree(Terminal());
                //';'
                stmt.addTree(Terminal());
            }
            else {
                stmt.addTree(Exp());
                //';'
                stmt.addTree(Terminal());
            }
        }
        else {
            if (!Objects.equals(symbol, "SEMICN")) {
                //Exp
                stmt.addTree(Exp());
            }
            //';'
            stmt.addTree(Terminal());
        }
        return stmt;
    }

    //MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    //MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
    public static TreeNode MulExp() {
        TreeNode mulExp = new TreeNode(NodeType.MULEXP);
        ArrayList<TreeNode> temp = new ArrayList<>();
        //UnaryExp
        temp.add(UnaryExp());
        // { ('*' | '/' | '%') UnaryExp }
        while (true) {
            if (Objects.equals(symbol, "MULT") ||
                    Objects.equals(symbol, "DIV") ||
                    Objects.equals(symbol, "MOD")) {
                temp.add(Terminal());
                //UnaryExp
                temp.add(UnaryExp());
                continue;
            }
            break;
        }
        int calPos = 1, length = temp.size(), cur = 0;
        while (calPos <= length-2) {
            TreeNode mulExp_temp = new TreeNode(NodeType.MULEXP);
            while (cur < calPos) {
                mulExp_temp.addTree(temp.get(cur));
                cur ++;
            }
            cur--;
            temp.set(cur, mulExp_temp);
            calPos+=2;
        }
        if (length >= 3) {
            mulExp.addTree(temp.get(length - 3));
            mulExp.addTree(temp.get(length - 2));
            mulExp.addTree(temp.get(length - 1));
        }
        else {
            mulExp.addTree(temp.get(0));
        }
        return mulExp;
    }

    //UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp
    public static TreeNode UnaryExp() {
        TreeNode unaryExp = new TreeNode(NodeType.UNARYEXP);
        getOne();
        String symbol1 = symbol;
        reTrack();
        if (Objects.equals(symbol, "IDENFR") && Objects.equals(symbol1, "LPARENT")) {
                unaryExp.addTree(Terminal());
                unaryExp.addTree(Terminal());
                if (!Objects.equals(symbol, "RPARENT")) {
                    unaryExp.addTree(FuncRParams());
                }
                unaryExp.addTree(Terminal());
        }
        else if (Objects.equals(symbol, "PLUS") ||
                Objects.equals(symbol, "MINU") ||
                Objects.equals(symbol, "NOT")) {
            unaryExp.addTree(UnaryOp());
            unaryExp.addTree(UnaryExp());
        }
        else {
            unaryExp.addTree(PrimaryExp());
        }
        return unaryExp;
    }

    //PrimaryExp → '(' Exp ')' | LVal | Number
    public static TreeNode PrimaryExp() {
        TreeNode primaryExp = new TreeNode(NodeType.PRIMARYEXP);
        if (Objects.equals(symbol, "LPARENT")) {
            primaryExp.addTree(Terminal());
            primaryExp.addTree(Exp());
            primaryExp.addTree(Terminal());
        }
        else if (Objects.equals(symbol, "IDENFR")) {
            primaryExp.addTree(LVal());
        }
        else if (Objects.equals(symbol, "INTCON")) {
            primaryExp.addTree(Number());
        }
        else {
            wrongExp = 1;
        }
        return primaryExp;
    }

    // FuncRParams → Exp { ',' Exp }
    public static TreeNode FuncRParams() {
        TreeNode funcRParams = new TreeNode(NodeType.FUNCRPARAMS);
        //Exp
        funcRParams.addTree(Exp());
        while (true) {
            if (Objects.equals(symbol, "COMMA")) {
                funcRParams.addTree(Terminal());
                funcRParams.addTree(Exp());
                continue;
            }
            break;
        }
        return funcRParams;
    }

    public static TreeNode Cond(){
        TreeNode cond = new TreeNode(NodeType.COND);
        cond.addTree(LOrExp());
        return cond;
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    // LOrExp → LAndExp {'||' LAndExp}
    public static TreeNode LOrExp() {
        TreeNode lorExp = new TreeNode(NodeType.LOREXP);
        ArrayList<TreeNode> temp = new ArrayList<>();
        temp.add(LAndExp());
        while (true) {
            if (Objects.equals(symbol, "OR")) {
                temp.add(Terminal());
                temp.add(LAndExp());
                continue;
            }
            break;
        }
        int calPos = 1, length = temp.size(), cur = 0;
        while (calPos <= length - 2) {
            TreeNode LOrExp_temp = new TreeNode(NodeType.LOREXP);
            while (cur < calPos) {
                LOrExp_temp.addTree(temp.get(cur));
                cur ++;
            }
            cur --;
            temp.set(cur, LOrExp_temp);
            calPos += 2;
        }
        if (length >= 3) {
            lorExp.addTree(temp.get(length - 3));
            lorExp.addTree(temp.get(length - 2));
            lorExp.addTree(temp.get(length - 1));
        }
        else {
            lorExp.addTree(temp.get(0));
        }
        return lorExp;
    }

    // LAndExp → EqExp | LAndExp '&&' EqExp
    // LAndExp → EqExp {'&&' EqExp}
    public static TreeNode LAndExp() {
        TreeNode landExp = new TreeNode(NodeType.LANDEXP);
        ArrayList<TreeNode> temp = new ArrayList<>();
        temp.add(EqExp());
        while (true) {
            if (Objects.equals(symbol, "AND")) {
                temp.add(Terminal());
                temp.add(EqExp());
                continue;
            }
            break;
        }
        int calPos = 1, length = temp.size(), cur = 0;
        while (calPos <= length - 2) {
            TreeNode LandExp_temp = new TreeNode(NodeType.LANDEXP);
            while (cur < calPos) {
                LandExp_temp.addTree(temp.get(cur));
                cur ++;
            }
            cur --;
            temp.set(cur, LandExp_temp);
            calPos += 2;
        }
        if (length >= 3) {
            landExp.addTree(temp.get(length - 3));
            landExp.addTree(temp.get(length - 2));
            landExp.addTree(temp.get(length - 1));
        }
        else {
            landExp.addTree(temp.get(0));
        }
        return landExp;
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    // EqExp → RelExp { ('==' | '!=') RelExp }
    public static TreeNode EqExp() {
        TreeNode eqExp = new TreeNode(NodeType.EQEXP);
        ArrayList<TreeNode> temp = new ArrayList<>();
        temp.add(RealExp());
        while (true) {
            if (Objects.equals(symbol, "EQL") ||
                Objects.equals(symbol, "NEQ")) {
                temp.add(Terminal());
                temp.add(RealExp());
                continue;
            }
            break;
        }
        int calPos = 1, length = temp.size(), cur = 0;
        while (calPos <= length - 2) {
            TreeNode eqExp_temp = new TreeNode(NodeType.EQEXP);
            while (cur < calPos) {
                eqExp_temp.addTree(temp.get(cur));
                cur ++;
            }
            cur --;
            temp.set(cur, eqExp_temp);
            calPos += 2;
        }
        if (length >= 3) {
            eqExp.addTree(temp.get(length - 3));
            eqExp.addTree(temp.get(length - 2));
            eqExp.addTree(temp.get(length - 1));
        }
        else {
            eqExp.addTree(temp.get(0));
        }
        return eqExp;
    }

    public static TreeNode RealExp() {
        TreeNode relExp = new TreeNode(NodeType.RELEXP);
        ArrayList<TreeNode> temp = new ArrayList<>();
        temp.add(AddExp());
        while (true) {
            if (Objects.equals(symbol, "LSS") ||
                    Objects.equals(symbol, "LEQ") ||
                    Objects.equals(symbol, "GRE") ||
                    Objects.equals(symbol, "GEQ")) {
                temp.add(Terminal());
                temp.add(AddExp());
                continue;
            }
            break;
        }
        int calPos = 1, length = temp.size(), cur = 0;
        while (calPos <= length - 2) {
            TreeNode RelExp_temp = new TreeNode(NodeType.RELEXP);
            while (cur < calPos) {
                RelExp_temp.addTree(temp.get(cur));
                cur ++;
            }
            cur --;
            temp.set(cur, RelExp_temp);
            calPos += 2;
        }
        if (length >= 3) {
            relExp.addTree(temp.get(length - 3));
            relExp.addTree(temp.get(length - 2));
            relExp.addTree(temp.get(length - 1));
        }
        else {
            relExp.addTree(temp.get(0));
        }
        return relExp;
    }

    //LVal → Ident {'[' Exp ']'}
    public static TreeNode LVal() {
        TreeNode lVal = new TreeNode(NodeType.LVAL);
        // Ident
        lVal.addTree(Terminal());
        while (true) {
            if (Objects.equals(symbol, "LBRACK")) {
                // '['
                lVal.addTree(Terminal());
                //Exp
                lVal.addTree(Exp());
                //']'
                lVal.addTree(Terminal());
                continue;
            }
            break;
        }
        return lVal;
    }

    // UnaryOp → '+' | '−' | '!'
    public static TreeNode UnaryOp() {
        TreeNode unaryOp = new TreeNode(NodeType.UNARYOP);
        //'+' | '−' | '!'
        unaryOp.addTree(Terminal());
        return unaryOp;
    }

    public static TreeNode FuncType() {
        TreeNode funcType = new TreeNode(NodeType.FUNCTYPE);
        //'void' | 'int'
        funcType.addTree(Terminal());
        return funcType;
    }

    public static TreeNode Number() {
        TreeNode number = new TreeNode(NodeType.NUMBER);
        number.addTree(Terminal());
        return number;
    }

    public static int getOne() {
        index++;

        if (index >= lexicalOutput.size()) {
            return -1;
        }
        preline = line;
        String s = lexicalOutput.get(index);
        int space = s.indexOf(' ');
        int last = s.lastIndexOf(' ');
        symbol = s.substring(0, space);
        token =  s.substring(space+1, last);
        line = s.substring(last + 1);
        return 1;
    }

    public static void reTrack() {
        index--;
        preline = line;
        String s = lexicalOutput.get(index);
        int space = s.indexOf(' ');
        int last = s.lastIndexOf(' ');
        symbol = s.substring(0, space);
        token =  s.substring(space+1, last);
        line = s.substring(last + 1);
    }

    public static TreeNode Terminal() {
        TreeNode terminal = new TreeNode(NodeType.TERMINAL);
        terminal.symbol = symbol;
        terminal.token = token;
        getOne();
        return terminal;
    }
}
