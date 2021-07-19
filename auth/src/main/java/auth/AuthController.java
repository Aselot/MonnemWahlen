package auth;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;

@Controller
//@RequestMapping(path="auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = new AuthService();

    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    //@ResponseBody
    public String getLoginPage() {
        return "login";
    }

    @GetMapping("logredirect")
    public ModelAndView redirectUser(@RequestParam(value = "name", required = true) String name,
                                     @RequestParam(value = "password", required = true) String password){

        if(this.authService.checkCredentials(name,password)){
            // TODO: url of electionworker here
            String worker_id = authService.getElectionWorker(name);
            String voter_id = authService.getVoterID(name);
            String url = String.format(authService.getUrl("ew")+"/submitVote/poll1/%s/%s",worker_id,voter_id );
            return new ModelAndView("redirect:"+url);
        }

        return new ModelAndView("login");
    }

    @GetMapping("logErr")
    public String showLoginSiteWError(){
        return "login";
    }

    @PostMapping("setWorkers")
    @ResponseBody
    public Map<String, String> saveAndAssignWorkerIDs(HttpServletRequest request,
                                                      HttpServletResponse response, Model model) throws SQLException {
        String jsonString = request.getParameter("json");

        Map<String,String> voter_workerList = this.authService.assignWorkerIDs(jsonString);
        this.authService.saveVoterWorkerListToDB(voter_workerList);
        return voter_workerList;

    }
}
