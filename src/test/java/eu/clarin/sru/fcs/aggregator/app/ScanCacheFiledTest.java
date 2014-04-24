package eu.clarin.sru.fcs.aggregator.app;

import eu.clarin.sru.fcs.aggregator.cache.ScanCacheFiled;
import eu.clarin.sru.fcs.aggregator.cache.ScanCache;
import eu.clarin.sru.fcs.aggregator.sopt.Corpus;
import eu.clarin.sru.fcs.aggregator.sopt.Endpoint;
import java.io.File;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author yanapanchenko
 */
public class ScanCacheFiledTest {
    
//    
//    
//    @Test
//    public void testReadWriteDepth1() {
//        String scanDir = "/scan-bas";
//        String scanPath1 = this.getClass().getResource(scanDir).getFile();
//        String scanPath2 = "/tmp/scan-bas";
//        File scanDir2 = new File(scanPath2);
//        if (!scanDir2.exists()) {
//            scanDir2.mkdir();
//        }
//        
//        ScanCacheFiled scanFiled1 = new ScanCacheFiled(scanPath1);
//        ScanCache cacheOrig = scanFiled1.read();
//        
//        ScanCacheFiled scanFiled2 = new ScanCacheFiled(scanPath2);
//        scanFiled2.write(cacheOrig);
//        
//        ScanCacheFiled scanFiled3 = new ScanCacheFiled(scanPath2);
//        ScanCache cacheRewritten = scanFiled3.read();
//        
//        //make sure caches contain the same info after read-write
//        Assert.assertEquals(cacheOrig.getInstitutions().size(), cacheRewritten.getInstitutions().size());
//        Endpoint epOrig = cacheOrig.getInstitutions().get(2).getEndpoint(0);
//        Endpoint epRewritten = cacheRewritten.getInstitutions().get(2).getEndpoint(0);
//        Assert.assertEquals(epOrig.getUrl(), epRewritten.getUrl());
//        Assert.assertEquals(epOrig, epRewritten);
//        List<Corpus> rootCorporaOrig = cacheOrig.getRootCorporaOfEndpoint(epOrig.getUrl());
//        List<Corpus> rootCorporaRewritten = cacheRewritten.getRootCorporaOfEndpoint(epOrig.getUrl());
//        Assert.assertEquals(rootCorporaOrig.size(), rootCorporaRewritten.size());
//        Assert.assertEquals(3, rootCorporaRewritten.size());
//        Assert.assertEquals(rootCorporaOrig.get(0), rootCorporaRewritten.get(0));
//        List<Corpus> childenOrig = cacheOrig.getChildrenCorpora(rootCorporaOrig.get(0).getHandle());
//        List<Corpus> childenRewritten = cacheRewritten.getChildrenCorpora(rootCorporaOrig.get(0).getHandle());
//        Assert.assertEquals(childenOrig, childenRewritten);
//        Assert.assertEquals(rootCorporaOrig.get(0).getLanguages(), rootCorporaRewritten.get(0).getLanguages());
//        
//        //System.out.println(cacheOrig);
//        //System.out.println();
//        //System.out.println(cacheRewritten);
//    } 
//    
//    @Test
//    public void testReadWriteDepth2() {
//        String scanDir = "/scan-mpi";
//        String scanPath1 = this.getClass().getResource(scanDir).getFile();
//        String scanPath2 = "/tmp/scan-mpi";
//        File scanDir2 = new File(scanPath2);
//        if (!scanDir2.exists()) {
//            scanDir2.mkdir();
//        }
//        
//        ScanCacheFiled scanFiled1 = new ScanCacheFiled(scanPath1);
//        ScanCache cacheOrig = scanFiled1.read();
//        
//        ScanCacheFiled scanFiled2 = new ScanCacheFiled(scanPath2);
//        scanFiled2.write(cacheOrig);
//        
//        ScanCacheFiled scanFiled3 = new ScanCacheFiled(scanPath2);
//        ScanCache cacheRewritten = scanFiled3.read();
//        
//        //make sure caches contain the same info after read-write
//        Assert.assertEquals(cacheOrig.getInstitutions().size(), cacheRewritten.getInstitutions().size());
//        Endpoint epOrig = cacheOrig.getInstitutions().get(4).getEndpoint(0);
//        Endpoint epRewritten = cacheRewritten.getInstitutions().get(4).getEndpoint(0);
//        Assert.assertEquals(epOrig.getUrl(), epRewritten.getUrl());
//        Assert.assertEquals(epOrig, epRewritten);
//        List<Corpus> rootCorporaOrig = cacheOrig.getRootCorporaOfEndpoint(epOrig.getUrl());
//        List<Corpus> rootCorporaRewritten = cacheRewritten.getRootCorporaOfEndpoint(epOrig.getUrl());
//        Assert.assertEquals(rootCorporaOrig.size(), rootCorporaRewritten.size());
//        Assert.assertEquals(3, rootCorporaRewritten.size());
//        Assert.assertEquals(rootCorporaOrig.get(0), rootCorporaRewritten.get(0));
//        List<Corpus> childenOrig = cacheOrig.getChildrenCorpora(rootCorporaOrig.get(0).getHandle());
//        List<Corpus> childenRewritten = cacheRewritten.getChildrenCorpora(rootCorporaOrig.get(0).getHandle());
//        Assert.assertEquals(childenOrig, childenRewritten);
//        Assert.assertEquals(2, childenRewritten.size());
//        Assert.assertEquals(rootCorporaOrig.get(0).getLanguages(), rootCorporaRewritten.get(0).getLanguages());
//        
////        System.out.println(cacheOrig);
////        System.out.println();
////        System.out.println(cacheRewritten);
//    } 
//    
//        @Test
//    public void testReadWriteDefaultCorpus() {
//        String scanDir = "/scan-def";
//        String scanPath1 = this.getClass().getResource(scanDir).getFile();
//        String scanPath2 = "/tmp/scan-def";
//        File scanDir2 = new File(scanPath2);
//        if (!scanDir2.exists()) {
//            scanDir2.mkdir();
//        }
//        
//        ScanCacheFiled scanFiled1 = new ScanCacheFiled(scanPath1);
//        ScanCache cacheOrig = scanFiled1.read();
//        
//        ScanCacheFiled scanFiled2 = new ScanCacheFiled(scanPath2);
//        scanFiled2.write(cacheOrig);
//        
//        ScanCacheFiled scanFiled3 = new ScanCacheFiled(scanPath2);
//        ScanCache cacheRewritten = scanFiled3.read();
//        
//        //make sure caches contain the same info after read-write
//        Assert.assertEquals(cacheOrig.getInstitutions().size(), cacheRewritten.getInstitutions().size());
//        Endpoint epOrig = cacheOrig.getInstitutions().get(4).getEndpoint(0);
//        Endpoint epRewritten = cacheRewritten.getInstitutions().get(4).getEndpoint(0);
//        Assert.assertEquals(epOrig.getUrl(), epRewritten.getUrl());
//        Assert.assertEquals(epOrig, epRewritten);
//        List<Corpus> rootCorporaOrig = cacheOrig.getRootCorporaOfEndpoint(epOrig.getUrl());
//        List<Corpus> rootCorporaRewritten = cacheRewritten.getRootCorporaOfEndpoint(epOrig.getUrl());
//        Assert.assertEquals(rootCorporaOrig.size(), rootCorporaRewritten.size());
//        Assert.assertEquals(1, rootCorporaRewritten.size());
//        Assert.assertEquals(rootCorporaOrig.get(0).getLanguages(), rootCorporaRewritten.get(0).getLanguages());
//        
////        System.out.println(cacheOrig);
////        System.out.println();
////        System.out.println(cacheRewritten);
//    } 
        
        @Test
    public void testReadWrite2Endpoints() {
        String scanDir = "/scan-2ep";
        String scanPath1 = this.getClass().getResource(scanDir).getFile();
        String scanPath2 = "/tmp/scan-2ep";
        File scanDir2 = new File(scanPath2);
        if (!scanDir2.exists()) {
            scanDir2.mkdir();
        }
        
        ScanCacheFiled scanFiled1 = new ScanCacheFiled(scanPath1);
        ScanCache cacheOrig = scanFiled1.read();
        
        ScanCacheFiled scanFiled2 = new ScanCacheFiled(scanPath2);
        scanFiled2.write(cacheOrig);
        
        ScanCacheFiled scanFiled3 = new ScanCacheFiled(scanPath2);
        ScanCache cacheRewritten = scanFiled3.read();
        
        //make sure caches contain the same info after read-write
        Assert.assertEquals(cacheOrig.getInstitutions().size(), cacheRewritten.getInstitutions().size());
        Assert.assertEquals(cacheOrig.getRootCorpora().size(), cacheRewritten.getRootCorpora().size());
        Endpoint epOrig = cacheOrig.getInstitutions().get(2).getEndpoint(0);
        Endpoint epRewritten = cacheRewritten.getInstitutions().get(2).getEndpoint(0);
        Assert.assertEquals(epOrig.getUrl(), epRewritten.getUrl());
        Assert.assertEquals(epOrig, epRewritten);
        epOrig = cacheOrig.getInstitutions().get(4).getEndpoint(0);
        epRewritten = cacheRewritten.getInstitutions().get(4).getEndpoint(0);
        Assert.assertEquals(epOrig.getUrl(), epRewritten.getUrl());
        Assert.assertEquals(epOrig, epRewritten);
        
//        List<Corpus> rootCorporaOrig = cacheOrig.getRootCorporaOfEndpoint(epOrig.getUrl());
//        List<Corpus> rootCorporaRewritten = cacheRewritten.getRootCorporaOfEndpoint(epOrig.getUrl());
//        Assert.assertEquals(rootCorporaOrig.size(), rootCorporaRewritten.size());
//        Assert.assertEquals(3, rootCorporaRewritten.size());
//        Assert.assertEquals(rootCorporaOrig.get(0), rootCorporaRewritten.get(0));
//        List<Corpus> childenOrig = cacheOrig.getChildrenCorpora(rootCorporaOrig.get(0).getHandle());
//        List<Corpus> childenRewritten = cacheRewritten.getChildrenCorpora(rootCorporaOrig.get(0).getHandle());
//        Assert.assertEquals(childenOrig, childenRewritten);
//        Assert.assertEquals(rootCorporaOrig.get(0).getLanguages(), rootCorporaRewritten.get(0).getLanguages());
        
        //System.out.println(cacheOrig);
        //System.out.println();
        //System.out.println(cacheRewritten);
    } 
    
}
