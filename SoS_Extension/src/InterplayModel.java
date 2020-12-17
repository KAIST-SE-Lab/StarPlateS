import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

class Message {
    Float time;
    String commandSent;
    String senderPltId;
    String receiverId;
    String receiverPltId;
    String senderRole;
    String receiverRole;
    Float distance;
}

public class InterplayModel {

    ArrayList<Message> msgSequence;
    ArrayList<String> vehRole;
    String id;

    public InterplayModel(int s_index, int r_index) {

        msgSequence = new ArrayList<>();
        vehRole = new ArrayList<>();
        id = s_index + "_" + r_index;
        
        try {
            File pltConfig = new File("./SoS_Extension/logs/" + s_index + "_" + r_index + "plnData.txt");
            FileReader filereader = new FileReader(pltConfig);
            BufferedReader bufReader = new BufferedReader(filereader);
            String line="";
            StringTokenizer stringTokenizer;

            Float t_time;
            String temp;
            String t_command;
            String vehID;

            while((line = bufReader.readLine()) != null) {
                stringTokenizer = new StringTokenizer(line," ");

                if(stringTokenizer.countTokens() != 9) continue;

                temp = stringTokenizer.nextToken();
                if(temp.equals("timeStep")) {
                    continue;
                } else {
                    t_time = Float.valueOf(temp);
                }

                vehID = stringTokenizer.nextToken();
                for(int i = 0; i < 2; i++) stringTokenizer.nextToken();

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

                temp = stringTokenizer.nextToken();
                if(temp.contains("Leave_Start") || temp.contains("Leave_Request")) {
                    vehRole.add(vehID);
                }

                if(vehRole.contains(msg.senderPltId)) {
                    if (vehRole.contains(msg.receiverId)) {
                        msg.senderRole = "None";
                        msg.receiverRole = "None";
                    } else {
                        msg.senderRole = "None";
                        msg.receiverRole = "Leader";
                    }
                } else if (vehRole.contains(msg.receiverId)) {
                    msg.senderRole = "Leader";
                    msg.receiverRole = "None";
                } else {
                    msg.senderRole = "Leader";
                    msg.receiverRole = "Leader";
                }

                msgSequence.add(msg);
            }
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public String printSequence() {
        String ret = "";
        Message temp;
        for(int i = 0; i < msgSequence.size(); i++) {
            temp = msgSequence.get(i);
            ret += temp.time + ": " + temp.commandSent + " from " + temp.senderPltId + " to " + temp.receiverId + "\n";
            System.out.println(temp.time + ": " + temp.commandSent + " from " + temp.senderPltId + " to " + temp.receiverId);
        }
        return ret;
    }
//
//    public String getCSRole(String vehID, float time) {
//        if(vehRole.containsKey(vehID) && time >= vehRole.get(vehID)) return "None";
//        else return "Leader";
//    }

    public ArrayList<Message> getMsgSequence() {
        return msgSequence;
    }
    public String getId() {return id; }
}
