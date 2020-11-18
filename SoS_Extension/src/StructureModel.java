import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


class PltNode {
    String vehId;
    String pltId;
    String pltDepth;
    String cNode;
}

public class StructureModel {

    Graph collaborationGraph;

    public PltNode searchPlt(List<PltNode> pltNodes, String pltId) {
        for (PltNode tempNode: pltNodes) {
            if (tempNode.pltId.equals(pltId))
                return tempNode;
        }
        return null;
    }

    public StructureModel (){}
    public StructureModel(int s_index, int r_index) {
        this.collaborationGraph = new SingleGraph("StructureModel" + s_index, false, false);

        String node = "";
        String cNode = "";
        String leader = "";
        String id = "";
        int numEvent = 1;
        ArrayList<PltNode> prevPltNodes = new ArrayList<PltNode>();
        try {
            String base = System.getProperty("user.dir");
            String currentdir = base + "/SoS_Extension/logs/";
            File pltConfig = new File(currentdir + s_index + "_" + r_index + "plnConfig.txt");
            FileReader filereader = new FileReader(pltConfig);
            BufferedReader bufReader = new BufferedReader(filereader);
            String line="";

            while(numEvent < 10 && (line = bufReader.readLine()) != null) { //TODO number of events
                if ((line.contains("0.00")) || (line.contains(String.valueOf(10*numEvent + 5) + ".50"))) { // Initial Condition (0.00) and every .50 condition
                    StringTokenizer st = new StringTokenizer(line);
                    st.nextToken();
                    String timeStamp = st.nextToken();
                    if((timeStamp.equals("0.00")) || (timeStamp.equals(String.valueOf(10*numEvent + 5) + ".50"))) {
                        // Insert platooning service nodes and edges
                        id = st.nextToken();
                        node = "Plt_" + id;
                        this.collaborationGraph.addNode(node);
                        st.nextToken();
                        leader = "Plt_" + st.nextToken();
                        this.collaborationGraph.addEdge(node + "-" + leader, node, leader);

                        // Insert Cruise-control service nodes and edges
                        cNode = "Crs_" + id;
                        this.collaborationGraph.addNode(cNode);
                        this.collaborationGraph.addEdge(cNode + "-" + node, node, cNode);

                        // Connect Cruise-control service to Platooning service
                        String depth =st.nextToken();
                        PltNode curPltNode = new PltNode();
                        curPltNode.vehId=node;
                        curPltNode.pltId=leader;
                        curPltNode.pltDepth=depth;
                        curPltNode.cNode=cNode;

                        if (depth.equals("0")) {
                            if (searchPlt(prevPltNodes, leader)==null)
                                prevPltNodes.add(curPltNode);
                            else {
                                PltNode updateNode = searchPlt(prevPltNodes, leader);
                                updateNode.pltDepth=depth;
                                updateNode.cNode=cNode;
                            }
                        } else {
                            PltNode updateNode = searchPlt(prevPltNodes, leader);
                            this.collaborationGraph.addEdge(updateNode.cNode + "-" + node, node, updateNode.cNode);
                            updateNode.pltDepth=depth;
                            updateNode.cNode=cNode;
                        }
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

    public String printGraphText() {
        String ret = "";
        ArrayList<String> nodeList = new ArrayList<>();
        ArrayList<String> fullNodeList = new ArrayList<>();

        fullNodeList.add("Plt_veh");
        fullNodeList.add("Crs_veh");
        for(int i = 1; i < 7; i++) {
            fullNodeList.add("Plt_veh." + i);
            fullNodeList.add("Crs_veh." + i);
        }
        fullNodeList.add("Plt_veh1");
        fullNodeList.add("Crs_veh1");
        for(int i = 1; i < 7; i++) {
            fullNodeList.add("Plt_veh1." + i);
            fullNodeList.add("Crs_veh1." + i);
        }

        for(int i = 0; i < fullNodeList.size(); i++) {
            if (this.collaborationGraph.getNode(fullNodeList.get(i)) != null)
                ret += "1\t";
            else
                ret += "0\t";
        }
        ret += "\n";

        for(int i = 0; i < fullNodeList.size(); i++) {
            for(int j = 0; j < fullNodeList.size(); j++) {
                Node temp_i = this.collaborationGraph.getNode(fullNodeList.get(i));
                Node temp_j = this.collaborationGraph.getNode(fullNodeList.get(j));
                if (temp_i == null || temp_j == null) {
                    ret += "0\t";
                } else {
                    if (temp_i.hasEdgeBetween(temp_j)) {
                        ret += "1\t";
                    } else {
                        ret += "0\t";
                    }
                }
            } ret += "\n";
        }
        /*for(Node node : this.collaborationGraph) {
            ret += "\t\t";
            ret += node.getId();
            nodeList.add(node.getId());
            ret += "\t";
        }

        for(Node node: this.collaborationGraph) {
            ret += node.getId();
            ret += "\t";
            for(String destNode: nodeList) {
                if(node.hasEdgeBetween(destNode)) {
                    ret += "1\t";
                } else {
                    ret += "0\t";
                }
            }
            ret += "\n";
        }*/
        return ret;
    }
}
