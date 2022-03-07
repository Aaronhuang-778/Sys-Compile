package LlvmIR;

import java.util.ArrayList;

public class MidCode {
    public MidOp midOp;
    public MidItem item1;
    public MidItem item2;
    public MidItem item3;

    public boolean isVoid;
    public String label;

    public ArrayList<MidItem> items = new ArrayList<>();
    public ArrayList<String> labels = new ArrayList<>();

    public MidCode() { }
    public MidCode(MidOp op) {
        this.midOp = op;
    }
    public void insertItem(MidItem item, int check) {
        if (check == 1) item1 = item;
        else if (check == 2) item2 = item;
        else if (check == 3) item3 = item;
    }

    public void setItems(ArrayList<MidItem> midItems) {
        items.addAll(midItems);
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public void setLabel(int label) {
        this.label = "Label" + Integer.toString(label);
    }
    public void setLabel(int label, String type) {
        this.label = "Label" + Integer.toString(label) + type;

    }
    @Override
    public String toString() {
        if (midOp == MidOp.ADD) {
            return "ADD" + item1.toString() + "=" +  item2.toString() + "+" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.ADDI) {
            return "ADDI" + item1.toString() + "=" +  item2.toString() + "+" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.MINUI) {
            return "MINUI" + item1.toString() + "=" +  item2.toString() + "-" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.MINU) {
            return "MINU" + item1.toString() + "=" +  item2.toString() + "-" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.DIV) {
            return "DIV" + item1.toString() + "=" +  item2.toString() + "/" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.MUL) {
            return "MUL" + item1.toString() + "=" +  item2.toString() + "*" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.MOD) {
            return "MOD" + item1.toString() + "=" +  item2.toString() + "%" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.AND) {
            return "AND" + item1.toString()  +  item2.toString() + "&&" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.OR) {
            return "OR" + item1.toString() + "=" +  item2.toString() + "||" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.BT) {
            return "BT" + item1.toString() + "=" + "(" + item2.toString() + ">" + item3.toString() + ")" + "\n";
        }
        else if (midOp == MidOp.BE) {
            return "BE" + item1.toString() + "=" + "(" + item2.toString() + ">=" + item3.toString() + ")" + "\n";
        }
        else if (midOp == MidOp.LT) {
            return "LT" + item1.toString() + "=" + "(" + item2.toString() + "<" + item3.toString() + ")" + "\n";
        }
        else if (midOp == MidOp.LE) {
            return "LE" + item1.toString() + "=" + "(" + item2.toString() + "<" + item3.toString() + ")" + "\n";
        }
        else if (midOp == MidOp.EQ) {
            return "EQ" + item1.toString() + "=" + "(" + item2.toString() + "==" + item3.toString() + ")" + "\n";
        }
        else if (midOp == MidOp.NE) {
            return "NE" + item1.toString() + "=" + "(" + item2.toString() + "!=" + item3.toString() + ")" + "\n";
        }
        else if (midOp == MidOp.CALL) {
            String call = "Call\t" + label + "\t";
            for (int i = 0; i < items.size(); i++) {
                if (i != items.size() - 1) {
                    call += items.get(i).toString() + ",";
                }
                else call += items.get(i).toString();
            }
            if (!isVoid) {
                call += "return (" + item1.toString() + ")";
            }
            call += "\n";
            return call;
        }
        else if (midOp == MidOp.ALLOC) {
            return "ALLOC" + item1.toString() + item2.toString() + "\n";
        }
        else if (midOp == MidOp.LOAD) {
            return "LOAD" + item1.toString() + item2.toString()  + item3.toString() + "\n";
        }
        else if (midOp == MidOp.STORE) {
            return "STORE" + item1.toString() + item2.toString()  + item3.toString() + "\n";
        }
        else if (midOp == MidOp.BR) {
            return "BR\t" + label + "\n";
        }
        else if (midOp == MidOp.RET) {
            if (item1 == null) {
                return "EXIT" + "\n";
            }
            return "RET" + item1.toString() + "\n";
        }
        else if (midOp == MidOp.DEFINE) {
            String define = "DEFINE\t" + label;
            for (int i = 0; i < items.size(); i++) {
                if (i != items.size() - 1) {
                    define += items.get(i).toString() + ",";
                }
                else define += items.get(i).toString();
            }
            define += "\n";
            return define;
        }
        else if (midOp == MidOp.ASSIGN) {
            return "ASSIGN" + item1.toString() + "="  + item2.toString()+ "\n";
        }
        else if (midOp == MidOp.LABEL) {
            return label + ":" + "\n";
        }
        else if (midOp == MidOp.EXIT) {
            return "EXIT" + "\n";
        }
        else if (midOp == MidOp.GETINT) {
            return "GETINT" + item1.toString() + "\n";
        }
        else if (midOp == MidOp.PRINT) {
            String print = "PRINT\t" + label;
            for (int i = 0; i < items.size(); i++) {
                if (i != items.size() - 1) {
                    print += items.get(i).toString() + ",";
                }
                else print += items.get(i).toString();
            }
            print += "\n";
            return print;
        }
        else if (midOp == MidOp.BZ) {
            return "BZ" + item1.toString() + "\t" + label + "\n";
        }
        else if (midOp == MidOp.EQZ) {
            return "EQZ" + item1.toString() + "= !" + item2.toString() + "\n";
        }
        else if (midOp == MidOp.LA) {
            return "LA" + item1.toString() + "\t" + label + "\n";
        }
        else if (midOp == MidOp.SHR) {
            return "SHR" + item1.toString() + "=" +  item2.toString() + ">>" + item3.toString() + "\n";
        }
        else if (midOp == MidOp.SHL) {
            return "SHL" + item1.toString() + "=" +  item2.toString() + "<<" + item3.toString() + "\n";
        }
        return "ERROR\n";

    }
}
