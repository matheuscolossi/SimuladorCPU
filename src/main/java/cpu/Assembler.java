package cpu;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assembler {

    public static class AsmOut {
        public final int[] code;
        public final Map<Integer,Integer> dataInits;
        public final Map<Integer,Integer> debugMap;

        public AsmOut(int[] code, Map<Integer,Integer> dataInits, Map<Integer,Integer> debugMap) {
            this.code = code;
            this.dataInits = dataInits;
            this.debugMap = debugMap;
        }
    }

    private static final Pattern varPat = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*,?\\s+DEC\\s+(-?\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern codeLabelPat = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*):\\s*$", Pattern.CASE_INSENSITIVE);

    public static int[] assemble(String src) {
        return assembleWithVars(src, 200).code;
    }

    public static AsmOut assembleWithVars(String src, int dataBase) {
        String[] lines = src.split("\\R");
        Map<String,Integer> symbols = new LinkedHashMap<>();
        Map<Integer,Integer> dataInits = new LinkedHashMap<>();
        Map<Integer,Integer> debugMap = new LinkedHashMap<>();

        int pc = 0;
        int nextData = dataBase;
        int lineNumber = 0;

        // Passada 1
        for (String raw : lines) {
            lineNumber++;
            String line = stripComments(raw).trim();
            if (line.isEmpty()) continue;

            Matcher mLabel = codeLabelPat.matcher(line);
            if (mLabel.matches()) {
                String name = mLabel.group(1).toUpperCase(Locale.ROOT);
                if (symbols.containsKey(name)) throw new IllegalArgumentException("Símbolo duplicado: " + name);
                symbols.put(name, pc);
                continue;
            }

            Matcher mVar = varPat.matcher(line);
            if (mVar.matches()) {
                String name = mVar.group(1).toUpperCase(Locale.ROOT);
                int val = clampByte(Integer.parseInt(mVar.group(2)));
                if (symbols.containsKey(name)) throw new IllegalArgumentException("Símbolo duplicado: " + name);
                symbols.put(name, nextData);
                dataInits.put(nextData, val);
                nextData++;
                continue;
            }

            debugMap.put(pc, lineNumber - 1);
            String[] parts = line.split("\\s+");
            String mn = normalizeMnemonic(parts[0]);
            if (needsArg(mn)) pc += 2; else pc += 1;
            if (pc > 256) throw new IllegalArgumentException("Código excede 256 bytes.");
        }

        // Passada 2
        List<Integer> code = new ArrayList<>();
        for (String raw : lines) {
            String line = stripComments(raw).trim();
            if (line.isEmpty()) continue;
            if (codeLabelPat.matcher(line).matches()) continue;
            if (varPat.matcher(line).matches()) continue;

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
                case "JN"    -> { code.add(CPU.JN);    code.add(parseOperand(parts, symbols, line)); }
                case "IN", "INPUT"  -> code.add(CPU.IN);
                case "OUT", "OUTPUT"-> code.add(CPU.OUT);
                case "HALT"  -> code.add(CPU.HALT);
                default -> throw new IllegalArgumentException("Mnemônico inválido: " + mn);
            }
        }

        int[] bytes = new int[code.size()];
        for (int i = 0; i < code.size(); i++) bytes[i] = code.get(i) & 0xFF;
        return new AsmOut(bytes, dataInits, debugMap);
    }

    private static boolean needsArg(String m) {
        return switch (m) {
            case "LOADI","LOADM","LOAD","STORE","ADDI","SUBI","ADDM","ADD","SUBM","SUB","JMP","JZ","JN" -> true;
            default -> false;
        };
    }

    private static String stripComments(String raw) {
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