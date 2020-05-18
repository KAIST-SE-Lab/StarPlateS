import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        boolean isSMBFL = false;
        boolean isBMBFL = false;
        boolean isIMBFL = false;
        boolean isClustering = false;
        boolean withSim = true;

        if (args.length == 0) {
            System.out.println("Usage: java main <switch> <withSim> [<file>]");
            System.out.println("Where: <switch> = -structure or -smbfl");
            System.out.println("                  -behavior or -bmbfl");
            System.out.println("                  -interplay or -imbfl");
            System.out.println("                  -clustering or -cl");
            System.out.println("                  -all");
            System.out.println("       <withSim> = -simon (default)");
            System.out.println("                   -simoff");
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
        else if (args[0].equals("-all")) {
            isSMBFL = true;
            isBMBFL = true;
            isIMBFL = true;
            isClustering = true;
        }

        if(args[1].equals("-simoff")) {
            withSim = false;
        }

        int numScenario = 150;                                          // TODO The number of scenarios generated
        int numRepeat = 1;                                              // TODO The number of repetition of the same scenario for statistical model checking

        StructureModelBasedFaultLocalization smbfl = new StructureModelBasedFaultLocalization();
//        BehaviorModelBasedFaultLocalization bmfl                      // TODO create new class
        InterplayModelBasedFaultLocalization imbfl = new InterplayModelBasedFaultLocalization();

        if(withSim) {
            // Generate Random Scenario with Scenario Generation Module
            ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
            scenarioGenerator.generateRandomScenario(numScenario);      // TODO Call the scenario generation module with the number of scenarios

            SimulationExecutor simulationExecutor = new SimulationExecutor();
            simulationExecutor.run(numScenario, numRepeat, isSMBFL, isBMBFL, isIMBFL, smbfl, imbfl); // TODO add more configuration params, eventDuration, etc
        }

        Clustering clustering = new Clustering();
        Verifier verifier = new Verifier();
        int[] thresholds = {80};                                        // TODO Threshold value for the Verirfication Property 1
        int[] thresholds2 = {4};                                        // TODO Threshold value for the VP2
        String base = System.getProperty("user.dir");
        System.out.println(System.getProperty("user.dir"));
        int matchingtxts = 0;
        String currentdir = base + "/SoS_Extension/logs/sample/";
        System.out.print("Current Working Directory : " + currentdir +"\n");
        File f = new File(currentdir);
        Boolean result;
        matchingtxts = 0;
        if(f.exists()){
            int numoffiles = f.listFiles().length + 300;
            System.out.println("and it has " + numoffiles + " files.");
            for (int i = 0; i < numoffiles; i++){
                String txtdir = currentdir + Integer.toString(i) + "_0plnData.txt";
                File temptxt = new File(txtdir);
                if(temptxt.exists()){
                    matchingtxts++;
                    for (int thshold : thresholds){
                        result = verifier.verifyLog(txtdir,"operationSuccessRate", thshold);
                        if(!result) {
                            InterplayModel interplayModel = new InterplayModel(i, 0);                        // TODO r_index = 0 로 설정해놓음
                            clustering.addTrace(interplayModel, 1);                                     // TODO Similarity Threshold = 100%
                        }
                    }
//                        for (int thshold2 : thresholds2){
////                           System.out.println("opreation Time" + thshold2);
//                            result = verifier.verifyLog(txtdir, nof, "operationTime", thshold2);
//                            smbfl.structureModelOverlapping(results, i, 0);
//                        }
                }
            }
        } else {
            System.out.println("There is no such directory");
        }
        System.out.println("There were " + matchingtxts + " platooning text files");
        //}
//
        if (isSMBFL) {                                                  // Structure Model-based Fault Localization
            ArrayList<EdgeInfo> edgeInfos = smbfl.SMcalculateSuspiciousness();
            StructureModel finalSM = new StructureModel();
            finalSM.collaborationGraph = smbfl.overlappedG;
//            finalSM.drawGraph();
            System.out.println(edgeInfos.size());
            File file2 = new File(System.getProperty("user.dir") + "/examples/platoon_SoS/results/SBFL_result.csv");
            FileWriter writer2 = null;

            for (EdgeInfo edgeInfo: edgeInfos) {
                System.out.println("name: "+edgeInfo.edge+",    pass: "+edgeInfo.pass+",    fail: "+edgeInfo.fail+",    tarantula: "+edgeInfo.tarantulaM+", ochiai: "+edgeInfo.ochiaiM + ", op2: "+edgeInfo.op2M + ",   barinel: "+edgeInfo.barinelM+", dstar: "+edgeInfo.dstarM);
                try {
                    writer2 = new FileWriter(file2, true);
                    writer2.write(edgeInfo.edge+ "," + edgeInfo.pass+ "," + edgeInfo.fail + "," + edgeInfo.tarantulaM + "," + edgeInfo.ochiaiM+ "," + edgeInfo.op2M+ "," + edgeInfo.barinelM+ "," + edgeInfo.dstarM+"\n");
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
            for (NodeInfo nodeInfo: smbfl.nodeInfos) {
                System.out.println("name: "+nodeInfo.node+",    pass: "+nodeInfo.pass+",    fail: "+nodeInfo.fail);
                try {
                    writer2 = new FileWriter(file2, true);
                    writer2.write(nodeInfo.node+ "," + nodeInfo.pass+ "," + nodeInfo.fail +"\n");
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
//
//        if (isBMBFL) {
//
//        }
//
        if (isIMBFL) {                                                 // Interaction Model-based Fault Localization
            imbfl.printSuspSequences();
        }

        if(isClustering) {
            clustering.printCluster();
        }
    }
}
