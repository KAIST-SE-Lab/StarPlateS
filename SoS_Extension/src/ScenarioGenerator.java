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
import java.util.concurrent.ConcurrentHashMap;

public class ScenarioGenerator {

    private ArrayList<String> vTypes_;
    private ArrayList<String> rTypes_;
    private Integer laneNum_;
    private ConcurrentHashMap<String, String> conditions_;
    private ArrayList<String> plEvents_;

    public ScenarioGenerator() {
        vTypes_ = new ArrayList<>();
        rTypes_ = new ArrayList<>();
        laneNum_ = 3;
        conditions_ = new ConcurrentHashMap<>();
        plEvents_ = new ArrayList<String>();

        // Platoon Events assign
        plEvents_.add("optSize");
        plEvents_.add("pltMerge");
        plEvents_.add("pltLeave");
        plEvents_.add("pltSplit");

    }

    public void generateRandomScenario(int num) {
        extractPools();

        generateTrafficControl(150, num);
    }

    private void extractPools() {

        try {
            File file = new File("./examples/platoon_SoS/sumocfg/4hello.rou.xml");
            DocumentBuilderFactory docBuildFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuild = docBuildFact.newDocumentBuilder();
            Document doc = docBuild.parse(file);
            doc.getDocumentElement().normalize();

            //System.out.println("Root element : " + doc.getDocumentElement().getNodeName());
            //System.out.println();

            NodeList vlist = doc.getElementsByTagName("vType");
            NodeList rlist = doc.getElementsByTagName("route");

            for(int i = 0; i < vlist.getLength(); i++) {
                this.vTypes_.add(vlist.item(i).getAttributes().item(0).getNodeValue());
                //System.out.println(vTypes_.get(i));
            }

            for(int i = 0; i < rlist.getLength(); i++) {
                this.rTypes_.add(rlist.item(i).getAttributes().item(1).getNodeValue());
                //System.out.println(rTypes_.get(i));
            }

            //System.out.println(vlist.item(0).getAttributes().item(1));

        } catch(Exception e) {
            System.out.println(e);
        }
    }

    private void nodeAssign() {
        //File inFile = new File("../examples/platoon_SoS/addNode.xml");
        File outFile = new File("./examples/platoon_SoS/addNode.xml");

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
        ret += "period=\"" + 5 + "\"  />";
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

    private void generateTrafficControl(int end, int numScenario) {
        File outFile = new File("./examples/platoon_SoS/trafficControl.xml");

            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(outFile));
                bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                bw.newLine();
                bw.newLine();

                for(int i = 0; i < numScenario; i++) {
                    // Generate addNode.xml
                    nodeAssign();
                    conditions_.put("leaves", "");

                    bw.write("<trafficControl id=\"example_" + i + "\">\n");

                    for (int t = 5; t <= end; t += 30) {
                        // Change speed of all Platoons assigned as nodes for starting simulation
                        if (t == 5) {
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

                        if (!availabilityCheck(selectedEvent)) continue;
                        System.out.println(selectedEvent);

                        String ret = assignEvent(bw, selectedEvent, t, r);
                        String[] rets = ret.split("/");
                        System.out.println(ret);

                        updateConditions(rets[0], selectedEvent, rets[1]);
                        System.out.println(conditions_);
                    }

                    bw.write("\n</trafficControl>\n\n");
                    conditions_.clear();
                }
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bw != null) try {
                    bw.close();
                } catch (IOException e) {
                }
            }
    }

    private void updateConditions(String id, String event, String attr) {
        switch (event) {
            case "plin": { // attr: platoon size
                conditions_.put(id, attr);
                break;
            }
            case "optSize": { // attr: optimal size
                for (String key : conditions_.keySet()) {
                    if (key.contains("veh1") && Integer.parseInt(conditions_.get(key)) > Integer.parseInt(attr)) {
                        int orgSize = Integer.parseInt(conditions_.get(key));

                        // update existing "veh1"
                        conditions_.replace(key, attr);
                        conditions_.put("veh1." + attr, String.valueOf(orgSize - Integer.parseInt(attr)));
                    }
                    else if (key.contains("veh") && Integer.parseInt(conditions_.get(key)) > Integer.parseInt(attr)) {
                        int orgSize = Integer.parseInt(conditions_.get(key));
                        //System.out.println("veh1 optSize");

                        // update existing platoons
                        conditions_.replace(key, attr);
                        conditions_.put("veh." + attr, String.valueOf(orgSize - Integer.parseInt(attr)));
                    }
                }
                break;
            }
            case "pltMerge": { // attr: none / id: assume that id should contain "."
                String mergeTarget = "";
                int mergeComp = -1;

                StringTokenizer st = new StringTokenizer(id, ".");
                String pre_id = st.nextToken();
                int post_id = Integer.parseInt(st.nextToken());

                // Finding the nearest, in front of  platoon id from the target
                for(String key: conditions_.keySet()) {
                    if(key.contains(pre_id) && !(key == id)) {
                        String[] values = key.split(".");
                        int post_key_id;

                        if(values.length > 1) {
                            post_key_id = Integer.parseInt(values[1]);
                        } else {
                            post_key_id = 0;
                        }

                        if(post_key_id < post_id && mergeComp < post_key_id) {
                            mergeComp = post_key_id;
                        }
                    }
                }

                int orgSize = Integer.parseInt(conditions_.get(id));
                if(mergeComp == 0) {
                    mergeTarget = pre_id;
                } else {
                    mergeTarget = pre_id + "." + mergeComp;
                }

                conditions_.replace(mergeTarget, String.valueOf(Integer.parseInt(conditions_.get(mergeTarget)) + orgSize));
                conditions_.remove(id);

                break;
            }
            case "pltLeave": { // attr: Leave index
                conditions_.replace(id, String.valueOf(Integer.parseInt(conditions_.get(id)) - 1));
                String leaves = conditions_.get("leaves");
                System.out.println(id);

                if(id.contains(".")) {
                    //String[] tmp = id.split(".");
                    StringTokenizer st = new StringTokenizer(id, ".");
                    //leaves += tmp[0] + "." + (Integer.parseInt(tmp[1]) + Integer.parseInt(attr)) + ",";
                    leaves += st.nextToken() + "." + (Integer.parseInt(st.nextToken())) + "_" + Integer.parseInt(attr) + "/";
                } else {
                    leaves += id + "_" + attr + "/";
                }

                conditions_.replace("leaves", leaves);
                break;
            }
            case "pltSplit": { // attr: Split index
                int orgSize = Integer.parseInt(conditions_.get(id));

                int start_id = 0;
                int split_id = Integer.parseInt(attr);

                if(id.contains(".")) {
                    //System.out.println(id);
                    StringTokenizer st = new StringTokenizer(id, ".");
                    String pre_id = st.nextToken();
                    start_id = Integer.valueOf(st.nextToken());

                    //System.out.println(split_id - start_id);
                    conditions_.replace(id, String.valueOf(split_id));
                    start_id += split_id;
                    conditions_.put(pre_id + "." + start_id, String.valueOf(orgSize - split_id));
                } else {
                    //System.out.println(split_id - start_id);
                    conditions_.replace(id, String.valueOf(split_id - start_id));
                    conditions_.put(id + "." + attr, String.valueOf(orgSize - (split_id - start_id)));
                }
                break;
            }
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
                    if (key.contains("veh.") || key.contains("veh1.")) {
                        count++;
                    }
                }
                if (count >= 1) return true;
                else return false;
            }
            case "pltLeave":
            case "pltSplit": {
                for (String key : conditions_.keySet()) {
                    if (key.contains("veh") && Integer.parseInt(conditions_.get(key)) > 1) {
                        return true;
                    }
                }
                return false;
            }
        }
        return ret;
    }

    private String assignEvent(BufferedWriter bw, String event, int begin, Random r) {

        String sV = "";
        int splitIndex = 0;

        try {
            bw.newLine();

            switch (event) {
                case "optSize": {                                         // Optimal size of platoon: 2 ~ 6
                    int ret = (r.nextInt(5) + 4);
                    bw.write("<optSize begin=\"" + begin +"\" value=\"" + ret + "\" /> \n");
                    return " " + "/" + ret;
                }
                case "pltMerge": {
                    ArrayList<String> vehs = new ArrayList<>();
                    for(String key : conditions_.keySet()) {
                        if(key.contains("veh.") || key.contains("veh1.")) vehs.add(key);
                    }
                    sV = vehs.get(r.nextInt(vehs.size()));
                    bw.write("<pltMerge pltId=\"" + sV + "\" begin=\"" + begin + "\" />\n");
                    return sV + "/ ";
                }
                case "pltLeave": {
                    ArrayList<String> vehs = new ArrayList<>();
                    for(String key : conditions_.keySet()) {
                        if(key.contains("veh") && Integer.parseInt(conditions_.get(key)) > 1) vehs.add(key);
                    }
                    sV = vehs.get(r.nextInt(vehs.size()));
                    String[] values = sV.split(".");

                    boolean t_f = true;
                    int leaveIndex = 0;
                    while(t_f) {
                        leaveIndex = r.nextInt(Integer.parseInt(conditions_.get(sV)));
                        String[] leaves = conditions_.get("leaves").split(",");

                        t_f = false;
                        for(String s : leaves) {
                            if(s.equals(sV + "." + leaveIndex)) t_f = true; // Same Leave Scenario generated
                        }
                    }

                    bw.write("<pltLeave pltId=\"" + sV  +
                            "\" leaveIndex=\"" + leaveIndex +
                            "\" begin=\"" + begin + "\"/> \n");
                    return sV + "/" + leaveIndex;
                }
                case "pltSplit": {
                    ArrayList<String> vehs = new ArrayList<>();
                    for(String key : conditions_.keySet()) {
                        if(key.contains("veh") && Integer.parseInt(conditions_.get(key)) > 1) vehs.add(key);
                    }
                    sV = vehs.get(r.nextInt(vehs.size()));
                    splitIndex = 1 + r.nextInt(Integer.parseInt(conditions_.get(sV))-1);
                    //sV += "." + r.nextInt(Integer.parseInt(conditions_.get(sV)));
                    //bw.write("<pltSplit splitVehId=\"" + sV + "\" begin=\"" + begin + "\" /> \n");
                    bw.write("<pltSplit pltId=\"" + sV  + "\" splitIndex=\"" +
                            splitIndex + "\" begin=\"" + begin + "\" />\n");

                    return sV + "/" + splitIndex;
                }
            }

        } catch(Exception e) {
            System.out.println(e);
        }

        return sV + "/" + splitIndex;
    }

}
