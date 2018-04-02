package CPMAN;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by monet on 2018-02-21.
 */
public class getTopology {
    public static final String CANcontrollerIP = "141.223.84.201";
    public final String TopologyRESTURL = "http://"
            + "<controllerIP>"
            + ":"
            + "<controllerPort>"
            + "/onos/v1/mastership"
            + "/<ONOSIP>"
            + "/device";

    public getTopology(){
    }
    public static WebTarget connectREST(String controllerIP, String url) {
        Client client = ClientBuilder.newClient();
        client.register(HttpAuthenticationFeature.basic("onos", "rocks"));
        return client.target(url);
    }
    public static String putRESTwithJson(String url, String VMcontrollerIP) {
        String resultString;

        WebTarget target = connectREST(CANcontrollerIP, url.replace("<controllerIP>", CANcontrollerIP)
                .replace("<controllerPort>", "60001").replace("<ONOSIP>", VMcontrollerIP));
        // 여러개 할때는 VMcontrollerIP를 string array로 만들어서 저장해둠
        // 그리고 replace("<controllerID>", VMcontrollerIP)에서 VMcontrollerIP 를 array element로 처리
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        if (response.getStatus() != 200) {
            System.out.println("REST Connection Error ");
            return null;
        }
        resultString = builder.get(String.class);

        return resultString;
    }

    public static JsonArray parseGetSwitches(String jsonRaw, String putter) {

        //Json으로 모든 Device를 받아와서 List로 저장함
        JsonObject parser = JsonObject.readFrom(jsonRaw);
        JsonArray DeviceList = parser.get(putter).asArray();
        return DeviceList;
    }

    public HashMap<String,String> ControllerSeviceSwitches(String IPaddress){
        HashMap<String, String> ResultServicesSwitch = new HashMap<>();

        //putRESTJson에 ONOS의 ip주소를 넣어서 완전한 url만들기
        // 결과적으로 onos/v1/mastership/<deviceIP>/device라는 주소가 만들어짐
        String MonitorURL_ONOS = getTopology.putRESTwithJson(TopologyRESTURL,IPaddress);

        // 완벽한 주소를 parseGetSwitches에 넣어서 DeviceID의 list를 만들어낸다.
        // DeviceList에서 ONOS controller가 서비스를 제공하는 DeviceID를 String으로 가져옴
        // 그 string을 hashmap의 key값으로 넣고 value로는 각 controller의 IPaddress를 넣어서 저장함.
        JsonArray getDeviceList = parseGetSwitches(MonitorURL_ONOS,"deviceIds");
        for (int index = 0; index < getDeviceList.size(); index++) {
            String DeviceID = getDeviceList.get(index).asString();
            ResultServicesSwitch.put(DeviceID,IPaddress);
        }
        // 이 hashmap을 return
        return ResultServicesSwitch;
    }
    public ControllerWithDevice makeControllerBean(HashMap<String,String> ControllerSwitch, List<SWBean> CPTrafficList){

        ControllerWithDevice tmpController = new ControllerWithDevice();
        Set key = ControllerSwitch.keySet();
        String getControllerIP = new String();

        for(Iterator iterator = key.iterator(); iterator.hasNext();){
            String DeviceID = (String)iterator.next();
            getControllerIP = (String)ControllerSwitch.get(DeviceID);
            for(int index1 = 0; index1 < CPTrafficList.size(); index1++)
            {
                SWBean targetBean = CPTrafficList.get(index1);
                if(DeviceID.equals(CPTrafficList.get(index1).getDpid()))
                {
                    tmpController.addSWBean(targetBean);
                }
            }
        }
        tmpController.ControllerIP = getControllerIP;

        return tmpController;
    }
}
