import java.util.ArrayList;

public class Clustering {

    private ArrayList<Message> LCSExtractor(ArrayList<Message> data_point, ArrayList<Message> input_trace) {
        int[][] LCS = new int[data_point.size()+1][input_trace.size()+1];
        ArrayList<Message> ret_i = new ArrayList<>();
        ArrayList<Message> ret_j = new ArrayList<>();
        ArrayList<Message> ret = new ArrayList<>();

        // Generate LCS Table between two inputs
        for(int i = 0; i <= data_point.size(); i++) {
            for(int j = 0; j <= input_trace.size(); j++) {
                // For the convenience of the calculation, assign i=0 or j=0 as 0
                if(i == 0 || j == 0) {
                    LCS[i][j] = 0;
                    continue;
                }
//                System.out.println(data_point.get(i-1).commandSent);
//                System.out.println(compareMessage(data_point.get(i-1), input_trace.get(j-1)));
                // Same message case
                if(compareMessage(data_point.get(i-1), input_trace.get(j-1))) {
                    LCS[i][j] = LCS[i-1][j-1] + 1;
                } else { // Different message case
                    LCS[i][j] = Math.max(LCS[i][j-1], LCS[i-1][j]);
                }
            }
        }

        // No LCS exists
        if(LCS[data_point.size()][input_trace.size()] == 0) return null;
        else if (LCS[data_point.size()][input_trace.size()] < data_point.size()) { // Shorter LCS exists
            // Extract new LCS
            int current = 0;
            for(int i = 1; i <= data_point.size(); i++) {
                for(int j = 1; j <= input_trace.size(); j++) {

                    if(LCS[i][j] > current) {
                        current++;
                        ret_i.add(data_point.get(i-1));
                        ret_j.add(input_trace.get(j-1));
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
            return data_point;
        }
    }

    private boolean compareMessage(Message m_a, Message m_b) {

        if(m_a.commandSent.equals(m_b.commandSent) && m_a.senderPltId.equals(m_b.senderPltId) // TODO How much information would be considered in comparison??
                && m_a.receiverId.equals(m_b.receiverId)) return true;

        return false;
    }

    private float similarityChecker(ArrayList<Message> lcs, ArrayList<Message> input_trace) {
        int matched = 0;
        int prevMatchedId = -1;
        int total = 0;

        for(int i = 0; i < lcs.size(); i++) {
            if (matched == 0) {
                for(Message m : input_trace) {
                    if(compareMessage(lcs.get(i), m) == true) {
                        matched += 1;
                        prevMatchedId = input_trace.indexOf(m);
                    }
                }
                total += 1;
            } else {
                for(Message m : input_trace) {
                    if(compareMessage(lcs.get(i), m) == true
                            && calMessageDelay(lcs, input_trace, i, input_trace.indexOf(m), prevMatchedId) == true) {
                        matched += 1;
                        prevMatchedId = input_trace.indexOf(m);
                    }
                }
                total += 1;
            }
        }

        return (float) matched/total;
    }

    private boolean calMessageDelay(ArrayList<Message> lcs, ArrayList<Message> input_trace, int id_lcs, int id_trace, int prev_id_trace) {
        float lcs_delay = lcs.get(id_lcs).time - lcs.get(id_lcs-1).time;
        float trace_delay = input_trace.get(id_trace).time - input_trace.get(prev_id_trace).time;

        if(Math.abs(lcs_delay - trace_delay) <= 0.1) return true;
        else return false;
    }
}
