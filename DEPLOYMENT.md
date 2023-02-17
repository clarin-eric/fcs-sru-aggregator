# Deployment

## Configuration

The [`aggregator.yml`](aggregator.yml) and [`aggregator_devel.yml`](aggregator_devel.yml) are examples for [configuring the DropWizard](https://www.dropwizard.io/en/latest/manual/core.html#configuration) backend server. [Environment variable substitution](https://www.dropwizard.io/en/latest/manual/core.html#environment-variables) is enabled, so `.env` files can be used.

The following subsections will only focus on the `aggregatorParams.*` parameters. Other parameters are explained in the DropWizard documentation.

### Endpoint configuration

Endpoints will be loaded from the URL configured in `CENTER_REGISTRY_URL`. Optional side-loading endpoints (for testing) can be done via `additionalCQLEndpoints` and `additionalFCSEndpoints`.

```yaml
aggregatorParams:
  CENTER_REGISTRY_URL: https://centres.clarin.eu/restxml/

  # optional, can also be commented out
  additionalCQLEndpoints:
    - https://clarin.ids-mannheim.de/digibibsru-new
  additionalFCSEndpoints:
    - https://spraakbanken.gu.se/ws/fcs/2.0/endpoint/korp/sru
```

Side-loaded endpoints can be configured in various ways (mixing of formats is possible), e.g.:

```yaml
# both (both CQL/FCS) `additionalCQLEndpoints`/`additionalFCSEndpoints`
aggregatorParams:
  additionalCQLEndpoints: 

# as simple URLs ("Unknown Institution" will be used as identifier)
# backwards compatibility (simple string/URL)
   - https://clarin.ids-mannheim.de/digibibsru-new
   - http://localhost:8080/korp-endpoint/sru
   - https://spraakbanken.gu.se/ws/fcs/2.0/endpoint/korp/sru

# or in the new format, structured:
# endpoint with custom name
  - url: https://spraakbanken.gu.se/ws/fcs/2.0/endpoint/korp/sru
    name: Språkbanken
# or (order doesn't matter)
  - name: Språkbanken 2
    url: https://spraakbanken.gu.se/ws/fcs/2.0/endpoint/korp/sru

# same as simple string (no institution name provided)
  - url: https://spraakbanken.gu.se/ws/fcs/2.0/endpoint/korp/sru

# invalid (URL is required)
  - name: abc
```

The `AGGREGATOR_FILE_PATH` and `AGGREGATOR_FILE_PATH_BACKUP` are used to store the current and previous `scan`/`explain` result. The same content is exposed via the REST API.

### WebLicht

The default WebLicht configuration is as follows. The only setting to change based on deployment is `weblichtConfig.exportServerUrl` or `SERVER_URL` which should be the publicly available domain (and path if prefixed) the Aggregator runs at. This is required since TCF exports for [WebLicht](https://weblicht.sfs.uni-tuebingen.de) are temporarily stored at the Aggregator and therefore need to be accessible to process results.

```yaml
aggregatorParams:
  weblichtConfig:
    url: https://weblicht.sfs.uni-tuebingen.de/weblicht/?input=
    exportServerUrl: ${SERVER_URL:-https://contentsearch.clarin.eu/}rest/
    acceptedTcfLanguages:
      - en
      - de
      - nl
      - fr
      - it
      - es
      - pl
```

### OpenAPI/Swagger

OpenAPI documentation (for Swagger etc.) is enabled by default.

```yaml
aggregatorParams:
  openapiEnabled: ${SWAGGER_ENABLED:-true}
```

### Matomo (Piwik) Statistics Collection

The following excerpt (from [`aggregator.yml`](aggregator.yml)) describes all the settings for configuring Matomo (Piwik). While the frontend will be built and delivered as static assets, the [`index.html`](src/main/resources/eu/clarin/sru/fcs/aggregator/app/index.mustache) uses templates to allow some dynamic configuration.

```yaml
aggregatorParams:
  piwikConfig:
    enabled: ${PIWIK_ENABLED:-true}
    url: ${PIWIK_URL:-https://stats.clarin.eu/}
    siteID: ${PIWIK_SITEID:-20}
    setDomains: ${PIWIK_SETDOMAINS:-['*.contentsearch.clarin.eu']}
```

Using environment variables those settings can be overwritten. `setDomains` / `PIWIK_SETDOMAINS` is an array of strings, so correct quoting is required. Examples: `PIWIK_SETDOMAINS=[]` for an empty list or `PIWIK_SETDOMAINS=['*.contentsearch.clarin.eu', 'contentsearch.claring.eu']` for one or more domain patterns.

## Single Page Application Proxying

The CLARIN FCS Aggregator uses a ReactJS-based single-page-application frontend. For (initial) requests to dynamic sub pages, the `index.html` start page needs to be returned.
The following are example configurations for `nginx` or `apache2` to transparently rewrite requests for the SPA to work.
NOTE: SSL for HTTPS can still be added on top and is recommended!

### Proxy with `nginx`

Use a `default.conf` configuration like the following:
```nginx
server {
    # public port
    listen       80;
    # catch all server name
    server_name  _;

    location ^~ /admin {
        # block all requests to dropwizard admin servlet
        # /admin/*, /tasks/*
        deny all;
        return 404;
    }

    location / {
        # SPA path rewrites
        rewrite ^/(help|about|stats|embed)$ / break;

        proxy_set_header        Host $host;
        proxy_set_header        X-Real-IP $remote_addr;
        #proxy_set_header        X-Forwarded-Host $host;
        #proxy_set_header        X-Forwarded-Server $host;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header        X-Forwarded-Proto https;
        proxy_redirect off;
        # "localhost" being the internal server name for the aggregator,
        # may be different depending on deployment
        proxy_pass http://localhost:4019/;
    }
}
```

### Proxy with `apache2`

Add your configuration to `/etc/apache2/sites-available`, e.g. `aggregator.conf`
```apacheconf
<VirtualHost *:80>
    # ...
    RewriteEngine on
    # block admin route (403=Forbidden) or use [R=404,L]
    RewriteRule ^/admin - [F]
    # deliver index.html for every dynamic page
    RewriteRule ^/help$ / [L,PT]
    RewriteRule ^/about$ / [L,PT]
    RewriteRule ^/stats$ / [L,PT]
    RewriteRule ^/embed$ / [L,PT]
    RewriteRule ^ - [L,PT]

    ProxyPreserveHost on
    ProxyPass / http://localhost:4019/
    ProxyPassReverse / http://localhost:4019/
    # ...
</VirtualHost>
```

## Docker

A simple [`Dockerfile`](Dockerfile) is included that shows how to build a small image with the application.

Building:
```bash
docker build --tag=fcs-aggregator .
```

Running:
```bash
# set up the files, so that docker does not create the files as root user
if [ ! -f fcsAggregatorCorpora.json ]; then
    touch fcsAggregatorCorpora.json fcsAggregatorCorpora.backup.json
fi

# -d = run in background
# -p 5005:5005 = vscode java debugging port
docker run \
    -d \
    --restart unless-stopped \
    -p 4019:4019 \
    -p 5005:5005 \
    -v $(pwd)/aggregator.yml:/work/aggregator.yml:ro \
    -v $(pwd)/fcsAggregatorCorpora.json:/var/lib/aggregator/fcsAggregatorCorpora.json \
    -v $(pwd)/fcsAggregatorCorpora.backup.json:/var/lib/aggregator/fcsAggregatorCorpora.backup.json \
    fcs-aggregator
```

## CLARIN

- [Dockerimage](https://gitlab.com/CLARIN-ERIC/docker-fcs) using [alpine+supervisor baseimage](https://gitlab.com/CLARIN-ERIC/docker-alpine-supervisor-base)
- [Docker: alpine+nginx](https://gitlab.com/CLARIN-ERIC/docker-alpine-nginx) as possible nginx image for [SPA proxying](#proxy-with-nginx)
