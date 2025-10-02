package cpu;

import java.util.ArrayList;
import java.util.List;

public class Assembler {
    /** Converte texto assembly em bytes (int 0..255). */
    public static int[] assemble(String src) {
        String[] lines = src.split("\\R");
        List<Integer> out = new ArrayList<>();

        for (String raw : lines) {
            String line = raw.replaceAll(";.*", "").trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String m = parts[0].toUpperCase();

            switch (m) {
                case "LOADI" -> { out.add(CPU.LOADI); out.add(parse(parts, line)); }
                case "LOADM" -> { out.add(CPU.LOADM); out.add(parse(parts, line)); }
                case "STORE" -> { out.add(CPU.STORE); out.add(parse(parts, line)); }
                case "ADDI"  -> { out.add(CPU.ADDI);  out.add(parse(parts, line)); }
                case "SUBI"  -> { out.add(CPU.SUBI);  out.add(parse(parts, line)); }
                case "JMP"   -> { out.add(CPU.JMP);   out.add(parse(parts, line)); }
                case "JZ"    -> { out.add(CPU.JZ);    out.add(parse(parts, line)); }
                case "HALT"  -> out.add(CPU.HALT);
                default -> throw new IllegalArgumentException("Mnemônico inválido: " + m);
            }
        }

        int[] bytes = new int[out.size()];
        for (int i = 0; i < out.size(); i++) bytes[i] = out.get(i) & 0xFF;
        return bytes;
    }

    private static int parse(String[] parts, String line) {
        if (parts.length < 2) throw new IllegalArgumentException("Falta argumento em: " + line);
        int v = Integer.parseInt(parts[1]);
        if (v < 0 || v > 255) throw new IllegalArgumentException("Valor fora de 0..255: " + v);
        return v;
    }
}
