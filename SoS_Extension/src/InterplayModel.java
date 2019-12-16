import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

class Message {
    Float time;
    String commandSent;
    String senderPltId;
    String receiverId;
    String receiverPltId;
}


public class InterplayModel {

    ArrayList<Message> msgSequence;

    public InterplayModel(int s_index, int r_index) {

        msgSequence = new ArrayList<>();

        try {
            File pltConfig = new File("./SoS_Extension/logs/" + s_index + "_" + r_index + "plnData.txt");
            FileReader filereader = new FileReader(pltConfig);
            BufferedReader bufReader = new BufferedReader(filereader);
            String line="";
            StringTokenizer stringTokenizer;

            Float t_time;
            String temp;
            String t_command;

            while((line = bufReader.readLine()) != null) {
                stringTokenizer = new StringTokenizer(line," ");

                if(stringTokenizer.countTokens() != 9) continue;

                temp = stringTokenizer.nextToken();
                if(temp.equals("timeStep")) {
                    continue;
                } else {
                    t_time = Float.valueOf(temp);
                }

                for(int i = 0; i < 3; i++) stringTokenizer.nextToken();

                temp = stringTokenizer.nextToken();
                if(temp.equals("-")) {
                    continue;
                }
                else {
                    t_command = temp;
                }

                Message msg = new Message();
                msg.time = t_time;
                msg.commandSent = t_command;
                msg.receiverId = stringTokenizer.nextToken();
                msg.senderPltId = stringTokenizer.nextToken();
                msg.receiverPltId = stringTokenizer.nextToken(); //TODO Consider ManueverStartEnd ?? ex) Split_Start

                msgSequence.add(msg);
            }
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public void printSequence() {
        Message temp;
        for(int i = 0; i < msgSequence.size(); i++) {
            temp = msgSequence.get(i);
            System.out.println(temp.time + ": " + temp.commandSent + " from " + temp.senderPltId + " to " + temp.receiverId);
        }
    }
}
