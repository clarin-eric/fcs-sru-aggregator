package clarind.fcs;

import java.io.InputStream;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;

public class CenterRegistry {

    final String crStartpoint = "http://130.183.206.32/restxml/";

     private NodeList evaluateXPath(String statement, org.w3c.dom.Document domtree){
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
    
    
    public  ArrayList<Endpoint> getEndpoints() throws Exception {
        ArrayList<Endpoint> ep = new ArrayList<Endpoint>();

        URL u = new URL(crStartpoint);
        InputStream is = u.openStream();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document document = builder.parse(is);

        is.close();
        String instituteName = evaluateXPathToString("//Name", document);
        
        NodeList institutionsUrls = evaluateXPath("//Center_id_link", document);
        
        int i, i2;
        
        for(i=0; i<institutionsUrls.getLength();i++){             
             u = new URL(institutionsUrls.item(i).getTextContent());
             is = u.openStream();
             
             org.w3c.dom.Document doc = builder.parse(is);
             is.close();
             ////WebReference[./Description[text()="CQL"]]/Website                         
             
             NodeList endpointsUrls = evaluateXPath("//WebReference[./Description[text()=\"CQL\"]]/Website", doc);
             
             for(i2=0; i2<endpointsUrls.getLength();i2++){
                 String epUrl = endpointsUrls.item(i2).getTextContent();                 
                 ep.add(new Endpoint(instituteName, epUrl));
             } // for i2
                          
        } // for i ...
        

        return ep;
    } //getEndpoints
    
    public static void main (String[] args) throws Exception {
        CenterRegistry cr = new CenterRegistry();
        ArrayList<Endpoint> ep = cr.getEndpoints();
        
        int i;
        
        for(i=0; i<ep.size();i++){
            System.out.println(ep.get(i).getInstitution() + " " + ep.get(i).getUrl());
        } // for i ...
        
        
    }
}
