package cpu;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
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

    // UI Components - Registradores
    private final JLabel pc  = bold("PC=000");
    private final JLabel ir  = bold("IR=0x00");
    private final JLabel acc = bold("ACC=0");
    private final JLabel z   = bold("Z=0");
    private final JLabel n   = bold("N=0");

    // Controles
    private final JComboBox<String> exampleBox = new JComboBox<>();
    private final JButton btLoad  = new JButton("Carregar");
    private final JButton btStep  = new JButton("Step");
    private final JButton btRun   = new JButton("Run");
    private final JButton btPause = new JButton("Pause");
    private final JButton btReset = new JButton("Reset");
    private final JToggleButton btTheme = new JToggleButton("Tema 🌓");

    // Tabela de Memória
    private final DefaultTableModel memModel = new DefaultTableModel(16, 16) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable memTable = new JTable(memModel);

    // Log e Depurador
    private final JTextArea expl = new JTextArea(6, 40);
    private final JTextPane codeViewPane = new JTextPane();

    // Mapas e Dados
    private final Map<String, String> examples = new LinkedHashMap<>();
    private Map<Integer, Integer> currentDebugMap = new LinkedHashMap<>();
    private String currentSourceCode = "";

    // Destaques Visuais
    private final Highlighter.HighlightPainter lineHighlighter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 255, 100, 150));
    private Object lastHighlightTag = null;
    private int lastReadAddr = -1;
    private int lastWriteAddr = -1;

    // --- CORES (Temas) ---
    private final Color C_LIGHT_PC = new Color(255, 255, 170);
    private final Color C_LIGHT_READ = new Color(200, 255, 200);
    private final Color C_LIGHT_WRITE = new Color(255, 230, 190);
    private final Color C_LIGHT_CHIP_FG = new Color(30, 30, 30);

    private final Color C_DARK_PC = new Color(130, 130, 0);
    private final Color C_DARK_READ = new Color(0, 100, 0);
    private final Color C_DARK_WRITE = new Color(120, 70, 0);
    private final Color C_DARK_CHIP_FG = new Color(220, 220, 220);

    private Color colorPC = C_LIGHT_PC;
    private Color colorRead = C_LIGHT_READ;
    private Color colorWrite = C_LIGHT_WRITE;

    // Regex para log
    private static final Pattern READ_PAT = Pattern.compile("(LOADM|ADDM|SUBM) \\[(\\d+)\\]");
    private static final Pattern WRITE_PAT = Pattern.compile("STORE \\[(\\d+)\\]");

    private final JTabbedPane abas = new JTabbedPane();

    public AppSwing() {
        super("Simulador Educativo de CPU");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // --- 1. BARRA DE MENUS ---
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu("Arquivo");
        JMenuItem itemOpen = new JMenuItem("Abrir...");
        JMenuItem itemSave = new JMenuItem("Salvar como...");
        JMenuItem itemExit = new JMenuItem("Sair");

        itemOpen.setAccelerator(KeyStroke.getKeyStroke("control O"));
        itemSave.setAccelerator(KeyStroke.getKeyStroke("control S"));

        itemOpen.addActionListener(e -> {
            String content = FileManager.openFile(this);
            if (content != null) {
                EditorTab.setText(content);
                loadUserProgram(content);
                abas.setSelectedIndex(2);
            }
        });

        itemSave.addActionListener(e -> {
            String content = EditorTab.getText();
            FileManager.saveFile(this, content);
        });

        itemExit.addActionListener(e -> System.exit(0));

        menuFile.add(itemOpen);
        menuFile.add(itemSave);
        menuFile.addSeparator();
        menuFile.add(itemExit);
        menuBar.add(menuFile);
        setJMenuBar(menuBar);

        // --- 2. PAINEL SUPERIOR (STATS E CONTROLES) ---
        JLabel title = new JLabel("Simulador Educativo de CPU");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        chipify(pc,  new Color(225,240,255), C_LIGHT_CHIP_FG);
        chipify(ir,  new Color(225,240,255), C_LIGHT_CHIP_FG);
        chipify(acc, new Color(220,255,220), C_LIGHT_CHIP_FG);
        chipify(z,   new Color(255,240,220), C_LIGHT_CHIP_FG);
        chipify(n,   new Color(255,220,220), C_LIGHT_CHIP_FG);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stats.setOpaque(false);
        stats.add(pc); stats.add(ir); stats.add(acc); stats.add(z); stats.add(n);

        exampleBox.setPrototypeDisplayValue("Ex.: Soma (X+Y → Z)             ");

        modernize(btLoad,  new Color(210,230,255));
        modernize(btStep,  new Color(235,235,235));
        modernize(btRun,   new Color(200,240,200));
        modernize(btPause, new Color(255,235,205));
        modernize(btReset, new Color(255,210,210));
        modernize(btTheme, new Color(230, 230, 230));

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
        titleRow.add(btTheme, BorderLayout.EAST);

        top.add(titleRow);
        top.add(Box.createVerticalStrut(6));
        top.add(stats);
        top.add(Box.createVerticalStrut(8));
        top.add(controls);

        setupMemoryTable();

        // --- 3. LAYOUT CENTRAL (SPLIT PANES) ---
        codeViewPane.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeViewPane.setEditable(false);
        codeViewPane.setBorder(BorderFactory.createTitledBorder("Código Fonte (Read-Only)"));

        expl.setEditable(false);
        expl.setLineWrap(true);
        expl.setWrapStyleWord(true);
        expl.setBorder(BorderFactory.createTitledBorder("Explicação do passo"));
        expl.setFont(new Font("Consolas", Font.PLAIN, 13));

        JSplitPane bottomSplitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(codeViewPane),
                new JScrollPane(expl)
        );
        bottomSplitPane.setResizeWeight(0.5);

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(memTable),
                bottomSplitPane
        );
        mainSplit.setResizeWeight(0.5);

        JPanel simPanel = new JPanel(new BorderLayout(8, 8));
        simPanel.add(top, BorderLayout.NORTH);
        simPanel.add(mainSplit, BorderLayout.CENTER);

        // --- 4. ABAS ---
        abas.addTab("Simulador", simPanel);
        abas.addTab("Manual", ManualTab.build());
        abas.addTab("Editor", EditorTab.build(code -> {
            loadUserProgram(code);
            abas.setSelectedIndex(0);
        }));
        setContentPane(abas);

        seedExamples();
        for (String k : examples.keySet()) exampleBox.addItem(k);
        exampleBox.setSelectedIndex(0);

        // --- 5. LISTENERS ---
        btLoad.addActionListener(e -> {
            timer.stop();
            loadSelectedProgram();
            refreshUI();
            expl.setText("Programa carregado.\n");
            highlightCurrentPCLine();
        });
        btStep.addActionListener(e -> doStep());
        btRun.addActionListener(e -> timer.start());
        btPause.addActionListener(e -> timer.stop());
        btReset.addActionListener(e -> {
            timer.stop();
            cpu.reset();
            clearLineHighlight();
            codeViewPane.setText("");
            currentDebugMap.clear();
            currentSourceCode = "";
            refreshUI();
            expl.setText("CPU e memória resetadas.\n\n");
        });

        btTheme.addActionListener(e -> {
            if (btTheme.isSelected()) applyDarkTheme();
            else applyLightTheme();

            SwingUtilities.updateComponentTreeUI(this);
            ManualTab.updateStyles();
            EditorTab.applyHighlighting(codeViewPane);
            highlightCurrentPCLine();
        });

        refreshUI();
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // --- LÓGICA DE TEMAS ---
    private void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            btTheme.setText("Tema ☀️");
            colorPC = C_DARK_PC; colorRead = C_DARK_READ; colorWrite = C_DARK_WRITE;
            Color darkChipBg = UIManager.getColor("Panel.background");
            chipify(pc, darkChipBg, C_DARK_CHIP_FG);
            chipify(ir, darkChipBg, C_DARK_CHIP_FG);
            chipify(acc, darkChipBg, C_DARK_CHIP_FG);
            chipify(z, darkChipBg, C_DARK_CHIP_FG);
            chipify(n, darkChipBg, C_DARK_CHIP_FG);
            modernize(btLoad, null); modernize(btStep, null); modernize(btRun, null);
            modernize(btPause, null); modernize(btReset, null); modernize(btTheme, null);
            EditorTab.updateStyles(true);
        } catch (Exception ignored) {}
    }

    private void applyLightTheme() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            btTheme.setText("Tema 🌓");
            colorPC = C_LIGHT_PC; colorRead = C_LIGHT_READ; colorWrite = C_LIGHT_WRITE;
            chipify(pc, new Color(225,240,255), C_LIGHT_CHIP_FG);
            chipify(ir, new Color(225,240,255), C_LIGHT_CHIP_FG);
            chipify(acc, new Color(220,255,220), C_LIGHT_CHIP_FG);
            chipify(z, new Color(255,240,220), C_LIGHT_CHIP_FG);
            chipify(n, new Color(255,220,220), C_LIGHT_CHIP_FG);
            modernize(btLoad, new Color(210,230,255)); modernize(btStep, new Color(235,235,235));
            modernize(btRun, new Color(200,240,200)); modernize(btPause, new Color(255,235,205));
            modernize(btReset, new Color(255,210,210)); modernize(btTheme, new Color(230, 230, 230));
            EditorTab.updateStyles(false);
        } catch (Exception ignored) {}
    }

    // --- LÓGICA DE EXECUÇÃO ---
    private void doStep() {
        lastReadAddr = -1; lastWriteAddr = -1;

        if (cpu.halted) {
            timer.stop();
            explain("HALT — fim do programa.\n" + stateLine());
            refreshUI();
            return;
        }

        highlightCurrentPCLine();
        String line = cpu.step();
        parseLogForMemAccess(line);
        explain(prettyLog(line));
        refreshUI();
    }

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
        n.setText("N=" + cpu.N);
        for (int r = 0; r < 16; r++)
            for (int c = 0; c < 16; c++)
                memModel.setValueAt(cpu.mem[r * 16 + c], r, c);
        memTable.repaint();
    }

    // --- MÉTODOS AUXILIARES UI ---
    private static JLabel bold(String s) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private void modernize(JButton b, Color bg) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBackground(bg);
        b.setFocusPainted(false);
    }
    private void modernize(JToggleButton b, Color bg) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBackground(bg);
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

                if (addr == lastReadAddr) c.setBackground(colorRead);
                else if (addr == lastWriteAddr) c.setBackground(colorWrite);
                else c.setBackground(null);

                if (addr == cpu.PC) c.setBackground(colorPC);

                ((JLabel)c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    // --- CARREGAMENTO DE PROGRAMAS ---
    private void seedExamples() {
        examples.put("Ex.: Soma (X+Y → Z)",
                "LOAD X     / Carrega valor de X\n"
                        + "ADD  Y     / Soma valor de Y\n"
                        + "STORE Z    / Salva resultado em Z\n"
                        + "HALT\n"
                        + "/ --- Dados ---\n"
                        + "X, DEC 5\n"
                        + "Y, DEC 3\n"
                        + "Z, DEC 0\n"
        );
        examples.put("Ex.: Divisão Robusta (JN)",
                "IN STORE A / Dividendo\n"
                        + "IN STORE B / Divisor\n"
                        + "LOADI 0 STORE Q\n"
                        + "LOOP:\n"
                        + "LOAD A SUB B\n"
                        + "JN FIM\n"
                        + "STORE A\n"
                        + "LOAD Q ADDI 1 STORE Q\n"
                        + "JMP LOOP\n"
                        + "FIM:\n"
                        + "LOAD Q OUT\n"
                        + "HALT\n"
                        + "A, DEC 0\n"
                        + "B, DEC 0\n"
                        + "Q, DEC 0\n"
        );
    }

    private void loadSelectedProgram() {
        try {
            String prog = examples.getOrDefault(exampleBox.getSelectedItem(), "");
            Assembler.AsmOut out = Assembler.assembleWithVars(prog, 200);
            loadToCPU(out);
            setupDebug(prog, out.debugMap);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUserProgram(String userCode) {
        try {
            timer.stop();
            // Correção Java 8: userCode.isBlank() -> userCode.trim().isEmpty()
            if (userCode == null || userCode.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Programa vazio", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String src = userCode.replace("\r\n","\n").trim();
            Assembler.AsmOut out = Assembler.assembleWithVars(src, 200);

            loadToCPU(out);
            setupDebug(src, out.debugMap);
            refreshUI();
            expl.setText("Programa carregado.\nDump [0..31]: " + dumpBytes(0, 32) + "\n");
            highlightCurrentPCLine();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadToCPU(Assembler.AsmOut out) {
        cpu.halted = false;
        cpu.reset();
        System.arraycopy(out.code, 0, cpu.mem, 0, Math.min(out.code.length, cpu.mem.length));
        for (Map.Entry<Integer,Integer> e : out.dataInits.entrySet()) {
            int addr = e.getKey();
            if (addr >= 0 && addr < cpu.mem.length) cpu.mem[addr] = e.getValue() & 0xFF;
        }
    }

    private void setupDebug(String source, Map<Integer,Integer> debugMap) {
        clearLineHighlight();
        currentSourceCode = source;
        currentDebugMap = debugMap;
        codeViewPane.setText(currentSourceCode);
        EditorTab.applyHighlighting(codeViewPane);
        codeViewPane.setCaretPosition(0);
    }

    private void clearLineHighlight() {
        if (lastHighlightTag != null) {
            codeViewPane.getHighlighter().removeHighlight(lastHighlightTag);
            lastHighlightTag = null;
        }
    }

    private void highlightCurrentPCLine() {
        clearLineHighlight();
        if (currentDebugMap == null) return;
        Integer lineToHighlight = currentDebugMap.get(cpu.PC);
        if (lineToHighlight != null) {
            try {
                int startIndex = codeViewPane.getDocument().getDefaultRootElement().getElement(lineToHighlight).getStartOffset();
                int endIndex = codeViewPane.getDocument().getDefaultRootElement().getElement(lineToHighlight).getEndOffset();
                lastHighlightTag = codeViewPane.getHighlighter().addHighlight(startIndex, endIndex, lineHighlighter);
                SwingUtilities.invokeLater(() -> {
                    try {
                        Rectangle viewRect = codeViewPane.modelToView(startIndex);
                        if (viewRect != null) {
                            viewRect.height *= 3;
                            codeViewPane.scrollRectToVisible(viewRect);
                        }
                    } catch (BadLocationException ignored) {}
                });
            } catch (BadLocationException ignored) {}
        }
    }

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
        return String.format("Estado: PC=%03d | IR=0x%02X | ACC=%d | Z=%d | N=%d", cpu.PC, cpu.IR, cpu.ACC, cpu.Z, cpu.N);
    }

    private String prettyLog(String log) {
        String titulo;
        if (log.contains("LOADI"))      titulo = "LOADI — carrega valor imediato";
        else if (log.contains("LOADM")) titulo = "LOADM — lê da memória";
        else if (log.contains("STORE")) titulo = "STORE — grava na memória";
        else if (log.contains("ADDI"))  titulo = "ADDI — soma imediato";
        else if (log.contains("SUBI"))  titulo = "SUBI — subtrai imediato";
        else if (log.contains("ADDM"))  titulo = "ADDM — soma memória";
        else if (log.contains("SUBM"))  titulo = "SUBM — subtrai memória";
        else if (log.startsWith("JMP"))   titulo = "JMP — desvio incondicional";
        else if (log.startsWith("JZ"))    titulo = "JZ — desvio se Zero (Z=1)";
        else if (log.startsWith("JN"))    titulo = "JN — desvio se Negativo (N=1)";
        else if (log.startsWith("IN"))    titulo = "IN — entrada de dados";
        else if (log.startsWith("OUT"))   titulo = "OUT — saída de dados";
        else if (log.startsWith("HALT"))  return "HALT — fim do programa.\n" + stateLine();
        else                              titulo = "Passo";

        // =========================================================
        // CORREÇÃO DO ERRO DE DIGITAÇÃO (E')
        // =========================================================
        return titulo + "\n  " + stateLine() + "\n  Ação: " + log;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 14);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ScrollBar.thumbArc", 12);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(AppSwing::new);
    }
}