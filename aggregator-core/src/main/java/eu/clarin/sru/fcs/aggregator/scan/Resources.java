package eu.clarin.sru.fcs.aggregator.scan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the cached scan data (endpoints descriptions) that stores
 * the cache in memory in maps. Must be thread safe, updates in the maps are
 * asynchronous.
 *
 * @author yanapanchenko
 * @author edima
 */
public class Resources {
    private List<Institution> institutions = Collections.synchronizedList(new ArrayList<>());
    private List<Resource> resources = new ArrayList<>();

    public List<Institution> getInstitutions() {
        return Collections.unmodifiableList(institutions);
    }

    public List<Resource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public void addInstitution(Institution institution) {
        institutions.add(institution);
    }

    public synchronized boolean addResource(Resource resource, Resource parentResource) {
        if (findByHandle(resource.getHandle()) != null) {
            return false;
        }
        if (parentResource == null) { // i.e it's a root resource
            resources.add(resource);
        } else {
            parentResource.addResource(resource);
        }
        return true;
    }

    public Set<String> getLanguages() {
        return getLanguagesByConsortia(null);
    }

    public Set<String> getLanguagesByConsortia(final Collection<String> consortia) {
        // all if consortia == null, else subset
        final List<Resource> resourcesWithConsortium = getResourcesByConsortia(consortia);

        final Set<String> languages = new HashSet<>();
        visit(resourcesWithConsortium, new CallResource() {
            @Override
            public void call(Resource resource) {
                languages.addAll(resource.getLanguages());
            }
        });
        return languages;
    }

    public Set<String> getConsortia() {
        return institutions.stream().map(Institution::getConsortium).collect(Collectors.toSet());
    }

    /**
     * Gather resources where the endpoint institution's consortium is included in
     * the provided set of <code>consortia</code>. If <code>consortia == null</code>
     * then return all resources.
     * 
     * @param consortia the list of centre consortia to filter resources with
     * @return all resources if <code>consortia == null</code> else a subset of
     *         resources where the endpoint's institution's consortium matches
     */
    public List<Resource> getResourcesByConsortia(final Collection<String> consortia) {
        final List<Resource> found = new ArrayList<>();
        for (Resource resource : resources) {
            if (consortia == null || consortia.contains(resource.getEndpointInstitution().getConsortium())) {
                found.add(resource);
            }
        }
        return found;
    }

    public List<Resource> findByEndpointUrl(final String endpointUrl) {
        final List<Resource> found = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource.getEndpoint().getUrl().equals(endpointUrl)) {
                found.add(resource);
            }
        }
        return found;
    }

    public List<Resource> getResourcesByIds(final Set<String> resourceIds) {
        final List<Resource> found = new ArrayList<>();
        visit(resources, new CallResource() {
            @Override
            public void call(Resource resource) {
                if (resourceIds.contains(resource.getId())) {
                    found.add(resource);
                }
            }
        });
        return found;
    }

    public Resource findByHandle(final String handle) {
        final List<Resource> found = new ArrayList<>();
        visit(resources, new CallResource() {
            @Override
            public void call(Resource resource) {
                if (resource.getHandle() != null && resource.getHandle().equals(handle)) {
                    found.add(resource);
                }
            }
        });
        return found.isEmpty() ? null : found.get(0);
    }

    public static interface CallResource {
        void call(Resource resource);
    }

    private static void visit(List<Resource> resources, CallResource clb) {
        for (Resource resource : resources) {
            clb.call(resource);
            visit(resource.getSubResources(), clb);
        }
    }

    @Override
    public String toString() {
        return "Resources [institutions=" + institutions + ", resources=" + resources + "]";
    }
}
