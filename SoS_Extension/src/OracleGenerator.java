import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class OracleGenerator {
    private ArrayList<ArrayList<String>> oracle;
    private ArrayList<String> IM_index;

    public OracleGenerator() {
        oracle = new ArrayList<>();     // Based on https://docs.google.com/spreadsheets/d/1QfmDA-O8_cf2X6Fyj1hTpAEp0HWKrBKZw-YEcS8B2WI/edit#gid=0
        oracle.add(new ArrayList<>());  // CASE 1: Simultaneous Merge & Merge
        oracle.add(new ArrayList<>());  // CASE 2: Simultaneous Split & Merge
        oracle.add(new ArrayList<>());  // CASE 3: Simultaneous LLeave & Merge
        oracle.add(new ArrayList<>());  // CASE 4: Simultaneous FLeave & Merge
        oracle.add(new ArrayList<>());  // CASE 5: Configuration conflict Split & Merge
        oracle.add(new ArrayList<>());  // CASE 6: Configuration conflict LLeave & Merge
        oracle.add(new ArrayList<>());  // CASE 7: Single Operation Failure LLeave
        oracle.add(new ArrayList<>());  // CASE 8: Configuration conflict FLeave & Merge (->Intermediate Leader)
        oracle.add(new ArrayList<>());  // CASE 9: Single Operation Failure FLeave
        oracle.add(new ArrayList<>());  // CASE 10: Configuration conflict FLeave & Merge (->Leaved vehicle)
        IM_index = new ArrayList<>();
    }

    public void oracleGeneration(ArrayList<InterplayModel> IMs) {
        ArrayList<Message> Msgs = null;
        ArrayList<Integer> time_to_check = new ArrayList<>();
        for (InterplayModel im : IMs) {
//            if(!(im.getId().equals("616_0"))) continue;
            IM_index.add(im.getId());
            Msgs = im.getMsgSequence();
//            System.out.println(im.getId());

            //======== Check continuous Merge_Requests ========
            time_to_check = timeToCheck(Msgs);

            for(int time : time_to_check) {
                int s_index;
                for(s_index = 0; s_index < Msgs.size(); s_index++) {
                    if(Msgs.get(s_index).time >= time) break;
                }

                // ======= Simultaneous Requests Cases (REQ 다음에 바로 REQ가 나오는 경우) =======
                if(Msgs.get(s_index+1).commandSent.contains("REQ") &&
                        (Msgs.get(s_index).receiverId.equals(Msgs.get(s_index+1).senderPltId)
                                || Msgs.get(s_index).senderPltId.equals(Msgs.get(s_index+1).receiverId))) {
                    // ****** CASE 1 ******
                    if(Msgs.get(s_index).commandSent.equals("MERGE_REQ")
                            && Msgs.get(s_index+1).commandSent.equals("MERGE_REQ")){
                        if(!oracle.get(0).contains(im.getId())) oracle.get(0).add(im.getId());
                    }
                    // ****** CASE 2 ******
                    else if((Msgs.get(s_index).commandSent.equals("SPLIT_REQ")
                            && Msgs.get(s_index+1).commandSent.equals("MERGE_REQ")) ||
                            (Msgs.get(s_index).commandSent.equals("MERGE_REQ")
                                    && Msgs.get(s_index+1).commandSent.equals("SPLIT_REQ"))){
                        if(!oracle.get(1).contains(im.getId())) oracle.get(1).add(im.getId());
                    }
                    // ****** CASE 3 ******
                    else if((Msgs.get(s_index).commandSent.equals("VOTE_LEADER")
                            && Msgs.get(s_index+1).commandSent.equals("MERGE_REQ")) ||
                            (Msgs.get(s_index).commandSent.equals("MERGE_REQ")
                                    && Msgs.get(s_index+1).commandSent.equals("VOTE_LEADER"))){
                        if(!oracle.get(2).contains(im.getId())) oracle.get(2).add(im.getId());
                    }
                    // ****** CASE 4 ******
                    else if((Msgs.get(s_index).commandSent.equals("LEAVE_REQ")
                            && Msgs.get(s_index+1).commandSent.equals("MERGE_REQ")) ||
                            (Msgs.get(s_index).commandSent.equals("MERGE_REQ")
                                    && Msgs.get(s_index+1).commandSent.equals("LEAVE_REQ"))){
                        if(!oracle.get(3).contains(im.getId())) oracle.get(3).add(im.getId());
                    }
                }
                else {
                    switch (Msgs.get(s_index).commandSent) {
                        // ======= SPLIT & MERGE (Case 5) =======
                        case "SPLIT_REQ":                           // TODO SPLIT_REQ 면 바로 가능한게 맞는지 확인
                            if(!oracle.get(4).contains(im.getId())) oracle.get(4).add(im.getId());
                            break;
                        // ======= LEADER LEAVE Cases =======
                        case "VOTE_LEADER":
                            String leaved = "";
                            String newLeader = "";
                            for(int i = s_index; i < Msgs.size(); i++) {
                                if(Msgs.get(i).time > time + 20) break;
                                if(i == s_index) leaved = Msgs.get(i).senderPltId;
                                if(Msgs.get(i).commandSent.equals("SPLIT_REQ")) {
                                    newLeader = Msgs.get(i).receiverId;
                                } else if(Msgs.get(i).commandSent.equals("MERGE_REQ")) {
                                    // ****** CASE 7 ******
                                    if(Msgs.get(i).senderPltId.equals(newLeader) && Msgs.get(i).receiverId.equals(leaved)) {
                                        if(!oracle.get(6).contains(im.getId())) oracle.get(6).add(im.getId());
                                    }
                                    // ****** CASE 6 ******
                                    else if(Msgs.get(i).receiverId.equals(newLeader)){
                                        if(!oracle.get(5).contains(im.getId())) oracle.get(5).add(im.getId());
                                    }
                                }
                            }
                            break;
                        // ======= FOLLOWER LEAVE Cases =======
                        case "LEAVE_REQ":
                            leaved = "";
                            String intermediateLeader = "";
                            for(int i = s_index; i < Msgs.size(); i++) {
                                if(Msgs.get(i).time > time + 20) break;
                                if (Msgs.get(i).commandSent.equals("LEAVE_ACCEPT")) leaved = Msgs.get(i).receiverId;
                                if (Msgs.get(i).commandSent.equals("SPLIT_REQ")) {
                                    if (!Msgs.get(i).receiverId.equals(leaved)) {
                                        intermediateLeader = Msgs.get(i).receiverId;
                                    }
                                }
                                if (Msgs.get(i).commandSent.equals("MERGE_REQ")) {
                                    // ****** CASE 9 ******
                                    if (Msgs.get(i).senderPltId.equals(intermediateLeader) && Msgs.get(i).receiverId.equals(leaved)) {
                                        if(!oracle.get(8).contains(im.getId())) oracle.get(8).add(im.getId());
                                    } else {
                                        // ****** CASE 8 ******
                                        if (Msgs.get(i).receiverId.equals(intermediateLeader)) {
                                            if(!oracle.get(7).contains(im.getId())) oracle.get(7).add(im.getId());
                                        }
                                        // ****** CASE 10 ******
                                        else if (Msgs.get(i).receiverId.equals(leaved)) {
                                            if(!oracle.get(9).contains(im.getId())) oracle.get(9).add(im.getId());
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        }
    }

    private ArrayList<Integer> timeToCheck(ArrayList<Message> Msgs) {
        ArrayList<Integer> time_to_check = new ArrayList<>();
        ArrayList<Integer> time_ret = new ArrayList<>();
        for(int i = 0; i < Msgs.size(); i++) {
            if(Msgs.get(i).commandSent.equals("MERGE_REQ")) {
                if(i < Msgs.size()-1 && (Msgs.get(i+1).commandSent.equals("MERGE_REQ") && Msgs.get(i+1).senderPltId.equals(Msgs.get(i).senderPltId)
                        && Msgs.get(i+1).receiverId.equals(Msgs.get(i).receiverId))) {
                    int time = Msgs.get(i).time.intValue();
                    if(time < 45) time_to_check.add(25);
                    else if (time < 65) time_to_check.add(45);
                    else if (time < 85) time_to_check.add(65);
                    else time_to_check.add(85);
                } else if (i < Msgs.size()-1 && (Msgs.get(i+1).commandSent.equals("MERGE_REJECT") && Msgs.get(i+1).senderPltId.equals(Msgs.get(i).receiverId)
                        && Msgs.get(i+1).receiverId.equals(Msgs.get(i).senderPltId))) {
                    int time = Msgs.get(i).time.intValue();
                    if(time < 45) time_to_check.add(25);
                    else if (time < 65) time_to_check.add(45);
                    else if (time < 85) time_to_check.add(65);
                    else time_to_check.add(85);
                }
            }
        }
        // Remove overlapping values
        for (int i = 0; i < time_to_check.size(); i++) {
            if (!time_ret.contains(time_to_check.get(i))) {
                time_ret.add(time_to_check.get(i));
            }
        }
//        for(int i = 0; i < time_ret.size(); i++) System.out.println(time_ret.get(i));
        return time_ret;
    }

    public ArrayList<ArrayList<String>> getOracle() {
        return oracle;
    }

    public void printOracle() {
        System.out.println("===========Oracle Print===========");
        for(int i = 0; i < oracle.size(); i++) {
            String prt = "";
            for(String str: oracle.get(i)) {
                prt += str + ",";
            }
            System.out.println("CASE " + (i+1) + ": " + prt);
        }
    }

    public ArrayList<String> getIndex() {
        return IM_index;
    }

    public void getOracleCSV() {
        File file2 = new File(System.getProperty("user.dir") + "/SoS_Extension/oracle.csv");
        System.out.println(System.getProperty("user.dir") + "/SoS_Extension/oracle.csv");
        FileWriter writer2 = null;
        try {
            writer2 = new FileWriter(file2);
//          writer.write("operationSuccessRate for " + txtdir.replace(System.getProperty("user.dir") + "/StarPlateS/SoS_Extension/", "") + " is " + Boolean.toString(ret) + "\n");
            String tempStr = "";
            for(int i = 0; i < oracle.size(); i++) {
                tempStr += "CASE " + (i+1) + ", ";
                for(String eachString: oracle.get(i)) {
                    tempStr += eachString + ",";
                }
                writer2.write(tempStr+"\n");
                tempStr = "";
            }
            writer2.flush();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(writer2 != null) writer2.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
