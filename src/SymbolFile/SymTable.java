package SymbolFile;

import java.util.HashMap;

public class SymTable {
    public HashMap<String, Symbol> symTable = new HashMap<String, Symbol>();
    public SymTable fatherTable = null;


    public SymTable(SymTable symTable) {
        this.fatherTable = symTable;
    }
    public SymTable() {
    }


    public void putSym(String name, Symbol symbol) {
        symTable.put(name, symbol);
    }

    public Symbol findSym(String symName) {
        for (SymTable temp = this; temp != null; temp = temp.fatherTable) {
          if (temp.symTable.containsKey(symName)) {
              return temp.symTable.get(symName);
          }
        }
        return null;
    }

}
