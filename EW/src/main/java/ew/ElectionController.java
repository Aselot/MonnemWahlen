package ew;

import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Controller
public class ElectionController {


    private int workerCount = 2;
    private ElectionService electionService;
    private Map<String, ElectionWorker> workerList;

    public ElectionController(ElectionService electionService) {
        this.electionService = electionService;

        if (!this.electionService.checkIfWorkerExist(workerCount)) {
            this.electionService.deleteWorkers();
            this.workerList = this.electionService.createNewWorkers(workerCount);
            this.electionService.saveWorkers(workerList);
        }
        this.workerList = this.electionService.loadWorkers();
        this.electionService.sendWorkerIDs(workerList);

    }

    //we could passelection worker public key with mapping(value = key)
    @GetMapping(path = "/submitVote/{pollhash}/{worker_id}/{voter_id}")
    public String sendVotingPage(@PathVariable(value = "pollhash") String pollhash,
                                 @PathVariable(value = "worker_id") String worker_id,
                                 @PathVariable(value = "voter_id") String voter_id,
                                 HttpServletRequest request) {

        // for multiple elections, we would use the election hash to differentiate

        request.setAttribute("voter_id",voter_id);
        if(this.electionService.checkIfWorkerAndVoterAlign(worker_id,voter_id)){
            Poll poll = electionService.getPoll(pollhash);
            List<Candidate> candidateList = electionService.getCandidates(poll);
            String ballotFilename = this.electionService.createAndSaveUserHtmlPage(poll,candidateList,voter_id,worker_id);
            return ballotFilename;
        }
        return "";
    }

    @PostMapping("/postBallot")
    public String postBallotInfo(@RequestBody String param, HttpServletRequest request,
                                 HttpServletResponse response, Model model){
        JSONParser parser = new JSONParser(param);
        try {
            Map<String,Object> json = parser.parseObject();
            electionService.giveBallotToElectionWorker(json);
            System.out.println(json.toString());

        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Map<String,String> voter_workerList p= this.authService.assignWorkerIDs(jsonString);
        return "";
    }

}
