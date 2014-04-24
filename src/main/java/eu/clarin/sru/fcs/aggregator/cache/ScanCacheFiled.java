package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;
import eu.clarin.sru.fcs.aggregator.sopt.Institution;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for reading/writing scan data (endpoints descriptions) from/to
 * ScanCache from/to local files.
 * 
 * The data saved in a folder into files has the following format:
 * inst.txt lists all the centers and the endpoints that have CQL compliant 
 * resources, with each CQL endpoint top resources assigned a number.
 * Number that represents a resource corresponds to a folder name, inside which 
 * resource info is stored in a file corpus.txt. If a resource has sub-resources, 
 * the folder contains sub-folders, also named by numbers, with their 
 * corresponding corpus.txt files, and so on. 
 * The corpus.txt file contains each piece of info on a separate line. Line 1 
 * is resource endpoint, line 2 - handle, line 3 - display name, line 4 - landing 
 * page, 5 - desription, 6 - languages (multiple languages are separated by | 
 * separator), 7 - number of records.
 *
 * @author yanapanchenko
 */
public class ScanCacheFiled {

    private String scanDirectory;
    private static final String INSTITUTION_ENDPOINTS_FILENAME = "inst.txt";
    private static final String CORPUS_INFO_FILENAME = "corpus.txt";
    private static final String ENCODING = "UTF-8";
    public static final String I = "II";
    public static final String IE = "IE";
    public static final String SEP = "|";
    public static final String NL = "\n";
    public static final String SPACE = " ";
    private static final Logger LOGGER = Logger.getLogger(ScanCacheFiled.class.getName());

    /**
     * Constructs ScanCache/files reading/writing utility.
     * 
     * @param scanDirectory path to local directory were files with 
     * ScanCache data are/should be stored.
     */
    public ScanCacheFiled(String scanDirectory) {
        this.scanDirectory = scanDirectory;
    }

    /**
     * Writes ScanCache data (endpoints and resources descriptions) in a special
     * simple plain text format into the files.
     * 
     * @param cache ScanCache the data of which should be written into the files.
     */
    public void write(ScanCache cache) {

        OutputStreamWriter os = null;
        int epCorpusCounter = 0;
        try {
            clearDir(scanDirectory);
            File sruInstitutionsFile = new File(scanDirectory, INSTITUTION_ENDPOINTS_FILENAME);
            os = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(sruInstitutionsFile)), ENCODING);
            for (Institution institution : cache.getInstitutions()) {
                writeInstitutionInfo(os, institution);
                for (Endpoint endp : institution.getEndpoints()) {
                    for (Corpus corpus : cache.getRootCorporaOfEndpoint(endp.getUrl())) {
                        if (corpus.getHandle() == null || corpus.getHandle().isEmpty()) {
                            //write endpoint info:
                            writeDefaultCorpusInfo(os, corpus);
                        } else {
                            writeEndpointCorpusInfo(epCorpusCounter, os, corpus);
                            writeCorpusData(epCorpusCounter, scanDirectory, corpus, cache);
                        }
                        epCorpusCounter++;
                    }
                }
                os.write(NL);
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    private void writeCorpusData(int corpusNumber, String currentDir, Corpus c, ScanCache cache) {

        File corpusDir = new File(currentDir, corpusNumber + "");
        corpusDir.mkdir();
        
        File corpusInfoFile = new File(corpusDir, CORPUS_INFO_FILENAME);
        OutputStreamWriter os = null;
        int childCounter = 0;
        
        try {
            os = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(corpusInfoFile)), ENCODING);
            writeCorpusInfo(os, c);

            List<Corpus> children = cache.getChildren(c);
            if (children != null) {
                for (Corpus child : children) {
                    writeCorpusData(childCounter, corpusDir.getAbsolutePath(), child, cache);
                    childCounter++;
                }
                //logger.log(Level.INFO, "Found {0} children corpora for {1} {2}", new String[]{"" + corpusToChildren.get(corpus).size(), corpus.getEndpointUrl(), corpus.getHandle()});
            }
            // else if () {
            // TODO if diagnistics came back, try simple scan without the 
            // SRUCQLscan.RESOURCE_INFO_PARAMETER
            //}

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void writeInstitutionInfo(Writer writer, Institution institution) throws IOException {

        writer.write(I);
        writer.write(SEP);
        writer.write(institution.getName());
        writer.write(NL);
    }

    private void writeCorpusInfo(Writer writer, Corpus c) throws IOException {

        writer.write(c.getEndpointUrl());
        writer.write(NL);
        writer.write(c.getHandle());
        writer.write(NL);
        if (c.getDisplayName() != null) {
            writer.write(c.getDisplayName());
        } else {
            writer.write(SPACE);
        }
        writer.write(NL);
        if (c.getLandingPage() != null) {
            writer.write(c.getLandingPage());
        } else {
            writer.write(SPACE);
        }
        writer.write(NL);
        if (c.getDescription() != null) {
            writer.write(c.getDescription());
        } else {
            writer.write(SPACE);
        }
        writer.write(NL);
        boolean hasLangs = false;
        for (String lang : c.getLanguages()) {
            if (hasLangs) {
                writer.write(SEP);
            }
            writer.write(lang);
            hasLangs = true;
        }
        writer.write(NL);
        if (c.getNumberOfRecords() != null) {
            writer.write(c.getNumberOfRecords().toString());
        }
        writer.write(NL);
    }

    private void writeEndpointCorpusInfo(int number, Writer writer, Corpus c) throws IOException {
        writer.write(IE);
        writer.write(SEP);
        writer.write(("" + number));
        writer.write(SEP);
        writer.write(c.getEndpointUrl());
        writer.write(NL);
    }

    private void writeDefaultCorpusInfo(Writer writer, Corpus c) throws IOException {
        writer.write(IE);
        writer.write(SEP);
        writer.write(SPACE);
        writer.write(SEP);
        writer.write(c.getEndpointUrl());
        writer.write(NL);
    }

    /**
     * Reads ScanCache data from the files in scanDirectory directory. The files 
     * contain endpoint and resources descriptions in a special simple plain 
     * text format, resulting from applying write(ScanCache scanCache) method.
     * 
     * @return ScanCache with the data read from the files containing endpoints 
     * and resources descriptions.
     */
    public ScanCache read() {
        SimpleInMemScanCache cache = new SimpleInMemScanCache();
        File sruInstitutionsFile = new File(scanDirectory, INSTITUTION_ENDPOINTS_FILENAME);
        BufferedReader reader = null;
        Set<Institution> institutions = new HashSet<Institution>();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(sruInstitutionsFile), ENCODING));
            String line;
            Institution inst = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    String[] splitted = line.split("\\" + SEP);
                    if (splitted.length == 2 && splitted[0].equals(I)) {
                        inst = new Institution(splitted[1], "");
                        if (!institutions.contains(inst)) {
                            institutions.add(inst);
                            cache.addInstitution(inst);
                        }
                    } else if (inst != null && splitted.length == 3 && splitted[0].equals(IE)) {
                        Endpoint ep = inst.add(splitted[2]);
                        if (!splitted[1].trim().isEmpty()) {
                            // traverse the corresponding dir
                            File corpusDir = new File(scanDirectory, splitted[1]);
                            readAndAddCorpus(corpusDir.getAbsolutePath(), null, inst, cache);
                        } else {
                            Corpus c = new Corpus(inst, ep.getUrl());
                            cache.addCorpus(c);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
        return cache;
    }

    private void readAndAddCorpus(String path, Corpus parentCorpus, Institution inst, SimpleInMemScanCache cache) {
        File corpusFile = new File(path, CORPUS_INFO_FILENAME);
        BufferedReader reader = null;
        Corpus corpus = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), ENCODING));
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {

                    if (lineCount == 0) {
                        corpus = new Corpus(inst, line);
                    } else if (lineCount == 1) {
                        // corpis id/handle
                        corpus.setHandle(line);
                    } else if (lineCount == 2) {
                        // corpis name
                        corpus.setDisplayName(line);
                    } else if (lineCount == 3) {
                        // corpis page
                        corpus.setLandingPage(line);
                    } else if (lineCount == 4) {
                        // corpis description
                        corpus.setDescription(line);
                    } else if (lineCount == 5) {
                        // corpus langs
                        Set<String> langs = new HashSet<String>();
                        for (String lang : line.split("\\" + SEP)) {
                            langs.add(lang);
                        }
                        corpus.setLanguages(langs);
                    } else if (lineCount == 6) {
                        corpus.setNumberOfRecords(Integer.parseInt(line));
                    } 
                }
                lineCount++;
            }
            if (corpus != null) {
                if (parentCorpus == null) {
                    cache.addCorpus(corpus);
                } else {
                    //cache.addCorpus(corpus, parentCorpus.getHandle());
                    cache.addCorpus(corpus, parentCorpus);
                }
                
                File currentDir = new File(path);
                for (File file : currentDir.listFiles()) {
                    if (file.isDirectory() && !file.isHidden()) {
                        readAndAddCorpus(file.getAbsolutePath(), corpus, inst, cache);
                    }
                }
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }


    }

    private void clearDir(String scanDirectory) {
        File file = new File(scanDirectory);
        for (File fileChild : file.listFiles()) {
            boolean deleted = deleteR(fileChild);
            if (!deleted) {
                LOGGER.warning("Could not delete in old cache: " + fileChild.getAbsolutePath());
            }
        }
    }

    public static boolean deleteR(File file) {
        boolean success = false;
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File fileChild : file.listFiles()) {
                    success = deleteR(fileChild);
                    if (!success) {
                        return success;
                    }
                }
            }
            success = file.delete();
        }
        return success;
    }
}
