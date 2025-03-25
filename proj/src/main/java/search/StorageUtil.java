package search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class StorageUtil {
    private static final String DATA_FOLDER = "..//data//";
    public static void saveData(Object data, String fileName) {
        String filePath = DATA_FOLDER + fileName;
        String tempFilePath = filePath + ".tmp";
        Path tempPath = Paths.get(tempFilePath);
        Path finalPath = Paths.get(filePath);

        try (FileOutputStream fos = new FileOutputStream(tempFilePath);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(data);
            oos.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            System.err.println("Erro ao salvar no arquivo temporário: " + tempFilePath);
            e.printStackTrace();
            return;
        }

        try {
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Dados salvos com sucesso em: " + filePath);
        } catch (IOException e) {
            System.err.println("Erro ao renomear o arquivo temporário para: " + filePath);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadData(String filename, T defaultValue) {
        String filePath = DATA_FOLDER + filename;
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Ficheiro não encontrado: " + filePath);
            return defaultValue;
        }

        if (file.length() == 0) {
            System.out.println("Arquivo vazio, retornando valor default.");
            return defaultValue;
        }

        try (FileInputStream fileIn = new FileInputStream(filePath);
                ObjectInputStream objIn = new ObjectInputStream(fileIn)) {
            return (T) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao carregar arquivo: " + filePath);
            e.printStackTrace();
            return defaultValue;
        }
    }

}
