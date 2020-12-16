import java.util.ArrayList;

public class OracleGenerator {
    private ArrayList<ArrayList<String>> oracle;

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
    }

    public ArrayList<ArrayList<String>> oracleGeneration(ArrayList<InterplayModel> IMs) {
        ArrayList<Message> Msgs = null;
        ArrayList<Integer> time_to_check = new ArrayList<>();
        for (InterplayModel im : IMs) {
            System.out.println("IM_"+im.getId());
            Msgs = im.getMsgSequence();

            time_to_check = timeToCheck(Msgs);

            for(int time : time_to_check) {
                int s_index;
                for(s_index = 0; s_index < Msgs.size(); s_index++) {
                    if(Msgs.get(s_index).time >= time) break;
                }

                // ======= Simultaneous Requests (REQ 다음에 바로 REQ가 나오는 경우) =======
                if(Msgs.get(s_index+1).commandSent.contains("REQ")) {

                }
                else {
                    switch (Msgs.get(s_index).commandSent) {
                        // ======= SPLIT & MERGE (by Optimal Size Policy) =======
                        case "SPLIT_REQ":
                            break;
                        // ======= LEADER LEAVE Cases =======
                        case "VOTE_LEADER":
                            break;
                        // ======= FOLLOWER LEAVE Cases =======
                        case "LEAVE_REQ":
                            break;
                    }
                }
            }
            break;
        }
        return oracle;
    }

    //======== Check continuous Merge_Requests ========
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
}
