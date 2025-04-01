package eu.clarin.sru.fcs.aggregator.scan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the cached scan data (endpoints descriptions) that stores
 * the cache in memory in maps. Must be thread safe, updates in the maps are
 * asynchronous.
 *
 * @author yanapanchenko
 * @author edima
 */
public class Resources {
    private List<Institution> institutions = Collections.synchronizedList(new ArrayList<Institution>());
    private List<Resource> resources = new ArrayList<Resource>();

    public List<Institution> getInstitutions() {
        return Collections.unmodifiableList(institutions);
    }

    public List<Resource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public void addInstitution(Institution institution) {
        institutions.add(institution);
    }

    public synchronized boolean addResource(Resource r, Resource parentResource) {
        if (findByHandle(r.getHandle()) != null) {
            return false;
        }
        if (parentResource == null) { // i.e it's a root resource
            resources.add(r);
        } else {
            parentResource.addResource(r);
        }
        return true;
    }

    public Set<String> getLanguages() {
        final Set<String> languages = new HashSet<String>();
        visit(resources, new CallResource() {
            @Override
            public void call(Resource r) {
                languages.addAll(r.getLanguages());
            }
        });
        return languages;
    }

    public List<Resource> getResourcesByIds(final Set<String> resourceIds) {
        final List<Resource> found = new ArrayList<Resource>();
        visit(resources, new CallResource() {
            @Override
            public void call(Resource r) {
                if (resourceIds.contains(r.getId())) {
                    found.add(r);
                }
            }
        });
        return found;
    }

    public List<Resource> findByEndpoint(final String endpointUrl) {
        final List<Resource> found = new ArrayList<Resource>();
        visit(resources, new CallResource() {
            @Override
            public void call(Resource r) {
                if (r.getEndpoint().getUrl().equals(endpointUrl)) {
                    found.add(r);
                }
            }
        });
        return found;
    }

    public Resource findByHandle(final String handle) {
        final List<Resource> found = new ArrayList<Resource>();
        visit(resources, new CallResource() {
            @Override
            public void call(Resource r) {
                if (r.getHandle() != null && r.getHandle().equals(handle)) {
                    found.add(r);
                }
            }
        });
        return found.isEmpty() ? null : found.get(0);
    }

    public static interface CallResource {
        void call(Resource r);
    }

    private static void visit(List<Resource> resources, CallResource clb) {
        for (Resource r : resources) {
            clb.call(r);
            visit(r.getSubResources(), clb);
        }
    }

    @Override
    public String toString() {
        return "Resources [institutions=" + institutions + ", resources=" + resources + "]";
    }
}
