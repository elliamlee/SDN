package CPMAN;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.w3c.dom.ls.LSInput;
import sun.security.krb5.internal.crypto.Des;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Calendar;



/**
 * Created by monet on 2018-02-12.
 */

class CPMAN{
    public static final String CANcontrollerIP = "141.223.84.201";

    static final String ONOS1_IP = "192.168.56.101";
    static final String ONOS2_IP = "192.168.56.102";
    static final String ONOS3_IP = "192.168.56.104";

    public static final String CPTrafficRESTURL = "http://"
            + "<controllerIP>"
            + ":"
            + "<controllerPort>"
            + "/onos/cpman/controlmetrics/messages";

    /* getTopology에서 URL 수정한거 참고
    public static final String TopologyRESTURL = "http://"
            + "<controllerIP>"
            + ":"
            + "<controllerPort>"
            + "/onos/v1/devices";
     */

    public static final String mastershipRESTURL = "http://"
            + "<controllerIP>"
            + ":"
            + "<controllerPort>"
            + "/onos/v1/mastership"
            + "<ONOSIP>"
            + "/devices";

    public CPMAN(){

    }

    public static WebTarget connectREST(String controllerIP, String url) {
        Client client = ClientBuilder.newClient();
        client.register(HttpAuthenticationFeature.basic("onos", "rocks"));
        return client.target(url);
    }

    public static List<SWBean>getControlTraffic(String url) {

        List<SWBean> tmpBean;

        WebTarget target = connectREST(CANcontrollerIP, url.replace("<controllerIP>", CANcontrollerIP)
                .replace("<controllerPort>", "60001"));
        // 여러개 할때는 VMcontrollerIP를 string array로 만들어서 저장해둠
        // 그리고 replace("<controllerID>", VMcontrollerIP)에서 VMcontrollerIP 를 array element로 처리
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        if (response.getStatus() != 200) {
            System.out.println("REST Connection Error ");
            return null;
        }

        String rawResults = builder.get(String.class);

        //System.out.println(rawResults);
        RESTParser restParser = new RESTParser();
        tmpBean = restParser.parseGetSwitches(rawResults);
        List<SWBean> SwitchCPTraffic = restParser.parseGetCPTraffic(rawResults,tmpBean);
        //List<SWBean> SwitchCPTraffic = restParser.parseGetSwitches(rawResults);


        return SwitchCPTraffic;
    }

    public static int totalControlPacket(List<SWBean> sourceSwitches) {

        int results = 0;

        for (int index = 0; index < sourceSwitches.size(); index++) {
            results = results + sourceSwitches.get(index).getTotalControlPackets();
        }

        return results;
    }
    public static LinkedList<ControllerWithDevice> DescendingOrdering(HashMap<ControllerWithDevice,List<SWBean>> candidateHashMap){
        HashMap<ControllerWithDevice,List<SWBean>> tmpHashMap = (HashMap<ControllerWithDevice, List<SWBean>>) candidateHashMap.clone();
        LinkedList<ControllerWithDevice> SortingResult = new LinkedList<>();

        for(int index = 0; index < candidateHashMap.size(); index++){
            ControllerWithDevice tmpController = highestController(tmpHashMap);
            SortingResult.add(tmpController);
            tmpHashMap.remove(tmpController);
        }
        return SortingResult;
    }

    public static ControllerWithDevice highestController(HashMap<ControllerWithDevice,List<SWBean>> SourceHashMap){
        Set<ControllerWithDevice> ControllerSet = SourceHashMap.keySet();
        Iterator<ControllerWithDevice> ControllerIt = ControllerSet.iterator();

        if(!ControllerIt.hasNext()){
            return null;
        }

        ControllerWithDevice highestController = ControllerIt.next();
        while(ControllerIt.hasNext()){
            ControllerWithDevice tmpController = ControllerIt.next();
            if(totalControlPacket(SourceHashMap.get(tmpController)) > totalControlPacket(SourceHashMap.get(highestController))){
                highestController = tmpController;
            }
        }
        return highestController;
    }
    public static void MigrationSWBean(SWBean movingSWBean, ControllerWithDevice targetController){

        JsonObject rootObj = new JsonObject();
        rootObj.add("deviceId",movingSWBean.getDpid());
        rootObj.add("nodeId",targetController.ControllerIP); //옮겨갈 곳의 IP
        rootObj.add("role", "MASTER");

        ChangeMastership Mastership = new ChangeMastership();
        Mastership.putRESTwithJson(Mastership.MastershipURL, rootObj);
    }


    public static void main(String[] args) throws IOException, JSchException {

        final ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        int sleepSec = 20;


        exec.scheduleAtFixedRate(new Runnable() {
            int checktime = 0;
            public void run() {
                try {
                    int averageControlPackets = 0;
                    LinkedList<ControllerWithDevice> totalControllerList = new LinkedList<ControllerWithDevice>();
                    LinkedList<ControllerWithDevice> moreControlPackControllerList = new LinkedList<ControllerWithDevice>();
                    LinkedList<ControllerWithDevice> lessControlPackControllerList = new LinkedList<ControllerWithDevice>();

                    //스위치의 dpid만 담아둔 hashmap
                    HashMap<ControllerWithDevice,List<String>> masterRoleSwitches = new HashMap<ControllerWithDevice, List<String>>();


                    HashMap<ControllerWithDevice,List<SWBean>>movingSwitches = new HashMap<ControllerWithDevice,List<SWBean>>();
                    HashMap<ControllerWithDevice,List<SWBean>>candidateSwitches = new HashMap<ControllerWithDevice,List<SWBean>>();

                    //controlTraffic가져오기
                    List<SWBean> CPtrafficList = getControlTraffic(CPTrafficRESTURL);
        /*for(int i =0; i < CPtrafficList.size(); i++){
            System.out.println(CPtrafficList.get(i));
        }*/

                    // 각 컨트롤러에 해당하는 스위치를 가져와서 List만듬
                    getTopology topology = new getTopology();
                    HashMap<String,String> tmphashmap1 = topology.ControllerSeviceSwitches(ONOS1_IP);
                    HashMap<String,String> tmphashmap2 = topology.ControllerSeviceSwitches(ONOS2_IP);
                    HashMap<String,String> tmphashmap3 = topology.ControllerSeviceSwitches(ONOS3_IP);
                    ControllerWithDevice ONOS1Bean = topology.makeControllerBean(tmphashmap1, CPtrafficList);
                    ControllerWithDevice ONOS2Bean = topology.makeControllerBean(tmphashmap2, CPtrafficList);
                    ControllerWithDevice ONOS3Bean = topology.makeControllerBean(tmphashmap3, CPtrafficList);

                    /**Elly:SortingTest**/
                    //ONOS2Bean.SwitchList.get(1).setFlowModPackets(500);
                    /**Elly:SortingTest**/

                    totalControllerList.add(ONOS1Bean);
                    totalControllerList.add(ONOS2Bean);
                    totalControllerList.add(ONOS3Bean);

                    //System.out.print("ONOS_1" + ONOS1Bean + "\n");
                    //System.out.print("ONOS_2" + ONOS2Bean + "\n");
                    //System.out.print("ONOS_3" + ONOS3Bean + "\n");

                    //모든 컨트롤러의 switch ID 받아오기
                    for(int index1= 0; index1< totalControllerList.size(); index1++){
                        ControllerWithDevice tmpControllerWithDevice = totalControllerList.get(index1);
                        List<String> tmpdpidList = new CopyOnWriteArrayList<>();
                        for(int index2 = 0; index2 < tmpControllerWithDevice.SwitchSize(); index2++){
                            String tmpdpid = tmpControllerWithDevice.getSWBean(index2).getDpid();
                            tmpdpidList.add(tmpdpid);
                        }
                        masterRoleSwitches.put(tmpControllerWithDevice,tmpdpidList);

                    }
                    System.out.print("masterRoleSwitches" + masterRoleSwitches + "\n");

                    //모든 컨트롤러에 있는 controlpacket 더하기
                    for(int index = 0; index < totalControllerList.size(); index++){
                        List<SWBean> tmpCPTraffic = totalControllerList.get(index).SwitchList;

                        averageControlPackets = averageControlPackets + totalControlPacket(tmpCPTraffic);
                        //System.out.print("tmpCPTraffic" + totalControlPacket(tmpCPTraffic)+ "\n");
                    }
                    averageControlPackets = averageControlPackets / totalControllerList.size();
                    //System.out.print("averageContorlpackets" + averageControlPackets + "\n");

                    //seperating the controller which has more control packets than average or not
                    for(int index = 0; index < totalControllerList.size(); index++){
                        ControllerWithDevice targetController = totalControllerList.get(index);

                        System.out.print("controllerIP " + totalControllerList.get(index).ControllerIP + "\n");
                        System.out.print("totalcontrolpacket\n" +totalControlPacket(targetController.SwitchList)  + "\n");
                        System.out.print("averageControlPackets\n" + averageControlPackets + "\n");
                        if(totalControlPacket(targetController.SwitchList) > averageControlPackets){
                            moreControlPackControllerList.add(targetController);
                        }else{
                            lessControlPackControllerList.add(targetController);
                        }
                    }
                    System.out.print(" moreControlPackControllerList\n" + moreControlPackControllerList + "\n");
                    System.out.print(" lessControlPackControllerList\n" + lessControlPackControllerList + "\n");

                    //initialize moving Switches hashmap
                    for(int index = 0; index <lessControlPackControllerList.size(); index++){
                        movingSwitches.put(lessControlPackControllerList.get(index), new CopyOnWriteArrayList<SWBean>());
                    }

                    //initialise candidateSwitches hashmap
                    for(int index1 = 0; index1 < moreControlPackControllerList.size(); index1++){
                        candidateSwitches.put(moreControlPackControllerList.get(index1),new CopyOnWriteArrayList<SWBean>());
                        List<SWBean> tmpSWBeanAllCT = moreControlPackControllerList.get(index1).SwitchList;
                        List<String> tmpMasterSWList = masterRoleSwitches.get(moreControlPackControllerList.get(index1));
                        Set<String> tmpMasterSWSet = new CopyOnWriteArraySet<>();

                        for(int index2 = 0; index2 < tmpMasterSWList.size(); index2++){
                            tmpMasterSWSet.add(tmpMasterSWList.get(index2));
                        }
                        for(int index2 = 0; index2 < tmpSWBeanAllCT.size(); index2++) {
                            if (tmpMasterSWSet.contains(tmpSWBeanAllCT.get(index2).getDpid())) {
                                candidateSwitches.get(moreControlPackControllerList.get(index1)).add(tmpSWBeanAllCT.get(index2));
                            }
                        }

                    }
                    /**Elly:PrintingTest**
                     System.out.print("candidateSwitches\n" + candidateSwitches + "\n");
                     /**Elly:PrintingTest**/

                    //averageControlPacket 보다 control packet이 많은 controller를 내림차순정리 (많은게 우선)
                    LinkedList<ControllerWithDevice> DesOrderController = DescendingOrdering(candidateSwitches);
                    int numControlPacket = 0;
                    /**Elly:PrintingTest**
                     for(int index = 0; index < DesOrderController.size(); index++){
                     System.out.print("asOrderController\n" + DesOrderController.get(index).ControllerIP+ "\n");
                     }
                     /**Elly:PrintingTest**/

                    /**Elly:PrintingTest**/
                    System.out.print("DesOrderController\n" + DesOrderController + "\n");
                    /**Elly:PrintingTest**/

                    // mastership이 변경될 적절한 switch를 골라서 movingSwitches 목록에 저장
                    for(int index1 = 0; index1 < DesOrderController.size(); index1++){
                        ControllerWithDevice MaxController = DesOrderController.get(index1);
                        numControlPacket = totalControlPacket(MaxController.SwitchList);

                        for(int index2 = 0; index2 < lessControlPackControllerList.size(); index2++){
                            //Maxcontroller의 스위치 목록 사이즈를 말함
                            int MaxControllerSize = candidateSwitches.get(MaxController).size();
                            for(int index3 = 0; index3 < MaxControllerSize; index3++){
                                SWBean movingSWBean = candidateSwitches.get(MaxController).get(index3);
                                /**Elly:PrintingTest**
                                 int printtmp =  numControlPacket - movingSWBean.getTotalControlPackets();
                                 System.out.print(" numControlPacket" +  numControlPacket +"\n");
                                 System.out.print(" movingSWBeanTotalPacket" +  movingSWBean.getTotalControlPackets() +"\n");
                                 System.out.print(" printtmp" +  printtmp +"\n\n");

                                 System.out.print("totalControlPacket(lessControlPackControllerList.get(index2).SwitchList)" + totalControlPacket(lessControlPackControllerList.get(index2).SwitchList) +"\n");
                                 System.out.print(" movingSWBeanTotalPacket" +  movingSWBean.getTotalControlPackets() +"\n");
                                 System.out.print(" totalControlPacket(movingSwitches.get(lessControlPackControllerList.get(index2))" +  totalControlPacket(movingSwitches.get(lessControlPackControllerList.get(index2))) +"\n");
                                 int abcprint =totalControlPacket(lessControlPackControllerList.get(index2).SwitchList)
                                 + movingSWBean.getTotalControlPackets()
                                 + totalControlPacket(movingSwitches.get(lessControlPackControllerList.get(index2)));
                                 System.out.print(" Result" + abcprint + "\n");
                                 System.out.print(" movingSWBean" +  movingSWBean +"\n");
                                 /**Elly:PrintingTest**/

                                if(numControlPacket - movingSWBean.getTotalControlPackets() > averageControlPackets
                                        && totalControlPacket(lessControlPackControllerList.get(index2).SwitchList)
                                        + movingSWBean.getTotalControlPackets()
                                        + totalControlPacket(movingSwitches.get(lessControlPackControllerList.get(index2))) //옮겨진 후의 control packet고려
                                        < averageControlPackets){
                                    /**Elly:PrintingTest**
                                     System.out.print("Into if문:\n");
                                     System.out.print("movingSWBean" + movingSWBean.getDpid() + "\n");
                                     /**Elly:PrintingTest**/
                                    movingSwitches.get(lessControlPackControllerList.get(index2)).add(movingSWBean);
                                    numControlPacket = numControlPacket - movingSWBean.getTotalControlPackets();
                                    candidateSwitches.get(MaxController).remove(movingSWBean);
                                    MaxControllerSize = candidateSwitches.get(MaxController).size();
                                    index3--;
                                }
                            }
                        }
                        numControlPacket = 0;
                    }

                    /**Elly:PrintingTest**/
                    System.out.print("movingSwitches" + movingSwitches);
                    /**Elly:PrintingTest**/

                    /**Elly:PrintingTest**/
                    System.out.print("Before_totalControolerList" + totalControllerList);
                    /**Elly:PrintingTest**/

                    for(int index1 = 0; index1 < lessControlPackControllerList.size(); index1++){
                        ControllerWithDevice getSwitchController = lessControlPackControllerList.get(index1);
                        List<SWBean> MastershipSWBean = movingSwitches.get(getSwitchController);
                        for(int index2 = 0; index2 < MastershipSWBean.size(); index2++){
                            MigrationSWBean(MastershipSWBean.get(index2), getSwitchController);
                        }
                    }

                    System.out.print("===============checktime"+ checktime +"=================\n");
                    checktime++;

                }catch (Exception e) {
                    e.printStackTrace();
                    exec.shutdown();
                }
            }
        },0,sleepSec, TimeUnit.SECONDS);
    }

}
