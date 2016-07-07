(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
"use strict";

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol ? "symbol" : typeof obj; };

(function () {
	"use strict";

	var VERSION = window.MyAggregator.VERSION = "v.2.9.90-alpha-56";

	var URLROOT = window.MyAggregator.URLROOT = window.location.pathname.substring(0, window.location.pathname.indexOf("/", 2)) || "/Aggregator";

	var PT = React.PropTypes;

	var ErrorPane = window.MyReact.ErrorPane;
	var AggregatorPage = window.MyAggregator.AggregatorPage;

	/**
 The FCS Aggregator UI is based on reactjs.
 - index.html: describes the general page structure, with a push-down footer;
   on that structure the Main and Footer components are plugged.
 - main.jsx: defines the simple top components (Main, HelpPage, AboutPage, StatisticsPage)
 - search.jsx: defines
 	- the Corpora store of collections
 	- the AggregatorPage component which deals with search and displays the search results
 - corpora.jsx: defines the CorpusView, rendered when the user views the available collections
 - components.jsx: various general usage React components
 
 The top-most component, Main, tracks of the window's location URL and, depending on the value,
   renders various components inside its frame:
 	- AggregatorPage is the view corresponding to the normal search UI (search bar and all)
 	  This is the most complex component.
 	- HelpPage renders the help page
 	- About renders the about page
 	- Statistics renders the stats page
 	- another URL, /Aggregator/embed, determines Main and AggregatorPage to render just the search bar.
 	  The embedded view is supposed to work like a YouTube embedded clip.
 */

	var Main = React.createClass({
		displayName: "Main",

		componentWillMount: function componentWillMount() {
			routeFromLocation.bind(this)();
		},

		getInitialState: function getInitialState() {
			return {
				navbarCollapse: false,
				navbarPageFn: this.renderAggregator,
				errorMessages: []
			};
		},

		error: function error(errObj) {
			var err = "";
			if (typeof errObj === 'string' || errObj instanceof String) {
				err = errObj;
			} else if ((typeof errObj === "undefined" ? "undefined" : _typeof(errObj)) === 'object' && errObj.statusText) {
				console.log("ERROR: jqXHR = ", errObj);
				err = errObj.statusText;
			} else {
				return;
			}

			var that = this;
			var errs = this.state.errorMessages.slice();
			errs.push(err);
			this.setState({ errorMessages: errs });

			setTimeout(function () {
				var errs = that.state.errorMessages.slice();
				errs.shift();
				that.setState({ errorMessages: errs });
			}, 10000);
		},

		ajax: function ajax(ajaxObject) {
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

		toggleCollapse: function toggleCollapse() {
			this.setState({ navbarCollapse: !this.state.navbarCollapse });
		},

		renderAggregator: function renderAggregator() {
			return React.createElement(AggregatorPage, { ajax: this.ajax, error: this.error });
		},

		renderHelp: function renderHelp() {
			return React.createElement(HelpPage, null);
		},

		renderAbout: function renderAbout() {
			return React.createElement(AboutPage, null);
		},

		renderStatistics: function renderStatistics() {
			return React.createElement(StatisticsPage, { ajax: this.ajax });
		},

		renderEmbedded: function renderEmbedded() {
			return React.createElement(AggregatorPage, { ajax: this.ajax, error: this.error, embedded: true });
		},

		getPageFns: function getPageFns() {
			return {
				'': this.renderAggregator,
				'help': this.renderHelp,
				'about': this.renderAbout,
				'stats': this.renderStatistics,
				'embed': this.renderEmbedded
			};
		},

		gotoPage: function gotoPage(doPushHistory, pageFnName) {
			var pageFn = this.getPageFns()[pageFnName];
			if (this.state.navbarPageFn !== pageFn) {
				if (doPushHistory) {
					window.history.pushState({ page: pageFnName }, '', URLROOT + "/" + pageFnName);
				}
				this.setState({ navbarPageFn: pageFn });
				// console.log("new page: " + document.location + ", name: " + pageFnName);
			}
		},

		toAggregator: function toAggregator(doPushHistory) {
			this.gotoPage(doPushHistory, '');
		},
		toHelp: function toHelp(doPushHistory) {
			this.gotoPage(doPushHistory, 'help');
		},
		toAbout: function toAbout(doPushHistory) {
			this.gotoPage(doPushHistory, 'about');
		},
		toStatistics: function toStatistics(doPushHistory) {
			this.gotoPage(doPushHistory, 'stats');
		},
		toEmbedded: function toEmbedded(doPushHistory) {
			this.gotoPage(doPushHistory, 'embed');
		},

		renderLogin: function renderLogin() {
			return false;
			// return  <li className="unauthenticated">
			// 			<a href="login" tabIndex="-1"><span className="glyphicon glyphicon-log-in"></span> LOGIN</a>
			// 		</li>;
		},

		renderCollapsible: function renderCollapsible() {
			var classname = "navbar-collapse collapse " + (this.state.navbarCollapse ? "in" : "");
			return React.createElement(
				"div",
				{ className: classname },
				React.createElement(
					"ul",
					{ className: "nav navbar-nav" },
					React.createElement(
						"li",
						{ className: this.state.navbarPageFn === this.renderAggregator ? "active" : "" },
						React.createElement(
							"a",
							{ className: "link", tabIndex: "-1", onClick: this.toAggregator.bind(this, true) },
							"Aggregator"
						)
					),
					React.createElement(
						"li",
						{ className: this.state.navbarPageFn === this.renderHelp ? "active" : "" },
						React.createElement(
							"a",
							{ className: "link", tabIndex: "-1", onClick: this.toHelp.bind(this, true) },
							"Help"
						)
					)
				),
				React.createElement(
					"ul",
					{ className: "nav navbar-nav navbar-right" },
					React.createElement(
						"li",
						null,
						" ",
						React.createElement("div", { id: "clarinservices", style: { padding: 4 } }),
						" "
					),
					this.renderLogin()
				)
			);
		},

		renderTop: function renderTop() {
			if (this.state.navbarPageFn === this.renderEmbedded) {
				return false;
			}
			return React.createElement(
				"div",
				null,
				React.createElement(
					"div",
					{ className: "navbar navbar-default navbar-static-top", role: "navigation" },
					React.createElement(
						"div",
						{ className: "container" },
						React.createElement(
							"div",
							{ className: "navbar-header" },
							React.createElement(
								"button",
								{ type: "button", className: "navbar-toggle", onClick: this.toggleCollapse },
								React.createElement(
									"span",
									{ className: "sr-only" },
									"Toggle navigation"
								),
								React.createElement("span", { className: "icon-bar" }),
								React.createElement("span", { className: "icon-bar" }),
								React.createElement("span", { className: "icon-bar" })
							),
							React.createElement(
								"a",
								{ className: "navbar-brand", href: URLROOT, tabIndex: "-1" },
								React.createElement("img", { width: "28px", height: "28px", src: "img/magglass1.png" }),
								React.createElement(
									"header",
									{ className: "inline" },
									" Content Search "
								)
							)
						),
						this.renderCollapsible()
					)
				),
				React.createElement(ErrorPane, { errorMessages: this.state.errorMessages })
			);
		},

		render: function render() {
			return React.createElement(
				"div",
				null,
				React.createElement(
					"div",
					null,
					" ",
					this.renderTop(),
					" "
				),
				React.createElement(
					"div",
					{ id: "push" },
					React.createElement(
						"div",
						{ className: "container" },
						this.state.navbarPageFn()
					),
					React.createElement("div", { className: "top-gap" })
				)
			);
		}
	});

	var StatisticsPage = React.createClass({
		displayName: "StatisticsPage",

		propTypes: {
			ajax: PT.func.isRequired
		},

		getInitialState: function getInitialState() {
			return {
				stats: {},
				activeTab: 0
			};
		},

		componentDidMount: function componentDidMount() {
			this.refreshStats();
		},

		refreshStats: function refreshStats() {
			this.props.ajax({
				url: 'rest/statistics',
				success: function (json, textStatus, jqXHR) {
					this.setState({ stats: json });
					// console.log("stats:", json);
				}.bind(this)
			});
		},

		renderWaitTimeSecs: function renderWaitTimeSecs(t) {
			var hue = t * 4;
			if (hue > 120) {
				hue = 120;
			}
			var a = hue / 120;
			hue = 120 - hue;
			var shue = "hsla(" + hue + ",100%,80%," + a + ")";
			return React.createElement(
				"span",
				{ className: "badge", style: { backgroundColor: shue, color: "black" } },
				t.toFixed(3),
				"s"
			);
		},

		renderCollections: function renderCollections(colls) {
			return React.createElement(
				"div",
				{ style: { marginLeft: 40 } },
				colls.length === 0 ? React.createElement(
					"div",
					{ style: { color: "#a94442" } },
					"NO collections found"
				) : React.createElement(
					"div",
					null,
					colls.length,
					" root collection(s):",
					React.createElement(
						"ul",
						{ className: "list-unstyled", style: { marginLeft: 40 } },
						colls.map(function (name, i) {
							return React.createElement(
								"div",
								{ key: i },
								name
							);
						})
					)
				)
			);
		},

		renderDiagnostic: function renderDiagnostic(d) {
			var classes = "inline alert alert-warning " + (d.diagnostic.uri === 'LEGACY' ? "legacy" : "");
			return React.createElement(
				"div",
				{ key: d.diagnostic.uri },
				React.createElement(
					"div",
					{ className: classes },
					React.createElement(
						"div",
						null,
						d.counter <= 1 ? false : React.createElement(
							"div",
							{ className: "inline", style: { margin: "5px 5px 5px 5px" } },
							React.createElement(
								"span",
								{ className: "badge", style: { backgroundColor: '#ae7241' } },
								"x ",
								d.counter
							)
						),
						"Diagnostic: ",
						d.diagnostic.message,
						": ",
						d.diagnostic.diagnostic
					),
					React.createElement(
						"div",
						null,
						"Context: ",
						React.createElement(
							"a",
							{ href: d.context },
							d.context
						)
					)
				)
			);
		},

		renderError: function renderError(e) {
			var xc = e.exception;
			return React.createElement(
				"div",
				{ key: xc.message },
				React.createElement(
					"div",
					{ className: "inline alert alert-danger", role: "alert" },
					React.createElement(
						"div",
						null,
						e.counter <= 1 ? false : React.createElement(
							"div",
							{ className: "inline", style: { margin: "5px 5px 5px 5px" } },
							React.createElement(
								"span",
								{ className: "badge", style: { backgroundColor: '#c94442' } },
								"x ",
								e.counter,
								" "
							)
						),
						"Exception: ",
						xc.message
					),
					React.createElement(
						"div",
						null,
						"Context: ",
						React.createElement(
							"a",
							{ href: e.context },
							e.context
						)
					),
					xc.cause ? React.createElement(
						"div",
						null,
						"Caused by: ",
						xc.cause
					) : false
				)
			);
		},

		renderEndpoint: function renderEndpoint(isScan, endpoint) {
			var stat = endpoint[1];
			var errors = _.values(stat.errors);
			var diagnostics = _.values(stat.diagnostics);
			return React.createElement(
				"div",
				{ style: { marginTop: 10 }, key: endpoint[0] },
				React.createElement(
					"ul",
					{ className: "list-inline list-unstyled", style: { marginBottom: 0 } },
					React.createElement(
						"li",
						null,
						stat.version == "LEGACY" ? React.createElement(
							"span",
							{ style: { color: '#a94442' } },
							"legacy ",
							React.createElement("i", { className: "glyphicon glyphicon-thumbs-down" }),
							" "
						) : React.createElement(
							"span",
							{ style: { color: '#3c763d' } },
							React.createElement("i", { className: "glyphicon glyphicon-thumbs-up" }),
							" "
						),
						" " + endpoint[0]
					)
				),
				React.createElement(
					"div",
					{ style: { marginLeft: 40 } },
					isScan ? React.createElement(
						"div",
						null,
						"Max concurrent scan requests:",
						" ",
						" ",
						stat.maxConcurrentRequests,
						" "
					) : React.createElement(
						"div",
						null,
						"Max concurrent search requests:",
						" ",
						" ",
						stat.maxConcurrentRequests,
						" "
					)
				),
				React.createElement(
					"div",
					{ style: { marginLeft: 40 } },
					React.createElement(
						"span",
						null,
						stat.numberOfRequests
					),
					" request(s), average:",
					this.renderWaitTimeSecs(stat.avgExecutionTime),
					", max: ",
					this.renderWaitTimeSecs(stat.maxExecutionTime)
				),
				isScan ? this.renderCollections(stat.rootCollections) : false,
				errors && errors.length ? React.createElement(
					"div",
					{ className: "inline", style: { marginLeft: 40 } },
					errors.map(this.renderError)
				) : false,
				diagnostics && diagnostics.length ? React.createElement(
					"div",
					{ className: "inline", style: { marginLeft: 40 } },
					diagnostics.map(this.renderDiagnostic)
				) : false
			);
		},

		renderInstitution: function renderInstitution(isScan, inst) {
			return React.createElement(
				"div",
				{ style: { marginTop: 30 }, key: inst[0] },
				React.createElement(
					"h4",
					null,
					inst[0]
				),
				React.createElement(
					"div",
					{ style: { marginLeft: 20 } },
					" ",
					_.pairs(inst[1]).map(this.renderEndpoint.bind(this, isScan))
				)
			);
		},

		renderStatistics: function renderStatistics(stats) {
			return React.createElement(
				"div",
				{ className: "container statistics", style: { marginTop: 20 } },
				React.createElement(
					"div",
					null,
					React.createElement(
						"div",
						null,
						"Start date: ",
						new Date(stats.date).toLocaleString()
					),
					React.createElement(
						"div",
						null,
						"Timeout: ",
						" ",
						React.createElement(
							"kbd",
							null,
							stats.timeout,
							" seconds"
						)
					)
				),
				React.createElement(
					"div",
					null,
					" ",
					_.pairs(stats.institutions).map(this.renderInstitution.bind(this, stats.isScan)),
					" "
				)
			);
		},

		setTab: function setTab(idx) {
			this.setState({ activeTab: idx });
		},

		render: function render() {
			return React.createElement(
				"div",
				null,
				React.createElement(
					"div",
					{ className: "top-gap" },
					React.createElement(
						"h1",
						null,
						"Statistics"
					),
					React.createElement("p", null),
					React.createElement(
						"div",
						{ role: "tabpanel" },
						React.createElement(
							"ul",
							{ className: "nav nav-tabs", role: "tablist" },
							_.pairs(this.state.stats).map(function (st, idx) {
								var classname = idx === this.state.activeTab ? "active" : "";
								return React.createElement(
									"li",
									{ role: "presentation", className: classname, key: st[0] },
									React.createElement(
										"a",
										{ href: "#", role: "tab", onClick: this.setTab.bind(this, idx) },
										st[0]
									)
								);
							}.bind(this))
						),
						React.createElement(
							"div",
							{ className: "tab-content" },
							_.pairs(this.state.stats).map(function (st, idx) {
								var classname = idx === this.state.activeTab ? "tab-pane active" : "tab-pane";
								return React.createElement(
									"div",
									{ role: "tabpanel", className: classname, key: st[0] },
									this.renderStatistics(st[1])
								);
							}.bind(this))
						)
					)
				)
			);
		}
	});

	var HelpPage = React.createClass({
		displayName: "HelpPage",

		openHelpDesk: function openHelpDesk() {
			window.open('http://support.clarin-d.de/mail/form.php?queue=Aggregator&lang=en', '_blank', 'height=560,width=370');
		},

		render: function render() {
			return React.createElement(
				"div",
				null,
				React.createElement(
					"div",
					{ className: "top-gap" },
					React.createElement(
						"h1",
						null,
						"Help"
					),
					React.createElement(
						"h3",
						null,
						"Performing search in Federated Content Search corpora"
					),
					React.createElement(
						"p",
						null,
						"To perform a simple keyword search in all CLARIN Federated Content Search centres and their corpora, go to the search field at the top of the page, enter your query, and click the 'search' button or press the 'Enter' key."
					),
					React.createElement(
						"p",
						null,
						"To perform an advanced search on multiple annotation layers in CLARIN Federated Content Search centres that support this, switch to Multi-layer Federated Content Search (FCS) in the dropdown list, enter a FCS query in the search field at the top of the page, and click the 'search' button or press the 'Enter' key."
					),
					React.createElement(
						"p",
						null,
						"When the search starts, the page will start filling in with the corpora responses. After the entire search process has ended you have the option to download the results in various formats."
					),
					React.createElement(
						"p",
						null,
						"If you are particularly interested in the results returned by a corpus, you have the option to focus only on the results of that corpus, by clicking on the 'Watch' button. In this view mode you can also download the results of use the WebLicht processing services to further analyse the results."
					),
					React.createElement(
						"h3",
						null,
						"Adjusting search criteria"
					),
					React.createElement(
						"p",
						null,
						"The FCS Aggregator makes possible to select specific corpora based on their name or language and to specify the number of search results (hits) per corpus per page. The user interface controls that allows to change these options are located right below the search fiels on the main page. The current options are to filter resources based on their language, to select specific resources, and to set the maximum number of hits. In the multi-layer FCS search the supported layers filter on the supported features like, e. g. part of speech or lemma in addition to the other filter options."
					),
					React.createElement(
						"h3",
						null,
						"More help"
					),
					React.createElement(
						"p",
						null,
						"More detailed information on using FCS Aggregator is available at the  ",
						React.createElement(
							"a",
							{ href: "http://weblicht.sfs.uni-tuebingen.de/weblichtwiki/index.php/FCS_Aggregator" },
							"Aggregator wiki page"
						),
						". If you still cannot find an answer to your question, or if want to send a feedback, you can write to Clarin-D helpdesk: "
					),
					React.createElement(
						"button",
						{ type: "button", className: "btn btn-default btn-lg", onClick: this.openHelpDesk },
						React.createElement("span", { className: "glyphicon glyphicon-question-sign", "aria-hidden": "true" }),
						" HelpDesk"
					)
				)
			);
		}
	});

	var AboutPage = React.createClass({
		displayName: "AboutPage",

		render: function render() {
			return React.createElement(
				"div",
				null,
				React.createElement(
					"div",
					{ className: "top-gap" },
					React.createElement(
						"h1",
						{ style: { padding: 15 } },
						"About"
					),
					React.createElement(
						"div",
						{ className: "col-md-6" },
						React.createElement(
							"h3",
							null,
							"People"
						),
						React.createElement(
							"ul",
							null,
							React.createElement(
								"li",
								null,
								"Emanuel Dima"
							),
							React.createElement(
								"li",
								null,
								"Leif-Jöran Olsson"
							),
							React.createElement(
								"li",
								null,
								"Yana Panchenko"
							),
							React.createElement(
								"li",
								null,
								"Oliver Schonefeld"
							),
							React.createElement(
								"li",
								null,
								"Dieter Van Uytvanck"
							)
						),
						React.createElement(
							"h3",
							null,
							"Statistics"
						),
						React.createElement(
							"button",
							{ type: "button", className: "btn btn-default btn-lg", onClick: function onClick() {
									main.toStatistics(true);
								} },
							React.createElement(
								"span",
								{ className: "glyphicon glyphicon-cog", "aria-hidden": "true" },
								" "
							),
							"View server log"
						)
					),
					React.createElement(
						"div",
						{ className: "col-md-6" },
						React.createElement(
							"h3",
							null,
							"Technology"
						),
						React.createElement(
							"p",
							null,
							"The Aggregator uses the following software components:"
						),
						React.createElement(
							"ul",
							null,
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://dropwizard.io/" },
									"Dropwizard"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://www.apache.org/licenses/LICENSE-2.0" },
									"Apache License 2.0"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://eclipse.org/jetty/" },
									"Jetty"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://www.apache.org/licenses/LICENSE-2.0" },
									"Apache License 2.0"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://jackson.codehaus.org/" },
									"Jackson"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://www.apache.org/licenses/LICENSE-2.0" },
									"Apache License 2.0"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "https://jersey.java.net/" },
									"Jersey"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "https://jersey.java.net/license.html#/cddl" },
									"CCDL 1.1"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "https://github.com/optimaize/language-detector" },
									"Optimaize Language Detector"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://www.apache.org/licenses/LICENSE-2.0" },
									"Apache License 2.0"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://poi.apache.org/" },
									"Apache POI"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://www.apache.org/licenses/LICENSE-2.0" },
									"Apache License 2.0"
								),
								")"
							)
						),
						React.createElement(
							"ul",
							null,
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://facebook.github.io/react/" },
									"React"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "https://github.com/facebook/react/blob/master/LICENSE" },
									"BSD license"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://getbootstrap.com/" },
									"Bootstrap"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://opensource.org/licenses/mit-license.html" },
									"MIT license"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://jquery.com/" },
									"jQuery"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://opensource.org/licenses/mit-license.html" },
									"MIT license"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://glyphicons.com/" },
									"GLYPHICONS free"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "https://creativecommons.org/licenses/by/3.0/" },
									"CC-BY 3.0"
								),
								")"
							),
							React.createElement(
								"li",
								null,
								React.createElement(
									"a",
									{ href: "http://fortawesome.github.io/Font-Awesome/" },
									"FontAwesome"
								),
								" ",
								"(",
								React.createElement(
									"a",
									{ href: "http://opensource.org/licenses/mit-license.html" },
									"MIT"
								),
								", ",
								React.createElement(
									"a",
									{ href: "http://scripts.sil.org/OFL" },
									"SIL Open Font License"
								),
								")"
							)
						),
						React.createElement(
							"p",
							null,
							"The content search icon is made by",
							React.createElement(
								"a",
								{ href: "http://www.freepik.com", title: "Freepik" },
								" Freepik "
							),
							"from",
							React.createElement(
								"a",
								{ href: "http://www.flaticon.com", title: "Flaticon" },
								" www.flaticon.com "
							),
							"and licensed under",
							React.createElement(
								"a",
								{ href: "http://creativecommons.org/licenses/by/3.0/", title: "Creative Commons BY 3.0" },
								" CC BY 3.0 "
							)
						)
					)
				)
			);
		}
	});

	var Footer = React.createClass({
		displayName: "Footer",

		toAbout: function toAbout(e) {
			main.toAbout(true);
			e.preventDefault();
			e.stopPropagation();
		},

		render: function render() {
			return React.createElement(
				"div",
				{ className: "container", style: { textAlign: 'center' } },
				React.createElement(
					"div",
					{ className: "row" },
					React.createElement(
						"div",
						{ style: { position: 'relative', float: 'left' } },
						React.createElement(
							"div",
							{ className: "leftist", style: { position: 'absolute' } },
							React.createElement(
								"div",
								null,
								React.createElement(
									"a",
									{ title: "about", href: "about", onClick: this.toAbout },
									"About"
								)
							),
							React.createElement(
								"div",
								{ style: { color: '#777' } },
								VERSION
							)
						)
					),
					React.createElement(
						"a",
						{ title: "CLARIN ERIC", href: "https://www.clarin.eu/" },
						React.createElement("img", { src: "img/clarindLogo.png", alt: "CLARIN ERIC logo", style: { height: 60 } })
					),
					React.createElement(
						"div",
						{ style: { position: 'relative', float: 'right' } },
						React.createElement(
							"div",
							{ className: "rightist", style: { position: 'absolute', right: '0' } },
							React.createElement(
								"a",
								{ title: "contact", href: "mailto:fcs@clarin.eu" },
								"Contact"
							)
						)
					)
				)
			);
		}
	});

	var EmbeddedFooter = React.createClass({
		displayName: "EmbeddedFooter",

		render: function render() {
			return React.createElement(
				"div",
				{ className: "container", style: { textAlign: 'center' } },
				React.createElement(
					"div",
					{ className: "row" },
					React.createElement(
						"div",
						{ style: { position: 'relative', float: 'right' } },
						React.createElement(
							"div",
							{ className: "rightist", style: { position: 'absolute', right: 0, width: 150 } },
							React.createElement(
								"a",
								{ href: URLROOT, target: "_blank", tabIndex: "-1" },
								React.createElement("img", { width: "28px", height: "28px", src: "img/magglass1.png" }),
								React.createElement(
									"header",
									{ className: "inline float-left" },
									" Content Search "
								)
							)
						)
					)
				)
			);
		}
	});

	function isEmbeddedView() {
		var path = window.location.pathname.split('/');
		return path.length >= 3 && path[2] === 'embed';
	}

	function endsWith(str, suffix) {
		return str.indexOf(suffix, str.length - suffix.length) !== -1;
	}

	var routeFromLocation = function routeFromLocation() {
		// console.log("routeFromLocation: " + document.location);
		if (!this) throw "routeFromLocation must be bound to main";
		var path = window.location.pathname.split('/');
		if (path.length === 3) {
			var p = path[2];
			if (p === 'help') {
				this.toHelp(false);
			} else if (p === 'about') {
				this.toAbout(false);
			} else if (p === 'stats') {
				this.toStatistics(false);
			} else if (p === 'embed') {
				this.toEmbedded(false);
			} else {
				this.toAggregator(false);
			}
		} else {
			this.toAggregator(false);
		}
	};

	var main = ReactDOM.render(React.createElement(Main, null), document.getElementById('body'));
	if (!isEmbeddedView()) {
		ReactDOM.render(React.createElement(Footer, null), document.getElementById('footer'));
	} else {
		React.render(React.createElement(EmbeddedFooter, null), document.getElementById('footer'));
		if (jQuery) {
			jQuery('body, #footer').addClass('embedded');
		}
	}

	window.onpopstate = routeFromLocation.bind(main);
	window.MyAggregator.main = main;
})();

},{}]},{},[1]);
