import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;


public class StructureModel {

    Graph collaborationGraph;

    public StructureModel(String logAddress, int s_index, int r_index) {
        this.collaborationGraph = new SingleGraph("StructureModel" + s_index, false, false);

        String node = "";
        String cNode = "";
        String leader = "";
        String id = "";
        int numEvent = 1;
        try {
            File pltConfig = new File("./examples/platoon_SoS/results/" + s_index + "_" + r_index + "plnConfig.txt");
            FileReader filereader = new FileReader(pltConfig);
            BufferedReader bufReader = new BufferedReader(filereader);
            String line="";
            while(numEvent < 10 && (line = bufReader.readLine()) != null) { //TODO number of events
                if (line.contains("0.00")) { // Initial Condition
                    StringTokenizer st = new StringTokenizer(line);
                    st.nextToken();
                    if(st.nextToken().equals("0.00")) {
                        // Insert platooning service nodes and edges
                        id = st.nextToken();
                        node = "Plt_" + id;
                        this.collaborationGraph.addNode(node);
                        st.nextToken();
                        leader = "Plt_" + st.nextToken();
                        this.collaborationGraph.addEdge(node + "-" + leader, node, leader);
                        System.out.println(this.collaborationGraph.getEdgeCount());
                        // Insert Cruise-control service nodes and edges
                        cNode = "Crs_" + id;
                        this.collaborationGraph.addNode(cNode);
                        this.collaborationGraph.addEdge(cNode + "-" + node, node, cNode);
                    }
                } else if(line.contains(String.valueOf(10*numEvent + 5) + ".50")) { // Every 10 ticks, update the connections
                                                                                    // between vehicles
                    StringTokenizer st = new StringTokenizer(line);
                    st.nextToken();
                    if(st.nextToken().equals(String.valueOf(10*numEvent + 5) + ".50")) {
                        // Insert platooning service nodes and edges
                        id = st.nextToken();
                        node = "Plt_" + id;
                        this.collaborationGraph.addNode(node);
                        st.nextToken();
                        leader = "Plt_" + st.nextToken();
                        this.collaborationGraph.addEdge(node + "-" + leader, node, leader);
                        System.out.println(node + " " + leader);
                    }
                } else if (line.contains(String.valueOf(10*numEvent + 5) + ".60")) {
                    numEvent++;
                }
            }


        } catch(Exception e) {
            System.out.println(e);
        }
    }

    protected String styleSheet =
            "node {" +
                    "	fill-color: orange;" +
                    "   size: 10px;" +
                    "   text-size:13px;" +
                    "   text-color: black;" +
                    "}" +
                    "node.marked {" +
                    "	fill-color: grey;" +
                    "   size: 10px;" +
                    "   text-size:13px;" +
                    "   text-color: black;" +
                    "}";

    public void drawGraph() {
        for (Node node : this.collaborationGraph) { // Labelling to nodes
            node.addAttribute("ui.label", node.getId());

            if(node.getId().contains("Crs_")) { // Mark Cruise-control nodes
                node.setAttribute("ui.class", "marked");
            }
        }

        this.collaborationGraph.addAttribute("ui.stylesheet", styleSheet);
        this.collaborationGraph.display();
    }
}
