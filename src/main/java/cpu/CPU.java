package cpu;

public class CPU {
    public final int[] mem = new int[256];

    public int PC;   // Program Counter
    public int IR;   // Instruction Register
    public int ACC;  // Acumulador
    public int Z;    // Flag Zero
    public boolean halted;

    // ISA básica
    public static final int LOADI = 0x01;
    public static final int LOADM = 0x02;
    public static final int STORE = 0x03;
    public static final int ADDI  = 0x04;
    public static final int SUBI  = 0x05;
    public static final int JMP   = 0x06;
    public static final int JZ    = 0x07;
    public static final int HALT  = 0xFF;

    // Novas instruções
    public static final int ADDM  = 0x08;  // ACC <- ACC + MEM[a]
    public static final int SUBM  = 0x09;  // ACC <- ACC - MEM[a]
    public static final int IN    = 0xF0;  // Lê inteiro p/ ACC
    public static final int OUT   = 0xF1;  // Exibe ACC (via log)

    public CPU() { reset(); }

    public void reset() {
        PC = 0; IR = 0; ACC = 0; Z = 0; halted = false;
        for (int i = 0; i < mem.length; i++) mem[i] = 0;
    }

    private int to8(int v) { return v & 0xFF; }
    private void setZ(int v) { Z = (to8(v) == 0) ? 1 : 0; }
    private int clampAddr(int a) {
        if (a < 0 || a > 255) throw new IllegalArgumentException("Endereço inválido: " + a);
        return a;
    }

    /** Executa UMA instrução e retorna log do passo */
    public String step() {
        if (halted) return "HALT";
        int op = mem[PC]; IR = op;

        // Pega o PC ANTES de o incrementar, para o debugMap
        int currentPC = PC;
        PC = to8(PC + 1);

        // quem precisa de operando?
        boolean needsArg = (op == LOADI || op == LOADM || op == STORE ||
                op == ADDI  || op == SUBI  || op == JMP   || op == JZ ||
                op == ADDM  || op == SUBM);
        int arg = 0;
        if (needsArg) { arg = mem[PC]; PC = to8(PC + 1); }

        String log;
        switch (op) {
            case LOADI -> { ACC = to8(arg); setZ(ACC); log = "LOADI " + arg; }
            case LOADM -> { ACC = to8(mem[clampAddr(arg)]); setZ(ACC); log = "LOADM [" + arg + "] -> " + ACC; }
            case STORE -> { mem[clampAddr(arg)] = ACC; log = "STORE [" + arg + "] <- " + ACC; }
            case ADDI  -> { ACC = to8(ACC + arg); setZ(ACC); log = "ADDI " + arg + " -> " + ACC; }
            case SUBI  -> { ACC = to8(ACC - arg); setZ(ACC); log = "SUBI " + arg + " -> " + ACC; }
            case ADDM  -> { ACC = to8(ACC + mem[clampAddr(arg)]); setZ(ACC); log = "ADDM [" + arg + "] -> ACC=" + ACC; }
            case SUBM  -> { ACC = to8(ACC - mem[clampAddr(arg)]); setZ(ACC); log = "SUBM [" + arg + "] -> ACC=" + ACC; }
            case JMP   -> { PC = clampAddr(arg); log = "JMP " + arg; }
            case JZ    -> {
                if (Z == 1) { PC = clampAddr(arg); log = "JZ -> salto para " + arg; }
                else { log = "JZ ignorado"; }
            }
            case IN    -> {
                String s = javax.swing.JOptionPane.showInputDialog(
                        null, "Digite um inteiro:", "IN", javax.swing.JOptionPane.QUESTION_MESSAGE);
                int v = 0;
                try { if (s != null) v = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
                ACC = to8(v); setZ(ACC);
                log = "IN -> ACC=" + ACC;
            }
            case OUT   -> { log = "OUT -> ACC=" + ACC; }
            case HALT  -> { halted = true; log = "HALT"; }
            default    -> { halted = true; log = "INV 0x" + Integer.toHexString(op); }
        }

        // Retorna o PC que foi USADO para este passo (para o depurador)
        return String.format("PC=%03d | IR=0x%02X | ACC=%d | Z=%d :: %s", currentPC, op, ACC, Z, log);
    }
}