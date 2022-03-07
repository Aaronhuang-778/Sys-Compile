import java.util.ArrayList;

public class SymbolItem {
    public String name;
    public int type; //0:int,1:d, 2:2d, 3:void
    public int kind; // 0:var, 1:const, 2:func
    public ArrayList<Integer> parameterTable; // int:0; arr1: 1; arr2:2; none:3
    public int addr;
    public int alreadyReturn = 0;

    public SymbolItem(String name, int addr, int kind, int type) {
        this.name = name;
        this.addr = addr;
        this.kind = kind;
        this.type = type;
        parameterTable = new ArrayList<>();
    }

    public void insertPar(int t){
        parameterTable.add(t);
    }
}
