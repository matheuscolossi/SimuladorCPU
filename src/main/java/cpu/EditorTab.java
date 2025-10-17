package cpu;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class EditorTab {
    public static JComponent build(Consumer<String> onRunProgram) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JTextArea editor = new JTextArea(14, 60);
        editor.setFont(new Font("Consolas", Font.PLAIN, 14));
        editor.setText("""
LOADI 5
ADDI  3
STORE 10
HALT
""");


        JButton btRunUser = new JButton("Executar no simulador");
        btRunUser.setBackground(new Color(205, 240, 205));
        btRunUser.addActionListener(e -> onRunProgram.accept(editor.getText()));

        panel.add(new JScrollPane(editor), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btRunUser);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }
}
