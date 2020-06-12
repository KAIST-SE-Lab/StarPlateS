
import java.lang.reflect.Array;
import java.util.*;

public class Clustering {

    private ArrayList<ArrayList<InterplayModel>> cluster;
    private ArrayList<ArrayList<Message>> centroidLCS;
    private ArrayList<Float> startingTime;

    public Clustering() {
        cluster = new ArrayList<>();
        centroidLCS = new ArrayList<>();
        startingTime = new ArrayList<>();
        startingTime.add((float)25.00);
        startingTime.add((float)45.00);
        startingTime.add((float)65.00);
        startingTime.add((float)85.00);
    }

    public void addTrace(InterplayModel im_trace, double simlr_threshold, double delay_threshold
            , int lcs_min_len_threshold) {                                                                        // simlrThreshold: Similarity Threshold
        ArrayList<Integer> updatedCluster = new ArrayList<>(Collections.nCopies(cluster.size(), 0));
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        Boolean assignFlag = false;
        int lcs_index;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            return;
        }

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for(int i = 0; i < cluster.size(); i++) {
            generatedLCS.clear();
            lcs_index = -1;
            for(int j = 0; j < startingTime.size(); j++) {
                generatedLCS.add(LCSExtractor(IMSlicer(startingTime.get(j),im_trace.getMsgSequence()),                  // Starting time에 따라 given IM을 slicing 하여
                        cluster.get(i).get(0).getMsgSequence(), delay_threshold));                                                       // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
                if(generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
            }
            
            if(cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                for(int j = 0; j < startingTime.size(); j++) {                                                          // Starting time에 따라 slicing 된 given IM에 대해 생성된
                    if(generatedLCS.get(j) == null) continue;                                                           // generated_LCS (lcs between centroid_lcs and given IM)
                    double temp = (double) generatedLCS.get(j).size() / (double) centroidLCS.get(i).size();             // 와 기존 centroid_lcs와의 size를 비교하여 simlr_threshold
                                                                                                                        // 를 넘는지 확인함
                    if (temp >= simlr_threshold) {                                                                      // simlr_threshold를 넘는 경우, centroidLCS를 업데이트

                        assignFlag = true;
                        if(lcs_index == -1) centroidLCS.set(i, generatedLCS.get(j));
                        else {
                            if(generatedLCS.get(j).size() > generatedLCS.get(lcs_index).size()) {                         //길이가 더 긴 경우가 있다면 추가로 업데이트
                                centroidLCS.set(i, generatedLCS.get(lcs_index));
                            }
                        }
                        if(!cluster.get(i).contains(im_trace)) cluster.get(i).add(im_trace);                              // im_trace가 중복으로 cluster에 입력되는 거 방지
                    }
                }
            } else {                                                                                                    // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                int best_message_type_num = 0;
                int lcs_length = 0;
                int current_message_type_num = 0;
                for(ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                    if(lcs == null) continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                    current_message_type_num = LCSPatternAnalyzer(lcs);
                    if(best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                        lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                        lcs_length = lcs.size();                                                                          // 후자 선택
                        best_message_type_num = current_message_type_num;
                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                        if(lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                            lcs_index = generatedLCS.indexOf(lcs);
                            lcs_length = lcs.size();
                            best_message_type_num = current_message_type_num;
                        }
                    }
                }

                if(lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                    cluster.get(i).add(im_trace);
                    centroidLCS.set(i,generatedLCS.get(lcs_index));
//                    updatedCluster.set(i,1);
                    assignFlag = true;
                }
            }
        }
//
        if(!assignFlag) {                                                                                               // given IM이 어떠한 existing cluster에도 입력되지 않은 경우
            cluster.add(new ArrayList<>());                                                                             // 새로운 cluster와 centroidLCS slot를 생성한 후
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size()-1).add(im_trace);                                                                // 해당 IM을 cluster에 삽입
            return;
        }

        // Updated cluster에 대해 Representative LCS (Centroid)를 업데이트하는 과정
//        for(int i = 0; i < cluster.size(); i++) {
//            if(updatedCluster.get(i) == 1) {
//                int j = 1;
//                Collections.shuffle(cluster.get(i));  // TODO Choose whether to use the shuffle in LCS generation for generating appropriate LCS among multiple IMs
//                generatedLCS = (ArrayList) cluster.get(i).get(0).getMsgSequence().clone();
//                while(j <= cluster.get(i).size()-1) {
//                    generatedLCS = LCSExtractor(generatedLCS, cluster.get(i).get(j).getMsgSequence());
////                    Collections.reverse(generatedLCS);
//                    j++;
//                }
//                updatedCluster.set(i,0);
//                centroidLCS.set(i, generatedLCS);
//            }
//            LCSRedundancyAnalyzer(i, 20); // TODO Threshold: the number of repetition of the same sync messages threshold
//        }
//        System.out.println("add trace Finish");
    }

    private ArrayList<Message> IMSlicer(float starting_time, ArrayList<Message> IM_msg) {
        ArrayList<Message> ret = new ArrayList<>();

        for(int i = 0; i < IM_msg.size(); i++) {
            if(IM_msg.get(i).time >= starting_time) ret.add(IM_msg.get(i));
        }

        return (ArrayList)ret.clone();
    }

    private int LCSPatternAnalyzer(ArrayList<Message> lcs) {
        HashMap<String, Integer> LCS_analysis = new HashMap<>();
        String key_ = "";

        if(lcs.size() == 0) return 0;

        for(int i = 0; i < lcs.size()-1; i++) {
            key_ = lcs.get(i).commandSent + "_" + lcs.get(i + 1).commandSent;
            if (LCS_analysis.containsKey(key_)) {
                LCS_analysis.put(key_, LCS_analysis.get(key_) + 1);
            } else {
                LCS_analysis.put(key_, 1);
            }
        }

        return LCS_analysis.size();
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

    public void ClusteringFinalize(double simlr_threshold, double delay_threshold, int lcs_min_len_threshold) {         // TODO Clustering Finalize Concurrent Modification Exception
//        for(Iterator<ArrayList<InterplayModel>> iterator = cluster.iterator(); iterator.hasNext();) {                 // TODO cluster에 접근해서 데이터를 cluster내의 다른 arraylist에 입력
//            for(Iterator<InterplayModel> it2= iterator.next().iterator(); it2.hasNext();) {                           // TODO 해서 그런거라 예상됨
//                this.addTrace(it2.next(), simlr_threshold, delay_threshold, lcs_min_len_threshold);
//            }
//        }
//        cluster.stream().forEach(IMs -> IMs.stream().forEach(
//                IM -> this.addTrace(IM, simlr_threshold, delay_threshold, lcs_min_len_threshold)
//                )
//        );
    }

    private ArrayList<Message> LCSExtractor(ArrayList<Message> data_point, ArrayList<Message> input_trace, double delay_threshold) {
        int[][] LCS = new int[data_point.size()+1][input_trace.size()+1];
        ArrayList<Message> ret_i = new ArrayList<>();
        ArrayList<Message> ret_j = new ArrayList<>();
        ArrayList<Message> ret = new ArrayList<>();

        // To save previous LCS point for comparing delay function.
        int prev_i = -1;
        int prev_j = -1;
        ArrayList<Pair> LCS_log = new ArrayList<>();

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
                    // Checking message delay difference between two given IM
                    if((prev_i == -1 && prev_j == -1)
                            || calMessageDelay(data_point.get(prev_i-1), data_point.get(i-1), input_trace.get(prev_j-1), input_trace.get(j-1), delay_threshold))
                    {
                        LCS[i][j] = LCS[i - 1][j - 1] + 1;
                        if(prev_i != i) {
                            prev_i = i;
                            prev_j = j;
//                            LCS_log.add(new Pair(prev_i, prev_j));
                        }
                    } else { // Same message but different delay
                        LCS[i][j] = Math.max(LCS[i][j-1], LCS[i-1][j]);
                    }
                } else { // Different message case
                    LCS[i][j] = Math.max(LCS[i][j-1], LCS[i-1][j]);
                }
            }
        }

//        System.out.println(LCS[data_point.size()][input_trace.size()]);

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

//    private double similarityCheckerLCS(ArrayList<Message> lcs, ArrayList<Message> input_trace) {
//        ArrayList<Message> lcs_lcs = LCSExtractor(lcs, input_trace);
//
//        if(lcs_lcs == null) return 0;
//        else {
//            return (double) lcs_lcs.size() / (double) lcs.size();
//        }
//    }

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

    private boolean calMessageDelay(Message prev_i, Message curnt_i, Message prev_j, Message curnt_j, double delay_threshold) {
        float i_delay = curnt_i.time - prev_i.time;
        float j_delay = curnt_j.time - prev_j.time;

        if(Math.abs(i_delay - j_delay) <= delay_threshold) return true;
        else return false;
    }

    public double EvaluateClusteringResult(ArrayList<ArrayList<String>> oracle) {
        double TP = 0;
        double FP = 0;
        double FN = 0;
        double TN = 0;
        boolean cl_front;
        boolean cl_back;
        boolean cl_same;
        boolean ol_front;
        boolean ol_back;
        boolean ol_same;

        ArrayList<String> index = new ArrayList<>(Arrays.asList("3_0","6_0","7_0","8_0","9_0","11_0"
                ,"12_0","13_0","17_0","22_0","24_0","27_0","29_0","30_0","34_0","38_0"
                ,"41_0","43_0","45_0","46_0","47_0","49_0"));

        for(int i = 0; i < index.size(); i++) {                                                                         // Generate pair for indexes
            for(int j = i+1; j < index.size(); j++) {
                cl_front = false;
                cl_back = false;
                cl_same = false;
                ol_front = false;
                ol_back = false;
                ol_same = false;

                for(ArrayList<InterplayModel> IMs : cluster) {                                                          // Checking whether the pair is in the
                    for(InterplayModel IM : IMs) {                                                                      // same cluster in the Clustering result
                        if(IM.getId().equals(index.get(i))) cl_front = true;
                        if(IM.getId().equals(index.get(j))) cl_back = true;
                        if(cl_front && cl_back) {
                            cl_same = true;
                            break;
                        }
                    }
                    if(cl_same) break;
                    cl_front = false;
                    cl_back = false;
                }

                for(ArrayList<String> ids: oracle) {                                                                    // Checking whether the pair is in the
                    for(String id: ids) {                                                                               // same cluster in the Oracle
                        if(id.equals(index.get(i))) ol_front = true;
                        if(id.equals(index.get(j))) ol_back = true;
                        if(ol_front && ol_back) {
                            ol_same = true;
                            break;
                        }
                    }
                    if(ol_same) break;
                    ol_front = false;
                    ol_back = false;
                }

                if(cl_same && ol_same) TP++;
                else if (cl_same) FP++;
                else if (ol_same) FN++;
                else TN++;
            }
        }
        return Math.sqrt((TP/(TP+FP)) * (TP/(TP+FN)));                                                                  // Fowlkes-Mallows index
    }                                                                                                                   // https://gentlej90.tistory.com/64

    public void clusterClear() {
        this.cluster.clear();
        this.centroidLCS.clear();
    }
}
