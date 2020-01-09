import java.io.File;

public class Main {

    public static void main(String[] args) {
//        boolean isSMBFL = false;
//        boolean isBMBFL = false;
//        boolean isIMBFL = true;
//        if (args.length == 0) {
//            System.out.println("Usage: java main <switch> [<file>]");
//            System.out.println("Where: <switch> = -structure or -smbfl");
//            System.out.println("                  -behavior or -bmbfl");
//            System.out.println("                  -interplay or -imbfl");
//            System.out.println("                  -all");
//            System.exit(1);
//        }
//        if (args[0].equals("-structure") || args[0].equals("-smbfl"))
//            isSMBFL = true;
//        else if (args[0].equals("-behavior") || args[0].equals("-bmbfl"))
//            isBMBFL = true;
//        else if (args[0].equals("-interplay") || args[0].equals("-imbfl"))
//            isIMBFL = true;
//        else if (args[0].equals("-all")) {
//            isSMBFL = true;
//            isBMBFL = true;
//            isIMBFL = true;
//        }
//
//        int numScenario = 1;
//        int numRepeat = 1;

	    // Generate Random Scenario
//	    ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
//        scenarioGenerator.generateRandomScenario(50);

//        StructureModelBasedFaultLocalization smbfl = new StructureModelBasedFaultLocalization();
////        BehaviorModelBasedFaultLocalization bmfl TODO create new class
//        InterplayModelBasedFaultLocalization imbfl = new InterplayModelBasedFaultLocalization();

//        SimulationExecutor simulationExecutor = new SimulationExecutor();
//        simulationExecutor.run(numScenario, numRepeat, isSMBFL, isBMBFL, isIMBFL, smbfl, imbfl); // TODO add more configuration params, eventDuration, etc

        Verifier verifier = new Verifier();
//        verifier.verifyLog(3,0,"operationTime", 5);
        String[] nofs = {"1", "2", "3", "4", "5", "tau1", "tau2", "tau3"};
        int[] thresholds = {10, 50, 70, 80, 100, 150, 300, 500, 1000, 5000, 10000};
        String base = System.getProperty("user.dir");
        int matchingtxts = 0;
        for (String nof : nofs) {
            String currentdir = base + "/StarPlateS/SoS_Extension/" + nof + "/";
            System.out.print("Current Working Directory : " + currentdir);
            File f = new File(currentdir);
            matchingtxts = 0;
            if(f.exists()){
                int numoffiles = f.listFiles().length;
                System.out.println("and it has " + numoffiles + " files.");
                for (int i = 0; i < numoffiles; i++){
                    String txtdir = currentdir + Integer.toString(i) + "_0plnData.txt";
                    File temptxt = new File(txtdir);
                    if(temptxt.exists()){
                        matchingtxts++;
//                        for (int thshold : thresholds){
                        for(int thshold = 5; thshold <= 100; thshold += 5){
                            verifier.verifyLog(txtdir, nof, "operationSuccessRate", thshold);
                        }
                    }
                }
            } else {
                System.out.println("There is no such directory");
            }
            System.out.println("There were " + matchingtxts + " platooning text files");
        }
//
//        if (isSMBFL) {
//            ArrayList<EdgeInfo> edgeInfos = smbfl.SMcalculateSuspiciousness();
//            StructureModel finalSM = new StructureModel();
//            finalSM.collaborationGraph = smbfl.overlappedG;
//            finalSM.drawGraph();
//            System.out.println(edgeInfos.size());
//            for (EdgeInfo edgeInfo: edgeInfos) {
//                System.out.println("name: "+edgeInfo.edge+",    pass: "+edgeInfo.pass+",    fail: "+edgeInfo.fail+",    tarantula: "+edgeInfo.tarantulaM+", ochiai: "+edgeInfo.ochiaiM + ", op2: "+edgeInfo.op2M + ",   barinel: "+edgeInfo.barinelM+", dstar: "+edgeInfo.dstarM);
//            }
//            for (NodeInfo nodeInfo: smbfl.nodeInfos) {
//                System.out.println("name: "+nodeInfo.node+",    pass: "+nodeInfo.pass+",    fail: "+nodeInfo.fail);
//            }
//        }
//
//        if (isBMBFL) {
//
//        }
//
//        if (isIMBFL) {
//            imbfl.printSuspSequences();
//        }
    }
}
