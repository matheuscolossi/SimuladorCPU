package cpu;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FileManager {

    /** Abre um diálogo para escolher arquivo (versão Java 8) */
    public static String openFile(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivos Assembly (*.asm)", "asm", "txt"));

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {

                // Lê o arquivo linha por linha (compatível com Java 8)
                return reader.lines().collect(Collectors.joining("\n"));

            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Erro ao ler arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null; // Cancelado ou erro
    }

    /** Abre um diálogo para salvar o conteúdo (versão Java 8) */
    public static void saveFile(JFrame parent, String content) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivos Assembly (*.asm)", "asm"));

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".asm")) {
                file = new File(file.getParentFile(), file.getName() + ".asm");
            }

            // =========================================================
            // CORREÇÃO DO CONSTRUTOR PARA JAVA 8
            // =========================================================
            // Precisamos criar o "caminho" de bytes manualmente
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 PrintWriter out = new PrintWriter(osw)) {

                out.print(content);
                JOptionPane.showMessageDialog(parent, "Arquivo salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Erro ao salvar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
            // =========================================================
        }
    }
}