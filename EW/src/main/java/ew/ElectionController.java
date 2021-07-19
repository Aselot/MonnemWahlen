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

        System.out.println("check if worker exists");
        if (!this.electionService.checkIfWorkerExist(workerCount)) {
            System.out.println("no workers, generating workers");
            electionService.deleteWorkers();
            workerList = electionService.createNewWorkers(workerCount);
            System.out.println("save workers");
            electionService.saveWorkers(workerList);
        }

        workerList = electionService.loadWorkers();
        System.out.println("send Worker IDs to auth");
        electionService.sendWorkerIDs(workerList);
        System.out.println("send rvl Lists to entities and get their public keys");
        electionService.getPublicKeysOfEntitiesAndSendRVLs();

    }

    //we could passelection worker public key with mapping(value = key)
    @GetMapping(path = "/submitVote/{poll_id}/{worker_id}/{voter_id}")
    public String sendVotingPage(@PathVariable(value = "poll_id") String poll_id,
                                 @PathVariable(value = "worker_id") String worker_id,
                                 @PathVariable(value = "voter_id") String voter_id,
                                 HttpServletRequest request,
                                 Model model) {

        // for multiple elections, we would use the election hash to differentiate

        request.setAttribute("voter_id",voter_id);
        if(electionService.checkIfWorkerAndVoterAlign(worker_id,voter_id) &&
        electionService.voterHasNotVoted(worker_id,voter_id)){
            //Poll poll = electionService.getPoll(poll_id);
          //  List<Candidate> candidateList = electionService.getCandidates(poll);
            //String ballotFilename = this.electionService.createAndSaveUserHtmlPage(poll,candidateList,voter_id,worker_id);
            model.addAttribute("worker_id",worker_id);
            model.addAttribute("voter_id",voter_id);
            model.addAttribute("poll_id",poll_id);
            model.addAttribute("url", electionService.getUrl("ew"));
            return "poll1_ballot";
        }
        return "alryVoted";
    }

    @PostMapping("/postBallot")
    @ResponseBody
    public String postBallotInfo(@RequestBody String param, HttpServletRequest request,
                                 HttpServletResponse response, Model model){
        JSONParser parser = new JSONParser(param);
        try {
            Map<String,Object> json = parser.parseObject();

            String trackingNumber = electionService.giveBallotToElectionWorker(json);


            return trackingNumber;


        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Map<String,String> voter_workerList p= this.authService.assignWorkerIDs(jsonString);
        return "Not valid Ballot";
    }

}
