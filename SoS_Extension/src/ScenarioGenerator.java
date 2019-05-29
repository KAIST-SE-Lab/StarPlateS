import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.*;

public class ScenarioGenerator {

    private ArrayList<String> vTypes_;
    private ArrayList<String> rTypes_;
    private Integer laneNum_;
    private HashMap<String, String> conditions_;
    private ArrayList<String> plEvents_;

    public ScenarioGenerator() {
        vTypes_ = new ArrayList<>();
        rTypes_ = new ArrayList<>();
        laneNum_ = 3;
        conditions_ = new HashMap<>();
        plEvents_ = new ArrayList<String>();

        // Platoon Events assign
        plEvents_.add("optSize");
        plEvents_.add("pltMerge");
        plEvents_.add("pltLeave");
        plEvents_.add("pltSplit");
    }

    public void generateRandomScenario(int num) {
        extractPools();
        nodeAssign(num);

        generateTrafficControl(150);

        System.out.println(this.conditions_);
    }

    private void extractPools() {

        try {
            File file = new File("../examples/platoon_SoS/sumocfg/4hello.rou.xml");
            DocumentBuilderFactory docBuildFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuild = docBuildFact.newDocumentBuilder();
            Document doc = docBuild.parse(file);
            doc.getDocumentElement().normalize();

            System.out.println("Root element : " + doc.getDocumentElement().getNodeName());
            System.out.println();

            NodeList vlist = doc.getElementsByTagName("vType");
            NodeList rlist = doc.getElementsByTagName("route");

            for(int i = 0; i < vlist.getLength(); i++) {
                this.vTypes_.add(vlist.item(i).getAttributes().item(0).getNodeValue());
                System.out.println(vTypes_.get(i));
            }

            for(int i = 0; i < rlist.getLength(); i++) {
                this.rTypes_.add(rlist.item(i).getAttributes().item(1).getNodeValue());
                System.out.println(rTypes_.get(i));
            }

            //System.out.println(vlist.item(0).getAttributes().item(1));

        } catch(Exception e) {
            System.out.println(e);
        }
    }

    private void nodeAssign(int num) {
        //File inFile = new File("../examples/platoon_SoS/addNode.xml");
        File outFile = new File("../examples/platoon_SoS/addNode.xml");

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(outFile));
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            bw.newLine();
            bw.newLine();
            bw.write("<addNode id=\"example_0\">\n");
            bw.write(platoonInsert("veh"));
            bw.newLine();
            bw.write(platoonInsert("veh1"));
            bw.newLine();
            bw.write(vFlowInsert("flow1"));
            bw.write("\n</addNode>");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(bw != null) try {bw.close(); } catch (IOException e) {}
        }
    }

    private String platoonInsert(String id) {
        String ret = "<vehicle_platoon id=\"" + id +"\" ";
        ret += "type=\"" + vTypes_.get(2) + "\" ";
        ret += "size=\""+ 6 + "\" ";

        String tmp = routeConditionChecker(id);
        StringTokenizer st = new StringTokenizer(tmp, "_"); // return: route_lane

        ret += "route=\"" + st.nextToken() + "\" ";
        ret += "departPos=\"" + 100 + "\" ";
        ret += "departLane=\"" + st.nextToken() + "\" ";
        ret += "platoonMaxSpeed=\"" + 0 + "\" ";
        ret += "pltMgmtProt=\"" + "true" + "\" ";
        ret += "optSize=\"" + 4 + "\" ";
        ret += "maxSize=\"" + 10 + "\" />";

        String vehicles = "";
        for(int i = 0; i < 6; i++) vehicles += i + " ";

        updateConditions(id, "plin", Integer.toString(6));

        return ret;
    }

    private String vFlowInsert(String id) {
        String ret = "<vehicle_flow id=\"" + id +"\" ";

        ret += "type=\"" + vTypes_.get(1) + "\" ";
        ret += "color=\"" + "gold" + "\" ";
        ret += "route=\"" + "route1" + "\" ";
        ret += "begin=\"" + 5 + "\" ";
        ret += "end=\"" + 200 + "\" ";
        ret += "distribution=\"" + "deterministic" + "\" ";
        ret += "period=\"" + 1 + "\"  />";
        return ret;
    }

    private String routeConditionChecker(String id) {
        long seed = System.currentTimeMillis();
        Random r = new Random(seed);
        String tmp;

        do {
            tmp = "";
            tmp += rTypes_.get(r.nextInt(rTypes_.size()));
            tmp += "_" + r.nextInt(laneNum_);
        } while(conditions_.containsKey(tmp));

        conditions_.put(tmp, id);

        return tmp;
    }

    private void generateTrafficControl(int end) {
        File outFile = new File("../examples/platoon_SoS/trafficControl.xml");

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(outFile));
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            bw.newLine();
            bw.newLine();
            bw.write("<trafficControl id=\"example_0\">\n");

            for(int t = 5; t <= end; t+=10) {
                // Change speed of all Platoons assigned as nodes for starting simulation
                if(t == 5) {
                    for (String key : conditions_.keySet()) {
                        if (key.contains("veh")) {
                            bw.newLine();
                            bw.write("<speed id=\"" + key + "\" begin=\"" + t + "\" value=\"20\" />  ");
                        }
                    }
                    continue;
                }

                Random r = new Random();

                // Select random event and check availability
                String selectedEvent = plEvents_.get(r.nextInt(plEvents_.size()));
                System.out.println(selectedEvent);
                if(!availabilityCheck(selectedEvent)) continue;

                assignEvent(bw, selectedEvent, t, r);
            }

            bw.write("\n</trafficControl>");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(bw != null) try {bw.close(); } catch (IOException e) {}
        }
    }

    private void updateConditions(String id, String event, String attr) {
        switch (event) {
            case "plin":
                conditions_.put(id, attr);
                break;
        }
    }

    private boolean availabilityCheck(String event) {
        boolean ret = false;
        switch (event) {
            case "optSize": {
                return true;
            }
            case "pltMerge": {
                int count = 0;
                for (String key : conditions_.keySet()) {
                    if (key.contains("veh.")) {
                        count++;
                    }
                }
                System.out.println("PLTMERGE: " + count);
                if (count >= 2) return true;
                else return false;
            }
            case "pltLeave":
            case "pltSplit": {
                for (String key : conditions_.keySet()) {
                    if (key.contains("veh") && Integer.parseInt(conditions_.get(key)) > 1) {
                        System.out.println("SPLIT");
                        return true;
                    }
                }
                return false;
            }
        }
        return ret;
    }

    private void assignEvent(BufferedWriter bw, String event, int begin, Random r) {

        try {
            bw.newLine();

            switch (event) {
                case "optSize": {                                         // Optimal size of platoon: 2 ~ 6
                    bw.write("<optSize begin=\"" + begin +"\" value=\"" + (r.nextInt(5) + 2) + "\" /> \n");
                    break;
                }
                case "pltMerge": {
                    ArrayList<String> vehs = new ArrayList<>();
                    for(String key : conditions_.keySet()) {
                        if(key.contains("veh.")) vehs.add(key);
                    }
                    String sV = vehs.get(r.nextInt(vehs.size()));
                    bw.write("<pltMerge pltId=\"" + sV + "\" begin=\"" + begin + "\" />");
                    break;
                }
                case "pltLeave": {
                    ArrayList<String> vehs = new ArrayList<>();
                    for(String key : conditions_.keySet()) {
                        if(key.contains("veh")) vehs.add(key);
                    }
                    String sV = vehs.get(r.nextInt(vehs.size()));
                    bw.write("<pltLeave pltId=\"" + sV  +
                            "\" leaveIndex=\"" + r.nextInt(Integer.parseInt(conditions_.get(sV))) +
                            "\" begin=\"" + begin + "\"/> \n");
                    break;
                }
                case "pltSplit": {
                    ArrayList<String> vehs = new ArrayList<>();
                    for(String key : conditions_.keySet()) {
                        if(key.contains("veh")) vehs.add(key);
                    }
                    String sV = vehs.get(r.nextInt(vehs.size()));
                    sV += "." + r.nextInt(Integer.parseInt(conditions_.get(sV)));
                    bw.write("<pltSplit splitVehId=\"" + sV + "\" begin=\"" + begin + "\" /> \n");
                    break;
                }
            }

        } catch(Exception e) {
            System.out.println(e);
        }

    }

}
