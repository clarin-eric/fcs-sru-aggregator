/** @jsx React.DOM */
(function() {
"use strict";

var VERSION = "VERSION 2.0.0.α21";
var URLROOT = "/Aggregator-testing";

var PT = React.PropTypes;

var ErrorPane = window.MyReact.ErrorPane;
var AggregatorPage = window.MyAggregator.AggregatorPage;

var Main = React.createClass({
	componentWillMount: function() {
		routeFromLocation.bind(this);
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
		return <AggregatorPage ajax={this.ajax} corpora={this.state.corpora} languageMap={this.state.languageMap} />;
	},

	renderHelp: function() {
		return <HelpPage />;
	},

	renderAbout: function() {
		return <AboutPage/>;
	},

	renderStatistics: function() {
		return <StatisticsPage ajax={this.ajax} />;
	},

	getPageFns: function() { 
		return {
			'': this.renderAggregator,
			'help': this.renderHelp,
			'about': this.renderAbout,
			'stats': this.renderStatistics,
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

	renderCollapsible: function() {
		var classname = "navbar-collapse collapse " + (this.state.navbarCollapse?"in":"");
		return (
			<div className={classname}>
				<ul className="nav navbar-nav">
					<li className={this.state.navbarPageFn === this.renderAggregator ? "active":""}>
						<a className="link" tabIndex="-1" onClick={this.toAggregator.bind(this, true)}>Aggregator</a>
					</li>
					<li className={this.state.navbarPageFn === this.renderHelp ? "active":""}>
						<a className="link" tabIndex="-1" onClick={this.toHelp.bind(this, true)}>Help</a>
					</li>
				</ul>
				<ul id="CLARIN_header_right" className="nav navbar-nav navbar-right">
					<li className="unauthenticated">
						<a href="login" tabIndex="-1"><span className="glyphicon glyphicon-log-in"></span> LOGIN</a>
					</li>
				</ul>
			</div>
		);
	},

	render: function() {
		return	(
			<div>
				<div className="container">
					<div className="beta-tag">
						<span>ALPHA</span>
					</div>
				</div>
			
				<div className="navbar navbar-default navbar-static-top" role="navigation">
					<div className="container">
						<div className="navbar-header">
							<button type="button" className="navbar-toggle" onClick={this.toggleCollapse}>
								<span className="sr-only">Toggle navigation</span>
								<span className="icon-bar"></span>
								<span className="icon-bar"></span>
								<span className="icon-bar"></span>
							</button>
							<a className="navbar-brand" href="#" tabIndex="-1"><header>Federated Content Search</header></a>
						</div>
						{this.renderCollapsible()}
					</div>
				</div>

				<ErrorPane errorMessages={this.state.errorMessages} />

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


var StatisticsPage = React.createClass({
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
		return	<span className="badge" style={{backgroundColor:shue, color:"black"}}>
					{t.toFixed(3)}s
				</span>;
	},

	renderCollections: function(colls) {
		return	<div style={{marginLeft:40}}>
					{ colls.length === 0 ? 
						<div style={{color:"#a94442"}}>NO collections found</div>
						: 
						<div>
							{colls.length} root collection(s):
							<ul className='list-unstyled' style={{marginLeft:40}}>
								{ colls.map(function(name, i) { return <div key={i}>{name}</div>; }) }
							</ul>
						</div>
					}
				</div>;
	},

	renderDiagnostic: function(d) {
		return 	<div key={d.diagnostic.uri}>
					<div className="inline alert alert-warning"> 
						<div>
							{ d.counter <= 1 ? false : 
								<div className="inline" style={{margin:"5px 5px 5px 5px"}}>
									<span className="badge" style={{backgroundColor:'#ae7241'}}>x {d.counter}</span>
								</div>
							}
							Diagnostic: {d.diagnostic.message}: {d.diagnostic.diagnostic}
						</div>
						<div>Context: <a href={d.context}>{d.context}</a></div>
					</div>
				</div>; 
	},

	renderError: function(e) {
		var xc = e.exception;
		return 	<div key={xc.message}>
					<div className="inline alert alert-danger" role="alert">
						<div>
							{ e.counter <= 1 ? false : 
								<div className="inline" style={{margin:"5px 5px 5px 5px"}}>
									<span className="badge" style={{backgroundColor:'#c94442'}}>x {e.counter} </span>
								</div>
							}
							Exception: {xc.message}
						</div>
						<div>Context: <a href={e.context}>{e.context}</a></div>
						{ xc.cause ? <div>Caused by: {xc.cause}</div> : false}
					</div>
				</div>; 
	},

	renderEndpoint: function(isScan, endpoint) {
		var stat = endpoint[1];
		var errors = _.values(stat.errors);
		var diagnostics = _.values(stat.diagnostics);
		return <div style={{marginTop:10}} key={endpoint[0]}>
					<ul className='list-inline list-unstyled' style={{marginBottom:0}}>
						<li>
							{ stat.version == "LEGACY" ? 
								<span style={{color:'#a94442'}}>legacy <i className="glyphicon glyphicon-thumbs-down"></i> </span> 
								: <span style={{color:'#3c763d'}}><i className="glyphicon glyphicon-thumbs-up"></i> </span> 
							}
							{ " "+endpoint[0] }: 
						</li>
						<li>
							<span>{stat.numberOfRequests}</span> request(s),
							average:{this.renderWaitTimeSecs(stat.avgExecutionTime)}, 
							max: {this.renderWaitTimeSecs(stat.maxExecutionTime)}
						</li>
					</ul>
					{ isScan ? this.renderCollections(stat.rootCollections) : false }
					{	(errors && errors.length) ? 
						<div className='inline' style={{marginLeft:40}}>
							{ errors.map(this.renderError) }
						</div> : false
					}
					{	(diagnostics && diagnostics.length) ? 
						<div className='inline' style={{marginLeft:40}}>
							{ diagnostics.map(this.renderDiagnostic) }
						</div> : false
					}
				</div>;
	},

	renderInstitution: function(isScan, inst) {
		return 	<div style={{marginTop:30}} key={inst[0]}>
					<h4>{inst[0]}</h4>
					<div style={{marginLeft:20}}> {_.pairs(inst[1]).map(this.renderEndpoint.bind(this, isScan)) }</div>
 				</div>;
	},

	renderStatistics: function(stats) {
		return 	<div className="container statistics" style={{marginTop:20}}>
					<div>
						<div>Start date: {new Date(stats.date).toLocaleString()}</div>
						{ stats.isScan ? 
							<div>Max concurrent scan requests per endpoint:{" "}
								<kbd>{stats.maxConcurrentScanRequestsPerEndpoint}</kbd>
							</div> 
							: 
							<div>Max concurrent search requests per endpoint:{" "}
								<kbd>{stats.maxConcurrentSearchRequestsPerEndpoint}</kbd>
							</div>
						}
						<div>Timeout: {" "}<kbd>{stats.timeout} seconds</kbd></div>
					</div>
					<div> { _.pairs(stats.institutions).map(this.renderInstitution.bind(this, stats.isScan)) } </div>
				</div>
				 ;
	},

	setTab: function(idx) {
		this.setState({activeTab:idx});
	},

	render: function() {
		return	(
			<div>
				<div className="top-gap">
					<h1>Statistics</h1>
					<p/>
					<div role="tabpanel">
						<ul className="nav nav-tabs" role="tablist">
							{ _.pairs(this.state.stats).map(function(st, idx){
									var classname = idx === this.state.activeTab ? "active":"";
									return 	<li role="presentation" className={classname} key={st[0]}>
												<a href="#" role="tab" onClick={this.setTab.bind(this, idx)}>{st[0]}</a>
											</li>;
								}.bind(this))
							}
						</ul>

						<div className="tab-content">
							{ _.pairs(this.state.stats).map(function(st, idx){
									var classname = idx === this.state.activeTab ? "tab-pane active" : "tab-pane";
									return 	<div role="tabpanel" className={classname} key={st[0]}>
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

var HelpPage = React.createClass({
	openHelpDesk: function() {
		window.open('http://support.clarin-d.de/mail/form.php?queue=Aggregator&lang=en', 
			'_blank', 'height=560,width=370');
	},

	render: function() {
		return	(
			<div>
				<div className="top-gap">
					<h1>Help</h1>
					<h3>Performing search in FCS corpora</h3>
					<p>To perform simple keyword search in all CLARIN-D Federated Content Search centers 
					and their corpora, go to the search field at the top of the page, 
					enter your query, and click 'search' button or press the 'Enter' key.</p>
					
					<h3>Search Options - adjusting search criteria</h3>
					<p>To select specific corpora based on their name or language and to specify 
					number of search results (hits) per corpus per page, click on the 'Search options'
					link. Here, you can filter resources based on the language, select specific resources, 
					set the maximum number of hits.</p>

					<h3>Search Results - inspecting search results</h3>
					<p>When the search starts, the 'Search results' page is displayed 
					and its content starts to get filled with the corpora responses. 
					To save or process the displayed search result, in the 'Search results' page, 
					go to the menu and select either 'Export to Personal Workspace', 
					'Download' or 'Use WebLicht' menu item. This menu appears only after 
					all the results on the page have been loaded. To get the next hits from each corpus, 
					click the 'next' arrow at the bottom of 'Search results' page.</p>


					<h3>More help</h3>
					<p>More detailed information on using FCS Aggregator is available 
					at the Aggegator wiki page. If you still cannot find an answer to your question, 
					or if want to send a feedback, you can write to Clarin-D helpdesk: </p>
					<button type="button" className="btn btn-default btn-lg" onClick={this.openHelpDesk} >
						<span className="glyphicon glyphicon-question-sign" aria-hidden="true"></span>
						&nbsp;HelpDesk
					</button>					
				</div>
			</div>
		);
	}
});

var AboutPage = React.createClass({
	render: function() {
		return	<div>
					<div className="top-gap">
						<h1>About</h1>
						<h3>Technology</h3>

						<p>The Aggregator uses the following software components:</p>

						<ul>
							<li>
								<a href="http://dropwizard.io/">Dropwizard</a>{" "}
								(<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
							</li>
							<li>
								<a href="http://eclipse.org/jetty/">Jetty</a>{" "}
								(<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
							</li>
							<li>
								<a href="http://jackson.codehaus.org/">Jackson</a>{" "}
								(<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
							</li>
							<li>
								<a href="https://jersey.java.net/">Jersey</a>{" "}
								(<a href="https://jersey.java.net/license.html#/cddl">CCDL 1.1</a>)
							</li>
							<li>
								<a href="https://github.com/optimaize/language-detector">Optimaize Language Detector</a>{" "}
								(<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
							</li>
							<li>
								<a href="http://poi.apache.org/">Apache POI</a>{" "}
								(<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
							</li>
						</ul>

						<ul>
							<li>
								<a href="http://facebook.github.io/react/">React</a>{" "}
								(<a href="https://github.com/facebook/react/blob/master/LICENSE">BSD license</a>)
							</li>
							<li>
								<a href="http://getbootstrap.com/">Bootstrap</a>{" "}
								(<a href="http://opensource.org/licenses/mit-license.html">MIT license</a>)
							</li>
							<li>
								<a href="http://jquery.com/">jQuery</a>{" "}
								(<a href="http://opensource.org/licenses/mit-license.html">MIT license</a>)
							</li>
							<li>
								<a href="http://glyphicons.com/">GLYPHICONS free</a>{" "}
								(<a href="https://creativecommons.org/licenses/by/3.0/">CC-BY 3.0</a>)
							</li>
							<li>
								<a href="http://fortawesome.github.io/Font-Awesome/">FontAwesome</a>{" "}
								(<a href="http://opensource.org/licenses/mit-license.html">MIT</a>, <a href="http://scripts.sil.org/OFL">SIL Open Font License</a>)
							</li>
						</ul>

						<h3>Statistics</h3>
						<button type="button" className="btn btn-default btn-lg" onClick={function() {main.toStatistics(true);}} >
							<span className="glyphicon glyphicon-cog" aria-hidden="true"> </span> 
							View server log
						</button>
					</div>
				</div>;
	}
});

var Footer = React.createClass({
	toAbout: function(e) {
		main.toAbout(true);
		e.preventDefault();
		e.stopPropagation();
	},

	render: function() {
		return	(
			<div className="container">
				<div id="CLARIN_footer_left">
						<a title="about" href="about" onClick={this.toAbout}> 
						<span className="glyphicon glyphicon-info-sign"></span>
						<span>{VERSION}</span>
					</a>
				</div>
				<div id="CLARIN_footer_middle">
					<a title="CLARIN ERIC" href="https://www.clarin.eu/">
					<img src="img/clarindLogo.png" alt="CLARIN ERIC logo" style={{height:80}}/>
					</a>
				</div>
				<div id="CLARIN_footer_right">
					<a title="contact" href="mailto:fcs@clarin.eu">
						<span className="glyphicon glyphicon-envelope"></span>
						<span> CONTACT</span>
					</a>
				</div>
			</div>
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
		} else {
			this.toAggregator(false);
		}
	} else {
		this.toAggregator(false);
	}
};

var main = React.render(<Main />,  document.getElementById('body'));
React.render(<Footer />, document.getElementById('footer') );

window.onpopstate = routeFromLocation.bind(main);

routeFromLocation.bind(main)();

})();







