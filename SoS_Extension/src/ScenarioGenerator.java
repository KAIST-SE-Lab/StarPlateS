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
import java.util.ArrayList;

public class ScenarioGenerator {

    private ArrayList<String> vTypes_;
    private ArrayList<String> rTypes_;

    public ScenarioGenerator() {
        vTypes_ = new ArrayList<>();
        rTypes_ = new ArrayList<>();
    }

    public void generateRandomScenario(int num) {
        extractPools();
        nodeAssign(num);
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
            bw.write("<vehicle_platoon id=\"veh\" type=\"TypeACC\" size=\"6\" route=\"route1\" departPos=\"100\" departLane=\"1\" platoonMaxSpeed=\"0\" pltMgmtProt=\"true\" optSize=\"4\" maxSize=\"10\" />");
            bw.newLine();
            bw.write("<vehicle_platoon id=\"veh1\" type=\"TypeCACC1\" size=\"10\" route=\"route1\" departPos=\"100\" departLane=\"2\" platoonMaxSpeed=\"30\" pltMgmtProt=\"true\" optSize=\"10\" maxSize=\"10\" />");
            bw.write("\n</addNode>");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(bw != null) try {bw.close(); } catch (IOException e) {}
        }


/*
        try {
            File file = new File("../examples/platoon_SoS/addNode.xml");
            DocumentBuilderFactory docBuildFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuild = docBuildFact.newDocumentBuilder();
            Document doc = docBuild.parse(file);

            //Element root = doc.getDocumentElement();
            NodeList root = doc.getChildNodes();

            for(int i = 0; i < num; i++) {
                Element add = doc.createElement("addNode");
                add.setAttribute("id", "add_"+ (i+5));

                Element v1 = doc.createElement("vehicle_platoon");
                v1.setAttribute("id", "veh");
                v1.setAttribute("type", vTypes_.get(0));
                v1.setAttribute("size", "6");
                v1.setAttribute("route", rTypes_.get(0));
                v1.setAttribute("departPos", "100");
                v1.setAttribute("departLane", "1");
                v1.setAttribute("platoonMaxSpeed", "0");
                v1.setAttribute("pltMgmtProt", "true");
                v1.setAttribute("optSize", "4");
                v1.setAttribute("maxSize", "10");

                Element v2 = doc.createElement("vehicle_platoon");
                v2.setAttribute("id", "veh");
                v2.setAttribute("type", vTypes_.get(0));
                v2.setAttribute("size", "6");
                v2.setAttribute("route", rTypes_.get(0));
                v2.setAttribute("departPos", "100");
                v2.setAttribute("departLane", "1");
                v2.setAttribute("platoonMaxSpeed", "0");
                v2.setAttribute("pltMgmtProt", "true");
                v2.setAttribute("optSize", "4");
                v2.setAttribute("maxSize", "10");

                add.appendChild(v1);
                add.appendChild(v2);
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult("../examples/platoon_SoS/addNode.xml");
            t.transform(source, result);

        } catch(Exception e) {
            System.out.println(e);
        }
*/
    }

}
