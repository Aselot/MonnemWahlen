package val;

import monnemwahlen.mw.KeyPairEntityService;
import monnemwahlen.mw.MoService;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.transform.Result;
import java.io.*;
import java.security.*;
import java.sql.*;
import java.util.Base64;
import java.util.Map;

@Service
public class ValidatorService implements MoService, KeyPairEntityService {

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

        try {
            Class.forName(this.JDBC_DRIVER);


        //init conenctions for election workers db

        this.val_conn = DriverManager.getConnection(
                this.DB_URL, this.DBUser, this.password);

        this.val_stmt = val_conn.createStatement();

        String sql = "Use validatordb;";
        this.val_stmt.executeUpdate(sql);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        if (!checkForKeyPair()) {
            createNewKeyPair(privateKeyPath,publicKeyPath);
        }
    }

    public String checkForSignature(Map<String, Object> json) {
        String id1 = (String) json.get("id");
        String ballot = (String) json.get("ballot");
        String voter_ids;
        try{
            val_stmt = val_conn.createStatement();
            ResultSet rs =  val_stmt.executeQuery("select * from rvl1 where worker_id = \""+id1+"\";");
            rs.next();
            PublicKey pk = stringToPublicKey(rs.getString("public_key"));

            String hashBallot = encrypt(ballot, Cipher.DECRYPT_MODE,pk);
            System.out.println(hashBallot);
            return hashBallot;


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return "";

    }

    public String getSignedBallot(String ballot) {

        PrivateKey sk = (PrivateKey) getKey(privateKeyPath);

        String signedBallot = encrypt(ballot,Cipher.ENCRYPT_MODE,sk);

        return signedBallot;

    }


    private boolean checkForKeyPair() {
        File publicKey = new File(publicKeyPath);
        File privateKey = new File(privateKeyPath);

        if ((publicKey.exists() && !publicKey.isDirectory()) && (privateKey.exists() && !privateKey.isDirectory())) {
            return true;
        }
        return false;

    }




    public void saveRVL(Map<String, Object> json) {

        try {
            val_stmt = val_conn.createStatement();

            val_stmt.executeQuery("Delete from rvl1");

            StringBuilder rvl1Query = new StringBuilder("Insert into rvl1 (worker_id, public_key) values ");

            for (var entry:json.entrySet()){
                rvl1Query.append(String.format("(\"%s\",  \"%s\"), ", entry.getKey(),entry.getValue()));
            }

            rvl1Query.setLength(rvl1Query.length()-2);

            val_stmt = val_conn.createStatement();
            val_stmt.executeQuery(rvl1Query.toString()+";");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }


    public String getPublicKeyPath() {
        return publicKeyPath;
    }
}
