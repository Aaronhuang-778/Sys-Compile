package LlvmIR;

import java.util.ArrayList;

public class MidItem {
    public MidType midType;
    public int var;
    public int num;

    public int frequence = 0;

    public MidItem() {
    }

    @Override
    public String toString() {
        if (midType == MidType.VAR) {
            return "\t#" + var;
        }
        else if (midType == MidType.NUM) {
            return "\t" + num;
        }
        else return "\t*" + var;
    }

}
