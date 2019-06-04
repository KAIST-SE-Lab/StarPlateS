import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
	// write your code here
        ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
        scenarioGenerator.generateRandomScenario(2);

/*        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new ArrayList<String>();
        cmd.add("opp_run");
        cmd.add("-r");
        cmd.add("0");
        cmd.add("-m");
        cmd.add("-u");
        cmd.add("Cmdenv");
        cmd.add("-c");
        cmd.add("Platooning");
        cmd.add("-n");
        cmd.add("..:../../src");
        cmd.add("-l");
        cmd.add("../../src/VENTOS_Public");
        cmd.add("omnetpp.ini");
        String[] cmd_ary = {"opp_run", "-r 0","-m", "-u Cmdenv", "-c Platooning", "-n ..:../../src", "-l ../../src/VENTOS_Public", "omnetpp.ini"};
        pb.command(cmd);
        //pb.command("opp_run -r 0 -m -u Cmdenv -c Platooning -n ..:../../src -l ../../src/VENTOS_Public omnetpp.ini");
        pb.directory(new File("/home/abalon1210/Desktop/VENTOS_Public/examples/platoon_SoS"));*/

        Runtime rt = Runtime.getRuntime();
        try {
            //Process p = pb.start();
            Process pr = rt.exec("opp_run -r 0 -m -u Cmdenv -c Platooning -n ..:../../src -l ../../src/VENTOS_Public omnetpp.ini", null, new File("/home/abalon1210/Desktop/VENTOS_Public/examples/platoon_SoS"));
        } catch(Exception e) {
            System.out.println(e);
        }
    }
}
