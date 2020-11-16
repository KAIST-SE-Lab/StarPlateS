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
            System.out.println("                  -distancechecker or -dsch");
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
        else if (args[0].equals("-distancechecker") || args[0].equals("-dsch"))
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
            int matchingtxts = 0;
            String currentdir = base + "/SoS_Extension/logs/";
            System.out.print("Current Working Directory : " + currentdir + "\n");
            File f = new File(currentdir);
            Boolean result;
            matchingtxts = 0;

            ArrayList<InterplayModel> IMs = new ArrayList<>();
            ArrayList<StructureModel> SMs = new ArrayList<>();

            if (f.exists()) {
                int numoffiles = f.listFiles().length + 300;
                System.out.println("and it has " + numoffiles + " files.");
                for (int i = 0; i < numoffiles; i++) {
                    String txtdir = currentdir + Integer.toString(i) + "_0plnData.txt";
                    File temptxt = new File(txtdir);
                    if (temptxt.exists()) {
                        matchingtxts++;
                        for (int thshold : thresholds) {
                            result = verifier.verifyLog(txtdir, "operationSuccessRate", thshold);
                            if (!result) {
                                InterplayModel interplayModel = new InterplayModel(i, 0);                       // TODO r_index = 0 로 설정해놓음
                                StructureModel structureModel = new StructureModel(i,0);
//                            clustering.addTrace(interplayModel, simlr_threshold);                                  // TODO Similarity Threshold = 75%
                                IMs.add(interplayModel);

                                // Structure & Interplay model ".txt" file exporting part
                                File exportTxt = new File(currentdir + Integer.toString(i) + "_S_I_Model.txt");
                                FileWriter writerExport = null;
                                try {
                                    writerExport = new FileWriter(exportTxt, true);
                                    writerExport.write(Integer.toString(i) + "\n");
                                    writerExport.write("Interplay\n");
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
                                }
                            }
                        }
//                        for (int thshold2 : thresholds2){
//                            result = verifier.verifyLog(txtdir, "operationTime", thshold2);
////                            smbfl.structureModelOverlapping(results, i, 0);
//                        }
                    }
                }

            } else {
                System.out.println("There is no such directory");
            }
            System.out.println("There were " + matchingtxts + " platooning text files");
            //}

            // TODO Check the input parameter whether distance checker should be processed
            if(DistanceChecker) {
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
                ArrayList<StructureModel> SMs = new ArrayList<>();

                if (f.exists()) {
                    int numoffiles = f.listFiles().length + 300;
                    System.out.println("and it has " + numoffiles + " files.");
                    for (int i = 0; i < numoffiles; i++) {
                        String txtdir_pltConfig = currentdir + Integer.toString(i) + "_0plnConfig.txt";
                        String txtdir_veh = currentdir + Integer.toString(i) + "_0vehicleData.txt";
                        File temptxt = new File(txtdir_pltConfig);
                        File temptxt2 = new File(txtdir_veh);
                        if (temptxt.exists() && temptxt2.exist()) {
                            matchingtxts++;
                            for (int thshold : thresholds) {
                                result = verifier.verifyLog(txtdir_pltConfig, txtdir_veh, "DistanceChecker", thshold); // TODO first txt file is for platoon data 
                                                                                                                       //and second txt file is for vehicle data
                                if (!result) {
                                    InterplayModel interplayModel = new InterplayModel(i, 0);                       // TODO r_index = 0 로 설정해놓음
                                    StructureModel structureModel = new StructureModel(i,0);
    //                            clustering.addTrace(interplayModel, simlr_threshold);                                  // TODO Similarity Threshold = 75%
                                    IMs.add(interplayModel);

                                    // Structure & Interplay model ".txt" file exporting part
                                    File exportTxt = new File(currentdir + Integer.toString(i) + "_S_I_Model.txt");
                                    FileWriter writerExport = null;
                                    try {
                                        writerExport = new FileWriter(exportTxt, true);
                                        writerExport.write(Integer.toString(i) + "\n");
                                        writerExport.write("Interplay\n");
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
                                    }
                                }
                            }
                        }
                    }

                } else {
                    System.out.println("There is no such directory");
                }
            }
//
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

            ArrayList<ArrayList<String>> oracle = new ArrayList<>();
            oracle.add(new ArrayList<>(Arrays.asList("6_0", "7_0", "8_0", "9_0", "11_0", "13_0", "41_0", "47_0")));
            oracle.add(new ArrayList<>(Arrays.asList("3_0", "6_0", "12_0", "46_0")));
            oracle.add(new ArrayList<>(Arrays.asList("17_0", "30_0", "45_0", "49_0")));
            oracle.add(new ArrayList<>(Arrays.asList("24_0", "29_0", "38_0", "46_0")));
            oracle.add(new ArrayList<>(Arrays.asList("24_0", "27_0", "29_0", "34_0", "38_0", "47_0")));
            oracle.add(new ArrayList<>(Arrays.asList("43_0")));
            oracle.add(new ArrayList<>(Arrays.asList("22_0")));

            double simlr_threshold;
            double delay_threshold;
            int lcs_min_len_threshold;
            double evaluation_score;
            boolean single = false;

            if (isClustering && !single) {
                File file2 = new File(base + "/SoS_Extension/" + "HyperparameterAnalysis.csv");

                try {
                    FileWriter writer = new FileWriter(file2, true);
                    // The code for Finalize Function Hyperparameter testing
//                    Clustering clustering = new Clustering();
//                    for (InterplayModel im : IMs) {
////                                clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);
//                        clustering.addTraceCase5(im, 0.66, 0.7, 18);                // TODO The best option so far
//                    }

                    for (int simlr_counter = 50; simlr_counter <= 100; simlr_counter++) {
                        simlr_threshold = (double) simlr_counter / 100;
                        for (int delay_counter = 10; delay_counter <= 100; delay_counter += 10) {
                            delay_threshold = (double) delay_counter / 100;
                            for (lcs_min_len_threshold = 5; lcs_min_len_threshold <= 25; lcs_min_len_threshold++) {
                            Clustering clustering = new Clustering();

                            for (InterplayModel im : IMs) {
//                                clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);
                                clustering.addTraceCase5(im, simlr_threshold, delay_threshold, lcs_min_len_threshold);
                            }
                                clustering.ClusteringFinalize(simlr_threshold, delay_threshold, lcs_min_len_threshold);
                                evaluation_score = clustering.EvaluateClusteringResult(oracle);
                                System.out.println(simlr_threshold + ", " + delay_threshold + "," + lcs_min_len_threshold + "," + "Clustering Evaluation Score: " + evaluation_score);
                                writer.write(simlr_threshold + "," + delay_threshold + "," + lcs_min_len_threshold + "," + evaluation_score);
                                writer.write("\n");
//                            clustering.printCluster();
//                            clustering.clusterClear();
                            }
                        }
                    }
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Clustering clustering = new Clustering();
                simlr_threshold = 0.78;
                delay_threshold = 0.8;
                lcs_min_len_threshold = 19;

                for (InterplayModel im : IMs) {
                    clustering.addTraceCase5(im, simlr_threshold, delay_threshold, lcs_min_len_threshold);
//                clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);
                }
                clustering.printCluster();
                simlr_threshold = 0.75;
                delay_threshold = 0.8;
                lcs_min_len_threshold = 15;
                clustering.ClusteringFinalize(simlr_threshold, delay_threshold, lcs_min_len_threshold);
                evaluation_score = clustering.EvaluateClusteringResult(oracle);
                System.out.println("Clustering Evaluation Score: " + evaluation_score);
                clustering.printCluster();
                clustering.clusterClear();
            }
        }
    }
}
