package auth;

import monnemwahlen.mw.KeyPairEntityService;
import monnemwahlen.mw.MoService;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService implements MoService, KeyPairEntityService {

    private final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    private final String DB_URL = "jdbc:mariadb://localhost:3306";

    private String DBUser = "auth";
    private String password = "authpass";

    private Connection conn;
    private Statement stmt;

    public AuthService() {

        try {
            Class.forName(this.JDBC_DRIVER);

            this.conn = DriverManager.getConnection(
                    this.DB_URL, this.DBUser, this.password);

            this.stmt = conn.createStatement();

            String sql = "Use authenticatordb";
            this.stmt.executeUpdate(sql);

        } catch (SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        finally {
//            try {
//                if (stmt != null) {
//                    conn.close();
//                }
//            } catch (SQLException se) {
//            }// do nothing
//            try {
//                if (conn != null) {
//                    conn.close();
//                }
//            } catch (SQLException se) {
//                se.printStackTrace();
//            }//end finally try
//        }//end try
    }//end main

    public boolean checkCredentials(String username, String password) {
        try {
//            this.conn = DriverManager.getConnection(
//                    this.DB_URL, this.DBUser, this.password);
            this.stmt = this.conn.createStatement();

            String query = "use authenticatordb";
            this.stmt.executeQuery(query);

            query = "select name,password from voters where name = '" + username + "';";
            ResultSet rs = this.stmt.executeQuery(query);

            rs.next();
            return rs.getString("password").equals(password);


        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }


    }

    public Map<String, String> assignWorkerIDs(String jsonString) throws SQLException {
        JSONParser parser = new JSONParser(jsonString);

        Map<String, Object> jsonMap;
        try {
            jsonMap = parser.parseObject();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        List<String> workerIDs = (List<String>) jsonMap.get("worker_IDs");
        int workerCount = workerIDs.size();
//        this.stmt = this.conn.createStatement();
//        String query = "select * from voters";
        try {
            this.stmt = this.conn.createStatement();
            String query = "select * from voters";
            ResultSet rs = this.stmt.executeQuery(query);
            Map<String, String> worker_voterList = new HashMap<String, String>();
            int counter = 0;
            while (rs.next()) {
                worker_voterList.put(rs.getString("id"), workerIDs.get(counter++));
                if (counter == workerCount) counter = 0;
            }
            return worker_voterList;
        } catch (SQLException sql) {
            sql.printStackTrace();
            return null;
        }


    }


    public String getElectionWorker(String name) {
        try {
            String id = getVoterID(name);

            String query = "select worker_id from voter_election_arrangement where voter_id = '" + id + "';";
            ResultSet rs = this.stmt.executeQuery(query);

            rs.next();
            return rs.getString("worker_id");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


        return "";
    }

    public String getVoterID(String name) {
        try {
            this.stmt = this.conn.createStatement();

            String query = "use authenticatordb";
            this.stmt.executeQuery(query);

            query = "select id,name from voters where name = '" + name + "';";
            ResultSet rs = this.stmt.executeQuery(query);
            rs.next();
            return rs.getString("id");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


        return "";
    }

    public void saveVoterWorkerListToDB(Map<String, String> voter_workerList) {

        try {
            //clear old entries
            this.stmt = this.conn.createStatement();
            this.stmt.executeQuery("delete from voter_election_arrangement;");

            this.stmt = this.conn.createStatement();
            StringBuilder query = new StringBuilder("insert into voter_election_arrangement (voter_id, worker_id) values ");
            for (var entry : voter_workerList.entrySet()) {
                query.append(String.format("(\"%s\",\"%s\"),", entry.getKey(), entry.getValue()));
            }
            query.setLength(query.length() - 1);
            stmt.executeQuery(String.valueOf(query + ";"));
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.out.println("SQL error, either no users in voters list or wrong credentials for the sql user ");
        }

    }
}
