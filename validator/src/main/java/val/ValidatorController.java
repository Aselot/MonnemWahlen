package val;

import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ValidatorController {

    private ValidatorService validatorService;

    public ValidatorController(ValidatorService validatorService){
        this.validatorService =validatorService;
    }

    @PostMapping("setup")
    @ResponseBody
    public Map<String, String>  saveRVLAndSendPublicKey( HttpServletRequest request){
        String jsonString = request.getParameter("json");
        JSONParser parser = new JSONParser(jsonString);
        try {
            Map<String,Object> json = parser.parseObject();
            validatorService.saveRVL(json);
            System.out.println(json.toString());
            Map<String, String> jsonRes = new HashMap<>();
            jsonRes.put("key", validatorService.getKeyString(validatorService.getPublicKeyPath()));
            return jsonRes;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Map<String,String> voter_workerList p= this.authService.assignWorkerIDs(jsonString);
        return null;
    }

    @GetMapping("getPK")
    @ResponseBody
    public Map<String, Object> getPK(){
        Map<String, Object> resJs = new HashMap<>();
        resJs.put("pk", validatorService.getKeyString(validatorService.getPublicKeyPath()));
        return resJs;

    }

    @PostMapping("validateBallot")
    @ResponseBody
    public Map<String, Object> validateBallot(HttpServletRequest request){
        String jsonString = request.getParameter("json");
        JSONParser parser = new JSONParser(jsonString);
        try{
            Map<String,Object> json = parser.parseObject();
            String ballot = validatorService.checkForSignature(json);
            if(!ballot.equals("")){
                Map<String, Object> resJs = new HashMap<>();
                resJs.put("ballot",validatorService.getSignedBallot(ballot));
                return resJs;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


}
