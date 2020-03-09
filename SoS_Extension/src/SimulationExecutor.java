import java.io.*;
import java.util.Random;

public class SimulationExecutor {
    public void run(int numScenario, int numRepeat, Boolean isSMBFL, Boolean isBMBFL, Boolean isIMBFL,
                    StructureModelBasedFaultLocalization smbfl, InterplayModelBasedFaultLocalization imbfl) {
        // To update Omnet.ini file for executing each scenario
        File omnetConf = new File("./examples/platoon_SoS/omnetpp.ini");

        PrintStream origin = System.out;

        for(int i = 0; i < numScenario; i++) { // TODO i = k for executing scenarios from k
            try {
                BufferedReader reader = new BufferedReader(new FileReader(omnetConf));

                String line = reader.readLine();

                String content = "";
                while (line != null) {
                    if(line.contains("addNode")) {

                        content += "Network.addNode.id = \"example_" + i + "\"\n";
                    } else if (line.contains("trafficControl")) {
                        content += "Network.trafficControl.id = \"example_" + i + "\"\n";
                    } else {
                        content += line + "\n";
                    }
                    line = reader.readLine();
                }
                reader.close();

                FileWriter writer = new FileWriter(omnetConf);
                writer.write(content);

                writer.close();
            } catch (Exception e) { // generated scenarios apply to VENTOS input files
                System.out.println(e);
            }

            Random r = new Random();
            int accidentThreshold = (int)(1000 * 0.01) + r.nextInt(10);

            for(int j = 0; j < numRepeat; j++) { // Number of execution of same scenarios & configurations
                String s;
                Runtime rt = Runtime.getRuntime();
                System.out.println("RUNNING");

                if(j == accidentThreshold) {
                    System.out.println("CHANGED");
                    File addNode = new File("./examples/platoon_SoS/addNode.xml");
                    String content = "";
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(addNode));

                        String line = br.readLine();
                        //System.out.println(line);

                        while (line != null) {
                            if(line.contains("stopped")) {
                                System.out.println(line);
                                content += "\n";
                            }
                            else content += line + "\n";
                            line = br.readLine();
                        }

                        br.close();

                        FileWriter writer = new FileWriter(addNode);
                        writer.write(content);

                        writer.close();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }

                try { // Execute a process and loggin the console output
                    System.setOut(new PrintStream(new FileOutputStream("./examples/platoon_SoS/results/consoleLog.txt")));
                    System.out.println("####### Scenario: " + i + " #######");
                    Process p = rt.exec("opp_run -m -u Cmdenv -c Platooning -n ..:../../src -l ../../src/VENTOS_Public omnetpp.ini", null, new File("./examples/platoon_SoS"));

                    // Logging the process result
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    while ((s = br.readLine()) != null) {
                        System.out.println("line: " + s);
                    }
                    p.waitFor();
                    System.out.println("exit: " + p.exitValue());
                    p.destroy();
                } catch (Exception e) {
                    System.out.println(e);
                }

                // Logging the data of platooning & vehicle emission
                File plnConfig = new File("./examples/platoon_SoS/results/000_plnConfig.txt");
                File plnData = new File("./examples/platoon_SoS/results/000_plnData.txt");
                File emissionData = new File("./examples/platoon_SoS/results/000_vehicleEmission.txt");
                File vehData = new File("./examples/platoon_SoS/results/000_vehicleData.txt");

                // boolean isTracePassed=false; //TODO #REFACTOR tag the simulation result
                // issue 1 insert fault vehicle?
                if (isSMBFL)
                    smbfl.structureModelOverlapping(false, 1, 0);
                if (isBMBFL);
//                  bmbfl.
                if (isIMBFL);
                    imbfl.addFailedLog(true, 1, 0);

                System.out.println(plnConfig.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"plnConfig.txt")));
                plnData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"plnData.txt"));
                emissionData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"emissionData.txt"));
                vehData.renameTo(new File("./examples/platoon_SoS/results/" + i + "_" + j +"vehicleData.txt"));

                System.setOut(origin);
            }
        }
    }
}
