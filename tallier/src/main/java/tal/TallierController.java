package tal;

import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Controller
public class TallierController {

    private TallierService tallierService;

    public TallierController(TallierService tallierService) {
        this.tallierService = tallierService;
    }

    @PostMapping("setup")
    @ResponseBody
    public Map<String, String> saveRVLAndSendPublicKey(HttpServletRequest request) {
        String jsonString = request.getParameter("json");
        JSONParser parser = new JSONParser(jsonString);
        try {
            Map<String, Object> json = parser.parseObject();
            tallierService.saveRVLs(json);
            System.out.println(json.toString());
            Map<String, String> jsonRes = new HashMap<>();
            jsonRes.put("key", tallierService.getKeyString(tallierService.getPublicKeyPath()));
            return jsonRes;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Map<String,String> voter_workerList p= this.authService.assignWorkerIDs(jsonString);
        return null;
    }

    @PostMapping("tallyBallot")
    @ResponseBody
    public Map<String, String> tallyBallotAndSendTrackingNumber(HttpServletRequest request) {
        try {
            Map<String, Object> json = new JSONParser(request.getParameter("json")).parseObject();

            String signedBallot = (String) json.get("signedHashBallot");
            String encryptedBallot = (String) json.get("encryptedBallot");
            String id = (String) json.get("id");

            tallierService.checkBallots(signedBallot,encryptedBallot,id);
            int trackingNumber = tallierService.addBallotToPollAndGetTrackingNumber(encryptedBallot);
            String talSignedBallot = tallierService.signBallot(encryptedBallot);
            Map <String,String> resJs = new HashMap<>();
            resJs.put("tracking_number",""+trackingNumber);
            resJs.put("signed_ballot",talSignedBallot);
            return resJs;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("decryptBallot")
    @ResponseBody
    public Map<String,String> decryptBallot(HttpServletRequest request){
        try {
            Map<String, Object> json = new JSONParser(request.getParameter("json")).parseObject();
            String trackingNumber = (String) json.get("tracking_number");
            String decryptionKey = (String) json.get("decryptionKey");
            BigInteger ballotLength = (BigInteger) json.get("ballotLength");

            tallierService.decryptBallot(trackingNumber,decryptionKey, ballotLength );

            return new HashMap<>(){{ put("status","ok");}};
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new HashMap<>(){{ put("status","ouff");}};

    }

    @GetMapping("showTally")
    @ResponseBody
    public String showTally(){
        String tally = tallierService.getTally();
        return tally;

    }


}
