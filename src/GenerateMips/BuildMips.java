package GenerateMips;

import LlvmIR.MidCode;
import LlvmIR.MidItem;
import LlvmIR.MidOp;
import LlvmIR.MidType;
import SymbolFile.SymTable;
import SymbolFile.Symbol;

import java.util.ArrayList;
import java.util.Objects;

public class BuildMips {
    public static ArrayList<MidCode> midCodes;
//    public static GlobalRegister globalRegister = new GlobalRegister();
    public static ArrayList<MidItem> varFrequent = new ArrayList<>();
    public static ArrayList<String> mips = new ArrayList<>();
    public static ArrayList<String> strList;
    public static SymTable symTable;
    public static int curOffset;
    public static int atMain;
    public static int atFunc;

    public static boolean isNum1 = false;
    public static boolean isNum2 = false;
    public static int num1 = 0;
    public static int num2 = 0;

    public static void StartMakeMips(ArrayList<MidCode> md, ArrayList<String> s, ArrayList<MidItem> recNumber) {
        midCodes = md;
        symTable = new SymTable(null);
        curOffset = 0;
        atMain = 0;
        atFunc = 1;
        strList = s;
        varFrequent = recNumber;

        mips.add(".data\n");
        mips.addAll(strList);
        mips.add(".text\n");
        //移动栈
        mips.add("move $fp, $sp\n");

        for (MidCode midCode : midCodes) {
            if (midCode.midOp == MidOp.CALL) Call(midCode);
            else if (midCode.midOp == MidOp.ALLOC) Alloc(midCode);
            else if (midCode.midOp == MidOp.LOAD) Load(midCode);
            else if (midCode.midOp == MidOp.STORE) Store(midCode);
            else if (midCode.midOp == MidOp.BR) Br(midCode);
            else if (midCode.midOp == MidOp.RET) Ret(midCode);
            else if (midCode.midOp == MidOp.DEFINE) Define(midCode);
            else if (midCode.midOp == MidOp.ASSIGN) Assign(midCode);
            else if (midCode.midOp == MidOp.LABEL) Label(midCode);
            else if (midCode.midOp == MidOp.EXIT) Exit(midCode);
            else if (midCode.midOp == MidOp.GETINT) Getint(midCode);
            else if (midCode.midOp == MidOp.PRINT) Print(midCode);
            else if (midCode.midOp == MidOp.BZ) Bz(midCode);
            else if (midCode.midOp == MidOp.EQZ) Eqz(midCode);
            else if (midCode.midOp == MidOp.LA) La(midCode);
            else RCode(midCode);
        }
        //结束
        mips.add("end:");
        CheckRepeat();
    }

    private static void CheckRepeat() {
        ArrayList<String> noMips = new ArrayList<>(mips);
        for (int i = 0; i < noMips.size(); i++) {
            noMips.set(i, noMips.get(i).replace(" ", ""));
        }
        for (int i = 1; i < noMips.size(); i++) {
            if (Objects.equals(noMips.get(i - 1), noMips.get(i))) {
                mips.remove(i);
                noMips.remove(i);
                i --;
            }
        }
    }

    private static void Call(MidCode midCode) {
        //返回值堆栈中的位置
        if (!midCode.isVoid) {
            curOffset += 4;
            mips.add("sub $sp, $sp, " + curOffset + "\n");
            MidItem item = midCode.item1;
            Symbol newSymbol = new Symbol();
            newSymbol.offset = curOffset;
            symTable.putSym(String.valueOf(item.var), newSymbol);
        }
        else {
            mips.add("sub $sp, $sp, " + curOffset + "\n");
        }
        //fp用来存放参数， ra存放返回地址
        mips.add("sub $sp, $sp, 4\n");
        mips.add("sw $fp, ($sp)\n");
        //传参之前的必要工作,直接放入栈底
        int mid = curOffset;
        curOffset = 4;//为ra留存的位置

        for (MidItem item : midCode.items) {
            curOffset += 4;
            mips.addAll(AllocReg(item, 1));
            mips.add("sw $s1, -" + curOffset + "($sp)\n");
        }
        //将fp指向栈底
        mips.add("move $fp, $sp\n");
        //跳转到函数label
        mips.add("jal " + midCode.label + "\n");

        curOffset = mid;
    }

    private static void Alloc(MidCode midCode) {
        //给数组分配空间
        Symbol newSymbol = new Symbol();
        MidItem item1 = midCode.item1, item2 = midCode.item2;
        curOffset += 4;
        newSymbol.offset = curOffset;
        symTable.putSym(String.valueOf(item1.var), newSymbol);
        //开辟数组空间
//        mips.addAll(AllocReg(item2, 1));
        mips.add("sub $t1, $fp, " + curOffset + "\n");
        curOffset += item2.num;
        //将真正的偏移量存进去
        mips.add("sub $t0, $fp, "  + curOffset+ "\n");
        mips.add("sw $t0, ($t1)\n");
    }

    private static void Load(MidCode midCode) {
        Symbol newSymbol = new Symbol();
        MidItem item1 = midCode.item1,
                item2 = midCode.item2, item3 = midCode.item3;
        mips.addAll(AllocReg(item1, 2));
        mips.addAll(AllocReg(item2, 3));
        mips.add("add $s1, $s2, $s3\n");
        //获取数据
        mips.add("lw $s1, ($s1)\n");
        Symbol symbol = symTable.findSym(String.valueOf(item3.var));
        if (symbol == null) {
            curOffset += 4;
            newSymbol.offset = curOffset;
            symTable.putSym(String.valueOf(item3.var), newSymbol);
            //real addr
            //存入数据
            mips.add("sw $s1, -" + curOffset + "($fp)\n");
        }
        else {
            //存入数据
            mips.add("sw $s1, -" + symbol.offset + "($fp)\n");
        }
    }

    private static void Store(MidCode midCode) {
        MidItem item1 = midCode.item1,
                item2 = midCode.item2, item3 = midCode.item3;
        mips.addAll(AllocReg(item1, 1));
        mips.addAll(AllocReg(item2, 2));
        mips.addAll(AllocReg(item3, 3));
        //地址
        mips.add(" add $s1, $s1, $s2\n");
        //存入
        mips.add(" sw $s3, ($s1)\n");
    }

    private static void Br(MidCode midCode) {
        mips.add(" j " + midCode.label + "\n");
    }

    private static void Ret(MidCode midCode) {
        MidItem item1 = midCode.item1;
        if (atMain == 1) {
            mips.add(" move $sp, $fp\n");
            mips.add(" j end\n");
            return;
        }
        if (!midCode.isVoid) {
            mips.addAll(AllocReg(item1, 1));
            mips.add(" sw $s1, 4($fp)\n");
        }
        mips.add(" lw $ra, -4($fp)\n");
        //clear sp
        mips.add(" add $sp, $fp, 4\n");
        mips.add(" lw $fp, ($fp)\n");
        mips.add(" move $sp, $fp\n");
        mips.add(" jr $ra\n");
    }

    private static void Define(MidCode midCode) {
        atMain = 0;
        if (atFunc == 1) {
            mips.add(" move $fp, $sp\n");
            mips.add(" j main\n");
            atFunc = 0;
        }
        String funcName = midCode.label;
        if (Objects.equals(funcName, "main")) {
            atMain = 1;
        }
        mips.add(funcName + ":\n");
        curOffset = 4;
        mips.add(" sw $ra, -4($fp)\n");
        for (MidItem midItem : midCode.items) {
            Symbol newSymbol = new Symbol();
            curOffset += 4;
            newSymbol.offset = curOffset;
            symTable.putSym(String.valueOf(midItem.var), newSymbol);
        }
        symTable = new SymTable(symTable);
    }

    private static void Assign(MidCode midCode) {
        MidItem item1 = midCode.item1, item2 = midCode.item2;
        mips.addAll(AllocReg(item2, 2));
        String name = String.valueOf(item1.var);
        Symbol symbol = symTable.findSym(name);
        if (item1.midType == MidType.VAR) {
            if (symbol == null) {
                    Symbol newSymbol = new Symbol();
                    curOffset += 4;
                    newSymbol.offset = curOffset;
                    symTable.putSym(name, newSymbol);
                    mips.add("sw $s2, -" + curOffset + "($fp)\n");
            }
            else {
                mips.add("sw $s2, -" + symbol.offset + "($fp)\n");
            }
        }
        else {
            mips.add("lw $s0, -" + symbol.offset + "($fp)\n");
            mips.add("sw $s2, ($s0)\n");
        }
    }

    private static void Label(MidCode midCode) {
        mips.add(midCode.label + ":\n");
    }

    private static void Exit(MidCode midCode) {
        symTable = symTable.fatherTable;
        if (symTable == null) {
            return;
        }
        //返回地址
        mips.add(" lw $ra, -4($fp)\n");
        //clear stack
        mips.add(" add $sp, $fp, 4\n");
        mips.add(" lw $fp, ($fp)\n");
        mips.add(" move $sp, $fp\n");
        mips.add(" jr $ra\n");
    }

    private static void Getint(MidCode midCode) {
        MidItem item1 = midCode.item1;
        Symbol symbol = symTable.findSym(String.valueOf(item1.var));
        mips.add(" li $v0, 5\n");
        mips.add(" syscall\n");
        if (symbol == null) {
            Symbol newSymbol = new Symbol();
            curOffset += 4;
            newSymbol.offset = curOffset;
            symTable.putSym(String.valueOf(item1.var), newSymbol);
            mips.add(" sw $v0, -" + curOffset + "($fp)\n");
        }
        else {
            if (item1.midType == MidType.VAR) {
                mips.add(" sw $v0, -" + symbol.offset + "($fp)\n");
            }
            else {
                mips.add(" lw $s0, -" + symbol.offset + "($fp)\n");
                mips.add(" sw $v0, ($s0)\n");
            }
        }
    }

    private static void Print(MidCode midCode) {
        ArrayList<String> labels = midCode.labels;
        ArrayList<MidItem> items = midCode.items;
        int i = 0;
        for (String s : labels) {
            if (Objects.equals(s, "%d")) {
                mips.addAll(AllocReg(items.get(i), 1));
                mips.add(" move $a0, $s1\n");
                mips.add(" li $v0, 1\n");
                mips.add(" syscall\n");
                i++;
            }
            else {
                mips.add(" la $a0, " + s + "\n");
                mips.add(" li $v0, 4\n");
                mips.add(" syscall\n");
            }
        }
    }

    private static void Bz(MidCode midCode) {
        MidItem item1 = midCode.item1;
        mips.addAll(AllocReg(item1, 1));
        mips.add("beqz $s1, " + midCode.label + "\n");
    }

    private static void Eqz(MidCode midCode) {
        MidItem item1 = midCode.item1, item2 = midCode.item2;
        Symbol symbol = new Symbol();
        curOffset += 4;
        symbol.offset = curOffset;
        symTable.putSym(String.valueOf(item1.var), symbol);
        mips.addAll(AllocReg(item2, 2));
        mips.add("seq $s1, $s2, 0\n");
        mips.add("sw $s1, -" + curOffset + "($fp)\n");
    }

    private static void La(MidCode midCode) {
        MidItem item1 = midCode.item1;
        Symbol symbol = new Symbol();
        curOffset += 4;
        symbol.offset = curOffset;
        symTable.putSym(String.valueOf(item1.var), symbol);
        //调用全局变量或者常量
        mips.add("la $s1, " + midCode.label + "\n");
        mips.add("sw $s1, -" + curOffset + "($fp)\n");
        mips.add("lw $s1, ($s1)\n");
    }

    private static ArrayList<String> AllocReg(MidItem item, int index) {
        String reg = "$s" + index;
        ArrayList<String> regMips = new ArrayList<>();
        if (item.midType == MidType.NUM) {
            regMips.add("li " + reg + ", " + item.num + "\n");
        }
        else if (item.midType == MidType.VAR || item.midType == MidType.PTR) {
            Symbol symbol = symTable.findSym(String.valueOf(item.var));
            if (symbol == null) System.out.println(item.toString());
            regMips.add("lw " + reg + ", -" + symbol.offset + "($fp)\n");
        }
        return regMips;
    }

    private static void RCode(MidCode midCode) {
        MidOp midOp = midCode.midOp;
        Symbol newSymbol = new Symbol();
        MidItem item1 = midCode.item1, item2 = midCode.item2, item3 = midCode.item3;
        //含有常数运算处理
        if (midOp == MidOp.ADDI) {
            mips.addAll(AllocReg(item2, 2));
            String number = String.valueOf(item3.num);
            mips.add("addiu $s1, $s2, " + number + "\n");
        }
        else if (midOp == MidOp.MINUI) {
            mips.addAll(AllocReg(item2, 2));
            String number = String.valueOf(item3.num);
            mips.add("subiu $s1, $s2, " + number + "\n");
        }
        else if (midOp == MidOp.SHR) {
            mips.addAll(AllocReg(item2, 2));
            String number = String.valueOf(item3.num);
            mips.add("srl $s1, $s2, " + number + "\n");
        }
        else if (midOp == MidOp.SHL) {
            mips.addAll(AllocReg(item2, 2));
            String number = String.valueOf(item3.num);
            mips.add("sll $s1, $s2, " + number + "\n");
        }
        else if (midOp == MidOp.DIV && item3.midType == MidType.NUM) {
            int num = item3.num;
            if (num < 0) num = -num;
            int l = maxLog(num);
            int m = (int)(getUnsignedInt(1) +
                    (getUnsignedInt(1) << (31 + l)) / getUnsignedInt(num) - (getUnsignedInt(1) << 32));
            int d_sign = num < 0 ? -1 : 0;
            int sh_post = l - 1;
            mips.addAll(AllocReg(item2, 2));
            mips.add("li " + "$s3, " + m + "\n");
            mips.add("mult $s2, $s3\n");
            mips.add("mfhi $s3\n");
            mips.add("addu $s1, $s2, $s3\n");
            mips.add("sra $s1, $s1, " + sh_post + "\n");
            mips.add("sge $s3, $s2, " + 0 + "\n");
            mips.add("subiu $s3, $s3, " + 1 + "\n");
            mips.add("subu $s1, $s1, $s3\n");
            mips.add("xori $s1, $s1, " + d_sign + "\n");
            mips.add("subiu $s1, $s1, " + d_sign + "\n");
        }
        else if (midOp == MidOp.MOD && item3.midType == MidType.NUM) {
            int num = item3.num;
            if (num < 0) num = -num;
            int l = maxLog(num);
            int m = (int)(getUnsignedInt(1) +
                    (getUnsignedInt(1) << (31 + l)) / getUnsignedInt(num) - (getUnsignedInt(1) << 32));
            int d_sign = num < 0 ? -1 : 0;
            int sh_post = l - 1;
            mips.addAll(AllocReg(item2, 2));
            mips.add("li " + "$s4, " + m + "\n");
            mips.add("mult $s2, $s4\n");
            mips.add("mfhi $s3\n");
            mips.add("addu $s1, $s2, $s3\n");
            mips.add("sra $s1, $s1, " + sh_post + "\n");
            mips.add("sge $s3, $s2, " + 0 + "\n");
            mips.add("subiu $s3, $s3, " + 1 + "\n");
            mips.add("subu $s1, $s1, $s3\n");
            mips.add("xori $s1, $s1, " + d_sign + "\n");
            mips.add("subiu $s1, $s1, " + d_sign + "\n");
            mips.add("li " + "$s4, " + num + "\n");
            mips.add("mult $s1, $s4\n");
            mips.add("mflo $s3\n");
            mips.add("subu $s1, $s2, $s3\n");
        }
        else {
            //分配寄存器
            mips.addAll(AllocReg(item2, 2));
            mips.addAll(AllocReg(item3, 3));

            if (midOp == MidOp.ADD) {
                mips.add("addu $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.MINU) {
                mips.add("subu $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.MUL) {
                mips.add("mult $s2, $s3\n");
                mips.add("mflo $s1\n");
            }
            else if (midOp == MidOp.DIV) {
                mips.add("div $s2, $s3\n");
                mips.add("mflo $s1\n");
            }
            else if (midOp == MidOp.MOD) {
                mips.add("div $s2, $s3\n");
                mips.add("mfhi $s1\n");
            }
            else if (midOp == MidOp.AND) {
                mips.add("sne $s2, $s2, 0\n");
                mips.add("sne $s3, $s3, 0\n");
                mips.add("and $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.OR) {
                mips.add("sne $s2, $s2, 0\n");
                mips.add("sne $s3, $s3, 0\n");
                mips.add("or $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.BE) {
                mips.add("sge $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.BT) {
                mips.add("sgt $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.LT) {
                mips.add("slt $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.LE) {
                mips.add("sle $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.EQ) {
                mips.add("seq $s1, $s2, $s3\n");
            }
            else if (midOp == MidOp.NE) {
                mips.add("sne $s1, $s2, $s3\n");
            }
            else mips.add("ERROR CODE");
        }

        Symbol symbol = symTable.findSym(String.valueOf(item1.var));
        if (symbol == null) {
            curOffset += 4;
            newSymbol.offset = curOffset;
            symTable.putSym(String.valueOf(item1.var), newSymbol);
            //s1
            mips.add("sw $s1, -" + curOffset + "($fp)" + "\n");
        }
        else {
            mips.add("sw $s1, -" + symbol.offset + "($fp)" + "\n");
        }
    }

    private static long getUnsignedInt (int data){     //将int数据转换为0~4294967295 (0xFFFFFFFF即DWORD)。
        return data & 0x0FFFFFFFF;
    }

    private static int maxLog(int num) {
        int x = 0;
        if ((int) (Math.log(num)/Math.log(2)) < Math.log(num)/Math.log(2)) x = (int) (Math.log(num)/Math.log(2)) + 1;
        return Math.max(x, 1);
    }
}
