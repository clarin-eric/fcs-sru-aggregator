WIP: this is documentation of the old Aggregator (3.x.x)

---

# Development

The FCS Aggregator consists of a frontend (ReactJS, JS) and backend (Dropwizard, Java) component. Both will be bundled into a single `aggregator-app-X.Y.Z.jar` file for deployment.

## Versions

- Java: 11 JDK (for building)  
  `java -version`: `21.0.6` (should still work with Java 11)  
  `mvn -version`: `3.6.3`
  - Java version of source code is `1.8` and compiled to `1.8` (see [`pom.xml`](pom.xml))
- NodeJS:  
  `npm --version`: `10.8.1`  
  `node --version`: `v20.14.0`

Note that maven (3.6.3 on Ubuntu) may not work with Java 17! Either setup maven manually, use a docker container for building or use Java 11.

## Filesystem Structure

The following are the important folders and files for this application:

```python
# folders
├── node_modules/                      # (ignore, nodejs dependencies)
├── src/
│   ├── main/
│   │   ├── java/                      # backend (Dropwizard, Jersey) sources
│   │   └── resources/
│   │       └── assets/                # frontend (React) sources
│   │           ├── fonts/
│   │           ├── js/                # build artifacts
│   │           ├── lib/               # frameworks (bootstrap, jquery, react)
│   │           └── index.html         # SPA page, React entry point
│   └── test/
└── target/                            # build artifacts (e.g. aggregator-X.Y.Z.jar)

# files
├── Dockerfile                         # Example Docker image
├── aggregator.yml                     # runtime configuration (production use)
├── aggregator_devel.yml               # runtime configuration (development use)
├── build.sh                           # helper script for building, running etc.
├── package-lock.json
├── package.json                       # frontend (JS) dependencies
└── pom.xml                            # backend (Java) dependencies

# information
├── CHANGELOG.md
├── DEPLOYMENT.md
├── DEVELOPMENT.md                     # (this file)
└── README.md
```

## OpenAPI/Swagger

There is an OpenAPI (Swagger) endpoint at: [`/openapi.json`](http://localhost:4019/openapi.json). It can be disabled in the [`aggregator.yml`](aggregator.yml) file. Details in [`DEPLOYMENT.md`](DEPLOYMENT.md#openapiswagger).

## Workflow: Building

A [`build.sh`](build.sh) script is included to handle common tasks such as building and running the application.

To do a full build of the application perform the following steps:  
... (WIP)

## Workflow: Running the Application

Check the [`aggregator_devel.yml`](aggregator_devel.yml) (development) or [`aggregator.yml`](aggregator.yml) (production) configuration file. If you want to sideload your enpoint simply add the endpoint to either `additionalCQLEndpoints` or `additionalFCSEndpoints`.

To run the application in debug mode:
```sh
JAR=`find target -iname 'aggregator-app-*.jar'`
java $DEBUGGER_OPTS -cp src/main/resources:$JAR -Xmx4096m eu.clarin.sru.fcs.aggregator.app.AggregatorApp server aggregator_devel.yml
```

For production run:
```sh
JAR=`find target -iname 'aggregator-app-*.jar'`
java $DEBUGGER_OPTS -Xmx4096m -jar $JAR server aggregator.yml
```

With `DEBUGGER_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=0.0.0.0:5005"` for VSCode debugging.

You might also want to change the path to your cache files in `AGGREGATOR_FILE_PATH` and `AGGREGATOR_FILE_PATH_BACKUP` respectively.

You then can access the locally running Aggregator at [http://localhost:4019/](http://localhost:4019/)

See [`DEPLOYMENT.md`](DEPLOYMENT.md) for example deployment configurations.

## Workflow: Releasing a New Version

- add changes to [`CHANGELOG.md`](CHANGELOG.md)
- update version number in:
  - [`pom.xml`](pom.xml) at `<project...><version>`
- run full build: `./build.sh`
- add changes: `git add CHANGELOG.md pom.xml`
- commit: `git commit -m "Bump version to <new-version>"`
- tag release: `git tag -a <new-version> -m "<new-version-description>"`
- publish: `git push origin <new-version> <working-branch>`

## Debugging with VSCode

The Visual Studio Code Java remote process debugging uses the `5005` port by default.

An example configuration `.vscode/launch.json` (auto-generated) looks like the following:
```json
    "configurations": [
        {
            "type": "java",
            "request": "attach",
            "name": "Attach by Process ID",
            "processId": "${command:PickJavaProcess}",
            "hostName": "localhost",
            "port": 5005,
        },
    ]
```

To start aggregator waiting for the VSCode debugger to connect (probably a `suspend=y` default) add `-agentlib:jdwp=transport=dt_socket,server=y,address=0.0.0.0:5005`. See below.

If _somehow_, _somewhat_, a local debugging session does not work, it may help to run the aggregator in docker container:
```bash
# start the container (expose both 4019 and the debugging port 5005)
docker run --rm -it -p 4019:4019 -p 5005:5005 -v $(pwd):/code --entrypoint bash eclipse-temurin:11-jdk-jammy

# ------------------------
# in the container:
# setup:
# (if you want to build in the container)
apt update
apt install -y --no-install-recommends maven

cd code/
mvn clean package
# you may want to update the owner/group since it is directly mapped to the host
chown -R 1000:1000 dependency-reduced-pom.xml target/

# run:
DEBUGGER_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=0.0.0.0:5005"
JAR=`find target -iname 'aggregator-app-*.jar'`
java $DEBUGGER_OPTS -Xmx4096m -jar $JAR server aggregator.yml
```
_NOTE: this might also help with other issues, like some jersey SSL stuff._

---

- `mvn -pl . clean package deploy`
- `mvn -pl aggregator-core clean package deploy`
- `mvn -pl aggregator-core,aggregator-app clean package`
- `mvn -pl aggregator-core,aggregator-app dependency:tree`
