

## 申优文档

[TOC]

#### 一、整体架构

文件结构：

1. 词法分析
2. 语法分析（栈式符号表）
3. 错误处理
4. 生成抽象语法树
5. 符号表建立（栈式符号表）       
6. 中间代码生成LLVMIR
7. 目标代码生成
8. 代码优化       			

![image-20211202220921281](C:\Users\MECHREVO\AppData\Roaming\Typora\typora-user-images\image-20211202220921281.png)

流程结构：



![未命名文件 (3)](G:\Aaron\semester_5\编译\实验\申优文档\未命名文件 (3).png)

#### 二、词法分析

​	按照功能进行迭代设计，从词法、语法分为两个大类，并设计全局类来保存全局信息，在每一个功能处理当中设计相应的数据结构，必要时进行全局的调用，不设置private属性。

​	**设计优势：**

1. ​	选取合适的数据结构：

   **ArrayList：**

   Array在java中具有处理不定长数据的能力，所以用它来存储我们需要读入的源代码再合适不过了，也就是我们上文提到的inputContent，将多行程序连接成一行长长的字符串，便于我们后续的处理

   **HaspMap：**

   HaspMap的作用是为了提供查表功能，并且使用最快速的匹配法，这里用了两个HashMap分别是：singleMap和reservedMap，来储存保留字以及单个操作符，这样能在匹配到相应的保留字或者操作符时快速的获取他们的代名词

2. 读取设置：

   ![img](file:///C:\Users\MECHREVO\AppData\Local\Temp\ksohtml7604\wps1.jpg)

   ​	通过逐行读入的方式将其连接到我们的全局代码内容当中，这里使用了一个小技巧，链接的时候为了方便日后出错处理的错误位置定位，我们用小的\n来确定每一行，在后续的处理操作中也会存在一个line变量，保存当前行数（出错位置行数）。

   ​	get_ch():读取一个字符

   ​	cat_token():将目前缓冲字符连接

   ​	我这里采用的简化版的状态转换功能，并没由使用较为复杂的状态转换关系。

   **带有回溯功能的词法读入，可以进行试错！**

   ```java
   private static void catToken() {
           token += ch;
       }
   
       private static void reTrack() {
           if (ch == '\n') line--;
           indexs--;
       }
   
       private static void clearToken() {
           token = "";
       }
   
       public static void get_ch() {
           ch = Global.inputContent.charAt(indexs);
           indexs ++;
           if (ch == '\n') {
               line++;
           }
       }
   
   
   ```

#### 三、语法分析

​	**在词法分析的基础上增加了Grammer类别来处理语法问题：Grammer**

​	该类的主要功能是语法分析单元，调用Lexical语法分析保存的结果，Lexical.ouput当中的词法元素，开始进行语法分析，主要采取了递归下降的方法分析，按照文法约束设计static函数，这里需要注意的是格式化字符串部分以及整数分析部分都在词法当中完成了，这里不再设计递归函数。

​	设计优势：

1. ​	改写左递归文法：

   对于以下几个文法设计需要消除左递归问题，这里采用了文法的改写；两行一组，上行为原来的文法，下行为消除左递归后的文法

   ```java
   // LOrExp → LAndExp | LOrExp '||' LAndExp
   // LOrExp → LAndExp {'||' LAndExp}
   // LAndExp → EqExp | LAndExp '&&' EqExp
   // LAndExp → EqExp {'&&' EqExp}
   // EqExp → RelExp | EqExp ('==' | '!=') RelExp
   // EqExp → RelExp { ('==' | '!=') RelExp }
   // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
   // RelExp → AddExp { ('<' | '>' | '<=' | '>=') AddExp }
   //MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
   //MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
   ```

2. 带有回溯功能的词法读入

   这里我们首先定义两函数用来读取词法分析结果以及回溯过程；

   Getone()用来读入一个词法类型

   Retrack()将读入的词法吐出，回到上一个位置

   ![img](file:///C:\Users\MECHREVO\AppData\Local\Temp\ksohtml7604\wps2.jpg)

   随后进行递归函数定义：这里采取递归下降的办法，按照文法约束设计递归函数（列出部分函数）：

   ![img](file:///C:\Users\MECHREVO\AppData\Local\Temp\ksohtml7604\wps3.jpg)

#### 四、错误处理+动态符号表生成

​	在每一个static函数递归调用内部，语法判断的同时进行错误分析，其中错误来源有三种：语法错误、词法错误、语义错误。

词法错误：“”当中的字符串，出现非法符号等情况，直接在Lexical当中进行判断，并 将错误输出，由于Lexical结构的特性，还可以在词法分析当中将本行printf语句所需要的%d数量放在一个表中，方便后续语法分析中判断格式化数量匹配问题。

语法错误：包括基础的错误如缺少括号，缺少分号，函数调用数量不匹配等基础问题，只需要在语法分析中添加相应的符号判断即可，这里再部分区域需要使用探测结构然后回溯判断等办法。

语义错误：该项错误较为复杂，需要通过建立符号表进行判断，并且使用堆栈操作进行作用域判断，如重名问题，首先在当前作用域进行重名判断，若不重名可以直接将此定义插入到当前作用域符号表当中。如函数调用参数类型问题，也通过一层栈的调用来判断函数调用每一个占位部分的最终类型，包括常数，一维数组，二维数组等。再如return类型也需要判断是否在作用域当中，且需要为第一作用域设置特性参数判断是否需要return等。

​	**将错误处理放在语法分析当中，爱去一边分析一边处理错误的办法。新设计了一个SymbolItem类用来保存标识符信息**

​	该类别设计用来存储标识码，参数类型，返回值类型，是否函数，二维数组维数信息，函数参数数量以及类型，int类型函数的返回值是否存在（满足文法约束的最后一条return）

![img](file:///C:\Users\MECHREVO\AppData\Local\Temp\ksohtml7604\wps4.jpg)

**数据结构设计**

**因为错误处理部分需要综合语法、词法、语义等条件来设计，所以需要定义大量复杂的数据结构。**

**ArrayList：**

​	存储不定长数据，当作栈来调用。

​	这里的locallayer用来保存栈中的符号表，是用来定义block的数据结构，用来区分作用域，若不在函数内则此栈为空，也就是layersize = 0，若不为空则当前作用域必在函数当中。

​	也用来通过入栈判断是否在while语句当中

**HashMap：**

​	是重要的数据结构。

（1）用来保存每一层作用域当中的变量、常量等定义属性。

（2）用来保存全局变量、常量

（3）用来保存全局函数

以上功能当中的key皆为name。

```java
public static ArrayList<HashMap<String, SymbolItem>> localLayer = new ArrayList<>(); //语句块中的return
public static int layerSize = -1;
public static ArrayList<ArrayList<Integer>> rp = new ArrayList<>(); //函数的调用参数
public static ArrayList<Integer> cut_prase = new ArrayList<>();
public static HashMap<String, SymbolItem> globalTable = new HashMap<>();
public static HashMap<String, SymbolItem> globalFuncTable = new HashMap<>();
```



#### 五、生成抽象语法树+新动态树形结构符号表

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

​	

#### 六、中间代码（LLVMIR）生成

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

#### 七、目标代码生成

##### （1）目标代码生成策略

​	我采取`$s1`， `$s2`， `$s3`这三个寄存器进行操作计算，分别对应了中间代码当中的`item1`， `item2`，`item3`右操作数。

##### （2）存储分配

​	首先将上文中的全局类型保存在.data段当中；

​	然后将所有的字符串类型都保存在`.asciiz`当中存储在.data段；

​	在栈式操作中我使用了`$fp`， `$sp`这两个栈指针，其中 `$sp`用来统筹整个程序，`$fp`用来进行局部变量的调用。

![微信截图_20211202224452](G:\Aaron\semester_5\编译\实验\申优文档\微信截图_20211202224452.png)

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

#### 八、代码优化：

##### 8.1 常数优化：

​	考虑到文法约束的限制以及初步优化，我们可以对于Const进行预处理，如常量定义，可以直接赋值，不需要再代码生成当中在为其进行Exp过程赋值，同样的也是为了提前算出所有定义数组的长度。

​	整体思路就是对原有的AST从head开始进行遍历，遇到Const类型进开始进行递归计算，从底到上进行计算，将每一个ConstExp的值求出来，保存在当前语法树当中，方便后续代码生成阶段直接调用。

![3](G:\Aaron\semester_5\编译\实验\文档2\3.png)

​	在这里我们可以清楚的看到，这里的a已经被计算出来并将其初始值21保存在了语法树当中

![4](G:\Aaron\semester_5\编译\实验\文档2\4.png)

（1）**对表达式中出现的常数计算进行合并，在生成中间代码时优化**

​		优化前：b = 3 + 5 + a;

​		优化后：b = 8 + a;

（2）**常量在生成中间代码时，直接替换成数值**

​		优化前：const int a=10; b = 3 + 5 + a;

​		优化后：const int a=10; b = 18;

##### 8.2 Exp计算中乘除法优化：

​	主要针对ALU，涉及到指令选择、跳转优化、循环优化等。

（1）四则运算指令，如果两个操作数都是数字，直接运算将结果赋给结果操作数，不需要取到寄存器中。

（2）加减法时，一个常数，一个寄存器，则直接使用addi，subi，省略一步取寄存器。

（3）乘除法时，注意x\*1=x，1\*x=1，0/x=0，x/1=x，可以省略取寄存器和`mflo`。

（4）比较运算符，如果两个操作数都是数字，直接比较，然后无条件跳转`j`。

（5）上述运算时，使用$0寄存器表示常数0，节约一步取寄存器。

（6）取数组值和给数组赋值时，数组下标如果是常数，直接计算出正确的地址去取值即可。

（7）如果是除法表达式，除数是整数那么判断进行优化处理，因为除法占用的指令周期较长，于是可以修改成乘法以及左移算法，如果是2的幂次则采用左移处理，否则采用《Division by Invariant Integers using Multiplication》当中的整数优化，改写除法

（8）乘法表达式如果是2的幂次改为左移指令

##### 8.3 While循环块优化处理：

​	代码的运行过程中while循环块内部的指令性能消耗最大，因为大数的多次循环会导致很多代码的重复执行，若是重复执行的代码中含有死代码，那么这些死代码的性能消耗是巨大的，这里我采用了while循环块的整体优化方案，若是while当中的每一句都是死代码那么我们直接将这个循环块删除

​	如Testfle1当中的样例：

​		可以看到在main函数当中有有一个循环块不断对n进行自增操作，并且还在改变全局变量d，而在整个程序的后续执行过程中不再调用这两个被修改的值。所以我们选择将这个while块进行整体删除。

```C
int main () {
    int i = 2,j = 5;
    i = getint();
    j = getint();
    j = 7*5923/56*56 - hhh(hhh(3)) + (1+2-(89/2*36-53) /1*6-2*(45*56/85-56+35*56/4-9));
    int k = -+-5;
    int n = 10;
    while (n < k*k*k*k*k*k) {
        d = d * d % 10000;
        n = n + 1;
    }
    printf("%d, %d, %d\n", i, j, k);
    return 0;
}
```

​	首先通过`makeFunc`函数对中间代码中形成的函数块的指令进行分块，并采取Hash的方式保存 函数代码块，以便检查死代码的时候直接通过key检查相应的变量、常量调用。

![微信截图_20211203135615](G:\Aaron\semester_5\编译\实验\申优文档\微信截图_20211203135615.png)

这里使用了这三个数据结构来进行数据处理：

```java
ArrayList<Integer> whileStack = new ArrayList<>();
        ArrayList<String> thisFuncPtr = new ArrayList<>();
        ArrayList<ArrayList<Integer>> deadWhile = new ArrayList<>();
```

​	其中whileStack是为了标注嵌套块内的while，FuncPtr是为了确定当前while块所在的函数位置当中的形式参数，应为这里最复杂的会处理到函数调用的数组形式参数传递，修改函数内的数组时不能删除while，因为修改的是地址，也会将原参数相应地址内容删除；所以若在函数内有数组类型的参数在while中修改，则不能删除当前while块。

```java
HashMap<String, Integer> whileMap = new HashMap<>();
                HashMap<String, String> laMap = new HashMap<>();
```

```java
MidCode midCode = optimizeMid.get(j);
MidOp midOp = midCode.midOp;
MidItem item1 = midCode.item1;
MidItem item2 = midCode.item2;
MidItem item3 = midCode.item3;
```

​	这两种数据结构用来保存当前while块中被修改的局部变量和常量，以及laMap保存当前while块中被修改的全局变量保存在.data段的数据。

​	然后获取后方块当中中间代码的操作数进行hash命中判断：下方举例全局变量是否调用，主要的判断还包括函数调用的函数中是否使用，函数参数，以及普通的计算使用过程。

```java
MidCode midCode = optimizeMid.get(j);
MidOp midOp = midCode.midOp;
MidItem item1 = midCode.item1;
MidItem item2 = midCode.item2;
MidItem item3 = midCode.item3;

if (midOp == MidOp.LA) {
  if (whileMap.containsKey(midCode.label)) {
        flag = 1;
        break;
    }
}
```

##### 8.4 窥孔优化：

（1）MIPS存取优化处理：

​	因为设计过程中有大量对堆栈的操作，若是上一条指令修改后将新变量的值使用sw存回，下一条指令又要取出来使用，这种情况下可以省去接连使用产生的多余lw，若是寄存器一样，那么直接删除这条lw指令；若是目标不同寄存器，如：

```
sw $s1, 4($fp);
lw $s2, 4($fp);
```

那么将其修改为：

```
sw $s1, 4($fp);
move $s2, $s1
```

（2）中间代码表达式计算过程中间量优化：

​	因为长表达式计算会产生大量的中间变量，其中在上一条计算结果出现之后下一条会紧跟着使用，按照中间代码设计会在计算结果之后产生一个新的中间变量做赋值处理，这样可以把这一条赋值删除，将目标变量直接移动到最后一条计算指令：

```
ADD #4 #2 #3
ASSIGN #5 #4
```

那么将其修改为：

```
ADD #5 #2 #3
```

##### 8.5 带有常数的除法优化：

​	优化前：

```c
div $s2, $s3
mflo $s1
//或者
div $s2, $s3
mfhi $s1
```

​	优化后：

```c
li $s4, -858993459//编译器计算得出
mult $s2, $s4
mfhi $s3
addu $s1, $s2, $s3
sra $s1, $s1, 2
sge $s3, $s2, 0
subiu $s3, $s3, 1
subu $s1, $s1, $s3
xori $s1, $s1, 0
subiu $s1, $s1, 0
```

​	前文中已经处理了除c法当中都是常数的情况，或者0与1等特殊情况，但是在编译期执行当中当除法的除数是常数是也可以通过优化来完成：

​	这里借鉴了一篇论文，对除法是常数的情况进行优化，详情查询《Division by Invariant Integers using Multiplication》这篇论文。主要通过逆过程来优化除法，因为$a / b = a * 1/b$；那么我们就实现这个过程，求出1/b就好了，其中的步骤如下：

​	![3](G:\Aaron\semester_5\编译\实验\申优文档\3.png)

​	其中d是除数，n是被除数，N代表我们目标编译器的结果位数，这里采用32；其中XSIGN操作为判断d是否小于0，小于0则返回-1，否则返回0；TRUNC操作为取整；MULSH为乘法取出高32位；SRA为算术右移；EOR代表异或。

#### 四、总结与感想

​	本学期的编译课真的带给我太多预料之外的收获，不论是课堂上理论课的学习还是实验课中紧张高强度的代码任务，使我在突破自我中不断成长，从这门课开始，我也开始享受到了努力取得成果的成就感，每次完成一部分的任务都会感觉自己的努力没有白费，我选择的是用java来完成本次简易编译器的实现，也通过一系列的锻炼使我更加掌握了这门程序语言的使用，在短短半个学期就能实现6k多行的代码回想这些经历也是感触颇深。

感谢张莉老师的讲解使得我在编译原理理论课程的学习中更加系统全面的培养了计算机思维以及设计模式，从理论的角度学习了从高级语言到汇编这一过程中不前端和后端的复杂知识，锻炼了我在系统架构、工程结构中设计算法，用理论知识解决问题的能力，使得我能够在实验课程中加以运用，巩固实现工程化方法。张老师讲课的方式也是充满了魅力，所以我老坐第一排。

也感谢两位助教学长，编译课程的难度不小，非常感谢两位助教学长在整个课程阶段对我们的帮助与辅导，我们总是遇到各种各样的问题，而学长们也总是在耐心的指导我们，想尽办法帮助我们解决问题，循循善诱，引导我们不断地提升自我，特别感谢王泽学长，我经常骚扰学长，但是学长总能耐心地帮助我解决问题，特别感谢久昂学长，学长忙到深夜也会帮助我们解决问题。因为我自己也当助教，也清楚助教工作的不易，学长们也将成为我们的榜样！

