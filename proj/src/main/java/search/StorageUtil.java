package search;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class StorageUtil {
    private static final String DATA_FOLDER = "..//data//";


    public static void saveData(Object data, String fileName) {
        
        String filePath = DATA_FOLDER + fileName;
        try (FileOutputStream fileOut = new FileOutputStream(filePath);
             ObjectOutputStream objOut = new ObjectOutputStream(fileOut)) {

            objOut.writeObject(data);
            //System.out.println(" Dados salvos em: " + filePath);

        } catch (IOException e) {
            System.err.println(" Erro ao salvar arquivo: " + filePath);
            e.printStackTrace();
        }
    }

    
    @SuppressWarnings("unchecked")
    public static <T> T loadData(String filename, T defaulValue){   
        String filePath = DATA_FOLDER + filename;
        File file = new File(filePath);

        if(!file.exists()){
            System.out.println("Ficheiro nao encontrado");
            return defaulValue;
        }

        try (FileInputStream fileIn = new FileInputStream(filePath);
             ObjectInputStream objIn = new ObjectInputStream(fileIn)) {

            return (T) objIn.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao carregar arquivo: " + filePath);
            e.printStackTrace();
            return defaulValue;
        }
    }
}

