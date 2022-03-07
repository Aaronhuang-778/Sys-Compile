package LlvmIR;

import AST.NodeType;
import AST.TreeNode;
import SymbolFile.SymTable;
import SymbolFile.SymType;
import SymbolFile.Symbol;


import java.util.ArrayList;
import java.util.Objects;

public class LLvmIR {
    public static TreeNode TopTree;
    public static SymTable symTable;
    public static SymTable funcSymTable;
    public static ArrayList<MidCode> midCodes = new ArrayList<>();
    public static int curVar = 0;
    public static int label = 0;
    public static int array = 0;
    public static boolean isGlobal = false;
    public static boolean needVar = false;
    public static ArrayList<Integer> labels = new ArrayList<>();
    public static ArrayList<String> data = new ArrayList<>();
    public static ArrayList<String> strList = new ArrayList<>();
    public static int print = 0;
    public static int globalVar = 0;

    public static void StartMakeIR(TreeNode head) {
        symTable = new SymTable(null);
        funcSymTable = new SymTable(null);
        TopTree = head;
        CompUnit(TopTree);
    }

    private static void CompUnit(TreeNode head) {
        isGlobal = true;
        for (TreeNode treeNode : head.children) {
            if (treeNode.nodeType == NodeType.CONSTDECL) {
                ConstDecl(treeNode);
            }
            else if (treeNode.nodeType == NodeType.VARDECL) {
                VarDecl(treeNode);
            }
            else if (treeNode.nodeType == NodeType.FUNCDEF)  {
                isGlobal = false;
                FuncDef(treeNode);
            }
            else {
                isGlobal = false;
                MainFuncDef(treeNode);
            }
        }
    }

    private static void ConstDecl(TreeNode head) {
        for (TreeNode treeNode : head.children) {
            NodeType nodeType = treeNode.nodeType;
            if (nodeType == NodeType.CONSTDEF) {
                if (isGlobal) GlobalConst(treeNode);
                else ConstDef(treeNode);
            }
        }
    }

    private static void VarDecl(TreeNode head) {
        for (TreeNode treeNode : head.children) {
            NodeType nodeType = treeNode.nodeType;
            if (nodeType == NodeType.VARDEF) {
                if (isGlobal) GlobalVar(treeNode);
                else VarDef(treeNode);
            }
        }
    }

    private static void GlobalConst(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;
        String name = treeNodes.get(0).token;
        Symbol symbol = new Symbol(name);
        for (TreeNode treeNode : treeNodes) {
            ArrayList<MidItem> items = ConstInitVal(treeNode);
            if (treeNode.nodeType == NodeType.CONSTINITVAL) {
                //单个常量
                if (treeNodes.size() == 3) {
                    symbol.symType = SymType.CONST;
                    symbol.label = "GLOBALVAR" + globalVar;
                    globalVar ++;
                    symbol.isGlobal = true;
                    symTable.putSym(name, symbol);;

                    data.add(symbol.label + ": .word " + items.get(items.size() - 1).num + "\n");
                }
                //一维数组
                else if (treeNodes.size() == 6) {
                    symbol.symType = SymType.CONSTARRAY_1;
                    symbol.var = curVar;
                    symbol.isGlobal = true;
                    symbol.label = "ARRAY" + array;
                    array ++;
                    symTable.putSym(name, symbol);
                    String mips_data = symbol.label + ": .word ";
                    for (int i = 0; i < items.size(); i++) {
                        mips_data += items.get(i).num;
                        if (i != items.size() - 1) mips_data += ", ";
                        else mips_data += "\n";
                    }
                    data.add(mips_data);
                }
                //二维数组
                else if (treeNodes.size() == 9) {
                    symbol.symType = SymType.CONSTARRAY_2;
                    MidItem num1 = ConstExp(treeNodes.get(5));
                    symbol.isGlobal = true;
                    symbol.midItem = num1;
                    symbol.label = "ARRAY" + array;
                    array ++;
                    symTable.putSym(name, symbol);
                    String mips_data = symbol.label + ": .word ";
                    for (int i = 0; i < items.size(); i++) {
                        mips_data += items.get(i).num;
                        if (i != items.size() - 1) mips_data += ", ";
                        else mips_data += "\n";
                    }
                    data.add(mips_data);
                }
            }
        }
    }

    private static void ConstDef(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;
        Symbol symbol = new Symbol();
        String name = treeNodes.get(0).token;
        symbol.name = name;

        MidItem item1 = new MidItem();
        // *4
        MidItem mul4 = new MidItem();
        mul4.num = 4;
        mul4.midType = MidType.NUM;
        for (TreeNode treeNode : treeNodes) {
            //输入结果
            ArrayList<MidItem> items = ConstInitVal(treeNode);
            if (treeNode.nodeType == NodeType.CONSTINITVAL) {
                //单个常量
                if (treeNodes.size() == 3) {
                    symbol.symType = SymType.CONST;
                    symbol.var = curVar;
                    symTable.putSym(name, symbol);
                    MidCode midCode = new MidCode(MidOp.ASSIGN);
                    item1.midType = MidType.VAR;
                    item1.var = curVar;
                    curVar ++;
                    midCode.insertItem(item1, 1);
                    midCode.insertItem(items.get(0), 2);
                    midCodes.add(midCode);
                }
                //一维数组
                else if (treeNodes.size() == 6) {
                    symbol.symType = SymType.CONSTARRAY_1;
                    MidItem offset = ConstExp(treeNodes.get(2));
                    symbol.var = curVar;
                    //地址指针
                    item1.midType = MidType.PTR;
                    item1.var = curVar;
                    curVar ++;
                    symTable.putSym(name, symbol);
                    //物理地址 * 4
                    MidItem phOffset = new MidItem();
                    phOffset.midType = MidType.NUM;
                    phOffset.num = offset.num * 4;
                    //数组地址分配
                    MidCode midCode = new MidCode(MidOp.ALLOC);
                    //为其进行alloc的中间代码
                    midCode.insertItem(item1, 1);
                    midCode.insertItem(phOffset, 2);
                    midCodes.add(midCode);
                    //按照地址进行赋值
                    int index = 0;
                    for (MidItem item : items) {
                        MidCode temp = new MidCode(MidOp.STORE);
                        MidItem e = new MidItem();
                        e.midType = MidType.NUM;
                        e.num = index;
                        index += 4;
                        temp.insertItem(item, 3);
                        temp.insertItem(e, 2);
                        temp.insertItem(item1, 1);
                        midCodes.add(temp);
                    }
                }
                //二维数组
                else if (treeNodes.size() == 9) {
                    symbol.symType = SymType.CONSTARRAY_2;
                    MidItem num1 = ConstExp(treeNodes.get(2));
                    MidItem num2 = ConstExp(treeNodes.get(5));
                    //计算数组容量

                    MidItem phOffset = new MidItem();
                    phOffset.midType = MidType.NUM;
                    phOffset.num = num1.num * num2.num * 4;
                    //符号表管理
                    symbol.var = curVar;
                    symbol.midItem = num2;
                    symTable.putSym(name, symbol);
                    //数组地址分配
                    MidCode midCode = new MidCode(MidOp.ALLOC);
                    //地址指针
                    item1.midType = MidType.PTR;
                    item1.var = curVar;
                    curVar ++;
                    midCode.insertItem(item1, 1);
                    midCode.insertItem(phOffset, 2);
                    midCodes.add(midCode);
                    int index = 0;
                    for (MidItem midItem : items) {
                        MidCode temp = new MidCode(MidOp.STORE);
                        MidItem e = new MidItem();
                        e.midType = MidType.NUM;
                        e.num = index;
                        index += 4;
                        temp.insertItem(midItem, 3);
                        temp.insertItem(e, 2);
                        temp.insertItem(item1, 1);
                        midCodes.add(temp);
                    }
                }
            }
        }
    }

    private static ArrayList<MidItem> ConstInitVal(TreeNode head) {
        ArrayList<MidItem> items = new ArrayList<>();
        for (TreeNode treeNode : head.children) {
            if (treeNode.nodeType == NodeType.CONSTINITVAL) {
                items.addAll(ConstInitVal(treeNode));
            }
            else if (treeNode.nodeType == NodeType.CONSTEXP) {
                MidItem item = ConstExp(treeNode);
                items.add(item);
            }
        }
        return items;
    }

    private static void GlobalVar(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;
        String name = treeNodes.get(0).token;
        Symbol symbol = new Symbol(name);
        ArrayList<MidItem> items = InitVal(treeNodes.get(treeNodes.size() - 1));
        if (treeNodes.size() == 1) {
            symbol.symType = SymType.VAR;
            symbol.label = "GLOBALVAR" + globalVar;
            symbol.isGlobal = true;
            globalVar ++;
            symTable.putSym(name, symbol);

            data.add(symbol.label + ": .word 0\n");
        }
        else if (treeNodes.size() == 3) {
            symbol.symType = SymType.VAR;
            symbol.label = "GLOBALVAR" + globalVar;
            globalVar ++;
            symbol.isGlobal = true;
            symTable.putSym(name, symbol);

            data.add(symbol.label + ": .word 0\n");

            MidCode la = new MidCode(MidOp.LA);
            MidItem addr = new MidItem();
            addr.midType = MidType.PTR;
            addr.var = curVar;
            curVar ++;
            la.insertItem(addr, 1);
            la.label = symbol.label;
            midCodes.add(la);

            MidCode midCode = new MidCode(MidOp.ASSIGN);
            midCode.insertItem(addr, 1);
            midCode.insertItem(items.get(0), 2);
            midCodes.add(midCode);
        }
        else if (treeNodes.size() == 4) {
            MidItem length = ConstExp(treeNodes.get(2));
            symbol.symType = SymType.ARRAY_1;
            symbol.isGlobal = true;
            symbol.label = "ARRAY" + array;
            array ++;
            symTable.putSym(name, symbol);

            String mips_data = "\t" + symbol.label + ": .word ";
            for (int i = 0; i < length.num; i++) {
                if (i != length.num - 1) mips_data += "0, ";
                else mips_data += "0\n";
            }
            data.add(mips_data);
        }
        else if (treeNodes.size() == 6) {
            symbol.symType = SymType.ARRAY_1;
            symbol.isGlobal = true;
            symbol.label = "ARRAY" + array;
            array ++;
            symTable.putSym(name, symbol);

            MidItem s = new MidItem();
            s.midType = MidType.PTR;
            s.var = curVar;
            curVar ++;
            MidCode la = new MidCode(MidOp.LA);
            la.insertItem(s, 1);
            la.label = symbol.label;
            midCodes.add(la);

            String mips_data = "\t" + symbol.label + ": .word ";
            int index = 0;
            for (int i = 0; i < items.size(); i++) {
                MidCode store = new MidCode(MidOp.STORE);
                MidItem offset = new MidItem();
                offset.midType = MidType.NUM;
                offset.num = index;
                store.insertItem(s, 1);
                store.insertItem(offset, 2);
                store.insertItem(items.get(i), 3);
                midCodes.add(store);
                index += 4;

                if (i != items.size() - 1) mips_data += "0, ";
                else mips_data += "0\n";
            }
            data.add(mips_data);
        }
        else if (treeNodes.size() == 7){
            symbol.symType = SymType.ARRAY_2;
            MidItem num1 = ConstExp(treeNodes.get(2));
            MidItem num2 = ConstExp(treeNodes.get(5));
            int length = num1.num * num2.num;
            symbol.isGlobal = true;
            symbol.midItem = num2;
            symbol.label = "ARRAY" + array;
            array ++;
            symTable.putSym(name, symbol);

            String mips_data = "\t" + symbol.label + ": .word ";
            for (int i = 0; i < length; i++) {
                if (i != length - 1) mips_data += "0, ";
                else mips_data += "0\n";
            }
            System.out.println(mips_data);
            data.add(mips_data);
        }
        else if (treeNodes.size() == 9) {
            symbol.symType = SymType.ARRAY_2;
            MidItem num1 = ConstExp(treeNodes.get(2));
            MidItem num2 = ConstExp(treeNodes.get(5));
            symbol.isGlobal = true;
            symbol.midItem = num2;
            symbol.label = "ARRAY" + array;
            array ++;
            symTable.putSym(name, symbol);

            MidItem s = new MidItem();
            s.midType = MidType.PTR;
            s.var = curVar;
            curVar ++;
            MidCode la = new MidCode(MidOp.LA);
            la.insertItem(s, 1);
            la.label = symbol.label;
            midCodes.add(la);

            String mips_data = "\t" + symbol.label + ": .word ";
            int index = 0;
            for (int i = 0; i < items.size(); i++) {
                MidCode store = new MidCode(MidOp.STORE);
                MidItem offset = new MidItem();
                offset.midType = MidType.NUM;
                offset.num = index;
                store.insertItem(s, 1);
                store.insertItem(offset, 2);
                store.insertItem(items.get(i), 3);
                midCodes.add(store);
                index += 4;
                if (i != items.size() - 1) mips_data += "0, ";
                else mips_data += "0\n";
            }
            data.add(mips_data);
        }
    }

    private static void VarDef(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;
        Symbol symbol = new Symbol();
        String name = treeNodes.get(0).token;
        symbol.name = name;

        MidItem mul4 = new MidItem();
        mul4.midType = MidType.NUM;
        mul4.num = 4;
        ArrayList<MidItem> items = InitVal(treeNodes.get(treeNodes.size() - 1));
        if (treeNodes.size() == 1) {
            symbol.symType = SymType.VAR;
            symbol.var = curVar;
            symTable.putSym(name, symbol);

            MidCode assign = new MidCode(MidOp.ASSIGN);
            MidItem item1 = new MidItem();
            item1.midType = MidType.VAR;
            item1.var = curVar;
            curVar ++;
            assign.insertItem(item1, 1);
            assign.insertItem(getZero(), 2);
            midCodes.add(assign);
        }
        else if (treeNodes.size() == 3) {
            //符号表添加
            symbol.symType = SymType.VAR;
            symbol.var = curVar;
            symTable.putSym(name, symbol);
            //中间代码类生成
            MidCode midCode = new MidCode(MidOp.ASSIGN);
            MidItem item1 = new MidItem();
            item1.midType = MidType.VAR;
            item1.var = curVar;
            curVar ++;
            midCode.insertItem(item1, 1);
            midCode.insertItem(items.get(0), 2);
            midCodes.add(midCode);
        }
        else if (treeNodes.size() == 6 || treeNodes.size() == 4) {
            //真实地址 *4
            MidItem offset = ConstExp(treeNodes.get(2));
            MidCode mult = new MidCode(MidOp.MUL);
            MidItem phOffset = new MidItem();
            phOffset.midType = MidType.NUM;
            phOffset.num = offset.num * 4;
            symbol.symType = SymType.ARRAY_1;
            symbol.var = curVar;
            //地址指针
            MidItem array1 = new MidItem();
            array1.midType = MidType.PTR;
            array1.var = curVar;
            curVar ++;
            //符号表管理
            symTable.putSym(name, symbol);
            //数组地址分配
            MidCode midCode = new MidCode(MidOp.ALLOC);
            //为其进行alloc的中间代码
            midCode.insertItem(array1, 1);
            midCode.insertItem(phOffset, 2);
            midCodes.add(midCode);
            //按照地址进行赋值
                int index = 0;
                for (MidItem item : items) {
                    MidCode temp = new MidCode(MidOp.STORE);
                    MidItem e = new MidItem();
                    e.midType = MidType.NUM;
                    e.num = index;
                    index += 4;
                    temp.insertItem(item, 3);
                    temp.insertItem(e, 2);
                    temp.insertItem(array1, 1);
                    midCodes.add(temp);
                }
        }
        else if (treeNodes.size() == 9 || treeNodes.size() == 7) {
            MidItem num1 = ConstExp(treeNodes.get(2));
            MidItem num2 = ConstExp(treeNodes.get(5));
            //真实长度计算
            MidItem phOffset = new MidItem();
            phOffset.midType = MidType.NUM;
            phOffset.num = num1.num * num2.num * 4;

            symbol.symType = SymType.ARRAY_2;
            symbol.var = curVar;
            //地址指针
            MidItem array2 = new MidItem();
            array2.midType = MidType.PTR;
            array2.var = curVar;
            curVar ++;
            //符号表管理
            symbol.midItem = num2;
            symTable.putSym(name, symbol);
            //数组地址分配
            MidCode midCode = new MidCode(MidOp.ALLOC);
            midCode.insertItem(array2, 1);
            midCode.insertItem(phOffset, 2);
            midCodes.add(midCode);
                int index = 0;
                for (MidItem midItem : items) {
                    MidCode temp = new MidCode(MidOp.STORE);
                    MidItem e = new MidItem();
                    e.midType = MidType.NUM;
                    e.num = index;
                    index += 4;
                    temp.insertItem(midItem, 3);
                    temp.insertItem(e, 2);
                    temp.insertItem(array2, 1);
                    midCodes.add(temp);
                }
        }
    }

    private static ArrayList<MidItem> InitVal(TreeNode head) {
        ArrayList<MidItem> items = new ArrayList<>();
        for (TreeNode treeNode : head.children) {
            if (treeNode.nodeType == NodeType.EXP) {
                MidItem item = Exp(treeNode);
                items.add(item);
            }
            else if (treeNode.nodeType == NodeType.INITVAL) {
                ArrayList<MidItem> temp = InitVal(treeNode);
                items.addAll(temp);
            }
        }
        return items;
    }

    private static void FuncDef(TreeNode head) {
        SymTable temp = CopySym(symTable);
        NewSymTable();
        ArrayList<TreeNode> treeNodes = head.children;
        //生成函数
        MidCode midCode = new MidCode(MidOp.DEFINE);
        String name = treeNodes.get(1).token;
        midCode.label = name;
        String funcType = treeNodes.get(0).token;
        boolean isVoid = false;
        if (Objects.equals(funcType, "void")) isVoid = true;
        //添加符号表
        Symbol func = new Symbol();
        func.name = name;
        if (isVoid) func.returnType = SymType.VOID;
        else func.returnType = SymType.VAR;
        funcSymTable.putSym(name,func);

        midCode.isVoid = isVoid;
        if (treeNodes.get(3).nodeType != NodeType.TERMINAL) {
            ArrayList<MidItem> items = FuncFParams(treeNodes.get(3));
            midCode.setItems(items);
        }
        midCodes.add(midCode);
        //进入Block
        Block(treeNodes.get(treeNodes.size() - 1));
        //设置退出函数栈
        MidCode exit = new MidCode(MidOp.EXIT);
        midCodes.add(exit);
        //
        symTable = CopySym(temp);
    }

    private static void MainFuncDef(TreeNode head) {
        SymTable temp = CopySym(symTable);
        NewSymTable();
        ArrayList<TreeNode> treeNodes = head.children;
        //生成函数
        MidCode midCode = new MidCode(MidOp.DEFINE);
        midCode.label = "main";
        midCode.isVoid = false;
        midCodes.add(midCode);
        //进入Block
        Block(treeNodes.get(treeNodes.size() - 1));
        //设置退出函数栈
        MidCode exit = new MidCode(MidOp.EXIT);
        midCodes.add(exit);
        //
        symTable = CopySym(temp);
    }

    public static ArrayList<MidItem> FuncFParams(TreeNode head) {
        ArrayList<MidItem> items = new ArrayList<>();
        for (TreeNode treeNode : head.children) {
            if (treeNode.nodeType == NodeType.FUNCFPARAM) {
                MidItem item = FuncFParam(treeNode);
                items.add(item);
            }
        }
        return items;
    }

    //FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    private static MidItem FuncFParam(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;
        String name = treeNodes.get(1).token;
        Symbol symbol = new Symbol(name);
        MidItem item = new MidItem();
        if (treeNodes.size() == 2) {
            //普通单一量
            symbol.symType = SymType.VAR;
            symbol.var = curVar;
            symTable.putSym(name, symbol);
            item.var = curVar;
            item.midType = MidType.VAR;
        }
        else if (treeNodes.size() == 4) {
            //一维数组
            symbol.symType = SymType.ARRAY_1;
            symbol.var = curVar;
            symTable.putSym(name, symbol);
            item.var = curVar;
            item.midType = MidType.PTR;
        }
        else if (treeNodes.size() == 7) {
            //二维数组
            symbol.symType = SymType.ARRAY_2;
            symbol.var = curVar;
            symbol.midItem = ConstExp(treeNodes.get(5));
            symTable.putSym(name, symbol);

            item.var = curVar;
            item.midType = MidType.PTR;
        }
        curVar ++;
        return item;
    }

    private static void Block(TreeNode head) {
        for (TreeNode treeNode : head.children) {
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

    public static void Stmt(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;;
        NodeType nodeType = treeNodes.get(0).nodeType;
        String symbol = treeNodes.get(0).symbol;

        if (nodeType == NodeType.LVAL && treeNodes.size() == 4) {
            ArrayList<MidItem> lval = LVal(treeNodes.get(0));
            MidItem exp = Exp(treeNodes.get(2));
            MidItem oldPtr = lval.get(0);
            MidItem phOffset = lval.get(1);
            if (oldPtr.midType == MidType.VAR) {
                MidCode midCode = new MidCode(MidOp.ASSIGN);
                midCode.insertItem(oldPtr, 1);
                midCode.insertItem(exp, 2);
                midCodes.add(midCode);
            }
            else {
                MidCode midCode = new MidCode(MidOp.STORE);
                midCode.insertItem(oldPtr, 1);
                midCode.insertItem(phOffset, 2);
                midCode.insertItem(exp, 3);
                midCodes.add(midCode);
            }
        }
        else if (nodeType == NodeType.LVAL && treeNodes.size() == 6) {
            //getint
            ArrayList<MidItem> lval = LVal(treeNodes.get(0));
            MidCode getint = new MidCode(MidOp.GETINT);
            MidItem oldPtr = lval.get(0);
            MidItem phOffset = lval.get(1);
            if (oldPtr.midType == MidType.VAR) {
                getint.insertItem(oldPtr, 1);
                midCodes.add(getint);
            }
            else {
                MidItem storeVar = new MidItem();
                storeVar.midType = MidType.VAR;
                storeVar.var = curVar;
                curVar += 1;
                getint.insertItem(storeVar, 1);
                midCodes.add(getint);
                //存到数组位置
                MidCode store = new MidCode(MidOp.STORE);
                store.insertItem(oldPtr, 1);
                store.insertItem(phOffset, 2);
                store.insertItem(storeVar, 3);
                midCodes.add(store);
            }
        }
        else if (nodeType == NodeType.EXP) {
            Exp(treeNodes.get(0));
        }
        else if (nodeType == NodeType.BLOCK) {
            SymTable temp = CopySym(symTable);
            NewSymTable();
            Block(treeNodes.get(0));
            symTable = CopySym(temp);
        }
        else if (Objects.equals(symbol, "RETURNTK")) {
            MidCode midCode = new MidCode(MidOp.RET);
            if (treeNodes.size() == 2) {
                midCode.isVoid = true;
            }
            else {
                midCode.isVoid = false;
                MidItem item = Exp(treeNodes.get(1));
                midCode.insertItem(item, 1);
            }
            midCodes.add(midCode);
        }
        else if (Objects.equals(symbol, "IFTK")) {
            //条件跳转
            MidCode bz = new MidCode(MidOp.BZ);
            MidItem cond = Cond(treeNodes.get(2));
            bz.insertItem(cond, 1);
            bz.setLabel(label);
            midCodes.add(bz);
            if (treeNodes.size() == 5) {
                //label1: false的位置
                MidCode label1 = new MidCode(MidOp.LABEL);
                label1.setLabel(label);
                label ++;
                //
                SymTable temp = CopySym(symTable);
                NewSymTable();
                Stmt(treeNodes.get(4));
                symTable = CopySym(temp);
                midCodes.add(label1);
            }
            else if (treeNodes.size() == 7) {
                //label1: false的位置
                MidCode label1 = new MidCode(MidOp.LABEL);
                label1.setLabel(label);
                label ++;
                //label2: 跳出去的位置
                MidCode label2 = new MidCode(MidOp.LABEL);
                label2.setLabel(label);
                label ++;
                // if : Stmt
                SymTable temp = CopySym(symTable);
                NewSymTable();
                Stmt(treeNodes.get(4));
                symTable = CopySym(temp);
                //无条件跳转
                MidCode br = new MidCode(MidOp.BR);
                br.setLabel(label2.label); // ??????????
                midCodes.add(br);
                midCodes.add(label1);
                //else : Stmt
                SymTable temp1 = CopySym(symTable);
                NewSymTable();
                Stmt(treeNodes.get(6));
                symTable = CopySym(temp1);

                midCodes.add(label2);
            }
        }
        else if (Objects.equals(symbol, "WHILETK")) {
            ArrayList<Integer> labelset = new ArrayList<>(labels);

            MidCode whilestart = new MidCode(MidOp.LABEL);
            whilestart.setLabel("WhileStart");
            midCodes.add(whilestart);
            //while的位置
            MidCode label1 = new MidCode(MidOp.LABEL);
            //结束的位置
            MidCode label2 = new MidCode(MidOp.LABEL);
            label1.setLabel(label);
            labels.add(0, label);
            label++;
            label2.setLabel(label);
            labels.add(1, label);
            label++;
            midCodes.add(label1);
            //设置判断内容以及跳转
            MidCode bz = new MidCode(MidOp.BZ);//条件跳转
            MidItem cond = Cond(treeNodes.get(2));
            bz.insertItem(cond, 1);
            bz.setLabel(labels.get(1));
            midCodes.add(bz);
            //Stmt
            SymTable temp = CopySym(symTable);
            NewSymTable();
            Stmt(treeNodes.get(4));
            symTable = CopySym(temp);
            //无条件跳转
            MidCode br = new MidCode(MidOp.BR);
            br.setLabel(labels.get(0));
            midCodes.add(br);
            midCodes.add(label2);
            //重置labels
            labels.clear();
            labels.addAll(labelset);

            MidCode whileend = new MidCode(MidOp.LABEL);
            whileend.setLabel("WhileEnd");
            midCodes.add(whileend);
        }
        else if (Objects.equals(symbol, "BREAKTK")) {
            //无条件跳转 while->label2
            MidCode br = new MidCode(MidOp.BR);
            br.setLabel(labels.get(1));
            midCodes.add(br);
        }
        else if (Objects.equals(symbol, "CONTINUETK")) {
            //无条件跳转 while->label1
            MidCode br = new MidCode(MidOp.BR);
            br.setLabel(labels.get(0));
            midCodes.add(br);
        }
        else if (Objects.equals(symbol, "PRINTFTK")) {
            ArrayList<MidItem> items = new ArrayList<>();
            MidCode printFormat = new MidCode(MidOp.PRINT);
            for (TreeNode treeNode : treeNodes) {
                if (treeNode.nodeType == NodeType.EXP) {
                    items.add(Exp(treeNode));
                }
            }
            printFormat.label = treeNodes.get(2).token;
            printFormat.labels = FormatAnalyse(printFormat.label);
            printFormat.setItems(items);
            midCodes.add(printFormat);
        }
    }

    private static MidItem Exp(TreeNode head) {
        return AddExp(head.children.get(0));
    }

    private static MidItem Cond(TreeNode head) {
        return LOrExp(head.children.get(0));
    }

    private static ArrayList<MidItem> LVal(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;
        String name = treeNodes.get(0).token;
        Symbol symbol = symTable.findSym(name);
        if (symbol.symType == SymType.VAR || symbol.symType == SymType.CONST) {
            if (symbol.isGlobal) {
                MidItem odlPtr = new MidItem();
                MidCode la = new MidCode(MidOp.LA);
                odlPtr.midType = MidType.PTR;
                odlPtr.var = curVar;
                curVar ++;
                la.insertItem(odlPtr,1);
                la.label = symbol.label;
                midCodes.add(la);

                ArrayList<MidItem> returnLval = new ArrayList<>();
                returnLval.add(0, odlPtr);
                returnLval.add(1, getZero());

                needVar = true;
                return returnLval;

            }
            else {
                MidItem lval = new MidItem();
                lval.midType = MidType.VAR;
                lval.var = symbol.var;
                ArrayList<MidItem> returnLval = new ArrayList<>();
                returnLval.add(0, lval);
                returnLval.add(1, getZero());

                needVar = false;
                return returnLval;
            }
        }
        else if (symbol.symType == SymType.ARRAY_1 || symbol.symType == SymType.CONSTARRAY_1) {
            if (treeNodes.size() == 4) {
                MidItem exp = Exp(treeNodes.get(2));
                //计算exp带来的偏移量
                MidItem offset = new MidItem();
                MidItem mul4 = new MidItem();
                //计算数组首地址
                MidItem oldPtr = new MidItem();
                if (symbol.isGlobal) {
                    MidCode la = new MidCode(MidOp.LA);
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = curVar;
                    curVar ++;
                    la.insertItem(oldPtr, 1);
                    la.label = symbol.label;
                    midCodes.add(la);
                }
                else {
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = symbol.var;
                }
                //计算偏移量
                offset.midType = MidType.VAR;
                offset.var = curVar;
                curVar++;
                mul4.midType = MidType.NUM;
                mul4.num = 4;
                MidCode mult = new MidCode(MidOp.MUL);
                mult.insertItem(offset, 1);
                mult.insertItem(mul4, 2);
                mult.insertItem(exp, 3);
                midCodes.add(mult);
                //
                ArrayList<MidItem> returnLval = new ArrayList<>();
                returnLval.add(0, oldPtr);
                returnLval.add(1, offset);

                needVar = true;
                return returnLval;
            }
            else if (treeNodes.size() == 1) {
                MidItem oldPtr = new MidItem();
                if (symbol.isGlobal) {
                    MidCode la = new MidCode(MidOp.LA);
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = curVar;
                    curVar ++;
                    la.insertItem(oldPtr, 1);
                    la.label = symbol.label;
                    midCodes.add(la);
                }
                else {
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = symbol.var;
                }
                ArrayList<MidItem> returnLval = new ArrayList<>();
                returnLval.add(0, oldPtr);
                returnLval.add(1, getZero());

                needVar = false;
                return returnLval;
            }
        }
        else if (symbol.symType == SymType.ARRAY_2 || symbol.symType == SymType.CONSTARRAY_2) {
            if (treeNodes.size() == 7) {
                MidItem temp = symbol.midItem;
                MidItem exp1 = Exp(treeNodes.get(2));
                MidItem exp2 = Exp(treeNodes.get(5));
                MidItem offset = new MidItem();
                MidItem offset1 = new MidItem();
                MidItem offset2 = new MidItem();
                MidItem mul4 = new MidItem();
                mul4.midType = MidType.NUM;
                mul4.num = 4;
                offset1.midType = MidType.VAR;
                offset1.var = curVar;
                curVar++;
                offset2.midType = MidType.VAR;
                offset2.var = curVar;
                curVar++;
                offset.midType = MidType.VAR;
                offset.var = curVar;
                curVar++;
                //计算偏移量
                MidCode mult1 = new MidCode(MidOp.MUL);
                mult1.insertItem(offset1, 1);
                mult1.insertItem(exp1, 2);
                mult1.insertItem(temp, 3);
                midCodes.add(mult1);

                MidCode add = new MidCode(MidOp.ADD);
                add.insertItem(offset2, 1);
                add.insertItem(exp2, 2);
                add.insertItem(offset1, 3);
                midCodes.add(add);

                MidCode mult2 = new MidCode(MidOp.MUL);
                mult2.insertItem(offset, 1);
                mult2.insertItem(offset2, 3);
                mult2.insertItem(mul4, 2);
                midCodes.add(mult2);
                //计算数组首地址->偏移量
                MidItem oldPtr = new MidItem();
                if (symbol.isGlobal) {
                    MidCode la = new MidCode(MidOp.LA);
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = curVar;
                    curVar ++;
                    la.insertItem(oldPtr, 1);
                    la.label = symbol.label;
                    midCodes.add(la);
                }
                else {
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = symbol.var;
                }

                ArrayList<MidItem> returnLval = new ArrayList<>();
                returnLval.add(0, oldPtr);
                returnLval.add(1, offset);
                needVar = true;
                return returnLval;
            }
            else if (treeNodes.size() == 4) {
                MidItem temp = symbol.midItem;
                MidItem exp1 = Exp(treeNodes.get(2));
                MidItem offset = new MidItem();
                MidItem offset1 = new MidItem();
                MidItem mul4 = new MidItem();
                mul4.midType = MidType.NUM;
                mul4.num = 4;
                offset1.midType = MidType.VAR;
                offset1.var = curVar;
                curVar++;
                offset.midType = MidType.VAR;
                offset.var = curVar;
                curVar++;
                //计算二维数组中一维的起始位置
                MidCode mult = new MidCode(MidOp.MUL);
                mult.insertItem(offset1, 1);
                mult.insertItem(temp, 2);
                mult.insertItem(exp1, 3);
                midCodes.add(mult);
                //计算起始物理地址
                MidCode mult1 = new MidCode(MidOp.MUL);
                mult1.insertItem(offset, 1);
                mult1.insertItem(mul4, 2);
                mult1.insertItem(offset1, 3);
                midCodes.add(mult1);
                //计算数组首地址->偏移量
                MidItem oldPtr = new MidItem();
                if (symbol.isGlobal) {
                    MidCode la = new MidCode(MidOp.LA);
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = curVar;
                    curVar ++;
                    la.insertItem(oldPtr, 1);
                    la.label = symbol.label;
                    midCodes.add(la);
                }
                else {
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = symbol.var;
                }

                ArrayList<MidItem> returnLval = new ArrayList<>();
                returnLval.add(0, oldPtr);
                returnLval.add(1, offset);
                needVar = false;
                return returnLval;
            }
            else if (treeNodes.size() == 1) {
                //计算数组首地址->偏移量
                MidItem oldPtr = new MidItem();
                if (symbol.isGlobal) {
                    MidCode la = new MidCode(MidOp.LA);
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = curVar;
                    curVar ++;
                    la.insertItem(oldPtr, 1);
                    la.label = symbol.label;
                    midCodes.add(la);
                }
                else {
                    oldPtr.midType = MidType.PTR;
                    oldPtr.var = symbol.var;
                }
                ArrayList<MidItem> returnLval = new ArrayList<>();
                returnLval.add(0, oldPtr);
                returnLval.add(1, getZero());
                needVar = false;
                return returnLval;
            }
        }

        ArrayList<MidItem> returnLval = new ArrayList<>();
        returnLval.add(0, new MidItem());
        returnLval.add(1, new MidItem());
        return returnLval;
    }

    //PrimaryExp → '(' Exp ')' | LVal | Number
    private static MidItem PrimaryExp(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;;
        if (treeNodes.size() == 3) {
            return Exp(treeNodes.get(1));
        }
        else if (treeNodes.get(0).nodeType == NodeType.LVAL) {
            ArrayList<MidItem> lval = LVal(treeNodes.get(0));
            MidItem oldPtr = lval.get(0);
            MidItem offset = lval.get(1);
            if (oldPtr.midType == MidType.VAR) {
                return oldPtr;
            }
            else {
                if (needVar) {
                    MidItem returnVar = new MidItem();
                    returnVar.midType = MidType.VAR;
                    returnVar.var = curVar;
                    curVar ++;
                    MidCode load = new MidCode(MidOp.LOAD);
                    load.insertItem(oldPtr, 1);
                    load.insertItem(offset, 2);
                    load.insertItem(returnVar, 3);
                    midCodes.add(load);
                    return returnVar;
                }
                else {
                    MidItem newPtr = new MidItem();
                    newPtr.midType = MidType.PTR;
                    newPtr.var = curVar;
                    curVar ++;
                    MidCode add = new MidCode(MidOp.ADD);
                    add.insertItem(newPtr, 1);
                    add.insertItem(oldPtr, 2);
                    add.insertItem(offset, 3);
                    midCodes.add(add);
                    return newPtr;
                }
            }
        }
        return Number(treeNodes.get(0));
    }

    private static MidItem Number(TreeNode head) {
        int num = Integer.parseInt(head.children.get(0).token);
        MidItem item = new MidItem();
        item.midType = MidType.NUM;
        item.num = num;
        return item;
    }

    //UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')'| UnaryOp UnaryExp
    private static MidItem UnaryExp(TreeNode head) {
        ArrayList<TreeNode> treeNodes = head.children;
        NodeType nodeType = treeNodes.get(0).nodeType;
        if (nodeType == NodeType.PRIMARYEXP) {
            return PrimaryExp(treeNodes.get(0));
        }
        else if (nodeType == NodeType.UNARYOP) {
            int op = UnaryOp(treeNodes.get(0));
            //-
            if (op == 1) {
                return UnaryExp(treeNodes.get(1));
            }
            else if (op == 2) {
                MidCode minu = new MidCode(MidOp.MINU);
                MidItem zero = getZero();
                MidItem newexp = new MidItem();
                newexp.midType = MidType.VAR;
                newexp.var = curVar;
                curVar ++;
                // unaryExp
                MidItem exp = UnaryExp(treeNodes.get(1));
                minu.insertItem(newexp, 1);
                minu.insertItem(zero, 2);
                minu.insertItem(exp, 3);
                midCodes.add(minu);
                return newexp;
            }
            //！
            else if (op == 3) {
                MidCode eqz = new MidCode(MidOp.EQZ);
                MidItem newexp = new MidItem();
                newexp.midType = MidType.VAR;
                newexp.var = curVar;
                curVar ++;
                // unaryExp
                MidItem exp = UnaryExp(treeNodes.get(1));
                eqz.insertItem(newexp, 1);
                eqz.insertItem(exp, 2);
                midCodes.add(eqz);
                return newexp;
            }
        }
        //函数调用
        else {
            MidCode callFunc = new MidCode(MidOp.CALL);
            ArrayList<MidItem> funcParams = new ArrayList<>();
            if (treeNodes.size() == 4) {
                funcParams = FuncRParams(treeNodes.get(2));
            }
            String name = treeNodes.get(0).token;
            Symbol func = funcSymTable.findSym(name);

            callFunc.setLabel(name);
            callFunc.items = funcParams;

            SymType returnType = func.returnType;
            MidItem returnVar = new MidItem();
            if (returnType == SymType.VAR) {
                returnVar.midType = MidType.VAR;
                returnVar.var = curVar;
                curVar ++;
                callFunc.insertItem(returnVar, 1);
                callFunc.isVoid = false;
            }
            midCodes.add(callFunc);
            return returnVar;
        }
        return new MidItem();
    }

    private static int UnaryOp(TreeNode head) {
        String type = head.children.get(0).symbol;
        if (Objects.equals(type, "MINU")) {
            return 2;
        }
        // !
        else if (Objects.equals(type, "PLUS"))
            return 1;
        return 3;
    }

    private static ArrayList<MidItem> FuncRParams(TreeNode head) {
        ArrayList<MidItem> items = new ArrayList<>();
        for (TreeNode treeNode : head.children) {
            if (treeNode.nodeType == NodeType.EXP) {
                items.add(Exp(treeNode));
            }
        }
        return items;
    }

    //MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private static MidItem MulExp(TreeNode head) {
        ArrayList<TreeNode> items = head.children;
        if (items.size() == 1) {
            return UnaryExp(items.get(0));
        }
        else {
            String mulOp = items.get(1).token;
            MidItem mulExp1 = MulExp(items.get(0));
            MidItem unaryExp = UnaryExp(items.get(2));
            MidItem newMulExp = new MidItem();
            newMulExp.midType = MidType.VAR;
            newMulExp.var = curVar;
            curVar ++;
            MidCode mulExp = new MidCode();
            if (Objects.equals(mulOp, "*")) {
                mulExp.midOp = MidOp.MUL;
            }
            else if (Objects.equals(mulOp, "/")) {
                mulExp.midOp = MidOp.DIV;
            }
            else if (Objects.equals(mulOp, "%")) {
                mulExp.midOp = MidOp.MOD;
            }
            mulExp.insertItem(newMulExp, 1);
            mulExp.insertItem(mulExp1, 2);
            mulExp.insertItem(unaryExp, 3);

            midCodes.add(mulExp);
            return newMulExp;
        }
    }

    //AddExp→ MulExp | AddExp ('+' | '−') MulExp
    private static MidItem AddExp(TreeNode head) {
        ArrayList<TreeNode> items = head.children;
//        System.out.println(items.size());
        if (items.size() == 1) {
            return MulExp(items.get(0));
        }
        else {
            String addOp = items.get(1).token;
            MidItem addExp1 = AddExp(items.get(0));
            MidItem mulExp = MulExp(items.get(2));
            MidItem newAddExp = new MidItem();
            newAddExp.midType = MidType.VAR;
            newAddExp.var = curVar;
            curVar ++;
            MidCode addExp = new MidCode();
            if (Objects.equals(addOp, "+")) {
                addExp.midOp = MidOp.ADD;
            }
            else if (Objects.equals(addOp, "-")) {
                addExp.midOp = MidOp.MINU;
            }
            addExp.insertItem(newAddExp, 1);
            addExp.insertItem(addExp1, 2);
            addExp.insertItem(mulExp, 3);

            midCodes.add(addExp);
            return newAddExp;
        }
    }

    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private static MidItem RelExp(TreeNode head) {
        ArrayList<TreeNode> items = head.children;
        if (items.size() == 1) {
            return AddExp(items.get(0));
        }
        else {
            String relOp = items.get(1).token;
            MidItem relExp1 = RelExp(items.get(0));
            MidItem addExp = AddExp(items.get(2));
            MidItem newRelExp = new MidItem();
            newRelExp.midType = MidType.VAR;
            newRelExp.var = curVar;
            curVar ++;
            MidCode relExp = new MidCode();
            if (Objects.equals(relOp, "<")) {
                relExp.midOp = MidOp.LT;
            }
            else if (Objects.equals(relOp, ">")) {
                relExp.midOp = MidOp.BT;
            }
            else if (Objects.equals(relOp, "<=")) {
                relExp.midOp = MidOp.LE;
            }
            else if (Objects.equals(relOp, ">=")) {
                relExp.midOp = MidOp.BE;
            }
            relExp.insertItem(newRelExp, 1);
            relExp.insertItem(relExp1, 2);
            relExp.insertItem(addExp, 3);

            midCodes.add(relExp);
            return newRelExp;
        }
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private static MidItem EqExp(TreeNode head) {
        ArrayList<TreeNode> items = head.children;
        if (items.size() == 1) {
            return RelExp(items.get(0));
        }
        else {
            String eqOp = items.get(1).token;
            MidItem eqExp1 = EqExp(items.get(0));
            MidItem relExp = RelExp(items.get(2));
            MidItem newEqExp = new MidItem();
            newEqExp.midType = MidType.VAR;
            newEqExp.var = curVar;
            curVar ++;
            MidCode eqExp = new MidCode();
            if (Objects.equals(eqOp, "==")) {
                eqExp.midOp = MidOp.EQ;
            }
            else if (Objects.equals(eqOp, "!=")) {
                eqExp.midOp = MidOp.NE;
            }
            eqExp.insertItem(newEqExp, 1);
            eqExp.insertItem(eqExp1, 2);
            eqExp.insertItem(relExp, 3);

            midCodes.add(eqExp);
            return newEqExp;
        }
    }


    // LAndExp → EqExp | LAndExp '&&' EqExp
    private static MidItem LAndExp(TreeNode head) {
        ArrayList<TreeNode> items = head.children;
        if (items.size() == 1) {
            return EqExp(items.get(0));
        }
        else {
            MidItem landExp1 = LAndExp(items.get(0));
            MidItem newLandExp = new MidItem();
            newLandExp.midType = MidType.VAR;
            newLandExp.var = curVar;
            curVar ++;
            //进行LAnd的真值比较:eq
            MidItem eq = new MidItem();
            eq.midType = MidType.VAR;
            eq.var = curVar;
            curVar ++;
            MidCode eqCode = new MidCode(MidOp.EQ);
            eqCode.insertItem(eq, 1);
            eqCode.insertItem(landExp1, 2);
            eqCode.insertItem(getZero(), 3);
            midCodes.add(eqCode);
            //eq如果为0则跳转到label1
            MidCode bz = new MidCode(MidOp.BZ);
            bz.insertItem(eq, 1);
            bz.setLabel(label);
            int labelTemp1 = label;
            label++;
            midCodes.add(bz);
            //结果赋值为0
            MidCode assign = new MidCode(MidOp.ASSIGN);
            assign.insertItem(newLandExp, 1);
            assign.insertItem(getZero(), 2);
            midCodes.add(assign);
            //无条件跳转：label2
            MidCode br = new MidCode(MidOp.BR);
            br.setLabel(label);
            int labelTemp2 = label;
            label++;
            midCodes.add(br);
            //
            MidCode label0 = new MidCode(MidOp.LABEL);
            label0.setLabel(labelTemp1);
            midCodes.add(label0);
            //
            MidItem eqExp = EqExp(items.get(2));
            MidCode landExp = new MidCode(MidOp.AND);
            landExp.insertItem(newLandExp, 1);
            landExp.insertItem(landExp1, 2);
            landExp.insertItem(eqExp, 3);
            midCodes.add(landExp);
            //
            MidCode label1 = new MidCode(MidOp.LABEL);
            label1.setLabel(labelTemp2);
            midCodes.add(label1);
            return newLandExp;
        }
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    private static MidItem LOrExp(TreeNode head) {
        ArrayList<TreeNode> items = head.children;
        if (items.size() == 1) {
            return LAndExp(items.get(0));
        }
        else {
            MidItem lorExp1 = LOrExp(items.get(0));
            MidItem newLorExp = new MidItem();
            newLorExp.midType = MidType.VAR;
            newLorExp.var = curVar;
            curVar ++;
            //比较第一个LOrExp的真值:
            // 等于1则将eq值为1
            MidItem eq = new MidItem();
            eq.midType = MidType.VAR;
            eq.var = curVar;
            curVar ++;
            MidItem one = new MidItem();
            one.midType = MidType.NUM;
            one.num = 1;
            MidCode ne = new MidCode(MidOp.NE);
            ne.insertItem(eq, 1);
            ne.insertItem(lorExp1, 2);
            ne.insertItem(getZero(), 3);
            midCodes.add(ne);
            //
            MidCode bz = new MidCode(MidOp.BZ);
            bz.insertItem(eq, 1);
            bz.setLabel(label);
            int labelTemp1 = label;
            label ++;
            midCodes.add(bz);
            //结果赋值为0
            MidCode assign = new MidCode(MidOp.ASSIGN);
            assign.insertItem(newLorExp, 1);
            assign.insertItem(one, 2);
            midCodes.add(assign);
            //无条件跳转：label2
            MidCode br = new MidCode(MidOp.BR);
            br.setLabel(label);
            int labelTemp2 = label;
            label++;
            midCodes.add(br);
            //label1
            MidCode label1 = new MidCode(MidOp.LABEL);
            label1.setLabel(labelTemp1);
            midCodes.add(label1);
            //
            MidItem landExp = LAndExp(items.get(2));
            MidCode lorExp = new MidCode(MidOp.OR);
            lorExp.insertItem(newLorExp, 1);
            lorExp.insertItem(lorExp1, 2);
            lorExp.insertItem(landExp, 3);
            midCodes.add(lorExp);
            //label2
            MidCode label2 = new MidCode(MidOp.LABEL);
            label2.setLabel(labelTemp2);
            midCodes.add(label2);
            return newLorExp;
        }
    }

    private static MidItem ConstExp(TreeNode head) {
        MidItem constExp = new MidItem();
        constExp.num = head.num;
        constExp.midType = MidType.NUM;
        return constExp;
    }

    private static ArrayList<String> FormatAnalyse(String label) {
        String label1 = label.substring(1, label.length() -1);
        String temp;
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (j < label1.length()) {
            if (label1.charAt(j) == '%' && label1.charAt(j + 1) == 'd') {
                temp = label1.substring(i, j);
                if (!Objects.equals(temp, "")) {
                    data.add("String" + print + ": .asciiz \"" + temp + "\"\n");
                    labels.add("String" + print);
                    print ++;
                }
                j += 2;
                i = j;
                labels.add("%d");
                continue;
            }
            j ++;
        }
        temp = label1.substring(i, j);
        if (!Objects.equals(temp, "")) {
            data.add("String" + print + ": .asciiz \"" + temp + "\"\n");
            labels.add("String" + print);
            print ++;
        }
        return labels;
    }


    private static void NewSymTable() {
        symTable = new SymTable(symTable);
    }

    private static SymTable CopySym(SymTable s1) {
        SymTable newSym = new SymTable();
        newSym.fatherTable = s1.fatherTable;
        newSym.symTable.putAll(s1.symTable);
        return newSym;
    }

    public static MidItem getZero() {
        MidItem zero = new MidItem();
        zero.midType = MidType.NUM;
        zero.num = 0;
        return zero;
    }
}

