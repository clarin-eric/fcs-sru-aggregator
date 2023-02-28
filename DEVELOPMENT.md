# Development

## Versions

- Java: 11 JDK (17 should work, too)  
  `java -version`: `11.0.17`  
  `mvn -version`: `3.6.3`
- NodeJS:  
  `npm --version`: `8.19.3`  
  `node --version`: `v16.19.1`

## Workflow: Release a New Version

- add changes to [`CHANGELOG.md`](CHANGELOG.md)
- update version number in:
  - [`pom.xml`](pom.xml) at `<project...><version>`
  - [`package.json`](package.json) at `"version":`
  - [`src/main/resources/assets/js/main.jsx`](src/main/resources/assets/js/main.jsx) at `var VERSION = window.MyAggregator.VERSION`
- run full build: `./build.sh --npm --jsx-force --jar`
- add changes: `git add` changed files
- commit: `git commit`
- tag release: `git tag -a <new-version> -m "<new-version-description>"`
- publish: `git push origin <new-version> <working-branch>`
