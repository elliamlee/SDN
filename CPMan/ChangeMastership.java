package CPMAN;

import com.eclipsesource.json.JsonObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by monet on 2018-02-28.
 */
public class ChangeMastership {

        public static final String MastershipURL = "http://"
                + "<controllerIP>"
                + ":"
                + "<controllerPort>"
                + "/onos/v1/mastership";

        public static final String CANcontrollerIP = "141.223.84.201";
        //public static final String MigrationTo = "192.168.56.102";

        public ChangeMastership() {
        }

        public WebTarget connectREST(String controllerIP, String url) {
            Client client = ClientBuilder.newClient();
            client.register(HttpAuthenticationFeature.basic("onos", "rocks"));
            return client.target(url);
        }

        public void putRESTwithJson(String url, JsonObject json) {
            WebTarget target = connectREST(CANcontrollerIP, url.replace("<controllerIP>", CANcontrollerIP)
                    .replace("<controllerPort>", "60001"));
            Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
            Response response = builder.put(Entity.entity(json.toString(), MediaType.APPLICATION_JSON));

            if (response.getStatus() != 200) {
                System.out.println("REST Connection Error ");
                return;
            }
        }
}
