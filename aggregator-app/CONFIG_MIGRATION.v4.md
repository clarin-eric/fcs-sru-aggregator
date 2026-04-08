Aggregator configuration migration v3 → v4
==========================================

from: 2026-04-08

---

aggregatorParams → aggregator

# (new)
 → aggregator.echoConfig

# scan
aggregatorParams.CENTER_REGISTRY_URL → aggregator.scan.centreRegistryUrl
aggregatorParams.SCAN_MAX_DEPTH → aggregator.scan.maxScanDepth
aggregatorParams.SCAN_TASK_INITIAL_DELAY → aggregator.scan.scanTaskInitialDelay
aggregatorParams.SCAN_TASK_INTERVAL → aggregator.scan.scanTaskInterval
aggregatorParams.SCAN_TASK_TIME_UNIT → aggregator.scan.scanTaskIntervalTimeUnit
aggregatorParams.SCAN_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT → aggregator.scan.maxConcurrentRequests
aggregatorParams.ENDPOINTS_SCAN_TIMEOUT_MS → aggregator.scan.requestTimeoutMs
aggregatorParams.AGGREGATOR_FILE_PATH → aggregator.scan.cachedResourcesFile
aggregatorParams.AGGREGATOR_FILE_PATH_BACKUP → aggregator.scan.cachedResourcesBackupFile

# search
aggregatorParams.SEARCH_MAX_CONCURRENT_REQUESTS_PER_ENDPOINT → aggregator.search.maxConcurrentRequests
aggregatorParams.ENDPOINTS_SEARCH_TIMEOUT_MS → aggregator.search.requestTimeoutMs
 → aggregator.search.maxAmountSearchCache
 → aggregator.search.maxAgeSearchCacheMs

# endpoint overrides
 → aggregator.endpointOverrides

aggregatorParams.additionalCQLEndpoints → aggregator.endpointOverrides (with `isCQL: true`)
aggregatorParams.additionalFCSEndpoints → aggregator.endpointOverrides
aggregatorParams.slowEndpoints → aggregator.endpointOverrides (with `maxConcurrentSearchRequests: 1`)

# executor
aggregatorParams.EXECUTOR_SHUTDOWN_TIMEOUT_MS → aggregator.executorShutdownTimeoutMs

# weblicht
aggregatorParams.weblichtConfig.* → aggregator.weblicht.*

# matomo/piwik
aggregatorParams.piwikConfig.* → aggregator.matomo.*

# rest... (api)
aggregatorParams.prettyPrintJSON → aggregator.prettyPrintJSON
aggregatorParams.openapiEnabled → aggregator.openapiEnabled

aggregatorParams.searchResultLinkEnabled → aggregator.searchResultLinkEnabled

aggregatorParams.SERVER_URL → aggregator.serverUrl
aggregatorParams.VALIDATOR_URL → aggregator.validatorUrl


aggregatorParams.aaiConfig.enabled → aggregator.auth.enabled
aggregatorParams.aaiConfig.key.* → aggregator.auth.keys.*
aggregatorParams.aaiConfig.shibWebappHost → aggregator.auth.shibWebappHost
aggregatorParams.aaiConfig.shibLogin → aggregator.auth.shibLogin
aggregatorParams.aaiConfig.shibLogout → aggregator.auth.shibLogout

