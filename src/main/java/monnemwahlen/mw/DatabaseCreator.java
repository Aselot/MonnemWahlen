package monnemwahlen.mw;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseCreator {

    static String[] queries = new String[]{
            "Create database if not exists authenticatordb;",
            "Create database if not exists electionworkerdb;",
            "Create database if not exists validatordb;",
            "Create database if not exists tallierdb;",
            "Create database if not exists polldb;",
            "use authenticatordb;",
            "create table if not exists voters (id int not null auto_increment, name char(50) not null, email char (50) not null, password char(50) not null, primary key(id));",
            "create table if not exists voter_election_arrangement (voter_id char(50) not null, worker_id char(50) not null, primary key(voter_id));",
            "use electionworkerdb;",
            "create table if not exists rvl1 (worker_id char(50) not null, public_key varchar(2000) not null,voter_ids varchar(20000), primary key(worker_id));",
            "create table if not exists rvl2 (worker_id char(50) not null, public_key varchar(2000) not null,voter_ids varchar(20000), primary key(worker_id));",
            "use polldb;",
            "create table if not exists polls(poll_id int auto_increment, name char(50) not null, seats int not null, primary key(poll_id));",
            "create table if not exists poll1(candidate_id int auto_increment, name char(50) not null, primary key(candidate_id));",
            "use valdb;",
            "create table if not exists rvl1 (worker_id char(50) not null, public_key varchar(2000) not null,voter_ids varchar(20000), primary key(worker_id));",
            "create table if not exists rvl2 (worker_id char(50) not null, public_key varchar(2000) not null,voter_ids varchar(20000), primary key(worker_id));",

    };

    public static void main(String[] args) {

        String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
        String DB_URL = "jdbc:mariadb://localhost:3306";

        String DBUser = "creator";
        String password = "creatorpass";

        try {
            Connection conn = DriverManager.getConnection(
                    DB_URL, DBUser, password);
            Statement stmt;
            for (String query : queries){

                System.out.println("query: "+query);
                stmt = conn.createStatement();
                stmt.executeQuery(query);

            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }
}
