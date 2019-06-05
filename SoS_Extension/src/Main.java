import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
	// write your code here
        ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
        scenarioGenerator.generateRandomScenario(2);

        String s;


        Runtime rt = Runtime.getRuntime();
        try {
            Process p = rt.exec("opp_run -r 0 -m -u Cmdenv -c Platooning -n ..:../../src -l ../../src/VENTOS_Public omnetpp.ini", null, new File("./examples/platoon_SoS"));
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            while ((s = br.readLine()) != null)
                System.out.println("line: " + s);
            p.waitFor();
            System.out.println ("exit: " + p.exitValue());
            p.destroy();
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}
