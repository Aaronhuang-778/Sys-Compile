import AST.MakeTree;
import GenerateMips.BuildMips;
import LlvmIR.LLvmIR;
import LlvmIR.MidCode;
import Optimize.OptimizeAst;
import Optimize.OptimizeExp;
import Optimize.OptimizeMips;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class Compiler {

    public static void main(String[] args) throws Exception{
        String inputFile = "testfile.txt";
        String outputFile = "midCode.txt";
        String mipsFile = "mips.txt";
        //读入文件
        FileReader fileReader = new FileReader(inputFile);
        FileWriter fileWriter = new FileWriter(outputFile);
        FileWriter mipsWriter = new FileWriter(mipsFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        BufferedWriter MipsWriter = new BufferedWriter(mipsWriter);

        String line = "";
        line = bufferedReader.readLine();
        //放置字符串
        while (line != null) {
            line += "\n";
            Global.inputContent += line;
            line = bufferedReader.readLine();
        }
        //标识符和类型设置
        Global.setReservedMap();
        Global.setSingleMap();
        int re;
        for (int i = 0; i <= 2000; i++) {
            Global.error_output.add("");
        }
        while (Lexical.indexs < Global.inputContent.length()) {
            re = Lexical.getSym();
        }
        //语法分析错误处理
//        Grammer.CompUnit();
        //建立抽象语法树
        Global.head = MakeTree.ParseTree(Lexical.output);
        System.out.println("开始建立符号表:");
        //动态符号表：基于树
//        SymbolAnalyse.StartAnalyse(Global.head);
        //语法树优化：常量优化
        OptimizeAst .StartOptimize(Global.head);
        //打印树
        System.out.println(Global.head.toString());
        //生成llvm
        LLvmIR.StartMakeIR(Global.head);


        ArrayList<MidCode> newMidcodes = OptimizeExp.StartOptimize(LLvmIR.midCodes);

        for (MidCode midCode : newMidcodes) {
            bufferedWriter.write(midCode.toString() + "\r\n");
        }
        bufferedWriter.close();

        for (String s : LLvmIR.data) {
            System.out.println(s);
        }



        BuildMips.StartMakeMips(newMidcodes, LLvmIR.data, OptimizeExp.recNumber);
        ArrayList<String> newMips = OptimizeMips.StartOptimize(BuildMips.mips);

        for (String s : newMips) {
            MipsWriter.write(s);
        }

        bufferedReader.close();
        fileReader.close();
        fileWriter.close();
        MipsWriter.close();
    }

    public static void op() {
        //        for (String i : Lexical.output) {
//            System.out.println(i);
//        }


        //        Iterator iter = SymbolAnalyse.symTable.symTable.entrySet().iterator();
//        while (iter.hasNext()) {
//            Map.Entry entry = (Map.Entry) iter.next();
//            Object key = entry.getKey();
//            Object value = entry.getValue();
//            System.out.println(key);
//
//        }
//        System.out.println("局部符号表:=========");
//        for (String item : Global.LayerTable.keySet()) {
//            System.out.println("这个函数是： " + item);
//            for (String name : Global.LayerTable.get(item).keySet()) {
//                System.out.println(name);
//            }
//        }
//        System.out.println("全局函数表");
//        for (String item : Global.globalFuncTable.keySet()) {
//            System.out.println(item + " " +Global.globalFuncTable.get(item).parameterTable.size());
//        }
//
//        for (String item : Grammer.output) {
//            bufferedWriter.write(item + "\r\n");
//        }

//        BufferedWriter bufferedWriterError = new BufferedWriter(errorWriter);
//        System.out.println("==================");
//        for (String item : Global.error_output) {
//            if (!Objects.equals(item, "")) {
//                System.out.println(item);
//                bufferedWriterError.write(item + "\r\n");
//            }
//        }
    }

}
