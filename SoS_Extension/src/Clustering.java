import java.lang.reflect.Array;
import java.util.*;

public class Clustering {

    private ArrayList<ArrayList<InterplayModel>> cluster;
    private ArrayList<ArrayList<Message>> centroidLCS;

    public Clustering() {
        cluster = new ArrayList<>();
        centroidLCS = new ArrayList<>();
    }

    public void addTrace(InterplayModel im_trace, double simlr_threshold) {                                               // simThreshold: Similarity Threshold
        ArrayList<Integer> updatedCluster = new ArrayList<>(Collections.nCopies(cluster.size(), 0));
        ArrayList<Message> generatedLCS;
        Boolean assignFlag = false;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            return;
        }

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for(int i = 0; i < cluster.size(); i++) {
            if(cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재할때, Cluster의 LCS가 존재하는것을
                double temp = similarityChecker(centroidLCS.get(i), im_trace.getMsgSequence());
                System.out.println("Cluster id: " + i + " Input_trace id: "+ im_trace.id + " similarity: " + temp);
                if(temp >= simlr_threshold) {                                                                            // 가정하기 때문에 LCS와 given IM간의 Similarity를 비교함
                    cluster.get(i).add(im_trace);
                    updatedCluster.set(i,1);
                    assignFlag = true;
                }
            } else {
                generatedLCS = LCSExtractor(cluster.get(i).get(0).getMsgSequence(), im_trace.getMsgSequence());         // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지
                if(generatedLCS != null && generatedLCS.size() > 10) {                                                                          // 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                    cluster.get(i).add(im_trace);
                    updatedCluster.set(i,1);
                    assignFlag = true;
                }
            }
        }

        if(!assignFlag) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size()-1).add(im_trace);
            return;
        }

        // Updated cluster에 대해 Representative LCS (Centroid)를 업데이트하는 과정
        for(int i = 0; i < cluster.size(); i++) {
            if(updatedCluster.get(i) == 1) {
                int j = 1;
                generatedLCS = (ArrayList) cluster.get(i).get(0).getMsgSequence().clone();
                while(j <= cluster.get(i).size()-1) {
                    generatedLCS = LCSExtractor(generatedLCS, cluster.get(i).get(j).getMsgSequence());
                    Collections.reverse(generatedLCS);
                    j++;
                }
                updatedCluster.set(i,0);
                centroidLCS.set(i, generatedLCS);
            }
            LCSRedundancyAnalyzer(i, 20); // TODO Threshold: the number of repetition of the same sync messages threshold
        }
//        System.out.println("add trace Finish");
    }

    private void LCSRedundancyAnalyzer(int id_cluster, int redundancy_threshold) {
        HashMap<String, Integer> LCS_analysis = new HashMap<>();
        ArrayList<Message> target_LCS = centroidLCS.get(id_cluster);
        ArrayList<Message> result_LCS = new ArrayList<>();
        ArrayList<Message> redundancy_list = new ArrayList<>();
        String key_ = "";
        Boolean flag = false;

        if(target_LCS.size() == 0) return;

        for(int i = 0; i < target_LCS.size()-1; i++) {
            key_ = target_LCS.get(i).commandSent + "_" + target_LCS.get(i+1).commandSent;
            if(LCS_analysis.containsKey(key_)) {
                LCS_analysis.put(key_, LCS_analysis.get(key_)+1);

                if(LCS_analysis.get(key_) >= redundancy_threshold
                        && !redundancy_list.contains(target_LCS.get(i))) {
                    redundancy_list.add(target_LCS.get(i));
                }
            } else {
                LCS_analysis.put(key_, 1);
            }

            if(LCS_analysis.get(key_) < redundancy_threshold) {
                for(int j = 0; j < redundancy_list.size(); j++) {
                    if(compareMessage(target_LCS.get(i), redundancy_list.get(j))) {
                        flag = true;
                    }
                }
                if (!flag) result_LCS.add(target_LCS.get(i));
            }
        }

        // 위의 for문에 포함되지 않는 LCS의 맨 마지막 인자 추가
        if(!redundancy_list.contains(target_LCS.get(target_LCS.size()-1))) result_LCS.add(target_LCS.get(target_LCS.size()-1));
//
//        for(String key : LCS_analysis.keySet()) {
//            System.out.println(key + ": " + LCS_analysis.get(key));
//        }
//
//        Message temp;
//        for(int i = 0; i < result_LCS.size(); i++) {
//            temp = result_LCS.get(i);
//            System.out.println(i + " " + temp.time + ": " + temp.commandSent + " from " + temp.senderPltId + " to " + temp.receiverId);
//        }
        centroidLCS.set(id_cluster, (ArrayList) result_LCS.clone());
    }

    public void printCluster() {
        Message temp;
        for(int i = 0; i <cluster.size(); i++) {
            System.out.println("Cluster " + i + "=================");
            System.out.println("Representative LCS:");

//            for(int j = 0; j < centroidLCS.get(i).size(); j++) {
//                temp = centroidLCS.get(i).get(j);
//                System.out.println(j + " " + temp.time + ": " + temp.commandSent + " from " + temp.senderPltId + " to " + temp.receiverId);
//            }

            System.out.println("Clustered IMs:");
            for(int j = 0; j < cluster.get(i).size(); j++) {
                System.out.println((j+1) + ": IM_" + cluster.get(i).get(j).getId());
            }
        }
    }

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
                if(compareMessage(data_point.get(i-1), input_trace.get(j-1))) {                                         // TODO Delay Comparison?
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
            Collections.reverse(data_point);
            return data_point;
        }
    }

    private boolean compareMessage(Message m_a, Message m_b) {

        if(m_a.time < 25.00 || m_b.time < 25.00) return false;
        if(m_a.commandSent.equals(m_b.commandSent) && m_a.senderRole.equals(m_b.senderRole)
                && m_a.receiverRole.equals(m_b.receiverRole)) return true;

//        if(m_a.commandSent.equals(m_b.commandSent) && m_a.senderPltId.equals(m_b.senderPltId) // TODO How much information would be considered in comparison??
//                && m_a.receiverId.equals(m_b.receiverId)) return true;

        return false;
    }

    private double similarityChecker(ArrayList<Message> lcs, ArrayList<Message> input_trace) {
        int matched = 0;
        int prevMatchedId = -1;
        int total = 0;

        for(int i = 0; i < lcs.size(); i++) { // TODO 단일 IM 상에서 여러 개의 시작 지점 고려해야함
            if (matched == 0) {
                for(int j = 0; j < input_trace.size(); j++) {
                    if(compareMessage(lcs.get(i), input_trace.get(j)) == true) {
                        matched += 1;
                        prevMatchedId = j;
//                        System.out.println(i);
                        break;
                    }
                }
                total += 1;
            } else {
                for(int j = prevMatchedId + 1; j <input_trace.size(); j++) {
                    if(compareMessage(lcs.get(i), input_trace.get(j)) == true
                            && calMessageDelay(lcs, input_trace, i, j, prevMatchedId) == true) {
                        matched += 1;
                        prevMatchedId = j;
//                        System.out.println(i);
                        break;
                    }
                }
                total += 1;
            }
        }

        return (double)matched/(double)total;
    }

    private boolean calMessageDelay(ArrayList<Message> lcs, ArrayList<Message> input_trace, int id_lcs, int id_trace, int prev_id_trace) {
        float lcs_delay = lcs.get(id_lcs-1).time - lcs.get(id_lcs).time;
        float trace_delay = input_trace.get(prev_id_trace).time - input_trace.get(id_trace).time;

        if(Math.abs(lcs_delay - trace_delay) <= 1.0) return true; // TODO Message Delay Similarity Threshold 0.1 & Simlr 0.85 / 0.15 & Simlr 0.95 / 1.00 Simlr 1.0?
        else {
//            System.out.println("lcs_id: " + id_lcs + "/ id_trace: " + id_trace + " time: "+ input_trace.get(id_trace).time + "/ prev_id_trace " + prev_id_trace+ " time: "+ input_trace.get(prev_id_trace).time);
//            System.out.println(Math.abs(lcs_delay - trace_delay));
            return false;
        }
    }
}
