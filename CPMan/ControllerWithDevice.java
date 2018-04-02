package CPMAN;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by monet on 2018-02-21.
 */
public class ControllerWithDevice {
    LinkedList<SWBean> SwitchList = new LinkedList<>();
    //List<SWBean> SwitchList = new CopyOnWriteArrayList<>();
    String ControllerIP;

    public ControllerWithDevice(){
    }

    public int addSWBean(SWBean tmpSWBean) {
        SwitchList.add(tmpSWBean);
        return 0;
    }

    public SWBean getSWBean(int i){
        SWBean resultSWBean = SwitchList.get(i);
        return resultSWBean;
    }
    public int SwitchSize(){
        int NumofSWBean = 0;

        Iterator<SWBean> it = SwitchList.iterator();

        while(it.hasNext()){
            SWBean tmpSWBean = it.next();
            if((tmpSWBean.getDpid()) != null)
                NumofSWBean++;
        }
        return NumofSWBean;
    }

    public String toString(){
        return "ControllerIP:" + ControllerIP + "\n-------------------SwitchList-------------------\n"+String.valueOf(SwitchList)+ "\n";
    }

}
