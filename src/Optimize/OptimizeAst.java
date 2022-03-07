package Optimize;

import AST.NodeType;
import AST.TreeNode;
import SymbolFile.SymTable;
import SymbolFile.SymType;
import SymbolFile.Symbol;

import java.util.ArrayList;
import java.util.Objects;

public class OptimizeAst {
    public static TreeNode TopTree;
    public static SymTable symTable;


    public static void StartOptimize(TreeNode head) {
        symTable = new SymTable(null);
        TopTree = head;
        CutConstExp(TopTree);
    }

    //常量处理,剪枝
    private static void CutConstExp(TreeNode treeTop) {
        for (TreeNode treeNode : treeTop.children) {
            if (treeNode.nodeType == NodeType.CONSTEXP) {
                treeNode.num = ConstExp(treeNode);
                treeNode.constnum = true;
            }
            else if (treeNode.nodeType == NodeType.CONSTDEF) {
                ConstDef(treeNode);
            }
            else if (treeNode.nodeType == NodeType.BLOCK) {
                ConstBlock(treeNode);
            }
            else if (treeNode.nodeType == NodeType.STMT) {
                ConstStmt(treeNode);
            }
            else {
                CutConstExp(treeNode);
            }
        }
    }


    private static int ConstExp(TreeNode treeTop) {
        int num = 0;
        for (TreeNode treeNode : treeTop.children) {
            num = ConstAddExp(treeNode);
        }
        return num;
    }

    private static int ConstAddExp(TreeNode treeTop) {
        int num = 0, num1 = 0, num2 = 0;
        int length = treeTop.children.size();
        if (length == 1) {
            num = ConstMulExp(treeTop.children.get(0));
        }
        else {
            num1 = ConstAddExp(treeTop.children.get(0));
            num2 = ConstMulExp(treeTop.children.get(2));
            if (Objects.equals(treeTop.children.get(1).symbol, "PLUS")) {
                num = num1 + num2;
            }
            else num = num1 - num2;
        }
        return num;
    }

    private static int ConstMulExp(TreeNode treeTop) {
        int num = 0, num1 = 0, num2 = 0;
        int length = treeTop.children.size();
        if (length == 1) {
            num = ConstUnaryExp(treeTop.children.get(0));
        }
        else {
            num1 = ConstMulExp(treeTop.children.get(0));
            num2 = ConstUnaryExp(treeTop.children.get(2));
            if (Objects.equals(treeTop.children.get(1).symbol, "MULT")) {
                num = num1 * num2;
            }
            else if (Objects.equals(treeTop.children.get(1).symbol, "DIV")) {
                num = num1 / num2;
            }
            else num = num1 % num2;
        }
        return num;
    }

    private static int ConstUnaryExp(TreeNode treeTop) {
        int num = 0;
        int length = treeTop.children.size();
        if (length == 1) {
            num = ConstPrimaryExp(treeTop.children.get(0));
        }
        else if (length == 2) {
            num = ConstUnaryExp(treeTop.children.get(1));
            if (Objects.equals(treeTop.children.get(0).children.get(0).symbol, "PLUS")) {
                num = num;
            }
            else if (Objects.equals(treeTop.children.get(0).children.get(0).symbol, "MINU")) {
                num = -num;
            }
        }
        return num;
    }

    private static int ConstPrimaryExp(TreeNode treeTop) {
        int num = 0;
        int length = treeTop.children.size();
        if (length == 3) {
            num = ConstExp(treeTop.children.get(1));
        }
        else if (treeTop.children.get(0).nodeType == NodeType.NUMBER) {
            num = Integer.parseInt(treeTop.children.get(0).children.get(0).token);
        }
        else {
            num = ConstLVal(treeTop.children.get(0));
        }
        return num;
    }

    private static void ConstDef(TreeNode treeTop) {
        int length = treeTop.children.size();
        String name = treeTop.children.get(0).token;
        Symbol symbol = new Symbol();
        symbol.name = name;
        if (length == 3) {
            symbol.symType = SymType.CONST;
            symbol.constNum = ConstInitVal(treeTop.children.get(2));
            symTable.putSym(name, symbol);
        }
        else if (length == 6) {
            treeTop.children.get(2).num = ConstExp(treeTop.children.get(2));
            symbol.symType = SymType.CONSTARRAY_1;
            symbol.constNum = ConstInitVal(treeTop.children.get(5));
            symTable.putSym(name, symbol);
        }
        else if (length == 9) {
            treeTop.children.get(2).num = ConstExp(treeTop.children.get(2));
            treeTop.children.get(5).num = ConstExp(treeTop.children.get(5));
            symbol.length = ConstExp(treeTop.children.get(2));
            symbol.symType = SymType.CONSTARRAY_2;
            symbol.constNum = ConstInitVal(treeTop.children.get(8));
            symTable.putSym(name, symbol);
        }
    }

    private static int ConstLVal(TreeNode treeNode) {
        int num;
        String name = treeNode.children.get(0).token;
        Symbol symbol = symTable.findSym(name);
        int length = treeNode.children.size();

        //单个量
        if (length == 1) {
            num = symbol.constNum.get(0);
        }
        //一维数组
        else if (length == 4) {
            int temp = ConstExp(treeNode.children.get(2));
            num = symbol.constNum.get(temp);
        }
        //二维度数组
        else {
            int t1 = ConstExp(treeNode.children.get(2));
            int t2 = ConstExp(treeNode.children.get(5));
            int len = symbol.length;
            num = symbol.constNum.get(t1 * len + t2);
        }
        return num;
    }

    private static ArrayList<Integer> ConstInitVal(TreeNode treeTop) {
        ArrayList<Integer> nums = new ArrayList<>();
        for (TreeNode treeNode : treeTop.children) {
            if (treeNode.nodeType == NodeType.CONSTEXP) {
                int num = ConstExp(treeNode);
                treeNode.num = num;
                nums.add(num);
            }
            else if (treeNode.nodeType == NodeType.CONSTINITVAL) {
                ArrayList<Integer> t = ConstInitVal(treeNode);
                nums.addAll(t);
            }
        }
        return nums;
    }

    private static void ConstBlock(TreeNode treeTop) {
        SymTable temp = new SymTable();
        temp.fatherTable = symTable.fatherTable;
        temp.symTable.putAll(symTable.symTable);

        symTable = new SymTable(symTable);
        CutConstExp(treeTop);

        symTable = new SymTable();
        symTable.fatherTable = temp.fatherTable;
        symTable.symTable.putAll(temp.symTable);
    }

    private static void ConstStmt(TreeNode treeTop) {
        ArrayList<TreeNode> treeNodes = treeTop.children;
        String symbol = treeNodes.get(0).symbol;
        if (Objects.equals(symbol, "IFTK")) {
            for (TreeNode treeNode : treeNodes) {
                if (treeNode.nodeType == NodeType.COND) {
                    CutConstExp(treeNode);
                }
                else if (treeNode.nodeType == NodeType.STMT) {
                    SymTable temp = new SymTable();
                    temp.fatherTable = symTable.fatherTable;
                    temp.symTable.putAll(symTable.symTable);

                    symTable = new SymTable(symTable);
                    ConstStmt(treeNode);

                    symTable = new SymTable();
                    symTable.fatherTable = temp.fatherTable;
                    symTable.symTable.putAll(temp.symTable);

                }
            }
        }
        else if (Objects.equals(symbol, "WHILETK")) {
            for (TreeNode treeNode : treeNodes) {
                if (treeNode.nodeType == NodeType.COND) {
                    CutConstExp(treeNode);
                }
                else if (treeNode.nodeType == NodeType.STMT) {
                    SymTable temp = new SymTable();
                    temp.fatherTable = symTable.fatherTable;
                    temp.symTable.putAll(symTable.symTable);

                    symTable = new SymTable(symTable);
                    // ?????????????????????????????????????????????????????
                    ConstStmt(treeNode);

                    symTable = new SymTable();
                    symTable.fatherTable = temp.fatherTable;
                    symTable.symTable.putAll(temp.symTable);

                }
            }
        }
        else {
            for (TreeNode treeNode : treeNodes) {
                CutConstExp(treeNode);
            }
        }
    }

}
