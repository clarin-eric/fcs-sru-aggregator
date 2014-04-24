package eu.clarin.sru.fcs.aggregator.sopt;

import eu.clarin.sru.fcs.aggregator.cache.ScanCacheI;
import eu.clarin.sru.fcs.aggregator.app.CacheCorporaScanIntoFileTask;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Yana Panchenko
 */
@Deprecated
public class CorporaScanCache implements ScanCacheI {

    private Map<String, List<Corpus>> enpUrlToRootCorpora = new LinkedHashMap<String, List<Corpus>>(30);
    private Map<Corpus, List<Corpus>> corpusToChildren = new HashMap<Corpus, List<Corpus>>();
    private Map<String, Set<Corpus>> langToCorpora = new HashMap<String, Set<Corpus>>();
    
    private static final String ENCODING = "UTF-8";

    public CorporaScanCache(
            Map<String, List<Corpus>> enpUrlToRootCorpora,
            Map<Corpus, List<Corpus>> corpusToChildren,
            Map<String, Set<Corpus>> langToCorpora) {

        this.enpUrlToRootCorpora.putAll(enpUrlToRootCorpora);
        this.corpusToChildren.putAll(corpusToChildren);
        this.langToCorpora.putAll(langToCorpora);

    }

    public CorporaScanCache(String corporaScanDir) {
        readCache(corporaScanDir);
    }

    public Map<String, Set<Corpus>> getLangToCorpora() {
        return langToCorpora;
    }

    public boolean isEmpty() {
        return enpUrlToRootCorpora.isEmpty();
    }

    public List<Corpus> getRootCorpora() {
        List<Corpus> rootCorpora = new ArrayList<Corpus>(enpUrlToRootCorpora.size());
        for (List<Corpus> corpora : this.enpUrlToRootCorpora.values()) {
            rootCorpora.addAll(corpora);
        }
        return rootCorpora;
    }
    
    

    @Override
    public List<Corpus> getRootCorporaOfEndpoint(String enpointUrl) {
        return this.enpUrlToRootCorpora.get(enpointUrl);
    }

    public List<Corpus> getRootCorporaForLang(String lang) {
        List<Corpus> rootCorpora = new ArrayList<Corpus>(enpUrlToRootCorpora.size());
        for (List<Corpus> corpora : this.enpUrlToRootCorpora.values()) {
            for (Corpus corpus : corpora) {
                if (corpus.getLanguages().contains(lang)) {
                    rootCorpora.add(corpus);
                }
            }
        }
        return rootCorpora;
    }

    @Override
    public Set<String> getLanguages() {
        Set<String> languages = new HashSet<String>(this.langToCorpora.size());
        languages.addAll(this.langToCorpora.keySet());
        return languages;
    }

    @Override
    public List<Corpus> getChildren(Corpus corpus) {
        List<Corpus> corpora = this.corpusToChildren.get(corpus);
        if (corpora == null) {
            return (new ArrayList<Corpus>());
        } else {
            List<Corpus> corporaCopy = new ArrayList<Corpus>(corpora);
            return corporaCopy;
        }
    }
    
    
    @Override
    public String toString() {
        return "CorpusCache{\n" + "enpUrlToRootCorpora=" + enpUrlToRootCorpora + "\n corpusToChildren=" + corpusToChildren + "\n langToCorpora=" + langToCorpora + "\n}";
    }

    private void readCache(String corporaScanDir) {
        File sruInstitutionsFile = new File(corporaScanDir + "inst.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(sruInstitutionsFile), ENCODING));
            String line;
            Institution inst = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    String[] splitted = line.split("\\" + CacheCorporaScanIntoFileTask.SEP);
                    if (splitted.length == 2 && splitted[0].equals(CacheCorporaScanIntoFileTask.I)) {
                        inst = new Institution(splitted[1], "");
                    } else if (splitted.length == 3 && splitted[0].equals(CacheCorporaScanIntoFileTask.IE)) {
                        Endpoint ep = inst.add(splitted[2]);
                        if (!this.enpUrlToRootCorpora.containsKey(ep.getUrl())) {
                            this.enpUrlToRootCorpora.put(ep.getUrl(), new ArrayList<Corpus>());
                        }
                        if (!splitted[1].trim().isEmpty()) {
                            // traverse the corresponding dir
                            String path = corporaScanDir + splitted[1] + "/";
                            readRootCorpus(path, inst);
                        }
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void readRootCorpus(String path, InstitutionI inst) {
        File corpusFile = new File(path + "corpus.txt");
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
                        // endpoint url
                        List<Corpus> corpora = this.enpUrlToRootCorpora.get(line);
                        corpus = new Corpus(inst, line);
                        corpora.add(corpus);
                    }
                    if (lineCount == 1) {
                        // corpis id/handle
                        corpus.setHandle(line);
                    }
                    if (lineCount == 2) {
                        // corpis name
                        corpus.setDisplayName(line);
                    }
                    if (lineCount == 3) {
                        // corpis page
                        corpus.setLandingPage(line);
                    }
                    if (lineCount == 4) {
                        // corpis description
                        corpus.setDescription(line);
                    }
                    if (lineCount == 5) {
                        // corpis langs
                        Set<String> langs = new HashSet<String>();
                        for (String lang : line.split("\\" + CacheCorporaScanIntoFileTask.SEP)) {
                            langs.add(lang);
                            if (!this.langToCorpora.containsKey(lang)) {
                                this.langToCorpora.put(lang, new HashSet<Corpus>());
                            }
                            this.langToCorpora.get(lang).add(corpus);
                        }
                        corpus.setLanguages(langs);
                    }
                }
                lineCount++;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        File currentDir = new File(path);
        for (File file : currentDir.listFiles()) {
            if (file.isDirectory() && !file.isHidden()) {
                readCorpus(corpus, file.getAbsolutePath() + "/", inst);
            }
        }
    }
    
    
    private void readCorpus(Corpus parentCorpus, String path, InstitutionI inst) {
        
        File corpusFile = new File(path + "corpus.txt");
        BufferedReader reader = null;
        Corpus corpus = null;
        if (!this.corpusToChildren.containsKey(parentCorpus)) {
            this.corpusToChildren.put(parentCorpus, new ArrayList<Corpus>());
        }
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(corpusFile), ENCODING));
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (lineCount == 0) {
                        // endpoint url
                        corpus = new Corpus(inst, line);
                        this.corpusToChildren.get(parentCorpus).add(corpus);
                    }
                    if (lineCount == 1) {
                        // corpis id/handle
                        corpus.setHandle(line);
                    }
                    if (lineCount == 2) {
                        // corpis name
                        corpus.setDisplayName(line);
                    }
                    if (lineCount == 3) {
                        // corpis page
                        corpus.setLandingPage(line);
                    }
                    if (lineCount == 4) {
                        // corpis description
                        corpus.setDescription(line);
                    }
                    if (lineCount == 5) {
                        // corpis langs
                        Set<String> langs = new HashSet<String>();
                        for (String lang : line.split("\\" + CacheCorporaScanIntoFileTask.SEP)) {
                            langs.add(lang);
                            if (!this.langToCorpora.containsKey(lang)) {
                                this.langToCorpora.put(lang, new HashSet<Corpus>());
                            }
                            this.langToCorpora.get(lang).add(corpus);
                        }
                        corpus.setLanguages(langs);
                    }
                }
                lineCount++;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(CorporaScanCache.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        File currentDir = new File(path);
        for (File file : currentDir.listFiles()) {
            if (file.isDirectory() && !file.isHidden()) {
                readCorpus(corpus, file.getAbsolutePath() + "/", inst);
            }
        }
    }


    
    public static void main(String[] args) {
        CorporaScanCache cache = new CorporaScanCache("/Users/yanapanchenko/Documents/Work/temp/agca-r/");
        System.out.println(cache);
    }

    @Override
    public Map<String, Set<Corpus>> getRootCorporaForLang() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, Set<Corpus>> getTopUniqueLangToCorpora() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Corpus> getTopUniqueLanguageCorpora(String lang) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
