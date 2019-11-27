import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.*;
import java.io.FileOutputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Random;

public class Main {

    public static void main(String[] args) {
	    // Generate Random Scenario
//	    ScenarioGenerator scenarioGenerator = new ScenarioGenerator();
//        scenarioGenerator.generateRandomScenario(50);

        // Update Omnet.ini file for executing each scenario
        File omnetConf = new File("./examples/platoon_SoS/omnetpp.ini");
        Graph overlappedG = new SingleGraph("CompleteStructureModel", false, false);
        ArrayList<NodeInfo> nodeInfos = new ArrayList<NodeInfo>();
        ArrayList<EdgeInfo> edgeInfos = new ArrayList<EdgeInfo>();
        double totalPassed=0;
        double totalFailed=0;
        for(int i = 1; i <= 1; i++) { // Number of scenarios, currently 50
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
                Graph currentG = stm.collaborationGraph;
                for (Node node: currentG.getEachNode()) {
                    if (!searchNode(node.getId(), overlappedG)) {
                        overlappedG.addNode(node.getId());
                        NodeInfo nodeInfo = new NodeInfo();
                        nodeInfo.node = node.getId();
                        // nodeIfo.pass value
                        // nodeInfo.fail value
                        nodeInfos.add(nodeInfo);
                    } else {
                        NodeInfo tempNodeIfo = searchNode(node.getId(), nodeInfos);
                        //tempNodeInfo.pass value
                        //tempNodeInfo.fail value
                    }
                }
                for (Edge edge: currentG.getEachEdge()) {
                    if (!searchEdge(edge.getId(), overlappedG)) {
                        overlappedG.addEdge(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId());
                        EdgeInfo edgeInfo = new EdgeInfo();
                        edgeInfo.edge = edge.getId();
                        //edgeInfo.pass value
                        //edgeInfo.fail value
                        edgeInfos.add(edgeInfo);
                    } else {
                        EdgeInfo tempEdgeIfo = searchEdge(edge.getId(), edgeInfos);
                        //tempNodeInfo.pass value
                        //tempNodeInfo.fail value
                    }
                }
                if (true) // simulation result pass
                    totalPassed++;
                else
                    totalFailed++;
                stm.drawGraph();
            }
        }

        for (EdgeInfo edgeInfo: edgeInfos) {
            SuspisiousnessMeasure sm = new SuspisiousnessMeasure();
            sm.totalFailed = totalFailed;
            sm.totalPassed = totalPassed;
            sm.faileds = edgeInfo.fail;
            sm.passeds = edgeInfo.pass;

            edgeInfo.tarantulaM = sm.tarantula();
            edgeInfo.ochiaiM = sm.ochiai();
            edgeInfo.op2M = sm.op2();
            edgeInfo.barinelM = sm.barinel();
            edgeInfo.dstarM = sm.dstar();
        }
    }

    static boolean searchNode(String nodeId, Graph overlappedG) {
        for (Node node: overlappedG.getEachNode()) {
            if (node.getId().equals(nodeId))
                return true;
        }
        return false;
    }

    static NodeInfo searchNode (String node, ArrayList<NodeInfo> nodeInfos) {
        for (NodeInfo tempN : nodeInfos) {
            if (tempN.node.equals(node))
                return tempN;
        }
        return null;
    }

    static boolean searchEdge (String edgeId, Graph overlappedG) {
        for (Edge edge: overlappedG.getEachEdge()) {
            if (edge.getId().equals(edgeId))
                return true;
        }
        return false;
    }

    static EdgeInfo searchEdge (String edge, ArrayList<EdgeInfo> edgeInfos) {
        for (EdgeInfo tempE: edgeInfos) {
            if (tempE.edge.equals(edge))
                return tempE;
        }
        return null;
    }
}

class NodeInfo {
    String node;
    int pass;
    int fail;
}

class EdgeInfo {
    String edge;
    int pass;
    int fail;
    double tarantulaM;
    double ochiaiM;
    double op2M;
    double barinelM;
    double dstarM;
}
