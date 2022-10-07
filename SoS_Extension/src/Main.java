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
            String currentdir = base + "/SoS_Extension/logs_full/MCI Sample";
            System.out.print("Current Working Directory : " + currentdir + "\n");
            File f = new File(currentdir);
            Boolean result;

            ArrayList<InterplayModel> IMs = new ArrayList<>();
            ArrayList<InterplayModel> PIMs = new ArrayList<>(); // Passed IMs
            ArrayList<ArrayList<String>> oracle = new ArrayList();
            ArrayList<String> o_index = new ArrayList();

            File[] folders = f.listFiles();
            int file_id = 0;
            for (File folder : folders) {
                ArrayList<String> temp_oracle = new ArrayList();
                for (File target : folder.listFiles()) {
                    if (target.toString().contains("$")) continue;
                    try(FileInputStream fis = new FileInputStream(target)) {
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

                        if (folder.getPath().contains("coll")) {
                            temp_oracle.add(String.valueOf(file_id));
                            o_index.add(String.valueOf(file_id));
                            IMs.add(interplayModel);
                        } else {
                            PIMs.add(interplayModel);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println(e);
                    }
                }
                if (temp_oracle.size() != 0) oracle.add(temp_oracle);
            }
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

//            FilenameFilter filter = new FilenameFilter() {
//                public boolean accept(File f, String name) {
//                    return name.contains(".xlsx");
//                }
//            };
//
//            if (BasicVerifierProperty) {
//                if (f.exists()) {
//                    File files[] = f.listFiles(filter);
//                    for (File txtdir : files) {
//                        matchingtxts++;
//                        for (int thshold : thresholds) {
//                            result = verifier.verifyLog(txtdir.getPath(), "operationSuccessRate", thshold);
//                            InterplayModel interplayModel = new InterplayModel(Integer.parseInt(txtdir.getName().split("_")[0]), 0); // TODO r_index = 0 로 설정해놓음
//                            if (!result) {
//                                IMs.add(interplayModel);
//                                // Structure & Interplay model ".txt" file exporting part
//                                /*
//                                StructureModel structureModel = new StructureModel(i,0);
//                                File exportTxt = new File(currentdir + Integer.toString(i) + "_S_I_Model.txt");
//                                FileWriter writerExport = null;
//                                try {
//                                    writerExport = new FileWriter(exportTxt, true);
//                                    writerExport.write(Integer.toString(i) + "\n");
//                                    writerExport.write("Interplay\n");                                              // OpSuccessRate -> I / OpTime -> S
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
//                                }*/
//                            } else {
//                                PIMs.add(interplayModel);
//                            }
//                        }
////                        for (int thshold2 : thresholds2){
////                            result = verifier.verifyLog(txtdir, "operationTime", thshold2);
//////                            smbfl.structureModelOverlapping(results, i, 0);
////                            if (!result) {
////                                InterplayModel interplayModel = new InterplayModel(i, 0);                       // TODO r_index = 0 로 설정해놓음
////                                StructureModel structureModel = new StructureModel(i,0);
//////                            clustering.addTrace(interplayModel, simlr_threshold);                                  // TODO Similarity Threshold = 75%
////                                IMs.add(interplayModel);
////
////                                // Structure & Interplay model ".txt" file exporting part
////                                File exportTxt = new File(currentdir + Integer.toString(i+1512) + "_S_I_Model.txt");
////                                FileWriter writerExport = null;
////                                try {
////                                    writerExport = new FileWriter(exportTxt, true);
////                                    writerExport.write(Integer.toString(i) + "\n");
////                                    writerExport.write("Structure\n");                                              // OpSuccessRate -> I / OpTime -> S
////                                    writerExport.write("Structure Model\n");
////                                    writerExport.write(structureModel.printGraphText());
////                                    writerExport.write("Interplay Model\n");
////                                    writerExport.write(interplayModel.printSequence());
////                                } catch (IOException e) {
////                                    System.out.println(e);
////                                } finally {
////                                    try {
////                                        if (writerExport != null) writerExport.close();
////                                    } catch (IOException e) {
////                                        e.printStackTrace();
////                                    }
////                                }
////                            }
////                        }
//                    }
//                } else {
//                    System.out.println("There is no such directory");
//                }
//                System.out.println("There were " + matchingtxts + " platooning text files");
//            }

            // TODO Check the input parameter whether distance checker should be processed
            if (DistanceChecker) {
                if (f.exists()) {
                    int numoffiles = f.listFiles().length;
                    System.out.println("and it has " + numoffiles + " files.");
                    for (int i = 0; i < numoffiles; i++) {
                        String txtdir_pltConfig = currentdir + Integer.toString(i) + "_0plnConfig.txt";
                        String txtdir_veh = currentdir + Integer.toString(i) + "_0vehicleData.txt";
                        File temptxt = new File(txtdir_pltConfig);
                        File temptxt2 = new File(txtdir_veh);
                        if (temptxt.exists() && temptxt2.exists()) {
                            for (int thshold : thresholds) {
                                result = verifier.verifyLog(txtdir_pltConfig, txtdir_veh, "DistanceChecker", thshold); // TODO first txt file is for platoon data
                            }
                        }
                    }
                } else {
                    System.out.println("There is no such directory [" + f.getAbsolutePath() + "]");
                }
            }

            if (CollisionChecker) {
                if (f.exists()) {
                    int numoffiles = f.listFiles().length;
                    System.out.println("and it has " + numoffiles + " files.");
                    for (int i = 0; i < numoffiles; i++) {
                        String txtdir_console = currentdir + Integer.toString(i) + "_0consoleLog.txt";
                        String txtdir_vehData = currentdir + Integer.toString(i) + "_0vehicleData.txt";
                        File temptxt = new File(txtdir_console);
                        if (temptxt.exists()) {
                            result = verifier.verifyLog(txtdir_console, txtdir_vehData, "collision");
                        }
                    }
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

            double c_simlr = 0.6;
            double c_delay = 0.1;
            int c_len = 8;

            double m_simlr = 0.86;
            double m_delay = 1;
            int m_len = 9;

            boolean Localization = false;

            OracleGenerator oracleGenerator = new OracleGenerator(oracle, o_index);

            Clustering clustering = new Clustering();
            clustering.setId_patterns(id_patterns);
            Collections.shuffle(IMs);
            for (InterplayModel im : IMs) {
                clustering.SingleCasePatternMining(im, 5.0, 15);
            }
            clustering.printMaxPattern();

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
                clustering.codeLocalizer(base, "/src/nodes/vehicle/05_PlatoonMg.cc");
            } else {
                for (int k = 0; k < 30; k++) {
                    ArrayList<InterplayModel> IMs_batch = new ArrayList<>();
//
                    File batch_im = new File(base + "/SoS_Extension/results/" + "F1P - HyperparameterAnalysis_Base_" + k + ".txt");
                    try {
                        FileWriter writer = new FileWriter(batch_im, true);
                        String ret = "";
                        Collections.shuffle(IMs);
                        for (int i = 0; i < 1000; i++) {
                            IMs_batch.add(IMs.get(i));
                            ret += IMs.get(i).getId() + "\n";
                        }
                        writer.write(ret);
                        writer.close();
                    } catch (Exception e) {
                        System.out.println(e);
                    }

//                    OracleGenerator oracleGenerator = new OracleGenerator();
//                    oracleGenerator.oracleGeneration(IMs_batch);
//                oracleGenerator.oracleGeneration(IMs);
//                    oracleGenerator.printOracle();
//                    oracleGenerator.getOracleCSV();
//                    ArrayList<ArrayList<String>> oracle = oracleGenerator.getOracle();

                    boolean multiple_cases = true;
                    boolean single_run = false;

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
                                                    double pattern_identity_score = clustering.PatternIdentityCheckerSingleCase(delay_threshold, oracle, i);
                                                    double pattern_identity_score_w = clustering.PatternIdentityCheckerWeightSingleCase(delay_threshold, oracle, i);
                                                    if (max_PIT < pattern_identity_score)
                                                        max_PIT = pattern_identity_score;
                                                    if (max_PITW < pattern_identity_score_w)
                                                        max_PITW = pattern_identity_score_w;
                                                    System.out.println(delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters + ", PIT value: " + pattern_identity_score + ", PITW value: " + pattern_identity_score_w + ", Time(ms): " + (endTime - startTime));
                                                    ret += delay_threshold + "," + lcs_min_len_threshold + "," + f1p_ev_score.get(2) + "," + f1p_ev_score.get(0) + "," + f1p_ev_score.get(1) + "," + number_of_clusters + "," + pattern_identity_score + "," + pattern_identity_score_w + ", Time(ms): ," + (endTime - startTime) + "\n";

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
//                                for (int i = 0; i < 12; i++) {
//                                ArrayList<Double> pattern_identity_score_w = clustering.SPADEPatternIdentityCheckerWeight(0.1, oracle, "SPADE_" + i + "_0.75.txt");
//                                    double pattern_identity_score_w = clustering.LogLinerPatternIdentityCheckerWeight(0.1, oracle, "LogLiner_0_" + i + ".txt", i);
//                                String pattern_identity_score_w_print = String.valueOf(pattern_identity_score_w.get(pattern_identity_score_w.size() - 1));
//                                for (int l = 0; l < pattern_identity_score_w.size() - 1; l++) {
//                                    pattern_identity_score_w_print += "," + pattern_identity_score_w.get(l);
//                                }
//                                System.out.println(pattern_identity_score_w_print);
//                                    System.out.println(pattern_identity_score_w);
//                                    writer_pattern += pattern_identity_score_w + ",";
//                                File file2 = new File(base + "/SoS_Extension/results/" + "SPADE_PIT_PITW_0.csv");
//                                try {
//                                    FileWriter writer = new FileWriter(file2, true);
//                                    writer.write(pattern_identity_score_w_print);
//                                    writer.write(",");
//                                    writer.close();
//                                } catch (Exception e) {
//                                    System.out.println(e);
//                                }
//                                pattern_identity_score_w.clear();
//                                }
                                File file2 = new File(base + "/SoS_Extension/results/" + "LogLiner_PIT_PITW_0.csv");
                                try {
                                    FileWriter writer = new FileWriter(file2, true);
//                                    writer.write(pattern_identity_score_w_print);
                                    writer.write(writer_pattern);
                                    writer.close();
                                } catch (Exception e) {
                                    System.out.println(e);
                                }
                            }
                        } else {
                            // Multiple cases run

                            // Generate folders for patterns
//                    File folder1 = new File(base + "/SoS_Extension/results/patterns");
//                    if (!folder1.exists()) {
//                        try {
//                            folder1.mkdir();
//                        } catch (Exception e) {
//                            System.out.println(e);
//                        }
//                    }
//                Date date = new Date();
//                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH-mm");
////              System.out.println(formatter.format(date));
//                File folder2 = new File(base + "/SoS_Extension/results/patterns/" + formatter.format(date));
//                folder2.mkdir();
                            if (!single_run) {
                                File file2 = new File(base + "/SoS_Extension/results/" + "F1P - HyperparameterAnalysis_Base_" + k + ".csv");  // TODO Which Case? -> File Name Change
//                    File file2 = new File(base + "/SoS_Extension/results/" + "F1P - Base HyperparameterAnalysis_withTime_03_19.csv");
                                try {
                                    FileWriter writer = new FileWriter(file2, true);
                                    String ret = "";
                                    // The code for Hyperparameter optimization of clustering algorithm
//                                    Clustering clustering = new Clustering();
                                    for (int simlr_counter = 60; simlr_counter <= 90; simlr_counter++) {
                                        simlr_threshold = (double) simlr_counter / 100;
                                        for (int delay_counter = 10; delay_counter <= 100; delay_counter += 10) {
                                            delay_threshold = (double) delay_counter / 100;
                                            for (lcs_min_len_threshold = 2; lcs_min_len_threshold <= 15; lcs_min_len_threshold++) {

                                                long startTime = System.currentTimeMillis();

//                                    Collections.shuffle(IMs); // TODO Random Sort
                                                for (InterplayModel im : IMs_batch) {
                                                    // 대조군 Clustering Algorithm
//                                                clustering.addTraceBaseLCS(im, delay_threshold, lcs_min_len_threshold);

                                                    clustering.addTraceCase6(im, simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                                    // For Merging&Finalizing Optimization
//                                    clustering.addTraceCase6(im, c_simlr, c_delay, c_len);
                                                }

                                                // Clustering Merge Optimization
                                                clustering.ClusterMerge(simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                                // Clustering Finalize Optimization
//                                clustering.ClusterMerge(m_simlr, m_delay, m_len);
                                                clustering.ClusteringFinalize(simlr_threshold, delay_threshold, lcs_min_len_threshold);

                                                // Pattern Logging
//                                    File folder3 = new File(base + "/SoS_Extension/results/patterns/" + formatter.format(date) + "/" + simlr_threshold*100 + "_" + delay_threshold*100 + "_" + lcs_min_len_threshold);
//                                    folder3.mkdir();
//                                    clustering.PatternTxt(folder3);
                                                long endTime = System.currentTimeMillis();
                                                number_of_clusters = clustering.clusterSize();
                                                // Oracle-based Evaluation Score
                                                f1p_ev_score = clustering.EvaluateF1P(oracle, oracleGenerator.getIndex()); // 0: F_C_O, 1: F_O_C, 2: Evaluation Score
//                                evaluation_score = clustering.EvaluateClusteringResult(oracle, oracleGenerator.getIndex());
                                                double pattern_identity_score = clustering.PatternIdentityChecker(delay_threshold, oracle);
                                                ArrayList<Double> pattern_identity_score_w = clustering.PatternIdentityCheckerWeight(delay_threshold, oracle);
                                                String pattern_identity_score_w_print = String.valueOf(pattern_identity_score_w.get(pattern_identity_score_w.size() - 1));
                                                for (int l = 0; l < pattern_identity_score_w.size() - 1; l++) {
                                                    pattern_identity_score_w_print += "," + pattern_identity_score_w.get(l);
                                                }
                                                System.out.println(simlr_threshold + ", " + delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters + ", PIT value: " + pattern_identity_score + ", PITW value: " + pattern_identity_score_w_print + ", Time(ms): " + (endTime - startTime));
                                                ret += simlr_threshold + "," + delay_threshold + "," + lcs_min_len_threshold + "," + f1p_ev_score.get(2) + "," + f1p_ev_score.get(0) + "," + f1p_ev_score.get(1) + "," + number_of_clusters + "," + pattern_identity_score + "," + pattern_identity_score_w_print + "," + (endTime - startTime) + "\n";

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
//                            clustering.printCluster();
                                clustering.printMaxPattern();
                                f1p_ev_score = clustering.EvaluateF1P(oracle, oracleGenerator.getIndex());
                                number_of_clusters = clustering.clusterSize();
                                double pattern_identity_score = clustering.PatternIdentityChecker(delay_threshold, oracle);
                                ArrayList<Double> pattern_identity_score_w = clustering.PatternIdentityCheckerWeight(delay_threshold, oracle);
                                String pattern_identity_score_w_print = String.valueOf(pattern_identity_score_w.get(pattern_identity_score_w.size() - 1));
                                for (int l = 0; l < pattern_identity_score_w.size() - 1; l++) {
                                    pattern_identity_score_w_print += "," + pattern_identity_score_w.get(l);
                                }
                                System.out.println(simlr_threshold + ", " + delay_threshold + ", " + lcs_min_len_threshold + "," + " Clustering Evaluation Score: " + f1p_ev_score.get(2) + ", F_C_O: " + f1p_ev_score.get(0) + ", F_O_C: " + f1p_ev_score.get(1) + ", Cluster Size: " + number_of_clusters + ", PIT value: " + pattern_identity_score + ", PITW value: " + pattern_identity_score_w_print + ", Time(ms): " + (endTime - startTime));

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
