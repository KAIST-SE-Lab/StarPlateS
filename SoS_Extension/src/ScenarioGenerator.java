import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class ScenarioGenerator {
    public void generateRandomScenario() {
        extractNodePools();
    }

    private void extractNodePools() {

        try {
            File file = new File("../examples/platoon_SoS/sumocfg/4hello.rou.xml");
            DocumentBuilderFactory docBuildFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuild = docBuildFact.newDocumentBuilder();
            Document doc = docBuild.parse(file);
            doc.getDocumentElement().normalize();

            System.out.println("Root element : " + doc.getDocumentElement().getNodeName());
            System.out.println();

        } catch(Exception e) {
            System.out.println(e);
        }
    }

}
