package SymbolFile;

import LlvmIR.MidItem;

import java.util.ArrayList;

public class Symbol {
    public  String name;
    public  SymType symType;
    public  SymType returnType;
    public int var;
    public  int length;
    public  int offset;
    public boolean isGlobal = false;
    public String label;
    public MidItem midItem = new MidItem();
    public  ArrayList<Integer> constNum;
    public  ArrayList<SymType> paramTypes;

    public Symbol() {

    }
    public Symbol(String name) {
        this.name = name;
    }
    public Symbol(String name, SymType symType) {
        this.name = name;
        this.symType = symType;
    }
}

