package tal;

import monnemwahlen.mw.KeyPairEntityService;
import monnemwahlen.mw.MoService;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.io.File;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static j2html.TagCreator.*;

@Service
public class TallierService implements MoService, KeyPairEntityService {

    private final int trackingNumberStart = 1000;

    private String path = "./tallier/src/main/java/tal";

    private String publicKeyPath = path + "/keys/publicKey";
    private String privateKeyPath = path + "/keys/privateKey";
    private String validatorPKPath = path + "/publicKeys/validatorPK";

    private final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    private final String DB_URL = "jdbc:mariadb://localhost:3306";

    private String DBUser = "tal";
    private String password = "talpass";

    private Connection tal_conn;
    private Statement tal_stmt;

    public TallierService() {

        try {
            Class.forName(this.JDBC_DRIVER);


            //init conenctions for election workers db

            this.tal_conn = DriverManager.getConnection(
                    this.DB_URL, this.DBUser, this.password);

            this.tal_stmt = tal_conn.createStatement();

            String sql = "Use tallierdb;";
            this.tal_stmt.executeUpdate(sql);

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        if (!checkForKeyPair()) {
            createNewKeyPair(privateKeyPath, publicKeyPath);
        }

        getAndSaveValidatorPK();

    }

    public int addBallotToPollAndGetTrackingNumber(String encryptedBallot) {
        try {
            tal_stmt = tal_conn.createStatement();
            ResultSet rs = tal_stmt.executeQuery("Select * from rv;");

            Map<Integer, String> rv = new HashMap<>();
            while (rs.next()) {
                rv.put(rs.getInt("tracking_number"), rs.getString("ballot"));
            }
            int trackingNumber = rv.size() + trackingNumberStart + 1;

            tal_stmt = tal_conn.createStatement();
            tal_stmt.executeQuery(String.format("Insert into rv (tracking_number, ballot) VALUE (\"%s\",\"%s\");", trackingNumber, encryptedBallot));

            return trackingNumber;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return 0;
    }

    public void checkBallots(String signedBallot, String encryptedBallot, String id) {
        try {
            tal_stmt = tal_conn.createStatement();
            ResultSet rs = tal_stmt.executeQuery("select * from rvl2 where worker_id = \"" + id + "\";");
            rs.next();

            PublicKey worker_pk = stringToPublicKey(rs.getString("public_key"));

            //remove sign from worker
            String validatedBallot = encrypt(signedBallot, Cipher.DECRYPT_MODE, worker_pk);

            PublicKey val_pk = (PublicKey) getKey(validatorPKPath);

            String hashedBallotCheck = digest(encryptedBallot);
            // remove sign of validator, the resulting String has an increased size due to cryptigraphic functions of its byte []
            String hashedBallot = encrypt(validatedBallot, Cipher.DECRYPT_MODE, val_pk);
            String guttedHashedBallot = hashedBallot.substring(hashedBallot.length() - hashedBallotCheck.length());
            if (!guttedHashedBallot.equals(hashedBallotCheck)) {
                System.out.println("Ballots not correct!");
            } else System.out.println("Ballots identical!");
            System.out.println("hash from worker: " + guttedHashedBallot);
            System.out.println("hashed (self): " + hashedBallotCheck);


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }

    public void decryptBallot(String trackingNumber, String decryptionKey, BigInteger ballotLength) {

        PublicKey decryptKey = stringToPublicKey(decryptionKey);

        try {
            tal_stmt = tal_conn.createStatement();
            ResultSet rs = tal_stmt.executeQuery("select * from rv where tracking_number = \"" + trackingNumber + "\";");
            rs.next();
            String encryptedBallot = rs.getString("ballot");
            String ballot = encrypt(encryptedBallot, Cipher.DECRYPT_MODE, decryptKey);
            System.out.println(ballot);

            String correctBallot = ballot.substring(ballot.length() - ballotLength.intValue());

//            JSONParser parser = new JSONParser(correctBallot);
//
//            Map<String,Object> map = parser.parseObject();
            //Object obj = map.get("vote");

            tal_stmt = tal_conn.createStatement();
            String query = "Update rv set ballot = \'" + correctBallot + "\' where tracking_number = \'" + trackingNumber + "\';";
            tal_stmt.executeQuery(query);


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }

    private void getAndSaveValidatorPK() {
        LinkedHashMap<String, Object> res = sendGetRequest(getUrl("val") + "/getPK");
//        String pk = (String) ;
//        saveFile(validatorPKPath,pk);
        PublicKey key = stringToPublicKey((String) res.get("pk"));
        saveFile(validatorPKPath, key);
    }


    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    private boolean checkForKeyPair() {
        File publicKey = new File(path + "/" + publicKeyPath);
        File privateKey = new File(path + "/" + privateKeyPath);

        if ((publicKey.exists() && !publicKey.isDirectory()) && (privateKey.exists() && !privateKey.isDirectory())) {
            return true;
        }
        return false;

    }

    public String getTally() {
        try {
            tal_stmt = tal_conn.createStatement();
            ResultSet rs = tal_stmt.executeQuery("Select * from rv");
            Map<String, String> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("tracking_number"), rs.getString("ballot"));
            }
            return toHtml(map);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    private String toHtml(Map<String, String> map) {

        Map<String,String> validList = new HashMap<>();
        Map<String, Integer> candidateList = new HashMap<>();
        try {
            for (var entry : map.entrySet()) {

                JSONParser parser = new JSONParser(entry.getValue());
                Map<String, Object> parsedBallot = parser.parseObject();

                if(ballotIsValid(parsedBallot)) {
                    for (var entry2 : parsedBallot.entrySet()) {
                        String candidate = entry2.getKey();
                        Integer voteCount;
                        try {
                            voteCount = Integer.valueOf((String) entry2.getValue());
                        } catch (NumberFormatException e) {
                            voteCount = 0;
                        }
                        if (candidateList.containsKey(candidate)) {
                            candidateList.put(candidate, candidateList.get(candidate) + voteCount);
                        } else {
                            candidateList.put(candidate, voteCount);
                        }
                    }
                    validList.put(entry.getKey(),"+");
                }
                else{
                    validList.put(entry.getKey(),"-");
                }


            }

            var page = html(
                    head(
                            title(
                                    "Tally"
                            ),
                            style(
                                    "table, th, td {\n" +
                                            "border: 1px solid black;\n" +
                                            "border-collapse: collapse;\n" +
                                            "}\n" +
                                            "th, td {\n" +
                                            "padding: 15px;\n" +
                                            "}\n"
                            )
//                        link(
//
//                        ).withRel("stylesheet").attr("th:href=\"@{/ballot.css}\""),
//                        script().withType("text/javascript").attr("th:src=\"@{/script.js}\"")
//                                .attr("th:inline=\"javascript\"")
                    ),
                    body(

                            h2(
                                    "Status of Tally"
                            ),
                            table(
                                    tr(
                                            each(candidateList, candidate -> th(
                                                    candidate.getKey()
                                            ))
                                    ),
                                    tr(
                                            each(candidateList, candidate2 -> td(
                                                    candidate2.getValue().toString()
                                            ))
                                    )
                            ),

                            h2("Tallied ballots"),
                            table(
                                    tr(
                                            th("tracking number"),
                                            th("votes"),
                                            th("is valid? (+,yes), (-,no)")
                                    ),
                                    each(map.entrySet(), entry -> {

                                        try {
                                            return tr(
                                                    td(entry.getKey()),
                                                    td(
                                                            table(
                                                                    tr(
                                                                            each(new JSONParser(entry.getValue()).parseObject(), entry2 -> {
                                                                                return th(entry2.getKey());
                                                                            })
                                                                    ),
                                                                    tr(
                                                                            each(new JSONParser(entry.getValue()).parseObject(), entry3 -> {
                                                                                return td((String) entry3.getValue());
                                                                            })
                                                                    )
                                                            )
                                                    ),
                                                    td(validList.get(entry.getKey()))
                                            );
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            return null;
                                        }

                                    })

                            )
                    )
            ).attr("xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:th=\"http://www.thymeleaf.org\"");
            return "<!DOCTYPE html>\n" + page.renderFormatted();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    private boolean ballotIsValid(Map<String, Object> parsedBallot) {
        int count = 0;
        for (var entry : parsedBallot.entrySet()){
            try{
                Integer i = Integer.valueOf((String) entry.getValue());
                if (i.intValue()<1 && i.intValue()!= 0)
                    return false;
                else if (i.intValue()>7) return false;
                count += i.intValue();
            }
            catch (Exception e ){
                if (entry.getValue() == "")
                    continue;
                return false;
            }
        }
        return count <= 7;
    }


    public void saveRVLs(Map<String, Object> json) {

        for (var entry : json.entrySet()) {

            String tableName = entry.getKey();

            try {


                Map<String, Object> rvl = (Map<String, Object>) entry.getValue();

                tal_stmt = tal_conn.createStatement();

                tal_stmt.executeQuery("Delete from " + tableName + ";");

                StringBuilder rvl1Query = new StringBuilder(String.format("Insert into %s (worker_id, public_key) values ", tableName));

                for (var subEntry : rvl.entrySet()) {
                    rvl1Query.append(String.format("(\"%s\",  \"%s\"), ", subEntry.getKey(), subEntry.getValue()));
                }

                rvl1Query.setLength(rvl1Query.length() - 2);

                tal_stmt = tal_conn.createStatement();
                tal_stmt.executeQuery(rvl1Query.toString() + ";");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }


    }

    public String signBallot(String encryptedBallot) {
        return encrypt(encryptedBallot, Cipher.ENCRYPT_MODE, (PrivateKey) getKey(privateKeyPath));
    }
}

