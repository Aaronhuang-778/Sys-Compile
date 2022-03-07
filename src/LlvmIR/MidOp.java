package LlvmIR;

public enum MidOp {
    ADD, MINU, MUL, DIV, MOD, AND, OR, BT, BE, LT, LE, EQ, NE,
    CALL, ALLOC, LOAD, STORE, BR, RET, DEFINE, ASSIGN, LABEL,
    SHL, SHR, EXIT, GETINT, PRINT, BZ, EQZ, LA,
    ADDI, MINUI, GET0, MULSH,
    //br 无条件跳转
}
