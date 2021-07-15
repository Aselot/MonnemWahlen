package monnemwahlen.mw;

import java.util.HashMap;
import java.util.Map;

public interface MoService {

    String IP = "localhost";
    Map<String, String> ports = new HashMap<>() {{
        put("auth", "8081");
        put("ew", "8082");
        put("val", "8083");
        put("tal", "8084");
    }};


    default String getUrl(String entity){
        return IP+":"+ports.get(entity);
    }

}
