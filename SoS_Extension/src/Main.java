import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Main {

    public static void main(String[] args) {
	    // Generate Random Scenario
	    ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
        scenarioGenerator.generateRandomScenario(2);

        // Update Omnet.ini file for executing each scenario
        File omnetConf = new File("./examples/platoon_SoS/omnetpp.ini");
        for(int i = 0; i < 2; i++) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(omnetConf));

                String line = reader.readLine();
                System.out.println(line);

                String content = "";
                while (line != null) {
                    System.out.println("OMNETPP");
                    if(line.contains("addNode")) {
                        StringTokenizer st = new StringTokenizer(line, "=");
                        String pre = st.nextToken();
                        String post = st.nextToken();

                        post = post.substring(0, post.length() - 2) + i +"\"";

                        content += pre + "=" + post + "\n";
                    } else if (line.contains("trafficControl")) {
                        StringTokenizer st = new StringTokenizer(line, "=");
                        String pre = st.nextToken();
                        String post = st.nextToken();

                        post = post.substring(0, post.length() - 2) + i +"\"";

                        content += pre + "=" + post + "\n";
                    } else {
                        content += line + "\n";
                    }
                    line = reader.readLine();
                }
                reader.close();

                FileWriter writer = new FileWriter(omnetConf);
                writer.write(content);

                writer.close();
            } catch (Exception e) {
                System.out.println(e);
            }


            for(int j = 0; j < 1; j++) {
                // Executing the simulator with specific trafficControl events.
                String s;
                Runtime rt = Runtime.getRuntime();
                try {
                    Process p = rt.exec("opp_run -m -u Cmdenv -c Platooning -n ..:../../src -l ../../src/VENTOS_Public omnetpp.ini", null, new File("./examples/platoon_SoS"));

                    // Logging the process result
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    while ((s = br.readLine()) != null)
                        System.out.println("line: " + s);
                    p.waitFor();
                    System.out.println("exit: " + p.exitValue());
                    p.destroy();
                } catch (Exception e) {
                    System.out.println(e);
                }

                File plnConfig = new File("./examples/platoon_SoS/results/000_plnConfig.txt");
                File plnData = new File("./examples/platoon_SoS/results/000_plnData.txt");
                File vehData = new File("./examples/platoon_SoS/results/000_vehicleData.txt");
            }
        }
    }
}
