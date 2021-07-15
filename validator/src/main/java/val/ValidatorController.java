package val;

import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class ValidatorController {

    private ValidatorService validatorService;

    public ValidatorController(ValidatorService validatorService){
        this.validatorService =validatorService;
    }

    @GetMapping("setup")
    @ResponseBody
    public String saveRVLAndSendPublicKey(@RequestBody String param){
        JSONParser parser = new JSONParser(param);
        try {
            Map<String,Object> json = parser.parseObject();
            validatorService.saveRVL(json);
            System.out.println(json.toString());

        } catch (ParseException e) {
            e.printStackTrace();
        }
        //Map<String,String> voter_workerList p= this.authService.assignWorkerIDs(jsonString);
        return "";


    }

}
