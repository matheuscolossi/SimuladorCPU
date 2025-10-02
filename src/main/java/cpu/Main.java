package cpu;

public class Main {
    public static void main(String[] args) {
        String prog = """
            LOADI 5
            ADDI  3
            STORE 10
            HALT
            """;

        CPU cpu = new CPU();
        int[] bin = Assembler.assemble(prog);
        for (int i = 0; i < bin.length; i++) cpu.mem[i] = bin[i];

        System.out.println("=== Execução ===");
        int steps = 0;
        while (!cpu.halted && steps < 100) {
            System.out.println(cpu.step());
            steps++;
        }
        System.out.println("=== Fim ===");
        System.out.println("Resultado em MEM[10] = " + cpu.mem[10]);
    }
}
