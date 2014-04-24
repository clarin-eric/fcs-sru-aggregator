package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.InstitutionI;
import eu.clarin.sru.fcs.aggregator.cache.ScanCacheI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author yanapanchenko
 */
public class ScanCache implements ScanCacheI {

    private Map<String, List<Corpus>> enpUrlToRootCorpora = new LinkedHashMap<String, List<Corpus>>(30);
    private Map<String, List<Corpus>> corpusToChildren = new HashMap<String, List<Corpus>>();
    private Map<String, String> childToParent = new HashMap<String, String>();
    private Map<String, Set<Corpus>> langToRootCorpora = new HashMap<String, Set<Corpus>>();
    private Map<String, Set<Corpus>> langToTopUniqueCorpora = new HashMap<String, Set<Corpus>>();
    private List<InstitutionI> institutions = new ArrayList<InstitutionI>();

    private static final Logger LOGGER = Logger.getLogger(ScanCache.class.getName());
    

    public List<InstitutionI> getInstitutions() {
        return institutions;
    }

    @Override
    public List<Corpus> getRootCorporaOfEndpoint(String enpointUrl) {
        List<Corpus> roots = new ArrayList<Corpus>();
        if (enpUrlToRootCorpora.containsKey(enpointUrl)) {
            roots.addAll(enpUrlToRootCorpora.get(enpointUrl));
        }
        return roots;
    }

    public List<Corpus> getChildrenCorpora(String handle) {
        List<Corpus> children = new ArrayList<Corpus>();
        if (corpusToChildren.containsKey(handle)) {
            children.addAll(corpusToChildren.get(handle));
        }
        return children;
    }

    public void addInstitution(InstitutionI institution) {
        institutions.add(institution);
    }

    public void addCorpus(Corpus c) {
        addCorpus(c, true, null);
    }

    public void addCorpus(Corpus c, Corpus parentCorpus) {
        addCorpus(c, false, parentCorpus);
    }

    public void addCorpus(Corpus c, boolean root, Corpus parentCorpus) {

        // index top corpora with unique language as for their languages
        //if (c.getLanguages().size() == 1 && 
        //        (root || this.))
        
        
        // don't add corpus that introduces cyclic references
        if (this.childToParent.containsKey(c.getHandle())) {
            // as of March 2014, there are 2 such endpoints...
            LOGGER.warning("Cyclic reference in corpus " + c.getHandle() + " of endpoint " + c.getEndpointUrl());
            return;
        }
        

        if (root) {
            // index root corpora as for their languages
            for (String lang : c.getLanguages()) {
                if (!langToRootCorpora.containsKey(lang)) {
                    langToRootCorpora.put(lang, new HashSet<Corpus>());
                }
                langToRootCorpora.get(lang).add(c);
            }
            // index root corpora as for their endpint url
            if (!enpUrlToRootCorpora.containsKey(c.getEndpointUrl())) {
                enpUrlToRootCorpora.put(c.getEndpointUrl(), new ArrayList<Corpus>());
            }
            enpUrlToRootCorpora.get(c.getEndpointUrl()).add(c);
            childToParent.put(c.getHandle(), Corpus.ROOT_HANDLE);
        } else {
            if (!corpusToChildren.containsKey(parentCorpus.getHandle())) {
                corpusToChildren.put(parentCorpus.getHandle(), new ArrayList<Corpus>());
            }
            corpusToChildren.get(parentCorpus.getHandle()).add(c);
            childToParent.put(c.getHandle(), parentCorpus.getHandle());
        }
    }

    @Override
    public String toString() {
        return "cache{\n" + "institutions=" + institutions + "\n"
                + "enpUrlToRootCorpora=" + enpUrlToRootCorpora
                + "\n corpusToChildren=" + corpusToChildren
                + "\n langToTopUniqueCorpora=" + langToTopUniqueCorpora + "\n}";
    }

    @Override
    public boolean isEmpty() {
        return enpUrlToRootCorpora.isEmpty();
    }

    @Override
    public List<Corpus> getRootCorpora() {
        List<Corpus> rootCorpora = new ArrayList<Corpus>(enpUrlToRootCorpora.size());
        for (List<Corpus> corpora : this.enpUrlToRootCorpora.values()) {
            rootCorpora.addAll(corpora);
        }
        return rootCorpora;
    }

    @Override
    public Set<String> getLanguages() {
        Set<String> languages = new HashSet<String>(this.langToRootCorpora.size());
        languages.addAll(this.langToRootCorpora.keySet());
        return languages;
    }

    @Override
    public List<Corpus> getChildren(Corpus corpus) {
        List<Corpus> corpora = this.corpusToChildren.get(corpus.getHandle());
        if (corpora == null) {
            return (new ArrayList<Corpus>());
        } else {
            List<Corpus> corporaCopy = new ArrayList<Corpus>(corpora);
            return corporaCopy;
        }
    }

    @Override
    public Map<String, Set<Corpus>> getRootCorporaForLang() {
        return langToRootCorpora;
    }

    @Override
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
    public Map<String, Set<Corpus>> getTopUniqueLangToCorpora() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Corpus> getTopUniqueLanguageCorpora(String lang) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
}
