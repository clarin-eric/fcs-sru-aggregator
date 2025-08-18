# FCS SRU Aggregator

CLARIN Federated Content Search Aggregator – Augmenting your Search Engine

## Introduction

The [CLARIN Federated Content Search (CLARIN-FCS)](https://www.clarin.eu/content/content-search) introduces an _interface specification_ that decouples the _search engine_ functionality from its _exploitation_, i.e. user-interfaces, third-party applications, to allow services to access heterogeneous search engines in a uniform way.

## How does it work?

1. a _Client_ submits a query to an _Endpoint_
2. The _Endpoint_ translates the query from CQL or FCS-QL to the query dialect used by the _Search Engine_ and submits the translated query to the _Search Engine_
3. The _Search Engine_ processes the query and generates a result set, i.e. it compiles a set of hits that match the search criteria.
4. The _Endpoint_ then translates the results from the Search Engine-specific result set format to the CLARIN-FCS result format and sends them to the _Client_.

## The Aggregator

The FCS Aggregator is a search engine for language resources hosted at a [variety of institutions](https://centres.clarin.eu/fcs). The user interface allows search queries to access heterogeneous data sources in a uniform way. It is called _Aggregator_ because it sends out search queries to remote search engines and then collects the search results.

The [FCS Aggregator](https://contentsearch.clarin.eu/) is running at [CLARIN](https://www.clarin.eu/).

## The Specification

The Specification for **Federated Content Search Core 2.0** can be found as a
[PDF document](https://office.clarin.eu/v/CE-2017-1046-FCS-Specification-v20230426.pdf). Sources for the various specification documents can be found at the [FCS Misc Webpage](https://clarin-eric.github.io/fcs-misc/).

For more details visit at the [CLARIN FCS – Technical Details](https://www.clarin.eu/content/federated-content-search-clarin-fcs-technical-details) page.

## Changelog

For a detailed list of changes, please take a look at [`CHANGELOG.md`](CHANGELOG.md).

## Building

### Requirements

The backend Java web server requires **Java 11** or newer, with Maven version 3.6.3.

The frontend React web application requires **Node 22** (with NPM 10). It is only required for re-compiling the frontend. You can also use the bundled, pre-compiled files if no further customization is required.

Docker might be required if you want to run everything containerized. Then both the Java and Node requirements are also not necessary as the multi-stage build process handles everything.

### Source Code

```bash
# fetch the source code
git clone https://github.com/clarin-eric/fcs-sru-aggregator.git && cd fcs-sru-aggregator/

# initialize the git submodules
git submodule update --init --recursive aggregator-webui/
```

### Dropwizard Server Application (Backend)

You can build the Java [dropwizard](https://www.dropwizard.io/en/stable/) server application by running the [`scripts/build.sh`](scripts/build.sh) script. This will generate a [`aggregator-app-*.jar`](aggregator-app/target/) binary that bundles backend, frontend and all relevant dependencies.

```bash
./scripts/build.sh
```

### React Web Frontend

The React web frontend is already included as assets in [`aggregator-app/src/main/resources/assets/webapp`](aggregator-app/src/main/resources/assets/webapp) with the [index page `aggregator-app/src/main/resources/eu/clarin/sru/fcs/aggregator/app/index.mustache`](aggregator-app/src/main/resources/eu/clarin/sru/fcs/aggregator/app/index.mustache).

If you want to modify or update the web application, you can use the [`scripts/update-webui.sh`](scripts/update-webui.sh) script. This will use the [`aggregator-webui/`](aggregator-webui/) git submodule containing the sources and rebuild everything and then place the assets back into the Java [asset folder](aggregator-app/src/main/resources/assets/webapp).

```bash
# optionally update the git submodule code
#git submodule update --remote aggregator-webui/

# rebuild and update web UI files
./scripts/update-webui.sh
```

The React web frontend can also be run as a standalone web application. However, it requires the FCS Aggregator REST API to get data and process search requests. You can find the source code at https://github.com/clarin-eric/fcs-sru-aggregator-ui/.

### Using Docker

To create the docker image, simply build the [`Dockerfile`](Dockerfile). This also allows to configure the frontend a bit by modifying the `webui.env` ([template](webui.env.template.textplus)) file. The aggregator also uses the default configurations in [`aggregator-app/aggregator.yml`](aggregator-app/aggregator.yml) which can be overriden by mounting another file over it.

```bash
docker build --tag=fcs-sru-aggregator .
```

If you do not want to change anything, you can also use the [`Dockerfile.simple`](Dockerfile.simple) which uses the prebuild web frontend assets and only recompiles the Java sources. The [`aggregator-app/aggregator.yml`](aggregator-app/aggregator.yml) configurations will also be used.

## Running

### Configuration

See [`DEPLOYMENT.md`](aggregator-app/DEPLOYMENT.md) for example deployment configurations and descriptions about settings.

### Natively

You can run the application in development and production mode. Please look at the [`scripts/run-(devel|prod).sh`](scripts/) files for more details.

In development the [`aggregator_devel.yml`](aggregator-app/aggregator_devel.yml) and for production the [`aggregator.yml`](aggregator-app/aggregator.yml) configuration files are used.

```bash
# Run in development mode
# Also allows to attach a debugger
./scripts/run-devel.sh

# Run in production mode (uses the build aggregator-app-*.jar file)
./scripts/run-prod.sh
```

You then can access the locally running Aggregator at [http://localhost:4019/](http://localhost:4019/).

### Using Docker

Use docker mounts to allow access to generated data and logs, and to override default configuration files.

```bash
# copy of resources for faster startups
touch fcsAggregatorResources.json fcsAggregatorResources.backup.json
# statistics output directory
mkdir stats

# run in background (detached), and restart (unless stopped by operator)
#   -d --restart unless-stopped
# configure public port (internal 4019, external 4020)
#   -p 4020:4019
# aggregator configuration
#   -v $(pwd)/aggregator-app/aggregator.yml:/work/aggregator.yml:ro
# (cached) aggregator resources
#   -v $(pwd)/fcsAggregatorResources.json:/var/lib/aggregator/fcsAggregatorResources.json
#   -v $(pwd)/fcsAggregatorResources.backup.json:/var/lib/aggregator/fcsAggregatorResources.backup.json
# if statistics logging (fcsstats logger) is enabled, mount the output directory
#   -v $(pwd)/stats:/var/log/aggregator/stats
# provide a container name with `--name`
# and specify the docker image (see build step above)
docker run \
    -d \
    --restart unless-stopped \
    -p 4020:4019 \
    -v $(pwd)/aggregator-app/aggregator.yml:/work/aggregator.yml:ro \
    -v $(pwd)/fcsAggregatorResources.json:/var/lib/aggregator/fcsAggregatorResources.json \
    -v $(pwd)/fcsAggregatorResources.backup.json:/var/lib/aggregator/fcsAggregatorResources.backup.json \
    -v $(pwd)/stats:/var/log/aggregator/stats \
    --name fcs-sru-aggregator \
    fcs-sru-aggregator

# if you did not specify the `--rm` option and used the `--restart` parameter,
# you have to manually stop and remove the container to quit the server and allow for future starts
docker stop fcs-sru-aggregator
docker rm fcs-sru-aggregator
```

You then can access the locally running Aggregator at [http://localhost:4020/](http://localhost:4020/).

## FCS Endpoints

### Endpoint Reference Implementations

If you have any kind of RESTful API to your Search Engine using the [Korp Endpoint Reference Implementation](https://github.com/clarin-eric/fcs-korp-endpoint/) as a starting point should be the way to go. If you more specifically are using Korp it should only be a simple adaptation to corpora and tagsets needed. In any case do not forget to look at the tests.

For lexical resources, an [FCS Endpoint with Solr search engine](https://github.com/saw-leipzig/lexfcs-endpoint) might be a good starting point. It allows index XML structured data in the Solr search engine and handles search query conversion. It can also be used as a template if you want to connect other search engines by modifying the query translation and API handling.

An older [CQI Bridge endpoint](https://github.com/clarin-eric/fcs-sru-cqi-bridge) also exists but only support simple full-texts search.

For more endpoint implementations, please take a look at the [**Awesome FCS** list](https://github.com/clarin-eric/awesome-fcs).

### Endpoint Validation

To test your Endpoint you can point the [FCS Endpoint Validator](https://www.clarin.eu/fcsvalidator/) ([code](https://github.com/saw-leipzig/fcs-endpoint-validator)) to your Endpoint.

<!--

### What is new in FCS Aggregator v3.0?

- A new graphical query builder (GQB) to support the new Query Language FCS-QL
- Support for the AdvancedDataView, ADV, with layer capabilities
- Backwards compatibility to earlier versions of Endpoints and protocols, _legacy_ and _version1_

The backwards compatibility gives you as a Centre search engine maintainer a smooth transtion
to the new features and capabilities at your own convenience.

### What is new in FCS v2.0?

- A new Query Language
- A matching display of query results, the AdvancedDataView, ADV, with layer capabilities
- Backwards compatibility to earlier versions

These new additions to the CLARIN-FCS will not only enhance the power user experience and
possibilities when performing queries from repositories, but also that less experienced
users will find it easier to explore different corpora.

-->
