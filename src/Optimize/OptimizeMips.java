package Optimize;

import LlvmIR.MidCode;

import java.util.ArrayList;
import java.util.Objects;

public class OptimizeMips {
    public static ArrayList<String> optimizeMips= new ArrayList<>();

    public static ArrayList<String> StartOptimize(ArrayList<String> mips) {
        optimizeMips.addAll(mips);

        //
        optSWandLW();
        return optimizeMips;
    }

    private static void optSWandLW() {
        for (int i = 0; i < optimizeMips.size() - 1; i++) {
            String [] str = optimizeMips.get(i).split(" ");
            String [] str1 = optimizeMips.get(i + 1).split(" ");
            if (Objects.equals(str[0], "sw") && Objects.equals(str1[0], "lw") ) {
                if (Objects.equals(str[2], str1[2]) && Objects.equals(str[1], str1[1])) {
                    optimizeMips.remove(i + 1);
                }
                else if (Objects.equals(str[2], str1[2])){
                    optimizeMips.set(i + 1, "move " + str1[1] + str[1].replace(",", "") + "\n");
                }
            }
        }
    }
}
