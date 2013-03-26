package eu.clarin.sru.fcs.aggregator.data;

import eu.clarin.sru.fcs.aggregator.sparam.CorpusTreeNode;
import eu.clarin.sru.fcs.aggregator.data.CenterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Institution node. Can have Endpoint children.
 * 
 * @author Yana Panchenko
 */
public class InstitutionForTesting extends Institution {

    private List<Endpoint> endpoints = new ArrayList<Endpoint>();
    private boolean hasChildrenLoaded = false;
    
    private static final String[] testEndpoints = new String[]{"http://lux17.mpi.nl/cqltest"};
    
    private static final Logger logger = Logger.getLogger(Institution.class.getName());

    public InstitutionForTesting(String name, String link) {
        super(name,link);
    }

    

    @Override
    public boolean hasChildrenLoaded() {
        return hasChildrenLoaded;
    }

    @Override
    public void loadChildren() {
        
        if (hasChildrenLoaded) {
            return;
        }
        hasChildrenLoaded = true;
        
        for (int j = 0; j < testEndpoints.length; j++) {
                String epUrl = testEndpoints[j];
                Endpoint endpoint = new Endpoint(epUrl, this);
                endpoints.add(endpoint);
        } 
        
    }

    @Override
    public List<? extends CorpusTreeNode> getChildren() {
        loadChildren();
        return  this.endpoints;
    }

    @Override
    public CorpusTreeNode getChild(int index) {
        loadChildren();
        if (index >= endpoints.size()) {
            return null;
        }
        return endpoints.get(index);
    }
    

}
