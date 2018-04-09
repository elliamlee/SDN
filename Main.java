import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        int[] hostnumber = new int[20];
        int mininetnum;
        String Fullmininet;
        String[] ServerString = new String[20];

        System.out.println("Tell me the mininetnumber: ");
        Scanner scanmininet = new Scanner(System.in);
        mininetnum = scanmininet.nextInt();
        Fullmininet = "h"+mininetnum+"_";
        System.out.println(Fullmininet);

        System.out.println("sleep count at the FRONT? ");
        Scanner scansleep = new Scanner(System.in);
        int sleepcnt = scansleep.nextInt();
        System.out.println("Front: sleep duration? ");
        Scanner scansleepduration = new Scanner(System.in);
        int sleepduration = scansleepduration.nextInt();

        System.out.println("sleep count at the BACK? ");
        Scanner scanbacksleep = new Scanner(System.in);
        int sleepbackcnt = scanbacksleep.nextInt();
        System.out.println("Back: sleep duration? ");
        Scanner scanbacksleepduration = new Scanner(System.in);
        int sleepbackduration = scanbacksleepduration.nextInt();

        System.out.println("How many servers are there? ");
        Scanner serverscan = new Scanner(System.in);
        int servercount = serverscan.nextInt();

        for (int i = 0; i < servercount; i++) {
            System.out.println("hostnumber " + (i+1) + " ?");
            Scanner scan = new Scanner(System.in);
            int scannum = scan.nextInt();
            hostnumber[i] = scannum;
            ServerString[i] = Fullmininet + hostnumber[i] + " nohup iperf3 -s &";
        }

        for( int sleepindex=0; sleepindex<sleepcnt; sleepindex++){
            if(sleepcnt != 0){
                System.out.println("sh sleep " + sleepduration);
            }
        }

        for(int index=0; index<servercount; index++){
            System.out.println(ServerString[index]);
        }

        for(int i = 0; i < servercount; i++){
            for(int j = 0; j < 8; j++){
                int tmpj = 2 + (4*j);
                if(  tmpj != hostnumber[i]){
                    System.out.println(Fullmininet+tmpj+" nohup iperf3 -c "+Fullmininet+hostnumber[i]+" -b 5M -t 2 &");
                }
                int tmpk = 3 + (4*j);
                if(  tmpk != hostnumber[i]){
                    System.out.println(Fullmininet+tmpk+" nohup iperf3 -c "+Fullmininet+hostnumber[i]+" -b 5M -t 2 &");
                }
            }

            for(int t = 0; t < 8; t ++){
                int tmpjReverse = 2 + (4*t);
                if(  tmpjReverse != hostnumber[i]){
                    System.out.println(Fullmininet+tmpjReverse+" nohup iperf3 -c "+Fullmininet+hostnumber[i]+" -b 5M -R -t 2 &");
                }
                int tmpkReverse = 3 + (4*t);
                if(  tmpkReverse != hostnumber[i]){
                    System.out.println(Fullmininet+tmpkReverse+" nohup iperf3 -c "+Fullmininet+hostnumber[i]+" -b 5M -R -t 2 &");
                }
            }

            for( int sleepindex=0; sleepindex<sleepbackcnt; sleepindex++){
                if(sleepbackcnt != 0){
                    System.out.println("sh sleep " + sleepbackduration);
                }
            }
        }

    }
}
