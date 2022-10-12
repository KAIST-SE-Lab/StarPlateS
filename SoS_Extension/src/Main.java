import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Main {

    public static void main(String[] args) {
        boolean isSMBFL = false;
        boolean isBMBFL = false;
        boolean isIMBFL = false;
        boolean isClustering = false;
        boolean withSim = true;
        boolean onlySim = false;
        boolean DistanceChecker = false;
        boolean CollisionChecker = false;
        boolean BasicVerifierProperty = false;

        if (args.length == 0) {
            System.out.println("Usage: java main <switch> <withSim> [<file>]");
            System.out.println("Where: <switch> = -structure or -smbfl");
            System.out.println("                  -behavior or -bmbfl");
            System.out.println("                  -interplay or -imbfl");
            System.out.println("                  -clustering or -cl");
            System.out.println("                  -distance or -dsch");
            System.out.println("                  -all");
            System.out.println("       <withSim> = -simon (default)");
            System.out.println("                   -simoff");
            System.out.println("                   -onlysim");
            System.exit(1);
        }

        if (args[0].equals("-structure") || args[0].equals("-smbfl")) {
            isSMBFL = true;
            BasicVerifierProperty = true;
        }
        else if (args[0].equals("-behavior") || args[0].equals("-bmbfl")) {
            isBMBFL = true;
            BasicVerifierProperty = true;
        }
        else if (args[0].equals("-interplay") || args[0].equals("-imbfl")) {
            isIMBFL = true;
            BasicVerifierProperty = true;
        }
        else if (args[0].equals("-clustering") || args[0].equals("-cl")) {
            isClustering = true;
            BasicVerifierProperty = true;
        }
        else if (args[0].equals("-distance") || args[0].equals("-dsch"))
            DistanceChecker = true;
        else if (args[0].equals("-collision") || args[0].equals("coll"))
            CollisionChecker = true;
        else if (args[0].equals("-all")) {
            isSMBFL = true;
            isBMBFL = true;
            isIMBFL = true;
            isClustering = true;
            DistanceChecker = true;
            BasicVerifierProperty = true;
        }

        if (args[1].equals("-simoff")) {
            withSim = false;
        } else if(args[1].equals("-onlysim")) {
            onlySim = true;
        }

        int numScenario = 1000;                                          // TODO The number of scenarios generated
        int numRepeat = 1;                                              // TODO The number of repetition of the same scenario for statistical model checking

        StructureModelBasedFaultLocalization smbfl = new StructureModelBasedFaultLocalization();
//        BehaviorModelBasedFaultLocalization bmfl                      // TODO create new class
        InterplayModelBasedFaultLocalization imbfl = new InterplayModelBasedFaultLocalization();

        if (withSim) {
            // Generate Random Scenario with Scenario Generation Module
//            ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
//            scenarioGenerator.generateRandomScenario(numScenario);      // TODO Call the scenario generation module with the number of scenarios

            SimulationExecutor simulationExecutor = new SimulationExecutor();
            simulationExecutor.run(numScenario, numRepeat, isSMBFL, isBMBFL, isIMBFL, smbfl, imbfl); // TODO add more configuration params, eventDuration, etc
        } else {
            Verifier verifier = new Verifier();
            int[] thresholds = {80};                                        // TODO Threshold value for the Verirfication Property 1
            int[] thresholds2 = {4};                                        // TODO Threshold value for the VP2
            String base = System.getProperty("user.dir");
            System.out.println(System.getProperty("user.dir"));
            String currentdir = base + "/SoS_Extension/logs_full/MCI/";
            System.out.print("Current Working Directory : " + currentdir + "\n");
            File f = new File(currentdir);
            Boolean result;

            ArrayList<InterplayModel> IMs = new ArrayList<>();
            ArrayList<InterplayModel> PIMs = new ArrayList<>(); // Passed IMs
            ArrayList<ArrayList<String>> oracle = new ArrayList();
            ArrayList<String> o_index = new ArrayList();

//            File[] folders = f.listFiles();
            int file_id = 0;
//            for (File folder : folders) {
//                ArrayList<String> temp_oracle = new ArrayList();
//                for (File target : folder.listFiles()) {
//                    if (target.toString().contains("$")) continue;
//                    try(FileInputStream fis = new FileInputStream(target)) {
//                        XSSFWorkbook workbook = new XSSFWorkbook(fis);
//
//                        XSSFSheet communication_sheet = workbook.getSheetAt(1);
//                        int num_rows = communication_sheet.getLastRowNum() + 1;
//                        ArrayList<Message> msgSequence = new ArrayList();
//                        for (int id = 2; id <num_rows; id++) {
//                            Row row = communication_sheet.getRow(id);
//
//                            float time = (float)row.getCell(0).getNumericCellValue();
//
////                            int[] msg_index_list = {1,2,4,5,7,8,10,11,13,14,16,17,19,20}; With Received
//                            int[] msg_index_list = {1,4,7,10,13,16,19}; // Without Received
//                            for (int index : msg_index_list) {
//                                Message msg = new Message();
//                                switch (index) {
////                                    case 1: // FF -> FF Broadcast
////                                        for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
////                                            msg.time = time;
////                                            msg.vehID = "FF";
////                                            msg.commandSent = "MemorySharing";
////                                            msg.receiverId = "FF-Broadcast";
////                                            msg.senderPltId = "FF";
////                                            msg.receiverPltId = "FF-Broadcast";
////                                            msg.senderRole = "FF";
////                                            msg.receiverRole = "FF-Broadcast";
////                                            msgSequence.add(msg);
////                                        }
////                                        break;
//
//                                    case 4: // FF -> Org
//                                        for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
//                                            msg.time = time;
//                                            msg.vehID = "FF";
//                                            msg.commandSent = "Nearest Hospital";
//                                            msg.receiverId = "Org";
//                                            msg.senderPltId = "FF";
//                                            msg.receiverPltId = "Org";
//                                            msg.senderRole = "FF";
//                                            msg.receiverRole = "Org";
//                                            msgSequence.add(msg);
//                                        }
//                                        break;
//
//                                    case 7: // Org -> FF
//                                        for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
//                                            msg.time = time;
//                                            msg.vehID = "Org";
//                                            msg.commandSent = "Nearest Hospital Info";
//                                            msg.receiverId = "FF";
//                                            msg.senderPltId = "Org";
//                                            msg.receiverPltId = "FF";
//                                            msg.senderRole = "Org";
//                                            msg.receiverRole = "FF";
//                                            msgSequence.add(msg);
//                                        }
//                                        break;
//
//                                    case 10: // Amb -> Org
//                                        if (folder.getPath().contains("coll1") || folder.getPath().contains("coll2") || folder.getPath().contains("coll3")) {
//                                            if ((int)row.getCell(index).getNumericCellValue() > 5) break;
//                                        }
//                                            for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
//                                            msg.time = time;
//                                            msg.vehID = "Amb";
//                                            msg.commandSent = "Free State Start";
//                                            msg.receiverId = "Org";
//                                            msg.senderPltId = "Amb";
//                                            msg.receiverPltId = "Org";
//                                            msg.senderRole = "Amb";
//                                            msg.receiverRole = "Org";
//                                            msgSequence.add(msg);
//                                        }
//                                        break;
//
//                                    case 13: // Org -> Amb
//                                        for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
//                                            msg.time = time;
//                                            msg.vehID = "Org";
//                                            msg.commandSent = "Move to Bridgehead";
//                                            msg.receiverId = "Amb";
//                                            msg.senderPltId = "Org";
//                                            msg.receiverPltId = "Amb";
//                                            msg.senderRole = "Org";
//                                            msg.receiverRole = "Amb";
//                                            msgSequence.add(msg);
//                                        }
//                                        break;
//
//                                    case 16: // Brg -> Org
//                                        for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
//                                            msg.time = time;
//                                            msg.vehID = "Brg";
//                                            msg.commandSent = "Patient Arrived";
//                                            msg.receiverId = "Org";
//                                            msg.senderPltId = "Brg";
//                                            msg.receiverPltId = "Org";
//                                            msg.senderRole = "Brg";
//                                            msg.receiverRole = "Org";
//                                            msgSequence.add(msg);
//                                        }
//                                        break;
//
//                                    case 19: // Org -> Brg
//                                        for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
//                                            msg.time = time;
//                                            msg.vehID = "Org";
//                                            msg.commandSent = "Not Defined";
//                                            msg.receiverId = "Brg";
//                                            msg.senderPltId = "Org";
//                                            msg.receiverPltId = "Brg";
//                                            msg.senderRole = "Org";
//                                            msg.receiverRole = "Brg";
//                                            msgSequence.add(msg);
//                                        }
//                                        break;
//                                }
//                            }
//                        }
//                        InterplayModel interplayModel = new InterplayModel(String.valueOf(file_id), msgSequence);
//
//                        if (folder.getPath().contains("coll")) {
//                            temp_oracle.add(String.valueOf(file_id));
//                            o_index.add(String.valueOf(file_id));
//                            IMs.add(interplayModel);
//                        } else {
//                            PIMs.add(interplayModel);
//                        }
//                        file_id++;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        System.out.println(e);
//                    }
//                }
//                if (temp_oracle.size() != 0) oracle.add(temp_oracle);
//            }

            file_id = 0;
            ArrayList<InterplayModel> id_patterns = new ArrayList();
            File folder = new File(System.getProperty("user.dir") + "/SoS_Extension/results/Ideal_MCI/");
            File files[] = folder.listFiles();
            for (File file : files) {
                if (file.toString().contains("$")) continue;
                try(FileInputStream fis = new FileInputStream(file)) {
                    XSSFWorkbook workbook = new XSSFWorkbook(fis);

                    XSSFSheet communication_sheet = workbook.getSheetAt(1);
                    int num_rows = communication_sheet.getLastRowNum() + 1;
                    ArrayList<Message> msgSequence = new ArrayList();
                    for (int id = 2; id <num_rows; id++) {
                        Row row = communication_sheet.getRow(id);

                        float time = (float)row.getCell(0).getNumericCellValue();

//                            int[] msg_index_list = {1,2,4,5,7,8,10,11,13,14,16,17,19,20}; With Received
                        int[] msg_index_list = {1,4,7,10,13,16,19}; // Without Received
                        for (int index : msg_index_list) {
                            Message msg = new Message();
                            switch (index) {
//                                    case 1: // FF -> FF Broadcast
//                                        for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
//                                            msg.time = time;
//                                            msg.vehID = "FF";
//                                            msg.commandSent = "MemorySharing";
//                                            msg.receiverId = "FF-Broadcast";
//                                            msg.senderPltId = "FF";
//                                            msg.receiverPltId = "FF-Broadcast";
//                                            msg.senderRole = "FF";
//                                            msg.receiverRole = "FF-Broadcast";
//                                            msgSequence.add(msg);
//                                        }
//                                        break;

                                case 4: // FF -> Org
                                    for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
                                        msg.time = time;
                                        msg.vehID = "FF";
                                        msg.commandSent = "Nearest Hospital";
                                        msg.receiverId = "Org";
                                        msg.senderPltId = "FF";
                                        msg.receiverPltId = "Org";
                                        msg.senderRole = "FF";
                                        msg.receiverRole = "Org";
                                        msgSequence.add(msg);
                                    }
                                    break;

                                case 7: // Org -> FF
                                    for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
                                        msg.time = time;
                                        msg.vehID = "Org";
                                        msg.commandSent = "Nearest Hospital Info";
                                        msg.receiverId = "FF";
                                        msg.senderPltId = "Org";
                                        msg.receiverPltId = "FF";
                                        msg.senderRole = "Org";
                                        msg.receiverRole = "FF";
                                        msgSequence.add(msg);
                                    }
                                    break;

                                case 10: // Amb -> Org
                                    for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
                                        msg.time = time;
                                        msg.vehID = "Amb";
                                        msg.commandSent = "Free State Start";
                                        msg.receiverId = "Org";
                                        msg.senderPltId = "Amb";
                                        msg.receiverPltId = "Org";
                                        msg.senderRole = "Amb";
                                        msg.receiverRole = "Org";
                                        msgSequence.add(msg);
                                    }
                                    break;

                                case 13: // Org -> Amb
                                    for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
                                        msg.time = time;
                                        msg.vehID = "Org";
                                        msg.commandSent = "Move to Bridgehead";
                                        msg.receiverId = "Amb";
                                        msg.senderPltId = "Org";
                                        msg.receiverPltId = "Amb";
                                        msg.senderRole = "Org";
                                        msg.receiverRole = "Amb";
                                        msgSequence.add(msg);
                                    }
                                    break;

                                case 16: // Brg -> Org
                                    for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
                                        msg.time = time;
                                        msg.vehID = "Brg";
                                        msg.commandSent = "Patient Arrived";
                                        msg.receiverId = "Org";
                                        msg.senderPltId = "Brg";
                                        msg.receiverPltId = "Org";
                                        msg.senderRole = "Brg";
                                        msg.receiverRole = "Org";
                                        msgSequence.add(msg);
                                    }
                                    break;

                                case 19: // Org -> Brg
                                    for (int i = 0; i < (int)row.getCell(index).getNumericCellValue(); i++) {
                                        msg.time = time;
                                        msg.vehID = "Org";
                                        msg.commandSent = "Not Defined";
                                        msg.receiverId = "Brg";
                                        msg.senderPltId = "Org";
                                        msg.receiverPltId = "Brg";
                                        msg.senderRole = "Org";
                                        msg.receiverRole = "Brg";
                                        msgSequence.add(msg);
                                    }
                                    break;
                            }
                        }
                    }
                    InterplayModel interplayModel = new InterplayModel(String.valueOf(file_id++), msgSequence);
                    id_patterns.add(interplayModel);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e);
                }
            }

            double simlr_threshold;
            double delay_threshold;
            int lcs_min_len_threshold;
            ArrayList<Double> f1p_ev_score;
            int number_of_clusters;

            double c_simlr = 0.6;
            double c_delay = 0.1;
            int c_len = 8;

            double m_simlr = 0.86;
            double m_delay = 1;
            int m_len = 9;

            boolean Localization = true;

            OracleGenerator oracleGenerator = new OracleGenerator(oracle, o_index);

            Clustering clustering = new Clustering();
            clustering.setId_patterns(id_patterns);
//            Collections.shuffle(IMs);
//            for (InterplayModel im : IMs) {
//                clustering.SingleCasePatternMining(im, 5.0, 15);
//            }
//            clustering.printMaxPattern();

            if (Localization) {
                // Whole log running
//                Clustering clustering = new Clustering();
//                clustering.codeLocalizerSBFL(base, "/src/nodes/vehicle/05_PlatoonMg.cc", IMs, PIMs, 0, 0);

                // Single case running
//                OracleGenerator oracleGenerator = new OracleGenerator();
//                oracleGenerator.oracleGeneration(IMs);
//                oracleGenerator.printOracle();
//                oracleGenerator.getOracleCSV();
//                ArrayList<ArrayList<String>> oracle = oracleGenerator.getOracle();

//                for(int j = 1; j < 2; j++) {
//                    for (int i = 0; i < oracle.size(); i++) {
//                        ArrayList<InterplayModel> IMs_batch = new ArrayList<>();
//                        for (InterplayModel im : IMs) {
//                            if (oracle.get(i).contains(im.getId())) IMs_batch.add(im);
//                        }
//                        Collections.shuffle(IMs_batch);
//                        clustering.codeLocalizerSBFL(base, "/src/nodes/vehicle/05_PlatoonMg.cc", IMs_batch, PIMs, i, j);
//                    }
//                }

                // Sequential Overlap-based Localization
//                clustering.codeLocalizer(base, "/src/nodes/vehicle/05_PlatoonMg.cc");
//                clustering.codeLocalizerMCISBFL(base + "/SoS_Extension/logs_full/LocalizationMCI/", base);
                clustering.codeLocalizerMCISeqOverlap(base, base + "/SoS_Extension/logs_full/LocalizationMCISeqOverlap/CodeCoverageBase.xlsx", base + "/SoS_Extension/results/MCI_Pattern/");
            } else {
                for (int k = 0; k < 30; k++) {
                    ArrayList<InterplayModel> IMs_batch = new ArrayList<>();
//
//                    File batch_im = new File(base + "/SoS_Extension/results/" + "F1P - HyperparameterAnalysis_Base_" + k + ".txt");
                    try {
//                        FileWriter writer = new FileWriter(batch_im, true);
//                        String ret = "";
                        Collections.shuffle(IMs);
                        for (int i = 0; i < 1000; i++) {
                            IMs_batch.add(IMs.get(i));
//                            ret += IMs.get(i).getId() + "\n";
                        }
//                        writer.write(ret);
//                        writer.close();
                    } catch (Exception e) {
                        System.out.println(e);
                    }

//                    OracleGenerator oracleGenerator = new OracleGenerator();
//                    oracleGenerator.oracleGeneration(IMs_batch);
//                oracleGenerator.oracleGeneration(IMs);
//                    oracleGenerator.printOracle();
//                    oracleGenerator.getOracleCSV();
//                    ArrayList<ArrayList<String>> oracle = oracleGenerator.getOracle();

                    boolean multiple_cases = false;
                    boolean single_run = true;

                    if (isClustering) {
                        if (!multiple_cases) {
                            // Single case run
                            if (!single_run) {
                                // TODO Which Case? -> File Name Change
                                String max_ret = "";
                                String max_ret_w = "";
//                    File file2 = new File(base + "/SoS_Extension/results/" + "F1P - Base HyperparameterAnalysis_withTime_03_19.csv");
                                for (int j = 0; j < 30; j++) {
                                    Collections.shuffle(IMs);
                                    for (int i = 0; i < oracle.size(); i++) {
                                        double max_PIT = 0;
                                        double max_PITW = 0;
                                        File file2 = new File(base + "/SoS_Extension/results/" + "Single Case Hyperparameter_" + j + "_" + i + ".csv");
                                        try {
                                            FileWriter writer = new FileWriter(file2, true);
                                            String ret = "";
                                            // The code for Hyperparameter optimization of clustering algorithm
//                                            Clustering clustering = new Clustering();
                                            for (int delay_counter = 10; delay_counter <= 100; delay_counter += 10) {
                                                delay_threshold = (double) delay_counter / 100;
                                                for (lcs_min_len_threshold = 2; lcs_min_len_threshold <= 10; lcs_min_len_threshold++) {

                                                    long startTime = System.currentTimeMillis();

//                                    Collections.shuffle(IMs); // TODO Random Sort
                                                    for (InterplayModel im : IMs) {
                                                        if (!oracle.get(i).contains(im.getId())) continue;
//                                                    clustering.SingleCasePatternMiningBase(im, delay_threshold, lcs_min_len_threshold);
                                                        clustering.SingleCasePatternMining(im, delay_threshold, lcs_min_len_threshold);
                                                    }

                                                    // Pattern Logging
//                                    File folder3 = new File(base + "/SoS_Extension/results/patterns/" + formatter.format(date) + "/" + simlr_threshold*100 + "_" + delay_threshold*100 + "_" + lcs_min_len_threshold);
//                                    folder3.mkdir();
//                                    clustering.PatternTxt(folder3);
                                                    long endTime = System.currentTimeMillis();
                                                    number_of_clusters = clustering.clusterSize();
                                                    // Oracle-based Evaluation Score
                                                    f1p_ev_score = clustering.EvaluateF1P(oracle, oracleGenerator.getIndex()); // 0: F_C_O, 1: F_O_C, 2: Evaluation Score
//                                evaluation_score = clustering.EvaluateClusteringResult(oracle, oracleGenerator.getIndex());
//                                                    double pattern_identity_score = clustering.PatternIdentityCheckerSingleCase(delay_threshold, oracle, i);
                                                    double pattern_identity_score_w = clustering.PatternIdentityCheckerWeightSingleCase(delay_threshold, oracle, i);
//                                                    if (max_PIT < pattern_identity_score)
//                                                        max_PIT = pattern_identity_score;
                                                    if (max_PITW < pattern_identity_score_w)
                                                        max_PITW = pattern_identity_score_w;
                                                    System.out.println(delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters + ", PITW value: " + pattern_identity_score_w + ", Time(ms): " + (endTime - startTime));
                                                    ret += delay_threshold + "," + lcs_min_len_threshold + "," + f1p_ev_score.get(2) + "," + f1p_ev_score.get(0) + "," + f1p_ev_score.get(1) + "," + number_of_clusters + "," + pattern_identity_score_w + ", Time(ms): ," + (endTime - startTime) + "\n";

                                                    clustering.clusterClear();
                                                }
                                            }
                                            writer.write(ret);
                                            writer.close();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        max_ret += max_PIT + ",";
                                        max_ret_w += max_PITW + ",";
                                    }
                                    max_ret += "\n";
                                    max_ret_w += "\n";
                                }
                                File file2 = new File(base + "/SoS_Extension/results/" + "Max PIT_PITW.csv");
                                try {
                                    FileWriter writer = new FileWriter(file2, true);
                                    writer.write(max_ret);
                                    writer.write("\n");
                                    writer.write(max_ret_w);
                                    writer.close();
                                } catch (Exception e) {
                                    System.out.println(e);
                                }
                            } else {
//                                Clustering clustering = new Clustering();
                                String writer_pattern = "";
//                                for(int j = 0; j < 30; j++) {
                                    for (int i = 0; i < 6; i++) {
                                        ArrayList<Double> pattern_identity_score_w = clustering.SPADEPatternIdentityCheckerWeight(0.1, oracle, "SPADE_" + i + "_0_53.txt");
//                                        double pattern_identity_score_w = clustering.LogLinerPatternIdentityCheckerWeight(0.1, oracle, "LogLiner_" + j + "_" + i + ".csv", i);
                                        String pattern_identity_score_w_print = String.valueOf(pattern_identity_score_w.get(pattern_identity_score_w.size() - 1));
                                for (int l = 0; l < pattern_identity_score_w.size() - 1; l++) {
                                    pattern_identity_score_w_print += "," + pattern_identity_score_w.get(l);
                                }
                                System.out.println(pattern_identity_score_w_print);
                                pattern_identity_score_w_print += "\n";
//                                    System.out.println(pattern_identity_score_w);
//                                        writer_pattern += String.valueOf(pattern_identity_score_w) + ",";
                                        File file2 = new File(base + "/SoS_Extension/results/" + "SPADE_PIT_PITW_0.csv");
                                        try {
                                            FileWriter writer = new FileWriter(file2, true);
                                            writer.write(pattern_identity_score_w_print);
//                                            writer.write(",");
                                            writer.close();
                                        } catch (Exception e) {
                                            System.out.println(e);
                                        }
//                                pattern_identity_score_w.clear();
                                    }
//                                    writer_pattern += "\n";
//                                }
//                                File file2 = new File(base + "/SoS_Extension/results/" + "LogLiner_PIT_PITW_0.csv");
//                                try {
//                                    FileWriter writer = new FileWriter(file2, true);
////                                    writer.write(String.valueOf(pattern_identity_score_w));
//                                    writer.write(writer_pattern);
//                                    writer.close();
//                                } catch (Exception e) {
//                                    System.out.println(e);
//                                }
                                break;
                            }
                        } else {
                            // Multiple cases run
                            if (!single_run) {
                                File file2 = new File(base + "/SoS_Extension/results/" + "F1P - HyperparameterAnalysis_MCI_Base_" + k + ".csv");  // TODO Which Case? -> File Name Change
                                try {
                                    FileWriter writer = new FileWriter(file2, true);
                                    String ret = "";
                                    // The code for Hyperparameter optimization of clustering algorithm
                                    for (int simlr_counter = 65; simlr_counter <= 99; simlr_counter++) {
                                        simlr_threshold = (double) simlr_counter / 100;
                                        for (int delay_counter = 50; delay_counter <= 100; delay_counter += 10) {
                                            delay_threshold = (double) delay_counter / 10;
                                            for (lcs_min_len_threshold = 5; lcs_min_len_threshold <= 15; lcs_min_len_threshold++) {

                                                long startTime = System.currentTimeMillis();

                                                for (InterplayModel im : IMs_batch) {
                                                    // 대조군 Clustering Algorithm
                                                   clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);

//                                                    clustering.addTraceCase6(im, simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                                    // For Merging&Finalizing Optimization
//                                    clustering.addTraceCase6(im, c_simlr, c_delay, c_len);
                                                }

                                                // Clustering Merge Optimization
//                                                clustering.ClusterMerge(simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                                // Clustering Finalize Optimization
//                                clustering.ClusterMerge(m_simlr, m_delay, m_len);
//                                                clustering.ClusteringFinalize(simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                                // Pattern Logging
//                                    File folder3 = new File(base + "/SoS_Extension/results/patterns/" + formatter.format(date) + "/" + simlr_threshold*100 + "_" + delay_threshold*100 + "_" + lcs_min_len_threshold);
//                                    folder3.mkdir();
//                                    clustering.PatternTxt(folder3);
                                                long endTime = System.currentTimeMillis();
                                                number_of_clusters = clustering.clusterSize();
                                                // Oracle-based Evaluation Score
                                                f1p_ev_score = clustering.EvaluateF1P(oracle, oracleGenerator.getIndex()); // 0: F_C_O, 1: F_O_C, 2: Evaluation Score
                                                ArrayList<Double> pattern_identity_score = clustering.PatternIdentityChecker(delay_threshold, oracle);
//                                                ArrayList<Double> pattern_identity_score_w = clustering.PatternIdentityCheckerWeight(delay_threshold, oracle);
                                                String pattern_identity_score_w_print = String.valueOf(pattern_identity_score.get(pattern_identity_score.size() - 1));
                                                double temp_val = 0;
                                                for (int l = 0; l < pattern_identity_score.size() - 1; l++) {
                                                    pattern_identity_score_w_print += "," + pattern_identity_score.get(l);
                                                    temp_val += pattern_identity_score.get(l);
                                                }
                                                pattern_identity_score_w_print += "," + String.valueOf(temp_val /pattern_identity_score.size());
                                                System.out.println(simlr_threshold + ", " + delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters + ", PIT value: " + pattern_identity_score_w_print + ", Time(ms): " + (endTime - startTime));
                                                ret += simlr_threshold + "," + delay_threshold + "," + lcs_min_len_threshold + "," + f1p_ev_score.get(2) + "," + f1p_ev_score.get(0) + "," + f1p_ev_score.get(1) + "," + number_of_clusters + "," + pattern_identity_score_w_print + "," + (endTime - startTime) + "\n";

                                                clustering.clusterClear();
                                            }
                                        }
                                    }
                                    writer.write(ret);
                                    writer.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } // Single run with an optimized Hyperparameter setting
                            else {
//                                Clustering clustering = new Clustering();
                                simlr_threshold = 0.6;
                                delay_threshold = 0.9;
                                lcs_min_len_threshold = 3;
                                long startTime = System.currentTimeMillis();
                                for (InterplayModel im : IMs_batch) {
//                        clustering.addTraceClusterNoise(im, delay_threshold, lcs_min_len_threshold);
                                    clustering.addTraceCase6(im, simlr_threshold, delay_threshold, lcs_min_len_threshold);
//                    clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);
                                }
                                clustering.ClusterMerge(simlr_threshold, delay_threshold, lcs_min_len_threshold);
                                clustering.ClusteringFinalize(simlr_threshold, delay_threshold, lcs_min_len_threshold);
                                long endTime = System.currentTimeMillis();
                                clustering.printMaxPattern();
                                f1p_ev_score = clustering.EvaluateF1P(oracle, oracleGenerator.getIndex());
                                number_of_clusters = clustering.clusterSize();
                                ArrayList<Double> pattern_identity_score = clustering.PatternIdentityChecker(delay_threshold, oracle);
//                                                ArrayList<Double> pattern_identity_score_w = clustering.PatternIdentityCheckerWeight(delay_threshold, oracle);
                                String pattern_identity_score_w_print = String.valueOf(pattern_identity_score.get(pattern_identity_score.size() - 1));
                                for (int l = 0; l < pattern_identity_score.size() - 1; l++) {
                                    pattern_identity_score_w_print += "," + pattern_identity_score.get(l);
                                }
                                System.out.println(simlr_threshold + ", " + delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters + ", PIT value: " + pattern_identity_score_w_print + ", Time(ms): " + (endTime - startTime));

//                    ArrayList<Double> simWithPassed;
//                    simWithPassed = clustering.patternSimilarityChecker(PIMs, delay_threshold);
//                    File file3 = new File(base + "/SoS_Extension/results/" + "PatternSimAnalysis_Case6-5.csv");
//                    String ret = "";
//                    try {
//                        FileWriter writer = new FileWriter(file3);
//                        for (int i = 0; i < simWithPassed.size(); i++) {
//                            ret += simWithPassed.get(i) + ",\n";
//                        }
//                        writer.write(ret);
//                        writer.close();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                                clustering.codeLocalizer(base, "/src/nodes/vehicle/05_PlatoonMg.cc");
                            }
                        }
                    }
                }
            }
        }
    }
}
