# Changelog

# WIP [3.4.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.4.0) - 2023-02-16

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

- Changes:
  - Added Github Actions CI build script
  - Upgraded dropwizard from 2.0.3 to 2.1.4
  - Upgraded `org.apache.poi:poi-ooxml` from 3.17 to 5.2.3
  - Added `org.glassfish.jersey.media:jersey-media-jaxb:2.36` for newer jaxb dependency
  - Upgraded `eu.clarin.weblicht:wlfxb` from 1.3.1 to 1.4.3

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

For older changes, see commit history at https://github.com/clarin-eric/fcs-sru-aggregator/commits/master
