package utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.HashMap;
import java.util.Map;

public class XMLParser {

    private Map<String, String> parameters;
    private Map<String, String> filePaths;

    public XMLParser(String xmlFilePath) {
        try {
            // Create a DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Create a DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Parse the XML file and build the Document
            Document document = builder.parse(xmlFilePath);

            // Normalize the XML structure
            document.getDocumentElement().normalize();

            // Initialize maps
            parameters = new HashMap<>();
            filePaths = new HashMap<>();

            // Store parameters in a map
            NodeList paramList = document.getElementsByTagName("parameters").item(0).getChildNodes();
            for (int i = 0; i < paramList.getLength(); i++) {
                Node node = paramList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element param = (Element) node;
                    parameters.put(param.getTagName(), param.getTextContent());
                }
            }

            // Store file paths in a map
            NodeList filePathList = document.getElementsByTagName("filePaths").item(0).getChildNodes();
            for (int i = 0; i < filePathList.getLength(); i++) {
                Node node = filePathList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element filePath = (Element) node;
                    filePaths.put(filePath.getTagName(), filePath.getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getFilePaths() {
        return filePaths;
    }

    public SimulationConfig createScenario() {
        return new SimulationConfig(
            Integer.parseInt(parameters.get("MAX_PARKING_EPISODES")),
            Integer.parseInt(parameters.get("TIME_BIN_COUNT")),
            Integer.parseInt(parameters.get("ROUND_TRIP_COUNT")),
            Float.parseFloat(parameters.get("LOCATION_PROPOSAL_WEIGHT")),
            Float.parseFloat(parameters.get("DEPARTURE_PROPOSAL_WEIGHT")),
            Integer.parseInt(parameters.get("WEIGHT")),
            Float.parseFloat(parameters.get("MINIMAL_DURATION_AT_HOME")),
            Float.parseFloat(parameters.get("MINIMAL_DURATION_AT_WORK")),
            Float.parseFloat(parameters.get("AT_HOME_INTERVAL_LENGTH")),
            Float.parseFloat(parameters.get("AT_WORK_INTERVAL_LENGTH")),
            Float.parseFloat(parameters.get("MORNING_START")),
            Float.parseFloat(parameters.get("EVENING_END")),
            Integer.parseInt(parameters.get("LOGGER_INTERVAL")),
            Integer.parseInt(parameters.get("MESSAGE_INTERVAL")),
            filePaths.get("DISTRICTS_HOME_LOCATIONS"),
            filePaths.get("DISTRICTS_WORK_LOCATIONS"),
            filePaths.get("DISTRICTS_OD"),
            filePaths.get("DISTRICTS_SKIM_H"),
            filePaths.get("DISTRICTS_SKIM_KM")
        );
    }
}

