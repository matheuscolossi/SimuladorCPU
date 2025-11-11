package cpu;

import javax.swing.*;
import java.awt.Color;

public class ManualTab {

    private static JEditorPane pane; // O painel é estático para ser atualizado

    /**
     * Constrói o HTML para o JEditorPane, injetando dinamicamente
     * as cores corretas do tema (claro ou escuro) obtidas do UIManager.
     */
    private static String getHtmlContent() {
        // Pega a cor de texto correta do tema ATUAL
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) fg = Color.BLACK; // Fallback
        String hexFg = String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue());

        // Pega a cor de texto "desabilitado" (para o .small)
        Color dis = UIManager.getColor("Label.disabledForeground");
        if (dis == null) dis = Color.GRAY; // Fallback
        String hexDisabled = String.format("#%02x%02x%02x", dis.getRed(), dis.getGreen(), dis.getBlue());

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
/* CSS ATUALIZADO: Injeta cores HEX dinamicamente */
body, h1, h2, p, li, td, th {
    font-family: Segoe UI, Arial, sans-serif;
    margin: 12px;
    line-height: 1.4;
    background-color: var(--Panel-background);
    color: %s; /* COR DO TEXTO INJETADA */
}
h1{font-size:22px;margin-bottom:8px}
h2{font-size:16px;margin-top:20px;margin-bottom:4px}
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
    padding: 8px;
    border-radius: 6px;
}
pre {
    background: var(--EditorPane-background);
    padding: 8px;
    border-radius: 4px;
    border: 1px solid var(--Component-borderColor);
}
.small{
    color: %s; /* COR DO TEXTO DESABILITADO INJETADA */
}
</style>
</head>
<body>

<h1>Manual do Simulador Educativo de CPU</h1>
<p>Este simulador demonstra, de forma didática, como um processador simples executa instruções passo a passo.</p>

<h2>Partes principais da interface</h2>
<ul>
  <li><b>PC</b>: Contador de Programa – mostra o endereço da próxima instrução.</li>
  <li><b>IR</b>: Registrador de Instrução – armazena a instrução atual.</li>
  <li><b>ACC</b>: Acumulador – guarda o resultado das operações.</li>
  <li><b>Z</b>: Flag Zero – fica em 1 quando o ACC é igual a 0.</li>
  <li><b>Memória</b>: mostra o conteúdo de cada posição de memória (endereços e valores).</li>
</ul>

<h2>Botões de controle</h2>
<ul>
  <li><b>Carregar</b>: carrega o exemplo escolhido no menu.</li>
  <li><b>Step</b>: executa um único ciclo (<i>fetch → decode → execute</i>).</li>
  <li><b>Run</b>: executa automaticamente as instruções até <b>HALT</b>.</li>
  <li><b>Pause</b>: pausa a execução automática.</li>
  <li><b>Reset</b>: reinicia a CPU e limpa a memória.</li>
</ul>

<h2>Instruções suportadas</h2>
<table>
  <tr><th>Instrução</th><th>Função</th></tr>
  <tr><td><b>LOADI n</b></td><td>Carrega o valor imediato <b>n</b> no ACC.</td></tr>
  <tr><td><b>LOADM a</b> (ou <b>LOAD a</b>)</td><td>Carrega no ACC o valor armazenado em MEM[<b>a</b>].</td></tr>
  <tr><td><b>STORE a</b></td><td>Armazena o valor do ACC em MEM[<b>a</b>].</td></tr>
  <tr><td><b>ADDI n</b></td><td>Soma o valor imediato <b>n</b> ao ACC.</td></tr>
  <tr><td><b>SUBI n</b></td><td>Subtrai o valor imediato <b>n</b> do ACC.</td></tr>
  <tr><td><b>ADDM a</b> (ou <b>ADD a</b>)</td><td>Soma o valor de MEM[<b>a</b>] ao ACC.</td></tr>
  <tr><td><b>SUBM a</b> (ou <b>SUB a</b>)</td><td>Subtrai o valor de MEM[<b>a</b>] do ACC.</td></tr>
  <tr><td><b>JMP a</b></td><td>Desvia a execução para o endereço <b>a</b> (incondicional).</td></tr>
  <tr><td><b>JZ a</b></td><td>Desvia para o endereço <b>a</b> se o ACC for zero (flag Z = 1).</td></tr>
  <tr><td><b>IN</b> (ou <b>INPUT</b>)</td><td>Pede ao usuário um valor inteiro e o salva no ACC.</td></tr>
  <tr><td><b>OUT</b> (ou <b>OUTPUT</b>)</td><td>Exibe o valor do ACC no log de explicação.</td></tr>
  <tr><td><b>HALT</b></td><td>Encerra a execução.</td></tr>
</table>

<h2>Usando o Editor (aba "Editor")</h2>
<p>A aba <b>Editor</b> permite que você escreva, monte e execute seus próprios programas em Assembly.</p>
<p>O montador suporta <b>comentários</b> (<code>;</code> ou <code>/</code>), <b>variáveis nomeadas</b> (<code>X, DEC 0</code>) e <b>rótulos de desvio</b> (<code>LOOP:</code>).</p>

<ol>
  <li>Clique na aba <b>Editor</b> no topo da janela.</li>
  <li>No campo de texto, digite seu programa. Exemplo:
    <pre>
/ Exemplo de Loop
LOADI 3
STORE CONTADOR
LOOP:
LOAD CONTADOR
JZ   FIM
OUT
SUBI 1
STORE CONTADOR
JMP LOOP
FIM:
HALT
/ --- Dados ---
CONTADOR, DEC 0
    </pre>
  </li>
  <li>Clique em <b>Executar no simulador</b>.</li>
  <li>O código será montado (<code>JMP LOOP</code> será traduzido para o endereço correto) e carregado na memória.</li>
  <li>Volte para a aba <b>Simulador</b> e use <b>Step</b> ou <b>Run</b>.</li>
</ol>

<div class="tip">
<b>Dica:</b> Observe os valores de PC, IR e ACC. Na aba Simulador, o seu código-fonte terá a linha atual destacada a amarelo. Na tabela de memória, a célula do <b>PC</b> fica amarela, a lida (<b>LOADM</b>) fica verde e a escrita (<b>STORE</b>) fica laranja.
</div>

<h2>Sobre</h2>
<p>Desenvolvido como ferramenta didática para Arquitetura de Computadores.</p>

<p class="small">Versão 1.6 — Depurador Visual de Código-Fonte</p>

</body>
</html>
""".formatted(hexFg, hexDisabled); // Formata o HTML com as cores corretas
    }

    public static JComponent build() {
        pane = new JEditorPane(); // Atribui ao campo estático
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setOpaque(false);

        pane.setText(getHtmlContent());

        return new JScrollPane(pane);
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