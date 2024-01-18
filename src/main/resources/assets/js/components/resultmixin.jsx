"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";

var PT = PropTypes;

window.MyAggregator = window.MyAggragtor || {};
var NO_MORE_RECORDS_DIAGNOSTIC_URI = window.MyAggregator.NO_MORE_RECORDS_DIAGNOSTIC_URI = "info:srw/diagnostic/1/61";

var ResultMixin = {
  // getDefaultProps: function(){
  // 	return {hasPopover: true};
  // },

  getInitialState: function () {
    return {
      displayKwic: false,
      displayADV: false,
    };
  },

  toggleKwic: function () {
    this.setState({ displayKwic: !this.state.displayKwic });
  },

  toggleADV: function () {
    this.setState({ displayADV: !this.state.displayADV });
  },

  renderPanelTitle: function (resource) {
    return (<div className='inline'>
      <span className="resourceName"> {resource.title}</span>
      <span className="institutionName"> — {resource.institution}</span>
    </div>);
  },

  renderRowLanguage: function (hit) {
    return false; //<span style={{fontFace:"Courier",color:"black"}}>{hit.language} </span> ;
  },

  renderRefLink: function (hit) {
    if (!hit.reference) {
      return false;
    }
    return (<a href={hit.reference} target="_blank" title="Go to resource hit">
      <span class="glyphicon glyphicon-link" style={{ marginRight: ".5em" }} aria-hidden="true"></span>
    </a>);
  },

  renderRowsAsHits: function (hit, i) {
    function renderTextFragments(tf, idx) {
      return (<span key={idx} className={tf.hit ? "keyword" : ""}>{tf.text}</span>);
    }
    return (<p key={i} className="hitrow">
      {this.renderRowLanguage(hit)}
      {this.renderRefLink(hit)}
      {hit.fragments.map(renderTextFragments)}
    </p>);
  },

  renderRowsAsKwic: function (hit, i) {
    var sleft = { textAlign: "left", verticalAlign: "top", width: "50%" };
    var scenter = { textAlign: "center", verticalAlign: "top", maxWidth: "50%" };
    var sright = { textAlign: "right", verticalAlign: "top", maxWidth: "50%" };
    return (<tr key={i} className="hitrow">
      <td>{this.renderRowLanguage(hit)}</td>
      <td style={sright}>{hit.left}</td>
      <td style={scenter} className="keyword">{hit.keyword}</td>
      <td style={sleft}>{hit.right}</td>
    </tr>);
  },

  renderRowsAsADV: function (hit, i) {
    var sleft = { textAlign: "left", verticalAlign: "top", width: "50%" };
    var scenter = { textAlign: "center", verticalAlign: "top", maxWidth: "50%" };
    var sright = { textAlign: "right", verticalAlign: "top", maxWidth: "50%" };

    function renderSpans(span, idx) {
      return <td key={idx} className={span.hit ? "keyword" : ""}>{span.text}</td>;
    }
    return (<tr key={i} className="hitrow">
      {this.renderRowLanguage(hit)}
      <td style={sleft}>{hit.pid}</td>
      <td style={sleft}>{hit.reference}</td>
      {hit.spans.map(renderSpans)}
    </tr>);
  },

  renderRowsAsADVGrouped: function (resourceHit) {
    function renderWithSeperators(layers, i) {
      var pre = (i != 0) ? [(
        <tr class="hitrow-sep"><td colspan="100%" /></tr>
      )] : [];
      return pre.concat(layers.map(this.renderRowsAsADV));
    }
    function renderPlainList(layers, i) {
      return layers.map(this.renderRowsAsADV);
    }
    var needsSeparators = Math.min(...resourceHit.advancedLayers.map(x => x.length)) > 1;
    return resourceHit.advancedLayers.map((needsSeparators ? renderWithSeperators : renderPlainList).bind(this));
  },

  renderDiagnostic: function (d, key) {
    if (d.uri === window.MyAggregator.NO_MORE_RECORDS_DIAGNOSTIC_URI) {
      return false;
    }
    return (<div className="alert alert-warning" key={key}>
      <div>{d.message}</div>
    </div>);
  },

  renderDiagnostics: function (resourceHit) {
    if (!resourceHit.diagnostics || resourceHit.diagnostics.length === 0) {
      return false;
    }
    return resourceHit.diagnostics.map(this.renderDiagnostic);
  },

  renderErrors: function (resourceHit) {
    var xc = resourceHit.exception;
    if (!xc) {
      return false;
    }
    return (
      <div className="alert alert-danger" role="alert">
        <div>Exception: {xc.message}</div>
        {xc.cause ? <div>Caused by: {xc.cause}</div> : false}
      </div>
    );
  },

  renderPanelBody: function (resourceHit) {
    var fulllength = { width: "100%" };

    if (this.state.displayADV) {
      return (<div className="resourceResultsADV">
        {this.renderErrors(resourceHit)}
        {this.renderDiagnostics(resourceHit)}
        <table className="table table-condensed table-hover advanced-layers" style={fulllength}>
          <tbody>{this.renderRowsAsADVGrouped(resourceHit)}</tbody>
        </table>
      </div>);
    } else if (this.state.displayKwic) {
      return (<div>
        {this.renderErrors(resourceHit)}
        {this.renderDiagnostics(resourceHit)}
        <table className="table table-condensed table-hover kwic" style={fulllength}>
          <tbody>{resourceHit.kwics.map(this.renderRowsAsKwic)}</tbody>
        </table>
      </div>);
    } else {
      return (<div>
        {this.renderErrors(resourceHit)}
        {this.renderDiagnostics(resourceHit)}
        {resourceHit.kwics.map(this.renderRowsAsHits)}
      </div>);
    }
  },

  renderDisplayKWIC: function () {
    return (<div className="inline btn-group" style={{ display: "inline-block" }}>
      <label htmlFor="inputKwic" className="btn btn-flat">
        {this.state.displayKwic ?
          <input id="inputKwic" type="checkbox" value="kwic" checked onChange={this.toggleKwic} /> :
          <input id="inputKwic" type="checkbox" value="kwic" onChange={this.toggleKwic} />
        }
        &nbsp;
        Display as Key Word In Context
      </label>
    </div>);
  },

  renderDisplayADV: function () {
    return (<div className="inline btn-group" style={{ display: "inline-block" }}>
      <label htmlFor="inputADV" className="btn btn-flat">
        {this.state.displayADV ?
          <input id="inputADV" type="checkbox" value="adv" checked onChange={this.toggleADV} /> :
          <input id="inputADV" type="checkbox" value="adv" onChange={this.toggleADV} />
        }
        &nbsp;
        Display as AdvancedDataView (ADV)
      </label>
    </div>);
  },

  renderDownloadLinks: function (resourceId) {
    return (
      <div className="dropdown">
        <button className="btn btn-flat" aria-expanded="false" data-toggle="dropdown">
          <span className="glyphicon glyphicon-download-alt" aria-hidden="true" />
          {" "} Download {" "}
          <span className="caret" />
        </button>
        <ul className="dropdown-menu">
          <li> <a href={this.props.getDownloadLink(resourceId, "csv")}>
            {" "} As CSV file</a></li>
          <li> <a href={this.props.getDownloadLink(resourceId, "ods")}>
            {" "} As ODS file</a></li>
          <li> <a href={this.props.getDownloadLink(resourceId, "excel")}>
            {" "} As Excel file</a></li>
          <li> <a href={this.props.getDownloadLink(resourceId, "tcf")}>
            {" "} As TCF file</a></li>
          <li> <a href={this.props.getDownloadLink(resourceId, "text")}>
            {" "} As Plain Text file</a></li>
        </ul>
      </div>
    );
  },

  renderToWeblichtLinks: function (resourceId, forceLanguage, error) {
    return (
      <div className="dropdown">
        <button className="btn btn-flat" aria-expanded="false" data-toggle="dropdown">
          <span className="glyphicon glyphicon-export" aria-hidden="true" />
          {" "} Use Weblicht {" "}
          <span className="caret" />
        </button>
        <ul className="dropdown-menu">
          <li>
            {error ?
              <div className="alert alert-danger" style={{ margin: 10, width: 200 }}>{error}</div> :
              <a href={this.props.getToWeblichtLink(resourceId, forceLanguage)} target="_blank">{" "}
                Send to Weblicht</a>
            }
          </li>
        </ul>
      </div>
    );
  },

};

module.exports = ResultMixin;
