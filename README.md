# fcs-sru-aggregator
CLARIN Federated Content Search v2.0 Aggregator – Augmenting your Search Engine

# Introduction
  The CLARIN Federated Content Search
  (CLARIN-FCS) introduces an _interface specification_ that
  decouples the _search engine_ functionality from its _exploitation_, i.e. user-interfaces, third-party applications,
  to allow services to access heterogeneous search engines in a
  uniform way.


# The Aggregator
[The Aggregator v3.0](https://spraakbanken.gu.se/ws/fcs/2.0/aggregator/) is running at [The National Swedish Language Bank](https://spraakbanken.gu.se/eng/).

# The Specification
The Specification for Federated Content Search v2.0 can be found as a [PDF document](https://office.clarin.eu/v/CE-2017-1046-FCS-Specification.pdf).

# What is new in FCS v2.0?
* A new Query Language
* A matching display of query results, the AdvancedDataView, ADV, with layer capabilities
* Backwards compatibility to earlier versions

These new additions to the CLARIN-FCS will not only enhance the power
user experience and possibilities when performing queries from
repositories, but also that less experienced users will find it easier
to explore different corpora.

# What is new in FCS Aggregator v3.0?
* A new graphical query builder (GQB) to support  the new Query Language FCS-QL
* Support for the AdvancedDataView, ADV, with layer capabilities
* Backwards compatibility to earlier versions of Endpoints and protocols, _legacy_ and _version1_

 The backwards compatibility gives you as a Centre search engine maintainer
 a smooth transtion to the new features and capabilities at your own convenience.

# How does it work?
1. a Client submits a query to an Endpoint 
1. The Endpoint translates the query from CQL or FCS-QL to the query dialect used by the Search Engine and submits the translated query to the Search Engine
1. The Search Engine processes the query and generates a result set, i.e. it compiles a set of hits that match the search criteria.
1. The Endpoint then translates the results from the Search Engine-specific result set format to the CLARIN-FCS result format and sends them to the Client.

# Endpoint Reference Implementation
If you have any kind of RESTful API to your Search Engine using the  
 [Endpoint Reference Implementation](https://github.com/clarin-eric/fcs-korp-endpoint/). Do not forget to look at the tests.

# Endpoint Tester
To test your Endpoint you can point the [Endpoint Tester]() to your Endpoint.

# Endpoint developer's tutorial
There is also an Endpoint developer's tutorial available. 

# Building
To build the FCS Aggregator you need a few simple steps:
1. `./build.sh --bower`
1. `npm init`
1. `./build.sh --jar`

The frontend (React) and backend (jersey servlet ) are then built using node and maven.
