import java.io.*;
import java.io.FileOutputStream;
import java.nio.Buffer;
import java.util.StringTokenizer;
import java.util.Random;

public class Main {

    public static void main(String[] args) {
	    // Generate Random Scenario
//	    ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
//        scenarioGenerator.generateRandomScenario(1);

        // Update Omnet.ini file for executing each scenario
        File omnetConf = new File("./examples/platoon_SoS/omnetpp.ini");
        for(int i = 0; i < 1; i++) {
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
//                        StringTokenizer st = new StringTokenizer(line, "=");
//                        String pre = st.nextToken();
//                        String post = st.nextToken();
//
//                        post = post.substring(0, post.length() - 2) + i +"\"";
//
//                        content += pre + "=" + post + "\n";
//                    } else if (line.contains("trafficControl")) {
//                        StringTokenizer st = new StringTokenizer(line, "=");
//                        String pre = st.nextToken();
//                        String post = st.nextToken();
//
//                        post = post.substring(0, post.length() - 2) + i +"\"";
//
//                        content += pre + "=" + post + "\n";
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

            Random r = new Random();
            int accidentThreshold = (int)(1000 * 0.01) + r.nextInt(10);

            for(int j = 0; j < 1; j++) { // Executing the simulator with specific trafficControl events.
//                String s;
//                Runtime rt = Runtime.getRuntime();
//                System.out.println("RUNNING");
//
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
//
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
//
//                // Logging the data of platooning & vehicle emission
//                File plnConfig = new File("./examples/platoon_SoS/results/000_plnConfig.txt");
//                File plnData = new File("./examples/platoon_SoS/results/000_plnData.txt");
//                File emissionData = new File("./examples/platoon_SoS/results/000_vehicleEmission.txt");
//                File vehData = new File("./examples/platoon_SoS/results/000_vehicleData.txt");
//
//                System.out.println(plnConfig.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"plnConfig.txt")));
//                plnData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"plnData.txt"));
//                emissionData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"emissionData.txt"));
//                vehData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"vehicleData.txt"));

                StructureModel stm = new StructureModel("./examples/platoon_SoS/results/", i, j);
                stm.drawGraph();
            }
        }
    }
}
