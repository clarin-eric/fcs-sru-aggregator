# Changelog

# [3.7.1](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.7.1) - 2023-09-19

- Bug fixes:
  - `_paq` race-condition on initial page load with page routing
  - font path

# [3.7.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.7.0) - 2023-06-16

- Refactor:
  - Rename _corpus_ to _resource_ and _collection_ to _resources_, this changes a lot of internal classes, the REST API and the frontend!

# [3.6.3](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.6.3) - 2023-06-15

- Changes:
  - API: Expose corpus/resource dataviews, layers and capabilities
  - API: Add `numberOfRecords` for resource results
  - UI: update corpus selection when visiting page by `/search-<id>` URL

# [3.6.2](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.6.2) - 2023-04-17

- Changes:
  - Added `searchResultLinkEnabled` (`SEARCH_RESULT_LINK_ENABLED`) configuration
  - Hide search-result-link by default, allow enabling it (mostly for debugging)

# [3.6.1](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.6.1) - 2023-04-06

- Bug fixes:
  - Changed spacing a bit for ADV results view to fit in a single line
  - Added missing `/search-(\d+)` path rewrite to [`DEPLOYMENT.md`](DEPLOYMENT.md)
  - Fixed spacing on about page for gear icon on button

- General:
  - Added section to documentation for using swagger with apache2
  - Added section about known issue with `eclipse-temurin` image and older docker engines
  - Improved tracking/stats collection

# [3.6.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.6.0) - 2023-03-31

- Enhancements:
  - Added frontend only perma link to search results
  - Added separator for ADV result rows (grouping by dataview/result hit)
  - Added Ref-link to hits results

- Dependencies:
  - Upgraded `dropwizard` from 2.1.5 to 2.1.6

# [3.5.7](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.7) - 2023-03-30

- Bug fixes:
  - Fixed external search request integration (e.g. VLO)

- Changes:
  - `SERVER_KEY` configuration (for Swagger)

- General:
  - Code cleanup
  - Removed request filter, code in `IndexResource`

# [3.5.6](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.6) - 2023-03-29

- Enhancements:
  - Added openapi.json (swagger) documentation

- General:
  - Added some notes to [`DEPLOYMENT.md`](DEPLOYMENT.md) about configurations
  - Exposed Piwik settings via environment variables

# [3.5.5](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.5) - 2023-03-28

- Changes:
  - Added (missing) [`LICENSE`](LICENSE) file
  - Updated Github Actions build script (generates `Report` artifact for `mvn site`)
  - Parameterize Piwik statistics configuration/usage

# [3.5.4](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.4) - 2023-03-06

- Bug fixes:
  - Fixed jersey client usage in `ScanCrawlTask` for `CenterRegistryLive`,  
    might relate and fix to Issue #20

- Changes:
  - Added OWASP dependency check (`mvn verify`, `mvn site`)
  - Wrote more documentation (see [`DEVELOPMENT.md`](DEVELOPMENT.md))

# [3.5.3](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.3) - 2023-02-28

- Changes:
  - Added [`DEPLOYMENT.md`](DEPLOYMENT.md) notes
  - Small cleanups and improvements
  - Moved to junit5 due to dropwizard 2.* deprecation;  
    Fixed tests to use junit5 + adjusted center repository URLs

- Dependencies:
  - Migrated `junit` from 4.13.2 to `org.junit.jupiter` 5.9.1
  - Added `mvn site` plugins with pinned versions

# [3.5.2](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.2) - 2023-02-28

- Changes:
  - Set source/target java version to **1.8**
  - Added [`DEVELOPMENT.md`](DEVELOPMENT.md) notes

- Dependencies:
  - Migrated from deprecated `org.apache.commons:commons-lang3:3.9` (`StringEscapeUtils`) to `org.apache.commons:commons-text:1.10.0`
  - Upgraded `junit` from 4.12 to 4.13.2
  - Upgraded `eu.clarin.sru:sru-client` from 1.8.0 to 2.1.0
  - Updated maven plugin versions to latest

# [3.5.1](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.1) - 2023-02-27

- Dependencies:
  - Upgraded `dropwizard` from 2.1.4 to 2.1.5

# [3.5.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.5.0) - 2023-02-17

- Bug fixes:
  - Fixed configuration validation (removed deprecation of `@NotEmpty`, added `@Valid`)

- Enhancements:
  - Extend sideloading configs to allow for custom endpoint names (institutions)
  - Show info message when corpus selected from external search (`x-aggregation-context`)

- General:
  - Some cleanup, commenting

# [3.4.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.4.0) - 2023-02-16

- Bug fixes:
  - Fix external search requests handling (e.g. for VLO) - Issue #13;
    due to changes in how [jetty](https://github.com/dropwizard/dropwizard/issues/6691) handles unconsumed input in error cases, now a `ServletFilter` instead of `ErrorHandler` is being used.
  - Fix single page application routing (behind proxy)

- General:
  - Add VSCode debugger options to `build.js`
  - Some general code leanup
  - Extra logging to allow debugging for corpus resources with duplicate handles (same/other endpoints)
  - Set application prefix to `/` (remove `/Aggregator`)
  - Extract to central `APIROOT` variable in frontend
  - Set `aggregatorParams.weblichtConfig.exportServerUrl` with environment variable `SERVER_URL` for more flexible deployments
  - Update [README](README.md)

# [3.3.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.3.0) - 2023-01-19

- Enhancements:
  - Added "Select visible" button to corpus view modal
  - Added grouping by institute/language to corpus view modal
  - Resolved Issue #22, switched to SVG icon for clarinservices menu; changed hover shape
  - Refactor logging (fix format substitution, only use `org.slf4j`)
  - Added endpoint capabilities to model (show in statistics, use for search type corpus filtering)

- Bug fixes:
  - Allow resources without descriptions
  - Added button to hide empty results with warnings/errors only
  - Fix weblicht support (cache tcf exports in aggregator)

# [3.2.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.2.0) - 2023-01-18

- Dependencies:
  - Upgraded `dropwizard` from 2.0.3 to 2.1.4
  - Upgraded `org.apache.poi:poi-ooxml` from 3.17 to 5.2.3
  - Added `org.glassfish.jersey.media:jersey-media-jaxb:2.36` for newer jaxb dependency
  - Upgraded `eu.clarin.weblicht:wlfxb` from 1.3.1 to 1.4.3

- Changes:
  - Added Github Actions CI build script

# [3.1.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.1.0) - 2023-01-18

- Enhancements:
  - Reformatted source code (Java, JavaScript, CSS; `pom.xml`, `index.html`) using VSCode
  - Added support for Java 11
  - Added basic Dockerfile
  - Updated npm dependencies (package lock version, uprades `npm audit fix`)
  - Updated `build.sh` script (reformatted, fixed `--npm`, added `--jsx-force`)

- Bug fixes:
  - Mouse click events on tabs on statistics page
  - Auto scroll on Advanced DataView overflow
  - Fixed mouse interaction for hidden remove buttons in FCS-QL query builder
  - Fixed corpora description

# [3.0.2-68](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.0.2-68) - CLARIN FCS prior to 2023

For older changes, see commit history at [https://github.com/clarin-eric/fcs-sru-aggregator/commits/master](https://github.com/clarin-eric/fcs-sru-aggregator/commits/master?after=7ea45464475b0df6d4d8e2b93970c803b13ffb54+0&branch=master&qualified_name=refs%2Fheads%2Fmaster)
