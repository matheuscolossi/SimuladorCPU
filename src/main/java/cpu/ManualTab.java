package cpu;

import javax.swing.*;

public class ManualTab {
    public static JComponent build() {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText("""
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
body{font-family:Segoe UI,Arial,sans-serif;margin:12px;line-height:1.4}
h1{font-size:22px;margin-bottom:8px}
h2{font-size:16px;margin-top:20px;margin-bottom:4px}
table{border-collapse:collapse}
td,th{border:1px solid #ccc;padding:4px 6px}
ul{margin-top:6px}
li{margin-bottom:3px}
.tip{background:#e8f5e9;border:1px solid #c8e6c9;padding:8px;border-radius:6px}
.small{color:#666;font-size:12px}
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
  <tr><td><b>LOADI n</b></td><td>Carrega o valor imediato n no ACC.</td></tr>
  <tr><td><b>LOADM a</b></td><td>Carrega no ACC o valor armazenado em MEM[a].</td></tr>
  <tr><td><b>STORE a</b></td><td>Armazena o valor do ACC em MEM[a].</td></tr>
  <tr><td><b>ADDI n</b></td><td>Soma o valor n ao ACC.</td></tr>
  <tr><td><b>SUBI n</b></td><td>Subtrai o valor n do ACC.</td></tr>
  <tr><td><b>JMP a</b></td><td>Desvia a execução para o endereço a×2 (incondicional).</td></tr>
  <tr><td><b>JZ a</b></td><td>Desvia se o ACC for zero (flag Z = 1).</td></tr>
  <tr><td><b>HALT</b></td><td>Encerra a execução.</td></tr>
</table>

<h2>Usando o simulador</h2>
<ol>
  <li>Escolha um exemplo no menu suspenso acima dos botões.</li>
  <li>Clique em <b>Carregar</b> para colocar o programa na memória.</li>
  <li>Use <b>Step</b> para executar uma instrução por vez e observar o ciclo completo.</li>
  <li>Ou use <b>Run</b> para deixar o simulador rodar automaticamente.</li>
  <li>Quando quiser reiniciar, clique em <b>Reset</b>.</li>
</ol>

<div class="tip">
<b>Dica:</b> Observe os valores de PC, IR, ACC e a flag Z mudando a cada passo. A tabela de memória mostra quais posições estão sendo acessadas.
</div>

<h2>Usando o Editor (aba "Editor")</h2>
<p>A aba <b>Editor</b> permite que você escreva e execute seus próprios programas em Assembly diretamente no simulador.</p>

<ol>
  <li>Clique na aba <b>Editor</b> no topo da janela.</li>
  <li>No campo de texto, digite seu programa, linha por linha. Exemplo:
    <pre>
LOADI 5
ADDI  3
STORE 10
HALT
    </pre>
  </li>
  <li>Quando terminar, clique em <b>Executar no simulador</b>.</li>
  <li>O código será montado e carregado na memória do simulador automaticamente.</li>
  <li>Volte para a aba <b>Simulador</b> e use <b>Step</b> ou <b>Run</b> para acompanhar a execução.</li>
</ol>

<div class="tip">
<b>Dica:</b> Você pode testar qualquer sequência de instruções suportadas. Se o programa tiver erro de sintaxe, o simulador exibirá uma mensagem de erro.
</div>

<h2>Sobre</h2>
<p>Desenvolvido como ferramenta didática para auxiliar no aprendizado de Arquitetura de Computadores, ilustrando o ciclo de instruções e o funcionamento de uma CPU simples.</p>

<p class="small">Versão 1.1 — agora com Editor integrado</p>

</body>
</html>
""");
        return new JScrollPane(pane);
    }
}
