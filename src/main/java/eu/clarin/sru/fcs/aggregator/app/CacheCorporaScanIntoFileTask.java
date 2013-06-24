package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.client.SRUClientException;
import eu.clarin.sru.client.SRUScanRequest;
import eu.clarin.sru.client.SRUScanResponse;
import eu.clarin.sru.client.SRUTerm;
import eu.clarin.sru.client.SRUThreadedClient;
import eu.clarin.sru.client.fcs.ClarinFCSRecordParser;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryI;
import eu.clarin.sru.fcs.aggregator.sopt.CenterRegistryLive;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import static eu.clarin.sru.fcs.aggregator.sopt.Corpus.ROOT_HANDLE;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusCache;
import eu.clarin.sru.fcs.aggregator.sopt.CorpusModelLive;
import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;
import eu.clarin.sru.fcs.aggregator.sopt.InstitutionI;
import eu.clarin.sru.fcs.aggregator.sopt.Languages;
import eu.clarin.sru.fcs.aggregator.util.SRUCQLscan;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.DefaultTreeNode;

/**
 *
 * @author Yana Panchenko
 */
public class CacheCorporaScanIntoFileTask extends TimerTask {

    private static final Logger logger = Logger.getLogger(CacheCorporaScanTask.class.getName());
    private SRUThreadedClient sruClient;
    private String outputDir;
    public static final String I = "II";
    public static final String IE = "IE";
    public static final String SEP = "|";
    public static final String NL = "\n";
    public static final String SPACE = " ";

    public CacheCorporaScanIntoFileTask(SRUThreadedClient sruClient, String outputDir) {
        this.outputDir = outputDir;
        this.sruClient = sruClient;
    }

    @Override
    public void run() {

        LocalTime date = new LocalTime();
        logger.log(Level.INFO, "STARTING CACHING CORPORA SCAN: {0}", new String[]{date.toString()});

        File sruInstitutionsFile = new File(outputDir + "inst.txt");
        BufferedOutputStream os = null;

        //TODO remember not responding root corpora and come back to them...

        int epCorpusCounter = 0;
        try {
            os = new BufferedOutputStream(new FileOutputStream(sruInstitutionsFile));
            // save institutions that needs to be retrayed later
            CenterRegistryI cr = new CenterRegistryLive();
            List<InstitutionI> institutions = cr.getCQLInstitutions();
            for (InstitutionI institution : institutions) {

                writeInstitutionInfo(os, institution);
                for (Endpoint endp : institution.getEndpoints()) {
                    //if (!endp.getUrl().contains("weblicht")
                    //        && !endp.getUrl().contains("uni-leipzig.de")) {
                    //    continue;
                    //}
                    try {
                        Future<SRUScanResponse> corporaResponse = null;
                        SRUScanRequest corporaRequest = new SRUScanRequest(endp.getUrl());
                        StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
                        scanClause.append("=");
                        scanClause.append(ROOT_HANDLE);
                        corporaRequest.setScanClause(scanClause.toString());
                        corporaRequest.setExtraRequestData(SRUCQLscan.RESOURCE_INFO_PARAMETER, "true");
                        corporaResponse = sruClient.scan(corporaRequest);
                        Thread.sleep(3000);
                        SRUScanResponse response = corporaResponse.get(600, TimeUnit.SECONDS);
                        Corpus c;
                        if (response != null && response.hasTerms()) {
                            for (SRUTerm term : response.getTerms()) {
                                c = new Corpus(institution, endp.getUrl());
                                c.setHandle(term.getValue());
                                c.setDisplayName(term.getDisplayTerm());
                                c.setNumberOfRecords(term.getNumberOfRecords());
                                addExtraInfo(c, term);
                                writeEndpointCorpusInfo(epCorpusCounter, os, c);
                                addCorpusData(epCorpusCounter, outputDir, c);
                                epCorpusCounter++;
                            }
                        } else {
                            c = new Corpus(endp.getInstitution(), endp.getUrl());
                            //write endpoint info:
                            writeDefaultCorpusInfo(os, c);
                        }
                    } catch (SRUClientException ex) {
                        logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                                new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                    } catch (InterruptedException ex) {
                        logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                                new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                    } catch (ExecutionException ex) {
                        logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                                new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                    } catch (TimeoutException ex) {
                        logger.log(Level.SEVERE, "Timeout scanning corpora {0} at {1} {2} {3}",
                                new String[]{ROOT_HANDLE, endp.getUrl(), ex.getClass().getName(), ex.getMessage()});
                    }

                }
                os.write(NL.getBytes());
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CacheCorporaScanIntoFileTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CacheCorporaScanIntoFileTask.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    Logger.getLogger(CacheCorporaScanIntoFileTask.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }




        //}
        //TODO log problems and time taken
        date = new LocalTime();

        logger.log(Level.INFO,
                "FINISHED CACHING CORPORA SCAN: {0}", new String[]{date.toString()
        });
    }

    private void addExtraInfo(Corpus c, SRUTerm term) {

        DocumentFragment extraInfo = term.getExtraTermData();
        String enDescription = null;
        if (extraInfo != null) {
            NodeList infoNodes = extraInfo.getChildNodes().item(0).getChildNodes();
            for (int i = 0; i < infoNodes.getLength(); i++) {
                Node infoNode = infoNodes.item(i);
                if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("LandingPageURI")) {
                    c.setLandingPage(infoNode.getTextContent().trim());
                } else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Languages")) {
                    NodeList languageNodes = infoNode.getChildNodes();
                    for (int j = 0; j < languageNodes.getLength(); j++) {
                        if (languageNodes.item(j).getNodeType() == Node.ELEMENT_NODE && languageNodes.item(j).getLocalName().equals("Language")) {
                            Element languageNode = (Element) languageNodes.item(j);
                            String languageText = languageNode.getTextContent().trim();
                            if (!languageText.isEmpty()) {
                                c.addLanguage(languageText.trim());
                            }
                        }

                    }
                } else if (infoNode.getNodeType() == Node.ELEMENT_NODE && infoNode.getLocalName().equals("Description")) {
                    Element element = (Element) infoNode;
                    c.setDescription(infoNode.getTextContent().trim());
                    //String lang = element.getAttributeNS("http://clarin.eu/fcs/1.0/resource-info", "lang");
                    //System.out.println("ATTRIBUTE LANG: " + lang);
                    if ("en".equals(element.getAttribute("xml:lang"))) {
                        enDescription = infoNode.getTextContent().trim();
                    }
                }
            }
            // description in Engish has priority
            if (enDescription != null && !enDescription.isEmpty()) {
                c.setDescription(enDescription);
            }
        }
    }

    private void writeInstitutionInfo(BufferedOutputStream os, InstitutionI institution) throws IOException {
        // write institute info

        os.write(I.getBytes());
        os.write(SEP.getBytes());
        os.write(institution.getName().getBytes());
        os.write(NL.getBytes());
    }

    private void writeCorpusInfo(BufferedOutputStream os, Corpus c) throws IOException {

        os.write(c.getEndpointUrl().getBytes());
        os.write(NL.getBytes());
        os.write(c.getHandle().getBytes());
        os.write(NL.getBytes());
        if (c.getDisplayName() != null) {
            os.write(c.getDisplayName().getBytes());
        } else {
            os.write(SPACE.getBytes());
        }
        os.write(NL.getBytes());
        if (c.getLandingPage() != null) {
            os.write(c.getLandingPage().getBytes());
        } else {
            os.write(SPACE.getBytes());
        }
        os.write(NL.getBytes());
        if (c.getDescription() != null) {
            os.write(c.getDescription().getBytes());
        } else {
            os.write(SPACE.getBytes());
        }
        os.write(NL.getBytes());
        boolean hasLangs = false;
        for (String lang : c.getLanguages()) {
            if (hasLangs) {
                os.write(SEP.getBytes());
            }
            os.write(lang.getBytes());
            hasLangs = true;
        }
        os.write(NL.getBytes());
    }

    private void writeEndpointCorpusInfo(int number, BufferedOutputStream os, Corpus c) throws IOException {
        os.write(IE.getBytes());
        os.write(SEP.getBytes());
        os.write(("" + number).getBytes());
        os.write(SEP.getBytes());
        os.write(c.getEndpointUrl().getBytes());
        os.write(NL.getBytes());
    }

    private void writeDefaultCorpusInfo(BufferedOutputStream os, Corpus c) throws IOException {
        os.write(IE.getBytes());
        os.write(SEP.getBytes());
        os.write(SPACE.getBytes());
        os.write(SEP.getBytes());
        os.write(c.getEndpointUrl().getBytes());
        os.write(NL.getBytes());
    }

    private void addCorpusData(int corpusNumber, String currentDir, Corpus c) {
        String corpusDirPath = currentDir + corpusNumber + "/";
        File corpusDir = new File(corpusDirPath);
        corpusDir.mkdir();
        addCorpusData(corpusDirPath, c);
    }

    private void addCorpusData(String corpusDirPath, Corpus c) {
        File corpusInfoFile = new File(corpusDirPath + "corpus.txt");
        BufferedOutputStream os = null;
        int childCounter = 0;
        try {
            os = new BufferedOutputStream(new FileOutputStream(corpusInfoFile));
            writeCorpusInfo(os, c);
            try {
                SRUScanRequest corporaRequest = new SRUScanRequest(c.getEndpointUrl());
                StringBuilder scanClause = new StringBuilder(SRUCQLscan.RESOURCE_PARAMETER);
                scanClause.append("=");
                String resourceValue = c.getHandle();
                if (Corpus.HANDLE_WITH_SPECIAL_CHARS.matcher(resourceValue).matches()) {
                    resourceValue = "%22" + resourceValue + "%22";
                }
                scanClause.append(resourceValue);
                corporaRequest.setScanClause(scanClause.toString());
                Future<SRUScanResponse> corporaResponse = sruClient.scan(corporaRequest);
                Thread.sleep(1000);
                SRUScanResponse response = corporaResponse.get(200, TimeUnit.SECONDS);
                if (response != null && response.hasTerms()) {
                    for (SRUTerm term : response.getTerms()) {
                        if (c.getHandle().equals(term.getValue())) {
                            continue;
                        }
                        Corpus childCorpus = new Corpus(c.getInstitution(), c.getEndpointUrl());
                        childCorpus.setHandle(term.getValue());
                        childCorpus.setDisplayName(term.getDisplayTerm());
                        childCorpus.setNumberOfRecords(term.getNumberOfRecords());
                        //writeCorpusInfo(os, c);
                        addCorpusData(childCounter, corpusDirPath, childCorpus);
                        childCounter++;
                        //TODO temp to make sure it works
                        //break;
                    }
                    //logger.log(Level.INFO, "Found {0} children corpora for {1} {2}", new String[]{"" + corpusToChildren.get(corpus).size(), corpus.getEndpointUrl(), corpus.getHandle()});
                }
            } catch (SRUClientException ex) {
                logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                        new String[]{c.getHandle(), c.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                        new String[]{c.getHandle(), c.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
            } catch (ExecutionException ex) {
                logger.log(Level.SEVERE, "Error accessing corpora {0} at {1} {2} {3}",
                        new String[]{c.getHandle(), c.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
            } catch (TimeoutException ex) {
                logger.log(Level.SEVERE, "Timeout scanning corpora {0} at {1} {2} {3}",
                        new String[]{c.getHandle(), c.getEndpointUrl(), ex.getClass().getName(), ex.getMessage()});
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CacheCorporaScanIntoFileTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CacheCorporaScanIntoFileTask.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    Logger.getLogger(CacheCorporaScanIntoFileTask.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public static void main(String[] args) {
        SRUThreadedClient sruClient = new SRUThreadedClient();
        sruClient.registerRecordParser(new ClarinFCSRecordParser());


        //TODO do caching
        CacheCorporaScanIntoFileTask task = new CacheCorporaScanIntoFileTask(sruClient, "/Users/yanapanchenko/Documents/Work/temp/agca-w/");
        task.run();
        sruClient.shutdown();
    }
}
