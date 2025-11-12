package cpu;

import javax.swing.*;
import java.awt.Color;

public class ManualTab {

    private static JEditorPane pane; // Painel estático

    /**
     * Constrói o HTML injetando as cores do tema atual.
     */
    private static String getHtmlContent() {
        // Pega cores do tema (Claro ou Escuro)
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) fg = Color.BLACK;
        String hexFg = String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue());

        Color dis = UIManager.getColor("Label.disabledForeground");
        if (dis == null) dis = Color.GRAY;
        String hexDisabled = String.format("#%02x%02x%02x", dis.getRed(), dis.getGreen(), dis.getBlue());

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
body, h1, h2, h3, p, li, td, th {
    font-family: Segoe UI, Arial, sans-serif;
    margin: 12px;
    line-height: 1.4;
    background-color: var(--Panel-background);
    color: %s; /* Cor do texto dinâmica */
}
h1{font-size:22px;margin-bottom:8px}
h2{font-size:18px;margin-top:20px;margin-bottom:6px; border-bottom: 1px solid var(--Component-borderColor); padding-bottom: 4px;}
table{border-collapse:collapse}
td,th{
    border: 1px solid var(--Component-borderColor);
    padding:4px 6px;
}
ul{margin-top:6px}
li{margin-bottom:3px}
.tip {
    background: var(--Table-selectionBackground);
    border: 1px solid var(--Component-borderColor);
    padding: 10px;
    border-radius: 6px;
    margin-top: 15px;
}
/* Estilo para a caixa de aviso de limite */
.warn {
    background: #FFF4E5; /* Laranja bem claro (funciona ok em dark mode também se for opaco, ou ajustamos) */
    color: #663C00;
    border: 1px solid #FFCCAA;
    padding: 10px;
    border-radius: 6px;
    margin-top: 15px;
}
/* Ajuste para Dark Mode no warning se necessário, mas vamos manter simples */
pre {
    background: var(--EditorPane-background);
    padding: 8px;
    border-radius: 4px;
    border: 1px solid var(--Component-borderColor);
}
.small{
    color: %s; 
    font-size: 11px;
    margin-top: 20px;
}
</style>
</head>
<body>

<h1>Manual do Simulador</h1>
<p>Bem-vindo ao Simulador Educativo de CPU. Aprenda como um processador funciona visualizando o ciclo de instruções, a memória e o código em tempo real.</p>

<h2>1. Interface Principal</h2>
<ul>
  <li><b>PC (Program Counter):</b> Aponta para o endereço da próxima instrução.</li>
  <li><b>IR (Instruction Register):</b> Mostra a instrução que está sendo executada.</li>
  <li><b>ACC (Acumulador):</b> Registrador principal para operações matemáticas.</li>
  <li><b>Z (Zero Flag):</b> Fica em <b>1</b> se o último resultado foi zero (usado no JZ).</li>
  <li><b>Memória:</b>
      <ul>
        <li><span style="background-color:#FFFFAA; color:#000">Amarelo</span>: Posição atual do PC.</li>
        <li><span style="background-color:#AAFFAA; color:#000">Verde</span>: Leitura da memória.</li>
        <li><span style="background-color:#FFCCAA; color:#000">Laranja</span>: Escrita na memória.</li>
      </ul>
  </li>
</ul>

<h2>2. Editor e Depurador </h2>
<p>A aba <b>Simulador</b> possui um painel que exibe seu código-fonte.</p>
<ul>
    <li>A linha atual de execução é destacada em <b>Amarelo</b>.</li>
    <li>Use o botão <b>Step</b> para acompanhar a lógica linha a linha.</li>
</ul>

<h2>3. Instruções Suportadas</h2>
<table>
  <tr><th>Mnemônico</th><th>Descrição</th></tr>
  <tr><td><b>LOAD / LOADM</b> addr</td><td>Carrega valor da memória [addr] para o ACC.</td></tr>
  <tr><td><b>LOADI</b> val</td><td>Carrega um valor imediato (número fixo) para o ACC.</td></tr>
  <tr><td><b>STORE</b> addr</td><td>Salva o valor do ACC na memória [addr].</td></tr>
  <tr><td><b>ADD / ADDM</b> addr</td><td>Soma o valor da memória [addr] ao ACC.</td></tr>
  <tr><td><b>ADDI</b> val</td><td>Soma um valor imediato ao ACC.</td></tr>
  <tr><td><b>SUB / SUBM</b> addr</td><td>Subtrai o valor da memória [addr] do ACC.</td></tr>
  <tr><td><b>SUBI</b> val</td><td>Subtrai um valor imediato do ACC.</td></tr>
  <tr><td><b>JMP</b> label/addr</td><td>Desvio incondicional (pula para o endereço/rótulo).</td></tr>
  <tr><td><b>JZ</b> label/addr</td><td>Desvio condicional: pula se <b>Z=1</b> (ACC é zero).</td></tr>
  <tr><td><b>IN</b></td><td>Abre uma janela <b>pop-up</b> pedindo um número ao usuário.</td></tr>
  <tr><td><b>OUT</b></td><td>Escreve o valor do ACC no painel de <b>"Explicação do passo"</b>.</td></tr>
  <tr><td><b>HALT</b></td><td>Encerra a execução.</td></tr>
</table>

<h2>4. Entendendo IN e OUT</h2>
<ul>
    <li><b>IN (Entrada):</b> Quando a CPU encontra esta instrução, a simulação <b>pausa</b> e uma janela aparece na tela. Digite um número inteiro e clique em OK. O valor será guardado no Acumulador (ACC).</li>
    <li><b>OUT (Saída):</b> O simulador não possui um monitor gráfico. A saída é exibida como texto no log inferior ("Explicação do passo"). Procure por linhas começando com "OUT ->".</li>
</ul>

<div class="warn">
<b>⚠️ Limite de 8 Bits (Max: 255)</b><br>
Este simulador utiliza uma arquitetura simples de 8 bits. Isso significa que:
<ul>
    <li>O <b>valor máximo</b> para dados e endereços é <b>255</b>.</li>
    <li>Se você digitar ou calcular um valor maior (ex: 300), ele será "cortado" (será usado o resto da divisão por 256). Exemplo: 300 vira 44.</li>
    <li>Números negativos também são convertidos para o formato sem sinal (0..255).</li>
</ul>
</div>

<div class="tip">
<b>Dica Pro:</b> Use <b>Rótulos</b> (ex: <code>LOOP:</code>) e <b>Variáveis</b> (ex: <code>A, DEC 0</code>) no Editor. O montador calcula os endereços automaticamente! Lembre-se de declarar as variáveis no final do código.
</div>

<p class="small">Versão 2.2 — Documentação Atualizada (8-bits)</p>

</body>
</html>
""".formatted(hexFg, hexDisabled);
    }

    public static JComponent build() {
        pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setText(getHtmlContent());
        return new JScrollPane(pane);
    }

    public static void updateStyles() {
        if (pane != null) {
            pane.setText(getHtmlContent());
        }
    }
}