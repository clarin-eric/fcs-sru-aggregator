/** @jsx React.DOM */

var PT = React.PropTypes;

var SearchBox = window.MyAggregator.SearchBox;
var CorpusSelection = window.MyAggregator.CorpusSelection;
var LanguageSelection = window.MyAggregator.LanguageSelection;
var HitNumber = window.MyAggregator.HitNumber;
var Results = window.MyAggregator.Results;
var CorpusView = window.MyAggregator.CorpusView;
var Modal = window.MyReact.Modal;
var ErrorPane = window.MyReact.ErrorPane;

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

Corpora.prototype.getLanguages = function() {
	var languages = {};
	this.recurse(function(corpus) {
		corpus.languages.forEach(function(lang) {
			languages[lang] = true;
		});
	});
	var ret = [];
	for (var l in languages) {
		if (languages.hasOwnProperty(l)) {
			ret.push({
				name:l,
				code:l,
			});
		}
	}
	return ret;
};


var Main = React.createClass({displayName: 'Main',
	getInitialState: function () {
		return {
			navbarCollapse: false,
			navbarPageFn: this.renderAggregator,
			errorMessages: [],

			corpora: new Corpora([], this.updateCorpora),
		};
	},

	componentDidMount: function() {
		this.refreshCorpora();
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
		}, 2000);
	},
	
	ajax: function(ajaxObject) {
		var that = this;
		if (!ajaxObject.error) {
			ajaxObject.error = function(jqXHR, textStatus, error) {
				if (jqXHR.readyState === 0) {
					that.error("Network error, please check your internet connection");
				} else {
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

	updateCorpora: function(corpora) {
		this.setState({corpora:corpora});
	},

	renderAggregator: function() {
		return React.createElement(AggregatorPage, {ajax: this.ajax, corpora: this.state.corpora});
	},

	renderStatistics: function() {
		return React.createElement(StatisticsPage, {ajax: this.ajax});
	},

	renderHelp: function() {
		return React.createElement(HelpPage, null);
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
			React.createElement("div", {className: classname}, 
				React.createElement("ul", {className: "nav navbar-nav"}, 
					React.createElement("li", {className: this.state.navbarPageFn === this.renderAggregator ? "active":""}, 
						React.createElement("a", {className: "link", tabIndex: "-1", 
							onClick: this.setNavbarPageFn.bind(this, this.renderAggregator)}, "Aggregator")
					), 
					React.createElement("li", {className: this.state.navbarPageFn === this.renderStatistics ? "active":""}, 
						React.createElement("a", {className: "link", tabIndex: "-1", 
							onClick: this.setNavbarPageFn.bind(this, this.renderStatistics)}, "Statistics")
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

var AggregatorPage = React.createClass({displayName: 'AggregatorPage',
	propTypes: {
		ajax: PT.func.isRequired,
		corpora: PT.object.isRequired,
	},

	getInitialState: function () {
		return {
			searchLayer: "text",
			numberOfResults: 10,

			searchId: null,
			hits: { 
				requests: [],
				results: [],
			},
		};
	},

	timeout: 0,

	search: function(query) {
		this.props.ajax({
			url: 'rest/search',
			type: "POST",
			data: {
				query: query
			},
			success: function(searchId, textStatus, jqXHR) {
				console.log("search ["+query+"] ok: ", searchId, jqXHR);
				this.setState({searchId : searchId});
				this.timeout = 250;
				setTimeout(this.refreshSearchResults, this.timeout);
			}.bind(this),
		});
	},

	setNumberOfResults: function(x) {
		this.setState({numberOfResults:x});
	},

	refreshSearchResults: function() {
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

	toggleCorpusSelection: function(e) {
		$(this.refs.corporaModal.getDOMNode()).modal();
		e.preventDefault();
		e.stopPropagation();
	},

	renderCorpusSelection: function() {
		var style={width:"240px"};
		return	React.createElement("button", {type: "button", className: "btn btn-default", style: style, onClick: this.toggleCorpusSelection}, 
					"All available corpora", React.createElement("span", {className: "caret"})
				);
	},

	renderAggregator: function() {
		var margin = {marginTop:"0", padding:"20px"};
		var inline = {display:"inline-block", margin:"0 5px 0 0"};
		var inlinew = {display:"inline-block", margin:"0 5px 0 0", width:"240px;"};
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "top-gap"}, 
					React.createElement(SearchBox, {search: this.search})
				), 
				React.createElement("div", {className: "aligncenter"}, 
					React.createElement("div", {style: margin}, 
						React.createElement("form", {className: "form-inline", role: "form"}, 
							React.createElement("label", {className: "muted"}, "search in "), 
							React.createElement("div", {id: "corpusSelection", style: inlinew}, 
								this.renderCorpusSelection()
							), 
							React.createElement("label", {className: "muted"}, " for results in "), 
							React.createElement("div", {id: "languageSelection", style: inlinew}, 
								React.createElement(LanguageSelection, {corpora: this.props.corpora})
							), 
							React.createElement("label", {className: "muted"}, " and show maximum "), 
							React.createElement("div", {style: inline}, 
								React.createElement(HitNumber, {onChange: this.setNumberOfResults, numberOfResults: this.state.numberOfResults})
							), 
							React.createElement("label", {className: "muted"}, " hits")
						)
					)
				), 

	            React.createElement(Modal, {ref: "corporaModal", title: "Collections"}, 
					React.createElement(CorpusView, {ref: "corpusView", corpora: this.props.corpora})
	            ), 

				React.createElement("div", {className: "top-gap"}, 
					React.createElement(Results, {requests: this.state.hits.requests, results: this.state.hits.results})
				)
			)
			);
	},
	render: function() {
		return this.renderAggregator();
	}
});

var StatsPage = React.createClass({displayName: 'StatsPage',
	propTypes: {
		ajax: PT.func.isRequired,
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "top-gap"}, 
					React.createElement("h1", null, "Statistics")
				)
			)
			);
	},
});

var HelpPage = React.createClass({displayName: 'HelpPage',
	openHelpDesk: function() {
		window.open('http://support.clarin-d.de/mail/form.php?queue=Aggregator', 
			'_blank', 'height=560,width=370');
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "top-gap"}, 
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


React.render(React.createElement(Main, null), document.getElementById('reactMain') );
