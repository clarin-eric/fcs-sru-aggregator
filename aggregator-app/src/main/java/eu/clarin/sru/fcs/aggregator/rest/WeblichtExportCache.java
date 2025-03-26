package eu.clarin.sru.fcs.aggregator.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.clarin.sru.fcs.aggregator.util.UniqueId;

public final class WeblichtExportCache {
    public static final int EXPORTS_SIZE_GC_THRESHOLD = 3;

    private final List<WeblichtExportCacheEntry> exports = Collections
            .synchronizedList(new ArrayList<WeblichtExportCacheEntry>());

    public WeblichtExportCache() {
    }

    public byte[] getWeblichtExport(String exportId) {
        synchronized (exports) {
            for (WeblichtExportCacheEntry export : exports) {
                if (exportId.equals(export.getId())) {
                    return export.getData();
                }
            }
        }
        return null;
    }

    public String addWeblichtExport(byte[] data) {
        synchronized (exports) {
            // check if data already exists in cache
            int dataHash = Arrays.hashCode(data);
            for (WeblichtExportCacheEntry export : exports) {
                if (Arrays.hashCode(export.data) == dataHash && Arrays.equals(data, export.data)) {
                    return export.getId();
                }
            }

            // needs to add new entry but check first if we need to evict old ones
            if (exports.size() > EXPORTS_SIZE_GC_THRESHOLD) {
                exports.remove(0);
            }

            // create and add new entry
            WeblichtExportCacheEntry export = new WeblichtExportCacheEntry(data);
            exports.add(export);
            return export.getId();
        }
    }

    // ----------------------------------------------------------------------

    public static class WeblichtExportCacheEntry {
        private final String id;
        private final byte[] data;

        public WeblichtExportCacheEntry(byte[] data) {
            this.id = UniqueId.generateId();
            this.data = data;
        }

        public String getId() {
            return id;
        }

        public byte[] getData() {
            return data;
        }
    }
}
