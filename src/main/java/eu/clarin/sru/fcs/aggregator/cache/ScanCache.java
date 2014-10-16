package eu.clarin.sru.fcs.aggregator.cache;

import eu.clarin.sru.fcs.aggregator.registry.Corpus;
import eu.clarin.sru.fcs.aggregator.registry.Institution;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for the cached scan data (endpoints descriptions).
 *
 * @author Yana Panchenko
 */
public interface ScanCache {

    /**
     * Checks whether the Cache has the endpoints resource tree cached.
     * @return true if the Cache is empty, false otherwise
     */
    public boolean isEmpty();
    
    
    /**
     * Gets all the institutions registered in center repository
     * as having a SRU/CQL endpoint
     * @return list of institutions that support SRU/CQL
     */
    public List<Institution> getInstitutions();

    /**
     * Gets all the root corpora of the endpoints (top nodes in the corpus
     * resource tree)
     * @return root corpora of the endpoints
     */
    public List<Corpus> getRootCorpora();

    
     /**
     * Gets languages mapped to all the root corpora of the endpoints 
     * (top nodes in the corpus resource tree) that have the corresponding language
     * @return a map from the languages to the root corpora of the endpoints 
     * in the corresponding language
     */
    public Map<String, Set<Corpus>> getRootCorporaForLang();
    
    /**
     * Gets all the root corpora of the endpoints (top nodes in the corpus
     * resource tree) that have the specified language
     * @param lang language of interest as a three-letter iso code
     * @return root corpora of the endpoints that are in the specified language
     */
    public List<Corpus> getRootCorporaForLang(String lang);
    
    /**
     * In the corpus resource tree, gets all the languages mapped to the corpus 
     * resources that have the corresponding language as the only one language,
     * and either have no parent resource or its parent has multiple languages.
     * @return map of languages to top corpora (in the corpus resource tree) 
     * that have the corresponding language as the only language
     */
    public Map<String, Set<Corpus>> getTopUniqueLangToCorpora();
    
    /**
     * In the corpus resource tree, gets all the resource nodes of the tree
     * that have the specified language as the only language of the resource,
     * and either have no parent resource or its parent resource has multiple 
     * languages.
     * @param lang language of interest as three-letter iso code 
     * @return corpora that have the specified language as the only language
     * and whose parent corpora do not have it as the only language
     */
    public List<Corpus> getTopUniqueLanguageCorpora(String lang);
    
    /**
     * Gets all the root corpora of the specified endpoints
     * @param enpointUrl the URL of the endpoint of interest
     * @return root corpora of the endpoint
     */
    public List<Corpus> getRootCorporaOfEndpoint(String enpointUrl);
    
    /**
     * Gets all the languages of the existing corpora in the corpus resource
     * tree (since parent corpora sum-up languages of their children corpora,
     * that's all the languages of the root corpora)
     * @return all the languages specified in the corpus resource tree
     */
    public Set<String> getLanguages();

    /**
     * Gets all children corpora of the specified corpus in the endpoints 
     * resource tree
     * @param corpus the parent corpus
     * @return children corpora of the specified parent corpus
     */
    public List<Corpus> getChildren(Corpus corpus);
    
    
    /**
     * Adds institution that supports SRU/CQL into the cache.
     * 
     * @param institution that supports SRU/CQL
     */
    public void addInstitution(Institution institution);
    
    /**
     * Adds corpus that is the top level resource of an endpoint
     * (root corpus that has no parent corpus) into the cache.
     * Same as addCorpus(c, null)
     * 
     * @param c root corpus to be added
     */
    public void addCorpus(Corpus c);

    
    /**
     * Adds corpus into the cache.
     * 
     * @param c corpus to be added
     * @param parentCorpus parent of the corpus to be added
     */
    public void addCorpus(Corpus c, Corpus parentCorpus);

    
    /**
     * Gets Corpus that corresponds to the specified handle.
     * 
     * @param handle of the Corpus of interest.
     * @return Corpus of the handle or null if there is no Corpus with
     * this handle in the cache.
     */
    public Corpus getCorpus(String handle);
    
}
