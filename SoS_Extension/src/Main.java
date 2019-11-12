import java.io.*;
import java.io.FileOutputStream;
import java.nio.Buffer;
import java.util.StringTokenizer;
import java.util.Random;

public class Main {

    public static void main(String[] args) {
	    // Generate Random Scenario
//	    ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
//        scenarioGenerator.generateRandomScenario(50);

        // Update Omnet.ini file for executing each scenario
        File omnetConf = new File("./examples/platoon_SoS/omnetpp.ini");
        for(int i = 0; i < 1; i++) { // Number of scenarios, currently 50
//            try {
//                BufferedReader reader = new BufferedReader(new FileReader(omnetConf));
//
//                String line = reader.readLine();
//                //System.out.println(line);
//
//                String content = "";
//                while (line != null) {
//                    //System.out.println("OMNETPP");
//                    if(line.contains("addNode")) {
//
//                        content += "Network.addNode.id = \"example_" + i + "\"\n";
//                    } else if (line.contains("trafficControl")) {
//                        content += "Network.trafficControl.id = \"example_" + i + "\"\n";
//                    } else {
//                        content += line + "\n";
//                    }
//                    line = reader.readLine();
//                }
//                reader.close();
//
//                FileWriter writer = new FileWriter(omnetConf);
//                writer.write(content);
//
//                writer.close();
//            } catch (Exception e) { // generated scenarios apply to VENTOS input files
//                System.out.println(e);
//            }

//            Random r = new Random();
//            int accidentThreshold = (int)(1000 * 0.01) + r.nextInt(10);

            for(int j = 0; j < 1; j++) { // Number of execution of same scenarios & configurations
//                String s;
//                Runtime rt = Runtime.getRuntime();
//                System.out.println("RUNNING");

//                if(j == accidentThreshold) {
//                    System.out.println("CHANGED");
//                    File addNode = new File("./examples/platoon_SoS/addNode.xml");
//                    String content = "";
//                    try {
//                        BufferedReader br = new BufferedReader(new FileReader(addNode));
//
//                        String line = br.readLine();
//                        //System.out.println(line);
//
//                        while (line != null) {
//                            if(line.contains("stopped")) {
//                                System.out.println(line);
//                                content += "\n";
//                            }
//                            else content += line + "\n";
//                            line = br.readLine();
//                        }
//
//                        br.close();
//
//                        FileWriter writer = new FileWriter(addNode);
//                        writer.write(content);
//
//                        writer.close();
//                    } catch (Exception e) {
//                        System.out.println(e);
//                    }
//                }

//                try { // Execute a process and loggin the console output
//                    System.setOut(new PrintStream(new FileOutputStream("./examples/platoon_SoS/results/consoleLog.txt")));
//                    System.out.println("####### Scenario: " + i + " #######");
//                    Process p = rt.exec("opp_run -m -u Cmdenv -c Platooning -n ..:../../src -l ../../src/VENTOS_Public omnetpp.ini", null, new File("./examples/platoon_SoS"));
//
//                    // Logging the process result
//                    BufferedReader br = new BufferedReader(
//                            new InputStreamReader(p.getInputStream()));
//                    while ((s = br.readLine()) != null) {
//                        System.out.println("line: " + s);
//                    }
//                    p.waitFor();
//                    System.out.println("exit: " + p.exitValue());
//                    p.destroy();
//                } catch (Exception e) {
//                    System.out.println(e);
//                }

                // Logging the data of platooning & vehicle emission
//                File plnConfig = new File("./examples/platoon_SoS/results/000_plnConfig.txt");
//                File plnData = new File("./examples/platoon_SoS/results/000_plnData.txt");
//                File emissionData = new File("./examples/platoon_SoS/results/000_vehicleEmission.txt");
//                File vehData = new File("./examples/platoon_SoS/results/000_vehicleData.txt");
//
//                System.out.println(plnConfig.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"plnConfig.txt")));
//                plnData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"plnData.txt"));
//                emissionData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"emissionData.txt"));
//                vehData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"vehicleData.txt"));

                StructureModel stm = new StructureModel("./logs/", i, j);
                stm.drawGraph();
            }
        }
    }
}
