# Development

## Versions

- Java: 11 JDK (for building)  
  `java -version`: `11.0.17`  
  `mvn -version`: `3.6.3`
  - Java version of source code is `1.8` (`1.7`?) and compiled to `1.8` (see [`pom.xml`](pom.xml))
- NodeJS:  
  `npm --version`: `8.19.3`  
  `node --version`: `v16.19.1`

Note that maven (3.6.3 on Ubuntu) may not work with Java 17! Either setup maven manually, use a docker container for building or use Java 11.

## Workflow: Release a New Version

- add changes to [`CHANGELOG.md`](CHANGELOG.md)
- update version number in:
  - [`pom.xml`](pom.xml) at `<project...><version>`
  - [`package.json`](package.json) at `"version":`
  - [`src/main/resources/assets/js/main.jsx`](src/main/resources/assets/js/main.jsx) at `var VERSION = window.MyAggregator.VERSION`
- run full build: `./build.sh --npm --jsx-force --jar`
- add changes: `git add CHANGELOG.md pom.xml package.json package-lock.json src/main/resources/assets/js/main.jsx src/main/resources/assets/js/main.js`
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

The [`build.sh`](build.sh) script contains a `--with-debugger` flag to automatically start the aggregator with `-agentlib:jdwp=transport=dt_socket,server=y,address=0.0.0.0:5005`. The aggregator will wait until connected with the VSCode debugger (probably a `suspend=y` default).

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
./build.sh --jar
# you may want to update the owner/group since it is directly mapped to the host
chown -R 1000:1000 dependency-reduced-pom.xml target/

# run:
./build.sh --run-production --with-debugger
```
_NOTE: this might also help with other issues, like some jersey SSL stuff._
