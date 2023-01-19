# Changelog

# [3.3.0](https://github.com/clarin-eric/fcs-sru-aggregator/releases/tag/3.3.0) - 2023-01-19

- Enhancements:
  - Added "Select visible" button to corpus view modal
  - Added grouping by institute/language to corpus view modal
  - Resolved Issue #22, switched to SVG icon for clarinservices menu; changed hover shape
  - Refactor logging (fix format substitution, only use `org.slf4j`)
  - Added endpoint capabilities to model (show in statistics, use for search type corpus filtering)
- Bug Fixes:
  - Allow resources without descriptions
  - Added button to hide empty results with warnings/errors only

- Bug fixes:
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
