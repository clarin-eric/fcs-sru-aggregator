/** @jsx React.DOM */
(function() {
"use strict";

var VERSION = window.MyAggregator.VERSION = "v.2.0.0-beta-50";

var URLROOT = window.MyAggregator.URLROOT =
	window.location.pathname.substring(0, window.location.pathname.indexOf("/",2)) ||
	"/Aggregator";

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
	componentWillMount: function() {
		routeFromLocation.bind(this)();
	},

	getInitialState: function () {
		return {
			navbarCollapse: false,
			navbarPageFn: this.renderAggregator,
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
		return <AggregatorPage ajax={this.ajax} error={this.error} />;
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

	renderEmbedded: function() {
		return <AggregatorPage ajax={this.ajax} error={this.error} embedded={true}/>;
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
			<div className={classname}>
				<ul className="nav navbar-nav">
					<li className={this.state.navbarPageFn === this.renderAggregator ? "active":""}>
						<a className="link" tabIndex="-1" onClick={this.toAggregator.bind(this, true)}>Aggregator</a>
					</li>
					<li className={this.state.navbarPageFn === this.renderHelp ? "active":""}>
						<a className="link" tabIndex="-1" onClick={this.toHelp.bind(this, true)}>Help</a>
					</li>
				</ul>
				<ul className="nav navbar-nav navbar-right">
					<li> <div id="clarinservices" style={{padding:4}}/> </li>
					{this.renderLogin()}
				</ul>
			</div>
		);
	},

	renderTop: function() {
		if (this.state.navbarPageFn === this.renderEmbedded) {
			return false;
		}
		return	(
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
							<a className="navbar-brand" href={URLROOT} tabIndex="-1">
								<img width="28px" height="28px" src="img/magglass1.png"/>
								<header className="inline"> Content Search </header>
							</a>
						</div>
						{this.renderCollapsible()}
					</div>
				</div>

				<ErrorPane errorMessages={this.state.errorMessages} />

			</div>
		);
	},

	render: function() {
		return	(
			<div>
				<div> { this.renderTop() } </div>

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
		var classes = "inline alert alert-warning " + (d.diagnostic.uri === 'LEGACY' ? "legacy" : "");
		return 	<div key={d.diagnostic.uri}>
					<div className={classes} >
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
							{ " "+endpoint[0] }
						</li>
					</ul>
					<div style={{marginLeft:40}}>
					{ isScan ?
						<div>Max concurrent scan requests:{" "} {stat.maxConcurrentRequests} </div> :
						<div>Max concurrent search requests:{" "} {stat.maxConcurrentRequests} </div>
					}
					</div>
					<div style={{marginLeft:40}}>
						<span>{stat.numberOfRequests}</span> request(s),
						average:{this.renderWaitTimeSecs(stat.avgExecutionTime)},
						max: {this.renderWaitTimeSecs(stat.maxExecutionTime)}
					</div>
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
					<p>To perform simple keyword search in all CLARIN-D Federated Content Search centres
					and their corpora, go to the search field at the top of the page,
					enter your query, and click 'search' button or press the 'Enter' key.</p>

					<p>When the search starts, the page will start filling in with the corpora responses.
					After the entire search process has ended you have the option to download the results
					in various formats.
					</p>

					<p>If you are particularly interested in the results returned by a corpus, you have
					the option to focus only on the results of that corpus, by clicking on the 'Watch' button.
					In this view mode you can also download the results of use the WebLicht processing services
					to further analyse the results.</p>


					<h3>Adjusting search criteria</h3>
					<p>The FCS Aggregator makes possible to select specific corpora based on their name
					or language and to specify the number of search results (hits) per corpus per page.
					The user interface controls that allows to change these options are located
					right below the search fiels on the main page. The current options are
					to filter resources based on their language, to select specific resources, and
					to set the maximum number of hits.</p>


					<h3>More help</h3>
					<p>More detailed information on using FCS Aggregator is available at the &nbsp;
					<a href="http://weblicht.sfs.uni-tuebingen.de/weblichtwiki/index.php/FCS_Aggregator">
						Aggregator wiki page
					</a>.
					If you still cannot find an answer to your question,
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
						<h1 style={{padding:15}}>About</h1>

						<div className="col-md-6">
						<h3>People</h3>

						<ul>
							<li>Emanuel Dima</li>
							<li>Yana Panchenko</li>
							<li>Oliver Schonefeld</li>
							<li>Dieter Van Uytvanck</li>
						</ul>

						<h3>Statistics</h3>
						<button type="button" className="btn btn-default btn-lg" onClick={function() {main.toStatistics(true);}} >
							<span className="glyphicon glyphicon-cog" aria-hidden="true"> </span>
							View server log
						</button>
						</div>

						<div className="col-md-6">
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

						<p>The content search icon is made by
							<a href="http://www.freepik.com" title="Freepik"> Freepik </a>
							from
							<a href="http://www.flaticon.com" title="Flaticon"> www.flaticon.com </a>
							and licensed under
							<a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0"> CC BY 3.0 </a>
						</p>
						</div>

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
		return (
			<div className="container" style={{textAlign:'center'}}>
				<div className="row">
					<div style={{position:'relative', float:'left'}}>
						<div className="leftist" style={{position:'absolute'}}>
							<div>
								<a title="about" href="about" onClick={this.toAbout}>About</a>
							</div>
							<div style={{color:'#777'}}>{VERSION}</div>
						</div>
					</div>
					<a title="CLARIN ERIC" href="https://www.clarin.eu/">
						<img src="img/clarindLogo.png" alt="CLARIN ERIC logo" style={{height:60}}/>
					</a>
					<div style={{position:'relative', float:'right'}}>
						<div className="rightist" style={{position:'absolute', right:'0'}}>
							<a title="contact" href="mailto:fcs@clarin.eu">Contact</a>
						</div>
					</div>
				</div>
			</div>
		);
	}
});

function isEmbeddedView() {
	var path = window.location.pathname.split('/');
	return (path.length >= 3 && path[2] === 'embed');
}

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

var main = React.render(<Main />,  document.getElementById('body'));
if (!isEmbeddedView()) {
	React.render(<Footer />, document.getElementById('footer') );
} else if (jQuery) {
	jQuery("#footer").remove();
}

window.onpopstate = routeFromLocation.bind(main);
window.MyAggregator.main = main;

})();
