"use strict";
import classNames from "classnames";
import ResultMixin from "./resultmixin.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";
import { CSSTransition, TransitionGroup } from "react-transition-group";

var PT = PropTypes;

var ZoomedResult = createReactClass({
  propTypes: {
    resourceHit: PT.object,
    nextResults: PT.func.isRequired,
    languageMap: PT.object.isRequired,
    weblichtLanguages: PT.array.isRequired,
    searchedLanguage: PT.array.isRequired,
    getDownloadLink: PT.func.isRequired,
    getToWeblichtLink: PT.func.isRequired,
    queryTypeId: PT.string.isRequired,
  },
  mixins: [ResultMixin],

  getInitialState: function () {
    return {
      forceUpdate: 1, // hack to force an update, used when searching for next results
    };
  },

  nextResults: function (e) {
    this.props.resourceHit.inProgress = true;
    this.setState({ forceUpdate: this.state.forceUpdate + 1 });
    this.props.nextResults(this.props.resourceHit.resource.id);
  },

  renderLanguages: function (languages) {
    return languages
      .map(function (l) { return this.props.languageMap[l]; }.bind(this))
      .sort()
      .join(", ");
  },

  renderMoreResults: function () {
    if (this.props.resourceHit.inProgress)
      return (<span style={{ fontStyle: 'italic' }}>Retrieving results, please wait...</span>);

    var moreResults = true;
    for (var i = 0; i < this.props.resourceHit.diagnostics.length; i++) {
      var d = this.props.resourceHit.diagnostics[i];
      if (d.uri === window.MyAggregator.NO_MORE_RECORDS_DIAGNOSTIC_URI) {
        moreResults = false;
        break;
      }
    }
    if (!moreResults)
      return (<span style={{ fontStyle: 'italic' }}>No other results available for this query</span>);
    return (<button className="btn btn-default" onClick={this.nextResults}>
      <span className="glyphicon glyphicon-option-horizontal" aria-hidden="true" /> More Results
    </button>);
  },

  render: function () {
    var resourceHit = this.props.resourceHit;
    if (!resourceHit) {
      return false;
    }

    var forceLanguage = null, wlerror = null;
    if (this.props.weblichtLanguages.indexOf(this.props.searchedLanguage[0]) < 0) {
      // the search language is either AnyLanguage or unsupported
      if (this.props.searchedLanguage[0] === window.MyAggregator.multipleLanguageCode) {
        if (resourceHit.resource.languages && resourceHit.resource.languages.length === 1) {
          forceLanguage = resourceHit.resource.languages[0];
        } else {
          var langs = resourceHit.kwics.map(function (kwic) { return kwic.language; });
          langs = _.uniq(langs.filter(function (l) { return l !== null; }));
          if (langs.length === 1) {
            forceLanguage = langs[0];
          }
        }
      }
      if (!forceLanguage) {
        wlerror = "Cannot use WebLicht: unsupported language (" + this.props.searchedLanguage[1] + ")";
      }
    }
    var resource = resourceHit.resource;
    return (<div>
      <div className='resourceDescription'>
        <p><i className="fa fa-institution" /> {resource.institution.name}</p>
        {resource.description ?
          <p><i className="glyphicon glyphicon-info-sign" /> {resource.description}</p> : false}
        <p><i className="fa fa-language" /> {this.renderLanguages(resource.languages)}</p>
      </div>
      <div style={{ marginBottom: 2 }}>
        <div className="float-right">
          <div>
            {this.renderDisplayKWIC()}
            {this.props.queryTypeId !== "fcs" ? "" : this.renderDisplayADV()}
            <div className="inline"> {this.renderDownloadLinks(resourceHit.resource.id)} </div>
            <div className="inline"> {this.renderToWeblichtLinks(resource.id, forceLanguage, wlerror)} </div>
          </div>
        </div>
        <div style={{ clear: 'both' }} />
      </div>
      <TransitionGroup>
        <CSSTransition classNames="fade" timeout={{ enter: 200, exit: 200 }}>
          <div className="panel">
            <div className="panel-body resourceResults">{this.renderPanelBody(resourceHit)}</div>
          </div>
        </CSSTransition>
      </TransitionGroup>
      <div style={{ textAlign: 'center', marginTop: 10 }}>
        {this.renderMoreResults()}
      </div>

    </div>);
  },
});

module.exports = ZoomedResult;
