package cpu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CPUTest {
    @Test
    void soma_e_store() {
        CPU cpu = new CPU();
        String prog = "LOADI 5\n" +
                "ADDI  3\n" +
                "STORE 10\n" +
                "HALT\n";

        int[] bin = Assembler.assemble(prog);
        for (int i = 0; i < bin.length; i++) cpu.mem[i] = bin[i];
        while (!cpu.halted) cpu.step();
        assertEquals(8, cpu.mem[10]);
    }
}
