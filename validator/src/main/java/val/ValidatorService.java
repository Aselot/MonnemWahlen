package val;

import org.springframework.stereotype.Service;

import java.io.*;
import java.security.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

@Service
public class ValidatorService {

    private String path = "./validator/src/main/java/val";

    private String publicKeyPath = path + "/keys/publicKey";
    private String privateKeyPath = path + "/keys/privateKey";

    private final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    private final String DB_URL = "jdbc:mariadb://localhost:3306";

    private String DBUser = "val";
    private String password = "valpass";

    private Connection val_conn;
    private Statement val_stmt;

    public ValidatorService() {
        if (!checkForKeyPair()) {
            createNewKeyPair();
        }
    }

    private void createNewKeyPair() {
        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");

            keygen.initialize(2048);

            KeyPair pair = keygen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            FileOutputStream fileOut = new FileOutputStream(privateKeyPath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(privateKey);
            objectOut.close();

            fileOut = new FileOutputStream(publicKeyPath);
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(publicKey);
            objectOut.close();

        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    public Key getKey(String keyType){
        try {
            FileInputStream fileIn = new FileInputStream(keyType.equals("private") ? privateKeyPath: publicKeyPath);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            Key key;
            if(keyType.equals("private")){
                key = (PrivateKey) objIn.readObject();
            }
            else
            {
                key = (PublicKey) objIn.readObject();
            }

            objIn.close();

            return key;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean checkForKeyPair() {
        File publicKey = new File(path + "/" + publicKeyPath);
        File privateKey = new File(path + "/" + privateKeyPath);

        if ((publicKey.exists() && !publicKey.isDirectory()) && (privateKey.exists() && !privateKey.isDirectory())) {
            return true;
        }
        return false;

    }


    public void saveRVL(Map<String, Object> json) {

        try {
            val_stmt = val_conn.createStatement();

            val_stmt.executeQuery("Delete from rvl1");

            for (var entry:json.entrySet()){
                val_stmt = val_conn.createStatement();
                val_stmt.executeQuery()

            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }
}
