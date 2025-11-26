package cpu;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppSwing extends JFrame {
    private final CPU cpu = new CPU();
    private final Timer timer = new Timer(80, e -> doStep());

    // UI Components
    private final StatCard cardPC  = new StatCard("PC (Counter)", "000");
    private final StatCard cardIR  = new StatCard("IR (Instruc)", "0x00");
    private final StatCard cardACC = new StatCard("ACC (Accum)", "0");
    private final StatCard cardZ   = new StatCard("Z (Zero)", "0");
    private final StatCard cardN   = new StatCard("N (Neg)", "0");

    private final JComboBox<String> exampleBox = new JComboBox<>();
    private final JButton btLoad  = new JButton("Carregar");
    private final JButton btStep  = new JButton("Step ⤵");
    private final JButton btRun   = new JButton("Run ▶");
    private final JButton btPause = new JButton("Pause ⏸");
    private final JButton btReset = new JButton("Reset ↺");
    private final JToggleButton btTheme = new JToggleButton("Tema 🌓");

    private final DefaultTableModel memModel = new DefaultTableModel(16, 16) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable memTable = new JTable(memModel);

    private final JTextArea expl = new JTextArea();
    private final JTextPane codeViewPane = new JTextPane();

    private final Map<String, String> examples = new LinkedHashMap<>();
    private Map<Integer, Integer> currentDebugMap = new LinkedHashMap<>();

    private final javax.swing.text.Highlighter.HighlightPainter lineHighlighter =
            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 235, 59, 100));
    private Object lastHighlightTag = null;
    private int lastReadAddr = -1;
    private int lastWriteAddr = -1;

    private Color colorRead = new Color(200, 255, 200);
    private Color colorWrite = new Color(255, 224, 178);
    private Color colorPC = new Color(255, 255, 141);

    private static final Pattern READ_PAT = Pattern.compile("(LOADM|ADDM|SUBM|LOAD) \\[(\\d+)\\]");
    private static final Pattern WRITE_PAT = Pattern.compile("STORE \\[(\\d+)\\]");

    private final JTabbedPane abas = new JTabbedPane();
    private JPanel topPanelRef;

    public AppSwing() {
        super("Simulador Educativo de CPU");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setupMenu();
        setupTopPanel();
        setupMemoryTable();

        // --- LAYOUT ---

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createCodePanel(), createLogPanel());
        bottomSplit.setResizeWeight(0.5);
        bottomSplit.setBorder(BorderFactory.createEmptyBorder());
        bottomSplit.setDividerSize(4);

        JScrollPane scrollTable = new JScrollPane(memTable);
        scrollTable.setBorder(BorderFactory.createEmptyBorder());
        scrollTable.getViewport().setBackground(UIManager.getColor("Table.background"));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTable, bottomSplit);
        mainSplit.setResizeWeight(0.0);

        // --- CORREÇÃO AQUI ---
        // Antes estava 480. Mudamos para 380 para dar mais espaço ao código embaixo.
        mainSplit.setDividerLocation(380);

        mainSplit.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainSplit.setDividerSize(4);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanelRef, BorderLayout.NORTH);
        mainPanel.add(mainSplit, BorderLayout.CENTER);

        abas.addTab(" Simulador ", mainPanel);
        abas.addTab(" Manual ", ManualTab.build());
        abas.addTab(" Editor ", EditorTab.build(code -> {
            loadUserProgram(code);
            abas.setSelectedIndex(0);
        }));

        setContentPane(abas);

        seedExamples();
        for (String k : examples.keySet()) exampleBox.addItem(k);

        setupListeners();

        // Aumentei a altura para 850px para caber tudo confortavelmente
        setSize(1100, 850);
        setLocationRelativeTo(null);
        applyLightTheme();
        setVisible(true);
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu("Arquivo");
        JMenuItem itemOpen = new JMenuItem("Abrir Código Fonte...");
        JMenuItem itemSave = new JMenuItem("Salvar Código...");
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

        itemSave.addActionListener(e -> FileManager.saveFile(this, EditorTab.getText()));
        itemExit.addActionListener(e -> System.exit(0));

        menuFile.add(itemOpen);
        menuFile.add(itemSave);
        menuFile.addSeparator();
        menuFile.add(itemExit);
        menuBar.add(menuFile);
        setJMenuBar(menuBar);
    }

    private void setupTopPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        statsPanel.setOpaque(false);
        statsPanel.add(cardPC);
        statsPanel.add(cardIR);
        statsPanel.add(cardACC);
        statsPanel.add(cardZ);
        statsPanel.add(cardN);
        statsPanel.setBorder(new EmptyBorder(0, 5, 10, 5));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setOpaque(false);

        exampleBox.setPreferredSize(new Dimension(200, 35));

        styleButton(btLoad, null);
        styleButton(btStep, null);
        styleButton(btRun, new Color(46, 125, 50));
        btRun.setForeground(Color.WHITE);
        styleButton(btPause, new Color(255, 143, 0));
        btPause.setForeground(Color.WHITE);
        styleButton(btReset, new Color(198, 40, 40));
        btReset.setForeground(Color.WHITE);
        styleButton(btTheme, null);

        toolbar.add(new JLabel("Exemplos:"));
        toolbar.add(exampleBox);
        toolbar.add(btLoad);
        toolbar.add(Box.createHorizontalStrut(15));
        toolbar.add(btStep);
        toolbar.add(btRun);
        toolbar.add(btPause);
        toolbar.add(btReset);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btTheme);

        topPanelRef = new JPanel(new BorderLayout());
        topPanelRef.setBorder(new EmptyBorder(15, 15, 5, 15));
        topPanelRef.add(statsPanel, BorderLayout.NORTH);
        topPanelRef.add(toolbar, BorderLayout.CENTER);
    }

    private JPanel createCodePanel() {
        codeViewPane.setFont(new Font("Consolas", Font.PLAIN, 14));
        codeViewPane.setEditable(false);

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), " 📜 Código Fonte (Execução) "));
        p.add(new JScrollPane(codeViewPane));
        return p;
    }

    private JPanel createLogPanel() {
        expl.setEditable(false);
        expl.setLineWrap(true);
        expl.setWrapStyleWord(true);
        expl.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), " ℹ️ Explicação do Passo "));
        p.add(new JScrollPane(expl));
        return p;
    }

    private void styleButton(AbstractButton b, Color bg) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (bg != null) {
            b.setBackground(bg);
            b.putClientProperty("JButton.borderColor", bg);
        }
    }

    private void setupMemoryTable() {
        String[] heads = "0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F".split(",");
        memModel.setColumnIdentifiers(heads);

        memTable.setRowHeight(28);
        memTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        memTable.setShowVerticalLines(false);
        memTable.setIntercellSpacing(new Dimension(0, 1));
        memTable.setGridColor(new Color(230,230,230));
        memTable.setFont(new Font("Monospaced", Font.PLAIN, 13));
        memTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        memTable.getTableHeader().setReorderingAllowed(false);

        for (int c = 0; c < 16; c++) {
            memTable.getColumnModel().getColumn(c).setPreferredWidth(55);
        }

        memTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int addr = row * 16 + column;

                c.setForeground(table.getForeground());

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? table.getBackground() : UIManager.getColor("Table.alternateRowColor"));
                    if (c.getBackground() == null) c.setBackground(new Color(248, 248, 248));
                }

                if (addr == lastReadAddr) {
                    c.setBackground(colorRead);
                    c.setForeground(Color.BLACK);
                } else if (addr == lastWriteAddr) {
                    c.setBackground(colorWrite);
                    c.setForeground(Color.BLACK);
                }

                if (addr == cpu.PC) {
                    c.setBackground(colorPC);
                    c.setForeground(Color.BLACK);
                    setBorder(BorderFactory.createLineBorder(Color.ORANGE, 2));
                } else {
                    setBorder(noFocusBorder);
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void setupListeners() {
        btLoad.addActionListener(e -> {
            timer.stop();
            loadSelectedProgram();
            refreshUI();
            highlightCurrentPCLine();
        });
        btStep.addActionListener(e -> doStep());
        btRun.addActionListener(e -> {
            if (!timer.isRunning()) {
                timer.start();
                btRun.setText("Rodando...");
                btRun.setEnabled(false);
                btStep.setEnabled(false);
            }
        });
        btPause.addActionListener(e -> {
            timer.stop();
            btRun.setText("Run ▶");
            btRun.setEnabled(true);
            btStep.setEnabled(true);
        });
        btReset.addActionListener(e -> {
            timer.stop();
            cpu.reset();
            clearLineHighlight();
            codeViewPane.setText("");
            currentDebugMap.clear();
            refreshUI();
            expl.setText(" Sistema resetado.\n");
            btRun.setText("Run ▶");
            btRun.setEnabled(true);
            btStep.setEnabled(true);
        });

        btTheme.addActionListener(e -> {
            if (btTheme.isSelected()) applyDarkTheme();
            else applyLightTheme();
            SwingUtilities.updateComponentTreeUI(this);
            ManualTab.updateStyles();
            EditorTab.applyHighlighting(codeViewPane);
            highlightCurrentPCLine();
        });
    }

    private void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            btTheme.setText("Tema ☀️");
            colorRead = new Color(27, 94, 32);
            colorWrite = new Color(230, 81, 0);
            colorPC = new Color(255, 214, 0);

            Color cardBg = new Color(60, 63, 65);
            cardPC.updateTheme(cardBg, Color.WHITE);
            cardIR.updateTheme(cardBg, Color.WHITE);
            cardACC.updateTheme(cardBg, Color.WHITE);
            cardZ.updateTheme(cardBg, Color.WHITE);
            cardN.updateTheme(cardBg, Color.WHITE);
            EditorTab.updateStyles(true);
        } catch (Exception ignored) {}
    }

    private void applyLightTheme() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            btTheme.setText("Tema 🌓");
            colorRead = new Color(200, 255, 200);
            colorWrite = new Color(255, 224, 178);
            colorPC = new Color(255, 245, 157);

            Color cardBg = new Color(245, 245, 245);
            cardPC.updateTheme(cardBg, new Color(50,50,50));
            cardIR.updateTheme(cardBg, new Color(50,50,50));
            cardACC.updateTheme(cardBg, new Color(50,50,50));
            cardZ.updateTheme(cardBg, new Color(50,50,50));
            cardN.updateTheme(cardBg, new Color(50,50,50));
            EditorTab.updateStyles(false);
        } catch (Exception ignored) {}
    }

    private void doStep() {
        lastReadAddr = -1; lastWriteAddr = -1;

        if (cpu.halted) {
            timer.stop();
            btRun.setText("Run ▶");
            btRun.setEnabled(true);
            btStep.setEnabled(true);
            explain("⏹ HALT encontrado. Execução finalizada.");
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
        cardPC.setValue(String.format("%03d", cpu.PC));
        cardIR.setValue(String.format("0x%02X", cpu.IR));
        cardACC.setValue(String.valueOf(cpu.ACC));
        cardZ.setValue(cpu.Z == 1 ? "1 (Sim)" : "0");
        cardN.setValue(cpu.N == 1 ? "1 (Sim)" : "0");

        if (cpu.Z == 1) cardZ.setValueColor(new Color(46, 125, 50));
        else cardZ.setValueColor(UIManager.getColor("Label.foreground"));

        if (cpu.N == 1) cardN.setValueColor(Color.RED);
        else cardN.setValueColor(UIManager.getColor("Label.foreground"));

        for (int r = 0; r < 16; r++)
            for (int c = 0; c < 16; c++)
                memModel.setValueAt(cpu.mem[r * 16 + c], r, c);
        memTable.repaint();
    }

    private static class StatCard extends JPanel {
        private final JLabel lblTitle;
        private final JLabel lblValue;

        public StatCard(String title, String initialValue) {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(8, 12, 8, 12));

            lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblTitle.setForeground(Color.GRAY);

            lblValue = new JLabel(initialValue);
            lblValue.setFont(new Font("Consolas", Font.BOLD, 22));

            add(lblTitle, BorderLayout.NORTH);
            add(lblValue, BorderLayout.CENTER);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200,200,200), 1, true),
                    new EmptyBorder(5, 10, 5, 10)
            ));
        }

        public void setValue(String v) { lblValue.setText(v); }
        public void setValueColor(Color c) { lblValue.setForeground(c); }
        public void updateTheme(Color bg, Color fg) {
            setBackground(bg);
            lblValue.setForeground(fg);
        }
    }

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
        examples.put("Ex.: Divisão (Loop)",
                "IN STORE A\n" +
                        "IN STORE B\n" +
                        "LOADI 0 STORE Q\n" +
                        "LOOP: LOAD A SUB B\n" +
                        "JN FIM\n" +
                        "STORE A\n" +
                        "LOAD Q ADDI 1 STORE Q\n" +
                        "JMP LOOP\n" +
                        "FIM: LOAD Q OUT HALT\n" +
                        "A, DEC 0\nB, DEC 0\nQ, DEC 0");
    }

    private void loadSelectedProgram() {
        try {
            String prog = examples.getOrDefault(exampleBox.getSelectedItem(), "");
            Assembler.AsmOut out = Assembler.assembleWithVars(prog, 200);
            loadToCPU(out);
            setupDebug(prog, out.debugMap);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
        }
    }

    private void loadUserProgram(String userCode) {
        try {
            timer.stop();
            if (userCode == null || userCode.trim().isEmpty()) return;
            String src = userCode.replace("\r\n","\n").trim();
            Assembler.AsmOut out = Assembler.assembleWithVars(src, 200);
            loadToCPU(out);
            setupDebug(src, out.debugMap);
            refreshUI();
            expl.setText("✅ Programa carregado com sucesso.\n");
            highlightCurrentPCLine();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
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
        codeViewPane.setText(source);
        currentDebugMap = debugMap;
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
                        Rectangle r = codeViewPane.modelToView(startIndex);
                        if(r!=null) codeViewPane.scrollRectToVisible(r);
                    } catch(Exception ignored){}
                });
            } catch (BadLocationException ignored) {}
        }
    }

    private void explain(String msg) {
        expl.setText(msg + "\n" + expl.getText());
        expl.setCaretPosition(0);
    }

    private String prettyLog(String log) {
        return " ► " + log;
    }

    public static void main(String[] args) {
        try {
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(AppSwing::new);
    }
}