/** @jsx React.DOM */
(function() {
"use strict";

var VERSION = window.MyAggregator.VERSION = "VERSION 2.0.0-beta-36";

var URLROOT = window.MyAggregator.URLROOT =
	window.location.pathname.substring(0, window.location.pathname.indexOf("/",2)) ||
	"/Aggregator";

var PT = React.PropTypes;

var ErrorPane = window.MyReact.ErrorPane;
var AggregatorPage = window.MyAggregator.AggregatorPage;

var Main = React.createClass({displayName: 'Main',
	componentWillMount: function() {
		routeFromLocation.bind(this)();
	},

	getInitialState: function () {
		return {
			navbarCollapse: false,
			navbarPageFn: this.renderAggregator,
			// navbarPageFn: this.renderStatistics,
			errorMessages: [],
		};
	},

	error: function(errObj) {
		var err = "";
		if (typeof errObj === 'string' || errObj instanceof String) {
			err = errObj;
		} else if (typeof errObj === 'object' && errObj.statusText) {
			console.log("ERROR: jqXHR = ", errObj);
			err = errObj.statusText;
		} else {
			return;
		}

		var that = this;
		var errs = this.state.errorMessages.slice();
		errs.push(err);
		this.setState({errorMessages: errs});

		setTimeout(function() {
			var errs = that.state.errorMessages.slice();
			errs.shift();
			that.setState({errorMessages: errs});
		}, 10000);
	},

	ajax: function(ajaxObject) {
		var that = this;
		if (!ajaxObject.error) {
			ajaxObject.error = function(jqXHR, textStatus, error) {
				if (jqXHR.readyState === 0) {
					that.error("Network error, please check your internet connection");
				} else if (jqXHR.responseText) {
					that.error(jqXHR.responseText + " ("+error+")");
				} else  {
					that.error(error + " ("+textStatus+")");
				}
				console.log("ajax error, jqXHR: ", jqXHR);
			};
		}
		// console.log("ajax", ajaxObject);
		jQuery.ajax(ajaxObject);
	},

	toggleCollapse: function() {
		this.setState({navbarCollapse: !this.state.navbarCollapse});
	},

	renderAggregator: function() {
		return React.createElement(AggregatorPage, {ajax: this.ajax, error: this.error});
	},

	renderHelp: function() {
		return React.createElement(HelpPage, null);
	},

	renderAbout: function() {
		return React.createElement(AboutPage, null);
	},

	renderStatistics: function() {
		return React.createElement(StatisticsPage, {ajax: this.ajax});
	},

	renderEmbedded: function() {
		return React.createElement(AggregatorPage, {ajax: this.ajax, embedded: true});
	},

	getPageFns: function() {
		return {
			'': this.renderAggregator,
			'help': this.renderHelp,
			'about': this.renderAbout,
			'stats': this.renderStatistics,
			'embed': this.renderEmbedded,
		};
	},

	gotoPage: function(doPushHistory, pageFnName) {
		var pageFn = this.getPageFns()[pageFnName];
		if (this.state.navbarPageFn !== pageFn) {
			if (doPushHistory) {
				window.history.pushState({page:pageFnName}, '', URLROOT+"/"+pageFnName);
			}
			this.setState({navbarPageFn: pageFn});
			// console.log("new page: " + document.location + ", name: " + pageFnName);
		}
	},

	toAggregator: function(doPushHistory) { this.gotoPage(doPushHistory, ''); },
	toHelp: function(doPushHistory) { this.gotoPage(doPushHistory, 'help'); },
	toAbout: function(doPushHistory) { this.gotoPage(doPushHistory, 'about'); },
	toStatistics: function(doPushHistory) { this.gotoPage(doPushHistory, 'stats'); },
	toEmbedded: function(doPushHistory) { this.gotoPage(doPushHistory, 'embed'); },

	renderLogin: function() {
		return false;
		// return  <li className="unauthenticated">
		// 			<a href="login" tabIndex="-1"><span className="glyphicon glyphicon-log-in"></span> LOGIN</a>
		// 		</li>;
	},

	renderCollapsible: function() {
		var classname = "navbar-collapse collapse " + (this.state.navbarCollapse?"in":"");
		return (
			React.createElement("div", {className: classname}, 
				React.createElement("ul", {className: "nav navbar-nav"}, 
					React.createElement("li", {className: this.state.navbarPageFn === this.renderAggregator ? "active":""}, 
						React.createElement("a", {className: "link", tabIndex: "-1", onClick: this.toAggregator.bind(this, true)}, "Aggregator")
					), 
					React.createElement("li", {className: this.state.navbarPageFn === this.renderHelp ? "active":""}, 
						React.createElement("a", {className: "link", tabIndex: "-1", onClick: this.toHelp.bind(this, true)}, "Help")
					)
				), 
				React.createElement("ul", {id: "CLARIN_header_right", className: "nav navbar-nav navbar-right"}, 
					React.createElement("li", null, 
						React.createElement("div", {id: "clarinservices", style: {padding:10}})
					), 
					this.renderLogin()
				)
			)
		);
	},

	renderTop: function() {
		if (this.state.navbarPageFn === this.renderEmbedded) {
			return false;
		}
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "container"}, 
					React.createElement("div", {className: "beta-tag"}, 
						React.createElement("span", null, "BETA")
					)
				), 

				React.createElement("div", {className: "navbar navbar-default navbar-static-top", role: "navigation"}, 
					React.createElement("div", {className: "container"}, 
						React.createElement("div", {className: "navbar-header"}, 
							React.createElement("button", {type: "button", className: "navbar-toggle", onClick: this.toggleCollapse}, 
								React.createElement("span", {className: "sr-only"}, "Toggle navigation"), 
								React.createElement("span", {className: "icon-bar"}), 
								React.createElement("span", {className: "icon-bar"}), 
								React.createElement("span", {className: "icon-bar"})
							), 
							React.createElement("a", {className: "navbar-brand", href: URLROOT, tabIndex: "-1"}, React.createElement("header", null, "Federated Content Search"))
						), 
						this.renderCollapsible()
					)
				), 

				React.createElement(ErrorPane, {errorMessages: this.state.errorMessages})
			)
		);
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", null, " ",  this.renderTop(), " "), 

				React.createElement("div", {id: "push"}, 
					React.createElement("div", {className: "container"}, 
						this.state.navbarPageFn()
		 			), 
		 			React.createElement("div", {className: "top-gap"})
				)
			)
		);
	}
});


var StatisticsPage = React.createClass({displayName: 'StatisticsPage',
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

	componentDidMount: function() {
		this.refreshStats();
	},

	refreshStats: function() {
		this.props.ajax({
			url: 'rest/statistics',
			success: function(json, textStatus, jqXHR) {
				this.setState({stats: json});
				// console.log("stats:", json);
			}.bind(this),
		});
	},

	renderWaitTimeSecs: function(t) {
		var hue = t * 4;
		if (hue > 120) {
			hue = 120;
		}
		var a = hue/120;
		hue = 120 - hue;
		var shue = "hsla("+hue+",100%,80%,"+a+")";
		return	React.createElement("span", {className: "badge", style: {backgroundColor:shue, color:"black"}}, 
					t.toFixed(3), "s"
				);
	},

	renderCollections: function(colls) {
		return	React.createElement("div", {style: {marginLeft:40}}, 
					 colls.length === 0 ?
						React.createElement("div", {style: {color:"#a94442"}}, "NO collections found")
						:
						React.createElement("div", null, 
							colls.length, " root collection(s):", 
							React.createElement("ul", {className: "list-unstyled", style: {marginLeft:40}}, 
								 colls.map(function(name, i) { return React.createElement("div", {key: i}, name); }) 
							)
						)
					
				);
	},

	renderDiagnostic: function(d) {
		var classes = "inline alert alert-warning " + (d.diagnostic.uri === 'LEGACY' ? "legacy" : "");
		return 	React.createElement("div", {key: d.diagnostic.uri}, 
					React.createElement("div", {className: classes}, 
						React.createElement("div", null, 
							 d.counter <= 1 ? false :
								React.createElement("div", {className: "inline", style: {margin:"5px 5px 5px 5px"}}, 
									React.createElement("span", {className: "badge", style: {backgroundColor:'#ae7241'}}, "x ", d.counter)
								), 
							
							"Diagnostic: ", d.diagnostic.message, ": ", d.diagnostic.diagnostic
						), 
						React.createElement("div", null, "Context: ", React.createElement("a", {href: d.context}, d.context))
					)
				);
	},

	renderError: function(e) {
		var xc = e.exception;
		return 	React.createElement("div", {key: xc.message}, 
					React.createElement("div", {className: "inline alert alert-danger", role: "alert"}, 
						React.createElement("div", null, 
							 e.counter <= 1 ? false :
								React.createElement("div", {className: "inline", style: {margin:"5px 5px 5px 5px"}}, 
									React.createElement("span", {className: "badge", style: {backgroundColor:'#c94442'}}, "x ", e.counter, " ")
								), 
							
							"Exception: ", xc.message
						), 
						React.createElement("div", null, "Context: ", React.createElement("a", {href: e.context}, e.context)), 
						 xc.cause ? React.createElement("div", null, "Caused by: ", xc.cause) : false
					)
				);
	},

	renderEndpoint: function(isScan, endpoint) {
		var stat = endpoint[1];
		var errors = _.values(stat.errors);
		var diagnostics = _.values(stat.diagnostics);
		return React.createElement("div", {style: {marginTop:10}, key: endpoint[0]}, 
					React.createElement("ul", {className: "list-inline list-unstyled", style: {marginBottom:0}}, 
						React.createElement("li", null, 
							 stat.version == "LEGACY" ?
								React.createElement("span", {style: {color:'#a94442'}}, "legacy ", React.createElement("i", {className: "glyphicon glyphicon-thumbs-down"}), " ")
								: React.createElement("span", {style: {color:'#3c763d'}}, React.createElement("i", {className: "glyphicon glyphicon-thumbs-up"}), " "), 
							
							 " "+endpoint[0]
						)
					), 
					React.createElement("div", {style: {marginLeft:40}}, 
					 isScan ?
						React.createElement("div", null, "Max concurrent scan requests:", " ", " ", stat.maxConcurrentRequests, " ") :
						React.createElement("div", null, "Max concurrent search requests:", " ", " ", stat.maxConcurrentRequests, " ")
					
					), 
					React.createElement("div", {style: {marginLeft:40}}, 
						React.createElement("span", null, stat.numberOfRequests), " request(s)," + ' ' +
						"average:", this.renderWaitTimeSecs(stat.avgExecutionTime), "," + ' ' +
						"max: ", this.renderWaitTimeSecs(stat.maxExecutionTime)
					), 
					 isScan ? this.renderCollections(stat.rootCollections) : false, 
						(errors && errors.length) ?
						React.createElement("div", {className: "inline", style: {marginLeft:40}}, 
							 errors.map(this.renderError) 
						) : false, 
					
						(diagnostics && diagnostics.length) ?
						React.createElement("div", {className: "inline", style: {marginLeft:40}}, 
							 diagnostics.map(this.renderDiagnostic) 
						) : false
					
				);
	},

	renderInstitution: function(isScan, inst) {
		return 	React.createElement("div", {style: {marginTop:30}, key: inst[0]}, 
					React.createElement("h4", null, inst[0]), 
					React.createElement("div", {style: {marginLeft:20}}, " ", _.pairs(inst[1]).map(this.renderEndpoint.bind(this, isScan)) )
 				);
	},

	renderStatistics: function(stats) {
		return 	React.createElement("div", {className: "container statistics", style: {marginTop:20}}, 
					React.createElement("div", null, 
						React.createElement("div", null, "Start date: ", new Date(stats.date).toLocaleString()), 
						React.createElement("div", null, "Timeout: ", " ", React.createElement("kbd", null, stats.timeout, " seconds"))
					), 
					React.createElement("div", null, " ",  _.pairs(stats.institutions).map(this.renderInstitution.bind(this, stats.isScan)), " ")
				)
				 ;
	},

	setTab: function(idx) {
		this.setState({activeTab:idx});
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "top-gap"}, 
					React.createElement("h1", null, "Statistics"), 
					React.createElement("p", null), 
					React.createElement("div", {role: "tabpanel"}, 
						React.createElement("ul", {className: "nav nav-tabs", role: "tablist"}, 
							 _.pairs(this.state.stats).map(function(st, idx){
									var classname = idx === this.state.activeTab ? "active":"";
									return 	React.createElement("li", {role: "presentation", className: classname, key: st[0]}, 
												React.createElement("a", {href: "#", role: "tab", onClick: this.setTab.bind(this, idx)}, st[0])
											);
								}.bind(this))
							
						), 

						React.createElement("div", {className: "tab-content"}, 
							 _.pairs(this.state.stats).map(function(st, idx){
									var classname = idx === this.state.activeTab ? "tab-pane active" : "tab-pane";
									return 	React.createElement("div", {role: "tabpanel", className: classname, key: st[0]}, 
												this.renderStatistics(st[1])
											);
								}.bind(this))
							
						)
					)
				)
			)
			);
	},
});

var HelpPage = React.createClass({displayName: 'HelpPage',
	openHelpDesk: function() {
		window.open('http://support.clarin-d.de/mail/form.php?queue=Aggregator&lang=en',
			'_blank', 'height=560,width=370');
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "top-gap"}, 
					React.createElement("h1", null, "Help"), 
					React.createElement("h3", null, "Performing search in FCS corpora"), 
					React.createElement("p", null, "To perform simple keyword search in all CLARIN-D Federated Content Search centres" + ' ' +
					"and their corpora, go to the search field at the top of the page," + ' ' +
					"enter your query, and click 'search' button or press the 'Enter' key."), 

					React.createElement("p", null, "When the search starts, the page will start filling in with the corpora responses." + ' ' +
					"After the entire search process has ended you have the option to download the results" + ' ' +
					"in various formats."
					), 

					React.createElement("p", null, "If you are particularly interested in the results returned by a corpus, you have" + ' ' +
					"the option to focus only on the results of that corpus, by clicking on the 'Watch' button." + ' ' +
					"In this view mode you can also download the results of use the WebLicht processing services" + ' ' +
					"to further analyse the results."), 


					React.createElement("h3", null, "Adjusting search criteria"), 
					React.createElement("p", null, "The FCS Aggregator makes possible to select specific corpora based on their name" + ' ' +
					"or language and to specify the number of search results (hits) per corpus per page." + ' ' +
					"The user interface controls that allows to change these options are located" + ' ' +
					"right below the search fiels on the main page. The current options are" + ' ' +
					"to filter resources based on their language, to select specific resources, and" + ' ' +
					"to set the maximum number of hits."), 


					React.createElement("h3", null, "More help"), 
					React.createElement("p", null, "More detailed information on using FCS Aggregator is available at the  ", 
					React.createElement("a", {href: "http://weblicht.sfs.uni-tuebingen.de/weblichtwiki/index.php/FCS_Aggregator"}, 
						"Aggregator wiki page"
					), "." + ' ' +
					"If you still cannot find an answer to your question," + ' ' +
					"or if want to send a feedback, you can write to Clarin-D helpdesk: "), 
					React.createElement("button", {type: "button", className: "btn btn-default btn-lg", onClick: this.openHelpDesk}, 
						React.createElement("span", {className: "glyphicon glyphicon-question-sign", 'aria-hidden': "true"}), 
						" HelpDesk"
					)
				)
			)
		);
	}
});

var AboutPage = React.createClass({displayName: 'AboutPage',
	render: function() {
		return	React.createElement("div", null, 
					React.createElement("div", {className: "top-gap"}, 
						React.createElement("h1", null, "About"), 
						React.createElement("h3", null, "Technology"), 

						React.createElement("p", null, "The Aggregator uses the following software components:"), 

						React.createElement("ul", null, 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://dropwizard.io/"}, "Dropwizard"), " ", 
								"(", React.createElement("a", {href: "http://www.apache.org/licenses/LICENSE-2.0"}, "Apache License 2.0"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://eclipse.org/jetty/"}, "Jetty"), " ", 
								"(", React.createElement("a", {href: "http://www.apache.org/licenses/LICENSE-2.0"}, "Apache License 2.0"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://jackson.codehaus.org/"}, "Jackson"), " ", 
								"(", React.createElement("a", {href: "http://www.apache.org/licenses/LICENSE-2.0"}, "Apache License 2.0"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "https://jersey.java.net/"}, "Jersey"), " ", 
								"(", React.createElement("a", {href: "https://jersey.java.net/license.html#/cddl"}, "CCDL 1.1"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "https://github.com/optimaize/language-detector"}, "Optimaize Language Detector"), " ", 
								"(", React.createElement("a", {href: "http://www.apache.org/licenses/LICENSE-2.0"}, "Apache License 2.0"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://poi.apache.org/"}, "Apache POI"), " ", 
								"(", React.createElement("a", {href: "http://www.apache.org/licenses/LICENSE-2.0"}, "Apache License 2.0"), ")"
							)
						), 

						React.createElement("ul", null, 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://facebook.github.io/react/"}, "React"), " ", 
								"(", React.createElement("a", {href: "https://github.com/facebook/react/blob/master/LICENSE"}, "BSD license"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://getbootstrap.com/"}, "Bootstrap"), " ", 
								"(", React.createElement("a", {href: "http://opensource.org/licenses/mit-license.html"}, "MIT license"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://jquery.com/"}, "jQuery"), " ", 
								"(", React.createElement("a", {href: "http://opensource.org/licenses/mit-license.html"}, "MIT license"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://glyphicons.com/"}, "GLYPHICONS free"), " ", 
								"(", React.createElement("a", {href: "https://creativecommons.org/licenses/by/3.0/"}, "CC-BY 3.0"), ")"
							), 
							React.createElement("li", null, 
								React.createElement("a", {href: "http://fortawesome.github.io/Font-Awesome/"}, "FontAwesome"), " ", 
								"(", React.createElement("a", {href: "http://opensource.org/licenses/mit-license.html"}, "MIT"), ", ", React.createElement("a", {href: "http://scripts.sil.org/OFL"}, "SIL Open Font License"), ")"
							)
						), 

						React.createElement("h3", null, "Statistics"), 
						React.createElement("button", {type: "button", className: "btn btn-default btn-lg", onClick: function() {main.toStatistics(true);}}, 
							React.createElement("span", {className: "glyphicon glyphicon-cog", 'aria-hidden': "true"}, " "), 
							"View server log"
						)
					)
				);
	}
});

var Footer = React.createClass({displayName: 'Footer',
	toAbout: function(e) {
		main.toAbout(true);
		e.preventDefault();
		e.stopPropagation();
	},

	render: function() {
		var path = window.location.pathname.split('/');
		if (path.length === 3 && path[2] === 'embed') {
			return false;
		}
		return	(
			React.createElement("div", {className: "container"}, 
				React.createElement("div", {id: "CLARIN_footer_left"}, 
						React.createElement("a", {title: "about", href: "about", onClick: this.toAbout}, 
						React.createElement("span", {className: "glyphicon glyphicon-info-sign"}), 
						React.createElement("span", null, VERSION)
					)
				), 
				React.createElement("div", {id: "CLARIN_footer_middle"}, 
					React.createElement("a", {title: "CLARIN ERIC", href: "https://www.clarin.eu/"}, 
					React.createElement("img", {src: "img/clarindLogo.png", alt: "CLARIN ERIC logo", style: {height:80}})
					)
				), 
				React.createElement("div", {id: "CLARIN_footer_right"}, 
					React.createElement("a", {title: "contact", href: "mailto:fcs@clarin.eu"}, 
						React.createElement("span", {className: "glyphicon glyphicon-envelope"}), 
						React.createElement("span", null, " CONTACT")
					)
				)
			)
		);
	}
});

function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

var routeFromLocation = function() {
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

var main = React.render(React.createElement(Main, null),  document.getElementById('body'));
React.render(React.createElement(Footer, null), document.getElementById('footer') );

window.onpopstate = routeFromLocation.bind(main);
window.MyAggregator.main = main;

})();
