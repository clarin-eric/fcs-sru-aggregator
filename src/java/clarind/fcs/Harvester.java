package clarind.fcs;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Harvester {

    final String crStartpoint = "http://130.183.206.32/restxml/";

    //https://centerregistry-clarin.esc.rzg.mpg.de/restxml/
    //http://130.183.206.32/restxml/
    
    private NodeList evaluateXPath(String statement, org.w3c.dom.Document domtree) {
        NodeList result = null;

        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            result = (NodeList) xpath.evaluate(statement, domtree, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            System.out.println(ex.getMessage());
        }
        return result;
    }

    public String evaluateXPathToString(String statement, org.w3c.dom.Document domtree) {
        String result = null;

        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            result = (String) xpath.evaluate(statement, domtree, XPathConstants.STRING);
        } catch (XPathExpressionException ex) {
            System.out.println(ex.getMessage());
        }
        return result;
    }

    public ArrayList<Endpoint> getEndpoints() throws Exception {
        ArrayList<Endpoint> ep = new ArrayList<Endpoint>();

        try {
            URL u = new URL(crStartpoint);
            URLConnection urlConn = u.openConnection();

            //HttpsURLConnection urlConn = (HttpsURLConnection) u.openConnection();
            
            urlConn.setConnectTimeout(5000);                                    
            urlConn.setReadTimeout(15000);
            urlConn.setAllowUserInteraction(false);
                                    
            InputStream is = urlConn.getInputStream();


            //InputStream is = u.openStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document document = builder.parse(is);

            is.close();
            String instituteName = evaluateXPathToString("//Name", document);

            NodeList institutionsUrls = evaluateXPath("//Center_id_link", document);

            int i, i2;

            for (i = 0; i < institutionsUrls.getLength(); i++) {
                u = new URL(institutionsUrls.item(i).getTextContent());
                is = u.openStream();

                org.w3c.dom.Document doc = builder.parse(is);
                is.close();
                ////WebReference[./Description[text()="CQL"]]/Website                         

                NodeList endpointsUrls = evaluateXPath("//WebReference[./Description[text()=\"CQL\"]]/Website", doc);

                for (i2 = 0; i2 < endpointsUrls.getLength(); i2++) {
                    String epUrl = endpointsUrls.item(i2).getTextContent();
                    ep.add(new Endpoint(epUrl, instituteName));
                } // for i2

            } // for i ...
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        System.out.println("Number of Endpoints: " + ep.size());
        return ep;
    } //getEndpoints

    public ArrayList<Corpus> getCorporaOfAnEndpoint(String endpointUrl) throws Exception {

        ArrayList<Corpus> corpora = new ArrayList<Corpus>();
        try {
            String urlToCall = endpointUrl + "/?operation=scan&scanClause=fcs.resource&version=1.2";
            URL u = new URL(urlToCall);
            
            URLConnection urlConn = u.openConnection();

            urlConn.setConnectTimeout(5000);                                    
            urlConn.setReadTimeout(15000);
            urlConn.setAllowUserInteraction(false);
                                    
            InputStream is = urlConn.getInputStream();

            System.out.println("getCorporaOfAnEndpoint: " + urlToCall);

            //InputStream is = u.openStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document document = builder.parse(is);

            is.close();

            //http://clarinws.informatik.uni-leipzig.de:8080/CQL?

            //NodeList corporaNodes = evaluateXPath("//*[local-name()='term']/*[local-name()='value']", document);
            NodeList corporaNodes = evaluateXPath("//*[local-name()='terms']/*[local-name()='term']", document);

            int i, i2;
            if (corporaNodes.getLength() > 0) {

                System.out.println("Length of corpora: " + corporaNodes.getLength());

                for (i = 0; i < corporaNodes.getLength(); i++) {
                    Node n = corporaNodes.item(i);

                    System.out.println("NODENAEM: " + n.getNodeName());

                    Corpus c = new Corpus();

                    for (i2 = 0; i2 < n.getChildNodes().getLength(); i2++) {
                        Node child = n.getChildNodes().item(i2);

                        if (child.getNodeName().endsWith("value")) {
                            c.setValue(child.getTextContent());
                        }

                        if (child.getNodeName().endsWith("displayTerm")) {
                            c.setDisplayTerm(child.getTextContent());
                        }

                        if (child.getNodeName().endsWith("numberOfRecords")) {
                            c.setNumberOfRecords(child.getTextContent());
                        }

                    } //for i2

                    corpora.add(c);

                } // for i ...
            } // if coporaNodes ...

            System.out.println("------------");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return corpora;
    }  // getCorporaOfAnEndpoint

    public static void main(String[] args) throws Exception {
        Harvester cr = new Harvester();
        ArrayList<Endpoint> ep = cr.getEndpoints();

        int i;

        for (i = 0; i < ep.size(); i++) {
            System.out.println(ep.get(i).getInstitution() + " " + ep.get(i).getUrl());
        } // for i ...


    }
}
