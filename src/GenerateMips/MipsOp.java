package GenerateMips;

 public enum MipsOp {
        add, addi, sub, mult, divop, mflo, mfhi, sll, beq, bne,
        bgt, //扩展指令 相当于一条ALU类指令+一条branch指令
        bge, //扩展指令 相当于一条ALU类指令+一条branch指令
        blt, //扩展指令 相当于一条ALU类指令+一条branch指令
        ble, //扩展指令 相当于一条ALU类指令+一条branch指令
        j, jal, jr, lw, sw,
        syscall, li, la,
        moveop,
        dataSeg,  //.data
        textSeg,  //.text
        asciizSeg,  //.asciiz
        globlSeg,  //.globl
        label,  //产生标号
        };