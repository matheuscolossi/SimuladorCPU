package cpu;

import javax.swing.*;
import java.awt.Color;
import java.awt.Desktop;

public class ManualTab {

    private static JEditorPane pane;

    // Utilitário para converter cor para Hex String
    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String getHtmlContent() {
        // Cores base do Sistema
        Color uiBg = UIManager.getColor("Panel.background");
        Color uiFg = UIManager.getColor("Label.foreground");
        if (uiBg == null) uiBg = Color.WHITE;
        if (uiFg == null) uiFg = Color.BLACK;

        // Detecta tema escuro
        int avgBg = (uiBg.getRed() + uiBg.getGreen() + uiBg.getBlue()) / 3;
        boolean isDark = avgBg < 128;

        // --- PALETA DE CORES ---
        String bgBody = toHex(uiBg);
        String fgBody = toHex(uiFg);
        String accentColor = isDark ? "#64B5F6" : "#1565C0";
        String titleColor  = isDark ? "#E3F2FD" : "#0D47A1";

        // Cores para as bolinhas (ajustadas para contraste)
        String dotYellow = isDark ? "#FDD835" : "#FBC02D"; // Amarelo ouro
        String dotGreen  = isDark ? "#66BB6A" : "#2E7D32"; // Verde
        String dotOrange = isDark ? "#FF9800" : "#EF6C00"; // Laranja

        // Estilos de container
        String codeBg  = isDark ? "#37474F" : "#F5F5F5";
        String codeFg  = isDark ? "#ECEFF1" : "#C2185B";
        String border  = isDark ? "#546E7A" : "#B0BEC5";

        // Estilo das badges e boxes
        String tipBg = isDark ? "#263238" : "#E3F2FD";
        String tipBorder = isDark ? "#0288D1" : "#2196F3";
        String warnBg = isDark ? "#3E2723" : "#FFF3E0";
        String warnBorder = isDark ? "#D84315" : "#FF6F00";
        String badgeBg = isDark ? "#004D40" : "#E0F2F1";
        String badgeFg = isDark ? "#80CBC4" : "#00695C";

        return "<html><head>"
                + "<style>"
                + "body { font-family: 'Segoe UI', sans-serif; font-size: 14px; color: " + fgBody + "; background-color: " + bgBody + "; margin: 0; padding: 20px; }"
                + "h1 { font-size: 26px; color: " + accentColor + "; margin-bottom: 5px; font-weight: normal; }"
                + "h2 { font-size: 18px; color: " + titleColor + "; border-bottom: 2px solid " + border + "; margin-top: 30px; padding-bottom: 5px; }"
                + "h3 { font-size: 15px; color: " + accentColor + "; margin-top: 20px; margin-bottom: 5px; }"
                + "p { line-height: 1.5; margin-bottom: 10px; }"
                + "li { line-height: 1.5; margin-bottom: 8px; }"
                + "b { color: " + titleColor + "; }"
                + "code { font-family: Monospaced; font-size: 13px; color: " + codeFg + "; background-color: " + codeBg + "; }"

                // Estilo da Tabela
                + "table { width: 100%; border-collapse: collapse; margin-top: 15px; }"
                + "th { text-align: left; padding: 10px; background-color: " + codeBg + "; color: " + titleColor + "; border-bottom: 2px solid " + border + "; }"
                + "td { padding: 8px 10px; border-bottom: 1px solid " + border + "; vertical-align: top; }"

                // Badges
                + ".badge { background-color: " + badgeBg + "; color: " + badgeFg + "; font-weight: bold; font-family: Monospaced; }"

                // Boxes
                + ".box { padding: 15px; margin-top: 20px; border-radius: 4px; }"
                + ".tip { background-color: " + tipBg + "; border-left: 5px solid " + tipBorder + "; }"
                + ".warn { background-color: " + warnBg + "; border-left: 5px solid " + warnBorder + "; color: " + fgBody + "; }"

                // Classe para as bolinhas (dots)
                + ".dot { font-size: 18px; vertical-align: middle; }"

                + "</style>"
                + "</head><body>"

                + "<h1>📘 Manual do Usuário</h1>"
                + "<p>Olá! Este simulador vai te ajudar a entender como um processador funciona por dentro, vendo cada passo do ciclo de instruções.</p>"

                + "<h2>🖥️ A Interface</h2>"
                + "<p>O simulador é dividido em três áreas principais:</p>"

                // --- NOVA SEÇÃO DETALHADA ---
                + "<h3>1. Registradores (Topo)</h3>"
                + "<p>São as \"memórias rápidas\" internas da CPU. Cada caixinha tem uma função vital:</p>"
                + "<ul>"
                + "  <li><code>PC</code> <b>(Counter):</b> O \"Contador\". Ele guarda o <b>endereço</b> da próxima linha de código que a CPU vai ler. Ele avança automaticamente a cada passo.</li>"
                + "  <li><code>IR</code> <b>(Instruction):</b> O \"Tradutor\". Ele guarda o código da instrução que está sendo executada <b>neste exato momento</b>.</li>"
                + "  <li><code>ACC</code> <b>(Accumulator):</b> A \"Calculadora\". É o registrador principal. Todos os resultados de somas, subtrações e dados carregados ficam aqui.</li>"
                + "  <li><code>Z</code> <b>(Zero Flag):</b> O \"Dedo-duro do Zero\". Essa flag vira <b>1</b> se o resultado da última conta for zero. O comando <code>JZ</code> usa isso para decidir se pula ou não.</li>"
                + "  <li><code>N</code> <b>(Neg Flag):</b> O \"Dedo-duro do Negativo\". Essa flag vira <b>1</b> se o resultado da última conta for negativo. O comando <code>JN</code> usa isso.</li>"
                + "</ul>"
                // -----------------------------

                + "<h3>2. Memória (Grade)</h3>"
                + "<p>A matriz de 16x16 células representa a memória RAM (256 bytes). É onde seu programa e suas variáveis ficam guardados.</p>"
                + "<div class='box tip'>"
                + "  <b>Legenda de Cores:</b><br><br>"
                + "  <span class='dot' style='color:" + dotYellow + ";'>●</span> &nbsp; <b>PC (Amarelo):</b> Onde a CPU está lendo agora.<br>"
                + "  <span class='dot' style='color:" + dotGreen + ";'>●</span> &nbsp; <b>Leitura (Verde):</b> Dado que acabou de ser lido.<br>"
                + "  <span class='dot' style='color:" + dotOrange + ";'>●</span> &nbsp; <b>Escrita (Laranja):</b> Dado que acabou de ser gravado."
                + "</div>"

                + "<h3>3. Controles</h3>"
                + "<p>Use os botões <b>Step</b> (Passo a Passo) para ver a mágica acontecer devagar, ou <b>Run</b> para rodar direto.</p>"

                + "<h2>🛠️ Escrevendo Código (Editor)</h2>"
                + "<p>Na aba <b>Editor</b>, você cria seus programas em Assembly.</p>"
                + "<ul>"
                + "  <li><b>Rótulos:</b> Palavras terminadas em dois pontos (ex: <code>LOOP:</code>) marcam um local para onde você pode pular.</li>"
                + "  <li><b>Comentários:</b> Tudo depois de <code>/</code> é ignorado (use para anotações).</li>"
                + "</ul>"

                + "<h2>📚 Comandos Disponíveis (ISA)</h2>"
                + "<p>Lista de instruções que esta CPU suporta:</p>"

                + "<table>"
                + "  <tr><th width='20%'>Comando</th> <th width='60%'>O que faz</th> <th width='20%'>Flags</th></tr>"

                + "  <tr><td><span class='badge'>&nbsp;ADD&nbsp;</span></td> <td>Soma o valor da memória ao <b>ACC</b>.</td> <td>Z, N</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;SUB&nbsp;</span></td> <td>Subtrai o valor da memória do <b>ACC</b>.</td> <td>Z, N</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;ADDI&nbsp;</span></td> <td>Soma um número fixo (imediato) ao <b>ACC</b>.</td> <td>Z, N</td></tr>"

                + "  <tr><td><span class='badge'>&nbsp;LOAD&nbsp;</span></td> <td>Copia um valor da memória para o <b>ACC</b>.</td> <td>Z, N</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;STORE&nbsp;</span></td> <td>Salva o valor do <b>ACC</b> na memória.</td> <td>-</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;LOADI&nbsp;</span></td> <td>Carrega um número fixo no <b>ACC</b>.</td> <td>Z, N</td></tr>"

                + "  <tr><td><span class='badge'>&nbsp;JUMP&nbsp;</span></td> <td>Pula incondicionalmente para um rótulo/endereço.</td> <td>-</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;JZ&nbsp;</span></td> <td>Pula SE a flag <b>Z</b> for 1 (Zero).</td> <td>-</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;JN&nbsp;</span></td> <td>Pula SE a flag <b>N</b> for 1 (Negativo).</td> <td>-</td></tr>"

                + "  <tr><td><span class='badge'>&nbsp;INPUT&nbsp;</span></td> <td>Pede um número ao usuário e põe no <b>ACC</b>.</td> <td>Z, N</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;OUTPUT&nbsp;</span></td> <td>Mostra o valor do <b>ACC</b> na tela.</td> <td>-</td></tr>"
                + "  <tr><td><span class='badge'>&nbsp;HALT&nbsp;</span></td> <td>Para a execução do programa.</td> <td>-</td></tr>"
                + "</table>"

                + "<h2>⚠️ Cuidado com os Limites</h2>"
                + "<div class='box warn'>"
                + "  <b>O limite é 8-bits (255)</b><br>"
                + "  Os valores só vão de 0 até 255. <br>"
                + "  Se você somar 255 + 1, o resultado volta para 0 (Overflow)."
                + "</div>"

                + "<div style='margin-top:40px; border-top:1px solid "+border+"; padding-top:10px; font-size:11px; color:"+border+"; text-align:right;'>"
                + "  Simulador Educativo v3.4"
                + "</div>"

                + "</body></html>";
    }

    public static JComponent build() {
        pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setOpaque(true);

        pane.setBorder(BorderFactory.createEmptyBorder());

        pane.setText(getHtmlContent());

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                if (e.getURL() != null) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        return scroll;
    }

    public static void updateStyles() {
        if (pane != null) {
            pane.setText(getHtmlContent());
            pane.repaint();
        }
    }
}