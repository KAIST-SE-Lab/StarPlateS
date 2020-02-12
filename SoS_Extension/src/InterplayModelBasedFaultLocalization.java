
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

public class InterplayModelBasedFaultLocalization {
    ArrayList<ArrayList<Message>> suspSequences;
    ArrayList<ArrayList<String>> suspIMs;

    public InterplayModelBasedFaultLocalization() {
        suspSequences = new ArrayList<>();
        suspIMs = new ArrayList<>();
    }

    public void addFailedLog(boolean isTracePassed, int s_index, int r_index) {
        ArrayList<Message> temp = null;
        Random random = new Random();
        if(isTracePassed) return; //TODO How to use Succeeded Test Cases

        InterplayModel interplayModel = new InterplayModel(s_index, r_index);
        //interplayModel.printSequence();

        if(suspSequences.size() == 0) {
            suspSequences.add(interplayModel.getMsgSequence());
            ArrayList<String> tempString = new ArrayList<>();
            tempString.add(interplayModel.getId());
            suspIMs.add((ArrayList)tempString.clone());
            return;
        }

        for(int i = 0; i < suspSequences.size(); i++) {
            temp = DP_LCS(suspSequences.get(i), interplayModel.getMsgSequence());

//            if(i == suspSequences.size() - 1)  {
//                suspSequences.add(interplayModel.getMsgSequence());
//                suspIMs.add(1);
//                break;
//            }
            // Matched
            if(temp != null && temp.size() >= 10) { // 10 is the smallest size of interaction sequences in the platooning VENTOS
                suspSequences.set(i, (ArrayList<Message>)temp.clone());
//                suspIMs.set(i, suspIMs.get(i) + 1);
                suspIMs.get(i).add(interplayModel.getId());
            } else { // Not matched
                suspSequences.add(interplayModel.getMsgSequence());
                ArrayList<String> tempString = new ArrayList<>();
                tempString.add(interplayModel.getId());
                suspIMs.add((ArrayList)tempString.clone());
            }
            
            // Random Addition of LCSs into the knowledge
            if(random.nextFloat() < 0.2) {
                for(ArrayList<String> tempString : suspIMs) {
                    for(String id: tempString) {
                        StringTokenizer st = new StringTokenizer(id, "_");
                        int ts_index = Integer.valueOf(st.nextToken());
                        int tr_index = Integer.valueOf(st.nextToken());
    
                        InterplayModel tm = new InterplayModel(ts_index, tr_index);
                        temp = DP_LCS(tm.getMsgSequence(), interplayModel.getMsgSequence());
                        if(temp != null && temp.size() >= 10) {
                            suspSequences.add(temp);
                            ArrayList<String> tempString2 = new ArrayList<>();
                            tempString2.add(interplayModel.getId());
                            tempString2.add(tm.getId());
                            suspIMs.add((ArrayList)tempString2.clone());
                        }
                    }
                }
            }
        }
    }

    private ArrayList<Message> DP_LCS(ArrayList<Message> lcs, ArrayList<Message> trace) {
//        System.out.println(lcs.size() + " " + trace.size());
        int[][] LCS = new int[lcs.size()+1][trace.size()+1];
        ArrayList<Message> ret_i = new ArrayList<>();
        ArrayList<Message> ret_j = new ArrayList<>();
        ArrayList<Message> ret = new ArrayList<>();

        // Generate LCS Table between two inputs
        for(int i = 0; i <= lcs.size(); i++) {
            for(int j = 0; j <= trace.size(); j++) {
                // For the convenience of the calculation, assign i=0 or j=0 as 0
                if(i == 0 || j == 0) {
                    LCS[i][j] = 0;
                    continue;
                }
//                System.out.println(lcs.get(i-1).commandSent);
//                System.out.println(compareMessage(lcs.get(i-1), trace.get(j-1)));
                // Same message case
                if(compareMessage(lcs.get(i-1), trace.get(j-1))) {
                    LCS[i][j] = LCS[i-1][j-1] + 1;
                } else { // Different message case
                    LCS[i][j] = Math.max(LCS[i][j-1], LCS[i-1][j]);
                }
            }
        }

        // No LCS exists
        if(LCS[lcs.size()][trace.size()] == 0) return null;
        else if (LCS[lcs.size()][trace.size()] < lcs.size()) { // Shorter LCS exists
            // Extract new LCS
            int current = 0;
            for(int i = 1; i <= lcs.size(); i++) {
                for(int j = 1; j <= trace.size(); j++) {

                    if(LCS[i][j] > current) {
                        current++;
                        ret_i.add(lcs.get(i-1));
                        ret_j.add(trace.get(j-1));
                    }

                }
            }
            // To remove commonly happened events in the beginning of the simulations
            // E.g. Split operation of platoons with size larger than the opt_size
            float time_i = -1;
            float time_j = -1;
            for(int i = ret_i.size()-1, j = ret_j.size()-1; i >= 0 && j >= 0; i--, j--) {
                time_i = Float.valueOf(ret_i.get(i).time);
                time_j = Float.valueOf(ret_j.get(j).time);
                if(time_i < 25.0 || time_j < 25.0) continue;
                else ret.add(ret_i.get(i));
            }
            return ret;
        } else { // No shorter LCS exists
            return lcs;
        }

    }

    private boolean compareMessage(Message m_a, Message m_b) {

        if(m_a.commandSent.equals(m_b.commandSent) && m_a.senderPltId.equals(m_b.senderPltId) // TODO How much information would be considered in comparison??
                && m_a.receiverId.equals(m_b.receiverId)) return true;

        return false;
    }

    public void printSuspSequences() {
        Message temp;
        for(int i = 0; i <suspSequences.size(); i++) {
//            if(suspCounter.get(i) < 2) continue;
//            System.out.println("LCS_" + i + ": counted " + suspCounter.get(i) + " times");

            for(int j = 0; j < suspSequences.get(i).size(); j++) {
                temp = suspSequences.get(i).get(j);
                System.out.println(temp.time + ": " + temp.commandSent + " from " + temp.senderPltId + " to " + temp.receiverId);
            }
        }
    }
}
