"use strict";
import classNames from "classnames";
import CorpusView from "../components/corpusview.jsx";
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
    embedded: PT.bool.isRequired
  },

  nohits: {
    results: null,
  },
  anyLanguage: [multipleLanguageCode, "Any Language"],

  getInitialState: function () {
    var aggrContext = getQueryVariable('x-aggregation-context');
    aggrContext = aggrContext && JSON.parse(aggrContext);

    return {
      corpora: new Corpora([], this.updateCorpora),
      languageMap: {},
      weblichtLanguages: [],
      queryTypeId: getQueryVariable('queryType') || 'cql',
      cqlQuery: ((getQueryVariable('queryType') || 'cql') === 'cql') && getQueryVariable('query') || '',
      fcsQuery: ((getQueryVariable('queryType') || 'cql') === 'fcs') && getQueryVariable('query') || '',
      aggregationContext: aggrContext || null,
      language: this.anyLanguage,
      languageFilter: 'byMeta',
      numberOfResults: 10,

      searchId: null,
      timeout: 0,
      hits: this.nohits,

      zoomedCorpusHit: null,
    };
  },

  componentDidMount: function () {
    this._isMounted = true;

    this.props.ajax({
      url: this.props.APIROOT + 'init',
      success: function (json, textStatus, jqXHR) {
        if (this._isMounted) {
          var corpora = new Corpora(json.corpora, this.updateCorpora);

          // // for testing aggregation context
          // json['x-aggregation-context'] = {
          // 	'EKUT': ["http://hdl.handle.net/11858/00-1778-0000-0001-DDAF-D"]
          // };

          var aggregationContext = json['x-aggregation-context'] || this.state.aggregationContext;

          window.MyAggregator.mode = getQueryVariable('mode') || json.mode;
          window.MyAggregator.corpora = json.corpora;
          window.MyAggregator.xAggregationContext = aggregationContext;

          // Setting visibility, e.g. only corpora 
          // from v2.0 endpoints for fcs v2.0
          corpora.setVisibility(this.state.queryTypeId, this.state.language[0]);

          if (aggregationContext) {
            const contextCorporaInfo = corpora.setAggregationContext(aggregationContext);
            const unavailableCorporaHandles = contextCorporaInfo.unavailable; // list of unavailable aggregationContext
            if (unavailableCorporaHandles.length > 0) {
              this.props.error("Could not find requested collection handles:\n" + unavailableCorporaHandles.join('\n'));
            }

            const actuallySelectedCorpora = corpora.getSelectedIds();

            if (contextCorporaInfo.selected.length !== actuallySelectedCorpora.length) {
              if (actuallySelectedCorpora.length === 0) {
                this.props.error("This search does not support the required collection(s), will search all collections instead"); // TODO give detailed reason its not supported.
                corpora.recurse(function (corpus) { corpus.selected = true; });
              } else {
                var err = "Some required context collections are not supported for this search:\n"
                err = err + contextCorpora.filter((c) => {
                  if (actuallySelectedCorpora.indexOf(c) === -1) {
                    console.warn("Requested corpus but not available for selection", c);
                    return true;
                  }
                  return false;
                }).map((c) => c.title).join('\n')
                this.props.error(err);
              }
            } else if (contextCorporaInfo.selected.length > 0) {
              this.props.info("Pre-selected " + contextCorporaInfo.selected.length + " collection(s):\n" + contextCorporaInfo.selected.map(x => x.title + " (" + x.handle + ")").join('\n'));
            }
          }
          else {
            // no context set all visibl to selected as default.
            console.log("no context set, selecting all available");
            corpora.recurse(c => { c.visible ? c.selected = true : null })
          }

          this.setState({
            corpora: corpora,
            languageMap: json.languages,
            weblichtLanguages: json.weblichtLanguages,
            aggregationContext: aggregationContext,
          }, this.postInit);
        }
        else {
          console.warn("Got Aggregator init response, but not mounted!");
        }
      }.bind(this),
    });
  },

  postInit() {
    if (window.MyAggregator.mode === 'search') {
      this.search();
    }
  },

  updateCorpora: function (corpora) {
    this.setState({ corpora: corpora });
  },

  getCurrentQuery() {
    return this.state.queryTypeId === 'fcs' ? this.state.fcsQuery : this.state.cqlQuery;
  },

  search() {
    var query = this.getCurrentQuery();
    var queryTypeId = this.state.queryTypeId;
    if (!query || (this.props.embedded && window.MyAggregator.mode !== 'search')) {
      this.setState({ hits: this.nohits, searchId: null });
      return;
    }
    var selectedIds = this.state.corpora.getSelectedIds();
    if (!selectedIds.length) {
      this.props.error("Please select a collection to search into");
      return;
    }

    // console.log("searching in the following corpora:", selectedIds);
    // console.log("searching with queryType:", queryTypeId);
    this.props.ajax({
      url: this.props.APIROOT + 'search',
      type: "POST",
      data: {
        query: query,
        queryType: queryTypeId,
        language: this.state.language[0],
        numberOfResults: this.state.numberOfResults,
        corporaIds: selectedIds,
      },
      success: function (searchId, textStatus, jqXHR) {
        // console.log("search ["+query+"] ok: ", searchId, jqXHR);
        //Piwik.getAsyncTracker().trackSiteSearch(query, queryTypeId);
        // automatic inclusion of piwik in prod
        //console.log("location.hostname: " + location.hostname);
        if (location.hostname !== "localhost") {
          //console.log("location.host: " + location.host);
          _paq.push(['trackSiteSearch', query, queryTypeId, false]);
        }

        var timeout = 250;
        setTimeout(this.refreshSearchResults, timeout);
        this.setState({ searchId: searchId, timeout: timeout });
      }.bind(this),
    });
  },

  nextResults: function (corpusId) {
    // console.log("searching next results in corpus:", corpusId);
    this.props.ajax({
      url: this.props.APIROOT + 'search/' + this.state.searchId,
      type: "POST",
      data: {
        corpusId: corpusId,
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
        var corpusHit = this.state.zoomedCorpusHit;
        if (corpusHit) {
          for (var resi = 0; resi < json.results.length; resi++) {
            var res = json.results[resi];
            if (res.corpus.id === corpusHit.corpus.id) {
              corpusHit = res;
              break;
            }
          }
        }
        this.setState({ hits: json, timeout: timeout, zoomedCorpusHit: corpusHit });
      }.bind(this),
    });
  },

  getExportParams: function (corpusId, format, filterLanguage) {
    var params = corpusId ? { corpusId: corpusId } : {};
    if (format) params.format = format;
    if (filterLanguage) {
      params.filterLanguage = filterLanguage;
    } else if (this.state.languageFilter === 'byGuess' || this.state.languageFilter === 'byMetaAndGuess') {
      params.filterLanguage = this.state.language[0];
    }
    return encodeQueryData(params);
  },

  getDownloadLink: function (corpusId, format) {
    return this.props.APIROOT + 'search/' + this.state.searchId + '/download?' +
      this.getExportParams(corpusId, format);
  },

  getToWeblichtLink: function (corpusId, forceLanguage) {
    return this.props.APIROOT + 'search/' + this.state.searchId + '/toWeblicht?' +
      this.getExportParams(corpusId, null, forceLanguage);
  },

  setLanguageAndFilter: function (languageObj, languageFilter) {
    this.state.corpora.setVisibility(this.state.queryTypeId,
      languageFilter === 'byGuess' ? multipleLanguageCode : languageObj[0]);
    this.setState({
      language: languageObj,
      languageFilter: languageFilter,
      corpora: this.state.corpora, // === this.state.corpora.update();
    });
  },

  setQueryType: function (queryTypeId) {
    this.state.corpora.setVisibility(queryTypeId, this.state.language[0]);
    setQueryVariable('queryType', queryTypeId);
    setQueryVariable('query', queryTypeId === 'cql' ? this.state.cqlQuery : this.state.fcsQuery)
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
      results = this.state.hits.results.map(function (corpusHit) {
        return {
          corpus: corpusHit.corpus,
          inProgress: corpusHit.inProgress,
          exception: corpusHit.exception,
          diagnostics: corpusHit.diagnostics,
          kwics: noLangFiltering ? corpusHit.kwics :
            corpusHit.kwics.filter(function (kwic) {
              return kwic.language === langCode ||
                langCode === multipleLanguageCode ||
                langCode === null;
            }),
          advancedLayers: noLangFiltering ? corpusHit.advancedLayers :
            corpusHit.advancedLayers.filter(function (layer) {
              return layer.language === langCode ||
                langCode === multipleLanguageCode ||
                langCode === null;
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

  toggleCorpusSelection: function (e) {
    $(ReactDOM.findDOMNode(this.refs.corporaModal)).modal();
    e.preventDefault();
    e.stopPropagation();
  },

  toggleResultModal: function (e, corpusHit) {
    $(ReactDOM.findDOMNode(this.refs.resultModal)).modal();
    this.setState({ zoomedCorpusHit: corpusHit });
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

  renderZoomedResultTitle: function (corpusHit) {
    if (!corpusHit) return (<span />);
    var corpus = corpusHit.corpus;
    return (<h3 style={{ fontSize: '1em' }}>
      {corpus.title}
      {corpus.landingPage ?
        <a href={corpus.landingPage} onClick={this.stop} style={{ fontSize: 12 }}>
          <span> – Homepage </span>
          <i className="glyphicon glyphicon-home" />
        </a> : false}
    </h3>);
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
        corpora={this.props.corpora}
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

  renderUnavailableCorporaMessage() {
    if (!this.state.corpora) {
      return;
    }
    const unavailable = [];
    this.state.corpora.recurse((c) => {
      if (c.selected && !c.visible) {
        unavailable.push(c);
      }
      if (c.selected) {
        // apparently a selected corpus 
      }
    });

    if (unavailable.length) {
      return <div id="unavailable-corpora-message" className="text-muted">
        <div id="unavailable-corpora-message-message">
          <a role="button" data-toggle="dropdown">{unavailable.length} selected collection{unavailable.length > 1 ? 's are' : ' is'} disabled in this search mode.</a>
        </div>
        <ul id="unavailable-corpora-message-list" className="dropdown-menu">
          {
            unavailable.map((c) => <li className="unavailable-corpora-message-item">{c.name}</li>)
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
              //this.renderUnavailableCorporaMessage()
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
                <button type="button" className="btn btn-default" onClick={this.toggleCorpusSelection}>
                  {this.state.corpora.getSelectedMessage()} <span className="caret" />
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

        <Modal ref="corporaModal" title={<span>Collections <small className="text-muted">{this.props.corpora && this.props.corpora.getSelectedMessage()}</small></span>}>
          <CorpusView corpora={this.state.corpora} languageMap={this.state.languageMap} />
        </Modal>

        <Modal ref="languageModal" title={<span>Select Language</span>}>
          <LanguageSelector anyLanguage={this.anyLanguage}
            languageMap={this.state.languageMap}
            selectedLanguage={this.state.language}
            languageFilter={this.state.languageFilter}
            languageChangeHandler={this.setLanguageAndFilter} />
        </Modal>

        <Modal ref="resultModal" title={this.renderZoomedResultTitle(this.state.zoomedCorpusHit)}>
          <ZoomedResult corpusHit={this.state.zoomedCorpusHit}
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

function Corpora(corpora, updateFn) {
  var that = this;
  this.corpora = corpora;
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

  this.recurse(function (corpus) { corpus.subCorpora.sort(sortFn); });
  this.corpora.sort(sortFn);

  this.recurse(function (corpus, index) {
    corpus.visible = true; // visible in the corpus view
    corpus.selected = false; // not selected in the corpus view, assign later
    corpus.expanded = false; // not expanded in the corpus view
    corpus.priority = 1; // used for ordering search results in corpus view
    corpus.index = index; // original order, used for stable sort
  });
}

Corpora.prototype.recurseCorpus = function (corpus, fn) {
  if (false === fn(corpus)) {
    // no recursion
  } else {
    this.recurseCorpora(corpus.subCorpora, fn);
  }
};

Corpora.prototype.recurseCorpora = function (corpora, fn) {
  var recfn = function (corpus, index) {
    if (false === fn(corpus, index)) {
      // no recursion
    } else {
      corpus.subCorpora.forEach(recfn);
    }
  };
  corpora.forEach(recfn);
};

Corpora.prototype.recurse = function (fn) {
  this.recurseCorpora(this.corpora, fn);
};

Corpora.prototype.getLanguageCodes = function () {
  var languages = {};
  this.recurse(function (corpus) {
    corpus.languages.forEach(function (lang) {
      languages[lang] = true;
    });
    return true;
  });
  return languages;
};

Corpora.prototype.isCorpusVisible = function (corpus, queryTypeId, languageCode) {
  // check search capabilities (ignore version, just check caps)
  if (queryTypeId === "fcs" && corpus.endpoint.searchCapabilities.indexOf("ADVANCED_SEARCH") === -1) {
    return false;
  }
  // yes for any language
  if (languageCode === multipleLanguageCode) {
    return true;
  }
  // yes if the corpus is in only that language
  if (corpus.languages && corpus.languages.length === 1 && corpus.languages[0] === languageCode) {
    return true;
  }

  // ? yes if the corpus also contains that language
  if (corpus.languages && corpus.languages.indexOf(languageCode) >= 0) {
    return true;
  }

  // ? yes if the corpus has no language
  // if (!corpus.languages || corpus.languages.length === 0) {
  // 	return true;
  // }
  return false;
};

Corpora.prototype.setVisibility = function (queryTypeId, languageCode) {
  // top level
  this.corpora.forEach(function (corpus) {
    corpus.visible = this.isCorpusVisible(corpus, queryTypeId, languageCode);
    this.recurseCorpora(corpus.subCorpora, function (c) { c.visible = corpus.visible; });
  }.bind(this));
};

Corpora.prototype.setAggregationContext = function (endpoints2handles) {
  var selectSubTree = function (select, corpus) {
    corpus.selected = select;
    this.recurseCorpora(corpus.subCorpora, function (c) { c.selected = corpus.selected; });
  };

  this.corpora.forEach(selectSubTree.bind(this, false));

  var handlesNotFound = [];
  var corporaToSelect = [];
  _.pairs(endpoints2handles).forEach((endp) => {
    var endpoint = endp[0];
    var handles = endp[1];
    console.log('setAggregationContext: endpoint', endpoint);
    console.log('setAggregationContext: handles', handles);
    handles.forEach((handle) => {
      var found = false;
      this.recurse((corpus) => {
        if (corpus.handle === handle) {
          found = true;
          corporaToSelect.push(corpus);
        }
      })
      if (!found) {
        console.warn("Handle not found in corpora", handle);
        handlesNotFound.push(handle);
      }
    })
  })

  corporaToSelect.forEach(selectSubTree.bind(this, true));
  return { 'selected': corporaToSelect, 'unavailable': handlesNotFound };
};

Corpora.prototype.getSelectedIds = function () {
  var ids = [];
  this.recurse(function (corpus) {
    if (corpus.visible && corpus.selected) {
      ids.push(corpus.id);
      //eturn false; // top-most collection in tree, don't delve deeper
      // But subcollections are also selectable on their own?...
    }
    return true;
  });

  // console.log("ids: ", ids.length, {ids:ids});
  return ids;
};

Corpora.prototype.getSelectedMessage = function () {
  var selected = this.getSelectedIds().length;
  if (this.corpora.length === selected) {
    return "All available collections (" + selected + ")";
  } else if (selected === 1) {
    return "1 selected collection";
  }
  return selected + " selected collections";
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
