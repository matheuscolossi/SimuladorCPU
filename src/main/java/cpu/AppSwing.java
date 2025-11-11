package cpu;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppSwing extends JFrame {
    private final CPU cpu = new CPU();
    private final Timer timer = new Timer(60, e -> doStep());

    // Labels dos registradores
    private final JLabel pc  = bold("PC=000");
    private final JLabel ir  = bold("IR=0x00");
    private final JLabel acc = bold("ACC=0");
    private final JLabel z   = bold("Z=0");

    // Controles
    private final JComboBox<String> exampleBox = new JComboBox<>();
    private final JButton btLoad  = new JButton("Carregar");
    private final JButton btStep  = new JButton("Step");
    private final JButton btRun   = new JButton("Run");
    private final JButton btPause = new JButton("Pause");
    private final JButton btReset = new JButton("Reset");
    private final JToggleButton btTheme = new JToggleButton("Tema 🌓");

    // Modelo da tabela de memória
    private final DefaultTableModel memModel = new DefaultTableModel(16, 16) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable memTable = new JTable(memModel);
    private final JTextArea expl = new JTextArea(6, 40);
    private final Map<String, String> examples = new LinkedHashMap<>();

    // --- CAMPOS DO DEPURADOR VISUAL E TEMAS ---
    private final JTextPane codeViewPane = new JTextPane();
    private final Highlighter.HighlightPainter lineHighlighter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 100, 150));
    private Map<Integer, Integer> currentDebugMap = new LinkedHashMap<>();
    private Object lastHighlightTag = null;
    private String currentSourceCode = "";

    private int lastReadAddr = -1;
    private int lastWriteAddr = -1;

    // Cores para o Tema CLARO
    private final Color C_LIGHT_PC = new Color(255, 255, 170);
    private final Color C_LIGHT_READ = new Color(200, 255, 200);
    private final Color C_LIGHT_WRITE = new Color(255, 230, 190);
    private final Color C_LIGHT_CHIP_FG = new Color(30, 30, 30);

    // Cores para o Tema ESCURO
    private final Color C_DARK_PC = new Color(130, 130, 0);
    private final Color C_DARK_READ = new Color(0, 100, 0);
    private final Color C_DARK_WRITE = new Color(120, 70, 0);
    private final Color C_DARK_CHIP_FG = new Color(220, 220, 220);

    // Cores Atuais em uso
    private Color colorPC = C_LIGHT_PC;
    private Color colorRead = C_LIGHT_READ;
    private Color colorWrite = C_LIGHT_WRITE;
    // --- FIM DOS CAMPOS ---

    // Padrões de Regex para parsear o log de acesso à memória
    private static final Pattern READ_PAT = Pattern.compile("(LOADM|ADDM|SUBM) \\[(\\d+)\\]");
    private static final Pattern WRITE_PAT = Pattern.compile("STORE \\[(\\d+)\\]");


    public AppSwing() {
        super("Simulador Educativo de CPU");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel title = new JLabel("Simulador Educativo de CPU");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        // Cores iniciais (Light)
        chipify(pc,  new Color(225,240,255), C_LIGHT_CHIP_FG);
        chipify(ir,  new Color(225,240,255), C_LIGHT_CHIP_FG);
        chipify(acc, new Color(220,255,220), C_LIGHT_CHIP_FG);
        chipify(z,   new Color(255,240,220), C_LIGHT_CHIP_FG);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stats.setOpaque(false);
        stats.add(pc); stats.add(ir); stats.add(acc); stats.add(z);

        exampleBox.setPrototypeDisplayValue("Ex.: 5+3 → MEM[10]             ");

        // Cores iniciais (Light)
        modernize(btLoad,  new Color(210,230,255));
        modernize(btStep,  new Color(235,235,235));
        modernize(btRun,   new Color(200,240,200));
        modernize(btPause, new Color(255,235,205));
        modernize(btReset, new Color(255,210,210));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.setOpaque(false);
        controls.add(exampleBox);
        controls.add(btLoad); controls.add(btStep); controls.add(btRun);
        controls.add(btPause); controls.add(btReset);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);

        modernize(btTheme, new Color(230, 230, 230));
        titleRow.add(btTheme, BorderLayout.EAST);

        top.add(titleRow);
        top.add(Box.createVerticalStrut(6));
        top.add(stats);
        top.add(Box.createVerticalStrut(8));
        top.add(controls);

        setupMemoryTable(); // Configura a tabela de memória

        // Configura o painel de código do depurador
        codeViewPane.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeViewPane.setEditable(false);
        codeViewPane.setBorder(BorderFactory.createTitledBorder("Código Fonte (Read-Only)"));

        // Configura o log de explicação
        expl.setEditable(false);
        expl.setLineWrap(true);
        expl.setWrapStyleWord(true);
        expl.setBorder(BorderFactory.createTitledBorder("Explicação do passo"));
        expl.setFont(new Font("Consolas", Font.PLAIN, 13));

        // Cria o painel dividido para o depurador e o log
        JSplitPane bottomSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(codeViewPane),   // Painel de código à esquerda
                new JScrollPane(expl)            // Log de explicação à direita
        );
        bottomSplitPane.setResizeWeight(0.6); // 60% do espaço para o código

        JPanel simPanel = new JPanel(new BorderLayout(8, 8));
        simPanel.add(top, BorderLayout.NORTH);
        simPanel.add(new JScrollPane(memTable), BorderLayout.CENTER);
        simPanel.add(bottomSplitPane, BorderLayout.SOUTH); // Adiciona o painel dividido

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Simulador", simPanel);
        abas.addTab("Manual", ManualTab.build());
        abas.addTab("Editor", EditorTab.build(code -> {
            loadUserProgram(code);
            abas.setSelectedIndex(0);
        }));
        setContentPane(abas);

        seedExamples(); // Popula os exemplos
        for (String k : examples.keySet()) exampleBox.addItem(k);
        exampleBox.setSelectedIndex(0);

        // --- Listeners dos Botões ---
        btLoad.addActionListener(e -> {
            timer.stop();
            loadSelectedProgram();
            refreshUI();
            expl.setText(
                    "Programa carregado.\n" +
                            "Use Step para executar uma instrução por vez, ou Run para executar automaticamente até HALT.\n\n"
            );
            highlightCurrentPCLine(); // Destaca a primeira linha
        });
        btStep.addActionListener(e -> doStep());
        btRun.addActionListener(e -> timer.start());
        btPause.addActionListener(e -> timer.stop());
        btReset.addActionListener(e -> {
            timer.stop();
            cpu.reset();
            clearLineHighlight(); // Limpa o destaque amarelo
            codeViewPane.setText(""); // Limpa o painel de código
            currentDebugMap.clear();
            currentSourceCode = "";
            refreshUI();
            expl.setText("CPU e memória resetadas.\n\n");
        });

        // --- LISTENER DO BOTÃO DE TEMA ---
        btTheme.addActionListener(e -> {
            if (btTheme.isSelected()) {
                // --- APLICAR MODO ESCURO ---
                try {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    btTheme.setText("Tema ☀️");

                    colorPC = C_DARK_PC;
                    colorRead = C_DARK_READ;
                    colorWrite = C_DARK_WRITE;

                    Color darkChipBg = UIManager.getColor("Panel.background");
                    chipify(pc, darkChipBg, C_DARK_CHIP_FG);
                    chipify(ir, darkChipBg, C_DARK_CHIP_FG);
                    chipify(acc, darkChipBg, C_DARK_CHIP_FG);
                    chipify(z, darkChipBg, C_DARK_CHIP_FG);

                    modernize(btLoad,  null);
                    modernize(btStep,  null);
                    modernize(btRun,   null);
                    modernize(btPause, null);
                    modernize(btReset, null);
                    modernize(btTheme, null);

                    EditorTab.updateStyles(true); // Avisa o Editor

                } catch (Exception ex) {
                    System.err.println("Falha ao carregar o tema escuro");
                }
            } else {
                // --- APLICAR MODO CLARO ---
                try {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
                    btTheme.setText("Tema 🌓");

                    colorPC = C_LIGHT_PC;
                    colorRead = C_LIGHT_READ;
                    colorWrite = C_LIGHT_WRITE;

                    chipify(pc,  new Color(225,240,255), C_LIGHT_CHIP_FG);
                    chipify(ir,  new Color(225,240,255), C_LIGHT_CHIP_FG);
                    chipify(acc, new Color(220,255,220), C_LIGHT_CHIP_FG);
                    chipify(z,   new Color(255,240,220), C_LIGHT_CHIP_FG);

                    modernize(btLoad,  new Color(210,230,255));
                    modernize(btStep,  new Color(235,235,235));
                    modernize(btRun,   new Color(200,240,200));
                    modernize(btPause, new Color(255,235,205));
                    modernize(btReset, new Color(255,210,210));
                    modernize(btTheme, new Color(230, 230, 230));

                    EditorTab.updateStyles(false); // Avisa o Editor

                } catch (Exception ex) {
                    System.err.println("Falha ao carregar o tema claro");
                }
            }
            // ATUALIZA A UI E O MANUAL
            SwingUtilities.updateComponentTreeUI(this);
            ManualTab.updateStyles(); // Avisa o Manual

            // Re-aplica o highlighting de sintaxe no painel de código
            EditorTab.applyHighlighting(codeViewPane);
            // Re-aplica o destaque amarelo da linha atual
            highlightCurrentPCLine();
        });
        // --- FIM DO LISTENER ---

        refreshUI();
        setSize(980, 610);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void doStep() {
        lastReadAddr = -1;
        lastWriteAddr = -1;

        if (cpu.halted) {
            timer.stop();
            explain("HALT — fim do programa.\n" + stateLine());
            refreshUI();
            return;
        }

        // Destaca a linha ANTES de executar o passo
        highlightCurrentPCLine();

        String line = cpu.step(); // Executa UMA instrução
        parseLogForMemAccess(line);
        explain(prettyLog(line));
        refreshUI();
    }

    /** Analisa o log da CPU para identificar acessos à memória para destaque visual. */
    private void parseLogForMemAccess(String log) {
        Matcher readMatcher = READ_PAT.matcher(log);
        if (readMatcher.find()) {
            try { lastReadAddr = Integer.parseInt(readMatcher.group(2)); } catch (Exception ignored) {}
            return;
        }
        Matcher writeMatcher = WRITE_PAT.matcher(log);
        if (writeMatcher.find()) {
            try { lastWriteAddr = Integer.parseInt(writeMatcher.group(1)); } catch (Exception ignored) {}
        }
    }

    private void refreshUI() {
        pc.setText(String.format("PC=%03d", cpu.PC));
        ir.setText(String.format("IR=0x%02X", cpu.IR));
        acc.setText("ACC=" + cpu.ACC);
        z.setText("Z=" + cpu.Z);
        for (int r = 0; r < 16; r++)
            for (int c = 0; c < 16; c++)
                memModel.setValueAt(cpu.mem[r * 16 + c], r, c);
        memTable.repaint();
    }

    private static JLabel bold(String s) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private void modernize(JButton b, Color bg) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBackground(bg); // Se bg for null, usa o padrão do tema
        b.setFocusPainted(false);
    }

    private void modernize(JToggleButton b, Color bg) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBackground(bg); // Se bg for null, usa o padrão do tema
        b.setFocusPainted(false);
    }

    private void chipify(JLabel l, Color bg, Color fg) {
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
    }

    private void setupMemoryTable() {
        String[] heads = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P".split(",");
        memModel.setColumnIdentifiers(heads);
        memTable.setRowHeight(22);
        memTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int c = 0; c < 16; c++) memTable.getColumnModel().getColumn(c).setPreferredWidth(48);

        memTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            final DefaultTableCellRenderer base = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int addr = row * 16 + column;

                if (addr == lastReadAddr) {
                    c.setBackground(colorRead);
                } else if (addr == lastWriteAddr) {
                    c.setBackground(colorWrite);
                } else {
                    c.setBackground(null); // Usa o padrão do tema (claro ou escuro)
                }

                if (addr == cpu.PC) { // PC tem a prioridade mais alta
                    c.setBackground(colorPC);
                }

                ((JLabel)c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    /** Popula os exemplos pré-definidos */
    private void seedExamples() {
        examples.put("Ex.: Soma (X+Y → Z)", """
            LOAD X     / Carrega valor de X
            ADD  Y     / Soma valor de Y
            STORE Z    / Salva resultado em Z
            HALT
            / --- Dados ---
            X, DEC 5
            Y, DEC 3
            Z, DEC 0
            """);

        examples.put("Ex.: Loop (Contagem regressiva)", """
            / Conta de 3 até 1
            LOADI 3
            STORE CONTADOR
            LOOP:
            LOAD CONTADOR  / Carrega 3, depois 2, depois 1
            JZ   FIM       / Se for 0, pula para FIM
            OUT            / Mostra 3, 2, 1 no log
            SUBI 1         / ACC vira 2, 1, 0
            STORE CONTADOR / Salva 2, 1, 0
            JMP LOOP       / Volta ao início do loop
            FIM:
            HALT
            / --- Dados ---
            CONTADOR, DEC 0
            """);

        examples.put("Ex.: I/O (Entrada/Saída)", """
            IN         / Pede um valor
            ADD  DEZ   / Soma 10
            OUT        / Mostra o resultado
            HALT
            / --- Dados ---
            DEZ, DEC 10
            """);
    }

    /** Carrega o programa selecionado no ComboBox */
    private void loadSelectedProgram() {
        try {
            String key = (String) exampleBox.getSelectedItem();
            String prog = examples.getOrDefault(key, "");

            Assembler.AsmOut out = Assembler.assembleWithVars(prog, 200);

            cpu.halted = false;
            cpu.reset();
            System.arraycopy(out.code, 0, cpu.mem, 0, Math.min(out.code.length, cpu.mem.length));
            for (Map.Entry<Integer,Integer> e : out.dataInits.entrySet()) {
                int addr = e.getKey();
                int val  = e.getValue();
                if (addr >= 0 && addr < cpu.mem.length) cpu.mem[addr] = val & 0xFF;
            }

            // --- Carrega dados de depuração ---
            clearLineHighlight();
            currentSourceCode = prog;
            currentDebugMap = out.debugMap;
            codeViewPane.setText(currentSourceCode);
            EditorTab.applyHighlighting(codeViewPane);
            codeViewPane.setCaretPosition(0);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao montar o programa de exemplo:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Carrega o programa do EDITOR do usuário */
    private void loadUserProgram(String userCode) {
        try {
            timer.stop();
            String src = (userCode == null ? "" : userCode.replace("\r\n","\n").trim());
            if (src.isBlank()) {
                JOptionPane.showMessageDialog(this,
                        "O programa está vazio. Exemplo:\n\nLOAD X\nADD Y\nHALT\n\nX, DEC 5\nY, DEC 3",
                        "Programa vazio", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Assembler.AsmOut out = Assembler.assembleWithVars(src, 200);

            cpu.halted = false;
            cpu.reset();
            System.arraycopy(out.code, 0, cpu.mem, 0, Math.min(out.code.length, cpu.mem.length));
            for (Map.Entry<Integer,Integer> e : out.dataInits.entrySet()) {
                int addr = e.getKey();
                int val  = e.getValue();
                if (addr >= 0 && addr < cpu.mem.length) cpu.mem[addr] = val & 0xFF;
            }

            // --- Carrega dados de depuração ---
            clearLineHighlight();
            currentSourceCode = src;
            currentDebugMap = out.debugMap;
            codeViewPane.setText(currentSourceCode);
            EditorTab.applyHighlighting(codeViewPane);
            codeViewPane.setCaretPosition(0);

            refreshUI();
            expl.setText(
                    "Programa do usuário carregado com sucesso!\n" +
                            "Use Step ou Run para executar.\n\n" +
                            "Dump [0..31]: " + dumpBytes(0, 32) + "\n\n"
            );
            highlightCurrentPCLine(); // Destaca a primeira linha

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao montar o programa:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- MÉTODOS DE DESTAQUE DE LINHA (DEPURADOR) ---

    /** Remove o destaque de linha amarela anterior. */
    private void clearLineHighlight() {
        if (lastHighlightTag != null) {
            codeViewPane.getHighlighter().removeHighlight(lastHighlightTag);
            lastHighlightTag = null;
        }
    }

    /** Encontra e destaca a linha de código-fonte correspondente ao PC atual. */
    private void highlightCurrentPCLine() {
        clearLineHighlight(); // Remove o destaque antigo

        if (currentDebugMap == null) return;

        Integer lineToHighlight = currentDebugMap.get(cpu.PC);

        if (lineToHighlight != null) {
            try {
                int startIndex = codeViewPane.getDocument().getDefaultRootElement()
                        .getElement(lineToHighlight).getStartOffset();
                int endIndex = codeViewPane.getDocument().getDefaultRootElement()
                        .getElement(lineToHighlight).getEndOffset();

                lastHighlightTag = codeViewPane.getHighlighter().addHighlight(
                        startIndex, endIndex, lineHighlighter
                );

                // Rola o painel para que a linha destacada fique visível
                SwingUtilities.invokeLater(() -> {
                    try {
                        Rectangle viewRect = codeViewPane.modelToView(startIndex);
                        if (viewRect != null) {
                            viewRect.height = viewRect.height * 3; // Mostra contexto
                            codeViewPane.scrollRectToVisible(viewRect);
                        }
                    } catch (BadLocationException e) {
                        // ignora
                    }
                });

            } catch (BadLocationException e) {
                // ignora
            }
        }
    }

    // --- OUTROS MÉTODOS HELPER ---

    private String dumpBytes(int start, int count) {
        StringBuilder sb = new StringBuilder();
        int end = Math.min(start + count, cpu.mem.length);
        for (int i = start; i < end; i++) {
            sb.append(cpu.mem[i]);
            if (i < end - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private void explain(String msg) {
        expl.append(msg + "\n\n");
        expl.setCaretPosition(expl.getDocument().getLength());
    }

    private String stateLine() {
        return String.format("Estado: PC=%03d | IR=0x%02X | ACC=%d | Z=%d",
                cpu.PC, cpu.IR, cpu.ACC, cpu.Z);
    }

    /** Gera o log bonito */
    private String prettyLog(String log) {
        String titulo;
        if (log.contains("LOADI"))      titulo = "LOADI — carrega valor imediato no ACC";
        else if (log.contains("LOADM")) titulo = "LOADM — lê da memória (endereço → ACC)";
        else if (log.contains("STORE")) titulo = "STORE — grava o ACC na memória";
        else if (log.contains("ADDI"))  titulo = "ADDI — soma valor imediato ao ACC (ULA)";
        else if (log.contains("SUBI"))  titulo = "SUBI — subtrai valor imediato do ACC (ULA)";
        else if (log.contains("ADDM"))  titulo = "ADDM — soma valor da memória ao ACC (ULA)";
        else if (log.contains("SUBM"))  titulo = "SUBM — subtrai valor da memória do ACC (ULA)";
        else if (log.startsWith("IN"))    titulo = "IN — lê do teclado para o ACC";
        else if (log.startsWith("OUT"))   titulo = "OUT — escreve o ACC na saída (log)";
        else if (log.startsWith("JMP"))   titulo = "JMP — desvio incondicional do PC";
        else if (log.startsWith("JZ"))    titulo = "JZ — desvia se a flag Z==1";
        else if (log.startsWith("HALT"))  return "HALT — fim do programa.\n" + stateLine();
        else                              titulo = "Passo";
        return titulo + "\n" +
                "  " + stateLine() + "\n" +
                "  Ação: " + log;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 14);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ScrollBar.thumbArc", 12);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(AppSwing::new);
    }
}