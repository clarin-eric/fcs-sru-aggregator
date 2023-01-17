"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var StatisticsPage = createReactClass({
  // fixme! - class StatisticsPage extends React.Component {
  propTypes: {
    ajax: PT.func.isRequired,
  },

  getInitialState: function () {
    return {
      stats: {},
      activeTab: 0,
      // searchStats: {},
      // lastScanStats: {},
    };
  },

  componentDidMount: function () {
    this.refreshStats();
  },

  refreshStats: function () {
    this.props.ajax({
      url: 'rest/statistics',
      success: function (json, textStatus, jqXHR) {
        this.setState({ stats: json });
        // console.log("stats:", json);
      }.bind(this),
    });
  },

  renderWaitTimeSecs: function (t) {
    var hue = t * 4;
    if (hue > 120) {
      hue = 120;
    }
    var a = hue / 120;
    hue = 120 - hue;
    var shue = "hsla(" + hue + ",100%,80%," + a + ")";
    return <span className="badge" style={{ backgroundColor: shue, color: "black" }}>
      {t.toFixed(3)}s
    </span>;
  },

  renderCollections: function (colls) {
    return <div style={{ marginLeft: 40 }}>
      {colls.length === 0 ?
        <div style={{ color: "#a94442" }}>NO collections found</div>
        :
        <div>
          {colls.length} root collection(s):
          <ul className='list-unstyled' style={{ marginLeft: 40 }}>
            {colls.map(function (name, i) { return <div key={i}>{name}</div>; })}
          </ul>
        </div>
      }
    </div>;
  },

  renderDiagnostic: function (d) {
    var classes = "inline alert alert-warning " + (d.diagnostic.uri === 'LEGACY' ? "legacy" : "");
    return <div key={d.diagnostic.uri}>
      <div className={classes} >
        <div>
          {d.counter <= 1 ? false :
            <div className="inline" style={{ margin: "5px 5px 5px 5px" }}>
              <span className="badge" style={{ backgroundColor: '#ae7241' }}>x {d.counter}</span>
            </div>
          }
          Diagnostic: {d.diagnostic.message}: {d.diagnostic.diagnostic}
        </div>
        <div>Context: <a href={d.context}>{d.context}</a></div>
      </div>
    </div>;
  },

  renderError: function (e) {
    var xc = e.exception;
    return <div key={xc.message}>
      <div className="inline alert alert-danger" role="alert">
        <div>
          {e.counter <= 1 ? false :
            <div className="inline" style={{ margin: "5px 5px 5px 5px" }}>
              <span className="badge" style={{ backgroundColor: '#c94442' }}>x {e.counter} </span>
            </div>
          }
          Exception: {xc.message}
        </div>
        <div>Context: <a href={e.context}>{e.context}</a></div>
        {xc.cause ? <div>Caused by: {xc.cause}</div> : false}
      </div>
    </div>;
  },

  renderEndpoint: function (isScan, endpoint) {
    var stat = endpoint[1];
    var errors = _.values(stat.errors);
    var diagnostics = _.values(stat.diagnostics);
    return <div style={{ marginTop: 10 }} key={endpoint[0]}>
      <ul className='list-inline list-unstyled' style={{ marginBottom: 0 }}>
        <li>
          {stat.version == "LEGACY" ?
            <span style={{ color: '#a94442' }}>legacy <i className="glyphicon glyphicon-thumbs-down"></i> </span>
            : stat.version == "VERSION_1" ? <span style={{ color: '#a94442' }}>version 1 <i className="glyphicon glyphicon-thumbs-down"></i></span>
              : <span style={{ color: '#3c763d' }}>version 2 <i className="glyphicon glyphicon-thumbs-up"></i> </span>
          }
          {" " + endpoint[0]}
        </li>
      </ul>
      <div style={{ marginLeft: 40 }}>
        {isScan ?
          <div>Max concurrent scan requests:{" "} {stat.maxConcurrentRequests} </div> :
          <div>Max concurrent search requests:{" "} {stat.maxConcurrentRequests} </div>
        }
      </div>
      <div style={{ marginLeft: 40 }}>
        <span>{stat.numberOfRequests}</span> request(s),
        average:{this.renderWaitTimeSecs(stat.avgExecutionTime)},
        max: {this.renderWaitTimeSecs(stat.maxExecutionTime)}
      </div>
      {isScan ? this.renderCollections(stat.rootCollections) : false}
      {(errors && errors.length) ?
        <div className='inline' style={{ marginLeft: 40 }}>
          {errors.map(this.renderError)}
        </div> : false
      }
      {(diagnostics && diagnostics.length) ?
        <div className='inline' style={{ marginLeft: 40 }}>
          {diagnostics.map(this.renderDiagnostic)}
        </div> : false
      }
    </div>;
  },

  renderInstitution: function (isScan, inst) {
    return <div style={{ marginTop: 30 }} key={inst[0]}>
      <h4>{inst[0]}</h4>
      <div style={{ marginLeft: 20 }}> {_.pairs(inst[1]).map(this.renderEndpoint.bind(this, isScan))}</div>
    </div>;
  },

  renderStatistics: function (stats) {
    return <div className="container statistics" style={{ marginTop: 20 }}>
      <div>
        <div>Start date: {new Date(stats.date).toLocaleString()}</div>
        <div>Timeout: {" "}<kbd>{stats.timeout} seconds</kbd></div>
      </div>
      <div> {_.pairs(stats.institutions).map(this.renderInstitution.bind(this, stats.isScan))} </div>
    </div>
      ;
  },

  setTab: function (idx) {
    this.setState({ activeTab: idx });
  },

  render: function () {
    return (
      <div>
        <div className="top-gap">
          <h1>Statistics</h1>
          <p />
          <div role="tabpanel">
            <ul className="nav nav-tabs" role="tablist">
              {_.pairs(this.state.stats).map(function (st, idx) {
                var classname = idx === this.state.activeTab ? "active" : "";
                return <li role="presentation" className={classname} key={st[0]}>
                  <a href="#" role="tab" onClick={this.setTab.bind(this, idx)}>{st[0]}</a>
                </li>;
              }.bind(this))
              }
            </ul>

            <div className="tab-content">
              {_.pairs(this.state.stats).map(function (st, idx) {
                var classname = idx === this.state.activeTab ? "tab-pane active" : "tab-pane";
                return <div role="tabpanel" className={classname} key={st[0]}>
                  {this.renderStatistics(st[1])}
                </div>;
              }.bind(this))
              }
            </div>
          </div>
        </div>
      </div>
    );
  },
});

module.exports = StatisticsPage;
