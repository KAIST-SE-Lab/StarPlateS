
import java.io.*;
import java.sql.Array;
import java.util.*;
import java.io.File;
import java.io.FileReader;

public class Clustering {

    private ArrayList<ArrayList<InterplayModel>> cluster;
    private ArrayList<ArrayList<Message>> centroidLCS;
    private ArrayList<Float> startingTime;

    private ArrayList<ArrayList<InterplayModel>> originCluster;
    private ArrayList<ArrayList<Message>> originCentroidLCS;
    private ArrayList<InterplayModel> id_patterns;

    public Clustering() {
        cluster = new ArrayList<>();
        centroidLCS = new ArrayList<>();
        startingTime = new ArrayList<>();
        originCluster = new ArrayList<>();
        originCentroidLCS = new ArrayList<>();
        id_patterns = new ArrayList<>();
        startingTime.add((float) 25.00);
        startingTime.add((float) 45.00);
        startingTime.add((float) 65.00);
        startingTime.add((float) 85.00);

        // Get ideal patterns from txt
        File folder = new File(System.getProperty("user.dir") + "/SoS_Extension/results/Ideal/");
        File files[] = folder.listFiles();
        for (File file : files) {
            InterplayModel interplayModel = new InterplayModel(file);
            id_patterns.add(interplayModel);
        }
    }

    public void addTraceCase1(InterplayModel im_trace, double simlr_threshold, double delay_threshold,
                              int lcs_min_len_threshold) {                                                                // simThreshold: Similarity Threshold
        ArrayList<Integer> updatedCluster = new ArrayList<>(Collections.nCopies(cluster.size(), 0));
        ArrayList<Message> generatedLCS;
        Boolean assignFlag = false;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for (int i = 0; i < cluster.size(); i++) {
            if (cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재할때, Cluster의 LCS가 존재하는것을
                double temp = similarityChecker(centroidLCS.get(i), im_trace.getMsgSequence(), delay_threshold);
                if (temp >= simlr_threshold) {                                                                            // 가정하기 때문에 LCS와 given IM간의 Similarity를 비교함
                    cluster.get(i).add(im_trace);
                    updatedCluster.set(i, 1);
                    assignFlag = true;
                }
            } else {
                generatedLCS = LCSExtractorWithoutDelay(cluster.get(i).get(0).getMsgSequence(), im_trace.getMsgSequence());      // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지
                if (generatedLCS != null && generatedLCS.size() > lcs_min_len_threshold) {                               // 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                    cluster.get(i).add(im_trace);
                    updatedCluster.set(i, 1);
                    assignFlag = true;
                }
            }
        }

        if (!assignFlag) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);
            return;
        }

        // Updated cluster에 대해 Representative LCS (Centroid)를 업데이트하는 과정
        for (int i = 0; i < cluster.size(); i++) {
            if (updatedCluster.get(i) == 1) {
                int j = 1;
//                Collections.shuffle(cluster.get(i));  // TODO Choose whether to use the shuffle in LCS generation for generating appropriate LCS among multiple IMs
                generatedLCS = (ArrayList) cluster.get(i).get(0).getMsgSequence().clone();
                while (j <= cluster.get(i).size() - 1) {
                    generatedLCS = LCSExtractorWithoutDelay(generatedLCS, cluster.get(i).get(j).getMsgSequence());
                    Collections.reverse(generatedLCS);
                    j++;
                }
                updatedCluster.set(i, 0);
                centroidLCS.set(i, generatedLCS);
            }
            LCSRedundancyAnalyzer(i, 20); // TODO Threshold: the number of repetition of the same sync messages threshold
        }
    }

    public void addTraceCase2(InterplayModel im_trace, double simlr_threshold, double delay_threshold,
                              int lcs_min_len_threshold) {                                                                        // simlrThreshold: Similarity Threshold
        ArrayList<Integer> updatedCluster = new ArrayList<>(Collections.nCopies(cluster.size(), 0));
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        Boolean assignFlag = false;
        int lcs_index;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for (int i = 0; i < cluster.size(); i++) {
            generatedLCS.clear();
            lcs_index = -1;
            for (int j = 0; j < startingTime.size(); j++) {
                generatedLCS.add(LCSExtractorWithDelay(IMSlicer(startingTime.get(j), im_trace.getMsgSequence()),                  // Starting time에 따라 given IM을 slicing 하여
                        centroidLCS.get(i), delay_threshold));                                                       // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
                if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
            }

            if (cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                for (int j = 0; j < startingTime.size(); j++) {                                                          // Starting time에 따라 slicing 된 given IM에 대해 생성된
                    if (generatedLCS.get(j) == null)
                        continue;                                                           // generated_LCS (lcs between centroid_lcs and given IM)
                    double temp = (double) generatedLCS.get(j).size() / (double) centroidLCS.get(i).size();             // 와 기존 centroid_lcs와의 size를 비교하여 simlr_threshold
                    // 를 넘는지 확인함
                    if (temp >= simlr_threshold) {                                                                      // simlr_threshold를 넘는 경우, centroidLCS를 업데이트

                        assignFlag = true;
                        if (lcs_index == -1) centroidLCS.set(i, generatedLCS.get(j));
                        else {
                            if (generatedLCS.get(j).size() > generatedLCS.get(lcs_index).size()) {                         //길이가 더 긴 경우가 있다면 추가로 업데이트
                                centroidLCS.set(i, generatedLCS.get(lcs_index));
                            }
                        }
                        if (!cluster.get(i).contains(im_trace))
                            cluster.get(i).add(im_trace);                              // im_trace가 중복으로 cluster에 입력되는 거 방지
                    }
                }
            } else {                                                                                                    // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                int best_message_type_num = 0;
                int lcs_length = 0;
                int current_message_type_num = 0;
                for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                    if (lcs == null)
                        continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                    current_message_type_num = LCSPatternAnalyzer(lcs);
                    if (best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                        lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                        lcs_length = lcs.size();                                                                          // 후자 선택
                        best_message_type_num = current_message_type_num;
                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                        if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                            lcs_index = generatedLCS.indexOf(lcs);
                            lcs_length = lcs.size();
                            best_message_type_num = current_message_type_num;
                        }
                    }
                }

                if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                    cluster.get(i).add(im_trace);
                    centroidLCS.set(i, generatedLCS.get(lcs_index));
                    assignFlag = true;
                }
            }
        }
//
        if (!assignFlag) {                                                                                               // given IM이 어떠한 existing cluster에도 입력되지 않은 경우
            cluster.add(new ArrayList<>());                                                                             // 새로운 cluster와 centroidLCS slot를 생성한 후
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);                                                                // 해당 IM을 cluster에 삽입
            centroidLCS.set(centroidLCS.size() - 1, im_trace.getMsgSequence());
        }
    }

    public void addTraceCase3(InterplayModel im_trace, double simlr_threshold, double delay_threshold,
                              int lcs_min_len_threshold) {                                                                        // simlrThreshold: Similarity Threshold
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        Boolean assignFlag = false;
        int lcs_index;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for (int i = 0; i < cluster.size(); i++) {

            if (cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                ArrayList<Message> LCS_lcs = LCSExtractorWithoutDelay(centroidLCS.get(i), im_trace.getMsgSequence());
                double temp;
                if (LCS_lcs == null) continue;
                temp = (double) LCS_lcs.size() / (double) centroidLCS.get(i).size();
                if (temp >= simlr_threshold) {                                                                            // 가정하기 때문에 LCS와 given IM간의 Similarity를 비교함
                    cluster.get(i).add(im_trace);
                    assignFlag = true;
                }
            } else {                                                                                                    // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                generatedLCS.clear();
                lcs_index = -1;
                for (int j = 0; j < startingTime.size(); j++) {
                    generatedLCS.add(LCSExtractorWithDelay(IMSlicer(startingTime.get(j), im_trace.getMsgSequence()),                  // Starting time에 따라 given IM을 slicing 하여
                            centroidLCS.get(i), delay_threshold));         //cluster.get(i).get(0).getMsgSequence()                                              // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
                    if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
                }

                int best_message_type_num = 0;
                int lcs_length = 0;
                int current_message_type_num = 0;
                for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                    if (lcs == null)
                        continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                    current_message_type_num = LCSPatternAnalyzer(lcs);
                    if (best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                        lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                        lcs_length = lcs.size();                                                                          // 후자 선택
                        best_message_type_num = current_message_type_num;
                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                        if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                            lcs_index = generatedLCS.indexOf(lcs);
                            lcs_length = lcs.size();
                            best_message_type_num = current_message_type_num;
                        }
                    }
                }

                if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                    cluster.get(i).add(im_trace);
                    centroidLCS.set(i, generatedLCS.get(lcs_index));
//                    updatedCluster.set(i,1);
                    assignFlag = true;
                }
            }
        }
//
        if (!assignFlag) {                                                                                               // given IM이 어떠한 existing cluster에도 입력되지 않은 경우
            cluster.add(new ArrayList<>());                                                                             // 새로운 cluster와 centroidLCS slot를 생성한 후
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);                                                                // 해당 IM을 cluster에 삽입
            centroidLCS.set(centroidLCS.size() - 1, im_trace.getMsgSequence());
        }
    }

    public void addTraceCase4(InterplayModel im_trace, double simlr_threshold, double delay_threshold,
                              int lcs_min_len_threshold) {                                                                        // simlrThreshold: Similarity Threshold
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        Boolean assignFlag = false;
        int lcs_index;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for (int i = 0; i < cluster.size(); i++) {
            generatedLCS.clear();
            lcs_index = -1;
            for (int j = 0; j < startingTime.size(); j++) {
                generatedLCS.add(LCSExtractorWithDelay(centroidLCS.get(i),  //cluster.get(i).get(0).getMsgSequence()                // Starting time에 따라 given IM을 slicing 하여
                        IMSlicer(startingTime.get(j), im_trace.getMsgSequence()), delay_threshold));                                                       // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
                if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
            }

            if (cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                for (int j = 0; j < startingTime.size(); j++) {                                                          // Starting time에 따라 slicing 된 given IM에 대해 생성된
                    if (generatedLCS.get(j) == null)
                        continue;                                                           // generated_LCS (lcs between centroid_lcs and given IM)
                    double temp = similarityChecker(centroidLCS.get(i), generatedLCS.get(j), delay_threshold);             // 와 기존 centroid_lcs와의 size를 비교하여 simlr_threshold
                    // 를 넘는지 확인함
                    if (temp >= simlr_threshold) {                                                                      // simlr_threshold를 넘는 경우, centroidLCS를 업데이트
                        assignFlag = true;
                        if (generatedLCS.get(j).size() > lcs_min_len_threshold) {
                            if (lcs_index == -1) {
                                centroidLCS.set(i, generatedLCS.get(j));
                                lcs_index = j;
                            } else {
                                if (generatedLCS.get(j).size() > generatedLCS.get(lcs_index).size()) {                         //길이가 더 긴 경우가 있다면 추가로 업데이트
                                    centroidLCS.set(i, generatedLCS.get(lcs_index));
                                }
                            }
                        }
                        if (!cluster.get(i).contains(im_trace))
                            cluster.get(i).add(im_trace);                        // im_trace가 중복으로 cluster에 입력되는 거 방지
                    }
                }
            } else {                                                                                                    // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                int best_message_type_num = 0;
                int lcs_length = 0;
                int current_message_type_num = 0;
                for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                    if (lcs == null)
                        continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                    current_message_type_num = LCSPatternAnalyzer(lcs);
                    if (best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                        lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                        lcs_length = lcs.size();                                                                          // 후자 선택
                        best_message_type_num = current_message_type_num;
                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                        if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                            lcs_index = generatedLCS.indexOf(lcs);
                            lcs_length = lcs.size();
                            best_message_type_num = current_message_type_num;
                        }
                    }
                }

                if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                    cluster.get(i).add(im_trace);
                    centroidLCS.set(i, generatedLCS.get(lcs_index));
//                    updatedCluster.set(i,1);
                    assignFlag = true;
                }
            }
        }
//
        if (!assignFlag) {                                                                                               // given IM이 어떠한 existing cluster에도 입력되지 않은 경우
            cluster.add(new ArrayList<>());                                                                             // 새로운 cluster와 centroidLCS slot를 생성한 후
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);                                                                // 해당 IM을 cluster에 삽입
            centroidLCS.set(centroidLCS.size() - 1, im_trace.getMsgSequence());
        }
    }

    public void addTraceCase5(InterplayModel im_trace, double simlr_threshold, double delay_threshold,
                              int lcs_min_len_threshold) {                                                                        // simlrThreshold: Similarity Threshold
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        Boolean assignFlag = false;
        int lcs_index;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

/*        Random random = new Random();
        Random random_index = new Random();

        if(random.nextFloat() < 0.1) {
            int rand_i = random_index.nextInt(cluster.size());
            if (cluster.get(rand_i).size() > 1) {
                RandomSplit(rand_i, delay_threshold);
            }
        }*/

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for (int i = 0; i < cluster.size(); i++) {
            generatedLCS.clear();
            lcs_index = -1;
            int best_message_type_num = 0;
            int lcs_length = 0;
            int current_message_type_num = 0;

            for (int j = 0; j < startingTime.size(); j++) {
                generatedLCS.add(LCSExtractorWithDelay(centroidLCS.get(i),  //cluster.get(i).get(0).getMsgSequence()    // Starting time에 따라 given IM을 slicing 하여
                        IMSlicer(startingTime.get(j), im_trace.getMsgSequence()), delay_threshold));                     // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
                if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
            }

            if (cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                for (int j = 0; j < startingTime.size(); j++) {                                                          // Starting time에 따라 slicing 된 given IM에 대해 생성된
                    if (generatedLCS.get(j) == null)
                        continue;                                                           // generated_LCS (lcs between centroid_lcs and given IM)
                    double temp = similarityChecker(centroidLCS.get(i), generatedLCS.get(j), delay_threshold);          // 와 기존 centroid_lcs와의 size를 비교하여 simlr_threshold
                    // 를 넘는지 확인함
                    if (temp >= simlr_threshold) {
                        assignFlag = true;
                        if (generatedLCS.get(j).size() > lcs_min_len_threshold) {
                            current_message_type_num = LCSPatternAnalyzer(generatedLCS.get(j));
                            if(best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                                lcs_index = j;                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                                lcs_length = generatedLCS.get(j).size();                                                                          // 후자 선택
                                best_message_type_num = current_message_type_num;
                            } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                                if (lcs_length < generatedLCS.get(j).size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                                    lcs_index = j;
                                    lcs_length = generatedLCS.get(j).size();
                                    best_message_type_num = current_message_type_num;
                                }
                            }
                        }
                        if(lcs_index != -1) centroidLCS.set(i, generatedLCS.get(lcs_index));
                        if (!cluster.get(i).contains(im_trace))
                            cluster.get(i).add(im_trace);                           // im_trace가 중복으로 cluster에 입력되는 거 방지
                        // simlr_threshold를 넘는 경우, centroidLCS를 업데이트
//                        assignFlag = true;
//                        if (generatedLCS.get(j).size() > lcs_min_len_threshold) {
//                            if (lcs_index == -1) {
//                                centroidLCS.set(i, generatedLCS.get(j));
//                                lcs_index = j;
//                            } else {
//                                if (generatedLCS.get(j).size() > generatedLCS.get(lcs_index).size()) {                         //길이가 더 긴 경우가 있다면 추가로 업데이트
//                                    centroidLCS.set(i, generatedLCS.get(lcs_index));
//                                }
//                            }
//                        }
//                        if (!cluster.get(i).contains(im_trace)) cluster.get(i).add(im_trace);
//                        // im_trace가 중복으로 cluster에 입력되는 거 방지
                    }
                }
            } else {                                                                                                    // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                    if (lcs == null)
                        continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                    current_message_type_num = LCSPatternAnalyzer(lcs);
                    if (best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                        lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                        lcs_length = lcs.size();                                                                          // 후자 선택
                        best_message_type_num = current_message_type_num;
                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                        if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                            lcs_index = generatedLCS.indexOf(lcs);
                            lcs_length = lcs.size();
                            best_message_type_num = current_message_type_num;
                        }
                    }
                }

                if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                    cluster.get(i).add(im_trace);
                    centroidLCS.set(i, generatedLCS.get(lcs_index));
                    assignFlag = true;
                }
            }
        }
//
        if (!assignFlag) {                                                                                               // given IM이 어떠한 existing cluster에도 입력되지 않은 경우
            cluster.add(new ArrayList<>());                                                                             // 새로운 cluster와 centroidLCS slot를 생성한 후
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);                                                                // 해당 IM을 cluster에 삽입
            centroidLCS.set(centroidLCS.size() - 1, im_trace.getMsgSequence());
        }
    }

    public void addTraceCase6(InterplayModel im_trace, double simlr_threshold, double delay_threshold,
                              int lcs_min_len_threshold) {                                                                        // simlrThreshold: Similarity Threshold
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        Boolean assignFlag = false;
        int lcs_index;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        // Given IM이 어떤 Cluster에 속하는지를 확인하는 과정: IM은 Failed tag를 가진다는 것을 가정함 / 여러 클러스터에 중복으로 할당 가능
        for (int i = 0; i < cluster.size(); i++) {
            generatedLCS.clear();
            lcs_index = -1;
            int best_message_type_num = 0;
            int lcs_length = 0;
            int current_message_type_num = 0;

            if (cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                for (int j = 0; j < startingTime.size(); j++) {
                    generatedLCS.add(LCSExtractorWithoutDelay(centroidLCS.get(i), //cluster.get(i).get(0).getMsgSequence()
                            IMSlicer(startingTime.get(j), im_trace.getMsgSequence())));
                    if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
                }

                for (int j = 0; j < startingTime.size(); j++) {                                                          // Starting time에 따라 slicing 된 given IM에 대해 생성된
                    if (generatedLCS.get(j) == null)
                        continue;                                                           // generated_LCS (lcs between centroid_lcs and given IM)
                    double temp = similarityChecker(centroidLCS.get(i), generatedLCS.get(j), delay_threshold);             // 와 기존 centroid_lcs와의 size를 비교하여 simlr_threshold
                    // 를 넘는지 확인함
                    if (temp >= simlr_threshold) {                                                                      // simlr_threshold를 넘는 경우, centroidLCS를 업데이트
//                        assignFlag = true;
//                        if (generatedLCS.get(j).size() > lcs_min_len_threshold) {
//                            current_message_type_num = LCSPatternAnalyzer(generatedLCS.get(j));
//                            if(best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
//                                lcs_index = j;                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
//                                lcs_length = generatedLCS.get(j).size();                                                                          // 후자 선택
//                                best_message_type_num = current_message_type_num;
//                            } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
//                                if (lcs_length < generatedLCS.get(j).size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
//                                    lcs_index = j;
//                                    lcs_length = generatedLCS.get(j).size();
//                                    best_message_type_num = current_message_type_num;
//                                }
//                            }
//                        }
//                        if(lcs_index != -1) centroidLCS.set(i, generatedLCS.get(lcs_index));
//                        if (!cluster.get(i).contains(im_trace))
//                            cluster.get(i).add(im_trace);                           // im_trace가 중복으로 cluster에 입력되는 거 방지
//                    }
                        assignFlag=true;
                        if (generatedLCS.get(j).size() > lcs_min_len_threshold) {
                            if (lcs_index == -1) {
                                centroidLCS.set(i, generatedLCS.get(j));
                                lcs_index = j;
                            } else {
                                if (generatedLCS.get(j).size() > generatedLCS.get(lcs_index).size()) {                         //길이가 더 긴 경우가 있다면 추가로 업데이트
                                    centroidLCS.set(i, generatedLCS.get(lcs_index));
                                }
                            }
                        }
                        if (!cluster.get(i).contains(im_trace))
                            cluster.get(i).add(im_trace);                           // im_trace가 중복으로 cluster에 입력되는 거 방지
                    }
                }
            } else {                                                                                                    // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                for (int j = 0; j < startingTime.size(); j++) {
                    generatedLCS.add(LCSExtractorWithDelay(IMSlicer(startingTime.get(j), im_trace.getMsgSequence()),                  // Starting time에 따라 given IM을 slicing 하여
                            centroidLCS.get(i), delay_threshold));                                                       // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
                    if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
                }

                for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                    if (lcs == null)
                        continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                    current_message_type_num = LCSPatternAnalyzer(lcs);
                    if(best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                        lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                        lcs_length = lcs.size();                                                                          // 후자 선택
                        best_message_type_num = current_message_type_num;
                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                        if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                            lcs_index = generatedLCS.indexOf(lcs);
                            lcs_length = lcs.size();
                            best_message_type_num = current_message_type_num;
                        }
                    }
                }

                if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                    cluster.get(i).add(im_trace);
                    centroidLCS.set(i, generatedLCS.get(lcs_index));
                    assignFlag = true;
                }
            }
        }

        if (!assignFlag) {                                                                                               // given IM이 어떠한 existing cluster에도 입력되지 않은 경우
            cluster.add(new ArrayList<>());                                                                             // 새로운 cluster와 centroidLCS slot를 생성한 후
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);                                                                // 해당 IM을 cluster에 삽입
            centroidLCS.set(centroidLCS.size() - 1, im_trace.getMsgSequence());
        }
    }

    public void addTraceBaseLCS(InterplayModel im_trace, double delay_threshold, int lcs_min_len_threshold) {
        ArrayList<Integer> updatedCluster = new ArrayList<>(Collections.nCopies(cluster.size(), 0));
        ArrayList<Message> generatedLCS;
        Boolean assignFlag = false;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        for (int i = 0; i < cluster.size(); i++) {
            generatedLCS = LCSExtractorWithoutDelayBase(centroidLCS.get(i), im_trace.getMsgSequence());      // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지
            if (generatedLCS != null && generatedLCS.size() > lcs_min_len_threshold) {                               // 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                cluster.get(i).add(im_trace);
                Collections.reverse(generatedLCS);
                centroidLCS.set(i, generatedLCS);
//                updatedCluster.set(i, 1);
                assignFlag = true;
            }
        }

        if (!assignFlag) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);
            return;
        }

        for (int i = 0; i < cluster.size(); i++) {
            LCSRedundancyAnalyzer(i, 5); // TODO Threshold: the number of repetition of the same sync messages threshold
        }
    }

    public void addTraceClusterNoise(InterplayModel im_trace, double delay_threshold, int lcs_min_len_threshold) {
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        HashMap<String,Double> MaxSim = new HashMap<>();
        ArrayList<ArrayList<Message>> MaxSimLCS = new ArrayList<>();
        Boolean assignFlag = false;
        int lcs_index;

        if (cluster.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        for (int i = 0; i < cluster.size(); i++) {
            generatedLCS.clear();
            lcs_index = -1;
            double simlrValue = -1;
            ArrayList<Message> maxLCS = new ArrayList<>();

            if (cluster.get(i).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                for (int j = 0; j < startingTime.size(); j++) {
                    generatedLCS.add(LCSExtractorWithoutDelay(centroidLCS.get(i), //cluster.get(i).get(0).getMsgSequence()
                            IMSlicer(startingTime.get(j), im_trace.getMsgSequence())));
                    if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
                }

                for (int j = 0; j < startingTime.size(); j++) {                                                         // Starting time에 따라 slicing 된 given IM에 대해 생성된
                    if (generatedLCS.get(j) == null)
                        continue;                                                                                       // generated_LCS (lcs between centroid_lcs and given IM)
                    double temp = similarityChecker(centroidLCS.get(i), generatedLCS.get(j), delay_threshold);          // 와 기존 centroid_lcs와의 size를 비교하여 simlr_threshold
                    if (temp > simlrValue) {
                        simlrValue = temp;
                        if(generatedLCS.get(j).size() > lcs_min_len_threshold) maxLCS = generatedLCS.get(j);
                        else maxLCS = centroidLCS.get(i);
                    }
                }
                MaxSim.put(Integer.toString(i),1-simlrValue);
                MaxSimLCS.add(maxLCS);
            } else {
                for (int j = 0; j < startingTime.size(); j++) {
                    generatedLCS.add(LCSExtractorWithDelay(IMSlicer(startingTime.get(j), im_trace.getMsgSequence()),                  // Starting time에 따라 given IM을 slicing 하여
                            centroidLCS.get(i), delay_threshold));                                                       // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
                    if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
                }

                int best_message_type_num = 0;
                int lcs_length = 0;
                int current_message_type_num = 0;
                for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                    if (lcs == null) continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                    current_message_type_num = LCSPatternAnalyzer(lcs);
                    if (best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                        lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                        lcs_length = lcs.size();                                                                          // 후자 선택
                        best_message_type_num = current_message_type_num;
                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                        if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                            lcs_index = generatedLCS.indexOf(lcs);
                            lcs_length = lcs.size();
                            best_message_type_num = current_message_type_num;
                        }
                    }
                }

                if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                    cluster.get(i).add(im_trace);
                    centroidLCS.set(i, generatedLCS.get(lcs_index));
                    MaxSim.put(Integer.toString(i),-1.0);                                                                                    // Min_Length를 넘으면 바로 삽입 진행.
                } else {
                    MaxSim.put(Integer.toString(i),1.0);                                                                                    // Min_Length를 넘지 못하는 경우에는 add하지 않음.
                }
            }
        }

        double createCost = 1/((double)cluster.size()+1);
//        boolean addList[] = new boolean[cluster.size()];
//        Arrays.fill(addList,false);

        ArrayList<Integer> selected = new ArrayList<>();
        for(int j = 0; j <MaxSim.size(); j++) {
            if(MaxSim.get(Integer.toString(j)) < createCost && MaxSim.get(Integer.toString(j))!= -1) selected.add(j);
        }

        if(selected.size() == 0) {
            cluster.add(new ArrayList<>());
            centroidLCS.add(new ArrayList<>());
            cluster.get(cluster.size() - 1).add(im_trace);
            centroidLCS.set(centroidLCS.size() - 1, im_trace.getMsgSequence());
        } else {
            for(int i = 0; i < selected.size(); i++) {
                int selectedIndex = selected.get(i);
                cluster.get(selectedIndex).add(im_trace);
                centroidLCS.set(selectedIndex, MaxSimLCS.get(selectedIndex));
            }
        }
    }

    private void RandomSplit(int index, double delay_threshold) {
        ArrayList<Double> simlr_value = new ArrayList<>();
        HashMap<Double, Integer> hashMap = new HashMap<>();

        for (int i = 0; i < cluster.get(index).size(); i++) {
            double simlr = similarityChecker(centroidLCS.get(index), cluster.get(index).get(i).getMsgSequence(), delay_threshold);
            simlr_value.add(simlr);
            hashMap.put(simlr, i);
        }

        Collections.sort(simlr_value);

        ArrayList<InterplayModel> ims = new ArrayList<>();
        for (int i = 0; i < cluster.get(index).size() / 2; i++) {
            ims.add(cluster.get(index).get(hashMap.get(simlr_value.get(i))));
        }

        ArrayList<Message> lcs = LCSExtractorWithDelay(ims.get(0).getMsgSequence(), ims.get(1).getMsgSequence(), delay_threshold);
        for (int i = 2; i < ims.size(); i++) {
            lcs = LCSExtractorWithDelay(lcs, ims.get(i).getMsgSequence(), delay_threshold);
        }

        cluster.add(ims);
        centroidLCS.add(lcs);

        for (int i = 0; i < ims.size(); i++) {
            cluster.get(index).remove(hashMap.get(simlr_value.get(i)));
        }

        ArrayList<Message> lcs2 = LCSExtractorWithDelay(cluster.get(index).get(0).getMsgSequence(), cluster.get(index).get(1).getMsgSequence(), delay_threshold);
        for (int i = 2; i < cluster.size(); i++) {
            lcs2 = LCSExtractorWithDelay(lcs2, cluster.get(index).get(i).getMsgSequence(), delay_threshold);
        }
        centroidLCS.set(index, lcs2);
    }

    private ArrayList<Message> IMSlicer(float starting_time, ArrayList<Message> IM_msg) {
        ArrayList<Message> ret = new ArrayList<>();

        for (int i = 0; i < IM_msg.size(); i++) {
            if (IM_msg.get(i).time >= starting_time) ret.add(IM_msg.get(i));
        }

        return (ArrayList) ret.clone();
    }

    private int LCSPatternAnalyzer(ArrayList<Message> lcs) {
        HashMap<String, Integer> LCS_analysis = new HashMap<>();
        String key_ = "";

        if (lcs.size() == 0) return 0;

        for (int i = 0; i < lcs.size() - 1; i++) {
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

        if (target_LCS != null && target_LCS.size() == 0) return;

        for (int i = 0; i < target_LCS.size() - 1; i++) {
            key_ = target_LCS.get(i).commandSent + "_" + target_LCS.get(i + 1).commandSent;
            if (LCS_analysis.containsKey(key_)) {
                LCS_analysis.put(key_, LCS_analysis.get(key_) + 1);

                if (LCS_analysis.get(key_) >= redundancy_threshold
                        && !redundancy_list.contains(target_LCS.get(i))) {
                    redundancy_list.add(target_LCS.get(i));
                }
            } else {
                LCS_analysis.put(key_, 1);
            }

            if (LCS_analysis.get(key_) < redundancy_threshold) {
                for (int j = 0; j < redundancy_list.size(); j++) {
                    if (compareMessage(target_LCS.get(i), redundancy_list.get(j))) {
                        flag = true;
                    }
                }
                if (!flag) result_LCS.add(target_LCS.get(i));
            }
        }

        // 위의 for문에 포함되지 않는 LCS의 맨 마지막 인자 추가
        if (!redundancy_list.contains(target_LCS.get(target_LCS.size() - 1)))
            result_LCS.add(target_LCS.get(target_LCS.size() - 1));
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
        for (int i = 0; i < cluster.size(); i++) {
            System.out.println("Cluster " + i + "=================");
            System.out.println("Representative LCS:");

            for (int j = 0; j < centroidLCS.get(i).size(); j++) {
                temp = centroidLCS.get(i).get(j);
                System.out.println(j + " " + temp.time + ": " + temp.commandSent + " from " + temp.senderPltId + " to " + temp.receiverId);
            }

            System.out.println("Clustered IMs: " + cluster.get(i).size());
//            for(int j = 0; j < cluster.get(i).size(); j++) {
//                System.out.println((j+1) + ": IM_" + cluster.get(i).get(j).getId());
//            }
        }
    }

    public void ClusterMerge(double simlr_threshold, double delay_threshold, int lcs_min_len_threshold) {
        double temp;
        ArrayList<Integer> merged = new ArrayList<>();
        ArrayList<Message> lcs_lcs;
        ArrayList<ArrayList<Message>> lcs_lcses = new ArrayList<>();

        for (int i = 0; i < cluster.size(); i++) {
            if (merged.contains(i)) continue;
            for (int j = i + 1; j < cluster.size(); j++) {
                lcs_lcses.clear();
                if (cluster.get(i).size() > 1 && cluster.get(j).size() > 1) {
                    lcs_lcs = LCSExtractorWithDelay(centroidLCS.get(i), centroidLCS.get(j), delay_threshold);
                    if (lcs_lcs == null) continue;
                    Collections.reverse(lcs_lcs);
                    temp = similarityChecker(centroidLCS.get(i), lcs_lcs, delay_threshold);
//                  temp = similarityChecker(centroidLCS.get(i), centroidLCS.get(j), delay_threshold); TODO Similarity: Cent vs Cent / Cent vs LCS_LCS ??
                    if (temp >= simlr_threshold) {
                        for (InterplayModel IM : cluster.get(j)) {
                            if (!cluster.get(i).contains(IM)) {
                                cluster.get(i).add(IM);
                            }
                        }
                        if(lcs_lcs.size() > lcs_min_len_threshold) centroidLCS.set(i, lcs_lcs);
                        if (!merged.contains(j)) merged.add(j);
                    }
                } else {
                    for (int k = 0; k < startingTime.size(); k++) {
                        lcs_lcses.add(LCSExtractorWithDelay(IMSlicer(startingTime.get(k), centroidLCS.get(i)),
                                centroidLCS.get(j), delay_threshold));
                        if (lcs_lcses.get(k) != null) Collections.reverse(lcs_lcses.get(k));
                    }
                    for (int k = 0; k < startingTime.size(); k++) {
                        if (lcs_lcses.get(k) != null && (lcs_lcses.get(k).size() > lcs_min_len_threshold)) {
                            for (InterplayModel IM : cluster.get(j)) {
                                if (!cluster.get(i).contains(IM)) {
                                    cluster.get(i).add(IM);
                                }
                            }
                            centroidLCS.set(i, lcs_lcses.get(k));
                            if (!merged.contains(j)) merged.add(j);
                            break;
                        }
                    }
                }
            }
        }
        Collections.sort(merged);
        int key = -1;
        for (int i = merged.size() - 1; i >= 0; i--) {
//          System.out.println(merged.get(i));
            cluster.remove((int) merged.get(i));
        }
        merged.clear();
    }

    public void ClusteringFinalize(double simlr_threshold, double delay_threshold, int lcs_min_len_threshold) {         // TODO Clustering Finalize Concurrent Modification Exception

        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        int lcs_index;

//        ClusterMerge(simlr_threshold, delay_threshold, lcs_min_len_threshold);

        for (int i = 0; i < cluster.size(); i++) {
            for (InterplayModel IM : cluster.get(i)) {
                for (int j = 0; j < cluster.size(); j++) {
                    int best_message_type_num = 0;
                    int lcs_length = 0;
                    int current_message_type_num = 0;
                    if (i == j) continue;
                    generatedLCS.clear();
                    lcs_index = -1;
                    for (int k = 0; k < startingTime.size(); k++) {
                        generatedLCS.add(LCSExtractorWithDelay(centroidLCS.get(j),
                                IMSlicer(startingTime.get(k), IM.getMsgSequence()), delay_threshold));
                        if (generatedLCS.get(k) != null) Collections.reverse(generatedLCS.get(k));
                    }

                    if (cluster.get(j).size() > 1) {                                                                             // Cluster에 2개 이상의 IM이 존재하는 경우, representative lcs 와 given IM 사이의 LCS를 생성하여 Similarity 비교
                        for (int k = 0; k < startingTime.size(); k++) {                                                          // Starting time에 따라 slicing 된 given IM에 대해 생성된
                            if (generatedLCS.get(k) == null)
                                continue;                                                           // generated_LCS (lcs between centroid_lcs and given IM)
                            double temp = similarityChecker(centroidLCS.get(j), generatedLCS.get(k), delay_threshold);          // 와 기존 centroid_lcs와의 size를 비교하여 simlr_threshold
                            // 를 넘는지 확인함
                            if (temp >= simlr_threshold) {                                                                      // simlr_threshold를 넘는 경우, centroidLCS를 업데이트
                                if (!cluster.get(j).contains(IM)) {
                                    cluster.get(j).add(IM);
                                    if(generatedLCS.get(k).size() > lcs_min_len_threshold) centroidLCS.set(j, generatedLCS.get(k));
                                    break;
                                }
//                                if (generatedLCS.get(k).size() > lcs_min_len_threshold) {
//                                    current_message_type_num = LCSPatternAnalyzer(generatedLCS.get(k));
//                                    if(best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
//                                        lcs_index = k;                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
//                                        lcs_length = generatedLCS.get(k).size();                                                                          // 후자 선택
//                                        best_message_type_num = current_message_type_num;
//                                    } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
//                                        if (lcs_length < generatedLCS.get(k).size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
//                                            lcs_index = k;
//                                            lcs_length = generatedLCS.get(k).size();
//                                            best_message_type_num = current_message_type_num;
//                                        }
//                                    }
//                                }
//                                if(lcs_index != -1) centroidLCS.set(j, generatedLCS.get(lcs_index));
//                                if (!cluster.get(j).contains(IM))
//                                    cluster.get(j).add(IM);
                            }
                        }
                    } else {                                                                                                    // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
                        for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
                            if (lcs == null)
                                continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
                            current_message_type_num = LCSPatternAnalyzer(lcs);
                            if (best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                                lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                                lcs_length = lcs.size();                                                                          // 후자 선택
                                best_message_type_num = current_message_type_num;
                            } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                                if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                                    lcs_index = generatedLCS.indexOf(lcs);
                                    lcs_length = lcs.size();
                                    best_message_type_num = current_message_type_num;
                                }
                            }
                        }

                        if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
                            if (!cluster.get(j).contains(IM)) {
                                cluster.get(j).add(IM);
                                centroidLCS.set(j, generatedLCS.get(lcs_index));
                            }
                        }
                    }
                }
            }
        }
    }

    private ArrayList<Message> LCSExtractorWithDelay(ArrayList<Message> data_point, ArrayList<Message> input_trace, double delay_threshold) {
        int[][] LCS = new int[data_point.size() + 1][input_trace.size() + 1];
        ArrayList<Message> ret_i = new ArrayList<>();
        ArrayList<Message> ret_j = new ArrayList<>();
        ArrayList<Message> ret = new ArrayList<>();

        // To save previous LCS point for comparing delay function.
        int prev_i = -1;
        int prev_j = -1;
        ArrayList<Pair> LCS_log = new ArrayList<>();

        // Generate LCS Table between two inputs
        for (int i = 0; i <= data_point.size(); i++) {
            for (int j = 0; j <= input_trace.size(); j++) {
                // For the convenience of the calculation, assign i=0 or j=0 as 0
                if (i == 0 || j == 0) {
                    LCS[i][j] = 0;
                    continue;
                }
//                System.out.println(data_point.get(i-1).commandSent);
//                System.out.println(compareMessage(data_point.get(i-1), input_trace.get(j-1)));
                // Same message case
                if (compareMessage(data_point.get(i - 1), input_trace.get(j - 1))) {
                    // Checking message delay difference between two given IM
                    if ((prev_i == -1 && prev_j == -1)
                            || calMessageDelay(data_point.get(prev_i - 1), data_point.get(i - 1), input_trace.get(prev_j - 1), input_trace.get(j - 1), delay_threshold)) {
                        LCS[i][j] = LCS[i - 1][j - 1] + 1;
                        if (prev_i != i) {
                            prev_i = i;
                            prev_j = j;
//                            LCS_log.add(new Pair(prev_i, prev_j));
                        }
                    } else { // Same message but different delay
                        LCS[i][j] = Math.max(LCS[i][j - 1], LCS[i - 1][j]);
                    }
                } else { // Different message case
                    LCS[i][j] = Math.max(LCS[i][j - 1], LCS[i - 1][j]);
                }
            }
        }

//        System.out.println(LCS[data_point.size()][input_trace.size()]);

        // No LCS exists
        if (LCS[data_point.size()][input_trace.size()] == 0) return null;
        else if (LCS[data_point.size()][input_trace.size()] < data_point.size()) { // Shorter LCS exists
            // Extract new LCS
            int current = 0;
            for (int i = 1; i <= data_point.size(); i++) {
                for (int j = 1; j <= input_trace.size(); j++) {
                    if (LCS[i][j] > current) {
                        current++;
                        ret_i.add(data_point.get(i - 1));
                        ret_j.add(input_trace.get(j - 1));
                    }

                }
            }
            // To remove commonly happened events in the beginning of the simulations
            // E.g. Split operation of platoons with size larger than the opt_size
            float time_i = -1;
            float time_j = -1;
            for (int i = ret_i.size() - 1, j = ret_j.size() - 1; i >= 0 && j >= 0; i--, j--) {
                time_i = Float.valueOf(ret_i.get(i).time);
                time_j = Float.valueOf(ret_j.get(j).time);
                if (time_i < 25.0 || time_j < 25.0) continue;
                else ret.add(ret_i.get(i));
            }
            return ret;
        } else { // No shorter LCS exists
            Collections.reverse(data_point);
            return (ArrayList)data_point.clone();
        }
    }

    private ArrayList<Message> LCSExtractorWithoutDelay(ArrayList<Message> data_point, ArrayList<Message> input_trace) {
        int[][] LCS = new int[data_point.size() + 1][input_trace.size() + 1];
        ArrayList<Message> ret_i = new ArrayList<>();
        ArrayList<Message> ret_j = new ArrayList<>();
        ArrayList<Message> ret = new ArrayList<>();

        // Generate LCS Table between two inputs
        for (int i = 0; i <= data_point.size(); i++) {
            for (int j = 0; j <= input_trace.size(); j++) {
                // For the convenience of the calculation, assign i=0 or j=0 as 0
                if (i == 0 || j == 0) {
                    LCS[i][j] = 0;
                    continue;
                }
//                System.out.println(data_point.get(i-1).commandSent);
//                System.out.println(compareMessage(data_point.get(i-1), input_trace.get(j-1)));
                // Same message case
                if (compareMessage(data_point.get(i - 1), input_trace.get(j - 1))) {                                         // TODO Delay Comparison?
                    LCS[i][j] = LCS[i - 1][j - 1] + 1;
                } else { // Different message case
                    LCS[i][j] = Math.max(LCS[i][j - 1], LCS[i - 1][j]);
                }
            }
        }

        // No LCS exists
        if (LCS[data_point.size()][input_trace.size()] == 0) return null;
        else if (LCS[data_point.size()][input_trace.size()] < data_point.size()) { // Shorter LCS exists
            // Extract new LCS
            int current = 0;
            for (int i = 1; i <= data_point.size(); i++) {
                for (int j = 1; j <= input_trace.size(); j++) {

                    if (LCS[i][j] > current) {
                        current++;
                        ret_i.add(data_point.get(i - 1));
                        ret_j.add(input_trace.get(j - 1));
                    }

                }
            }
            // To remove commonly happened events in the beginning of the simulations
            // E.g. Split operation of platoons with size larger than the opt_size
            float time_i = -1;
            float time_j = -1;
            for (int i = ret_i.size() - 1, j = ret_j.size() - 1; i >= 0 && j >= 0; i--, j--) {
                time_i = Float.valueOf(ret_i.get(i).time);
                time_j = Float.valueOf(ret_j.get(j).time);
                if (time_i < 25.0 || time_j < 25.0) continue;
                else ret.add(ret_i.get(i));
            }
            return ret;
        } else { // No shorter LCS exists
            Collections.reverse(data_point);
            return (ArrayList)data_point.clone();
        }
    }

    private ArrayList<Message> LCSExtractorWithoutDelayBase(ArrayList<Message> data_point, ArrayList<Message> input_trace) {
        int[][] LCS = new int[data_point.size() + 1][input_trace.size() + 1];
        ArrayList<Message> ret_i = new ArrayList<>();
        ArrayList<Message> ret_j = new ArrayList<>();
        ArrayList<Message> ret = new ArrayList<>();

        // Generate LCS Table between two inputs
        for (int i = 0; i <= data_point.size(); i++) {
            for (int j = 0; j <= input_trace.size(); j++) {
                // For the convenience of the calculation, assign i=0 or j=0 as 0
                if (i == 0 || j == 0) {
                    LCS[i][j] = 0;
                    continue;
                }
//                System.out.println(data_point.get(i-1).commandSent);
//                System.out.println(compareMessage(data_point.get(i-1), input_trace.get(j-1)));
                // Same message case
                if (compareMessageBase(data_point.get(i - 1), input_trace.get(j - 1))) {                                         // TODO Delay Comparison?
                    LCS[i][j] = LCS[i - 1][j - 1] + 1;
                } else { // Different message case
                    LCS[i][j] = Math.max(LCS[i][j - 1], LCS[i - 1][j]);
                }
            }
        }

        // No LCS exists
        if (LCS[data_point.size()][input_trace.size()] == 0) return null;
        else if (LCS[data_point.size()][input_trace.size()] < data_point.size()) { // Shorter LCS exists
            // Extract new LCS
            int current = 0;
            for (int i = 1; i <= data_point.size(); i++) {
                for (int j = 1; j <= input_trace.size(); j++) {

                    if (LCS[i][j] > current) {
                        current++;
                        ret_i.add(data_point.get(i - 1));
                        ret_j.add(input_trace.get(j - 1));
                    }

                }
            }
            // To remove commonly happened events in the beginning of the simulations
            // E.g. Split operation of platoons with size larger than the opt_size
            float time_i = -1;
            float time_j = -1;
            for (int i = ret_i.size() - 1, j = ret_j.size() - 1; i >= 0 && j >= 0; i--, j--) {
                time_i = Float.valueOf(ret_i.get(i).time);
                time_j = Float.valueOf(ret_j.get(j).time);
                if (time_i < 25.0 || time_j < 25.0) continue;
                else ret.add(ret_i.get(i));
            }
            return ret;
        } else { // No shorter LCS exists
            Collections.reverse(data_point);
            return (ArrayList)data_point.clone();
        }
    }

    private boolean compareMessage(Message m_a, Message m_b) {

        if (m_a.time < 25.00 || m_b.time < 25.00) return false;
        if (m_a.commandSent.equals(m_b.commandSent) && m_a.senderRole.equals(m_b.senderRole)
                && m_a.receiverRole.equals(m_b.receiverRole)) return true;

//        if(m_a.commandSent.equals(m_b.commandSent) && m_a.senderPltId.equals(m_b.senderPltId) // TODO How much information would be considered in comparison??
//                && m_a.receiverId.equals(m_b.receiverId)) return true;

        return false;
    }

    private boolean compareMessageBase(Message m_a, Message m_b) {

        if (m_a.time < 25.00 || m_b.time < 25.00) return false;
//        if(m_a.commandSent.equals(m_b.commandSent) && m_a.senderRole.equals(m_b.senderRole)
//                && m_a.receiverRole.equals(m_b.receiverRole)) return true;

        if (m_a.commandSent.equals(m_b.commandSent)) return true;

        return false;
    }

    private double similarityChecker(ArrayList<Message> lcs, ArrayList<Message> input_trace, double delay_threshold) {
        int matched = 0;
        int prevMatchedId = -1;
        int total = 0;

        if (lcs == null || input_trace == null) return 0;

        for (int i = 0; i < lcs.size(); i++) { // TODO 단일 IM 상에서 여러 개의 시작 지점 고려해야함
            if (matched == 0) {
                for (int j = 0; j < input_trace.size(); j++) {
                    if (compareMessage(lcs.get(i), input_trace.get(j)) == true) {
                        matched += 1;
                        prevMatchedId = j;
//                        System.out.println(i);
                        break;
                    }
                }
                total += 1;
            } else {
                for (int j = prevMatchedId + 1; j < input_trace.size(); j++) {
                    if (compareMessage(lcs.get(i), input_trace.get(j)) == true
                            && calMessageDelay(lcs, input_trace, i, j, prevMatchedId, delay_threshold) == true) {
                        matched += 1;
                        prevMatchedId = j;
//                        System.out.println(i);
                        break;
                    }
                }
                total += 1;
            }
        }

        return (double) matched / (double) total;
    }

    private boolean calMessageDelay(ArrayList<Message> lcs, ArrayList<Message> input_trace, int id_lcs, int id_trace, int prev_id_trace, double delay_threshold) {
        float lcs_delay = lcs.get(id_lcs - 1).time - lcs.get(id_lcs).time;
        float trace_delay = input_trace.get(prev_id_trace).time - input_trace.get(id_trace).time;

        if (Math.abs(lcs_delay - trace_delay) <= delay_threshold)
            return true; // TODO Message Delay Similarity Threshold 0.1 & Simlr 0.85 / 0.15 & Simlr 0.95 / 1.00 Simlr 1.0?
        else {
//            System.out.println("lcs_id: " + id_lcs + "/ id_trace: " + id_trace + " time: "+ input_trace.get(id_trace).time + "/ prev_id_trace " + prev_id_trace+ " time: "+ input_trace.get(prev_id_trace).time);
//            System.out.println(Math.abs(lcs_delay - trace_delay));
            return false;
        }
    }

    private boolean calMessageDelay(Message prev_i, Message curnt_i, Message prev_j, Message curnt_j, double delay_threshold) {
        float i_delay = curnt_i.time - prev_i.time;
        float j_delay = curnt_j.time - prev_j.time;

        if (Math.abs(i_delay - j_delay) <= delay_threshold) return true;
        else return false;
    }

    public ArrayList<Double> EvaluateF1P(ArrayList<ArrayList<String>> oracle, ArrayList<String> index) {
        ArrayList<Double> ret = new ArrayList<>();

        HashMap<String, ArrayList<Integer>> c_element_index = new HashMap<>();
        HashMap<String, ArrayList<Integer>> o_element_index = new HashMap<>();

        // Generate Element-wise hashmap of Oracle and the formed cluster
        for (String id : index) {
            ArrayList<Integer> c_index = new ArrayList<>();
            ArrayList<Integer> o_index = new ArrayList<>();

            for (ArrayList<String> cl : oracle) {
                if (cl.contains(id)) o_index.add(oracle.indexOf(cl));
            }
            for (ArrayList<InterplayModel> IMs : cluster) {
                for (InterplayModel IM : IMs) {
                    if (IM.getId().equals(id)) c_index.add(cluster.indexOf(IMs));
                }
            }
            o_element_index.put(id, o_index);
            c_element_index.put(id, c_index);
        }
/*
        for (String s : o_element_index.keySet())
            for (int i : o_element_index.get(s))
                System.out.println("key :" + s + " value: " + i);*/

        ArrayList<Double> bestMatches_cl = new ArrayList<Double>();
        ArrayList<Double> bestMatches_or = new ArrayList<Double>();
        double bestMatch = 0;
        double cl_cl_cntb = 0;
        double cl_or_cntb = 0;
        double matched_cntb = 0;
        double tempMatch = 0;

        // Formed cluster의 결과에서 단일 cluster를 기준으로 oracle에서 가장 Best한 match값을 가지는
        // oracle_cluster 와의 contribution sum을 해당 cluster의 match값으로 설정함. -> F_(C,O)
        for (ArrayList<InterplayModel> cl_cl : cluster) {
            for (ArrayList<String> cl_or : oracle) {
                // contribution (cnbt) 값은 단일 element에 대해 해당 element가 overlapping한 cluster 개수의 1/n으로 나타나는 값
                // 단일 element가 3개의 cluster에 중복으로 나타나면 해당 element의 cnbt 값은 0.3333
                cl_cl_cntb = sumContributionsIM(cl_cl, c_element_index);
                cl_or_cntb = sumContributions(cl_or, o_element_index);
                ArrayList<String> matched = matchedElements(cl_cl, cl_or);

                matched_cntb = sumContributions(matched, c_element_index);

                // Match 값 계산
                tempMatch = Math.pow(matched_cntb, 2) / (cl_cl_cntb * cl_or_cntb);
                if (tempMatch > bestMatch) bestMatch = tempMatch;
            }
            bestMatches_cl.add(Math.sqrt(bestMatch));
            bestMatch = 0;
        }
        double F_C_O = 0;
        for (double mat : bestMatches_cl) {
            F_C_O += mat;
        }
        F_C_O /= bestMatches_cl.size();
        F_C_O *= (1 / 0.7); // TODO # categories
        ret.add(F_C_O);
//        System.out.println(F_C_O);

        // 위와 같은 과정이지만 Oracle을 기준으로 -> F_(O,C)
        for (ArrayList<String> cl_or : oracle) {
            for (ArrayList<InterplayModel> cl_cl : cluster) {
                // contribution (cnbt) 값은 단일 element에 대해 해당 element가 overlapping한 cluster 개수의 1/n으로 나타나는 값
                // 단일 element가 3개의 cluster에 중복으로 나타나면 해당 element의 cnbt 값은 0.3333
                cl_cl_cntb = sumContributionsIM(cl_cl, c_element_index);
                cl_or_cntb = sumContributions(cl_or, o_element_index);
                ArrayList<String> matched = matchedElements(cl_cl, cl_or);

                matched_cntb = sumContributions(matched, o_element_index);

                // Match 값 계산
                tempMatch = Math.pow(matched_cntb, 2) / (cl_cl_cntb * cl_or_cntb);
                if (tempMatch > bestMatch) bestMatch = tempMatch;
            }
            bestMatches_or.add(Math.sqrt(bestMatch));
            bestMatch = 0;
        }
        double F_O_C = 0;
        for (double mat : bestMatches_or) {
            F_O_C += mat;
        }
        F_O_C /= bestMatches_or.size();
        F_O_C *= (1 / 0.7); // TODO # of categories
        ret.add(F_O_C);
//        System.out.println(F_O_C);

        // Harmonic mean calculation -> F1p
        ret.add(2 * F_C_O * F_O_C / (F_C_O + F_O_C));
        return ret;
    }

    /*public double EvaluateF1P(ArrayList<ArrayList<String>> oracle, ArrayList<ArrayList<String>> oracle2, ArrayList<String> index) {
        double ret = 0;

        HashMap<String, ArrayList<Integer>> c_element_index = new HashMap<>();
        HashMap<String, ArrayList<Integer>> o_element_index = new HashMap<>();

        // Generate Element-wise hashmap of Oracle and the formed cluster
        for(String id : index) {
            ArrayList<Integer> c_index = new ArrayList<>();
            ArrayList<Integer> o_index = new ArrayList<>();

            for(ArrayList<String> cl : oracle) {
                if(cl.contains(id)) o_index.add(oracle.indexOf(cl));
            }
            for(ArrayList<String> IMs : oracle2) {
                for(String IM: IMs) {
                    if(IM.equals(id)) c_index.add(cluster.indexOf(IMs));
                }
            }
            o_element_index.put(id, o_index);
            c_element_index.put(id, c_index);
        }

        for (String s : o_element_index.keySet())
            for (int i : o_element_index.get(s))
                System.out.println("key :" + s + " value: " + i);

        ArrayList<Double> bestMatches_cl = new ArrayList<Double>();
        ArrayList<Double> bestMatches_or = new ArrayList<Double>();
        double bestMatch = 0;
        double cl_cl_cntb = 0;
        double cl_or_cntb = 0;
        double matched_cntb = 0;
        double tempMatch = 0;

        // Formed cluster의 결과에서 단일 cluster를 기준으로 oracle에서 가장 Best한 match값을 가지는
        // oracle_cluster 와의 contribution sum을 해당 cluster의 match값으로 설정함. -> F_(C,O)
        for(ArrayList<String> cl_cl : oracle2) {
            for(ArrayList<String> cl_or: oracle) {
                // contribution (cnbt) 값은 단일 element에 대해 해당 element가 overlapping한 cluster 개수의 1/n으로 나타나는 값
                // 단일 element가 3개의 cluster에 중복으로 나타나면 해당 element의 cnbt 값은 0.3333
                cl_cl_cntb = sumContributions(cl_cl, c_element_index);
                cl_or_cntb = sumContributions(cl_or, o_element_index);
                ArrayList<String> matched = matchedElements2(cl_cl, cl_or);

                matched_cntb = sumContributions(matched, c_element_index);

                // Match 값 계산
                tempMatch = Math.pow(matched_cntb, 2) / (cl_cl_cntb * cl_or_cntb);
                if(tempMatch > bestMatch) bestMatch = tempMatch;
            }
            bestMatches_cl.add(Math.sqrt(bestMatch));
            bestMatch = 0;
        }
        double F_C_O = 0;
        for(double mat : bestMatches_cl) {
            F_C_O += mat;
        }
        F_C_O /= bestMatches_cl.size();
        F_C_O *= (1/0.7);
        System.out.println(F_C_O);

        // 위와 같은 과정이지만 Oracle을 기준으로 -> F_(O,C)
        for(ArrayList<String> cl_or: oracle) {
            for(ArrayList<String> cl_cl : oracle2) {
                // contribution (cnbt) 값은 단일 element에 대해 해당 element가 overlapping한 cluster 개수의 1/n으로 나타나는 값
                // 단일 element가 3개의 cluster에 중복으로 나타나면 해당 element의 cnbt 값은 0.3333
                cl_cl_cntb = sumContributions(cl_cl, c_element_index);
                cl_or_cntb = sumContributions(cl_or, o_element_index);
                ArrayList<String> matched = matchedElements2(cl_cl, cl_or);

                matched_cntb = sumContributions(matched, o_element_index);

                // Match 값 계산
                tempMatch = Math.pow(matched_cntb, 2) / (cl_cl_cntb * cl_or_cntb);
                if(tempMatch > bestMatch) bestMatch = tempMatch;
            }
            bestMatches_or.add(Math.sqrt(bestMatch));
            bestMatch = 0;
        }
        double F_O_C = 0;
        for(double mat : bestMatches_or) {
            F_O_C += mat;
        }
        F_O_C /= bestMatches_or.size();
        F_O_C *= (1/0.7);
        System.out.println(F_O_C);

        // Harmonic mean calculation -> F1p
        return (2*F_C_O*F_O_C / (F_C_O + F_O_C));
    }*/

    private ArrayList<String> matchedElements(ArrayList<InterplayModel> cl_cl, ArrayList<String> cl_or) {
        ArrayList<String> matched = new ArrayList<>();

        for (InterplayModel im : cl_cl) {
            for (String elem : cl_or) {
                if (im.getId().equals(elem)) matched.add(im.getId());
            }
        }

        return matched;
    }

/*    private ArrayList<String> matchedElements2(ArrayList<String> cl_cl, ArrayList<String> cl_or) {
        ArrayList<String> matched = new ArrayList<>();

        for(String im : cl_cl) {
            for(String elem : cl_or) {
                if(im.equals(elem)) matched.add(im);
            }
        }

        return matched;
    }*/

    private double sumContributions(ArrayList<String> elements, HashMap<String, ArrayList<Integer>> elem_clusters) {
        double ret = 0;

        for (String elem : elements) {
            double shares_elem = elem_clusters.get(elem).size();
            ret += (1 / shares_elem);
        }

        return ret;
    }

    private double sumContributionsIM(ArrayList<InterplayModel> elements, HashMap<String, ArrayList<Integer>> elem_clusters) {
        double ret = 0;

        for (InterplayModel elem : elements) {
            double shares_elem = elem_clusters.get(elem.getId()).size();
            ret += (1 / shares_elem);
        }

        return ret;
    }

    public double EvaluateClusteringResult(ArrayList<ArrayList<String>> oracle, ArrayList<String> index) {
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

        for (int i = 0; i < index.size(); i++) {                                                                         // Generate pair for indexes
            for (int j = i + 1; j < index.size(); j++) {
                cl_front = false;
                cl_back = false;
                cl_same = false;
                ol_front = false;
                ol_back = false;
                ol_same = false;

                for (ArrayList<InterplayModel> IMs : cluster) {                                                          // Checking whether the pair is in the
                    for (InterplayModel IM : IMs) {                                                                      // same cluster in the Clustering result
                        if (IM.getId().equals(index.get(i))) cl_front = true;
                        if (IM.getId().equals(index.get(j))) cl_back = true;
                        if (cl_front && cl_back) {
                            cl_same = true;
                            break;
                        }
                    }
                    if (cl_same) break;
                    cl_front = false;
                    cl_back = false;
                }

                for (ArrayList<String> ids : oracle) {                                                                    // Checking whether the pair is in the
                    for (String id : ids) {                                                                               // same cluster in the Oracle
                        if (id.equals(index.get(i))) ol_front = true;
                        if (id.equals(index.get(j))) ol_back = true;
                        if (ol_front && ol_back) {
                            ol_same = true;
                            break;
                        }
                    }
                    if (ol_same) break;
                    ol_front = false;
                    ol_back = false;
                }

                if (cl_same && ol_same) TP++;
                else if (cl_same) FP++;
                else if (ol_same) FN++;
                else TN++;
            }
        }

/*        cluster = (ArrayList)originCluster.clone();
        centroidLCS = (ArrayList)originCentroidLCS.clone();*/
        return Math.sqrt((TP / (TP + FP)) * (TP / (TP + FN)));                                                                  // Fowlkes-Mallows index
    }                                                                                                                   // https://gentlej90.tistory.com/64

    public void clusterClear() {
        cluster = new ArrayList<>();
        centroidLCS = new ArrayList<>();
        startingTime = new ArrayList<>();
        originCluster = new ArrayList<>();
        originCentroidLCS = new ArrayList<>();
        startingTime.add((float) 25.00);
        startingTime.add((float) 45.00);
        startingTime.add((float) 65.00);
        startingTime.add((float) 85.00);
    }

    public int clusterSize() {
        return cluster.size();
    }

    public void testOracleClusterGenerate(ArrayList<ArrayList<String>> oracle) {
        ArrayList<ArrayList<InterplayModel>> new_cluster = new ArrayList<>();
        for (ArrayList<String> cl : oracle) {
            ArrayList<InterplayModel> temp = new ArrayList<>();
            for (String elem : cl) {
                for (ArrayList<InterplayModel> IMs : cluster) {
                    for (InterplayModel im : IMs) {
                        if (im.getId().equals(elem)) {

                        }
                    }
                }
            }
            new_cluster.add(temp);
        }

        cluster = (ArrayList) new_cluster.clone();
    }

    public ArrayList<Double> patternSimilarityChecker(ArrayList<InterplayModel> PIMs, double delay_threshold) {
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        ArrayList<Double> ret = new ArrayList<>();
        double max = 0;

        for (int i = 0; i < centroidLCS.size(); i++) {
            if(i == 0 || i == 1 || i == 5 || i == 6) continue;
            for(int j = 0; j < PIMs.size(); j++) {
//                generatedLCS.clear();
//                for (int k = 0; k < startingTime.size(); k++) {
//                    generatedLCS.add(LCSExtractorWithoutDelay(centroidLCS.get(i), //cluster.get(i).get(0).getMsgSequence()
//                            IMSlicer(startingTime.get(k), PIMs.get(j).getMsgSequence())));
//                    if (generatedLCS.get(k) != null) Collections.reverse(generatedLCS.get(k));
//                }
                max = 0;
                for(int k = 0; k < startingTime.size(); k++) {
//                    if (generatedLCS.get(k) == null)
//                        continue;
//                    double temp = similarityChecker(centroidLCS.get(i), generatedLCS.get(k), delay_threshold);
                    double temp = similarityChecker(centroidLCS.get(i),IMSlicer(startingTime.get(k), PIMs.get(j).getMsgSequence()), delay_threshold);
                    if (temp > max) max = temp;
                }
                if(max > 0.8) {
                    System.out.println(i);
                }
                ret.add(max);
            }
        }
        return ret;
    }

    public void codeLocalizer(String base, String filepath) {
        File pltSource = new File(base + filepath);
        BufferedReader reader = null;
        ArrayList<String> source = new ArrayList<>();

        // Get the code of Platoon Operation Management Protocol
        try {
            reader = new BufferedReader(new FileReader(pltSource));
            String line;
            while ((line = reader.readLine()) != null) {
                source.add(line);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        try {
            File pltConfig = new File("./SoS_Extension/logs/generatedPatterns.txt");
            FileReader filereader = new FileReader(pltConfig);
            BufferedReader bufReader = new BufferedReader(filereader);
            String line = "";
            StringTokenizer stringTokenizer;
            ArrayList<Message> msgSequence = new ArrayList<>();
            centroidLCS.clear();

            while((line = bufReader.readLine()) != null) {
                stringTokenizer = new StringTokenizer(line, " ");
                Message msg = new Message();
                if(stringTokenizer.countTokens() == 3) {
                    if(msgSequence.size() != 0) {
                        centroidLCS.add((ArrayList)msgSequence.clone());
                    }
                    msgSequence = new ArrayList<>();
                }

                if(stringTokenizer.countTokens() != 7) continue;
                stringTokenizer.nextToken();
                stringTokenizer.nextToken();
                msg.commandSent = stringTokenizer.nextToken();
                msgSequence.add(msg);
            }
        } catch(Exception e){
            System.out.println(e);
        }

        File file2 = new File(base + "/SoS_Extension/results/" + "CodeScopeReductionRate_bestOption.csv");
        try {
            FileWriter writer = new FileWriter(file2);
            writer.write("Cluster/IM, Code Inspection Scope, Reduction Rate\n");
            int flag = 0; // 0 : normal, 1 : first time in log
            for(int i = 0; i < this.centroidLCS.size(); i++) {
                File file3 = new File(base + "/SoS_Extension/results/" + "SuspiciousOrders_" + i + ".csv");
                HashMap<Integer, Integer> patternScope = new HashMap<>();
                for(int j = 0; j < this.centroidLCS.get(i).size(); j++) {
                    String command = this.centroidLCS.get(i).get(j).commandSent;
                    setCodeInspectionScope(source, command, patternScope, flag);
                }
                String tempWriter = "";
//                tempWriter += "Cluster " + i + "," + patternScope.size() + "\n";
//                for(InterplayModel im : this.cluster.get(i)) {
//                    flag = 1;
//                    HashMap<Integer, Integer> logScope = new HashMap<>();
//                    for(Message m : im.msgSequence) {
//                        setCodeInspectionScope(source, m.commandSent, logScope, flag);
//                        if(flag==1) flag = 0;
//                    }
//                    tempWriter += im.id + "," + logScope.size() + "," + ((double)patternScope.size() / (double)logScope.size()) + "\n";
//                }
//                writer.write(tempWriter);
//                tempWriter = "";
                FileWriter writer2 = new FileWriter(file3);
                writer2.write("CodeLines, Overlaps\n");
                for(Integer key : patternScope.keySet()) {
                    tempWriter += key + "," + patternScope.get(key) + "\n";
                }
                writer2.write(tempWriter);
                writer2.close();
            }
            writer.close();
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public void codeLocalizerSBFL(String base, String filepath, ArrayList<InterplayModel> IMs, ArrayList<InterplayModel> PIMs) {
        File pltSource = new File(base + filepath);
        BufferedReader reader = null;
        ArrayList<String> source = new ArrayList<>();

        // Get the code of Platoon Operation Management Protocol
        try {
            reader = new BufferedReader(new FileReader(pltSource));
            String line;
            while ((line = reader.readLine()) != null) {
                source.add(line);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        HashMap<String, ArrayList<Integer>> codescope = new HashMap<>(); // (MessageCommand, Ranges of codes executed)
        HashMap<Integer, ArrayList<Integer>> SBFLTable = new HashMap<>(); // (Line number, ArrayList [passed, failed])
        // For the failed logs
        for(InterplayModel im : IMs) {
            for (Message message : im.getMsgSequence()) {
                if (!codescope.containsKey(message.commandSent)) { // When the message command is observed at the first time,
                    ArrayList<Integer> ranges = setCodeInspectionScope(source, message.commandSent, null, 0);
                    ArrayList<Integer> temp = new ArrayList<>();
                    for (int k = 0; k < ranges.size(); k += 2) {
                        for (int p = ranges.get(k); p <= ranges.get(k + 1); p++) {
                            temp.add(p);
                        }
                    }
                    codescope.put(message.commandSent, (ArrayList)temp.clone()); // Caching to the codescope map
                    for (int line : temp) {
                        SBFLTable.put(line, new ArrayList<Integer>(Collections.nCopies(2, 0))); // Generate initial SBFL table
                    }
                }
                ArrayList<Integer> temp = codescope.get(message.commandSent);
                for (int line : temp) {
                    ArrayList<Integer> pfcount = SBFLTable.get(line);
                    pfcount.set(1, pfcount.get(1)+1); // Increase the failed value
                }
            }
        }

        for (InterplayModel im: PIMs) {
            for (Message message : im.getMsgSequence()) {
                if (!codescope.containsKey(message.commandSent)) { // When the message command is observed at the first time,
                    ArrayList<Integer> ranges = setCodeInspectionScope(source, message.commandSent, null, 0);
                    ArrayList<Integer> temp = new ArrayList<>();
                    for (int k = 0; k < ranges.size(); k += 2) {
                        for (int p = ranges.get(k); p <= ranges.get(k + 1); p++) {
                            temp.add(p);
                        }
                    }
                    codescope.put(message.commandSent, (ArrayList)temp.clone()); // Caching to the codescope map
                    for (int line : temp) {
                        SBFLTable.put(line, new ArrayList<Integer>(Collections.nCopies(2, 0))); // Generate initial SBFL table
                    }
                }
                ArrayList<Integer> temp = codescope.get(message.commandSent);
                for (int line : temp) {
                    ArrayList<Integer> pfcount = SBFLTable.get(line);
                    pfcount.set(0, pfcount.get(0)+1); // Increase the passed value
                }
            }
        }
        File SBFLresult = new File(base + "/SBFLresults.csv");

        try {
            FileWriter writer2 = new FileWriter(SBFLresult);
            // TODO Suspicious Calculation Methods
            String log = "Line, Tarantula, Ochiai, OP2, Barinel, DStar\n";
            for (int line : SBFLTable.keySet()) {
                log += line + "," + Tarantula(PIMs.size(), IMs.size(), SBFLTable.get(line).get(0), SBFLTable.get(line).get(1)) + ","
                        + Ochiai(PIMs.size(), IMs.size(), SBFLTable.get(line).get(0), SBFLTable.get(line).get(1)) + ","
                        + OP2(PIMs.size(), IMs.size(), SBFLTable.get(line).get(0), SBFLTable.get(line).get(1)) + ","
                        + Barinel(PIMs.size(), IMs.size(), SBFLTable.get(line).get(0), SBFLTable.get(line).get(1)) + ","
                        + DStar(PIMs.size(), IMs.size(), SBFLTable.get(line).get(0), SBFLTable.get(line).get(1)) + "\n";
            }
            writer2.write(log);
            writer2.close();
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    private double Tarantula(int totalPassed, int totalFailed, int Passed, int Failed) {
        return ((double)Failed / (double)totalFailed) / (((double)Passed / (double)totalPassed ) + ((double)Failed / (double)totalFailed));
    }

    private double Ochiai(int totalPassed, int totalFailed, int Passed, int Failed) {
        return (double)Failed/ Math.sqrt((double)totalFailed * ((double)Failed + (double)Passed));
    }

    private double OP2(int totalPassed, int totalFailed, int Passed, int Failed) {
        return (double)Failed - ((double)Passed / ((double)totalPassed + 1));
    }

    private double Barinel(int totalPassed, int totalFailed, int Passed, int Failed) {
        return 1 - ((double)Passed / ((double)Passed + (double)Failed));
    }

    private double DStar(int totalPassed, int totalFailed, int Passed, int Failed) {
        return Math.pow((double)Failed,2) / ((double)Passed + (double)totalFailed - (double)Failed);
    }

    private ArrayList<Integer> setCodeInspectionScope(ArrayList<String> source, String command, HashMap<Integer, Integer> tempMap, int flag) {
        ArrayList<Integer> ranges = new ArrayList<>(); // Starting line, Finishing line, Starting line, Finishing line, ...
        if(command.equals("MERGE_REQ")) {
            ranges.add(727);
            ranges.add(745);
        } else if(command.equals("MERGE_DONE")) {
            ranges.add(746);
            ranges.add(799);
            ranges.add(808);
            ranges.add(877);
            ranges.add(1064);
            ranges.add(1083);
            ranges.add(1086);
            ranges.add(1109);
        } else if (command.equals("SPLIT_REQ")) {
            ranges.add(1116);
            ranges.add(1140);
        } else if (command.equals("GAP_CREATED")) {
            ranges.add(1143);
            ranges.add(1204);
            ranges.add(1485);
            ranges.add(1504);
            ranges.add(1507);
            ranges.add(1533);
        } else if (command.equals("VOTE_LEADER")) {
            ranges.add(1540);
            ranges.add(1554);
        } else if (command.equals("LEAVE_REQ")) {
            ranges.add(1642);
            ranges.add(1682);
        }
        if(flag == 1) {
            ranges.add(28);
            ranges.add(167);
            ranges.add(177);
            ranges.add(181);
            ranges.add(184);
            ranges.add(210);
            ranges.add(213);
            ranges.add(228);
            ranges.add(231);
            ranges.add(235);
            ranges.add(238);
            ranges.add(250);
            ranges.add(253);
            ranges.add(296);
            ranges.add(301);
            ranges.add(322);
            ranges.add(325);
            ranges.add(352);
            ranges.add(355);
            ranges.add(393);
            ranges.add(396);
            ranges.add(403);
            ranges.add(406);
            ranges.add(413);
//                ranges.add(421);
//                ranges.add(448);
//                ranges.add(452);
//                ranges.add(489);
//                ranges.add(492);
//                ranges.add(563);
            ranges.add(603);
            ranges.add(643);
        }

        for(int k = 0; k < source.size(); k++) {
            if(source.get(k).contains(command)) {
                if(source.get(k).contains("//")) continue;
                if(source.get(k).contains("if")) {
                    ranges.add(k+1);
                    int count = 0;
                    for(int p = k + 1; p < source.size(); p++) {
                        if (source.get(p).contains("{")) count += 1;
                        else if (source.get(p).contains("}")) {
                            if(--count == 0) {
                                ranges.add(p + 1);
                                break;
                            }
                        }
                    }
                } else {
                    for (int p = k; p > 0; p--) {
                        if (source.get(p).contains("{")) {
                            ranges.add(p);
                            break;
                        }
                    }
                    int count = 1;
                    for(int p = k + 1; p < source.size(); p++) {
                        if (source.get(p).contains("{")) count += 1;
                        else if (source.get(p).contains("}")) {
                            if(--count == 0) {
                                ranges.add(p + 1);
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (tempMap != null) {
            for (int k = 0; k < ranges.size(); k += 2) {
                for (int p = ranges.get(k); p <= ranges.get(k + 1); p++) {
                    if (tempMap.containsKey(p)) tempMap.put(p, tempMap.get(p) + 1);
                    else tempMap.put(p, 1);
                }
            }
        }
        return ranges;
    }

    public void PatternTxt(File folder) {
        File file = null;
        for(int i = 0; i < centroidLCS.size(); i++) {
            file = new File(folder.getPath() + "/" + i + ".txt");
            ArrayList<Message> pattern = centroidLCS.get(i);

            FileWriter writer2 = null;
            try {
                writer2 = new FileWriter(file);
                String tempStr = "";
                for(int j = 0; j < pattern.size(); j++) {
                    tempStr += pattern.get(j).time + "\t" + pattern.get(j).vehID + "\t" + "-\t-\t"
                            + pattern.get(j).commandSent + "\t" + pattern.get(j).receiverId + "\t" + pattern.get(j).senderPltId
                            + "\t" + pattern.get(j).receiverPltId + "\t" + "-";
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

    public double PatternIdentityChecker(double delay_threshold, ArrayList<ArrayList<String>> oracle) {
        double ret = 0;

        ArrayList<Integer> matched = new ArrayList<>();
        int max_len = -1;
        int matched_id = -1;
        for(int i = 0; i < centroidLCS.size(); i++) matched.add(-1);

        int id_p_list[] = {9, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8};
        int id_p_index = 0;
//        Collections.shuffle(id_patterns);
        for (InterplayModel id_pattern : id_patterns) {
            if(oracle.get(id_p_list[id_p_index]).size() == 0) continue;
            max_len = -1;
            matched_id = -1;
            for(int i = 0; i < centroidLCS.size(); i++) {
                if (matched.get(i) != 1) {
                  ArrayList<Message> lcs = LCSExtractorWithDelay(id_pattern.getMsgSequence(), centroidLCS.get(i), delay_threshold);
//                    ArrayList<Message> lcs = LCSExtractorWithoutDelay(id_pattern.getMsgSequence(), centroidLCS.get(i));
                    if (lcs == null) continue;
                    if (max_len < lcs.size()) {
                        matched_id = i;
                        max_len = lcs.size();
                    }
                    lcs.clear();
                }
            }
            if (matched_id != -1) matched.set(matched_id, 1);
            if (max_len != -1) ret += ((double)max_len / (double)id_pattern.getMsgSequence().size());
            id_p_index++;
        }

        return ret;
    }

    public double PatternIdentityCheckerWeight(double delay_threshold, ArrayList<ArrayList<String>> oracle) {
        double ret = 0;

        ArrayList<Integer> matched = new ArrayList<>();
        int max_len = -1;
        int matched_id = -1;
        ArrayList<Message> lcs = null;
        for(int i = 0; i < centroidLCS.size(); i++) matched.add(-1);
        int id_p_list[] = {9, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8};
        int id_p_index = 0;
//        Collections.shuffle(id_patterns);
        for (InterplayModel id_pattern : id_patterns) {
            if(oracle.get(id_p_list[id_p_index]).size() == 0) continue;
            max_len = -1;
            matched_id = -1;
            for(int i = 0; i < centroidLCS.size(); i++) {
                if (matched.get(i) != 1) {
                  lcs = LCSExtractorWithDelay(id_pattern.getMsgSequence(), centroidLCS.get(i), delay_threshold);
//                    lcs = LCSExtractorWithoutDelay(id_pattern.getMsgSequence(), centroidLCS.get(i));
                    if (lcs == null) continue;
                    if (max_len < lcs.size()) {
                        matched_id = i;
                        max_len = lcs.size();
                        for(Message msg : lcs) {
                            max_len += msg.weight;
                        }
                    }
                    lcs.clear();
                }
            }
            if (matched_id != -1) matched.set(matched_id, 1); // NO LCS generated at all
            if (max_len != -1) ret += ((double)max_len / (double)id_pattern.getMsgSequence().size());
            id_p_index++;
        }
        return ret;
    }

    public double SPADEPatternIdentityCheckerWeight(double delay_threshold, ArrayList<ArrayList<String>> oracle, String f_name) {
        double ret = 0;
        ArrayList<Message> sequence = new ArrayList<>();
        ArrayList<ArrayList<Message>> SPADE_lcs = new ArrayList<>();

        try {
            File spade_log = new File("./SoS_Extension/results/" + f_name);
            FileReader filereader = new FileReader(spade_log);
            BufferedReader bufReader = new BufferedReader(filereader);
            String line = "";
            int count = 0;

            while ((line = bufReader.readLine()) != null) {
                if (count % 2 != 0) continue;
                String messages[] = line.split("/");
                for (String message : messages) {
                    String items[] = message.split("-");
                    Message temp = new Message();
                    temp.commandSent = items[0];
                    temp.senderPltId = items[1];
                    temp.senderRole = items[1];
                    temp.receiverId = items[2];
                    temp.receiverRole = items[2];
                    temp.receiverPltId = items[2];
                    sequence.add(temp);
                }
                SPADE_lcs.add((ArrayList) sequence.clone());
                sequence.clear();
                count++;
            }
        } catch(Exception e) {
            System.out.println(e);
        }
        ArrayList<Integer> matched = new ArrayList<>();
        int max_len = -1;
        int matched_id = -1;
        ArrayList<Message> lcs = null;
        for(int i = 0; i < SPADE_lcs.size(); i++) matched.add(-1);
        int id_p_list[] = {9, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8};
        int id_p_index = 0;
//        Collections.shuffle(id_patterns);
        for (InterplayModel id_pattern : id_patterns) {
            if(oracle.get(id_p_list[id_p_index]).size() == 0) continue;
            max_len = -1;
            matched_id = -1;
            for(int i = 0; i < SPADE_lcs.size(); i++) {
                if (matched.get(i) != 1) {
                    lcs = LCSExtractorWithoutDelay(id_pattern.getMsgSequence(), SPADE_lcs.get(i));
                    if (lcs == null) continue;
                    if (max_len < lcs.size()) {
                        matched_id = i;
                        max_len = lcs.size();
                        for(Message msg : lcs) {
                            max_len += msg.weight;
                        }
                    }
                    lcs.clear();
                }
            }
            if (matched_id != -1) matched.set(matched_id, 1); // NO LCS generated at all
            if (max_len != -1) ret += ((double)max_len / (double)id_pattern.getMsgSequence().size());
            id_p_index++;
        }

        return ret;
    }

    public void SingleCasePatternMining(InterplayModel im_trace, double delay_threshold, double lcs_min_len_threshold) {
        ArrayList<ArrayList<Message>> generatedLCS = new ArrayList<>();
        int lcs_index;

        if (centroidLCS.size() == 0) {
            cluster.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.add(new ArrayList<>());
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        lcs_index = -1;
        int best_message_type_num = 0;
        int lcs_length = 0;
        int current_message_type_num = 0;
//            for (int j = 0; j < startingTime.size(); j++) {
//                    generatedLCS.add(LCSExtractorWithDelay(centroidLCS.get(0), //cluster.get(i).get(0).getMsgSequence()
//                            IMSlicer(startingTime.get(j), im_trace.getMsgSequence()), delay_threshold));
//                    if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
//                }
//
//                for (int j = 0; j < startingTime.size(); j++) {                                                          // Starting time에 따라 slicing 된 given IM에 대해 생성된
//                    if (generatedLCS.get(j) == null)
//                        continue;
//                    if (generatedLCS.get(j).size() > lcs_min_len_threshold) {
//                        if (lcs_index == -1) {
//                            centroidLCS.set(0, generatedLCS.get(j));
//                            lcs_index = j;
//                        } else {
//                            if (generatedLCS.get(j).size() > generatedLCS.get(lcs_index).size()) {                         //길이가 더 긴 경우가 있다면 추가로 업데이트
//                                centroidLCS.set(0, generatedLCS.get(lcs_index));
//                            }
//                        }
//                    }
//                }
                                // Cluster에 1개의 IM만 존재할때는 해당 IM 과의 LCS가 존재하는지 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
        for (int j = 0; j < startingTime.size(); j++) {
            generatedLCS.add(LCSExtractorWithDelay(IMSlicer(startingTime.get(j), im_trace.getMsgSequence()),                  // Starting time에 따라 given IM을 slicing 하여
                    centroidLCS.get(0), delay_threshold));                                                       // 중간에 중요 사건의 sequence가 시작하는 경우의 예외 처리 진행
            if (generatedLCS.get(j) != null) Collections.reverse(generatedLCS.get(j));
        }

        for (ArrayList<Message> lcs : generatedLCS) {                                                            // Starting time에 따른 IM_Sliced로 생성된 generated LCS
            if (lcs == null)
                continue;                                                                           // 중 가장 최적의 LCS를 선택하는 프로세스
            current_message_type_num = LCSPatternAnalyzer(lcs);
            if(best_message_type_num < current_message_type_num) {                                                // 1번 조건: LCS를 구성하는 Message type의 갯수
                lcs_index = generatedLCS.indexOf(lcs);                                                            // Ex) Merge_request로만 구성 vs Merge_request + Split_request
                lcs_length = lcs.size();                                                                          // 후자 선택
                best_message_type_num = current_message_type_num;
            } else if (best_message_type_num == current_message_type_num) {                                       // 2번 조건: LCS의 길이
                if (lcs_length < lcs.size()) {                                                                     // 같은 Message type의 갯수를 가지는 경우, LCS의 길이가 긴쪽 선택
                    lcs_index = generatedLCS.indexOf(lcs);
                    lcs_length = lcs.size();
                    best_message_type_num = current_message_type_num;
                }
            }
        }

        if (lcs_index != -1 && generatedLCS.get(lcs_index).size() > lcs_min_len_threshold) {                  // TODO Length Threshold
            centroidLCS.set(0, generatedLCS.get(lcs_index));
            cluster.get(0).add(im_trace);
        }
    }

    public double PatternIdentityCheckerSingleCase(double delay_threshold, ArrayList<ArrayList<String>> oracle, int oracle_index) {
        double ret = 0;

        ArrayList<Integer> matched = new ArrayList<>();
        int max_len = -1;
        int matched_id = -1;

        int id_p_list[] = {9, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8};
        int id_p_index = 0;
//        Collections.shuffle(id_patterns);
        for (InterplayModel id_pattern : id_patterns) {
            if(id_p_list[id_p_index] != oracle_index) {
                id_p_index++;
                continue;
            }
            ArrayList<Message> lcs = LCSExtractorWithDelay(id_pattern.getMsgSequence(), centroidLCS.get(0), delay_threshold);
            if (lcs == null) return 0;
            ret += ((double)lcs.size() / (double)id_pattern.getMsgSequence().size());
            id_p_index++;
        }

        return ret;
    }

    public double PatternIdentityCheckerWeightSingleCase(double delay_threshold, ArrayList<ArrayList<String>> oracle, int oracle_index) {
        double ret = 0;

        int id_p_list[] = {9, 10, 11, 0, 1, 2, 3, 4, 5, 6, 7, 8};
        int id_p_index = 0;
//        Collections.shuffle(id_patterns);
        for (InterplayModel id_pattern : id_patterns) {
            if(id_p_list[id_p_index] != oracle_index) {
                id_p_index++;
                continue;
            }
            ArrayList<Message> lcs = LCSExtractorWithDelay(id_pattern.getMsgSequence(), centroidLCS.get(0), delay_threshold);
            if (lcs == null) return 0;
            double len = (double)lcs.size();
            for(Message msg : lcs) {
                len += msg.weight;
            }
            ret += (len / (double)id_pattern.getMsgSequence().size());
            id_p_index++;
        }

        return ret;
    }

    public void SingleCasePatternMiningBase(InterplayModel im_trace, double delay_threshold, double lcs_min_len_threshold) {

        if (centroidLCS.size() == 0) {
            cluster.add(new ArrayList<>());
            cluster.get(0).add(im_trace);
            centroidLCS.add(new ArrayList<>());
            centroidLCS.set(0, im_trace.getMsgSequence());
            return;
        }

        ArrayList<Message> generatedLCS = LCSExtractorWithoutDelayBase(centroidLCS.get(0), im_trace.getMsgSequence());
        if (generatedLCS != null && generatedLCS.size() > lcs_min_len_threshold) {                               // 여부를 이용하여 해당 Cluster에 포함가능한지를 확인함
            cluster.get(0).add(im_trace);
            Collections.reverse(generatedLCS);
            centroidLCS.set(0, generatedLCS);
        }
    }
}
