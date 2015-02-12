/** @jsx React.DOM */
(function() {
"use strict";

var VERSION = "VERSION 2.0.0.α18";

var PT = React.PropTypes;

var ErrorPane = window.MyReact.ErrorPane;
var AggregatorPage = window.MyAggregator.AggregatorPage;

var Main = React.createClass({displayName: 'Main',
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

	renderAggregator: function() {
		return React.createElement(AggregatorPage, {ajax: this.ajax, corpora: this.state.corpora, languageMap: this.state.languageMap});
	},

	renderHelp: function() {
		return React.createElement(HelpPage, null);
	},

	renderAbout: function() {
		return React.createElement(AboutPage, {statistics: this.statistics});
	},

	renderStatistics: function() {
		return React.createElement(StatisticsPage, {ajax: this.ajax});
	},

	toggleCollapse: function() {
		this.setState({navbarCollapse: !this.state.navbarCollapse});
	},

	about: function(e) {
		this.setState({navbarPageFn: this.renderAbout});
	},
	statistics: function(e) {
		this.setState({navbarPageFn: this.renderStatistics});
	},


	setNavbarPageFn: function(pageFn) {
		this.setState({navbarPageFn:pageFn});
	},

	renderCollapsible: function() {
		var classname = "navbar-collapse collapse " + (this.state.navbarCollapse?"in":"");
		return (
			React.createElement("div", {className: classname}, 
				React.createElement("ul", {className: "nav navbar-nav"}, 
					React.createElement("li", {className: this.state.navbarPageFn === this.renderAggregator ? "active":""}, 
						React.createElement("a", {className: "link", tabIndex: "-1", 
							onClick: this.setNavbarPageFn.bind(this, this.renderAggregator)}, "Aggregator")
					), 
					React.createElement("li", {className: this.state.navbarPageFn === this.renderHelp ? "active":""}, 
						React.createElement("a", {className: "link", tabIndex: "-1", 
							onClick: this.setNavbarPageFn.bind(this, this.renderHelp)}, "Help")
					)
				), 
				React.createElement("ul", {id: "CLARIN_header_right", className: "nav navbar-nav navbar-right"}, 
					React.createElement("li", {className: "unauthenticated"}, 
						React.createElement("a", {href: "login", tabIndex: "-1"}, React.createElement("span", {className: "glyphicon glyphicon-log-in"}), " LOGIN")
					)
				)
			)
		);
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "container"}, 
					React.createElement("div", {className: "beta-tag"}, 
						React.createElement("span", null, "ALPHA")
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
							React.createElement("a", {className: "navbar-brand", href: "#", tabIndex: "-1"}, React.createElement("header", null, "Federated Content Search"))
						), 
						this.renderCollapsible()
					)
				), 

				React.createElement(ErrorPane, {errorMessages: this.state.errorMessages}), 

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
			searchStats: {}, 
			lastScanStats: {}, 
		};
	},

	componentDidMount: function() {
		this.refreshStats();
	},

	refreshStats: function() {
		this.props.ajax({
			url: 'rest/statistics',
			success: function(json, textStatus, jqXHR) {
				this.setState({
					searchStats: json.searchStats, 
					lastScanStats: json.lastScanStats, 
				});
				console.log("stats:", json);
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
								 colls.map(function(name) { return React.createElement("div", null, name); }) 
							)
						)
					
				);
	},

	renderDiagnostic: function(d) {
		return 	React.createElement("div", {key: d.diagnostic.uri}, 
					React.createElement("div", {className: "inline alert alert-warning"}, 
						React.createElement("div", null, "Diagnostic: ", d.diagnostic.message, ": ", d.diagnostic.diagnostic), 
						React.createElement("div", null, "Context: ", React.createElement("a", {href: d.context}, d.context))
					), 
					" ", 
					React.createElement("div", {className: "inline"}, React.createElement("span", {className: "badge"}, "x ", d.counter))
				); 
	},

	renderError: function(e) {
		var xc = e.exception;
		return 	React.createElement("div", {key: xc.message}, 
					React.createElement("div", {className: "inline alert alert-danger", role: "alert"}, 
						React.createElement("div", null, "Exception: ", xc.message), 
						React.createElement("div", null, "Context: ", React.createElement("a", {href: e.context}, e.context)), 
						 xc.cause ? React.createElement("div", null, "Caused by: ", xc.cause) : false
					), 
					" ", 
					React.createElement("div", {className: "inline"}, React.createElement("span", {className: "badge"}, "x ", e.counter))
				); 
	},

	renderEndpoint: function(isScan, endpoint) {
		var stat = endpoint[1];
		var errors = _.values(stat.errors);
		var diagnostics = _.values(stat.diagnostics);
		return React.createElement("div", {style: {marginTop:10}}, 
					React.createElement("ul", {className: "list-inline list-unstyled", style: {marginBottom:0}}, 
						React.createElement("li", null, 
							 stat.version == "LEGACY" ? 
								React.createElement("span", {style: {color:'#a94442'}}, "legacy ", React.createElement("i", {className: "glyphicon glyphicon-thumbs-down"}), " ") 
								: React.createElement("span", {style: {color:'#3c763d'}}, React.createElement("i", {className: "glyphicon glyphicon-thumbs-up"}), " "), 
							
							 " "+endpoint[0], ":" 
						), 
						React.createElement("li", null, 
							React.createElement("span", null, stat.numberOfRequests), " request(s)," + ' ' +
							"average:", this.renderWaitTimeSecs(stat.avgExecutionTime), "," + ' ' + 
							"max: ", this.renderWaitTimeSecs(stat.maxExecutionTime)
						)
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
		return 	React.createElement("div", {style: {marginBottom:30}}, 
					React.createElement("h4", null, inst[0]), 
					React.createElement("div", {style: {marginLeft:20}}, " ", _.pairs(inst[1]).map(this.renderEndpoint.bind(this, isScan)) )
 				);
	},

	renderStatistics: function(isScan, stats) {
		return 	React.createElement("div", {className: "container"}, 
					React.createElement("ul", {className: "list-inline list-unstyled"}, 
						 stats.maxConcurrentScanRequestsPerEndpoint ? 
							React.createElement("li", null, "max concurrent scan requests per endpoint:", " ", 
								React.createElement("kbd", null, stats.maxConcurrentScanRequestsPerEndpoint), ","
							) : false, 
						
						 stats.maxConcurrentSearchRequestsPerEndpoint ? 
							React.createElement("li", null, "max concurrent search requests per endpoint:", " ", 
								React.createElement("kbd", null, stats.maxConcurrentSearchRequestsPerEndpoint), ","
							) : false, 
						
						React.createElement("li", null, "timeout:", " ", React.createElement("kbd", null, stats.timeout, " seconds"))
					), 
					React.createElement("div", null, " ",  _.pairs(stats.institutions).map(this.renderInstitution.bind(this, isScan)), " ")
				)
				 ;
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "top-gap statistics"}, 
					React.createElement("h1", null, "Statistics"), 
					React.createElement("h2", null, "Last scan"), 
					this.renderStatistics(true, this.state.lastScanStats), 
					React.createElement("h2", null, "Searches since last scan"), 
					this.renderStatistics(false, this.state.searchStats)
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
					React.createElement("p", null, "To perform simple keyword search in all CLARIN-D Federated Content Search centers" + ' ' + 
					"and their corpora, go to the search field at the top of the page," + ' ' + 
					"enter your query, and click 'search' button or press the 'Enter' key."), 
					
					React.createElement("h3", null, "Search Options - adjusting search criteria"), 
					React.createElement("p", null, "To select specific corpora based on their name or language and to specify" + ' ' + 
					"number of search results (hits) per corpus per page, click on the 'Search options'" + ' ' +
					"link. Here, you can filter resources based on the language, select specific resources," + ' ' + 
					"set the maximum number of hits."), 

					React.createElement("h3", null, "Search Results - inspecting search results"), 
					React.createElement("p", null, "When the search starts, the 'Search results' page is displayed" + ' ' + 
					"and its content starts to get filled with the corpora responses." + ' ' + 
					"To save or process the displayed search result, in the 'Search results' page," + ' ' + 
					"go to the menu and select either 'Export to Personal Workspace'," + ' ' + 
					"'Download' or 'Use WebLicht' menu item. This menu appears only after" + ' ' + 
					"all the results on the page have been loaded. To get the next hits from each corpus," + ' ' + 
					"click the 'next' arrow at the bottom of 'Search results' page."), 


					React.createElement("h3", null, "More help"), 
					React.createElement("p", null, "More detailed information on using FCS Aggregator is available" + ' ' + 
					"at the Aggegator wiki page. If you still cannot find an answer to your question," + ' ' + 
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
	propTypes: {
		statistics: PT.func.isRequired,
	},

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
						React.createElement("button", {type: "button", className: "btn btn-default btn-lg", onClick: this.props.statistics}, 
							React.createElement("span", {className: "glyphicon glyphicon-cog", 'aria-hidden': "true"}, " "), 
							"View server log"
						)
					)
				);
	}
});

var Footer = React.createClass({displayName: 'Footer',
	about: function(e) {
		main.about();
		e.preventDefault();
		e.stopPropagation();
	},

	render: function() {
		return	(
			React.createElement("div", {className: "container"}, 
				React.createElement("div", {id: "CLARIN_footer_left"}, 
						React.createElement("a", {title: "about", href: "#", onClick: this.about}, 
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

var main = React.render(React.createElement(Main, null),  document.getElementById('body'));
React.render(React.createElement(Footer, null), document.getElementById('footer') );

})();
