/** @jsx React.DOM */
(function() {
"use strict";

var PT = React.PropTypes;

var ErrorPane = window.MyReact.ErrorPane;
var AggregatorPage = window.MyAggregator.AggregatorPage;

var Main = React.createClass({
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
		jQuery.ajax(ajaxObject);
	},

	renderAggregator: function() {
		return <AggregatorPage ajax={this.ajax} corpora={this.state.corpora} languageMap={this.state.languageMap} />;
	},

	renderStatistics: function() {
		return <StatisticsPage ajax={this.ajax} />;
	},

	renderHelp: function() {
		return <HelpPage />;
	},

	toggleCollapse: function() {
		this.setState({navbarCollapse: !this.state.navbarCollapse});
	},

	setNavbarPageFn: function(pageFn) {
		this.setState({navbarPageFn:pageFn});
	},

	renderCollapsible: function() {
		var classname = "navbar-collapse collapse " + (this.state.navbarCollapse?"in":"");
		return (
			<div className={classname}>
				<ul className="nav navbar-nav">
					<li className={this.state.navbarPageFn === this.renderAggregator ? "active":""}>
						<a className="link" tabIndex="-1" 
							onClick={this.setNavbarPageFn.bind(this, this.renderAggregator)}>Aggregator</a>
					</li>
					<li className={this.state.navbarPageFn === this.renderHelp ? "active":""}>
						<a className="link" tabIndex="-1" 
							onClick={this.setNavbarPageFn.bind(this, this.renderHelp)}>Help</a>
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
						<span>BETA</span>
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

	listItem: function(it) {
		return <li>	{it[0]}:
					{ typeof(it[1]) === "object" ? 
						<ul>{_.pairs(it[1]).map(this.listItem)}</ul> : 
						it[1]
					}
				</li>;
	},

	// renderEndpoint: function(endp) {
	// 	return <li>
	// 				<ul>
	// 					<li>endpoint: {endp[0]}</li>
	//           			<li>numberOfRequests: {endp[1].numberOfRequests}</li>
	// 			        <li>avgQueueTime: {endp[1].avgQueueTime}</li>
	// 			        <li>maxQueueTime: {endp[1].maxQueueTime}</li>
	// 			        <li>avgExecutionTime: {endp[1].avgExecutionTime}</li>
	// 			        <li>maxExecutionTime: {endp[1].maxExecutionTime}</li>
	// 					<li>errors 
	// 						<ul>
	// 							{ _.pairs(object).map(endp[1].errors, function(e) { return <li>{e[0]}:{e[1]}</li>; }) }
	// 						</ul>
	// 					</li>
	// 				</ul>
	// 			</li>;
	// },
	// renderInstitution: function(instname, instendps) {
	// 	return 	<li>
	// 				<ul>
	// 					<li>{instname}</li>
	// 					<li>
	// 						<ul>{_.pairs(object).map(instendps, this.renderEndpoint)}</ul>
	// 					</li>
 // 					</ul>
 // 				</li>;
	// },

	renderStatistics: function(stats) {
		return <ul>{_.pairs(stats).map(this.listItem)}</ul>;
	},

	render: function() {
		return	(
			<div>
				<div className="top-gap">
					<h1>Statistics</h1>
					<h2>Last Scan</h2>
					{this.renderStatistics(this.state.lastScanStats)}
					<h2>Search</h2>
					{this.renderStatistics(this.state.searchStats)}
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

React.render(<Main />, document.getElementById('reactMain') );
})();
