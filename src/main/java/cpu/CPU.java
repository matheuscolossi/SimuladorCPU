package cpu;

import javax.swing.JOptionPane;

public class CPU {
    public final int[] mem = new int[256];

    public int PC;
    public int IR;
    public int ACC;
    public int Z;
    public int N;
    public boolean halted;

    // ISA
    public static final int LOADI = 0x01;
    public static final int LOADM = 0x02;
    public static final int STORE = 0x03;
    public static final int ADDI  = 0x04;
    public static final int SUBI  = 0x05;
    public static final int JMP   = 0x06;
    public static final int JZ    = 0x07;
    public static final int JN    = 0x08;
    public static final int HALT  = 0xFF;
    public static final int ADDM  = 0x09;
    public static final int SUBM  = 0x0A;
    public static final int IN    = 0xF0;
    public static final int OUT   = 0xF1;

    public CPU() { reset(); }

    public void reset() {
        PC = 0; IR = 0; ACC = 0; Z = 0; N = 0; halted = false;
        for (int i = 0; i < mem.length; i++) mem[i] = 0;
    }

    private int to8(int v) { return v & 0xFF; }

    private void setFlags(int v) {
        v = to8(v);
        Z = (v == 0) ? 1 : 0;
        N = (v & 0x80) != 0 ? 1 : 0;
    }

    private int clampAddr(int a) {
        if (a < 0 || a > 255) throw new IllegalArgumentException("Endereço inválido: " + a);
        return a;
    }

    public String step() {
        if (halted) return "HALT";

        int op = mem[PC]; IR = op;
        int currentPC = PC;
        PC = to8(PC + 1);

        boolean needsArg = (op == LOADI || op == LOADM || op == STORE ||
                op == ADDI  || op == SUBI  || op == JMP   || op == JZ ||
                op == JN    || op == ADDM  || op == SUBM);
        int arg = 0;
        if (needsArg) { arg = mem[PC]; PC = to8(PC + 1); }

        String log;

        // =========================================================
        // CORREÇÃO DO SWITCH PARA JAVA 8
        // =========================================================
        switch (op) {
            case LOADI:
                ACC = to8(arg); setFlags(ACC); log = "LOADI " + arg;
                break;
            case LOADM:
                ACC = to8(mem[clampAddr(arg)]); setFlags(ACC); log = "LOADM [" + arg + "] -> " + ACC;
                break;
            case STORE:
                mem[clampAddr(arg)] = ACC; log = "STORE [" + arg + "] <- " + ACC;
                break;
            case ADDI:
                ACC = to8(ACC + arg); setFlags(ACC); log = "ADDI " + arg + " -> " + ACC;
                break;
            case SUBI:
                ACC = to8(ACC - arg); setFlags(ACC); log = "SUBI " + arg + " -> " + ACC;
                break;
            case ADDM:
                ACC = to8(ACC + mem[clampAddr(arg)]); setFlags(ACC); log = "ADDM [" + arg + "] -> ACC=" + ACC;
                break;
            case SUBM:
                ACC = to8(ACC - mem[clampAddr(arg)]); setFlags(ACC); log = "SUBM [" + arg + "] -> ACC=" + ACC;
                break;
            case JMP:
                PC = clampAddr(arg); log = "JMP " + arg;
                break;
            case JZ:
                if (Z == 1) { PC = clampAddr(arg); log = "JZ -> salto para " + arg; }
                else { log = "JZ ignorado"; }
                break;
            case JN:
                if (N == 1) { PC = clampAddr(arg); log = "JN -> salto (negativo) para " + arg; }
                else { log = "JN ignorado"; }
                break;
            case IN:
                String s = JOptionPane.showInputDialog(null, "Entrada (IN): Digite um valor:", "CPU Input", JOptionPane.QUESTION_MESSAGE);
                int v = 0;
                try { if (s != null) v = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                ACC = to8(v); setFlags(ACC);
                log = "IN -> Leu " + v;
                break;
            case OUT:
                log = "OUT -> ACC = " + ACC;
                break;
            case HALT:
                halted = true; log = "HALT";
                break;
            default:
                halted = true; log = "INV 0x" + Integer.toHexString(op);
        }
        // =========================================================

        return String.format("PC=%03d | IR=0x%02X | ACC=%d | Z=%d | N=%d :: %s", currentPC, op, ACC, Z, N, log);
    }
}