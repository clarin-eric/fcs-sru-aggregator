/** @jsx React.DOM */
(function() {
"use strict";

var PT = React.PropTypes;

var SearchBox = window.MyAggregator.SearchBox;
var CorpusSelection = window.MyAggregator.CorpusSelection;
var HitNumber = window.MyAggregator.HitNumber;
var Results = window.MyAggregator.Results;
var CorpusView = window.MyAggregator.CorpusView;
var Modal = window.MyReact.Modal;
var ErrorPane = window.MyReact.ErrorPane;

var layers = [
	{
		id: "sampa",
		name: "Phonetics Resources",
		searchPlaceholder: "stA:z",
		searchLabel: "SAMPA query",
		searchLabelBkColor: "#eef",
		allCollections: "All collections",
	},
	{
		id: "text",
		name: "Text Resources",
		searchPlaceholder: "Elephant",
		searchLabel: "Search text",
		searchLabelBkColor: "#fed",
		allCollections: "All collections",
	},
];
var layerMap = {
	sampa: layers[0], 
	text: layers[1],
};

function Corpora(corpora, updateFn) {
	var that = this;
	this.corpora = corpora;
	this.recurse(function(corpus, index){
		corpus.visible = true; // selected in the corpus view
		corpus.selected = true; // selected in the corpus view
		corpus.expanded = false; // expanded in the corpus view
		corpus.priority = 1; // priority in corpus view
		corpus.index = index;
	});
	this.update = function() { 
		updateFn(that); 
	};
}

Corpora.prototype.recurseCorpus = function(corpus, fn) {
	fn(corpus);
	if (corpus.subCorpora)
		this.recurseCorpora(corpus.subCorpora, fn);
};

Corpora.prototype.recurseCorpora = function(corpora, fn) {
	var recfn = function(corpus, index){
		fn(corpus, index);
		corpus.subCorpora.forEach(recfn);
	};
	corpora.forEach(recfn);
};

Corpora.prototype.recurse = function(fn) {
	this.recurseCorpora(this.corpora, fn);
};

Corpora.prototype.getLanguageCodes = function() {
	var languages = {};
	this.recurse(function(corpus) {
		corpus.languages.forEach(function(lang) {
			languages[lang] = true;
		});
	});
	return languages;
};


var Main = React.createClass({
	getInitialState: function () {
		return {
			navbarCollapse: false,
			navbarPageFn: this.renderAggregator,
			errorMessages: [],

			corpora: new Corpora([], this.updateCorpora),
			languageMap: {},
		};
	},

	componentDidMount: function() {
		this.refreshCorpora();
		this.refreshLanguages();
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

	refreshCorpora: function() {
		this.ajax({
			url: 'rest/corpora',
			success: function(json, textStatus, jqXHR) {
				this.setState({corpora : new Corpora(json, this.updateCorpora)});
			}.bind(this),
		});
	},

	refreshLanguages: function() {
		this.ajax({
			url: 'rest/languages',
			success: function(json, textStatus, jqXHR) {
				this.setState({languageMap : json});
			}.bind(this),
		});
	},

	updateCorpora: function(corpora) {
		this.setState({corpora:corpora});
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
					<li className={this.state.navbarPageFn === this.renderStatistics ? "active":""}>
						<a className="link" tabIndex="-1" 
							onClick={this.setNavbarPageFn.bind(this, this.renderStatistics)}>Statistics</a>
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

var AggregatorPage = React.createClass({
	propTypes: {
		ajax: PT.func.isRequired,
		corpora: PT.object.isRequired,
		languageMap: PT.object.isRequired,
	},

	mixins: [React.addons.LinkedStateMixin],
	timeout: 0,
	nohits: { 
		requests: [],
		results: [],
	},
	anyLanguage: ["ANY", "Any Language"],

	getInitialState: function () {
		return {
			searchLayerId: "text",
			language: this.anyLanguage,
			numberOfResults: 10,

			searchId: null,
			hits: this.nohits,
		};
	},

	search: function(query) {
		console.log(query);
		if (!query) {
			this.setState({ hits: this.nohits, searchId: null });
			return;			
		}
		this.props.ajax({
			url: 'rest/search',
			type: "POST",
			data: {
				layer: this.state.searchLayerId,
				query: query,
				numberOfResults: this.state.numberOfResults,
			},
			success: function(searchId, textStatus, jqXHR) {
				console.log("search ["+query+"] ok: ", searchId, jqXHR);
				this.setState({searchId : searchId});
				this.timeout = 250;
				setTimeout(this.refreshSearchResults, this.timeout);
			}.bind(this),
		});
	},

	refreshSearchResults: function() {
		if (!this.state.searchId) {
			return;
		}
		this.props.ajax({
			url: 'rest/search/'+this.state.searchId,
			success: function(json, textStatus, jqXHR) {
				if (json.requests.length > 0) {
					if (this.timeout < 10000) {
						this.timeout = 1.5 * this.timeout;
					}
					setTimeout(this.refreshSearchResults, this.timeout);
					// console.log("new search in: " + this.timeout+ "ms");
				} else {
					// console.log("search ended");
				}
				this.setState({hits:json});
				console.log("hits:", json);
			}.bind(this),
		});
	},

	setAState: function(id, value) {
		var v = {};
		v[id] = value;
		this.setState(v);
	},

	setNumberOfResults: function(e) {
		var n = e.target.value;
		if (n < 10) n = 10;
		if (n > 250) n = 250;
		this.setState({numberOfResults: n});
		e.preventDefault();
		e.stopPropagation();
	},

	stop: function(e) {
		e.preventDefault();
		e.stopPropagation();
	},

	toggleCorpusSelection: function(e) {
		$(this.refs.corporaModal.getDOMNode()).modal();
		e.preventDefault();
		e.stopPropagation();
	},

	renderAggregator: function() {
		var layer = layerMap[this.state.searchLayerId];
		return	(
			<div className="top-gap">
				<div className="row">
					<div className="aligncenter" style={{marginLeft:16, marginRight:16}}> 
						<div className="input-group">
							<span className="input-group-addon" style={{backgroundColor:layer.searchLabelBkColor}}>
								{layer.searchLabel}
							</span>

							<SearchBox search={this.search} placeholder={layer.searchPlaceholder} />
							<div className="input-group-btn">
								<button className="btn btn-default input-lg" type="button" onClick={this.search}>
									<i className="glyphicon glyphicon-search"></i>
								</button>
							</div>
						</div>
					</div>
				</div>

				<div className="wel" style={{marginTop:20}}>
					<div className="aligncenter" >
						<form className="form-inline" role="form">

							<div className="input-group" style={{marginRight:10}}>
								<span className="input-group-addon nobkg">Search in</span>
									<button type="button" className="btn btn-default" onClick={this.toggleCorpusSelection}>
										{layer.allCollections} <span className="caret"/>
									</button>
							</div>

							<div className="input-group" style={{marginRight:10}}>
								
								<span className="input-group-addon nobkg" >of</span>
								
								<div className="input-group-btn">
									<button className="form-control btn btn-default" 
											aria-expanded="false" data-toggle="dropdown">
										{this.state.language[1]} <span className="caret"/>
									</button>
									<ul ref="languageDropdownMenu" className="dropdown-menu">
										<li key={this.anyLanguage[0]}> <a tabIndex="-1" href="#" 
												onClick={this.setAState.bind(this, "language", this.anyLanguage)}>
											{this.anyLanguage[1]}</a>
										</li>
										{	_.pairs(this.props.languageMap).map(function(l) {
												var desc = l[1] + " [" + l[0] + "]";
												return <li key={l[0]}> <a tabIndex="-1" href="#" 
													onClick={this.setAState.bind(this, "language", l)}>{desc}</a></li>;
											}.bind(this))
										}
									</ul>
								</div>

								<div className="input-group-btn">
									<ul ref="layerDropdownMenu" className="dropdown-menu">
										{ 	layers.map(function(l) { 
												return <li key={l.id}> <a tabIndex="-1" href="#" 
													onClick={this.setAState.bind(this, "searchLayerId", l.id)}> {l.name} </a></li>;
											}.bind(this))
										}
									</ul>								
									<button className="form-control btn btn-default" 
											aria-expanded="false" data-toggle="dropdown" >
										{layer.name} <span className="caret"/>
									</button>
								</div>

							</div>

							<div className="input-group">
								<span className="input-group-addon nobkg">and show up to</span>
								<div className="input-group-btn">
									<input type="number" className="form-control input" min="10" max="250" step="5"
										onChange={this.setNumberOfResults} value={this.state.numberOfResults} 
										onKeyPress={this.stop}/>
								</div>
								<span className="input-group-addon nobkg">hits</span>
							</div>
						</form>
					</div>
				</div>

	            <Modal ref="corporaModal" title="Collections">
					<CorpusView ref="corpusView" corpora={this.props.corpora} />
	            </Modal>

				<div className="top-gap">
					<Results requests={this.state.hits.requests} results={this.state.hits.results} />
				</div>
			</div>
			);
	},
	render: function() {
		return this.renderAggregator();
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
		window.open('http://support.clarin-d.de/mail/form.php?queue=Aggregator', 
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

var _ = _ || {
	keys: function() {
		var ret = [];
		for (var x in o) {
			if (o.hasOwnProperty(x)) {
				ret.push(x);
			}
		}
		return ret;
	},

	pairs: function(o){
		var ret = [];
		for (var x in o) {
			if (o.hasOwnProperty(x)) {
				ret.push([x, o[x]]);
			}
		}
		return ret;
	},
};


React.render(<Main />, document.getElementById('reactMain') );
})();
