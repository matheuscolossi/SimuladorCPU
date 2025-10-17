package cpu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.table.DefaultTableCellRenderer;


public class AppSwing extends JFrame {
    // --- Núcleo ---
    private final CPU cpu = new CPU();
    private final Timer timer = new Timer(60, e -> doStep()); // ~16 steps/s

    // --- UI (topo) ---
    private final JLabel pc = bold("PC=000");
    private final JLabel ir = bold("IR=0x00");
    private final JLabel acc = bold("ACC=0");
    private final JLabel z = bold("Z=0");

    private final JComboBox<String> exampleBox = new JComboBox<>();
    private final JButton btLoad  = new JButton("Carregar");
    private final JButton btStep  = new JButton("Step");
    private final JButton btRun   = new JButton("Run");
    private final JButton btPause = new JButton("Pause");
    private final JButton btReset = new JButton("Reset");

    // --- Memória ---
    private final DefaultTableModel memModel = new DefaultTableModel(16, 16) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable memTable = new JTable(memModel);

    // --- Log / Explicação ---
    private final JTextArea expl = new JTextArea(6, 40);

    // Ex.: "Ex.1: 5+3 → MEM[10]" → programa
    private final Map<String, String> examples = new LinkedHashMap<>();

    public AppSwing() {
        super("Simulador Educativo de CPU");

        // ====== LAYOUT ======
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // Top bar (info + controles)
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.add(pc); top.add(ir); top.add(acc); top.add(z);

        exampleBox.setPrototypeDisplayValue("Ex.: 5+3 → MEM[10]          ");
        top.add(exampleBox);

        stylize(btLoad, new Color(200, 220, 255));
        stylize(btStep, new Color(230, 230, 230));
        stylize(btRun,  new Color(205, 240, 205));
        stylize(btPause,new Color(255, 235, 205));
        stylize(btReset,new Color(255, 215, 215));
        top.add(btLoad); top.add(btStep); top.add(btRun); top.add(btPause); top.add(btReset);

        add(top, BorderLayout.NORTH);

        // Memória (16x16)
        setupMemoryTable();
        add(new JScrollPane(memTable), BorderLayout.CENTER);

        // Explicação
        expl.setEditable(false);
        expl.setLineWrap(true);
        expl.setWrapStyleWord(true);
        expl.setBorder(BorderFactory.createTitledBorder("Explicação do passo"));
        add(new JScrollPane(expl), BorderLayout.SOUTH);

        // ====== EXEMPLOS ======
        seedExamples();
        for (String k : examples.keySet()) exampleBox.addItem(k);
        exampleBox.setSelectedIndex(0);

        // ====== AÇÕES ======
        btLoad.addActionListener(e -> {
            cpu.reset();
            loadSelectedProgram();
            refreshUI();
            expl.setText("Programa carregado. Clique em Step para executar uma instrução por vez,\n" +
                    "ou em Run para executar automaticamente até HALT.");
        });

        btStep.addActionListener(e -> doStep());
        btRun.addActionListener(e -> timer.start());
        btPause.addActionListener(e -> timer.stop());
        btReset.addActionListener(e -> {
            timer.stop();
            cpu.reset();
            refreshUI();
            expl.setText("CPU e memória resetadas.");
        });

        // Estado inicial
        refreshUI();
        setSize(920, 560);
        setLocationRelativeTo(null);
    }

    // ===================== LÓGICA DE PASSO =====================
    private void doStep() {
        if (cpu.halted) {
            timer.stop();
            explain("HALT: execução finalizada.");
            refreshUI();
            return;
        }
        // Executa 1 instrução
        String line = cpu.step();
        explain(fromLogToDidactic(line));
        refreshUI();
    }

    // ===================== UI HELPERS =====================
    private void refreshUI() {
        pc.setText(String.format("PC=%03d", cpu.PC));
        ir.setText(String.format("IR=0x%02X", cpu.IR));
        acc.setText("ACC=" + cpu.ACC);
        z.setText("Z=" + cpu.Z);

        // Repreenche a memória (16 x 16)
        for (int r = 0; r < 16; r++)
            for (int c = 0; c < 16; c++)
                memModel.setValueAt(cpu.mem[r * 16 + c], r, c);

        // Faz a tabela redesenhar com o destaque do PC
        memTable.repaint();
    }

    private static JLabel bold(String s) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static void stylize(JButton b, Color bg) {
        b.setBackground(bg);
        b.setFocusPainted(false);
    }

    private void setupMemoryTable() {
        // Cabeçalhos A..P
        String[] heads = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P".split(",");
        for (int c = 0; c < 16; c++) memModel.setColumnIdentifiers(heads);

        memTable.setRowHeight(22);
        memTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int c = 0; c < 16; c++) memTable.getColumnModel().getColumn(c).setPreferredWidth(48);

        // Renderer para destacar a célula apontada pelo PC
        memTable.setDefaultRenderer(Object.class, new TableCellRenderer() {
            final DefaultTableCellRenderer base = new DefaultTableCellRenderer();
            final Color pcColor = new Color(255, 255, 170);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int addr = row * 16 + column;
                if (addr == cpu.PC) {
                    c.setBackground(pcColor);
                } else {
                    c.setBackground(Color.WHITE);
                }
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

        // Semear memória quando necessário (exemplo de LOADM 30)
        for (int i = 0; i < cpu.mem.length; i++) cpu.mem[i] = 0;
        cpu.mem[30] = 7; // dado para o exemplo 2

        System.arraycopy(bin, 0, cpu.mem, 0, bin.length);
    }

    private void explain(String msg) {
        expl.append(msg + "\n");
        expl.setCaretPosition(expl.getDocument().getLength());
    }

    // Tradução de log técnico para linguagem didática
    private String fromLogToDidactic(String log) {
        // Exemplos de logs vindos do CPU.step():
        // "LOADI 5", "LOADM [30] -> ACC=7", "STORE [10] <- 8", "ADDI 3 -> ACC=8",
        // "SUBI 1 -> ACC=2", "JMP 14", "JZ -> salto para 10", "JZ ignorado", "HALT"
        if (log.contains("LOADI")) return "LOADI: carrega valor imediato no ACC. " + log;
        if (log.contains("LOADM")) return "LOADM: lê da memória no endereço indicado para o ACC. " + log;
        if (log.contains("STORE")) return "STORE: grava o ACC na memória. " + log;
        if (log.contains("ADDI"))  return "ADDI: soma valor ao ACC (ULA). " + log;
        if (log.contains("SUBI"))  return "SUBI: subtrai valor do ACC (ULA). " + log;
        if (log.startsWith("JMP")) return "JMP: desvio incondicional do PC. " + log;
        if (log.startsWith("JZ"))  return "JZ: desvia se a flag Z==1. " + log;
        if (log.startsWith("HALT"))return "HALT: fim do programa.";
        return log;
    }

    public static void main(String[] args) {
        // Tema de aparência (look and feel)
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.out.println("Tema padrão carregado.");
        }

        SwingUtilities.invokeLater(() -> new AppSwing().setVisible(true));
    }
}
