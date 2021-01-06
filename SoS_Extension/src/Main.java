import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        boolean isSMBFL = false;
        boolean isBMBFL = false;
        boolean isIMBFL = false;
        boolean isClustering = false;
        boolean withSim = true;
        boolean onlySim = false;
        boolean DistanceChecker = false;

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

        if (args[0].equals("-structure") || args[0].equals("-smbfl"))
            isSMBFL = true;
        else if (args[0].equals("-behavior") || args[0].equals("-bmbfl"))
            isBMBFL = true;
        else if (args[0].equals("-interplay") || args[0].equals("-imbfl"))
            isIMBFL = true;
        else if (args[0].equals("-clustering") || args[0].equals("-cl"))
            isClustering = true;
        else if (args[0].equals("-distance") || args[0].equals("-dsch"))
            DistanceChecker = true;
        else if (args[0].equals("-all")) {
            isSMBFL = true;
            isBMBFL = true;
            isIMBFL = true;
            isClustering = true;
            DistanceChecker = true;
        }

        if (args[1].equals("-simoff")) {
            withSim = false;
        } else if(args[1].equals("-onlysim")) {
            onlySim = true;
        }

        int numScenario = 2000;                                          // TODO The number of scenarios generated
        int numRepeat = 1;                                              // TODO The number of repetition of the same scenario for statistical model checking

        StructureModelBasedFaultLocalization smbfl = new StructureModelBasedFaultLocalization();
//        BehaviorModelBasedFaultLocalization bmfl                      // TODO create new class
        InterplayModelBasedFaultLocalization imbfl = new InterplayModelBasedFaultLocalization();

        if (withSim) {
            // Generate Random Scenario with Scenario Generation Module
            ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
            scenarioGenerator.generateRandomScenario(numScenario);      // TODO Call the scenario generation module with the number of scenarios

            SimulationExecutor simulationExecutor = new SimulationExecutor();
            simulationExecutor.run(numScenario, numRepeat, isSMBFL, isBMBFL, isIMBFL, smbfl, imbfl); // TODO add more configuration params, eventDuration, etc
        } else {
            Verifier verifier = new Verifier();
            int[] thresholds = {80};                                        // TODO Threshold value for the Verirfication Property 1
            int[] thresholds2 = {4};                                        // TODO Threshold value for the VP2
            String base = System.getProperty("user.dir");
            System.out.println(System.getProperty("user.dir"));
            int matchingtxts = 0;
            String currentdir = base + "/SoS_Extension/logs/";
            System.out.print("Current Working Directory : " + currentdir + "\n");
            File f = new File(currentdir);
            Boolean result;
            matchingtxts = 0;

            ArrayList<InterplayModel> IMs = new ArrayList<>();

            if (f.exists()) {
                int numoffiles = f.listFiles().length;
                System.out.println("and it has " + numoffiles + " files.");
                for (int i = 0; i < 1512; i++) {    // TODO the number of input log files
                    String txtdir = currentdir + i + "_0plnData.txt";
                    File temptxt = new File(txtdir);
                    if (temptxt.exists()) {
                        matchingtxts++;
                        for (int thshold : thresholds) {
                            result = verifier.verifyLog(txtdir, "operationSuccessRate", thshold);  // TODO operationSuccessRate or operationTime
                            if (!result) {
                                InterplayModel interplayModel = new InterplayModel(i, 0); // TODO r_index = 0 로 설정해놓음
                                IMs.add(interplayModel);
                                // Structure & Interplay model ".txt" file exporting part
                                /*
                                StructureModel structureModel = new StructureModel(i,0);
                                File exportTxt = new File(currentdir + Integer.toString(i) + "_S_I_Model.txt");
                                FileWriter writerExport = null;
                                try {
                                    writerExport = new FileWriter(exportTxt, true);
                                    writerExport.write(Integer.toString(i) + "\n");
                                    writerExport.write("Interplay\n");                                              // OpSuccessRate -> I / OpTime -> S
                                    writerExport.write("Structure Model\n");
                                    writerExport.write(structureModel.printGraphText());
                                    writerExport.write("Interplay Model\n");
                                    writerExport.write(interplayModel.printSequence());
                                } catch (IOException e) {
                                    System.out.println(e);
                                } finally {
                                    try {
                                        if (writerExport != null) writerExport.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }*/
                            }
                        }
//                        for (int thshold2 : thresholds2){
//                            result = verifier.verifyLog(txtdir, "operationTime", thshold2);
////                            smbfl.structureModelOverlapping(results, i, 0);
//                            if (!result) {
//                                InterplayModel interplayModel = new InterplayModel(i, 0);                       // TODO r_index = 0 로 설정해놓음
//                                StructureModel structureModel = new StructureModel(i,0);
////                            clustering.addTrace(interplayModel, simlr_threshold);                                  // TODO Similarity Threshold = 75%
//                                IMs.add(interplayModel);
//
//                                // Structure & Interplay model ".txt" file exporting part
//                                File exportTxt = new File(currentdir + Integer.toString(i+1512) + "_S_I_Model.txt");
//                                FileWriter writerExport = null;
//                                try {
//                                    writerExport = new FileWriter(exportTxt, true);
//                                    writerExport.write(Integer.toString(i) + "\n");
//                                    writerExport.write("Structure\n");                                              // OpSuccessRate -> I / OpTime -> S
//                                    writerExport.write("Structure Model\n");
//                                    writerExport.write(structureModel.printGraphText());
//                                    writerExport.write("Interplay Model\n");
//                                    writerExport.write(interplayModel.printSequence());
//                                } catch (IOException e) {
//                                    System.out.println(e);
//                                } finally {
//                                    try {
//                                        if (writerExport != null) writerExport.close();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }
//                        }
                    }
                }

            } else {
                System.out.println("There is no such directory");
            }
            System.out.println("There were " + matchingtxts + " platooning text files");

            // TODO Check the input parameter whether distance checker should be processed
            if(DistanceChecker) {
                if (f.exists()) {
                    int numoffiles = f.listFiles().length + 300;
                    System.out.println("and it has " + numoffiles + " files.");
                    for (int i = 0; i < numoffiles; i++) {
                        String txtdir_pltConfig = currentdir + Integer.toString(i) + "_0plnConfig.txt";
                        String txtdir_veh = currentdir + Integer.toString(i) + "_0vehicleData.txt";
                        File temptxt = new File(txtdir_pltConfig);
                        File temptxt2 = new File(txtdir_veh);
                        if (temptxt.exists() && temptxt2.exists()) {
                            matchingtxts++;
                            for (int thshold : thresholds) {
                                result = verifier.verifyLog(txtdir_pltConfig, txtdir_veh, "DistanceChecker", thshold); // TODO first txt file is for platoon data
                            }
                        }
                    }
                } else {
                    System.out.println("There is no such directory [" + f.getAbsolutePath()+ "]");
                }
            }
            if (isSMBFL) {                                                  // Structure Model-based Fault Localization
                ArrayList<EdgeInfo> edgeInfos = smbfl.SMcalculateSuspiciousness();
                StructureModel finalSM = new StructureModel();
                finalSM.collaborationGraph = smbfl.overlappedG;
//            finalSM.drawGraph();
                System.out.println(edgeInfos.size());
                File file2 = new File(System.getProperty("user.dir") + "/examples/platoon_SoS/results/SBFL_result.csv");
                FileWriter writer2 = null;

                for (EdgeInfo edgeInfo : edgeInfos) {
                    System.out.println("name: " + edgeInfo.edge + ",    pass: " + edgeInfo.pass + ",    fail: " + edgeInfo.fail + ",    tarantula: " + edgeInfo.tarantulaM + ", ochiai: " + edgeInfo.ochiaiM + ", op2: " + edgeInfo.op2M + ",   barinel: " + edgeInfo.barinelM + ", dstar: " + edgeInfo.dstarM);
                    try {
                        writer2 = new FileWriter(file2, true);
                        writer2.write(edgeInfo.edge + "," + edgeInfo.pass + "," + edgeInfo.fail + "," + edgeInfo.tarantulaM + "," + edgeInfo.ochiaiM + "," + edgeInfo.op2M + "," + edgeInfo.barinelM + "," + edgeInfo.dstarM + "\n");
                        writer2.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (writer2 != null) writer2.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                for (NodeInfo nodeInfo : smbfl.nodeInfos) {
                    System.out.println("name: " + nodeInfo.node + ",    pass: " + nodeInfo.pass + ",    fail: " + nodeInfo.fail);
                    try {
                        writer2 = new FileWriter(file2, true);
                        writer2.write(nodeInfo.node + "," + nodeInfo.pass + "," + nodeInfo.fail + "\n");
                        writer2.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (writer2 != null) writer2.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
//
//        if (isBMBFL) {
//
//        }
//
            if (isIMBFL) {                                                 // Interaction Model-based Fault Localization
                imbfl.printSuspSequences();
            }

            double simlr_threshold;
            double delay_threshold;
            int lcs_min_len_threshold;
            ArrayList<Double> f1p_ev_score;
            int number_of_clusters;
            boolean single = false;

            double c_simlr = 0.6;
            double c_delay = 0.1;
            int c_len = 8;

            double m_simlr = 0.86;
            double m_delay = 1;
            int m_len = 9;

            OracleGenerator oracleGenerator = new OracleGenerator();
            oracleGenerator.oracleGeneration(IMs);
            oracleGenerator.printOracle();
            ArrayList<ArrayList<String>> oracle = oracleGenerator.getOracle();

            if (isClustering && !single) {
                File file2 = new File(base + "/SoS_Extension/results/" + "F1P - 2-2) HyperparameterAnalysis_Case6.csv");  // TODO Which Case? -> File Name Change
                try {
                    FileWriter writer = new FileWriter(file2, true);
                    String ret = "";
                    // The code for Hyperparameter optimization of clustering algorithm
                    for (int simlr_counter = 60; simlr_counter <= 90; simlr_counter++) {
                        simlr_threshold = (double) simlr_counter / 100;
                        for (int delay_counter = 10; delay_counter <= 100; delay_counter += 10) {
                            delay_threshold = (double) delay_counter / 100;
                            for (lcs_min_len_threshold = 2; lcs_min_len_threshold <= 15; lcs_min_len_threshold++) {
                                Clustering clustering = new Clustering();

                                for (InterplayModel im : IMs) {
                                    // 대조군 Clustering Algorithm
//                                    clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);

                                    clustering.addTraceCase6(im, simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                    // For Merging&Finalizing Optimization
//                                    clustering.addTraceCase6(im, c_simlr, c_delay, c_len);
                                }

                                // Clustering Merge Optimization
                                clustering.ClusterMerge(simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                // Clustering Finalize Optimization
//                                clustering.ClusterMerge(m_simlr, m_delay, m_len);
                                clustering.ClusteringFinalize(simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                number_of_clusters = clustering.clusterSize();
                                // Oracle-based Evaluation Score
                                f1p_ev_score = clustering.EvaluateF1P(oracle, oracleGenerator.getIndex()); // 0: F_C_O, 1: F_O_C, 2: Evaluation Score
//                                evaluation_score = clustering.EvaluateClusteringResult(oracle, oracleGenerator.getIndex());
                                System.out.println(simlr_threshold + ", " + delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters);
                                ret += simlr_threshold + "," + delay_threshold + "," + lcs_min_len_threshold + "," + f1p_ev_score.get(2) + "," + f1p_ev_score.get(0) + "," + f1p_ev_score.get(1) + "," + number_of_clusters + "\n";
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
                Clustering clustering = new Clustering();
                simlr_threshold = 0.6;
                delay_threshold = 1;
                lcs_min_len_threshold = 9;

                for (InterplayModel im : IMs) {
                    clustering.addTraceCase5(im, simlr_threshold, delay_threshold, lcs_min_len_threshold);
//                clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);
                }
                clustering.printCluster();
                simlr_threshold = 0.6;
                delay_threshold = 0.4;
                lcs_min_len_threshold = 5;
                clustering.ClusteringFinalize(simlr_threshold, delay_threshold, lcs_min_len_threshold);
                f1p_ev_score = clustering.EvaluateF1P(oracle, oracleGenerator.getIndex());
                number_of_clusters = clustering.clusterSize();
                System.out.println(simlr_threshold + ", " + delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters);
            }
        }
    }
}
