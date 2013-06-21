package eu.clarin.sru.fcs.aggregator.sopt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Yana Panchenko
 */
public class CorpusCache {
    

    Map<String,List<Corpus>> enpUrlToRootCorpora = new ConcurrentHashMap<String,List<Corpus>>(30);
    //Map<String,Set<Corpus>> enpUrlToCorpora = new ConcurrentHashMap<String,Set<Corpus>>();
    Map<Corpus,List<Corpus>> corpusToChildren = new ConcurrentHashMap<Corpus,List<Corpus>>();
    Map<String,Set<Corpus>> langToCorpora = new ConcurrentHashMap<String,Set<Corpus>>();
    

    Map<String, Set<Corpus>> getLangToCorpora() {
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
    
    public List<Corpus> getRootCorpora(String lang) {
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
    
    public Set<String> getLanguages() {
        Set<String> languages = new HashSet<String>(this.langToCorpora.size());
        languages.addAll(this.langToCorpora.keySet());
        return languages;
    }
    
    public List<Corpus> getChildren(Corpus corpus) {
        List<Corpus> corpora = this.corpusToChildren.get(corpus);
        if (corpora == null) {
            return (new ArrayList<Corpus>());
        } else {
            List<Corpus> corporaCopy = new ArrayList<Corpus>(corpora);
            return corporaCopy;
        }
    }

    
    
//    public void update(
//            Map<String,List<Corpus>> enpUrlToRootCorpora, 
//            Map<Corpus,List<Corpus>> corpusToChildren, 
//            Map<String,Set<Corpus>> langToCorpora) {
//        this.enpUrlToRootCorpora.clear();
//        this.enpUrlToRootCorpora.putAll(enpUrlToRootCorpora);
//        this.corpusToChildren.clear();
//        this.corpusToChildren.putAll(corpusToChildren);
//        this.langToCorpora.clear();
//        this.langToCorpora.putAll(langToCorpora);
//    }
    
        public void update(
            Map<String,List<Corpus>> enpUrlToRootCorpora, 
            Map<Corpus,List<Corpus>> corpusToChildren, 
            Map<String,Set<Corpus>> langToCorpora) {
            
        this.enpUrlToRootCorpora.clear();
        this.enpUrlToRootCorpora.putAll(enpUrlToRootCorpora);
        this.corpusToChildren.clear();
        this.corpusToChildren.putAll(corpusToChildren);
        this.langToCorpora.clear();
        this.langToCorpora.putAll(langToCorpora);
        
        //System.out.println(this);
            
    }

    @Override
    public String toString() {
        return "CorpusCache{\n" + "enpUrlToRootCorpora=" + enpUrlToRootCorpora + "\n corpusToChildren=" + corpusToChildren + "\n langToCorpora=" + langToCorpora + "\n}";
    }
}
