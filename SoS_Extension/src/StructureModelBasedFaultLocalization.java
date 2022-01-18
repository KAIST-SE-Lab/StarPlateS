import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.ArrayList;

public class StructureModelBasedFaultLocalization {
    ArrayList<EdgeInfo> edgeInfos = new ArrayList<EdgeInfo>();
    ArrayList<NodeInfo> nodeInfos = new ArrayList<NodeInfo>();
    double totalPassed;
    double totalFailed;
    Graph overlappedG = new SingleGraph("CompleteStructureModel", false, false);

    ArrayList<EdgeInfo> structureModelOverlapping (boolean isTracePassed, int i, int j) {

        if (isTracePassed) // simulation result pass
            totalPassed++;
        else
            totalFailed++;

        StructureModel stm = new StructureModel(i, j);
        Graph currentG = stm.collaborationGraph;
        for (Node node: currentG.getEachNode()) {
            if (!searchNode(node.getId(), overlappedG)) {
                overlappedG.addNode(node.getId());
                NodeInfo nodeInfo = new NodeInfo();
                nodeInfo.node = node.getId();
                if (isTracePassed)
                    nodeInfo.pass++;
                else
                    nodeInfo.fail++;
                nodeInfos.add(nodeInfo);
            } else {
                NodeInfo tempNodeInfo = searchNode(node.getId(), nodeInfos);
                if (isTracePassed)
                    tempNodeInfo.pass++;
                else
                    tempNodeInfo.fail++;
            }
        }
        for (Edge edge: currentG.getEachEdge()) {
            if (!searchEdge(edge.getId(), overlappedG)) {
                overlappedG.addEdge(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId());
                EdgeInfo edgeInfo = new EdgeInfo();
                edgeInfo.edge = edge.getId();
                if (isTracePassed)
                    edgeInfo.pass++;
                else
                    edgeInfo.fail++;
                edgeInfos.add(edgeInfo);
            } else {
                EdgeInfo tempEdgeInfo = searchEdge(edge.getId(), edgeInfos);
                if (isTracePassed)
                    tempEdgeInfo.pass++;
                else
                    tempEdgeInfo.fail++;
            }
        }

        return edgeInfos;
    }

    ArrayList<EdgeInfo> SMcalculateSuspiciousness () {
        for (EdgeInfo edgeInfo: edgeInfos) {
            SuspiciousnessMeasure sm = new SuspiciousnessMeasure();
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
        return edgeInfos;
    }

    boolean searchNode(String nodeId, Graph overlappedG) {
        for (Node node: overlappedG.getEachNode()) {
            if (node.getId().equals(nodeId))
                return true;
        }
        return false;
    }

    NodeInfo searchNode (String node, ArrayList<NodeInfo> nodeInfos) {
        for (NodeInfo tempN : nodeInfos) {
            if (tempN.node.equals(node))
                return tempN;
        }
        return null;
    }

    boolean searchEdge (String edgeId, Graph overlappedG) {
        for (Edge edge: overlappedG.getEachEdge()) {
            if (edge.getId().equals(edgeId))
                return true;
        }
        return false;
    }

    EdgeInfo searchEdge (String edge, ArrayList<EdgeInfo> edgeInfos) {
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