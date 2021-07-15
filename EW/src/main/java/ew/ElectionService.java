package ew;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.security.PublicKey;
import java.sql.*;
import java.util.*;

import static j2html.TagCreator.*;


@Service
public class ElectionService implements MoService {

    private final String workerSavePath = "./EW/src/main/java/ew/electionWorkers";

    private final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    private final String DB_URL = "jdbc:mariadb://localhost:3306";

    private String DBUser = "ew";
    private String password = "ewpass";

    private Connection ewdb_conn;
    private Statement ewdb_stmt;

    private Connection polldb_conn;
    private Statement polldb_stmt;

    private PublicKey validatorPBKey;
    private PublicKey tallierPBKey;

    public ElectionService() {

        try {
            Class.forName(this.JDBC_DRIVER);

            //init conenctions for election workers db

            this.ewdb_conn = DriverManager.getConnection(
                    this.DB_URL, this.DBUser, this.password);

            this.ewdb_stmt = ewdb_conn.createStatement();

            String sql = "Use electionworkerdb;";
            this.ewdb_stmt.executeUpdate(sql);


            // same for the poll databse

            this.polldb_conn = DriverManager.getConnection(
                    this.DB_URL, this.DBUser, this.password);

            this.polldb_stmt = polldb_conn.createStatement();

            String sql2 = "Use polldb;";
            this.polldb_stmt.executeUpdate(sql2);

        } catch (Exception se) {
            se.printStackTrace();
        }

        getPublicKeysOfEntitiesAndSendRVLs();
        sendRVLLists();
    }

    public boolean checkIfWorkerAndVoterAlign(String worker_id, String voter_id) {
        ElectionWorker worker = getElectionWorker(worker_id);
        return worker.hasVoter(voter_id);

    }

    private void getPublicKeysOfEntitiesAndSendRVLs() {
        Map<String,Object> jsonMapRvl1 = new HashMap<>();
        Map<String,Object> jsonMapRvl2 = new HashMap<>();


        try {
            ewdb_stmt = ewdb_conn.createStatement();
            String query = "Select * From rvl1;";
            ResultSet rs = ewdb_stmt.executeQuery(query);

            while (rs.next()){
                jsonMapRvl1.put(rs.getString("worker_id"),rs.getString("public_key"));
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        try {
            ewdb_stmt = ewdb_conn.createStatement();
            String query = "Select * From rvl2;";
            ResultSet rs = ewdb_stmt.executeQuery(query);

            while (rs.next()){
                jsonMapRvl2.put(rs.getString("worker_id"),rs.getString("public_key"));
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        Map<String,Object> jsonTal = new HashMap<>();
        jsonTal.put("rvl1",jsonMapRvl1);
        jsonTal.put("rvl2",jsonMapRvl2);

        Map<String,Object> responseDataVal = sendRequest(getUrl("val")+"/setup",jsonMapRvl1);
        Map<String,Object> responseDataTal = sendRequest(getUrl("tal")+"/setup",jsonTal);
        System.out.println(responseDataVal.toString()+responseDataTal.toString());

    }



    public boolean checkIfWorkerExist(int workerCount) {
        File f = new File(this.workerSavePath);
        String[] files = f.list();
        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
        //if (files == null) return false;
        return files.length == workerCount;
    }

    public List<Candidate> getCandidates(Poll poll) {
        try {
            polldb_stmt = polldb_conn.createStatement();
            ResultSet rs = polldb_stmt.executeQuery("Select * from " + poll.getName() + ";");

            List<Candidate> candidateList = new ArrayList<>();
            while (rs.next()) {
                candidateList.add(new Candidate(rs.getInt("candidate_id"), rs.getString("name")));
            }

            return candidateList;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    public Poll getPoll(String pollName) {
        try {
            polldb_stmt = polldb_conn.createStatement();
            ResultSet rs = polldb_stmt.executeQuery("select * FROM polls where name = \"" + pollName + "\";");

            rs.next();
            return new Poll(
                    rs.getInt("poll_id"),
                    rs.getInt("seats"),
                    rs.getString("name")
            );

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }



    public String createAndSaveUserHtmlPage(Poll poll, List<Candidate> candidates, String voter_id, String worker_id) {
        var page = html(
                head(
                        title(
                                "vote sheet for " + poll.getName()
                        ),
                        link(

                        ).withRel("stylesheet").attr("th:href=\"@{/ballot.css}\""),
                        script().withType("text/javascript").attr("th:src=\"@{/script.js}\"")
                                .attr("th:inline=\"javascript\"")
                ),
                body(
                        div(
                                h3("remaining votes:"),
                                input().withId("remainingVotes").attr("readonly").withValue("" + poll.getSeats())
                        ).withClass("backgrnG row border margin1"),
                        div(
                                div(
                                        each(candidates, candidate -> div(
                                                label(candidate.getName()).withClass("fEl margin1"),
                                                input().withId("" + candidate.getId() + "_input").withType("number")
                                                        .attr("max=7").withClass("input fEl margin1")
                                                        .attr("th:onchange=\"calcInputs(value)\"").attr("min = 0")
                                                        .attr("value = 0")
                                        ).withClass("backgrnB row border margin1 doubleColumn flex fEl"))
                                        //label(worker_id).attr("display:none").withId("worker_id"),
                                        //label(voter_id).attr("display:none").withId("voter_id")

                                ).withClass("fCol margin1 doubleColumn flex"), //.withMethod("post").withAction("http://localhost:8082/postBallot"),
                                div(
                                        button("Send Ballot").attr(String.format("th:onclick=\"pushedButton('%s','%s','%s');\"", worker_id, voter_id, poll.getName()))
                                                .withClass("button margin1 flex")
                                )
                        ).withClass("backgrnG border margin1 flex fCol")
                )
        ).attr("xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:th=\"http://www.thymeleaf.org\"");


        String renderedHtml = "<!DOCTYPE html>\n" + page.renderFormatted();
        String filename = poll.getName() + "_" + voter_id + "_" + "_ballot";
        String filepath = "./EW/src/main/resources/templates/" + filename + ".html";

        try {
            PrintWriter out = new PrintWriter(filepath);
            out.write(renderedHtml);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return filename;
    }



    public Map<String, ElectionWorker> createNewWorkers(int workerCount) {

        List<String> ID1 = new ArrayList<String>();
        List<String> ID2 = new ArrayList<String>();
        Map<String, ElectionWorker> workers = new HashMap<>();

        for (int i = 0; i < workerCount; i++) {

            // list for ID#1
            String randomVal1;
            String randomVal2;
            boolean flag = false;

            do {
                randomVal1 = getRandomHexString(16);

                if (ID1.contains(randomVal1)) flag = true;
                else {
                    ID1.add(randomVal1);
                    flag = false;
                }

            } while (flag);

            // list for ID#2

            do {
                randomVal2 = getRandomHexString(16);

                if (ID2.contains(randomVal2)) flag = true;
                else {
                    ID2.add(randomVal2);
                    flag = false;
                }

            } while (flag);

            workers.put(randomVal1, new ElectionWorker(randomVal1, randomVal1));

        }
        return workers;

    }

    public void deleteWorkers() {
        try {
            FileUtils.cleanDirectory(new File(this.workerSavePath));
//            stmt = conn.createStatement();
//            stmt.executeQuery("DELETE FROM ")

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ewdb_stmt = ewdb_conn.createStatement();
            ewdb_stmt.executeQuery("DELETE from rvl1");
            ewdb_stmt = ewdb_conn.createStatement();
            ewdb_stmt.executeQuery("DELETE from rvl2");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private String getRandomHexString(int numchars) {
        Random r = new Random();
        StringBuffer sb = new StringBuffer();
        while (sb.length() < numchars) {
            sb.append(Integer.toHexString(r.nextInt()));
        }

        return sb.toString().substring(0, numchars);
    }


    public Map<String, ElectionWorker> loadWorkers() {
        Map<String, ElectionWorker> workerList = new HashMap<>();
        File f = new File(workerSavePath);
        try {
            for (String str : f.list()) {
                FileInputStream fin = new FileInputStream(workerSavePath + "/" + str);
                ObjectInputStream in = new ObjectInputStream(fin);
                ElectionWorker worker = (ElectionWorker) in.readObject();
                workerList.put(worker.getID1(), worker);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return workerList;

    }


    public void saveWorkers(Map<String, ElectionWorker> workerList) {

        saveWorkers_db(workerList);
        saveWorkers_serial(workerList);

    }

    private void saveWorkers_db(Map<String, ElectionWorker> workerList) {
        try {
            StringBuilder rvl1Query = new StringBuilder("Insert into rvl1 (worker_id, public_key) values ");
            StringBuilder rvl2Query = new StringBuilder("Insert into rvl2 (worker_id, public_key) values ");

            for (var entry : workerList.entrySet()) {

                PublicKey pbk1 = entry.getValue().getFirstPublicKey();
                String pbk1Str = Base64.getEncoder().encodeToString(pbk1.getEncoded());
                PublicKey pbk2 = entry.getValue().getSecondPublicKey();
                String pbk2Str = Base64.getEncoder().encodeToString(pbk2.getEncoded());

                rvl1Query.append(String.format("(\"%s\", \"%s\"),", entry.getValue().getID1(), pbk1Str));
                rvl2Query.append(String.format("(\"%s\", \"%s\"),", entry.getValue().getID2(), pbk2Str));
            }
            rvl1Query.setLength(rvl1Query.length() - 1);
            rvl2Query.setLength(rvl2Query.length() - 1);

            ewdb_stmt = ewdb_conn.createStatement();
            ewdb_stmt.executeQuery(String.valueOf(rvl1Query) + ";");
            ewdb_stmt = ewdb_conn.createStatement();
            ewdb_stmt.executeQuery(String.valueOf(rvl2Query) + ";");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void saveWorkers_serial(Map<String, ElectionWorker> workerList) {
        String filename;
        for (var entry : workerList.entrySet()) {
            filename = workerSavePath + "/worker_" + entry.getKey() + ".txt";
            try {
                FileOutputStream file = new FileOutputStream(filename);
                ObjectOutputStream out = new ObjectOutputStream(file);

                out.writeObject(entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendWorkerIDs(Map<String, ElectionWorker> workerList) {

        StringBuilder jsonString = new StringBuilder("{\"worker_IDs\": [");

        for (var entry : workerList.entrySet()) {
            jsonString.append("\"").append(entry.getKey()).append("\", ");
        }
        jsonString.setLength(jsonString.length() - 2);
        jsonString.append("]}");

        HttpURLConnection connection = null;
        try {

            HttpPost httppost = new HttpPost("http://localhost:8081/setWorkers");

// Request parameters and other properties.
            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("json", jsonString.toString()));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            try (CloseableHttpClient httpclient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpclient.execute(httppost)) {

                HttpEntity entity = response.getEntity();
                String handledResponse = new BasicResponseHandler().handleResponse(response);

                JSONParser parser = new JSONParser(handledResponse);

                LinkedHashMap<String, Object> voter_worker_map = parser.parseObject();

                //here we could work with the response which contains the voter ids mapped to the worker ids.
                saveVotersToWorkers(voter_worker_map);

            } catch (ParseException e) {
                e.printStackTrace();
            }


//            connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type",
//                    "application/json; utf-8");
//            connection.setRequestProperty("Accept", "application/json");
//
//            connection.setDoOutput(true);
//
//            connection.
//
//            try(OutputStream os = connection.getOutputStream()) {
//                byte[] input = jsonString.toString().getBytes(StandardCharsets.UTF_8);
//                os.write(input, 0, input.length);
//            }
//
//            try(BufferedReader br = new BufferedReader(
//                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
//                StringBuilder response = new StringBuilder();
//                String responseLine = null;
//                while ((responseLine = br.readLine()) != null) {
//                    response.append(responseLine.trim());
//                }
//                System.out.println(response.toString());
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveVotersToWorkers(LinkedHashMap<String, Object> voter_worker_map) {
        Map<String, ElectionWorker> workerList = new HashMap<>();
        List<String> IDlist = new ArrayList<>();
        for (var entry: voter_worker_map.entrySet()){
            ElectionWorker worker = getElectionWorker((String) entry.getValue());
            worker.addVoter((String) entry.getKey());
            if (!IDlist.contains(worker.getID1())) {
                workerList.put(worker.getID1(), worker);
                IDlist.add(worker.getID1());
            }
        }
        saveWorkers_serial(workerList);

    }

    public ElectionWorker getElectionWorker(String worker_ID) {
        Map<String, ElectionWorker> workerList = loadWorkers();
        return workerList.get(worker_ID);
    }

    public void giveBallotToElectionWorker(Map<String, Object> json) {

        ElectionWorker worker = getElectionWorker((String) json.get("worker_id"));
        ElectionWorker worker2 = new ElectionWorker("23", "23");
        worker2.test();
//        worker.encryptAndSetBallot(json);
        worker2.validateBallot();

    }

    private LinkedHashMap<String, Object> sendRequest(String url, Map<String, Object> jsonData) {
        try {

            HttpPost httppost = new HttpPost(url);

// Request parameters and other properties.
            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("json", jsonData.toString()));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            try (CloseableHttpClient httpclient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpclient.execute(httppost)) {

                String handledResponse = new BasicResponseHandler().handleResponse(response);

                JSONParser parser = new JSONParser(handledResponse);

                return parser.parseObject();

                //here we could work with the response which contains the voter ids mapped to the worker ids.

            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }


//    public void createHTMLVoteSheets() {
//
//        List<Poll> pollList = new ArrayList<>();
//        try {
//            polldb_stmt = polldb_conn.createStatement();
//            ResultSet rs = polldb_stmt.executeQuery("select * FROM polls;");
//
//            while (rs.next()) {
//                pollList.add(new Poll(
//                        rs.getInt("poll_id"),
//                        rs.getInt("seats"),
//                        rs.getString("name")
//                ));
//            }
//
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        }
//
//        for (Poll poll : pollList) {
//
//            try {
//                polldb_stmt = polldb_conn.createStatement();
//                ResultSet rs = polldb_stmt.executeQuery("Select * from " + poll.getName() + ";");
//
//                List<Candidate> candidateList = new ArrayList<>();
//                while (rs.next()) {
//                    candidateList.add(new Candidate(rs.getInt("candidate_id"), rs.getString("name")));
//                }
//
//                createAndSaveHtmlPage(poll, candidateList);
//
//            } catch (SQLException throwables) {
//                throwables.printStackTrace();
//            }
//
//
//        }
//
//    }

//    private void createAndSaveHtmlPage(Poll poll, List<Candidate> candidates) {
//
//        var page = html(
//                head(
//                        title(
//                                "vote sheet for " + poll.getName()
//                        ),
//                        link(
//
//                        ).withRel("stylesheet").attr("th:href=\"@{/ballot.css}\""),
//                        script().withType("text/javascript").attr("th:src=\"@{/script.js}\"")
//                                .attr("th:inline=\"javascript\"")
//                ),
//                body(
//                        div(
//                                h3("remaining votes:"),
//                                input().withId("remainingVotes").attr("readonly").withValue("" + poll.getSeats())
//                        ).withClass("backgrnG row border margin1"),
//                        div(
//                                div(
//                                        each(candidates, candidate -> div(
//                                                h3(candidate.getName()).withClass("fEl margin1"),
//                                                input().withId("" + candidate.getId() + "_input").withType("number")
//                                                        .attr("max=7").withClass("input fEl margin1")
//                                                        .attr("th:onchange=\"calcInputs(value)\"").attr("min = 0")
//                                                        .attr("value = 0")
//                                        ).withClass("backgrnB row border margin1 doubleColumn flex fEl"))
//
//                                ).withClass("fCol margin1 doubleColumn flex"),
//                                div(
//                                        button("Send Ballot").attr("th:onclick=\"pushedButton();\"")
//                                                .withClass("button margin1 flex")
//                                )
//                        ).withClass("backgrnG border margin1 flex fCol")
//                )
//        ).attr("xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:th=\"http://www.thymeleaf.org\"");
//
//        String renderedHtml = "<!DOCTYPE html>\n" + page.renderFormatted();
//        try {
//            PrintWriter out = new PrintWriter("./EW/src/main/resources/templates/" + poll.getName() + "_ballot.html");
//            out.write(renderedHtml);
//            out.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//
//    }
}
