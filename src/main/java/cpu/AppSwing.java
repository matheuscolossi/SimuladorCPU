package cpu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class AppSwing extends JFrame {
    private final CPU cpu = new CPU();
    private final JLabel pc = new JLabel("PC=000");
    private final JLabel ir = new JLabel("IR=0x00");
    private final JLabel acc = new JLabel("ACC=0");
    private final JLabel z = new JLabel("Z=0");
    private final DefaultTableModel memModel = new DefaultTableModel(16, 16);
    private final JTextArea log = new JTextArea(8, 40);
    private final Timer timer;
    private final JButton btnRun = new JButton("Run");
    private final JButton btnPause = new JButton("Pause");
    private final JButton btnStep = new JButton("Step");

    public AppSwing() {
        super("Simulador Educativo de CPU");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8,8));

        // TOP: registradores + exemplos + botões
        var top = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        top.add(pc); top.add(ir); top.add(acc); top.add(z);

        String[] nomes = {
                "Ex.1: 5+3 → MEM[10]",
                "Ex.2: 3,2,1,0 → MEM[21..24]",
                "Ex.3: JZ condicional → MEM[50]"
        };
        var exemplos = new JComboBox<>(nomes);
        var btnCarregar = new JButton("Carregar");
        top.add(exemplos); top.add(btnCarregar);

        top.add(btnStep); top.add(btnRun); top.add(btnPause);
        var btnReset = new JButton("Reset");
        top.add(btnReset);
        add(top, BorderLayout.NORTH);

        // CENTRO: memória
        JTable memTable = new JTable(memModel);
        memTable.setFillsViewportHeight(true);
        // highlight da célula do PC
        memTable.setDefaultRenderer(Object.class,
                new PcHighlighter(memTable.getDefaultRenderer(Object.class), () -> cpu.PC));
        add(new JScrollPane(memTable), BorderLayout.CENTER);

        // BAIXO: log
        log.setEditable(false);
        add(new JScrollPane(log), BorderLayout.SOUTH);

        // Timer para Run
        timer = new Timer(50, e -> doStep());

        // Ações
        btnCarregar.addActionListener(e -> {
            cpu.reset();
            String prog = switch (exemplos.getSelectedIndex()) {
                case 1 -> """
                        LOADI 3
                        STORE 21
                        SUBI  1
                        STORE 22
                        SUBI  1
                        STORE 23
                        SUBI  1
                        STORE 24
                        HALT
                        """;
                case 2 -> """
                        LOADI 0
                        JZ    10
                        LOADI 2
                        STORE 50
                        JMP   14
                        LOADI 1
                        STORE 50
                        HALT
                        """;
                default -> """
                        LOADI 5
                        ADDI  3
                        STORE 10
                        HALT
                        """;
            };
            int[] bin = Assembler.assemble(prog);
            for (int i = 0; i < bin.length; i++) cpu.mem[i] = bin[i];
            refreshUI(); log.setText("Programa carregado: " + nomes[exemplos.getSelectedIndex()] + "\n");
        });

        btnStep.addActionListener(e -> doStep());
        btnRun.addActionListener(e -> { timer.start(); setRunState(true); });
        btnPause.addActionListener(e -> { timer.stop(); setRunState(false); });
        btnReset.addActionListener(e -> { timer.stop(); cpu.reset(); refreshUI(); log.setText("Reset.\n"); setRunState(false); });

        pack();
        setSize(800, 560);
        setLocationRelativeTo(null);
        setRunState(false);
        refreshUI();
    }

    private void setRunState(boolean running) {
        btnRun.setEnabled(!running);
        btnPause.setEnabled(running);
        btnStep.setEnabled(!running);
    }

    private void doStep() {
        if (cpu.halted) { timer.stop(); log.append("HALT\n"); setRunState(false); return; }
        String line = cpu.step();
        log.append(line + "\n");
        refreshUI();
    }

    private void refreshUI() {
        pc.setText(String.format("PC=%03d", cpu.PC));
        ir.setText(String.format("IR=0x%02X", cpu.IR));
        acc.setText("ACC=" + cpu.ACC);
        z.setText("Z=" + cpu.Z);

        // Memória 16x16
        for (int r = 0; r < 16; r++)
            for (int c = 0; c < 16; c++)
                memModel.setValueAt(cpu.mem[r*16 + c], r, c);
    }

    // Renderer para destacar o PC
    private static class PcHighlighter implements TableCellRenderer {
        private final TableCellRenderer base;
        private final java.util.function.IntSupplier pcSupplier;
        PcHighlighter(TableCellRenderer base, java.util.function.IntSupplier pcSupplier) {
            this.base = base; this.pcSupplier = pcSupplier;
        }
        @Override public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = base.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            int addr = row * 16 + col;
            int pc = pcSupplier.getAsInt();
            if (addr == pc) c.setBackground(new Color(255, 255, 180));
            else c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppSwing().setVisible(true));
    }
}
