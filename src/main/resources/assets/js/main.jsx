import AboutPage from "./pages/aboutpage.jsx";
import AggregatorPage from "./pages/aggregatorpage.jsx";
import HelpPage from "./pages/helppage.jsx";
import StatisticsPage from "./pages/statisticspage.jsx";
import AlertPane from "./components/alertpane.jsx";
import Footer from "./components/footer.jsx";
import EmbeddedFooter from "./components/embeddedfooter.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

(function () {
  "use strict";

  window.MyAggregator = window.MyAggregator || {};
  window._paq = window._paq || [];

  var VERSION = window.MyAggregator.VERSION = "v.3.9.0";

  // TODO: set this via environment variables at build time (envify)
  var URLROOT = window.MyAggregator.URLROOT = "";
  var APIROOT = window.MyAggregator.APIROOT = "rest/";
  var CURURL = (document.location || window.location).href;

  var PT = PropTypes;

  /**
  The FCS Aggregator UI is based on reactjs.
  - index.html: describes the general page structure, with a push-down footer;
    on that structure the Main and Footer components are plugged.
  - main.jsx: composes the simple top components (Main, AggregatorPage, HelpPage, 
    AboutPage, StatisticsPage) in pages/
  - pages/aggregatorpage.jsx: defines
      - the Corpora store of resources
      - the AggregatorPage component which deals with search and displays the search results
  - components/resourceview.jsx: defines the ResourceView, rendered when the user views the available resources
  - plus in components/: various general usage React components
  
  The top-most component, Main, tracks of the window's location URL and, depending on the value,
    renders various components inside its frame:
      - AggregatorPage is the view corresponding to the normal search UI (search bar and all)
        This is the most complex component.
      - HelpPage renders the help page
      - AboutPage renders the about page
      - StatisticsPage renders the stats page
      - another URL, /Aggregator/embed, determines Main and AggregatorPage to render just the search bar.
        The embedded view is supposed to work like a YouTube embedded clip.
  */

  var Main = createReactClass({
    // fixme! - class Main extends React.Component {
    componentWillMount: function () {
      routeFromLocation.bind(this)();
    },

    getInitialState: function () {
      return {
        navbarCollapse: false,
        navbarPageFn: this.renderAggregator,
        alerts: [],  // messages (info/error)
        initialSearchId: null,  // search id extracted from URL
      };
    },

    error: function (errObj) {
      var err = "";
      if (typeof errObj === 'string' || errObj instanceof String) {
        err = errObj;
      } else if (typeof errObj === 'object' && errObj.statusText) {
        console.log("ERROR: jqXHR = ", errObj);
        err = errObj.statusText;
      } else {
        return;
      }
      this.alert(err, "error");
    },

    info: function (msgObj) {
      var msg = "";
      if (typeof msgObj === 'string' || msgObj instanceof String) {
        msg = msgObj;
      } else if (typeof msgObj === 'object') {
        console.log("INFO: obj = ", msgObj);
        msg = JSON.stringify(msgObj);
      } else {
        return;
      }
      this.alert(msg, "info");
    },

    alert: function (message, type) {
      var that = this;
      var alerts = this.state.alerts.slice();
      alerts.push({ msg: message, type: type });
      this.setState({ alerts: alerts });

      setTimeout(function () {
        var alerts = that.state.alerts.slice();
        alerts.shift();
        that.setState({ alerts: alerts });
      }, 10000);
    },

    ajax: function (ajaxObject) {
      var that = this;
      if (!ajaxObject.error) {
        ajaxObject.error = function (jqXHR, textStatus, error) {
          if (jqXHR.readyState === 0) {
            that.error("Network error, please check your internet connection");
          } else if (jqXHR.responseText) {
            that.error(jqXHR.responseText + " (" + error + ")");
          } else {
            that.error(error + " (" + textStatus + ")");
          }
          console.log("ajax error, jqXHR: ", jqXHR);
        };
      }
      // console.log("ajax", ajaxObject);
      jQuery.ajax(ajaxObject);
    },

    toggleCollapse: function () {
      this.setState({ navbarCollapse: !this.state.navbarCollapse });
    },

    renderAggregator: function () {
      return <AggregatorPage APIROOT={APIROOT} ajax={this.ajax} error={this.error} info={this.info} embedded={false} searchId={this.state.initialSearchId} />;
    },

    renderHelp: function () {
      return <HelpPage />;
    },

    renderAbout: function () {
      return <AboutPage toStatistics={this.toStatistics} />;
    },

    renderStatistics: function () {
      return <StatisticsPage APIROOT={APIROOT} ajax={this.ajax} />;
    },

    renderEmbedded: function () {
      return <AggregatorPage APIROOT={APIROOT} ajax={this.ajax} error={this.error} embedded={true} />;
    },

    getPageFns: function () {
      return {
        '': this.renderAggregator,
        'help': this.renderHelp,
        'about': this.renderAbout,
        'stats': this.renderStatistics,
        'embed': this.renderEmbedded,
      };
    },

    gotoPage: function (doPushHistory, pageFnName) {
      var pageFn = this.getPageFns()[pageFnName];
      if (this.state.navbarPageFn !== pageFn) {
        if (doPushHistory) {
          window.history.pushState({ page: pageFnName }, '', URLROOT + "/" + pageFnName);
        }
        this.setState({ navbarPageFn: pageFn });
        console.log("new page: " + document.location + ", name: " + pageFnName);

        _paq.push(['setReferrerUrl', CURURL]);
        CURURL = (document.location || window.location).pathname;
        _paq.push(['setCustomUrl', CURURL]);
        _paq.push(['trackPageView']);
      }
    },

    toAggregator: function (doPushHistory) { this.gotoPage(doPushHistory, ''); },
    toHelp: function (doPushHistory) { this.gotoPage(doPushHistory, 'help'); },
    toAbout: function (doPushHistory) { this.gotoPage(doPushHistory, 'about'); },
    toStatistics: function (doPushHistory) { this.gotoPage(doPushHistory, 'stats'); },
    toEmbedded: function (doPushHistory) { this.gotoPage(doPushHistory, 'embed'); },

    renderLogin: function () {
      return false;
      // return  <li className="unauthenticated">
      // 			<a href="login" tabIndex="-1"><span className="glyphicon glyphicon-log-in"></span> LOGIN</a>
      // 		</li>;
    },

    renderCollapsible: function () {
      var classname = "navbar-collapse collapse " + (this.state.navbarCollapse ? "in" : "");
      return (
        <div className={classname}>
          <ul className="nav navbar-nav">
            <li className={this.state.navbarPageFn === this.renderAggregator ? "active" : ""}>
              <a className="link" tabIndex="-1" onClick={this.toAggregator.bind(this, true)}>Aggregator</a>
            </li>
            <li className={this.state.navbarPageFn === this.renderHelp ? "active" : ""}>
              <a className="link" tabIndex="-1" onClick={this.toHelp.bind(this, true)}>Help</a>
            </li>
          </ul>
          <ul className="nav navbar-nav navbar-right">
            <li> <div id="clarinservices" style={{ padding: 4, paddingTop: 8 }} /> </li>
            {this.renderLogin()}
          </ul>
        </div>
      );
    },

    renderTop: function () {
      if (this.state.navbarPageFn === this.renderEmbedded) {
        return false;
      }
      return (
        <div>
          <div className="navbar navbar-default navbar-static-top" role="navigation">
            <div className="container">
              <div className="navbar-header">
                <button type="button" className="navbar-toggle" onClick={this.toggleCollapse}>
                  <span className="sr-only">Toggle navigation</span>
                  <span className="icon-bar"></span>
                  <span className="icon-bar"></span>
                  <span className="icon-bar"></span>
                </button>
                <a className="navbar-brand" href={URLROOT + "/"} tabIndex="-1">
                  <img width="28px" height="28px" src="img/magglass1.png" />
                  <header className="inline"> Content Search </header>
                </a>
              </div>
              {this.renderCollapsible()}
            </div>
          </div>

          <AlertPane alerts={this.state.alerts} />

        </div>
      );
    },

    render: function () {
      return (
        <div>
          <div> {this.renderTop()} </div>

          <div id="push">
            <div className="container">
              {this.state.navbarPageFn()}
            </div>
            <div className="top-gap" />
          </div>
        </div>
      );
    }
  });

  // StatisticsPage

  // HelpPage

  // AboutPage

  // Footer

  // EmbeddedFooter

  function isEmbeddedView() {
    var path = window.location.pathname.split('/');
    return (path.length >= 3 && path[path.length - 1] === 'embed');
  }

  function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
  }

  var routeFromLocation = function () {
    var url = (document.location || window.location);
    console.log("routeFromLocation: " + url);
    if (!this) throw "routeFromLocation must be bound to main";
    var path = url.pathname.slice(URLROOT.length).split('/').slice(1);
    console.log("path: " + path);
    var pageFnName = url.pathname.slice(URLROOT.length).replace(/^\/+/, "");
    console.log("pageFnName: " + pageFnName);
    if (pageFnName === 'help') {
      this.toHelp(false);
    } else if (pageFnName === 'about') {
      this.toAbout(false);
    } else if (pageFnName === 'stats') {
      this.toStatistics(false);
    } else if (pageFnName === 'embed') {
      this.toEmbedded(false);
    } else if (pageFnName.startsWith("search-")) {
      var searchId = Number.parseInt(pageFnName.substring(7));
      this.setState({ initialSearchId: searchId });
      this.toAggregator(false);
    } else {
      this.toAggregator(false);
    }
  };

  var main = ReactDOM.render(<Main />, document.getElementById('body'));
  if (!isEmbeddedView()) {
    ReactDOM.render(<Footer VERSION={VERSION} toAbout={main.toAbout} />, document.getElementById('footer'));
  } else {
    ReactDOM.render(<EmbeddedFooter URLROOT={URLROOT} />, document.getElementById('footer'));
    if (jQuery) { jQuery('body, #footer').addClass('embedded'); }
  }

  window.onpopstate = routeFromLocation.bind(main);

})();
