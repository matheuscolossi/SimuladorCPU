package cpu;

/**
 * Ponto de entrada (Main) principal da aplicação Simulador de CPU.
 *
 * Esta classe serve como o "lançador" (launcher) oficial.
 * Ela simplesmente delega toda a inicialização e execução
 * para a classe AppSwing, que contém nossa interface gráfica (GUI).
 */
public class Main {

    /**
     * O método main da aplicação.
     * @param args Argumentos de linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        // Chama o método main da nossa aplicação Swing para iniciar a interface
        AppSwing.main(args);
    }
}