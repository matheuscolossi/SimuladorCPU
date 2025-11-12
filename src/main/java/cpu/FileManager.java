package cpu;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileManager {

    /** Abre um diálogo para escolher arquivo e retorna o conteúdo dele. */
    public static String openFile(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivos Assembly (*.asm)", "asm", "txt"));

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Erro ao ler arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null; // Cancelado ou erro
    }

    /** Abre um diálogo para salvar o conteúdo em um arquivo. */
    public static void saveFile(JFrame parent, String content) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivos Assembly (*.asm)", "asm"));

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            // Garante a extensão .asm
            if (!file.getName().toLowerCase().endsWith(".asm")) {
                file = new File(file.getParentFile(), file.getName() + ".asm");
            }

            try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
                out.print(content);
                JOptionPane.showMessageDialog(parent, "Arquivo salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Erro ao salvar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}