# Deployment

The CLARIN FCS Aggregator uses a ReactJS-based single-page-application frontend. For (initial) requests to dynamic sub pages, the `index.html` start page needs to be returned.
The following are example configurations for `nginx` or `apache2` to transparently rewrite requests for the SPA to work.
NOTE: SSL for HTTPS can still be added on top and is recommended!

## Proxy with `nginx`

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

## Proxy with `apache2`

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
