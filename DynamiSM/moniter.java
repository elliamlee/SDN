package utils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by monet on 2017-06-14.
 */
/*class mastershipinmonitor {
    public static final String CANcontrollerIP = "128.110.153.202";
    public static final String RESTURL_DOMASTERSHIP = "http://"
            + "<controllerIP>"
            + ":"
            + "<controllerPort>"
            + "/onos/v1/mastership";
    public static final String RESTURL_PREFIX = "http://<controllerIP>:<controllerPort>";
    //public static final String RESTURL_DOMASTERSHIP = RESTURL_PREFIX + "/onos/v1/mastership";
    //public static final String MigrationTo = "192.168.56.102";
    // small topology: 141.223.84.201
    public mastershipinmonitor() {
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
}*/
class Switch {

    public String Device_ID;
    public Double CPU_Load;
    public String IP_Add;
    public Boolean Migration_check;
    public Boolean SwitchOnce;

    public Switch(String D_ID, String IP, Double CPU, Boolean Mi_Flag , Boolean S_once){
        this.Device_ID = D_ID;
        this.CPU_Load = CPU;
        this.IP_Add = IP;
        this.Migration_check = Mi_Flag;
        this.SwitchOnce = S_once;
    }
    public String toString(){
        return "(D_ID :" + Device_ID + ", IP :" + IP_Add + ", CPU :" + CPU_Load +  ", Migration :" + Migration_check +  ", SwitchOnce :" + SwitchOnce +")\n";
    }
}

class ControllerWithDevice{

    LinkedList<Switch> SwitchList = new LinkedList<>();
    Boolean ControllerOnce; //이 컨트롤러에서 한번이라도 migration이 있었으면 1 아예 한번도 한적없으면 0
    String ControllerIP;

    public ControllerWithDevice(){
        ControllerIP = "0";
        ControllerOnce = false; //일단 초기 세팅은 false이지만 migration이 끝나는 과정에서는 제대로 setting해주어야함
    }

    public int Add(String D_ID, String IP, Double CPU, Boolean Flag, Boolean SW_onceFlag) {
        Switch s = new Switch(D_ID,IP,CPU,Flag,SW_onceFlag);
        SwitchList.add(s);
        return 0;
    }
    public int AddWholeDevice(Switch wholeDevice) {
        SwitchList.add(wholeDevice);
        return 0;
    }
    public Switch WholeDevice(int i){
        Switch Device = SwitchList.get(i);
        return Device;
    }

    public void REMOVE(String removingID){

        Iterator<Switch> it = SwitchList.iterator();

        while(it.hasNext()){
            Switch Device = it.next();
            if((Device.Device_ID).equals(removingID) == true) {
                Switch removingDevice = Device;
                //System.out.print("REMOVE전에 Device   " + SwitchList+ "\n");
                SwitchList.remove(removingDevice);
                //System.out.print("REMOVE후에 Device  " + SwitchList + "\n");
                break;
            }
        }
    }

    // 옮기기전의 controller에서 해당 device를 찾는 함수
    public Switch FIND_DEVICE(String removingID){
        Iterator<Switch> it = SwitchList.iterator();
        Boolean TF = false;
        Switch TrashDevice = new Switch("-1","-1",-1.0,false,false);
        Switch findDevice = new Switch("0","0",0.0,false,false);

        while(it.hasNext()) {  //controller의 device목록을 돌면서 찾으면 t 없으면 f
            Switch Device = it.next();
            if ((Device.Device_ID).equals(removingID) == true) {
                TF = true;
                findDevice = Device;
                System.out.print("FindDevice 함수 안의 device: " + findDevice);
                break;
            }else{
                TF = false;
            }
        }

        if(TF == true) return findDevice;
        else return TrashDevice; // 찾는 device가 없으면 쓰레기 값을 return한다.
    }
    // ONOS3_Controller.get(checktime).EQUAL_ID(removing_ID)의 형식으로 쓰임
    // 해당 checktime의 controller의 device를 돌면서 같은거 찾기
    public Boolean EQUAL_DEVICE(Switch RemoveingDevice){

        Iterator<Switch> it = SwitchList.iterator();
        Boolean Equaility = false;

        while(it.hasNext()){
            Switch Device = it.next();
            if((Device.Device_ID).equals(RemoveingDevice.Device_ID) == true) {
                Equaility = true;
                break;
            }else{
                Equaility = false;
            }
        }
        return Equaility;
    }

    //time에 controller안에 몇개의 스위치가 들어있는지 계산하는 함수
    public int NumofCon(){
        Iterator<Switch> it = SwitchList.iterator();
        int count = 0;

        while(it.hasNext()){
            Switch Device = it.next();
            if((Device.Device_ID) != null)
                count++;
        }

        return count;
    }

    public Boolean BringMigFlag(String BringDeviceID){
        Iterator<Switch> it = SwitchList.iterator();
        Boolean TF = false;
        Boolean BringMigflag = null;

        while(it.hasNext()) {  //controller의 device목록을 돌면서 찾으면 t 없으면 f
            Switch Device = it.next();
            if ((Device.Device_ID).equals(BringDeviceID) == true) {
                TF = true;
                BringMigflag = Device.Migration_check;
                //System.out.print("Migration Flag: " + BringMigflag + "\n");
                break;
            }else{
                TF = false;
            }
        }

        if(TF == true) return BringMigflag;
        else{
            System.out.println("BringMigFlag: 찾으려는 deviceID와 일치하는 게 없어서 null을 return \n");
            return BringMigflag; // 찾는 device가 없으면 쓰레기 값을 return한다.
        }
    }

    public Boolean BringSOnceFlag(String BringDeviceID){
        Iterator<Switch> it = SwitchList.iterator();
        Boolean TF = false;
        Boolean BringSOnceflag = null;

        while(it.hasNext()) {  //controller의 device목록을 돌면서 찾으면 t 없으면 f
            Switch Device = it.next();
            if ((Device.Device_ID).equals(BringDeviceID) == true) {
                TF = true;
                BringSOnceflag = Device.SwitchOnce;
                //System.out.print("Switch Once Flag: " + BringSOnceflag + "\n");
                break;
            }else{
                TF = false;
            }
        }

        if(TF == true) return BringSOnceflag;
        else{
            System.out.println("BringSOnceflag: 찾으려는 deviceID와 일치하는 게 없어서 null을 return \n");
            return BringSOnceflag; // 찾는 device가 없으면 쓰레기 값을 return한다.
        }
    }

    public Double BringCPU(String BringDeviceID){
        Iterator<Switch> it = SwitchList.iterator();
        Boolean TF = false;
        Double BringCPU = null;

        while(it.hasNext()) {  //controller의 device목록을 돌면서 찾으면 t 없으면 f
            Switch Device = it.next();
            if ((Device.Device_ID).equals(BringDeviceID) == true) {
                TF = true;
                BringCPU = Device.CPU_Load;
                //System.out.print("Switch Once Flag: " + BringSOnceflag + "\n");
                break;
            }else{
                TF = false;
            }
        }

        if(TF == true) return BringCPU;
        else{
            System.out.println("BringCPU: 찾으려는 deviceID와 일치하는 게 없어서 null을 return \n");
            return BringCPU; // 찾는 device가 없으면 쓰레기 값을 return한다.
        }
    }

    public void SetConOnceFlag () {
        Iterator<Switch> it = SwitchList.iterator();

        while (it.hasNext()) {
            Switch Device = it.next();
            if((Device.SwitchOnce).equals(true) == true){
                ControllerOnce = true;
                break;
            }else{
                ControllerOnce = false;
            }
        }
    }


    public void setCPU(int i,Double newvalue){  // i번째 스위치의 CPU Load를 새롭게 setting함
        SwitchList.get(i).CPU_Load = newvalue;
    }
    public void setFlag(int i,Boolean newflag){ // i번째 스위치의 flag를 setting해줌

        SwitchList.get(i).Migration_check = newflag;
    }
    public String getID(int i){
        return String.valueOf(SwitchList.get(i).Device_ID);
    }
    public Double getCPU(int i){
        return Double.valueOf(SwitchList.get(i).CPU_Load);
    }

    //removingID가 해당되어 있는 controller의 IP주소를 가져오는 함수
    public String getIP(String removingID){
        Boolean TF = false;
        String RetrunIP = new String();
        String TrashIP = "-1";
        Iterator<Switch> it = SwitchList.iterator();

        while(it.hasNext()){
            Switch Device = it.next();
            if((Device.Device_ID).equals(removingID) == true){
                RetrunIP = Device.IP_Add;
                TF = true;
                //System.out.print("IPGET true\n");
                break;
            }else if((Device.Device_ID).equals(removingID) == false){
                TF = false;
               //System.out.print("IPGET False\n");
            }
        }

        if(TF == true) return RetrunIP;
        else return TrashIP;
    }
    ////////////////////migration한 device의 IP바꾸기///////////////////////////
    public void SETIP(String removingID, String ONOS_IP){
        Iterator<Switch> it = SwitchList.iterator();

        while(it.hasNext()){
            Switch Device = it.next();
            if((Device.Device_ID).equals(removingID) == true){
                Device.IP_Add = ONOS_IP;
                break;
            }
        }

    }
    ////////////////////migration한 device의 IP바꾸기///////////////////////////

    public Boolean getMigFlag(int i){
        return Boolean.valueOf(SwitchList.get(i).Migration_check);
    }
    public Boolean getSWonceFlag(int i){
        return Boolean.valueOf(SwitchList.get(i).SwitchOnce);
    }

    public String toString(){
        return "ControllerOnce:" + ControllerOnce + "\n-------------------SwitchList-------------------\n"+String.valueOf(SwitchList);
    }


}

class monitor {
    /******************Mininet 켜기전에 idle한 CPU 값_ 7월27일************************************/
    public static final double Idle_ONOS1 = 0.0;
    public static final double Idle_ONOS2 = 0.0;
    public static final double Idle_ONOS3 = 0.0;
    public static final double Idle_ONOS_AVG = (Idle_ONOS1 + Idle_ONOS2 + Idle_ONOS3) / 3 ;
    /*******************************************************************************************/

    public static final String RESTURL_PREFIX = "http://<controllerIP>:<controllerPort>";
    //public static final String RESTURL_DOMASTERSHIP = RESTURL_PREFIX + "/onos/v1/mastership";
    public static final String RESTURL_DOMASTERSHIP = "http://"
            + "<controllerIP>"
            + ":"
            + "<controllerPort>"
            + "/onos/v1/mastership";
    public static final String RESTURL_MONITERPREFIX = RESTURL_DOMASTERSHIP + "/<controllerID>";
    public static final String RESTURL_MONITER = RESTURL_MONITERPREFIX + "/device";
    public static final String CANcontrollerIP = "128.110.153.202";

    static final ArrayList<LinkedHashMap<String, Double>> MatchTimetoCPU2 = new ArrayList<>();
    static final ArrayList<LinkedHashMap<String, Double>> Controller_IdleCPU = new ArrayList<>();

    static final LinkedList<ControllerWithDevice> ONOS1_Controller = new LinkedList <ControllerWithDevice>();
    static final LinkedList<ControllerWithDevice> ONOS2_Controller = new LinkedList <ControllerWithDevice>();
    static final LinkedList<ControllerWithDevice> ONOS3_Controller = new LinkedList <ControllerWithDevice>();

    /*static final String ONOS1_IP = "192.168.201.101";
    static final String ONOS2_IP = "192.168.201.102";
    static final String ONOS3_IP = "192.168.201.103";*/
    static final String ONOS1_IP = "192.168.201.101";
    static final String ONOS2_IP = "192.168.201.102";
    static final String ONOS3_IP = "192.168.201.103";

    //static Boolean excuted[] = {false,false,false};

    public monitor() {

    }

    public static WebTarget connectREST(String controllerIP, String url) {
        Client client = ClientBuilder.newClient();
        client.register(HttpAuthenticationFeature.basic("onos", "rocks"));
        return client.target(url);
    }

    public static String putRESTwithJson(String url,String VMcontrollerIP) {
        String resultString;

        WebTarget target = connectREST(CANcontrollerIP, url.replace("<controllerIP>", CANcontrollerIP)
                .replace("<controllerPort>", "60001").replace("<controllerID>", VMcontrollerIP));
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

    public static HashMap<String,String> ControllerSeviceSwitches(String IPaddress){
        HashMap<String, String> ResultServicesSwitch = new HashMap<>();

        //putRESTJson에 ONOS의 ip주소를 넣어서 완전한 url만들기
        // 결과적으로 onos/v1/mastership/<deviceIP>/device라는 주소가 만들어짐
        String MonitorURL_ONOS = monitor.putRESTwithJson(RESTURL_MONITER,IPaddress);

        // 완벽한 주소를 parseGetSwitches에 넣어서 DeviceID의 list를 만들어낸다.
        // DeviceList에서 ONOS controller가 서비스를 제공하는 DeviceID를 String으로 가져옴
        // 그 string을 hashmap의 key값으로 넣고 value로는 각 controller의 IPaddress를 넣어서 저장함.
        JsonArray getDeviceList = monitor.parseGetSwitches(MonitorURL_ONOS,"deviceIds");
        for (int index = 0; index < getDeviceList.size(); index++) {
            String DeviceID = getDeviceList.get(index).asString();
            ResultServicesSwitch.put(DeviceID,IPaddress);
        }
        // 이 hashmap을 return
        return ResultServicesSwitch;
    }

    public static String ConnSSH() throws JSchException, IOException {
        String results = null;
        StringBuffer sb = new StringBuffer();

        //Connect SSH
        Session session = new JSch().getSession("wkim", CANcontrollerIP, Integer.parseInt("22"));
        Properties conf = new Properties();
        conf.put("StrictHostKeyChecking", "no");
        session.setConfig(conf);
        session.setPassword("monetghgh");
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("vboxmanage metrics query '*' CPU/Load/User:avg,CPU/Load/Kernel:avg");
        channel.connect();

        InputStreamReader ir = new InputStreamReader((channel.getInputStream()));
        char[] tmpBuf = new char[4096];
        while(ir.read(tmpBuf) != -1) {
            sb.append(tmpBuf);
        }
        results = sb.toString();

        channel.disconnect();
        session.disconnect();

        //결과는 results에 string으로 저장
        System.out.println(results);
        return results;
    }

    // SSH로 받은 string을 분할해서 저장하고 이를 더해서 Double값으로 hashmap에 저장
    public static HashMap< String, Double > resultHashMap (String tmpConnResult){

        int nameindex = 0;
        int resultindex = 0;
        int bringIndex = 0;
        int h = 1;
        int bringh = 0;
        Double cutPercent = 0.0;

        String[] onosname = new String[10];
        Double[] hello = new Double[100];
        Double[] tmpArray = new Double[100];
        HashMap<String, Double> AddKernelUser = new HashMap<>(); //Kernel의 CPU load와 kernel의 CPU load를 합쳐둔 배열
        // tmpConnResult에서 개행문자가 나타났을 때까지의 한줄을 String으로 간주하고
        // 이 string을 tmpList라는 array로 저장
        String[] tmpList = tmpConnResult.split("\n");

        //host,onos-1,onos-2,onos-3의 USER CPU만을 저장
        for(int index = 2; index < tmpList.length-1; index++){
            String tmpLine = tmpList[index];                     //tmplist를 순서대로 string으로 받고
            String[] tmpResult = tmpLine.split("\\s+");  // 여기서 띄어쓰기로 구분되어 있는 문자들을 string array로 저장
            String tmpName = tmpResult[0];
            onosname[nameindex++] = tmpName;
            String exceptPercent = tmpResult[2].replace("%",""); // 우리가 필요한 부분은 CPU Load뿐임으로 배열의 2번째만을 저장한다.
            tmpArray[resultindex++] = Double.valueOf(exceptPercent); // host,onos1,onos2,onos3의 CPU load를 resultarray에 저장
            bringIndex = index;
        }


        //각각 따로 저장한 kernel과 user의 CPU load를 더해서 onos#를 붙여서 Hashmap에 저장 (소수점 두번째자리)
        for(int c = 2; c < bringIndex - 1; c = c + 2){
            double addtmpArray = Double.parseDouble(String.format("%.3f", (tmpArray[c] + tmpArray[c + 1])));
            AddKernelUser.put(onosname[2 * h] , addtmpArray);
            h++;
        }
        System.out.println(AddKernelUser);
        return AddKernelUser; ////Kernel의 CPU load와 kernel의 CPU load를 합쳐둔 배열을 최종적으로 return
    }

    //controller 3개의 평균 cpu를 구하는 함수 (소수점 두번째자리)
    public static Double AvgCPU (HashMap<String, Double> IdleCPU_onemin){
        double AvgResult = 0.0;
        double sum = 0.0;
        double change = 0.0;
        Collection values = IdleCPU_onemin.values();
        Iterator iter = values.iterator();

        while(iter.hasNext()){
            sum += (double)iter.next();
        }

        AvgResult = sum / IdleCPU_onemin.size();
        change = Double.parseDouble(String.format("%.3f", AvgResult));
        //System.out.print("ConvertStingDouble.size()" + ConvertStingDouble.size());
        //System.out.print("AvgResult" + AvgResult);
        return change;

    }

    //packet processing에 쓰이는 CPU load만을 구하기 위해서 idle한 값을 빼는 함수
    //(소수점 두번째자리)
    public static LinkedHashMap<String, Double> SubtractIdle (LinkedHashMap<String, Double> tmp_hash){
        String IdleONOS1 = "onos-1";
        String IdleONOS2 = "onos-2";
        String IdleONOS3 = "onos-3";
        double idle_onos1 = 0.0;
        double idle_onos2 = 0.0;
        double idle_onos3 = 0.0;

        LinkedHashMap<String, Double> ResultHashMap = new LinkedHashMap<>();
        Iterator <String> itr =  tmp_hash.keySet().iterator();

        while (itr.hasNext()){
            String key = (String)itr.next();
            if(IdleONOS1.equals(key)){
                idle_onos1 = Double.parseDouble(String.format("%.3f",(tmp_hash.get(key) - Idle_ONOS1)));
                ResultHashMap.put(key,idle_onos1);
            }else if(IdleONOS2.equals(key)){
                idle_onos2 = Double.parseDouble(String.format("%.3f",(tmp_hash.get(key) - Idle_ONOS2)));
                ResultHashMap.put(key,idle_onos2);
            }else if(IdleONOS3.equals(key)){
                idle_onos3 = Double.parseDouble(String.format("%.3f",(tmp_hash.get(key) - Idle_ONOS3)));
                ResultHashMap.put(key,idle_onos3);
            }
        }

        //System.out.println("\nMatchTimeCPU2에서 Idle한 값을 뺀 결과" + ResultHashMap);

        return ResultHashMap;
    }

    //idle한 값을 뺀 CPU load에서 devide의 개수로 나누는 함수
    //(소수점 두번째자리)
    public static LinkedHashMap<String, Double> DivideSize (ArrayList<LinkedHashMap<String, Double>> Controller_IdleCPU, int time, Integer SizeofONOS1, Integer SizeofONOS2, Integer SizeofONOS3){
        String IdleONOS1 = "onos-1";
        String IdleONOS2 = "onos-2";
        String IdleONOS3 = "onos-3";
        double divideonos1 = 0.0;
        double divideonos2 = 0.0;
        double divideonos3 = 0.0;

        LinkedHashMap<String, Double> ResultHashMap = new LinkedHashMap<>();

        Iterator <String> itr =  Controller_IdleCPU.get(time).keySet().iterator();

        while (itr.hasNext()){
            String key = (String)itr.next();
            if(IdleONOS1.equals(key)){
                divideonos1 = Double.parseDouble(String.format("%.3f",(Controller_IdleCPU.get(time).get(key))/SizeofONOS1) );
                ResultHashMap.put(key,divideonos1);
                //System.out.println("\n(Controller_IdleCPU.get(time).get(key))" + (Controller_IdleCPU.get(time).get(key)));
            }else if(IdleONOS2.equals(key)){
                divideonos2 = Double.parseDouble(String.format("%.3f",(Controller_IdleCPU.get(time).get(key))/SizeofONOS2) );
                ResultHashMap.put(key,divideonos2);
            }else if(IdleONOS3.equals(key)){
                divideonos3 = Double.parseDouble(String.format("%.3f",(Controller_IdleCPU.get(time).get(key))/SizeofONOS3) );
                ResultHashMap.put(key,divideonos3);
            }
        }

        //System.out.println("\nMatchTimeCPU2에서 Idle한 값을 뺀 걸 사이즈로 나눈 결과" + ResultHashMap);
        return ResultHashMap;
    }

    /**public static HashMap<Integer, Double> ExceptSwitchCPU (ArrayList<HashMap<String,String>> MatchTimetoDeviceID){

     }**/
    // CPU load가 적은 순서대로 정렬하기위해 필요한 함수
    public static List sortByValue(final Map map) {
        List<Double> list = new ArrayList();
        list.addAll(map.keySet());

        Collections.sort(list,new Comparator() {

            public int compare(Object o1,Object o2) {
                Object v1 = map.get(o1);
                Object v2 = map.get(o2);

                return ((Comparable) v2).compareTo(v1);
            }

        });
        Collections.reverse(list); // 주석시 내림차순
        return list;
    }

    public static List BigfirstsortByValue(final Map map) {
        List<Double> list = new ArrayList();
        list.addAll(map.keySet());

        Collections.sort(list,new Comparator() {

            public int compare(Object o1,Object o2) {
                Object v1 = map.get(o1);
                Object v2 = map.get(o2);

                return ((Comparable) v2).compareTo(v1);
            }

        });
        //Collections.reverse(list); // 주석시 내림차순
        return list;
    }

    //해당 컨트롤러 안의 모든 device의 migration flag를 살핌, 한 개의 migration flag라도 있으면 이 flag는 true
    public static Boolean AllFlag(int time, int size, LinkedList<ControllerWithDevice> Controller){

        Boolean TF = null;
        //System.out.println("해당컨트롤러 모든 device의 time-1의 migflagtion flag를 체크!!");
        //System.out.println(Controller.get(time));
        for(int x = 0; x < size; x++){
            if(Controller.get(time).getMigFlag(x) == true){
                TF = true;
                break;
            }else{
                TF = false;
            }
        }
        return TF;
    }

    public static int CountMigFlag(LinkedList<ControllerWithDevice> Controller,int size, int time){
        int flagcnt = 0;
        //int size =  Controller.get(time).SIZE();

        for(int i = 0; i < size; i++){
            //여기서 존재하는 time 은 time - 1
            // flag setting은 time-1의 마지막부분에 일어남. 따라서, time-1의 flag가 몇개인지 따져야함.
            if(Controller.get(time).getMigFlag(i) == true)
                flagcnt++;
        }

        return flagcnt;
    }

    public static Double newCalMigSum(LinkedList<ControllerWithDevice> Controller, int time, int flagcnt, Double diff, ArrayList<HashMap<String, String>> MatchTimetoDeviceID){
        Double newsum = 0.0;

        Set set = MatchTimetoDeviceID.get(time).keySet();  // matchtimetoDeviceId는 update됐으니까 time으로
        Iterator iter_ = set.iterator();                   // Controller List는 지금 만드는 거니까 time-1을 참조해서 만듬

        while(iter_.hasNext()){
            String DeviceID = (String)iter_.next();
            Double tmpflagsum = ((Controller.get(time-1).BringCPU(DeviceID))*0.5) + ((diff/flagcnt) * 0.5);
            if((Controller.get(time-1).BringMigFlag(DeviceID)).equals(true) == true){
                newsum = newsum + tmpflagsum;
            }
        }

        return newsum;
    }

    public static void MakeSwitchList_time0(String onosname, LinkedList<ControllerWithDevice> Controller, ArrayList<HashMap<String, String>> MatchTimetoDeviceID, HashMap<String, Double> EachSwitch){
        ControllerWithDevice ONOS_Controller_obj = new ControllerWithDevice();

        Set set = MatchTimetoDeviceID.get(0).keySet();
        Iterator iter_ = set.iterator();

        while(iter_.hasNext()){
            String DeviceID = (String)iter_.next();
            String ConIP = (String)MatchTimetoDeviceID.get(0).get(DeviceID); // DeviceId 넣으면 해당 devicerk 속한 controller ip가 저장됨
            Double SwitchCPU = EachSwitch.get(onosname);
            Boolean MigrationFlag = false;
            Boolean SwitchOnce = false;

            ONOS_Controller_obj.Add(DeviceID,ConIP,SwitchCPU,MigrationFlag,SwitchOnce);
        }

        Controller.add(ONOS_Controller_obj);

    }

    public static void MakeSwitchList_timeelse(String onosname, LinkedList<ControllerWithDevice> Controller,int checktime, int size, ArrayList<HashMap<String, String>> MatchTimetoDeviceID){
        System.out.print("┌--------------- "+ onosname + "--------------------┐\n");
        ControllerWithDevice ONOS_Controller_obj = new ControllerWithDevice();
        Boolean CheckConOnceBefore = Controller.get(checktime-1).ControllerOnce;

        //int devicecnt = 0;
        int migFlagcnt = CountMigFlag(Controller,size,checktime-1);
        System.out.print("MakeSwitchList에 있는 MigFlagcnt-> " + migFlagcnt + "\n");

        Double abs_diff = Math.abs(((Controller_IdleCPU.get(checktime).get(onosname)) - (Controller_IdleCPU.get(checktime - 1).get(onosname))));
        Double diff = abs_diff;
        //Double diff = ((Controller_IdleCPU.get(checktime).get(onosname)) - (Controller_IdleCPU.get(checktime - 1).get(onosname)));
        Double migFlagsum = newCalMigSum(Controller,checktime,migFlagcnt,diff,MatchTimetoDeviceID);
        System.out.print("MakeSwitchList에 있는 MigFlagsum-> " + migFlagsum + "\n");


        Set set = MatchTimetoDeviceID.get(checktime).keySet();
        Iterator iter_ = set.iterator();
        while(iter_.hasNext()){
            String DeviceID = (String)iter_.next();
            String ConIP = (String)MatchTimetoDeviceID.get(checktime).get(DeviceID);
            Double SwitchCPU = 0.0;

            //time - 1의 마지막부분에서 setting된 flag를 검사하는 부분
            //검사해서 CPU까지 setting
            Boolean CheckAllFlag = AllFlag(checktime-1,size,Controller); //해당 컨트롤러 안의 모든 device의 migration flag를 살핌, 한 개의 migration flag라도 있으면 이 flag는 true
            //Boolean CheckMigBefore = Controller.get(checktime-1).getMigFlag(devicecnt);
            //Boolean CheckSWOnceBefore = Controller.get(checktime-1).getSWonceFlag(devicecnt);
            Boolean CheckMigBefore = Controller.get(checktime-1).BringMigFlag(DeviceID);
            Boolean CheckSWOnceBefore = Controller.get(checktime-1).BringSOnceFlag(DeviceID);

            // CASE1: 이제껏 migration을 경험한 device도 없고, time에 migration을 한 device도 없음.
            if(CheckAllFlag == false && CheckConOnceBefore == false){
                SwitchCPU = (Controller_IdleCPU.get(checktime).get(onosname))/size;
                System.out.print("===<case1>  :" + DeviceID + "\n");
            }
            // CASE2: 이제껏 migration을 경험한 device가 있지만, time엔 migration을 한 device가 없음.
            else if(CheckAllFlag == false && CheckConOnceBefore == true){
                SwitchCPU = Controller.get(checktime-1).BringCPU(DeviceID) + (diff/size);
                System.out.print("===<case2>  :" + DeviceID + "\n");
            }
            // CASE3: time에 migration을 한 device가 있고, 이 device가 migration을 한 device
            else if(CheckAllFlag == true && CheckMigBefore == true){
                System.out.print("===<case3>  :" + DeviceID + "==================\n");
                SwitchCPU = ((Controller.get(checktime-1).BringCPU(DeviceID))*0.5) + ((diff/migFlagcnt) * 0.5);

                System.out.println("(time-1)에 해당 device의 CPU: " + (Controller.get(checktime-1).BringCPU(DeviceID)) );
                System.out.println( (Controller.get(checktime-1).BringCPU(DeviceID)) * 0.5  + "<--(time-1)에 해당 device의 CPU x 0.5" );
                System.out.println("time에 " + onosname + "의 전체 CPU: " + (Controller_IdleCPU.get(checktime).get(onosname)) );
                System.out.println("(time-1)에 " + onosname + "의 전체 CPU: " + (Controller_IdleCPU.get(checktime-1).get(onosname)) );
                System.out.println("(time -1)에 migration flag가 true인 devide의 개수: " + migFlagcnt);
                System.out.println("+ " + ((diff/migFlagcnt) * 0.5) + "<--변화한 controller의 변화량 / migration flag가 1인 device 개수 x 0.5" );
                System.out.println( SwitchCPU + "<--둘의 합\n");
                System.out.println("=============================================\n");

            }
            // CASE4: time에 migration을 한 device가 있고, 이 device가 migration을 한 device는 아님
            else if(CheckAllFlag == true && CheckMigBefore == false){
                System.out.print("===<case4>  :" + DeviceID + "==================\n");
                Double newsum = Controller_IdleCPU.get(checktime).get(onosname) - migFlagsum;
                SwitchCPU = (newsum / (size - migFlagcnt));

                System.out.println("time에 " + onosname + "의 전체 CPU: " + (Controller_IdleCPU.get(checktime).get(onosname)));
                System.out.println(" - migration을 한 deivce들의 cpu 합: " + migFlagsum);
                System.out.println(" = " + newsum );
                System.out.println("이 컨트롤러에 있는 device의 개수: " + size );
                System.out.println("- migration을 한 device의 개수: " +  migFlagcnt);
                System.out.println(" = " + (size - migFlagcnt) );
                System.out.println( SwitchCPU + "<- newsum / (size-migFlgcnt)");
                System.out.println("=============================================\n");

            }
            //Boolean MigrationFlag = true;
            ONOS_Controller_obj.Add(DeviceID,ConIP,SwitchCPU,CheckMigBefore,CheckSWOnceBefore);
            //devicecnt++;
        } //while 끝

        Controller.add(ONOS_Controller_obj);
        Controller.get(checktime).ControllerOnce = Controller.get(checktime-1).ControllerOnce;
        System.out.print("└--------------- "+ onosname + "--------------------┘ \n");

    }

    public static LinkedHashMap<String,Double> Grouping(LinkedHashMap<String,Double> ControllerList, HashMap<Integer,Double> AVGCPU, int time, Boolean under){
        HashMap<String, Double> UnderLoad  = new HashMap<>();
        HashMap<String, Double> OverLoad  = new HashMap<>();

        Set setkey = ControllerList.keySet();
        Iterator iter = setkey.iterator();
        while(iter.hasNext()){
            String key = (String)iter.next();
            Double ControllerCPU = (Double)ControllerList.get(key);
            System.out.print("ControllerCPU " + ControllerCPU + "\n");
            if(ControllerCPU > AVGCPU.get(time)){
                OverLoad.put(key,ControllerCPU);
            }else if(ControllerCPU <= AVGCPU.get(time)){
                UnderLoad.put(key,ControllerCPU);
            }
        }//Controller_IdleCPU while문 끝

        // 평균을 넘는 Controller를 내림차순으로 정렬 (높은 걸 제일 먼저)
        if(under == false){
            LinkedHashMap<String, Double> LinkedOverLoad = new LinkedHashMap<>();
            Iterator overit = BigfirstsortByValue(OverLoad).iterator();
            while(overit.hasNext()){
                String ConName = (String)overit.next();
                LinkedOverLoad.put(ConName,OverLoad.get(ConName));
            }
            return LinkedOverLoad;
        }

        // 평균을 넘지 않는 controller를 오름차순으로 정렬 (낮은 걸 제일 먼저)
        else{
            LinkedHashMap<String, Double> LinkedUnderLoad = new LinkedHashMap<>();
            Iterator underit = sortByValue(UnderLoad).iterator();
            while(underit.hasNext()){
                String ConName = (String)underit.next();
                LinkedUnderLoad.put(ConName,UnderLoad.get(ConName));
            }
            return LinkedUnderLoad;
        }

    }


    //Max controller에서 Min controller로 보낼 적절한 스위치를 찾는 함수
    public static Switch FindMovingSwitch(LinkedHashMap<String,Double> controller_list, ControllerWithDevice MaxBeanSend, String MinConName, String MaxConName,Double AvgCPULoad){
        int MaxSize = MaxBeanSend.NumofCon();
        Double MaxConCPU = controller_list.get(MaxConName);
        Double MinConCPU = controller_list.get(MinConName);
        //임의로 아무값이나 넣어둠
        Switch MovingSwitch = new Switch("0","0",0.0,false,false);
        String MovingSwitchID = null;

        for(int i = 0; i < MaxSize ; i++){
            Double tmpDeviceCPU = MaxBeanSend.getCPU(i);
            //System.out.print(MaxConCPU +" - "+ tmpDeviceCPU +" >= "+ AvgCPULoad + "   " + MinConCPU +" + "+ tmpDeviceCPU +" <=  "+ AvgCPULoad +"\n");
            //System.out.print(MaxConCPU - tmpDeviceCPU +" >= "+ AvgCPULoad + "   " + (MinConCPU + tmpDeviceCPU) +" <=  "+ AvgCPULoad +"\n");
            if((MaxConCPU - tmpDeviceCPU >= AvgCPULoad && MinConCPU + tmpDeviceCPU <= AvgCPULoad) == true){
                MovingSwitchID = MaxBeanSend.getID(i);
                // 위에서 찾은 deviceID를 가지고 해당 Device를 찾음
                MovingSwitch = MaxBeanSend.FIND_DEVICE(MovingSwitchID);
                System.out.print("yes\n");
                System.out.print(MaxConCPU +" - "+ tmpDeviceCPU +" >= "+ AvgCPULoad +"   " + MinConCPU +" + "+ tmpDeviceCPU +" <=  "+ AvgCPULoad +"\n");
                System.out.print(MaxConCPU - tmpDeviceCPU +" >= "+ AvgCPULoad + "   " + (MinConCPU + tmpDeviceCPU) +" <=  "+ AvgCPULoad +"\n");
                break;
            }else{
                //System.out.print("no\n");
                MovingSwitch.Device_ID = "x";
            }
        }

        if(MovingSwitch.Device_ID.equals("-1") == true){
            System.out.print("\"ERROR: There is proper device but Cannot Find Removing Device in this controller\n");
            return null;
        }else if(MovingSwitch.Device_ID.equals("x") == true){
            System.out.print("There is no proper device in this controller(옮길만한 스위치가 없음)\n");
            return null;
        }else{
            return MovingSwitch;
        }

    }
    public static void RealMigration (ControllerWithDevice MinBeanGet, String MovingSwitchID){
        //System.out.print("MovingswitchId 출력" + MovingSwitchID + "\n");
        //System.out.print("MovingswitchId IP출력" + MinBeanGet.getIP(MovingSwitchID) + "\n");
        String mastership_url = "http://"
                + "128.110.153.202"
                + ":"
                + "60001"
                + "/onos/v1/mastership";

        //스위치가 옮겨갈 컨트롤러의 ip주소를 제대로 가져오지 못했을 때,
        if(MinBeanGet.getIP(MovingSwitchID).equals("-1") == true){
            System.out.print("ERROR: cannot find the exact IP address matched with removingID\n");
        }
        else{
            JsonObject rootObj = new JsonObject();
            rootObj.add("deviceId",MovingSwitchID);
            rootObj.add("nodeId",MinBeanGet.ControllerIP); //옮겨갈 곳의 IP
            rootObj.add("role", "MASTER");

            mastership restConn = new mastership();
            System.out.print("Bug before: print IP " + MinBeanGet.ControllerIP + "\n");
            System.out.print("Bug before: printf URL" + mastership_url + "\n");
            restConn.putRESTwithJson(mastership_url, rootObj);
            System.out.print("Bug after: printf URL" + mastership_url + "\n");
        }
    }
    public static void Initializing_migFlag(ControllerWithDevice Controller_atTime){
        int ControllerSize = Controller_atTime.NumofCon();

        for(int i =0; i<ControllerSize; i++){
            Controller_atTime.setFlag(i, false);
        }

    }

    public static void SetSOnceFlag(LinkedList<ControllerWithDevice> Controller, int checktime){
        int ControllerSize = Controller.get(checktime).NumofCon();
        int tmptime = checktime;

        for(int i = 0; i < ControllerSize; i++){
            Switch Device = Controller.get(checktime).WholeDevice(i);
            Boolean MigrationFlag = Device.Migration_check;

            if(MigrationFlag == true){
                Device.SwitchOnce = true;
            }

            else{
                for(int findbytime = tmptime; findbytime >=0; findbytime--){
                    System.out.print("SetSOnceFlag에 있는 findbytime " + findbytime + "\n");
                    if((Controller.get(findbytime).BringSOnceFlag(Device.Device_ID)).equals(true)){
                        Device.SwitchOnce = true;
                        break;
                    }
                    else{
                        Device.SwitchOnce = false;
                    }
                } //for문 끝
            }
        }
    }

    public static void main(String[] args) throws IOException, JSchException {


        final ArrayList<HashMap<String,String>> MatchTimetoDeviceID_ONOS1 = new ArrayList<HashMap<String, String>>();
        final ArrayList<HashMap<String,String>> MatchTimetoDeviceID_ONOS2 = new ArrayList<HashMap<String, String>>();
        final ArrayList<HashMap<String,String>> MatchTimetoDeviceID_ONOS3 = new ArrayList<HashMap<String, String>>();
        final HashMap<Integer,Double> AvgCPULoad= new HashMap<Integer, Double>();

        int sleepSec = 30;
        final ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);

        final HashMap<Double, Double> BringIdleCPU = new HashMap<>();

        exec.scheduleAtFixedRate(new Runnable() {
            int checktime = 0;
            int KernelAndUser = 0;

            public void run() {
                try {
                    //각 ONOS controller의 ip를 넣어 온전한 주소를 만들고
                    //각 controller가 서비스를 하는 deviceID를 hashmap으로 저장한다.
                    HashMap<String, String> MasterServices_ONOS1 = monitor.ControllerSeviceSwitches(ONOS1_IP);
                    HashMap<String, String> MasterServices_ONOS2 = monitor.ControllerSeviceSwitches(ONOS2_IP);
                    HashMap<String, String> MasterServices_ONOS3 = monitor.ControllerSeviceSwitches(ONOS3_IP);

                    // 위에서 만든 hashmap은 MatchTimetoDeviceID라는 arraylist에 저장되고
                    // 이는 1분마다 어떤 controller가 어떤 device에 서비스를 제공했는지를 모두 저장한다.
                    MatchTimetoDeviceID_ONOS1.add(MasterServices_ONOS1);
                    MatchTimetoDeviceID_ONOS2.add(MasterServices_ONOS2);
                    MatchTimetoDeviceID_ONOS3.add(MasterServices_ONOS3);



                    // Device 목록을 출력하는 part
                    // 여기서 MatchTimetoDeviceID_ONOS1.size()는 매분 1이고
                    // MatchTimetoDeviceID_ONOS1.get(i).size()는 1분에 서비스를 제공하는 device의 수를 의미
                    /************************************************
                    System.out.println("ONOS1가 service를 제공하는 Device ID를 출력합니다.");
                    for (int i = 0; i < MatchTimetoDeviceID_ONOS1.size(); i++) {
                        System.out.println(MatchTimetoDeviceID_ONOS1.get(i));
                        System.out.println("SIZE   " + MatchTimetoDeviceID_ONOS1.get(i).size() + "\n");
                    }
                    /************************************************/

                    /************************************************
                    System.out.println("ONOS2가 service를 제공하는 Device ID를 출력합니다.");
                    for (int i = 0; i < MatchTimetoDeviceID_ONOS2.size(); i++) {
                        System.out.println(MatchTimetoDeviceID_ONOS2.get(i));
                        System.out.println("SIZE   " + MatchTimetoDeviceID_ONOS2.get(i).size() + "\n");
                    }
                    /************************************************/

                    /************************************************
                    System.out.println("ONOS3가 service를 제공하는 Device ID를 출력합니다.");
                    for (int i = 0; i < MatchTimetoDeviceID_ONOS3.size(); i++) {
                        System.out.println(MatchTimetoDeviceID_ONOS3.get(i));
                        System.out.println("SIZE   " + MatchTimetoDeviceID_ONOS3.get(i).size() + "\n");
                    }
                    /************************************************/

                    //현재time에 controller가 서비스를 제공하는 device의 개수
                    int SizeofONOS1 = MatchTimetoDeviceID_ONOS1.get(checktime).size();
                    int SizeofONOS2 = MatchTimetoDeviceID_ONOS2.get(checktime).size();
                    int SizeofONOS3 = MatchTimetoDeviceID_ONOS3.get(checktime).size();

                    // 20180105 CPU Manuplate - real
                    String ConnResult = monitor.ConnSSH();
                    HashMap<String, Double> CovertStingDouble = monitor.resultHashMap(ConnResult);

                    /* 20180105 CPU Manuplate - fake
                    HashMap<String, Double> CovertStingDouble = new HashMap(){
                        {
                            put("onos-1",30.0);
                            put("onos-2",130.0);
                            put("onos-3",30.0);
                        }
                    };*/

                    //CPU load순으로 sort해서 it에 저장
                    LinkedHashMap<String, Double> tmp_hash = new LinkedHashMap<>();
                    Iterator it = sortByValue(CovertStingDouble).iterator();

                    //sort된 순으로 key값을 호출하고 그 value를 tmp_hash에 넣기
                    while (it.hasNext()) {
                        String temp = (String) it.next();
                        System.out.println(temp + "=" + CovertStingDouble.get(temp));
                        tmp_hash.put(temp, CovertStingDouble.get(temp));
                    }

                    //정렬된 hashmap을 시간순으로 arraylist에 삽입
                    MatchTimetoCPU2.add(tmp_hash);
                    System.out.println("\ntmp_hash" + tmp_hash);


                    //idle한 CPU LOAD를 빼지 않은 값을 오름차순으로 정렬한 ARRAYLIST
                    System.out.println("\nSorting된 CPU Load 목록(idle CPU 빼지 않은 값):" + MatchTimetoCPU2);

                    //Packet processing에 쓰이는 각 컨트롤러에서 쓰는 전체 CPU Load
                    LinkedHashMap<String, Double> Subtract_IdleCPU = monitor.SubtractIdle(tmp_hash);
                    Controller_IdleCPU.add(Subtract_IdleCPU);
                    System.out.println("\nMatchTimeCPU2에서 Idle한 값을 뺀 결과: 전체->" + Controller_IdleCPU);
                    System.out.println("\nMatchTimeCPU2에서 Idle한 값을 뺀 결과: 지금 -> " + Controller_IdleCPU.get(checktime));
                    System.out.println("\nMatchTimeCPU2에서 Idle한 값을 뺀 결과: checktime ->  " + checktime);

                    //시간당 CPU load의 평균을 구해서 시간별로 저장
                    AvgCPULoad.put(checktime, AvgCPU(Controller_IdleCPU.get(checktime)));

                    //idle한 값을 뺀 cpu load의 평균값
                    System.out.println("\n시간당 CPU Load의 평균값 (idle한 CPU를 뺀 값) -> " + AvgCPULoad);

                    //현재 time의 Idle CPU/ 현재 time의 device의 개수를 한 결과 =  EachSwitch
                    HashMap<String, Double> EachSwitch = monitor.DivideSize(Controller_IdleCPU,checktime, SizeofONOS1, SizeofONOS2, SizeofONOS3);
                    System.out.println("\n각 컨트롤러의 전체 CPU Load를 디바이스의 수로 나눈 결과 -> " + EachSwitch);


                    /*//////////////////REMOVETEST?////////////////////////////////////////
                    if(checktime == 1){
                        System.out.print("-----------CHECK -1 -----------------"+ "\n");
                        System.out.print("ONOS1 " + ONOS1_Controller.get(checktime -1) + "\n");
                        System.out.print("ONOS2 " + ONOS2_Controller.get(checktime- 1 ) + "\n");
                        System.out.print("ONOS3 " + ONOS3_Controller.get(checktime-1) + "\n");
                    }
                    ///////////////////////////////////////////////////////////////////////*/

                    //deveic가 있는 controller의 List를 만들기
                    //만들때, time-1의 flag들을 체크해서 CPU계산까지 해줌
                    if(checktime == 0){
                        MakeSwitchList_time0("onos-1", ONOS1_Controller, MatchTimetoDeviceID_ONOS1, EachSwitch);
                        MakeSwitchList_time0("onos-2", ONOS2_Controller, MatchTimetoDeviceID_ONOS2, EachSwitch);
                        MakeSwitchList_time0("onos-3", ONOS3_Controller, MatchTimetoDeviceID_ONOS3, EachSwitch);

                        System.out.println("\ntime == 0 일 때, ONOS1_Controller 출력\n" + ONOS1_Controller);
                        System.out.println("\ntime == 0 일 때, ONOS2_Controller 출력\n" + ONOS2_Controller);
                        System.out.println("\ntime == 0 일 때, ONOS3_Controller 출력\n" + ONOS3_Controller);
                    }else{
                        /*******TEST*********if(checktime == 1) {
                            ONOS3_Controller.get(checktime - 1).setFlag(1, true);
                            ONOS2_Controller.get(checktime - 1).ControllerOnce = true;
                        }
                        if(checktime == 2) {
                            ONOS3_Controller.get(checktime - 1).setFlag(1, false);
                            ONOS3_Controller.get(checktime - 1).ControllerOnce = true;
                            ONOS2_Controller.get(checktime - 1).ControllerOnce = false;
                        }********************/

                        MakeSwitchList_timeelse("onos-1",ONOS1_Controller,checktime,SizeofONOS1,MatchTimetoDeviceID_ONOS1);
                        MakeSwitchList_timeelse("onos-2",ONOS2_Controller,checktime,SizeofONOS2,MatchTimetoDeviceID_ONOS2);
                        MakeSwitchList_timeelse("onos-3",ONOS3_Controller,checktime,SizeofONOS3,MatchTimetoDeviceID_ONOS3);

                        System.out.println("\ntime > 0 일 때, ONOS1_Controller 출력\n" + ONOS1_Controller.get(checktime));
                        System.out.println("\ntime > 0 일 때, ONOS2_Controller 출력\n" + ONOS2_Controller.get(checktime));
                        System.out.println("\ntime > 0 일 때, ONOS3_Controller 출력\n" + ONOS3_Controller.get(checktime));
                    }

                    //migration 하기전에 해당 time의 각 device의 migration flag를 모두 false로 초기화
                    Initializing_migFlag(ONOS1_Controller.get(checktime));
                    Initializing_migFlag(ONOS2_Controller.get(checktime));
                    Initializing_migFlag(ONOS3_Controller.get(checktime));


                    //여기부터 migration하는 부분
                    LinkedHashMap<String,Double> controller_list = new LinkedHashMap<>();
                    controller_list.put("onos-1",Controller_IdleCPU.get(checktime).get("onos-1"));
                    controller_list.put("onos-2",Controller_IdleCPU.get(checktime).get("onos-2"));
                    controller_list.put("onos-3",Controller_IdleCPU.get(checktime).get("onos-3"));

                    ControllerWithDevice MaxBeanSend = new ControllerWithDevice();
                    ControllerWithDevice MinBeanGet = new ControllerWithDevice();
                    Switch MovingSwitch = new Switch("0","0",0.0,false,false); //임의로 아무값이나 넣어둠
                    Boolean ThereisDevice = true; //다 돌았는데 migration을 할 device를 찾았는지 여부를 알려주는 변수

                   while(true) {
                       //평균을 넘는 Controller와 넘지 않는 controller 구분하기
                       LinkedHashMap<String,Double> SortedOverCon = Grouping(controller_list,AvgCPULoad, checktime, false); // 평균보다 높은 controller를 골라내기
                       LinkedHashMap<String,Double> SortedUnderCon = Grouping(controller_list, AvgCPULoad, checktime, true); // 평균보다 낮은 controller를 골라내기


                       if (SortedOverCon == null || SortedUnderCon == null || ThereisDevice == false){
                           break;
                       }
                       else {
                           String[] FindMin = SortedUnderCon.keySet().toArray(new String[0]);

                           System.out.println("\nSortedOverCon:" + SortedOverCon);
                           System.out.println("SortedUnderCon:" + SortedUnderCon);

                           String MinConName = FindMin[0];
                           //스위치가 옮겨갈 controller를 MinBeanGet에 저장
                           if (MinConName.equals("onos-1") == true) {
                               MinBeanGet = ONOS1_Controller.get(checktime);
                               MinBeanGet.ControllerIP = ONOS1_IP;
                           } else if (MinConName.equals("onos-2") == true) {
                               MinBeanGet = ONOS2_Controller.get(checktime);
                               MinBeanGet.ControllerIP = ONOS2_IP;
                           } else if (MinConName.equals("onos-3") == true) {
                               MinBeanGet = ONOS3_Controller.get(checktime);
                               MinBeanGet.ControllerIP = ONOS3_IP;
                           }

                           //두번째 while문
                           int nullcnt = 0; // OverCon그룹을 얼마나 돌았는지를 cnt
                           Set OverConKey = SortedOverCon.keySet(); // OverCon을 검색하기 위한 key설정
                           Iterator OverConIt = OverConKey.iterator();
                           while (OverConIt.hasNext()) {
                               String MaxConName = (String) OverConIt.next();
                               System.out.print("\nMaxConName: " + MaxConName + "\n");
                               System.out.print(" ↓  ↓  ↓\n");
                               System.out.print("MinConName: " + MinConName + "\n");

                               //스위치가 옮겨갈 controller를 MaxBeanSend에 저장
                               if (MaxConName.equals("onos-1") == true) {
                                   MaxBeanSend = ONOS1_Controller.get(checktime);
                                   MaxBeanSend.ControllerIP = ONOS1_IP;
                               } else if (MaxConName.equals("onos-2") == true) {
                                   MaxBeanSend = ONOS2_Controller.get(checktime);
                                   MaxBeanSend.ControllerIP = ONOS2_IP;
                               } else if (MaxConName.equals("onos-3") == true) {
                                   MaxBeanSend = ONOS3_Controller.get(checktime);
                                   MaxBeanSend.ControllerIP = ONOS3_IP;
                               }
                               //TEST
                               //if(checktime == 0)
                               //MaxBeanSend.setCPU(0, 0.14);

                               //Controller안에서 migration시킬 스위치 찾기
                               MovingSwitch = FindMovingSwitch(controller_list, MaxBeanSend, MinConName, MaxConName, AvgCPULoad.get(checktime));
                               //System.out.print("Moving Switch:  " +  MovingSwitch + "\n");

                               // 2) migration할 적절한 switch를 찾았을 때,
                               if (MovingSwitch != null) {
                                   // 적절한 switch를 찾았는데 옮겨가려는 controller에 이미 해당 switch가 있을 때,
                                   // MinBeanGet으로 switch를 옮기는 거임
                                   if (MinBeanGet.EQUAL_DEVICE(MovingSwitch) == true) {
                                       System.out.print("ERROR: The device that you plan to move is already in this Controller ( 스위치를 찾았으나 이미 Min에 이 device가 있다)\n");
                                   }
                                   else {
                                       // 옮겨진 후의 controller 에서 migration이 device가 없는 경우는 실제로 migration 진행.
                                       System.out.print("ready to move!!!\n");
                                       System.out.print(MaxConName + " 에서" + MinConName + " 으로 " + MovingSwitch.Device_ID + " 가 이동" + "\n");
                                       //System.out.print("Before\n" + MinBeanGet + "\n");
                                       //System.out.print("Before\n" + MaxBeanSend + "\n");

                                       //Migration Flag Setting: Migration이 된 device에 true를 달아줌
                                       MovingSwitch.Migration_check = true;
                                       System.out.print("Moving Switch: " + MovingSwitch + "\n");
                                       MinBeanGet.AddWholeDevice(MovingSwitch);
                                       MinBeanGet.SETIP(MovingSwitch.Device_ID, MinBeanGet.ControllerIP); //Device의 Ip 주소를 옮겨갈 controller의 IP로 주소변경
                                       MaxBeanSend.REMOVE(MovingSwitch.Device_ID);
                                       RealMigration(MinBeanGet, MovingSwitch.Device_ID);  //실제로 migration이 일어나는 곳

                                       //Resorting을 위한 값을 갱신
                                       controller_list.put(MaxConName,controller_list.get(MaxConName) - MovingSwitch.CPU_Load);
                                       controller_list.put(MinConName,controller_list.get(MinConName) + MovingSwitch.CPU_Load);

                                       //System.out.println("\nController_IdleCPU" + Controller_IdleCPU.get(checktime));
                                       //System.out.println("\nController_List" + controller_list);

                                       System.out.print("두번째while문끝==================================================\n\n\n");
                                       break;
                                   }
                               }
                               // 2) migration할 적절한 switch를 찾지 못 했을 때,
                               else {
                                   System.out.print("There is no proper device in this controller. Go to next max controller\n");
                                   nullcnt++;
                               }

                           } //OverCon while문 끝
                           if(nullcnt == SortedOverCon.size()) ThereisDevice = false;
                       }
                   } // 바깥 while문 끝

                    //Switch Once Flag Setting
                    SetSOnceFlag(ONOS1_Controller,checktime);
                    SetSOnceFlag(ONOS2_Controller,checktime);
                    SetSOnceFlag(ONOS3_Controller,checktime);

                    //Controller Once Flag Setting
                    ONOS1_Controller.get(checktime).SetConOnceFlag();
                    ONOS2_Controller.get(checktime).SetConOnceFlag();
                    ONOS3_Controller.get(checktime).SetConOnceFlag();

                    System.out.print("-----------AFTERCHECKING-----------------"+ "\n");
                    System.out.print("ONOS1 " + ONOS1_Controller.get(checktime) + "\n");
                    System.out.print("ONOS2 " + ONOS2_Controller.get(checktime) + "\n");
                    System.out.print("ONOS3 " + ONOS3_Controller.get(checktime) + "\n");

                    /*/////////////////////migartion하기 전에 중복체크하는 부분 ///////////////
                    System.out.print("-----------BEFORE-----------------"+ "\n");
                    System.out.print("ONOS1 " + ONOS1_Controller.get(checktime) + "\n");
                    System.out.print("ONOS2 " + ONOS2_Controller.get(checktime) + "\n");
                    System.out.print("ONOS3 " + ONOS3_Controller.get(checktime) + "\n");

                    //옮겨지기전 controller에 migration이 될 device가 실제로 있는지 체크
                    String removingID = "of:0000000000000301";
                    Switch Device = ONOS1_Controller.get(checktime).FIND_DEVICE(removingID);


                    if(Device.Device_ID.equals("-1") == true){ // 옮겨지기 전 controller에 migration이 될 device가 없는 경우
                        System.out.print("ERROR: Cannot Find Removing Device in this controller\n");
                    }else{ // 옮겨지기 전 controller에 migration이 될 device가 있는 경우
                        if(ONOS2_Controller.get(checktime).EQUAL_DEVICE(Device) == true){
                            // 옮겨진 후의 controller 에서 migration이 device가 이미 있는 경우
                            System.out.print("ERROR: The device that you plan to move is already in this Controller\n");
                        }else{
                            // 옮겨진 후의 controller 에서 migration이 device가 없는 경우는 실제로 migration 진행.
                            System.out.print("ready to move!!!\n");
                            ONOS2_Controller.get(checktime).AddWholeDevice(Device);
                            ONOS2_Controller.get(checktime).SETIP(removingID, ONOS2_IP); //옮겨갈 controller의 IP로 주소변경
                            ONOS1_Controller.get(checktime).REMOVE(Device.Device_ID);
                        }
                    }
                    System.out.print("-----------AFTERCHECKING-----------------"+ "\n");
                    System.out.print("ONOS1 " + ONOS1_Controller.get(checktime) + "\n");
                    System.out.print("ONOS2 " + ONOS2_Controller.get(checktime) + "\n");
                    System.out.print("ONOS3 " + ONOS3_Controller.get(checktime) + "\n");
                    /////////////////////migartion하기 전에 중복체크하는 부분 ///////////////*/


                    /*/////////////////// 실제로 migration하는 부분 //////////////////////////////
                    if(checktime == 0) {
                        String MigrationTo = new String();
                        //ONOS2는 옮겨갈 곳을 임의로 지정해둔거
                        if(ONOS2_Controller.get(checktime).getIP(removingID).equals("-1") == true)
                            System.out.print("ERROR: cannot find the exact IP address matched with removingID\n");
                        else {
                            MigrationTo = ONOS2_Controller.get(checktime).getIP(removingID);
                            System.out.print("MigrationTO: " + MigrationTo +"\n");
                        }

                        JsonObject rootObj = new JsonObject();
                        rootObj.add("deviceId",removingID);
                        rootObj.add("nodeId",MigrationTo); //옮겨갈IP
                        rootObj.add("role", "MASTER");

                        mastership restConn = new mastership();
                        restConn.putRESTwithJson(mastershipinmonitor.RESTURL_DOMASTERSHIP, rootObj);
                        System.out.print("-----------AFTERMIGRATION-----------------"+ "\n");
                        System.out.print("ONOS1 " + ONOS1_Controller.get(checktime) + "\n");
                        System.out.print("ONOS2 " + ONOS2_Controller.get(checktime) + "\n");
                        System.out.print("ONOS3 " + ONOS3_Controller.get(checktime) + "\n");
                    }
                    ////////////////// 실제로 migration하는 부분 //////////////////////////////*/


                    System.out.println("\nTime "+checktime+"  ============================================================");
                    checktime++;
                } catch (Exception e) {
                    e.printStackTrace();
                    exec.shutdown();
                }

            }
        },0,sleepSec, TimeUnit.SECONDS);
    }
}





