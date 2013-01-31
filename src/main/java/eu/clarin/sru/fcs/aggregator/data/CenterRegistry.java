/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.clarin.sru.fcs.aggregator.data;

import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;

/**
 * Center registry node. Its children are centers (institutions).
 * 
 * @author Yana Panchenko
 */
public class CenterRegistry implements CorpusTreeNode {
    
    private static final Logger logger = Logger.getLogger(CenterRegistry.class.getName());

    private static final String crStartpoint = "http://130.183.206.32/restxml/";

    //https://centerregistry-clarin.esc.rzg.mpg.de/restxml/
    
    private boolean hasChildrenLoaded = false;
    private List<Institution> centers = new ArrayList<Institution>(); 

    @Override
    public boolean hasChildrenLoaded() {
        return hasChildrenLoaded;
    }

    @Override
    public void loadChildren() {
        //TODO change to use Alex binding for that...
        if (hasChildrenLoaded) {
            return;
        }
        hasChildrenLoaded = true;
        InputStream is = null;
        URL u;
        NodeList instituteNames;
        NodeList institutionsUrls;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        URLConnection urlConn;
        try {
            u = new URL(crStartpoint);
            urlConn = u.openConnection();

            //HttpsURLConnection urlConn = (HttpsURLConnection) u.openConnection();
            
            urlConn.setConnectTimeout(5000);                                    
            urlConn.setReadTimeout(15000);
            urlConn.setAllowUserInteraction(false);
                                    
            is = urlConn.getInputStream();


            //InputStream is = u.openStream();

            builder = factory.newDocumentBuilder();
            org.w3c.dom.Document document = builder.parse(is);

            
            instituteNames = evaluateXPath("//Centername", document);
            institutionsUrls = evaluateXPath("//Center_id_link", document);

            for (int i = 0; i < institutionsUrls.getLength(); i++) {
                String institutionUrl = institutionsUrls.item(i).getTextContent();
                String institutionName = instituteNames.item(i).getTextContent();
                Institution institution = new Institution(institutionName, institutionUrl);
                // display in the tree only those institutions that have CQL endpoints:
                if (!institution.getChildren().isEmpty()) {
                    centers.add(institution);
                }
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error accessing central registry information {0} {1}", new String[]{ex.getClass().getName(), ex.getMessage()});
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, ex.getMessage());
                }
            }
        }
        logger.log(Level.FINE, "Number of Centers: {0}", centers.size());

    }

    @Override
    public List<? extends CorpusTreeNode> getChildren() {
        loadChildren();
        return centers;
    }

    @Override
    public CorpusTreeNode getChild(int index) {
        loadChildren();
        if (index >= centers.size()) {
            return null;
        }
        return centers.get(index);
    }
    
    
    public void addForeignPoint(String endpointUrl, String institutionLink) {
        
        //TODO: ask what functionality is required here??
        
//        boolean added = false;
//        for (Institution center : this.centers) {
//            if (center.getLink().equals(institutionLink)) {
//                EndpointY ep = new EndpointY(endpointUrl, center);
//                center.loadChildren();
//                //center.loadChildren();
//                //ep.loadChildren();
//            }
//        }
//        if (!added) {
//            Institution institution = new Institution("unknown", institutionLink);
//            this.centers.add(institution);
//            //EndpointY ep = new EndpointY(endpointUrl, institution);
//            //this.loadChildren();
//            //institution.loadChildren();
//        }
    }
    
   
    public static NodeList evaluateXPath(String statement, org.w3c.dom.Document domtree) {
        NodeList result = null;

        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            result = (NodeList) xpath.evaluate(statement, domtree, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            System.out.println(ex.getMessage());
        }
        return result;
    }

}
