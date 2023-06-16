"use strict";
import classNames from "classnames";
import ResourceView from "../components/resourceview.jsx";
import LanguageSelector from "../components/languageselector.jsx"
import Modal from "../components/modal.jsx";
import Results from "../components/results.jsx";
import QueryInput from "../components/queryinput.jsx";
import ZoomedResult from "../components/zoomedresult.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

window.MyAggregator = window.MyAggregator || {};
var multipleLanguageCode = window.MyAggregator.multipleLanguageCode = "mul"; // see ISO-693-3

var AggregatorPage = createReactClass({
  // fixme! - class AggregatorPage extends React.Component {
  propTypes: {
    APIROOT: PT.string.isRequired,
    ajax: PT.func.isRequired,
    error: PT.func.isRequired,
    info: PT.func.isRequired,
    embedded: PT.bool.isRequired,
    searchId: PT.number,
  },

  nohits: {
    results: null,
  },
  anyLanguage: [multipleLanguageCode, "Any Language"],

  getInitialState: function () {
    var aggrContext = getQueryVariable('x-aggregation-context');
    aggrContext = aggrContext && JSON.parse(aggrContext);

    return {
      resources: new Resources([], this.updateResources),
      languageMap: {},
      weblichtLanguages: [],
      queryTypeId: getQueryVariable('queryType') || 'cql',
      cqlQuery: ((getQueryVariable('queryType') || 'cql') === 'cql') && getQueryVariable('query') || '',
      fcsQuery: ((getQueryVariable('queryType') || 'cql') === 'fcs') && getQueryVariable('query') || '',
      aggregationContext: aggrContext || null,
      language: this.anyLanguage,
      languageFilter: 'byMeta',
      numberOfResults: 10,

      searchId: this.props.searchId,
      timeout: 0,
      hits: this.nohits,

      zoomedResourceHit: null,
    };
  },

  componentDidMount: function () {
    this._isMounted = true;

    this.props.ajax({
      url: this.props.APIROOT + 'init',
      success: function (json, textStatus, jqXHR) {
        if (this._isMounted) {
          var resources = new Resources(json.resources, this.updateResources);

          // // for testing aggregation context
          // json['x-aggregation-context'] = {
          // 	'EKUT': ["http://hdl.handle.net/11858/00-1778-0000-0001-DDAF-D"]
          // };

          var aggregationContext = json['x-aggregation-context'] || this.state.aggregationContext;

          window.MyAggregator.mode = getQueryVariable('mode') || json.mode;
          window.MyAggregator.resources = json.resources;
          window.MyAggregator.xAggregationContext = aggregationContext;

          // Setting visibility, e.g. only resources 
          // from v2.0 endpoints for fcs v2.0
          resources.setVisibility(this.state.queryTypeId, this.state.language[0]);

          if (aggregationContext) {
            const contextResourcesInfo = resources.setAggregationContext(aggregationContext);
            const unavailableResourcesHandles = contextResourcesInfo.unavailable; // list of unavailable aggregationContext
            if (unavailableResourcesHandles.length > 0) {
              this.props.error("Could not find requested resource handles:\n" + unavailableResourcesHandles.join('\n'));
            }

            const actuallySelectedResources = resources.getSelectedIds();

            if (contextResourcesInfo.selected.length !== actuallySelectedResources.length) {
              if (actuallySelectedResources.length === 0) {
                this.props.error("This search does not support the required resource(s), will search all resources instead"); // TODO give detailed reason its not supported.
                resources.recurse(function (resource) { resource.selected = true; });
              } else {
                var err = "Some required context resources are not supported for this search:\n"
                err = err + contextresources.filter((r) => {
                  if (actuallySelectedResources.indexOf(r) === -1) {
                    console.warn("Requested resource but not available for selection", r);
                    return true;
                  }
                  return false;
                }).map((r) => r.title).join('\n')
                this.props.error(err);
              }
            } else if (contextResourcesInfo.selected.length > 0) {
              this.props.info("Pre-selected " + contextResourcesInfo.selected.length + " resource" + (contextResourcesInfo.selected.length != 1 ? "s" : "") + ":\n" + contextResourcesInfo.selected.map(x => x.title + " (" + x.handle + ")").join('\n'));
            }
          }
          else {
            // no context set all visibl to selected as default.
            console.log("no context set, selecting all available");
            resources.recurse(r => { r.visible ? r.selected = true : null })
          }

          this.setState({
            resources: resources,
            languageMap: json.languages,
            weblichtLanguages: json.weblichtLanguages,
            aggregationContext: aggregationContext,
          }, this.postInit);
        }
        else {
          console.warn("Got Aggregator init response, but not mounted!");
        }

        // load old search state if provided and possible
        if (this.state.searchId != null) {
          console.log("Try loading exiting search, from searchId provided from URL:", this.state.searchId);
          this.refreshSearchResults();
          this.setResourceSelectionBySearch();
        }
      }.bind(this),
    });
  },

  postInit() {
    if (window.MyAggregator.mode === 'search') {
      this.search();
    }
  },

  updateResources: function (resources) {
    this.setState({ resources: resources });
  },

  getCurrentQuery() {
    return this.getCurrentQueryByQueryTypeId(this.state.queryTypeId);
  },

  getCurrentQueryByQueryTypeId: function (queryTypeId) {
    if (queryTypeId === 'fcs') {
      return this.state.fcsQuery;
    } else {
      return this.state.cqlQuery;
    }
  },

  search() {
    var query = this.getCurrentQuery();
    var queryTypeId = this.state.queryTypeId;
    if (!query || (this.props.embedded && window.MyAggregator.mode !== 'search')) {
      this.setState({ hits: this.nohits, searchId: null });
      return;
    }
    var selectedIds = this.state.resources.getSelectedIds();
    if (!selectedIds.length) {
      this.props.error("Please select a resource to search into");
      return;
    }

    // console.log("searching in the following resources:", selectedIds);
    // console.log("searching with queryType:", queryTypeId);
    this.props.ajax({
      url: this.props.APIROOT + 'search',
      type: "POST",
      data: {
        query: query,
        queryType: queryTypeId,
        language: this.state.language[0],
        numberOfResults: this.state.numberOfResults,
        resourceIds: selectedIds,
      },
      success: function (searchId, textStatus, jqXHR) {
        // console.log("search ["+query+"] ok: ", searchId, jqXHR);
        //Piwik.getAsyncTracker().trackSiteSearch(query, queryTypeId);
        _paq.push(['trackSiteSearch', query, queryTypeId, false]);

        var timeout = 250;
        setTimeout(this.refreshSearchResults, timeout);
        this.setState({ searchId: searchId, timeout: timeout });
        // TODO: replace url with getSearchPermaLink() ?
      }.bind(this),
    });
  },

  nextResults: function (resourceId) {
    // console.log("searching next results in resource:", resourceId);
    this.props.ajax({
      url: this.props.APIROOT + 'search/' + this.state.searchId,
      type: "POST",
      data: {
        resourceId: resourceId,
        numberOfResults: this.state.numberOfResults,
      },
      success: function (searchId, textStatus, jqXHR) {
        // console.log("search ["+query+"] ok: ", searchId, jqXHR);
        var timeout = 250;
        setTimeout(this.refreshSearchResults, timeout);
        this.setState({ searchId: searchId, timeout: timeout });
      }.bind(this),
    });
  },

  refreshSearchResults: function () {
    if (!this.state.searchId || !this._isMounted) {
      return;
    }
    this.props.ajax({
      url: this.props.APIROOT + 'search/' + this.state.searchId,
      success: function (json, textStatus, jqXHR) {
        var timeout = this.state.timeout;
        if (json.inProgress) {
          if (timeout < 10000) {
            timeout = 1.5 * timeout;
          }
          setTimeout(this.refreshSearchResults, timeout);
          // console.log("new search in: " + this.timeout + "ms");
        } else {
          console.log("search ended; hits:", json);
        }
        var resourceHit = this.state.zoomedResourceHit;
        if (resourceHit) {
          for (var resi = 0; resi < json.results.length; resi++) {
            var res = json.results[resi];
            if (res.resource.id === resourceHit.resource.id) {
              resourceHit = res;
              break;
            }
          }
        }
        this.setState({ hits: json, timeout: timeout, zoomedResourceHit: resourceHit });
      }.bind(this),
    });
  },

  setResourceSelectionBySearch: function () {
    if (!this.state.searchId || !this._isMounted) {
      return;
    }
    this.props.ajax({
      url: this.props.APIROOT + 'search/' + this.state.searchId,
      success: function (json, textStatus, jqXHR) {
        var resourceIds = [];
        for (var resi = 0; resi < json.results.length; resi++) {
          var res = json.results[resi];
          resourceIds.push(res.resource.id);
        }
        this.state.resources.recurse(c => { c.selected = resourceIds.includes(c.id); });
        this.setState({ resources: this.state.resources });
      }.bind(this),
    });
  },

  getExportParams: function (resourceId, format, filterLanguage) {
    var params = resourceId ? { resourceId: resourceId } : {};
    if (format) params.format = format;
    if (filterLanguage) {
      params.filterLanguage = filterLanguage;
    } else if (this.state.languageFilter === 'byGuess' || this.state.languageFilter === 'byMetaAndGuess') {
      params.filterLanguage = this.state.language[0];
    }
    return encodeQueryData(params);
  },

  getDownloadLink: function (resourceId, format) {
    return this.props.APIROOT + 'search/' + this.state.searchId + '/download?' +
      this.getExportParams(resourceId, format);
  },

  getToWeblichtLink: function (resourceId, forceLanguage) {
    return this.props.APIROOT + 'search/' + this.state.searchId + '/toWeblicht?' +
      this.getExportParams(resourceId, null, forceLanguage);
  },

  setLanguageAndFilter: function (languageObj, languageFilter) {
    this.state.resources.setVisibility(this.state.queryTypeId,
      languageFilter === 'byGuess' ? multipleLanguageCode : languageObj[0]);
    this.setState({
      language: languageObj,
      languageFilter: languageFilter,
      resources: this.state.resources, // === this.state.resources.update();
    });
  },

  setQueryType: function (queryTypeId) {
    this.state.resources.setVisibility(queryTypeId, this.state.language[0]);
    setQueryVariable('queryType', queryTypeId);
    setQueryVariable('query', this.getCurrentQueryByQueryTypeId(queryTypeId))
    this.setState({
      queryTypeId: queryTypeId,
      hits: this.nohits,
      searchId: null,
    });
  },

  setNumberOfResults: function (e) {
    var n = e.target.value;
    if (n < 10) n = 10;
    if (n > 250) n = 250;
    this.setState({ numberOfResults: n });
    e.preventDefault();
    e.stopPropagation();
  },

  stop: function (e) {
    e.stopPropagation();
  },

  filterResults: function () {
    var noLangFiltering = this.state.languageFilter === 'byMeta';
    var langCode = this.state.language[0];
    var results = null, inProgress = 0, hits = 0;
    if (this.state.hits.results) {
      results = this.state.hits.results.map(function (resourceHit) {
        return {
          resource: resourceHit.resource,
          inProgress: resourceHit.inProgress,
          exception: resourceHit.exception,
          diagnostics: resourceHit.diagnostics,
          kwics: noLangFiltering ? resourceHit.kwics :
            resourceHit.kwics.filter(function (kwic) {
              return kwic.language === langCode ||
                langCode === multipleLanguageCode ||
                langCode === null;
            }),
          advancedLayers: noLangFiltering ? resourceHit.advancedLayers :
            resourceHit.advancedLayers.filter(function (layers) {
              return layers.every(function (layer) {
                return layer.language === langCode ||
                  langCode === multipleLanguageCode ||
                  langCode === null;
              });
            }),
        };
      });
      for (var i = 0; i < results.length; i++) {
        var result = results[i];
        if (result.inProgress) {
          inProgress++;
        }
        if (result.kwics.length > 0) {
          hits++;
        }
      }
    }
    return {
      results: results,
      hits: hits,
      inProgress: inProgress,
    };
  },

  toggleLanguageSelection: function (e) {
    $(ReactDOM.findDOMNode(this.refs.languageModal)).modal();
    e.preventDefault();
    e.stopPropagation();
  },

  toggleResourceSelection: function (e) {
    $(ReactDOM.findDOMNode(this.refs.resourcesModal)).modal();
    e.preventDefault();
    e.stopPropagation();
  },

  toggleResultModal: function (e, resourceHit) {
    $(ReactDOM.findDOMNode(this.refs.resultModal)).modal();
    this.setState({ zoomedResourceHit: resourceHit });
    e.preventDefault();
    e.stopPropagation();
  },

  onQueryChange: function (queryStr) {
    if (this.state.queryTypeId === 'cql') {
      this.setState({
        cqlQuery: queryStr || '',
      });
    } else {
      this.setState({
        fcsQuery: queryStr || '',
      });
    }
    setQueryVariable('query', queryStr);
  },

  handleKey: function (event) {
    if (event.keyCode == 13) {
      this.search();
    }
  },

  copyToClipboard: function (text) {
    if (!navigator.clipboard) {
      console.warn("Failed to copy to clipboard!");
      return;
    }
    navigator.clipboard.writeText(text).then(function () {
      console.log("Async: Copying to clipboard was successful!");
      _paq.push(['trackEvent', 'Search', 'CopyToClipboardClick', text]);
    }, function (err) {
      console.error("Async: Could not copy text: ", err);
    });
  },

  copyToClipboardInputHandler: function (event) {
    var text = event.target.value;
    _paq.push(['trackEvent', 'Search', 'CopyToClipboardMouse', text]);
  },

  renderZoomedResultTitle: function (resourceHit) {
    if (!resourceHit) return (<span />);
    var resource = resourceHit.resource;
    return (<h3 style={{ fontSize: '1em' }}>
      {resource.title}
      {resource.landingPage ?
        <a href={resource.landingPage} onClick={this.stop} style={{ fontSize: 12 }}>
          <span> – Homepage </span>
          <i className="glyphicon glyphicon-home" />
        </a> : false}
    </h3>);
  },

  getSearchPermaLink: function () {
    //var query = getQueryVariable('query');
    //var query = this.getCurrentQueryByQueryTypeId(queryTypeId)
    var query = this.getCurrentQuery();
    var queryTypeId = this.state.queryTypeId;

    //var tempUrl = window.location.origin + window.location.pathname + "/search-" + this.state.searchId;
    //window.history.replaceState(window.history.state, null, tempUrl);
    //setQueryVariable('queryType', queryTypeId);
    //setQueryVariable('query', query);        
    //var url = window.location.toString();
    var url = window.location.origin + '/'
      + (!!window.MyAggregator.URLROOT.length ? window.MyAggregator.URLROOT + '/' : '')
      + "search-" + this.state.searchId
      + '?' + encodeQueryData({ queryType: queryTypeId, query: query, });
    return url;
  },

  renderSearchPermaLink: function () {
    // DEBUG (only?): if not enable, do not show link
    if (!window.MyAggregator.showSearchResultLink) {
      return false;
    }
    // if no searchId then also do not show
    if (this.state.searchId === null) {
      return false;
    }
    var url = this.getSearchPermaLink();
    return (<div className="input-group input-group-sm col-md-4" title="NOTE: URL to search results is not permanent. This should not be used in publications or similar. It acts more like a short-term share link with limited life-span." style={{ float: "right" }}>
      <span className="input-group-addon">Search Result Link</span>
      <input type="text" readOnly value={url} onCopy={this.copyToClipboardInputHandler} id="search-perma-link" className="form-control input-sm search" />
      <div className="input-group-btn">
        <button className="btn btn-default input-sm image_button" type="button" onClick={this.copyToClipboard.bind(this, url)}>
          <i className="glyphicon glyphicon-copy" />
        </button>
      </div>
    </div>);
  },

  renderSearchButtonOrLink: function () {
    if (this.props.embedded) {
      var query = this.getCurrentQuery();
      var queryTypeId = this.state.queryTypeId;
      var btnClass = classNames({
        'btn': true,
        'btn-default': queryTypeId === 'cql',
        'btn-primary': true,
        'input-lg': true
      });
      var newurl = !query ? "#" :
        (window.MyAggregator.URLROOT + (this.props.embedded ? "/embed" : "/") + "?" + encodeQueryData({ queryType: queryTypeId, query: query, mode: 'search' }));
      return (<a className={btnClass} style={{ paddingTop: 13 }}
        type="button" target="_blank" href={newurl}>
        <i className="glyphicon glyphicon-search"></i>
      </a>
      );
    }
    return (
      <button className="btn btn-default input-lg image_button" type="button" onClick={this.search}>
        <i className="glyphicon glyphicon-search"></i>
      </button>
    );
  },

  renderQueryInput() {
    var queryType = queryTypeMap[this.state.queryTypeId];
    return (
      <QueryInput
        searchedLanguages={this.state.searchedLanguages || [multipleLanguageCode]}
        resources={this.props.resources}
        queryTypeId={this.state.queryTypeId}
        query={this.getCurrentQuery() === undefined ? queryType.searchPlaceholder : this.getCurrentQuery()}
        embedded={this.props.embedded}
        placeholder={queryType.searchPlaceholder}
        onQueryChange={this.onQueryChange}
        onKeyDown={this.handleKey} />
    );
  },

  renderEmbed() {
    var queryType = queryTypeMap[this.state.queryTypeId];

    return <div className="aligncenter" style={{ marginLeft: 16, marginRight: 16 }}>
      <div className={"input-group"}>
        <span className="input-group-addon" style={{ backgroundColor: queryType.searchLabelBkColor }}>
          {queryType.searchLabel}
        </span>

        {this.renderQueryInput()}

        <div className="input-group-btn">
          {this.renderSearchButtonOrLink()}
        </div>
      </div>
    </div>
  },

  renderGQB() {
    var queryType = queryTypeMap[this.state.queryTypeId];

    return <div style={{ marginLeft: 16, marginRight: 16 }}>
      <div className="panel panel-default">
        <div className="panel-heading" style={{ backgroundColor: queryType.searchLabelBkColor, fontSize: "120%" }}>
          {queryType.searchLabel}
        </div>

        <div className="panel-body">
          {this.renderQueryInput()}
        </div>

        <div className="panel-footer">
          <div className="input-group">

            <pre className="adv-query-preview aligncenter input-control input-lg">{this.getCurrentQuery()}</pre>

            <div className="input-group-btn">
              {this.renderSearchButtonOrLink()}
            </div>
          </div>
        </div>
      </div>
    </div>
  },

  renderUnavailableResourcesMessage() {
    if (!this.state.resources) {
      return;
    }
    const unavailable = [];
    this.state.resources.recurse((r) => {
      if (r.selected && !r.visible) {
        unavailable.push(r);
      }
      if (r.selected) {
        // apparently a selected resource 
      }
    });

    if (unavailable.length) {
      return <div id="unavailable-resources-message" className="text-muted">
        <div id="unavailable-resources-message-message">
          <a role="button" data-toggle="dropdown">{unavailable.length} selected resource{unavailable.length > 1 ? 's are' : ' is'} disabled in this search mode.</a>
        </div>
        <ul id="unavailable-resources-message-list" className="dropdown-menu">
          {
            unavailable.map((r) => <li className="unavailable-resources-message-item">{r.name}</li>)
          }
        </ul>
      </div>
    }
  },

  render: function () {
    var queryType = queryTypeMap[this.state.queryTypeId];
    return (
      <div className="top-gap">
        <div className="row">
          {(!this.props.embedded && this.state.queryTypeId == "fcs") ? this.renderGQB() : this.renderEmbed()}
        </div>

        <div className="well" style={{ marginTop: 20 }}>
          <div className="aligncenter" >
            {
              //this.renderUnavailableResourcesMessage()
            }
            <form className="form-inline" role="form">

              <div className="input-group">

                <span className="input-group-addon nobkg" >Search for</span>

                <div className="input-group-btn">
                  <button className="form-control btn btn-default"
                    onClick={this.toggleLanguageSelection}>
                    {this.state.language[1]} <span className="caret" />
                  </button>
                  <span />
                </div>
                <div className="input-group-btn hidden-xxs">
                  <ul ref="queryTypeDropdownMenu" className="dropdown-menu">
                    {queryTypes.map(function (l) {
                      var cls = l.disabled ? 'disabled' : '';
                      var handler = function () { if (!l.disabled) this.setQueryType(l.id); }.bind(this);
                      return (<li key={l.id} className={cls}> <a tabIndex="-1" href="#"
                        onClick={handler}> {l.name} </a></li>);
                    }.bind(this))
                    }
                  </ul>

                  <button className="form-control btn btn-default"
                    aria-expanded="false" data-toggle="dropdown" >
                    {queryType.name} <span className="caret" />
                  </button>
                </div>

              </div>

              <div className="input-group hidden-xs">
                <span className="input-group-addon nobkg">in</span>
                <button type="button" className="btn btn-default" onClick={this.toggleResourceSelection}>
                  {this.state.resources.getSelectedMessage()} <span className="caret" />
                </button>
              </div>

              <div className="input-group hidden-xs hidden-sm">
                <span className="input-group-addon nobkg">and show up to</span>
                <div className="input-group-btn">
                  <input type="number" className="form-control input" min="10" max="250"
                    style={{ width: 60 }}
                    onChange={this.setNumberOfResults} value={this.state.numberOfResults}
                    onKeyPress={this.stop} />
                </div>
                <span className="input-group-addon nobkg">hits per endpoint</span>
              </div>

            </form>
          </div>
        </div>

        {this.renderSearchPermaLink()}

        <Modal ref="resourcesModal" title={<span>Resources <small className="text-muted">{this.props.resources && this.props.resources.getSelectedMessage()}</small></span>}>
          <ResourceView resources={this.state.resources} languageMap={this.state.languageMap} />
        </Modal>

        <Modal ref="languageModal" title={<span>Select Language</span>}>
          <LanguageSelector anyLanguage={this.anyLanguage}
            languageMap={this.state.languageMap}
            selectedLanguage={this.state.language}
            languageFilter={this.state.languageFilter}
            languageChangeHandler={this.setLanguageAndFilter} />
        </Modal>

        <Modal ref="resultModal" title={this.renderZoomedResultTitle(this.state.zoomedResourceHit)}>
          <ZoomedResult resourceHit={this.state.zoomedResourceHit}
            nextResults={this.nextResults}
            getDownloadLink={this.getDownloadLink}
            getToWeblichtLink={this.getToWeblichtLink}
            searchedLanguage={this.state.language}
            weblichtLanguages={this.state.weblichtLanguages}
            languageMap={this.state.languageMap}
            queryTypeId={this.state.queryTypeId} />
        </Modal>

        <div className="top-gap">
          <Results collhits={this.filterResults()}
            toggleResultModal={this.toggleResultModal}
            getDownloadLink={this.getDownloadLink}
            getToWeblichtLink={this.getToWeblichtLink}
            searchedLanguage={this.state.language}
            queryTypeId={this.state.queryTypeId} />
        </div>
      </div>
    );
  },
});

function Resources(resources, updateFn) {
  var that = this;
  this.resources = resources;
  this.update = function () {
    updateFn(that);
  };

  var sortFn = function (x, y) {
    var r = x.institution.name.localeCompare(y.institution.name);
    if (r !== 0) {
      return r;
    }
    return x.title.toLowerCase().localeCompare(y.title.toLowerCase());
  };

  this.recurse(function (resource) { resource.subResources.sort(sortFn); });
  this.resources.sort(sortFn);

  this.recurse(function (resource, index) {
    resource.visible = true; // visible in the resource view
    resource.selected = false; // not selected in the resource view, assign later
    resource.expanded = false; // not expanded in the resource view
    resource.priority = 1; // used for ordering search results in resource view
    resource.index = index; // original order, used for stable sort
  });
}

Resources.prototype.recurseResource = function (resource, fn) {
  if (false === fn(resource)) {
    // no recursion
  } else {
    this.recurseResources(resource.subResources, fn);
  }
};

Resources.prototype.recurseResources = function (resources, fn) {
  var recfn = function (resource, index) {
    if (false === fn(resource, index)) {
      // no recursion
    } else {
      resource.subResources.forEach(recfn);
    }
  };
  resources.forEach(recfn);
};

Resources.prototype.recurse = function (fn) {
  this.recurseResources(this.resources, fn);
};

Resources.prototype.getLanguageCodes = function () {
  var languages = {};
  this.recurse(function (resource) {
    resource.languages.forEach(function (lang) {
      languages[lang] = true;
    });
    return true;
  });
  return languages;
};

Resources.prototype.isResourceVisible = function (resource, queryTypeId, languageCode) {
  // check search capabilities (ignore version, just check caps)
  if (queryTypeId === "fcs" && resource.endpoint.searchCapabilities.indexOf("ADVANCED_SEARCH") === -1) {
    return false;
  }
  // yes for any language
  if (languageCode === multipleLanguageCode) {
    return true;
  }
  // yes if the resource is in only that language
  if (resource.languages && resource.languages.length === 1 && resource.languages[0] === languageCode) {
    return true;
  }

  // ? yes if the resource also contains that language
  if (resource.languages && resource.languages.indexOf(languageCode) >= 0) {
    return true;
  }

  // ? yes if the resource has no language
  // if (!resource.languages || resource.languages.length === 0) {
  // 	return true;
  // }
  return false;
};

Resources.prototype.setVisibility = function (queryTypeId, languageCode) {
  // top level
  this.resources.forEach(function (resource) {
    resource.visible = this.isResourceVisible(resource, queryTypeId, languageCode);
    this.recurseResources(resource.subResources, function (c) { c.visible = resource.visible; });
  }.bind(this));
};

Resources.prototype.setAggregationContext = function (endpoints2handles) {
  var selectSubTree = function (select, resource) {
    resource.selected = select;
    this.recurseResources(resource.subResources, function (c) { c.selected = resource.selected; });
  };

  this.resources.forEach(selectSubTree.bind(this, false));

  var handlesNotFound = [];
  var resourcesToSelect = [];
  _.pairs(endpoints2handles).forEach((endp) => {
    var endpoint = endp[0];
    var handles = endp[1];
    console.log("setAggregationContext: endpoint", endpoint);
    console.log("setAggregationContext: handles", handles);
    handles.forEach((handle) => {
      var found = false;
      this.recurse((resource) => {
        if (resource.handle === handle) {
          found = true;
          resourcesToSelect.push(resource);
        }
      })
      if (!found) {
        console.warn("Handle not found in resources", handle);
        handlesNotFound.push(handle);
      }
    })
  })

  resourcesToSelect.forEach(selectSubTree.bind(this, true));
  return { 'selected': resourcesToSelect, 'unavailable': handlesNotFound };
};

Resources.prototype.getSelectedIds = function () {
  var ids = [];
  this.recurse(function (resource) {
    if (resource.visible && resource.selected) {
      ids.push(resource.id);
      //return false; // top-most resource in tree, don't delve deeper
      // But subresources are also selectable on their own?...
    }
    return true;
  });

  // console.log("ids: ", ids.length, {ids:ids});
  return ids;
};

Resources.prototype.getSelectedMessage = function () {
  var selected = this.getSelectedIds().length;
  if (this.resources.length === selected) {
    return "All available resources (" + selected + ")";
  } else if (selected === 1) {
    return "1 selected resource";
  }
  return selected + " selected resources";
};

function getQueryVariable(variable) {
  var query = window.location.search.substring(1);
  var vars = query.split('&');
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split('=');
    if (decodeURIComponent(pair[0]) == variable) {
      console.log("variable found: (", variable, ") = ", decodeURIComponent(pair[1]));
      return decodeURIComponent(pair[1]);
    }
  }
  return null;
}

/* setter opposite of getQueryVariable*/
function setQueryVariable(qvar, value) {
  var query = window.location.search.substring(1);
  var vars = query.split('&');
  var d = {};
  d[qvar] = value;
  var found = false;
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split('=');
    if (decodeURIComponent(pair[0]) === qvar) {

      vars[i] = encodeQueryData(d);
      found = true;
      break;
    }
  }

  if (!found) {
    // add to end of url
    vars.push(encodeQueryData(d));
  }

  var searchPart = vars.join('&');
  var newUrl = window.location.origin + window.location.pathname + '?' + searchPart;
  console.log("set url", newUrl);
  window.history.replaceState(window.history.state, null, newUrl);
}

function encodeQueryData(data) {
  var ret = [];
  for (var d in data) {
    ret.push(encodeURIComponent(d) + "=" + encodeURIComponent(data[d]));
  }
  return ret.join("&");
}

var queryTypes = [
  {
    id: "cql",
    name: "Text layer Contextual Query Language (CQL)",
    searchPlaceholder: "Elephant",
    searchLabel: "Text layer CQL query",
    searchLabelBkColor: "#fed",
    className: '',
  },
  {
    id: "fcs",
    name: "Multi-layer Federated Content Search Query Language (FCS-QL)",
    searchPlaceholder: "[word = 'annotation'][word = 'focused']",
    searchLabel: "Multi-layer FCS query",
    searchLabelBkColor: "#efd",
    disabled: false,
  },
];

var queryTypeMap = {
  cql: queryTypes[0],
  fcs: queryTypes[1],
};

module.exports = AggregatorPage;
