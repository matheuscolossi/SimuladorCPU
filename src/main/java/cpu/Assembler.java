package cpu;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assembler {

    /** Saída com código e dados inicializados (endereço -> valor). */
    public static class AsmOut {
        public final int[] code;
        public final Map<Integer,Integer> dataInits;
        public AsmOut(int[] code, Map<Integer,Integer> dataInits) {
            this.code = code; this.dataInits = dataInits;
        }
    }

    // ===== API antiga (continua funcionando p/ exemplos prontos) =====
    /** Converte texto assembly simples (somente números) em bytes (0..255). */
    public static int[] assemble(String src) {
        String[] lines = src.split("\\R");
        List<Integer> out = new ArrayList<>();

        for (String raw : lines) {
            String line = stripComments(raw).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String m = normalizeMnemonic(parts[0]);

            switch (m) {
                case "LOADI" -> { out.add(CPU.LOADI); out.add(parseNumber(parts, line)); }
                case "LOADM" -> { out.add(CPU.LOADM); out.add(parseNumber(parts, line)); }
                case "STORE" -> { out.add(CPU.STORE); out.add(parseNumber(parts, line)); }
                case "ADDI"  -> { out.add(CPU.ADDI);  out.add(parseNumber(parts, line)); }
                case "SUBI"  -> { out.add(CPU.SUBI);  out.add(parseNumber(parts, line)); }
                case "ADDM"  -> { out.add(CPU.ADDM);  out.add(parseNumber(parts, line)); }
                case "SUBM"  -> { out.add(CPU.SUBM);  out.add(parseNumber(parts, line)); }
                case "JMP"   -> { out.add(CPU.JMP);   out.add(parseNumber(parts, line)); }
                case "JZ"    -> { out.add(CPU.JZ);    out.add(parseNumber(parts, line)); }
                case "IN"    -> out.add(CPU.IN);
                case "OUT"   -> out.add(CPU.OUT);
                case "HALT"  -> out.add(CPU.HALT);
                default -> throw new IllegalArgumentException("Mnemônico inválido: " + m);
            }
        }

        int[] bytes = new int[out.size()];
        for (int i = 0; i < out.size(); i++) bytes[i] = out.get(i) & 0xFF;
        return bytes;
    }

    // ===== API nova: suporta símbolos/variáveis (A, DEC 0) =====
    /** Monta código e inicializações de dados. dataBase = endereço inicial das variáveis (ex.: 200). */
    public static AsmOut assembleWithVars(String src, int dataBase) {
        String[] lines = src.split("\\R");

        // 1) Primeira passada: localizar variáveis (A, DEC 0) e calcular tamanho do código
        Pattern varPat = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*,?\\s+DEC\\s+(-?\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
        Map<String,Integer> symbols = new LinkedHashMap<>();
        Map<Integer,Integer> dataInits = new LinkedHashMap<>();
        int pc = 0;
        int nextData = dataBase;

        for (String raw : lines) {
            String line = stripComments(raw).trim();
            if (line.isEmpty()) continue;

            Matcher m = varPat.matcher(line);
            if (m.matches()) {
                String name = m.group(1).toUpperCase(Locale.ROOT);
                int val = clampByte(Integer.parseInt(m.group(2)));
                if (symbols.containsKey(name))
                    throw new IllegalArgumentException("Símbolo duplicado: " + name);
                if (nextData > 255) throw new IllegalArgumentException("Sem espaço para dados (estouro de memória).");
                symbols.put(name, nextData);
                dataInits.put(nextData, val);
                nextData++;
                continue;
            }

            String[] parts = line.split("\\s+");
            String mn = normalizeMnemonic(parts[0]);

            // tamanho de cada instrução
            if (needsArg(mn)) pc += 2; else pc += 1;
            if (pc > 256) throw new IllegalArgumentException("Código excede 256 bytes.");
        }

        // 2) Segunda passada: gerar código, resolvendo símbolos
        List<Integer> code = new ArrayList<>();
        for (String raw : lines) {
            String line = stripComments(raw).trim();
            if (line.isEmpty()) continue;

            Matcher m = varPat.matcher(line);
            if (m.matches()) continue; // já processado: é dado

            String[] parts = line.split("\\s+");
            String mn = normalizeMnemonic(parts[0]);

            switch (mn) {
                case "LOADI" -> { code.add(CPU.LOADI); code.add(parseOperand(parts, symbols, line)); }
                case "LOADM", "LOAD" -> { code.add(CPU.LOADM); code.add(parseOperand(parts, symbols, line)); }
                case "STORE" -> { code.add(CPU.STORE); code.add(parseOperand(parts, symbols, line)); }
                case "ADDI"  -> { code.add(CPU.ADDI);  code.add(parseOperand(parts, symbols, line)); }
                case "SUBI"  -> { code.add(CPU.SUBI);  code.add(parseOperand(parts, symbols, line)); }
                case "ADDM", "ADD" -> { code.add(CPU.ADDM);  code.add(parseOperand(parts, symbols, line)); }
                case "SUBM", "SUB" -> { code.add(CPU.SUBM);  code.add(parseOperand(parts, symbols, line)); }
                case "JMP"   -> { code.add(CPU.JMP);   code.add(parseOperand(parts, symbols, line)); }
                case "JZ"    -> { code.add(CPU.JZ);    code.add(parseOperand(parts, symbols, line)); }
                case "IN", "INPUT"  -> code.add(CPU.IN);
                case "OUT", "OUTPUT"-> code.add(CPU.OUT);
                case "HALT"  -> code.add(CPU.HALT);
                default -> throw new IllegalArgumentException("Mnemônico inválido: " + mn);
            }
        }

        int[] bytes = new int[code.size()];
        for (int i = 0; i < code.size(); i++) bytes[i] = code.get(i) & 0xFF;
        return new AsmOut(bytes, dataInits);
    }

    // ===== helpers =====
    private static boolean needsArg(String m) {
        return switch (m) {
            case "LOADI","LOADM","LOAD","STORE","ADDI","SUBI","ADDM","ADD","SUBM","SUB","JMP","JZ" -> true;
            default -> false;
        };
    }

    private static String stripComments(String raw) {
        // remove '; ...' OU ' / ...' (estilo MARIE)
        String s = raw;
        int i = s.indexOf(';');
        if (i >= 0) s = s.substring(0, i);
        int j = s.indexOf('/');
        if (j >= 0) s = s.substring(0, j);
        return s;
    }

    private static String normalizeMnemonic(String m) {
        return m.toUpperCase(Locale.ROOT);
    }

    private static int parseNumber(String[] parts, String line) {
        if (parts.length < 2) throw new IllegalArgumentException("Falta argumento em: " + line);
        int v = Integer.parseInt(parts[1]);
        if (v < 0 || v > 255) throw new IllegalArgumentException("Valor fora de 0..255: " + v);
        return v;
    }

    private static int parseOperand(String[] parts, Map<String,Integer> symbols, String line) {
        if (parts.length < 2) throw new IllegalArgumentException("Falta operando em: " + line);
        String tok = parts[1];
        try {
            int v = Integer.parseInt(tok);
            if (v < 0 || v > 255) throw new IllegalArgumentException("Endereço fora de 0..255: " + v);
            return v;
        } catch (NumberFormatException ignore) {
            Integer addr = symbols.get(tok.toUpperCase(Locale.ROOT));
            if (addr == null) throw new IllegalArgumentException("Símbolo não encontrado: " + tok);
            return addr;
        }
    }

    private static int clampByte(int v) { return v & 0xFF; }
}
