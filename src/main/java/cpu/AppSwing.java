package cpu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppSwing extends JFrame {
    private final CPU cpu = new CPU();
    private final Timer timer = new Timer(60, e -> doStep());

    private final JLabel pc  = bold("PC=000");
    private final JLabel ir  = bold("IR=0x00");
    private final JLabel acc = bold("ACC=0");
    private final JLabel z   = bold("Z=0");

    private final JComboBox<String> exampleBox = new JComboBox<>();
    private final JButton btLoad  = new JButton("Carregar");
    private final JButton btStep  = new JButton("Step");
    private final JButton btRun   = new JButton("Run");
    private final JButton btPause = new JButton("Pause");
    private final JButton btReset = new JButton("Reset");

    private final DefaultTableModel memModel = new DefaultTableModel(16, 16) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable memTable = new JTable(memModel);

    private final JTextArea expl = new JTextArea(6, 40);
    private final Map<String, String> examples = new LinkedHashMap<>();

    public AppSwing() {
        super("Simulador Educativo de CPU");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel title = new JLabel("Simulador Educativo de CPU");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        chipify(pc,  new Color(225,240,255));
        chipify(ir,  new Color(225,240,255));
        chipify(acc, new Color(220,255,220));
        chipify(z,   new Color(255,240,220));

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stats.setOpaque(false);
        stats.add(pc); stats.add(ir); stats.add(acc); stats.add(z);

        exampleBox.setPrototypeDisplayValue("Ex.: 5+3 → MEM[10]             ");
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
        top.add(titleRow);
        top.add(Box.createVerticalStrut(6));
        top.add(stats);
        top.add(Box.createVerticalStrut(8));
        top.add(controls);

        setupMemoryTable();

        expl.setEditable(false);
        expl.setLineWrap(true);
        expl.setWrapStyleWord(true);
        expl.setBorder(BorderFactory.createTitledBorder("Explicação do passo"));
        expl.setFont(new Font("Consolas", Font.PLAIN, 13));

        JPanel simPanel = new JPanel(new BorderLayout(8, 8));
        simPanel.add(top, BorderLayout.NORTH);
        simPanel.add(new JScrollPane(memTable), BorderLayout.CENTER);
        simPanel.add(new JScrollPane(expl), BorderLayout.SOUTH);

        JTabbedPane abas = new JTabbedPane();
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

        btLoad.addActionListener(e -> {
            cpu.reset();
            loadSelectedProgram();
            refreshUI();
            expl.setText(
                    "Programa carregado.\n" +
                            "Use Step para executar uma instrução por vez, ou Run para executar automaticamente até HALT.\n\n"
            );
        });
        btStep.addActionListener(e -> doStep());
        btRun.addActionListener(e -> timer.start());
        btPause.addActionListener(e -> timer.stop());
        btReset.addActionListener(e -> {
            timer.stop();
            cpu.reset();
            refreshUI();
            expl.setText("CPU e memória resetadas.\n\n");
        });

        refreshUI();
        setSize(980, 610);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void doStep() {
        if (cpu.halted) {
            timer.stop();
            explain("HALT — fim do programa.\n" + stateLine());
            refreshUI();
            return;
        }
        String line = cpu.step();
        explain(prettyLog(line));
        refreshUI();
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
        b.setBackground(bg);
        b.setFocusPainted(false);
    }

    private void chipify(JLabel l, Color bg) {
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(new Color(30,30,30));
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
            final Color pcColor = new Color(255,255,170);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int addr = row * 16 + column;
                c.setBackground(addr == cpu.PC ? pcColor : Color.WHITE);
                ((JLabel)c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void seedExamples() {
        examples.put("Ex.: 5+3 → MEM[10]", """
            LOADI 5
            ADDI  3
            STORE 10
            HALT
        """);
        examples.put("Ex.: Ler MEM[30], somar 5 → MEM[31]", """
            LOADM 30
            ADDI  5
            STORE 31
            HALT
        """);
        examples.put("Ex.: JZ (zero desvia)", """
            LOADI 0
            JZ    10
            LOADI 2
            STORE 50
            JMP   14
            LOADI 1
            STORE 50
            HALT
        """);
        examples.put("Ex.: Contagem (3,2,1,0 → MEM[21..24])", """
            LOADI 3
            STORE 21
            SUBI  1
            STORE 22
            SUBI  1
            STORE 23
            SUBI  1
            STORE 24
            HALT
        """);
    }

    private void loadSelectedProgram() {
        String key = (String) exampleBox.getSelectedItem();
        String prog = examples.getOrDefault(key, "");
        int[] bin = Assembler.assemble(prog);
        for (int i = 0; i < cpu.mem.length; i++) cpu.mem[i] = 0;
        cpu.mem[30] = 7;
        System.arraycopy(bin, 0, cpu.mem, 0, bin.length);
    }

    private void loadUserProgram(String userCode) {
        try {
            timer.stop();
            String src = (userCode == null ? "" : userCode.replace("\r\n","\n").trim());
            if (src.isBlank()) {
                JOptionPane.showMessageDialog(this,
                        "O programa está vazio. Exemplo:\n\nLOADI 5\nADDI 3\nSTORE 10\nHALT",
                        "Programa vazio", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Monta com suporte a variáveis (dados a partir do endereço 200)
            Assembler.AsmOut out = Assembler.assembleWithVars(src, 200);

            cpu.halted = false;
            cpu.reset(); // zera registradores + memória

            // copia código
            System.arraycopy(out.code, 0, cpu.mem, 0, Math.min(out.code.length, cpu.mem.length));
            // inicializa dados
            for (Map.Entry<Integer,Integer> e : out.dataInits.entrySet()) {
                int addr = e.getKey();
                int val  = e.getValue();
                if (addr >= 0 && addr < cpu.mem.length) cpu.mem[addr] = val & 0xFF;
            }

            refreshUI();
            expl.setText(
                    "Programa do usuário carregado com sucesso!\n" +
                            "Use Step ou Run para executar.\n\n" +
                            "Dump [0..31]: " + dumpBytes(0, 32) + "\n\n"
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao montar o programa:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
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
        return String.format("Estado: PC=%03d | IR=0x%02X | ACC=%d | Z=%d",
                cpu.PC, cpu.IR, cpu.ACC, cpu.Z);
    }

    private String prettyLog(String log) {
        String titulo;
        if (log.contains("LOADI"))      titulo = "LOADI — carrega valor imediato no ACC";
        else if (log.contains("LOADM")) titulo = "LOADM — lê da memória (endereço → ACC)";
        else if (log.contains("STORE")) titulo = "STORE — grava o ACC na memória";
        else if (log.contains("ADDI"))  titulo = "ADDI — soma valor ao ACC (ULA)";
        else if (log.contains("SUBI"))  titulo = "SUBI — subtrai valor do ACC (ULA)";
        else if (log.startsWith("JMP")) titulo = "JMP — desvio incondicional do PC";
        else if (log.startsWith("JZ"))  titulo = "JZ — desvia se a flag Z==1";
        else if (log.startsWith("HALT"))return "HALT — fim do programa.\n" + stateLine();
        else                            titulo = "Passo";
        return titulo + "\n" +
                "  " + stateLine() + "\n" +
                "  Ação: " + log;
    }

    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
            UIManager.put("Component.arc", 14);
            UIManager.put("Button.arc", 14);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ScrollBar.thumbArc", 12);
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(AppSwing::new);
    }
}
