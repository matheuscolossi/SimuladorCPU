package cpu;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorTab {

    // --- Estilos ---
    private static StyleContext styleContext = StyleContext.getDefaultStyleContext();
    private static AttributeSet STYLE_DEFAULT;
    private static AttributeSet STYLE_MNEMONIC;
    private static AttributeSet STYLE_LABEL_VAR;
    private static AttributeSet STYLE_NUMBER;
    private static AttributeSet STYLE_COMMENT;

    // Cores Light
    private static final Color C_LIGHT_DEFAULT = Color.BLACK;
    private static final Color C_LIGHT_MNEMONIC = new Color(0, 0, 190);
    private static final Color C_LIGHT_LABEL_VAR = new Color(128, 0, 128);
    private static final Color C_LIGHT_NUMBER = new Color(20, 120, 20);
    private static final Color C_LIGHT_COMMENT = new Color(200, 100, 0); // Orange/Rust

    // Cores Dark
    private static final Color C_DARK_DEFAULT = new Color(220, 220, 220);
    private static final Color C_DARK_MNEMONIC = new Color(130, 180, 255);
    private static final Color C_DARK_LABEL_VAR = new Color(220, 140, 255);
    private static final Color C_DARK_NUMBER = new Color(140, 220, 140);
    private static final Color C_DARK_COMMENT = new Color(255, 180, 100); // Light Orange

    // RegEx
    private static final String[] MNEMONICS = {
            "LOADI", "LOADM", "LOAD", "STORE", "ADDI", "SUBI", "ADDM", "ADD", "SUBM", "SUB",
            "JMP", "JZ", "JN", "IN", "INPUT", "OUT", "OUTPUT", "HALT"
    };
    private static final Pattern MNEMONIC_PATTERN = Pattern.compile("\\b(" + String.join("|", MNEMONICS) + ")\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LABEL_VAR_PATTERN = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)(:|\\s*,\\s*DEC)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b(-?\\d+)\\b");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("[;/].*");

    private static JTextPane editorPane;


    public static void updateStyles(boolean isDark) {
        if (isDark) {
            STYLE_DEFAULT = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_DARK_DEFAULT);
            STYLE_MNEMONIC = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_DARK_MNEMONIC);
            STYLE_LABEL_VAR = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_DARK_LABEL_VAR);
            STYLE_NUMBER = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_DARK_NUMBER);
            STYLE_COMMENT = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_DARK_COMMENT);
        } else {
            STYLE_DEFAULT = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_LIGHT_DEFAULT);
            STYLE_MNEMONIC = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_LIGHT_MNEMONIC);
            STYLE_LABEL_VAR = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_LIGHT_LABEL_VAR);
            STYLE_NUMBER = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_LIGHT_NUMBER);
            STYLE_COMMENT = styleContext.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, C_LIGHT_COMMENT);
        }
        applyHighlighting(editorPane);
    }

    public static void setText(String text) {
        if (editorPane != null) {
            editorPane.setText(text);
            applyHighlighting(editorPane);
        }
    }

    public static String getText() {
        return editorPane != null ? editorPane.getText() : "";
    }

    public static JComponent build(Consumer<String> onRunProgram) {
        updateStyles(false);
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        editorPane = new JTextPane();
        editorPane.setFont(new Font("Consolas", Font.PLAIN, 14));

        // =========================================================
        // CORREÇÃO DOS TEXT BLOCKS PARA JAVA 8
        // =========================================================
        editorPane.setText(
                "/ Exemplo: Divisão Robusta\n"
                        + "IN STORE A / Dividendo\n"
                        + "IN STORE B / Divisor\n"
                        + "LOADI 0 STORE Q\n"
                        + "LOOP:\n"
                        + "LOAD A SUB B\n"
                        + "JN FIM / Pula se for negativo\n"
                        + "STORE A\n"
                        + "LOAD Q ADDI 1 STORE Q\n"
                        + "JMP LOOP\n"
                        + "FIM:\n"
                        + "LOAD Q OUT / Mostra Quociente\n"
                        + "LOAD A OUT / Mostra Resto\n"
                        + "HALT\n"
                        + "A, DEC 0\n"
                        + "B, DEC 0\n"
                        + "Q, DEC 0\n"
        );
        // =========================================================

        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> applyHighlighting(editorPane)); }
            @Override public void removeUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> applyHighlighting(editorPane)); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        SwingUtilities.invokeLater(() -> applyHighlighting(editorPane));

        JButton btRunUser = new JButton("Executar no simulador");
        btRunUser.setBackground(new Color(205, 240, 205));
        btRunUser.addActionListener(e -> onRunProgram.accept(editorPane.getText()));

        panel.add(new JScrollPane(editorPane), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btRunUser);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    public static void applyHighlighting(JTextPane pane) {
        if (pane == null) return;
        int caretPos = pane.getCaretPosition();
        StyledDocument doc = pane.getStyledDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) { return; }

        doc.setCharacterAttributes(0, text.length(), STYLE_DEFAULT, true);
        findAndApply(doc, text, MNEMONIC_PATTERN, STYLE_MNEMONIC);
        findAndApply(doc, text, LABEL_VAR_PATTERN, STYLE_LABEL_VAR);
        findAndApply(doc, text, NUMBER_PATTERN, STYLE_NUMBER);
        findAndApply(doc, text, COMMENT_PATTERN, STYLE_COMMENT);
        pane.setCaretPosition(caretPos);
    }

    private static void findAndApply(StyledDocument doc, String text, Pattern pattern, AttributeSet style) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, true);
        }
    }
}