package GenerateMips;

import java.util.ArrayList;

public class GlobalRegister {
    public int t2 = -1;
    public int t3 = -1;
    public int t4 = -1;
    public int t5 = -1;
    public int t6 = -1;
    public int t7 = -1;
    public int t8 = -1;
    public int t9 = -1;
    public int s3 = -1;
    public int s4 = -1;
    public int s5 = -1;
    public int s6 = -1;
    public int s7 = -1;
    public ArrayList<Integer> register = new ArrayList<>();

    public GlobalRegister() {
        for (int i = 0; i < 13; i++) {
            register.add(-1);
        }
    }

    public boolean hasEmpty() {
        for (Integer integer: register) {
            if (integer > -1) return true;
        }
        return false;
    }

    public String insertRegister(int var) {
        for (int i = 0; i < 13; i++) {
            if (register.get(i) == -1) {
                register.set(i, var);
                if (i <= 7) return "$t" + (i+2);
                else return "$s" + (i-5);
            }
        }
        return "rrr";
    }
}
