package eu.clarin.sru.fcs.aggregator.sopt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Institution. Can have Endpoint children.
 * 
 * @author Yana Panchenko
 */
    public class Institution {

    private String name;
    private String link;
    private ArrayList<Endpoint> endpoints;
    private boolean hasChildrenLoaded = false;
    
    private static final Logger LOGGER = Logger.getLogger(Institution.class.getName());

    public Institution(String name, String link) {
        this.name = name;
        this.link = link;
        this.endpoints = new ArrayList<Endpoint>();
    }

    public String getName() {
        return name;
    }
    
    public String getLink() {
        return link;
    }

    public boolean hasChildrenLoaded() {
        return hasChildrenLoaded;
    }

    public void loadChildren() {
        
        if (hasChildrenLoaded) {
            return;
        }
        
        hasChildrenLoaded = true;
        InputStream is = null;
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            URL u = new URL(link);
            is = u.openStream();
            Document doc = builder.parse(is);
            is.close();
            NodeList endpointsUrls = CenterLiveRegistry
                    .evaluateXPath("//WebReference[./Description[text()=\"CQL\"]]/Website", doc);

            for (int j = 0; j < endpointsUrls.getLength(); j++) {
                String epUrl = endpointsUrls.item(j).getTextContent();
                Endpoint endpoint = new Endpoint(epUrl, this);
                add(endpoint);
            } 
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error accessing endpoint of {0} {1} {2}", 
                    new String[]{link, ex.getClass().getName(), ex.getMessage()});
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
   public List<Endpoint> getChildren() {
        loadChildren();
        return  this.endpoints;
    }
   
    
    public Endpoint getChild(int index) {
        loadChildren();
        if (index >= endpoints.size()) {
            return null;
        }
        return endpoints.get(index);
    }
    
    @Override
    public String toString() {
        if (name != null && name.length() > 0) {
            return name;
        } else {
        return link;
       }
    }

    private void add(Endpoint endpoint) {
        endpoints.add(endpoint);
    }
}
