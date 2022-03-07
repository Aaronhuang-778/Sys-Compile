package Optimize;

import LlvmIR.MidCode;
import LlvmIR.MidItem;
import LlvmIR.MidOp;
import LlvmIR.MidType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class OptimizeExp {
    public static ArrayList<MidCode> optimizeMid = new ArrayList<>();
    public static HashMap<String, ArrayList<MidCode>> funcCode = new HashMap<>();
    public static ArrayList<MidItem> recNumber = new ArrayList<>();

    public static ArrayList<MidCode> StartOptimize(ArrayList<MidCode> midCodes) {
        optimizeMid.addAll(midCodes);
        //引用计数
        recFrequence();
        //表达式计算化简
        optExp();
        //函数分块
        makeFunc();
        //while冗余块优化
        optWhile();
        return optimizeMid;
    }

    private static void recFrequence() {
        for (int i = 0; i < optimizeMid.size() - 1; i++) {
            MidCode mc = optimizeMid.get(i);
            MidOp midOp = mc.midOp;
            MidItem item1 = mc.item1;
            MidItem item2 = mc.item2;
            MidItem item3 = mc.item3;
            if (midOp == MidOp.ASSIGN && item1.midType == MidType.VAR) {
                int flag = 0;
                for (MidItem midItem : recNumber) {
                    if (midItem.var == item1.var) {
                       flag = 1;
                    }
                }
                if (flag == 0) {
                    item1.frequence = 1;
                    recNumber.add(item1);
                }
            }
            else {
                if (item1 != null) {
                    for (MidItem midItem : recNumber) {
                        if (midItem.var == item1.var) {
                            midItem.frequence += 1;
                        }
                    }
                }
                else if (item2 != null) {
                    for (MidItem midItem : recNumber) {
                        if (midItem.var == item2.var) {
                            midItem.frequence += 1;
                        }
                    }
                }
                else if (item3 != null) {
                    for (MidItem midItem : recNumber) {
                        if (midItem.var == item3.var) {
                            midItem.frequence += 1;
                        }
                    }
                }
            }
        }
        Collections.sort(recNumber,new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((MidItem) o2).frequence - ((MidItem) o1).frequence;
            }
        });
//        for (MidItem midItem : recNumber) {
//            System.out.println(midItem.toString());
//        }
    }

    private static void optExp() {
        //赋值化简
        for (int i = 0; i < optimizeMid.size() - 1; i++) {
            MidCode mc0 = optimizeMid.get(i);
            MidCode mc1 = optimizeMid.get(i + 1);
            if (mc0.midOp == MidOp.ADD || mc0.midOp == MidOp.MINU || mc0.midOp == MidOp.DIV || mc0.midOp == MidOp.MUL
                || mc0.midOp == MidOp.MINUI || mc0.midOp == MidOp.ADDI) {
                if (mc1.midOp == MidOp.ASSIGN && mc0.item1.var == mc1.item2.var) {
                    mc0.item1 = mc1.item1;
                    optimizeMid.remove(i+1);
                }
            }
        }
        //常数化简
        for (int i = 0; i < optimizeMid.size(); i++) {
            MidCode mc = optimizeMid.get(i);
            if (mc.midOp == MidOp.ADD || mc.midOp == MidOp.MINU) {
                int op = -1;
                if (mc.midOp == MidOp.ADD) op = 1;
                else op = 2;
                if (mc.item2.midType == MidType.NUM && mc.item3.midType == MidType.NUM) {
                    MidCode assign = new MidCode(MidOp.ASSIGN);
                    MidItem number = new MidItem();
                    number.midType = MidType.NUM;
                    if (op == 1) {
                        number.num = mc.item2.num + mc.item3.num;
                    }
                    else {
                        number.num = mc.item2.num - mc.item3.num;
                    }
                    assign.insertItem(mc.item1, 1);
                    assign.insertItem(number, 2);
                    optimizeMid.set(i, assign);
                }
                else if (mc.item2.midType == MidType.NUM) {
                    if (op == 1) {
                        MidItem temp = mc.item2;
                        mc.item2 = mc.item3;
                        mc.item3 = temp;
                        mc.midOp = MidOp.ADDI;
                    }
                }
                else if (mc.item3.midType == MidType.NUM) {
                    if (op == 1) {
                        mc.midOp = MidOp.ADDI;
                    }
                    else {
                        mc.midOp = MidOp.MINUI;
                    }
                }
            }

            if (mc.midOp == MidOp.MUL || mc.midOp == MidOp.DIV || mc.midOp == MidOp.MOD) {
                int op = -1;
                if (mc.midOp == MidOp.MUL) op = 1;
                else if (mc.midOp == MidOp.DIV) op = 2;
                else op = 3;
                if (mc.item2.midType == MidType.NUM && mc.item3.midType == MidType.NUM) {
                    MidCode assign = new MidCode(MidOp.ASSIGN);
                    MidItem number = new MidItem();
                    number.midType = MidType.NUM;
                    if (op == 1) {
                        number.num = mc.item2.num * mc.item3.num;
                    }
                    else if (op == 2){
                        number.num = mc.item2.num / mc.item3.num;
                    }
                    else {
                        number.num = mc.item2.num % mc.item3.num;
                    }
                    assign.insertItem(mc.item1, 1);
                    assign.insertItem(number, 2);
                    optimizeMid.set(i, assign);
                }
                //除法的化简
                else if (op == 2) {
                    if (mc.item3.midType == MidType.NUM && mc.item3.num == 1) {
                        MidCode assign = new MidCode(MidOp.ASSIGN);
                        assign.insertItem(mc.item1, 1);
                        assign.insertItem(mc.item2, 2);
                        optimizeMid.set(i, assign);
                    }
                    else if (mc.item2.midType == MidType.NUM && mc.item2.num == 0) {
                        MidCode assign = new MidCode(MidOp.ASSIGN);
                        MidItem number = new MidItem();
                        number.midType = MidType.NUM;
                        number.num = 0;
                        assign.insertItem(mc.item1, 1);
                        assign.insertItem(number, 2);
                        optimizeMid.set(i, assign);
                    }
                    else if (mc.item3.midType == MidType.NUM ) {
                        int is2 = isPowerOf2(mc.item3.num);
                        if (is2 > 0) {
                            mc.item3.num = is2;
                            mc.midOp = MidOp.SHR;
                        }
                    }
                }
                else if (op == 3) {
                    if (mc.item3.midType == MidType.NUM && mc.item3.num == 1) {
                        MidCode assign = new MidCode(MidOp.ASSIGN);
                        MidItem number = new MidItem();
                        number.midType = MidType.NUM;
                        number.num = 0;
                        assign.insertItem(mc.item1, 1);
                        assign.insertItem(number, 2);
                        optimizeMid.set(i, assign);
                    }
                    else if (mc.item2.midType == MidType.NUM && mc.item2.num == 0) {
                        MidCode assign = new MidCode(MidOp.ASSIGN);
                        MidItem number = new MidItem();
                        number.midType = MidType.NUM;
                        number.num = 0;
                        assign.insertItem(mc.item1, 1);
                        assign.insertItem(number, 2);
                        optimizeMid.set(i, assign);
                    }
                }
                //乘法的化简
                else {
                    if (mc.item3.midType == MidType.NUM && mc.item3.num == 1) {
                        MidCode assign = new MidCode(MidOp.ASSIGN);
                        assign.insertItem(mc.item1, 1);
                        assign.insertItem(mc.item2, 2);
                        optimizeMid.set(i, assign);
                    }
                    else if (mc.item2.midType == MidType.NUM && mc.item2.num == 1) {
                        MidCode assign = new MidCode(MidOp.ASSIGN);
                        assign.insertItem(mc.item1, 1);
                        assign.insertItem(mc.item3, 2);
                        optimizeMid.set(i, assign);
                    }
                    else if (mc.item2.midType == MidType.NUM) {
                        int number = mc.item2.num;
                        int is2 = isPowerOf2(number);
                        if (is2 > 0) {
                            mc.item2.num = is2;
                            mc.midOp = MidOp.SHL;
                            MidItem temp = mc.item2;
                            mc.item2 = mc.item3;
                            mc.item3 = temp;
                        }
                    }
                    else if (mc.item3.midType == MidType.NUM) {
                        int number = mc.item3.num;
                        int is2 = isPowerOf2(number);
                        if (is2 > 0) {
                            mc.item3.num = is2;
                            mc.midOp = MidOp.SHL;
                        }
                    }
                    if ((mc.item2.midType == MidType.NUM && mc.item2.num == 0) ||
                            (mc.item3.midType == MidType.NUM && mc.item3.num == 0) ) {
                        MidCode assign = new MidCode(MidOp.ASSIGN);
                        MidItem number = new MidItem();
                        number.midType = MidType.NUM;
                        number.num = 0;
                        assign.insertItem(mc.item1, 1);
                        assign.insertItem(number, 2);
                        optimizeMid.set(i, assign);
                    }
                }
            }
        }
        //赋值化简
        for (int i = 0; i < optimizeMid.size() - 1; i++) {
            MidCode mc0 = optimizeMid.get(i);
            MidCode mc1 = optimizeMid.get(i + 1);
            if (mc0.midOp == MidOp.ADD || mc0.midOp == MidOp.MINU || mc0.midOp == MidOp.DIV || mc0.midOp == MidOp.MUL
                    || mc0.midOp == MidOp.MINUI || mc0.midOp == MidOp.ADDI) {
                if (mc1.midOp == MidOp.ASSIGN && mc0.item1.var == mc1.item2.var) {
                    mc0.item1 = mc1.item1;
                    optimizeMid.remove(i+1);
                }
            }
        }
    }



    private static void makeFunc() {
        String funcName = "";
        for (int i = 0; i < optimizeMid.size() - 1; i++) {
            if (optimizeMid.get(i).midOp == MidOp.DEFINE) {
                funcName = optimizeMid.get(i).label;
                funcCode.put(funcName, new ArrayList<>());
            }
            else if (optimizeMid.get(i).midOp == MidOp.EXIT) {
                funcName = "";
            }
            if (!Objects.equals(funcName, "")) {
                funcCode.get(funcName).add(optimizeMid.get(i));
            }
        }
    }

    private static void optWhile() {
        ArrayList<Integer> whileStack = new ArrayList<>();
        ArrayList<String> thisFuncPtr = new ArrayList<>();
        ArrayList<ArrayList<Integer>> deadWhile = new ArrayList<>();

        for (int i = 0; i < optimizeMid.size(); i++) {

            if (optimizeMid.get(i).midOp == MidOp.DEFINE) {
                thisFuncPtr = new ArrayList<>();
                for (MidItem item : optimizeMid.get(i).items) {
                    if (item.midType == MidType.PTR) {
                        thisFuncPtr.add(item.toString());

                    }
                }
            }

            if (Objects.equals(optimizeMid.get(i).label, "WhileStart")) {
                whileStack.add(i);
            }
            else if (Objects.equals(optimizeMid.get(i).label, "WhileEnd") && whileStack.size() == 1) {
                int start = whileStack.get(0);
                int end = i;
                int flag = 0;
                HashMap<String, Integer> whileMap = new HashMap<>();
                HashMap<String, String> laMap = new HashMap<>();
                for (int j = start + 1; j < end; j++) {
                    MidCode midCode = optimizeMid.get(j);
                    MidOp midOp = midCode.midOp;
                    MidItem item1 = midCode.item1;
                    MidItem item2 = midCode.item2;
                    MidItem item3 = midCode.item3;
                    if (midOp == MidOp.ADD) {
                        whileMap.put(item1.toString(), 0);
                    }
                    else if (midOp == MidOp.ADDI) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.MINUI) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.SHR) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.SHL) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.MINU) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.DIV) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.MUL) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.MOD) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.AND) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.OR) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.BT) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.BE) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.LT) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.LE) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.EQ) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.NE) {
                        whileMap.put(item1.toString(), 0);                    }
                    else if (midOp == MidOp.CALL) {
                        flag = 1;
                        break;
                    }
                    else if (midOp == MidOp.ALLOC) {
                        whileMap.put(item1.toString(), 0);
                    }
                    else if (midOp == MidOp.LOAD) {
                        whileMap.put(item3.toString(), 0);
                    }
                    else if (midOp == MidOp.STORE) {
                        if (thisFuncPtr.contains(item1.toString())) {
                            flag = 1;
                            break;
                        }
                        if (laMap.containsKey(item1.toString())) {
                            whileMap.put(laMap.get(item1.toString()), 0);
                        }
                        else whileMap.put(item1.toString(), 0);
                    }
                    else if (midOp == MidOp.RET) {
                        flag = 1;
                    }
                    else if (midOp == MidOp.ASSIGN) {
                        whileMap.put(item1.toString(), 0);
                    }
                    else if (midOp == MidOp.GETINT) {
                        flag = 1;
                        break;
                    }
                    else if (midOp == MidOp.PRINT) {
                        flag = 1;
                        break;
                    }
                    else if (midOp == MidOp.LA) {
                        laMap.put(item1.toString(), midCode.label);
                    }
                }
                if (flag == 0) {

                    Iterator iter = whileMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        Object key = entry.getKey();
                        Object value = entry.getValue();
                        System.out.println(key + ":" + value);

                    }


                        for (int j = i + 1; j < optimizeMid.size(); j++) {
                        MidCode midCode = optimizeMid.get(j);
                        MidOp midOp = midCode.midOp;
                        MidItem item1 = midCode.item1;
                        MidItem item2 = midCode.item2;
                        MidItem item3 = midCode.item3;
                        if (midOp == MidOp.PRINT) {
                            for (MidItem item : midCode.items) {
                                if (whileMap.containsKey(item.toString())) {
                                    flag = 1;
                                    break;
                                }
                            }
                        }
                        else if (midOp == MidOp.CALL) {
                            for (MidItem item : midCode.items) {
                                if (whileMap.containsKey(item.toString())) {
                                    flag = 1;
                                    break;
                                }
                            }
                            for (MidCode callFunc: funcCode.get(midCode.label)) {
                                if (callFunc.midOp == MidOp.LA) {
                                    if (whileMap.containsKey(callFunc.label)) {
                                        flag = 1;
                                        break;
                                    }
                                }
                            }
                        }
                        else if (midOp == MidOp.LA) {
                            if (whileMap.containsKey(midCode.label)) {
                                flag = 1;
                                break;
                            }
                        }
                        else if (midOp == MidOp.RET) {
                                if (item1 != null && whileMap.containsKey(item1.toString())) {
                                    flag = 1;
                                    break;
                                }

                        }
                        else if (item1 != null && whileMap.containsKey(item1.toString())) {
                            flag = 1;
                            break;
                        }
                        else if (item2 != null && whileMap.containsKey(item2.toString())) {
                            flag = 1;
                            break;
                        }
                        else if (item3 != null && whileMap.containsKey(item3.toString())) {
                            flag = 1;
                            break;
                        }
                    }
                }

                if (flag == 0) {
                    ArrayList<Integer> temp1 = new ArrayList<>();
                    temp1.add(start);
                    temp1.add(end);
                    deadWhile.add(temp1);
                }
                whileStack.remove(whileStack.size() - 1);
            }
            else if (Objects.equals(optimizeMid.get(i).label, "WhileEnd")) {
                whileStack.remove(whileStack.size() - 1);
            }

        }
        System.out.println(deadWhile.size());
        if (deadWhile.size() != 0) {
            ArrayList<MidCode> newTemp = new ArrayList<>();
            int newStart = 0;
            for (ArrayList<Integer> whileBolck : deadWhile) {
                int start = whileBolck.get(0);
                int end = whileBolck.get(1);
                System.out.println(start + "=========" + end);
                for (int i = newStart; i < optimizeMid.size(); i++) {
                    if (i < start) {
                        if (optimizeMid.get(i).midOp == MidOp.LABEL &&
                                (Objects.equals(optimizeMid.get(i).label, "WhileStart")
                                        || Objects.equals(optimizeMid.get(i).label, "WhileEnd"))){

                        }
                        else newTemp.add(optimizeMid.get(i));
                    }
                    else {
                        newStart = end + 1;
                        break;
                    }
                }
            }
            for (int i = newStart; i < optimizeMid.size(); i++) {
                if (optimizeMid.get(i).midOp == MidOp.LABEL &&
                        (optimizeMid.get(i).label == "WhileStart" || optimizeMid.get(i).label == "WhileEnd")){

                }
                else newTemp.add(optimizeMid.get(i));
            }
            optimizeMid.clear();
            optimizeMid.addAll(newTemp);
        }
        else {
            for (int i = 0; i < optimizeMid.size(); i++) {
                if (optimizeMid.get(i).midOp == MidOp.LABEL &&
                        (optimizeMid.get(i).label == "WhileStart" || optimizeMid.get(i).label == "WhileEnd")){
                    optimizeMid.remove(i);
                    i--;
                }
            }
        }
    }

    private static int isPowerOf2(int num) {
        if (num < 0) return 0;
        int power = 0;
        while (num % 2 == 0) {
            num /= 2;
            power ++;
        }
        if (num == 1) return power;
        return 0;
    }
}
