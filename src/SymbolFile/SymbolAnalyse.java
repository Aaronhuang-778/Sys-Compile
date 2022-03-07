package SymbolFile;

import AST.NodeType;
import AST.TreeNode;

import java.util.ArrayList;
import java.util.Objects;

public class SymbolAnalyse {
    public static SymTable symTable = new SymTable(null);
    public static SymTable funcSymTable = new SymTable(null);
    public static int inWhile = 0;
    public static int inVoid = 2;

    public static void StartAnalyse(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            NodeType nodeType = treeNode.nodeType;
            if (nodeType == NodeType.CONSTDECL) {
                ConstDecl(treeNode);
            }
            else if (nodeType == NodeType.VARDECL) {
                VarDecl(treeNode);
            }
            else if (nodeType == NodeType.FUNCDEF) {
                FuncDef(treeNode);
            }
            else if (nodeType == NodeType.MAINFUNCDEF) {
                MainFuncDef(treeNode);
            }
        }
    }

    public static void ConstDecl(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            NodeType nodeType = treeNode.nodeType;
            if (nodeType == NodeType.CONSTDEF) {
                ConstDef(treeNode);
            }
        }
    }

    public static void VarDecl(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            NodeType nodeType = treeNode.nodeType;
            if (nodeType == NodeType.VARDEF) {
                VarDef(treeNode);
            }
        }
    }
    //  FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public static void FuncDef(TreeNode topTree) {
        SymTable temp = CopySym(symTable);
        int needMake = 1;
        Symbol funcSym = new Symbol();
        // get funcName
        String name = topTree.children.get(1).token;
        funcSym.name = name;
        for (TreeNode treeNode : topTree.children) {
            NodeType nodeType = treeNode.nodeType;
            if (nodeType == NodeType.FUNCTYPE) {
                String type = topTree.children.get(0).children.get(0).token;
                if (Objects.equals(type, "VOID")) {
                    funcSym.returnType = SymType.VOID;
                }
                else {
                    funcSym.returnType = SymType.VAR;
                }
            }
            else if (nodeType == NodeType.FUNCFPARAMS) {
                needMake = 0;
                NewSymTable();
                funcSym.paramTypes = FuncFParams(treeNode);
            }
            else if (nodeType == NodeType.BLOCK) {
                funcSymTable.putSym(name, funcSym);
                if (needMake == 1) {
                    NewSymTable();
                }
                if (funcSym.returnType == SymType.VOID) {
                    inVoid = 1;
                }
                else {
                    inVoid = 0;
                }
                FuncBlock(treeNode, funcSym.returnType) ;
                inVoid = 2;
                symTable = CopySym(temp);
            }
        }
    }


    public static void MainFuncDef(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.BLOCK) {
                SymTable temp = CopySym(symTable);
                NewSymTable();
                FuncBlock(treeNode, SymType.VAR);
                symTable = CopySym(temp);
            }
        }
    }

    public static void ConstDef(TreeNode topTree) {
        Symbol varSymbol = new Symbol();
        int length = 0;
        // name
        varSymbol.name = topTree.children.get(0).token;

        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.CONSTEXP) {
                length ++;
            }
        }
        if (length == 0) {
            varSymbol.symType = SymType.CONST;
        }
        else if (length == 1) {
            varSymbol.symType = SymType.CONSTARRAY_1;
        }
        else if (length == 2) {
            varSymbol.symType = SymType.CONSTARRAY_2;
        }
        symTable.putSym(varSymbol.name, varSymbol);
    }

    public static void VarDef(TreeNode topTree) {
        Symbol varSymbol = new Symbol();
        int length = 0;
        // name
        varSymbol.name = topTree.children.get(0).token;

        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.CONSTEXP) {
                length ++;
            }
            else if (treeNode.nodeType == NodeType.INITVAL) {
                InitVal(treeNode);
            }
        }
        if (length == 0) {
            varSymbol.symType = SymType.VAR;
        }
        else if (length == 1) {
            varSymbol.symType = SymType.ARRAY_1;
        }
        else if (length == 2) {
            varSymbol.symType = SymType.ARRAY_2;
        }
        symTable.putSym(varSymbol.name, varSymbol);
    }

    private static ArrayList<SymType> FuncFParams(TreeNode topTree) {
        ArrayList<SymType> paramTypes = new ArrayList<>();
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.FUNCFPARAM) {
                paramTypes.add(FuncFParam(treeNode));
            }
        }
        return paramTypes;
    }

    public static void Block(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.CONSTDECL) {
                ConstDecl(treeNode);
            }
            else if (treeNode.nodeType == NodeType.VARDECL) {
                VarDecl(treeNode);
            }
            else if (treeNode.nodeType == NodeType.STMT) {
                Stmt(treeNode);
            }
        }
    }

    public static void FuncBlock(TreeNode topTree, SymType funcType) {
        SymType symType = SymType.OTHER;
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.CONSTDECL) {
                ConstDecl(treeNode);
            }
            else if (treeNode.nodeType == NodeType.VARDECL) {
                VarDecl(treeNode);
            }
            else if (treeNode.nodeType == NodeType.STMT) {
                symType = Stmt(treeNode);
            }
        }
    }

    public static void InitVal(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.EXP) {
                Exp(treeNode);
            }
            else if (treeNode.nodeType == NodeType.INITVAL) {
                InitVal(treeNode);
            }
        }
    }

    public static SymType FuncFParam(TreeNode topTree) {
        Symbol varSymbol = new Symbol();
        varSymbol.name = topTree.children.get(0).token;
        int fpSize = topTree.children.size();
        if (fpSize == 2) {
            varSymbol.symType = SymType.VAR;
        }
        else if (fpSize == 4) {
            varSymbol.symType = SymType.ARRAY_1;
        }
        else {
            varSymbol.symType = SymType.ARRAY_2;
        }

        symTable.putSym(varSymbol.name, varSymbol);
        if (fpSize == 2) {
           return SymType.VAR;
        }
        else if (fpSize == 4) {
            return SymType.ARRAY_1;
        }
        else {
            return SymType.ARRAY_2;
        }
    }

    public static SymType Stmt(TreeNode topTree) {
        ArrayList<TreeNode> treeNodes = topTree.children;
        NodeType nodeType = treeNodes.get(0).nodeType;
        String symbol = treeNodes.get(0).symbol;
        if (nodeType == NodeType.LVAL) {
            if (treeNodes.get(2).nodeType == NodeType.EXP) {
                Exp(treeNodes.get(2));
            }
        }
        else if (Objects.equals(symbol, "IFTK")) {
            for (TreeNode treeNode : treeNodes) {
                if (treeNode.nodeType == NodeType.COND) {
                    Cond(treeNode);
                }
                else if (treeNode.nodeType == NodeType.STMT) {
                    SymTable temp = CopySym(symTable);
                    NewSymTable();
                    Stmt(treeNode);
                    symTable = CopySym(temp);
                }
            }
        }
        else if (Objects.equals(symbol, "WHILETK")) {
            for (TreeNode treeNode : treeNodes) {
                if (treeNode.nodeType == NodeType.COND) {
                    Cond(treeNode);
                }
                else if (treeNode.nodeType == NodeType.STMT) {
                    SymTable temp = CopySym(symTable);
                    NewSymTable();
                    int temp1 = inWhile;
                    inWhile = 1;
                    Stmt(treeNode);
                    inWhile = temp1;
                    symTable = CopySym(temp);
                }
            }
        }
        else if (Objects.equals(symbol, "EXP")) {
            Exp(treeNodes.get(0));
        }
        else if (Objects.equals(symbol, "RETURNTK")) {
            if (treeNodes.size() == 2) {
                return  SymType.VOID;
            }
            else {
                SymType symType = Exp(treeNodes.get(1));
                if (symType == SymType.VAR) {
                    return SymType.VAR;
                }
                else {
                    return  SymType.OTHER;
                }
            }
        }
        else if (nodeType == NodeType.BLOCK) {
            SymTable temp = CopySym(symTable);
            NewSymTable();
            Block(treeNodes.get(0));
            symTable = CopySym(temp);
        }
        else if (Objects.equals(symbol, "PRINTFTK")) {
            int expNum = 0;
            int fExpNum = getExpNum(treeNodes.get(2));
            for (TreeNode treeNode : treeNodes) {
                if (treeNode.nodeType == NodeType.EXP) {
                    expNum ++;
                }
            }
        }
        else if (Objects.equals(symbol, "BREAKTK") ||
                Objects.equals(symbol, "CONTINUETK")) {

        }
        return SymType.VOID;
    }

    public static SymType Exp(TreeNode topTree) {
        return AddExp(topTree.children.get(0));
    }

    private static SymType AddExp(TreeNode topTree) {
        ArrayList<SymType> types = new ArrayList<>();
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.ADDEXP) {
                types.add(AddExp(treeNode));
            }
            else if (treeNode.nodeType == NodeType.MULEXP) {
                types.add(MulExp(treeNode));
            }
        }
        if (types.size() == 1) {
            return types.get(0);
        }
        return SymType.VAR;
    }

    public static SymType MulExp(TreeNode topTree) {
        ArrayList<SymType> types = new ArrayList<>();
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.UNARYEXP) {
                types.add(UnaryExp(treeNode));
            }
            else if (treeNode.nodeType == NodeType.MULEXP) {
                types.add(MulExp(treeNode));
            }
        }
        if (types.size() == 1) {
            return types.get(0);
        }
        return SymType.VAR;
    }

    //UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp
    public static SymType UnaryExp(TreeNode topTree) {
        TreeNode treeNode = topTree.children.get(0);
        if (treeNode.nodeType == NodeType.PRIMARYEXP) {
            return PrimaryExp(treeNode);
        }
        else if (treeNode.nodeType == NodeType.TERMINAL) {
            String name = treeNode.token;
            Symbol funcSym = funcSymTable.findSym(name);
            ArrayList<SymType> RparamTypes = new ArrayList<>();
            if (topTree.children.get(2).nodeType == NodeType.FUNCRPARAMS) {
                RparamTypes = FuncRParams(topTree.children.get(2));
            }
            ArrayList<SymType> FparamTypes = funcSym.paramTypes;
            return funcSym.returnType;
        }
        else if (treeNode.nodeType == NodeType.UNARYOP) {
            return UnaryExp(topTree.children.get(1));
        }
        return SymType.OTHER;
    }

    public static SymType PrimaryExp(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.EXP) {
                return Exp(treeNode);
            }
            else if (treeNode.nodeType == NodeType.LVAL) {
                return LVal(treeNode);
            }
            else if (treeNode.nodeType == NodeType.NUMBER) {
                return SymType.VAR;
            }
        }
        return SymType.VAR;
    }

    public static SymType LVal(TreeNode topTree) {
        int expNum = 0;
        String name = topTree.children.get(0).token;
        Symbol temp = symTable.findSym(name);
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.EXP) {
                expNum++;
                Exp(treeNode);
            }
        }
        SymType symType = temp.symType;
        if (symType == SymType.VAR || symType == SymType.CONST) {
            return symType;
        }
        else if (symType == SymType.ARRAY_1) {
            if (expNum == 1) return SymType.VAR;
            else return SymType.ARRAY_1;
        }
        else if (symType == SymType.CONSTARRAY_1) {
            if (expNum == 1) return SymType.CONST;
            else return SymType.CONSTARRAY_1;
        }
        else if (symType == SymType.ARRAY_2) {
            if (expNum == 0) return SymType.ARRAY_2;
            else if (expNum == 1)return SymType.ARRAY_1;
            else return SymType.VAR;
        }
        else if (symType == SymType.CONSTARRAY_2) {
            if (expNum == 0) return SymType.CONSTARRAY_2;
            else if (expNum == 1)return SymType.CONSTARRAY_1;
            else return SymType.CONST;
        }
        return SymType.OTHER;
    }

    public static void Cond(TreeNode topTree) {
        LOrExp(topTree.children.get(0));
    }

    public static ArrayList<SymType> FuncRParams(TreeNode topTree) {
        ArrayList<SymType> types = new ArrayList<>();
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.EXP) {
                SymType symType = Exp(treeNode);
                if (symType == SymType.CONST) {
                    symType = SymType.VAR;
                }
                types.add(symType);
            }
        }
        return types;
    }

    public static void LOrExp(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.LANDEXP) {
                LAndExp(treeNode);
            }
            else if (treeNode.nodeType == NodeType.LOREXP) {
                LOrExp(treeNode);
            }
        }
    }

    public static void LAndExp(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.LANDEXP) {
                LAndExp(treeNode);
            }
            else if (treeNode.nodeType == NodeType.EQEXP) {
                EqExp(treeNode);
            }
        }
    }

    public static void EqExp(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.RELEXP) {
                RelExp(treeNode);
            }
            else if (treeNode.nodeType == NodeType.EQEXP) {
                EqExp(treeNode);
            }
        }
    }

    public static void RelExp(TreeNode topTree) {
        for (TreeNode treeNode : topTree.children) {
            if (treeNode.nodeType == NodeType.RELEXP) {
                RelExp(treeNode);
            }
            else if (treeNode.nodeType == NodeType.ADDEXP) {
                AddExp(treeNode);
            }
        }
    }

    public static int getExpNum(TreeNode treeNode) {
        String fString = treeNode.token;
        int p = 0;
        int num = 0;
        while (p < fString.length() - 1) {
            if (fString.charAt(p) == '%' && fString.charAt(p+1) == 'd') {
                num ++;
            }
            p ++;
        }
        return num;
    }


    public static void NewSymTable() {
        symTable = new SymTable(symTable);
    }

    private static SymTable CopySym(SymTable s1) {
        SymTable newSym = new SymTable();
        newSym.fatherTable = s1.fatherTable;
        newSym.symTable.putAll(s1.symTable);
        return newSym;
    }
}
