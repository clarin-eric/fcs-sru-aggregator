package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.Institution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of the cached scan data (endpoints descriptions) that
 * stores the cache in memory in maps.
 *
 * @author yanapanchenko
 */
public class SimpleInMemScanCache implements ScanCache {

    private Map<String, List<Corpus>> enpUrlToRootCorpora = new LinkedHashMap<String, List<Corpus>>(30);
    private Map<String, List<Corpus>> corpusToChildren = new HashMap<String, List<Corpus>>();
    //private Map<String, String> childToParent = new HashMap<String, String>();
    private Map<String, Corpus> handleToCorpus = new HashMap<String, Corpus>();
    private Map<String, Set<Corpus>> langToRootCorpora = new HashMap<String, Set<Corpus>>();
    private Map<String, Set<Corpus>> langToTopUniqueCorpora = new HashMap<String, Set<Corpus>>();
    private List<Institution> institutions = new ArrayList<Institution>();

    private static final Logger LOGGER = Logger.getLogger(SimpleInMemScanCache.class.getName());
    
    @Override
    public List<Institution> getInstitutions() {
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

    @Override
    public void addInstitution(Institution institution) {
        institutions.add(institution);
    }

    @Override
    public void addCorpus(Corpus c) {
        addCorpus(c, null);
    }

    @Override
    public void addCorpus(Corpus c, Corpus parentCorpus) {

        

        handleToCorpus.put(c.getHandle(), c);

        if (parentCorpus == null) { //i.e it's a root corpus
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
            //childToParent.put(c.getHandle(), Corpus.ROOT_HANDLE);
        } else {
            if (!corpusToChildren.containsKey(parentCorpus.getHandle())) {
                corpusToChildren.put(parentCorpus.getHandle(), new ArrayList<Corpus>());
            }
            corpusToChildren.get(parentCorpus.getHandle()).add(c);
            //childToParent.put(c.getHandle(), parentCorpus.getHandle());
        }
        
        // index top corpora with unique language as for their languages
        if (c.getLanguages().size() == 1 && 
                (parentCorpus == null || parentCorpus.getLanguages().size() > 0)) {
            String lang = getElementOfStringUnitset(c.getLanguages());
            if (!langToTopUniqueCorpora.containsKey(lang)) {
                langToTopUniqueCorpora.put(lang, new LinkedHashSet<Corpus>());
            }
            langToTopUniqueCorpora.get(lang).add(c);
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
        return this.langToTopUniqueCorpora;
    }

    @Override
    public List<Corpus> getTopUniqueLanguageCorpora(String lang) {
        ArrayList<Corpus> corpora = new ArrayList<Corpus>(langToTopUniqueCorpora.get(lang).size());
        corpora.addAll(langToTopUniqueCorpora.get(lang));
        return corpora;
    }

    @Override
    public Corpus getCorpus(String handle) {
        return this.handleToCorpus.get(handle);
    }

    private String getElementOfStringUnitset(Set<String> stringUnitSet) {
        return stringUnitSet.iterator().next();
    }
}
