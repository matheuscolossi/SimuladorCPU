package cpu;

import javax.swing.*;
import java.awt.Color;
import java.awt.Desktop; // <--- IMPORT FALTANTE ADICIONADO AQUI

public class ManualTab {

    private static JEditorPane pane; // Painel estático

    /**
     * Constrói o HTML injetando as cores do tema atual.
     */
    private static String getHtmlContent() {
        // Pega cores do tema (Claro ou Escuro) para injeção no CSS
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground"); // Parágrafos
        Color heading = UIManager.getColor("Component.focusColor"); // Cor de destaque (ex: azul)
        Color dis = UIManager.getColor("Label.disabledForeground"); // Texto desabilitado
        Color warnBg = UIManager.getColor("TextField.warningBackground"); // Amarelo/Laranja
        Color warnFg = UIManager.getColor("TextField.foreground"); // Cor do texto no aviso
        Color tableHeadBg = UIManager.getColor("TableHeader.background");
        Color tipBg = UIManager.getColor("Table.selectionInactiveBackground");
        Color codeBg = UIManager.getColor("EditorPane.background");
        Color borderColor = UIManager.getColor("Component.borderColor");

        // Fallbacks para garantir que não falhe
        if (bg == null) bg = Color.WHITE;
        if (fg == null) fg = Color.BLACK;
        if (heading == null) heading = new Color(0, 100, 200);
        if (dis == null) dis = Color.GRAY;
        if (warnBg == null) warnBg = new Color(255, 244, 229);
        if (warnFg == null || (bg.getRed() > 128 && warnFg.getRed() > 128)) warnFg = Color.BLACK; // Força preto se a cor do texto for clara
        if (tableHeadBg == null) tableHeadBg = new Color(240, 240, 240);
        if (tipBg == null) tipBg = new Color(230, 230, 230);
        if (codeBg == null) codeBg = new Color(245, 245, 245);
        if (borderColor == null) borderColor = Color.LIGHT_GRAY;

        // Converte para HEX
        String hexBg = String.format("#%02x%02x%02x", bg.getRed(), bg.getGreen(), bg.getBlue());
        String hexFg = String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue());
        String hexHeading = String.format("#%02x%02x%02x", heading.getRed(), heading.getGreen(), heading.getBlue());
        String hexDisabled = String.format("#%02x%02x%02x", dis.getRed(), dis.getGreen(), dis.getBlue());
        String hexWarnBg = String.format("#%02x%02x%02x", warnBg.getRed(), warnBg.getGreen(), warnBg.getBlue());
        String hexWarnFg = String.format("#%02x%02x%02x", warnFg.getRed(), warnFg.getGreen(), warnFg.getBlue());
        String hexTableHeadBg = String.format("#%02x%02x%02x", tableHeadBg.getRed(), tableHeadBg.getGreen(), tableHeadBg.getBlue());
        String hexTipBg = String.format("#%02x%02x%02x", tipBg.getRed(), tipBg.getGreen(), tipBg.getBlue());
        String hexCodeBg = String.format("#%02x%02x%02x", codeBg.getRed(), codeBg.getGreen(), codeBg.getBlue());
        String hexBorder = String.format("#%02x%02x%02x", borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue());

        // CSS e HTML reescritos para um visual moderno (layout de página única)
        return "<!DOCTYPE html>"
                + "<html><head><meta charset=\"UTF-8\"><style>"
                // CSS Global
                + "body { "
                + "  margin: 0; padding: 20px 30px; " // Espaçamento interno
                + "  font-family: Segoe UI, Arial, sans-serif; "
                + "  color: " + hexFg + "; "
                + "  background-color: " + hexBg + "; "
                + "  font-size: 15px; "
                + "}"

                // Conteúdo Principal
                + "h1 { font-size: 26px; border-bottom: 1px solid " + hexBorder + "; padding-bottom: 10px; margin-top: 5px; color: " + hexHeading + "; }"
                + "h2 { font-size: 20px; margin-top: 30px; margin-bottom: 10px; color: " + hexFg + "; font-weight: 600; border-bottom: 1px solid " + hexBorder + "; padding-bottom: 5px; }"
                + "p, li { font-size: 15px; line-height: 1.6; }"
                + "ul { margin-top: 5px; padding-left: 30px; }"

                // Tabela
                + "table { border-collapse: collapse; width: 100%; margin-top: 15px; }"
                + "th, td { border: 1px solid " + hexBorder + "; padding: 9px 12px; text-align: left; font-size: 15px; }"
                + "th { background-color: " + hexTableHeadBg + "; }"

                // Destaques
                + "code { "
                + "  background: " + hexCodeBg + "; "
                + "  border: 1px solid " + hexBorder + "; "
                + "  padding: 2px 6px; border-radius: 4px; "
                + "  font-family: Consolas, monospace; "
                + "  color: " + hexFg + "; "
                + "  font-weight: 600; "
                + "}"

                + ".tip { "
                + "  background-color: " + hexTipBg + "; "
                + "  border-left: 4px solid " + hexHeading + "; "
                + "  padding: 12px 15px; border-radius: 4px; margin-top: 15px; "
                + "  color: " + (bg.getRed() < 128 ? "#E0E0E0" : "#000000") + "; "
                + "}"

                + ".warn { "
                + "  background-color: " + hexWarnBg + "; "
                + "  border-left: 4px solid #F57C00; "
                + "  padding: 12px 15px; border-radius: 4px; margin-top: 15px; "
                + "  color: " + hexWarnFg + "; "
                + "}"

                + ".small { color: " + hexDisabled + "; font-size: 12px; margin-top: 25px; }"

                + "</style></head><body>"

                // --- ESTRUTURA HTML (Página Única) ---
                + "<h1>Manual do Simulador de CPU</h1>"
                + "<p>Bem-vindo ao simulador. Esta ferramenta demonstra o ciclo de execução de uma CPU simples baseada na arquitetura MARIE.</p>"

                + "<h2>Interface Principal</h2>"
                + "<p>A aba <b>Simulador</b> é o seu painel de controle. Ela é dividida em 3 partes:</p>"
                + "<ul>"
                + "  <li><b>Registradores:</b> No topo, mostram o estado interno da CPU (<code>PC</code>, <code>IR</code>, <code>ACC</code>, <code>Z</code>, <code>N</code>).</li>"
                + "  <li><b>Memória:</b> A tabela (16x16) mostra os 256 bytes de memória da CPU.</li>"
                + "  <li><b>Painel de Depuração:</b> A área inferior que exibe o seu código e o log de explicação.</li>"
                + "</ul>"
                + "<h3>Destaques da Memória</h3>"
                + "<p>Observe as cores na tabela de memória durante a execução:</p>"
                + "<ul>"
                + "  <li><span style='background-color:#FFFFAA; color:#000; padding: 2px 5px;'>Amarelo</span>: Posição do <b>Program Counter (PC)</b>.</li>"
                + "  <li><span style='background-color:#AAFFAA; color:#000; padding: 2px 5px;'>Verde</span>: Endereço de memória sendo <b>lido</b> (ex: <code>LOADM</code>).</li>"
                + "  <li><span style='background-color:#FFCCAA; color:#000; padding: 2px 5px;'>Laranja</span>: Endereço de memória sendo <b>escrito</b> (ex: <code>STORE</code>).</li>"
                + "</ul>"

                + "<h2>Depurador Visual 🐛</h2>"
                + "<p>O painel <b>\"Código Fonte\"</b> na aba Simulador é o seu depurador. Conforme você usa o <b>Step</b>, a linha que está prestes a ser executada (apontada pelo PC) ficará destacada em amarelo. Isso conecta o seu código Assembly com o que a CPU está fazendo na memória.</p>"

                + "<h2>Editor (Assembly)</h2>"
                + "<p>A aba <b>Editor</b> é onde você escreve seu código. O montador (Assembler) suporta:</p>"
                + "<ul>"
                + "  <li><b>Comentários:</b> Use <code>/</code> ou <code>;</code>.</li>"
                + "  <li><b>Rótulos (Labels):</b> Crie um marcador de desvio (ex: <code>LOOP:</code>).</li>"
                + "  <li><b>Variáveis:</b> Declare no fim do código (ex: <code>DADO, DEC 10</code>).</li>"
                + "</ul>"
                + "<p>O menu <b>Arquivo</b> (no topo da janela) permite Abrir (<code>Ctrl+O</code>) e Salvar (<code>Ctrl+S</code>) seus programas <code>.asm</code>.</p>"

                + "<h2>Instruções (ISA)</h2>"
                + "<p>Este é o conjunto de instruções completo que a nossa CPU (baseada em MARIE) entende.</p>"
                + "<table>"
                + "  <tr><th>Mnemônico</th><th>Descrição</th><th>Flags</th></tr>"
                + "  <tr><td><code>LOADI val</code></td><td>Carrega um valor imediato (o próprio número) no ACC.</td><td>Z, N</td></tr>"
                + "  <tr><td><code>LOADM / LOAD</code></td><td>Carrega um valor da memória no ACC.</td><td>Z, N</td></tr>"
                + "  <tr><td><code>STORE</code></td><td>Salva o valor do ACC na memória.</td><td>-</td></tr>"
                + "  <tr><td><code>ADDI val</code></td><td>Soma um valor imediato ao ACC.</td><td>Z, N</td></tr>"
                + "  <tr><td><code>ADDM / ADD</code></td><td>Soma um valor da memória ao ACC.</td><td>Z, N</td></tr>"
                + "  <tr><td><code>SUBI val</code></td><td>Subtrai um valor imediato do ACC.</td><td>Z, N</td></tr>"
                + "  <tr><td><code>SUBM / SUB</code></td><td>Subtrai um valor da memória do ACC.</td><td>Z, N</td></tr>"
                + "  <tr><td><code>JMP</code></td><td>Desvio incondicional (pula para o rótulo/endereço).</td><td>-</td></tr>"
                + "  <tr><td><code>JZ</code></td><td>Desvio condicional: pula se <b>Z=1</b> (ACC é zero).</td><td>-</td></tr>"
                + "  <tr><td><code>JN</code></td><td>Desvio condicional: pula se <b>N=1</b> (ACC é negativo).</td><td>-</td></tr>"
                + "  <tr><td><code>IN</code></td><td>Abre um pop-up pedindo um número ao usuário.</td><td>Z, N</td></tr>"
                + "  <tr><td><code>OUT</code></td><td>Escreve o valor do ACC no log de \"Explicação do passo\".</td><td>-</td></tr>"
                + "  <tr><td><code>HALT</code></td><td>Encerra a execução do programa.</td><td>-</td></tr>"
                + "</table>"

                + "<h2>Limite de 8-Bits (Importante!)</h2>"
                + "<div class='warn'>" // Caixa de Aviso
                + "  <b>Atenção: Esta é uma CPU de 8-bits.</b><br>"
                + "  Isso significa que o valor máximo para qualquer dado ou endereço é <b>255</b> (<code>0xFF</code>)."
                + "  Se uma operação (como <code>IN</code> ou <code>ADDI</code>) resultar num valor maior, como 300, ele dará a volta (overflow/underflow)."
                + "  <br><b>Exemplo:</b> 300 se tornará 44 (porque 300 % 256 = 44)."
                + "</div>"

                + "<div class='tip'>" // Caixa de Dica
                + "  <b>Dica:</b> A instrução <code>JN</code> (Jump if Negative) é ativada quando o ACC ultrapassa 127 (<code>0x7F</code>). Por exemplo, 255 (que é <code>-1</code> em 8 bits) ativará a flag N."
                + "</div>"

                + "<p class='small'>SimuladorCPU v3.2 | Baseado em MARIE | GUI com FlatLaf</p>"

                + "</body></html>";
    }

    public static JComponent build() {
        pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setText(getHtmlContent());

        // Listener para links (embora não tenhamos mais navegação interna, pode ser útil)
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                // Se for um link externo (http), tenta abrir no navegador
                if (e.getURL() != null) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        // ignora
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    /**
     * Atualiza o conteúdo HTML do painel.
     * Chamado pelo AppSwing quando o tema é trocado.
     */
    public static void updateStyles() {
        if (pane != null) {
            // Define o texto novamente, recarregando o HTML
            // com as novas cores do UIManager
            pane.setText(getHtmlContent());
        }
    }
}