
## 阶段二设计文档

[TOC]

#### 一、整体架构

​	

#### 一、代码生成前奏

##### （1）抽象语法树

​	由于在之前的编译三大部分当中是中了传统的递归下降思路，不方便后期代码维护，所以前奏部分我基于词法分析的结果进行了重新构造语法，采取了同语法分析一样的递归下降办法生成了一棵AST（抽象语法树），在原来递归的基础上做了修改。



![1](G:\Aaron\semester_5\编译\实验\文档2\1.png)

​	这里是简单的文件结构，`MakeTree`用来进行递归生成一棵树，`TreeNode`是每一个节点的实体内容，`NodeType`使用一个枚举类来抽象节点类型，这里没有使用继承关系，因为枚举很方便。

​	其中`MakeTree`的步骤同实验文档一的递归步骤基本相同

![2](G:\Aaron\semester_5\编译\实验\文档2\2.png)

​	节点信息如下

```java
package AST;

import java.util.ArrayList;

public class TreeNode {

    public NodeType nodeType;
    public String token; //name
    public String symbol;//节点文法
    public ArrayList<TreeNode> children = new ArrayList<>();
    public TreeNode father = null;
    public int num; //参数内容
    public boolean constnum = false;

    public TreeNode(NodeType tp) {
        this.nodeType = tp;
    }
    public void addTree(TreeNode node) {
        node.father = this;
        children.add(node);
    }

```

​	语法树的节点抽象类，全部源自文法约束中的非终结符，终结符统一使用`Terminal`来表示

```java
package AST;

public enum NodeType {
    COMPUNIT, CONSTDECL, VARDECL, FUNCDEF, MAINFUNCDEF, CONSTDEF, VARDEF, FUNCFPARAMS, UNARYOP,
    CONSTEXP, CONSTINITVAL, INITVAL, FUNCFPARAM, ADDEXP, EXP, STMT, MULEXP, UNARYEXP, FUNCTYPE,
    PRIMARYEXP, FUNCRPARAMS, COND, LOREXP, LANDEXP, EQEXP, RELEXP, LVAL, BLOCK, TERMINAL, NUMBER,
}
```

##### （2）符号原子以及符号表

​	这里的符号也重新构造了，和原来错误分析时的基本相同，增加了一些数据类型用来处理函数传参以及数组等情况。

```java
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
```

​	符号表设计较为简单，每一个对象中包含两个类型，一个是其最近的外层符号表，一个是使用了`HashMap`保存的符号表内所有参数的集合。

​	

```java
public HashMap<String, Symbol> symTable = new HashMap<String, Symbol>();
    public SymTable fatherTable = null;
```

##### （3）语法树剪枝，常量处理

​	考虑到文法约束的限制以及初步优化，我们可以对于Const进行预处理，如常量定义，可以直接赋值，不需要再代码生成当中在为其进行Exp过程赋值，同样的也是为了提前算出所有定义数组的长度。

​	整体思路就是对原有的AST从head开始进行遍历，遇到Const类型进开始进行递归计算，从底到上进行计算，将每一个ConstExp的值求出来，保存在当前语法树当中，方便后续代码生成阶段直接调用。

![3](G:\Aaron\semester_5\编译\实验\文档2\3.png)

​	在这里我们可以清楚的看到，这里的a已经被计算出来并将其初始值21保存在了语法树当中

![4](G:\Aaron\semester_5\编译\实验\文档2\4.png)

#### 二、中间代码（LLVMIR）生成

##### 	（1）	中间表达式对象

​	我采取了类似LlvmIR的中间代码格式，也属于一种四元式，文件结构如下：

![5](G:\Aaron\semester_5\编译\实验\文档2\5.png)

​	这里的`MidOp`保存了中间代码的操作类型：

```java
public enum MidOp {
    ADD, MINU, MUL, DIV, MOD, AND, OR, BT, BE, LT, LE, EQ, NE,
    CALL, ALLOC, LOAD, STORE, BR, RET, DEFINE, ASSIGN, LABEL,
    SHL, SHR, EXIT, GETINT, PRINT, BZ, EQZ, LA
    //br 无条件跳转
}
```

​	基本与LlvmIR相同。

​	每一种操作数都有相应的计算式：`BR`和`BZ`分别代表无条件跳转和条件跳转（等于0），需要注意的是`CALL`以及·`RET`这两个操作，分别表示函数调用以及返回函数

​	操作四元设计如下：

​	`item1`：结果

​	`item2`：左操作数

​	`item3`：右操作数

![6](G:\Aaron\semester_5\编译\实验\文档2\6.png)

​	符号化输出结果：

​		在语法树操作过程中我将会将每一条生成的中间代码`MidCode`进行对象化，然后通过重写toString函数来生成中间表达式。

```java
public String toString() {
        if (midOp == MidOp.ADD) {
            return "ADD" + item1.toString() + "=" +  item2.toString() + "+" + item3.toString() + "\n";
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
        return "ERROR\n";

    }
```

##### 	（2）变量、常量类型分配

​	在这里我主要使用了三种数据类型来保存数据：

1. var：用来保存所有的内部变量，常量等编号

2. ptr：用来作为指针记录，指向数组的第一个地址

3. num：常数

   在处理过程中，对于文法约束当中的情况进行以下处理：

   - 对于全局变量和常量我将直接将他们以数组ARRAY类型或者 GLOBALVAR类型保存在List当中。其中设计的类型都是开辟在.data段的连续地址空间，其中const类型全局常量进行初始化设定，因为后续函数和程序运行中不会对其内容进行修改；var类型全局变量会在.data段当中初始化为0，如果是初始化内容完成的全局变量，在中间代码的开头调用这个初始地址，并且按照地址进行连续赋值。

     ![7](G:\Aaron\semester_5\编译\实验\文档2\7.png)

#### 三、目标代码生成（Mips未优化版本）

##### （1）目标代码生成策略

​	我采取`$s1`， `$s2`， `$s3`这三个寄存器进行操作计算，分别对应了中间代码当中的`item1`， `item2`，`item3`右操作数，由于每一个量我都在中间代码过程中生成了`$sp`的物理偏移量，所以在进行寄存器运算前，需要先将每一个操作数通过lw指令从相应的地址取出，然后计算后存入目标地址即可。

##### （2）存储分配

​	首先将上文中的全局类型保存在.data段当中；

​	然后将所有的字符串类型都保存在`.asciiz`当中存储在.data段；

​	在栈式操作中我使用了`$fp`， `$sp`这两个栈指针，其中 `$sp`用来统筹整个程序，`$fp`用来进行局部变量的调用。

​	整个操作步骤如下：

1. 首先将.data段的信息进行定义
2. 然后定义.text段
3. 在最开始的时候将`$fp`指向 `$sp`
4. 开始继续全局赋值，定义函数类型等，注意需要在第一条函数定义的前面增加一条指令`j main`用来执行main函数
5. 在最终增加一条label：end即可

##### （3）栈的维护

​	个人认为生成目标代码中最难的部分就是在函数调用以及跳转过程中对栈的维护工作了。

1. 首先是对函数进行调用：

   需要将 `$sp`指针移动当前记录变量的offset长度，等于将当前记录保存在了栈的前半部分，后续需进行函数调用，第一个位置留给return的value的，第二个位置留给原来调用前的`$ra`，第三个位置留给原来的`$fp`也就是原来函数的`$sp`，如果是void函数则可以忽略。

   接下来就是在新的 `$sp`地址头保存`$ra`寄存器，记录函数调用的下一条返回地址，然后传参，结束后令`$fp`指向`$sp`即可进行跳转。

2. 函数返回时的维护：

   如果有返回值需要将返回值保存在`4($fp)`的位置，将`$ra`从`-4($fp)`的位置取出，再将`$fp`指向`($fp)`所保存的原来参数地址，最后再将`$fp`指向的地址赋给`$sp`就可以使用jr指令跳回了！





#### 四、总结与感想

​	本学期的编译课真的带给我太多预料之外的收获，不论是课堂上理论课的学习还是实验课中紧张高强度的代码任务，使我在突破自我中不断成长，从这门课开始，我也开始享受到了努力取得成果的成就感，每次完成一部分的任务都会感觉自己的努力没有白费，我选择的是用java来完成本次简易编译器的实现，也通过一系列的锻炼使我更加掌握了这门程序语言的使用，在短短半个学期就能实现6k多行的代码回想这些经历也是感触颇深。

感谢张莉老师的讲解使得我在编译原理理论课程的学习中更加系统全面的培养了计算机思维以及设计模式，从理论的角度学习了从高级语言到汇编这一过程中不前端和后端的复杂知识，锻炼了我在系统架构、工程结构中设计算法，用理论知识解决问题的能力，使得我能够在实验课程中加以运用，巩固实现工程化方法。张老师讲课的方式也是充满了魅力，所以我老坐第一排。

也感谢两位助教学长，编译课程的难度不小，非常感谢两位助教学长在整个课程阶段对我们的帮助与辅导，我们总是遇到各种各样的问题，而学长们也总是在耐心的指导我们，想尽办法帮助我们解决问题，循循善诱，引导我们不断地提升自我，特别感谢王泽学长，我经常骚扰学长，但是学长总能耐心地帮助我解决问题。因为我自己也当助教，也清楚助教工作的不易，学长们也将成为我们的榜样！

