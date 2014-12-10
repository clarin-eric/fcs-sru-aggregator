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

var multipleLanguageCode = "mul"; // see ISO-693-3

var layers = [
	{
		id: "sampa",
		name: "Phonetics Resources",
		searchPlaceholder: "stA:z",
		searchLabel: "SAMPA query",
		searchLabelBkColor: "#eef",
	},
	{
		id: "text",
		name: "Text Resources",
		searchPlaceholder: "Elephant",
		searchLabel: "Search text",
		searchLabelBkColor: "#fed",
	},
];
var layerMap = {
	sampa: layers[0], 
	text: layers[1],
};

function Corpora(corpora, updateFn) {
	var that = this;
	this.corpora = corpora;
	this.update = function() { 
		updateFn(that); 
	};
	
	var sortFn = function(x, y) {
		var r = x.institution.name.localeCompare(y.institution.name);
		if (r !== 0) {
			return r;
		}
		var t1 = x.title ? x.title : x.displayName;
		var t2 = y.title ? y.title : y.displayName;
		return t1.toLowerCase().localeCompare(t2.toLowerCase()); 
	};

	this.recurse(function(corpus) { corpus.subCorpora.sort(sortFn); });
	this.corpora.sort(sortFn);

	this.recurse(function(corpus, index) {
		corpus.visible = true; // visible in the corpus view
		corpus.selected = true; // selected in the corpus view
		corpus.expanded = false; // not expanded in the corpus view
		corpus.priority = 1; // priority in corpus view
		corpus.index = index;
	});
}

Corpora.prototype.recurseCorpus = function(corpus, fn) {
	if (false === fn(corpus)) {		
		// no recursion
	} else {
		this.recurseCorpora(corpus.subCorpora, fn);
	}
};

Corpora.prototype.recurseCorpora = function(corpora, fn) {
	var recfn = function(corpus, index){
		if (false === fn(corpus)) {
			// no recursion
		} else {
			corpus.subCorpora.forEach(recfn);
		}
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
		return true;
	});
	return languages;
};

Corpora.prototype.isCorpusVisible = function(corpus, layerId, languageCode) {
	if (layerId !== "text") {
		return false;
	}
	// yes for any language
	if (languageCode === multipleLanguageCode) {
		return true;
	}
	// yes if the corpus is in only that language
	if (corpus.languages && corpus.languages.length === 1 && corpus.languages[0] === languageCode) {
		return true;
	}	

	// ? yes if the corpus also contains that language
	if (corpus.languages && corpus.languages.indexOf(languageCode) >=0) {
		return true;
	}

	// ? yes if the corpus has no language
	// if (!corpus.languages || corpus.languages.length === 0) {
	// 	return true;
	// }
	return false;
};

Corpora.prototype.setVisibility = function(layerId, languageCode) {
	// top level
	this.corpora.forEach(function(corpus) {
		corpus.visible = this.isCorpusVisible(corpus, layerId, languageCode);
		this.recurseCorpora(corpus.subCorpora, function(c) { c.visible = corpus.visible; });
	}.bind(this));
};

Corpora.prototype.getSelectedIds = function() {
	var ids = [];
	this.recurse(function(corpus) {
		if (corpus.visible && corpus.selected) {
			ids.push(corpus.id);
			return false; // top-most collection in tree, don't delve deeper
		}
		return true;
	});

	// console.log("ids: ", ids.length, {ids:ids});
	return ids;
};

Corpora.prototype.getSelectedMessage = function() {
	var selected = this.getSelectedIds().length;
	if (this.corpora.length === selected) {
		return "All available collections";
	} else if (selected === 1) {
		return "1 selected collection";
	}
	return selected+" selected collections";
};


var Main = React.createClass({displayName: 'Main',
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
		return React.createElement(AggregatorPage, {ajax: this.ajax, corpora: this.state.corpora, languageMap: this.state.languageMap});
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
		languageMap: PT.object.isRequired,
	},

	mixins: [React.addons.LinkedStateMixin],
	timeout: 0,
	nohits: { 
		requests: [],
		results: [],
	},
	anyLanguage: [multipleLanguageCode, "Any Language"],

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
		// console.log(query);
		if (!query) {
			this.setState({ hits: this.nohits, searchId: null });
			return;			
		}
		this.props.ajax({
			url: 'rest/search',
			type: "POST",
			data: {
				layer: this.state.searchLayerId,
				language: this.state.language[0],
				query: query,
				numberOfResults: this.state.numberOfResults,
				corporaIds: this.props.corpora.getSelectedIds(),
			},
			success: function(searchId, textStatus, jqXHR) {
				// console.log("search ["+query+"] ok: ", searchId, jqXHR);
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
				// console.log("hits:", json);
			}.bind(this),
		});
	},

	setLanguage: function(languageObj) {
		this.props.corpora.setVisibility(this.state.searchLayerId, languageObj[0]);
		this.setState({language: languageObj});
		this.props.corpora.update();
	},

	setLayer: function(layerId) {
		this.props.corpora.setVisibility(layerId, this.state.language[0]);
		this.props.corpora.update();
		this.setState({searchLayerId: layerId});
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
			React.createElement("div", {className: "top-gap"}, 
				React.createElement("div", {className: "row"}, 
					React.createElement("div", {className: "aligncenter", style: {marginLeft:16, marginRight:16}}, 
						React.createElement("div", {className: "input-group"}, 
							React.createElement("span", {className: "input-group-addon", style: {backgroundColor:layer.searchLabelBkColor}}, 
								layer.searchLabel
							), 

							React.createElement(SearchBox, {search: this.search, placeholder: layer.searchPlaceholder}), 
							React.createElement("div", {className: "input-group-btn"}, 
								React.createElement("button", {className: "btn btn-default input-lg", type: "button", onClick: this.search}, 
									React.createElement("i", {className: "glyphicon glyphicon-search"})
								)
							)
						)
					)
				), 

				React.createElement("div", {className: "wel", style: {marginTop:20}}, 
					React.createElement("div", {className: "aligncenter"}, 
						React.createElement("form", {className: "form-inline", role: "form"}, 

							React.createElement("div", {className: "input-group", style: {marginRight:10}}, 
								React.createElement("span", {className: "input-group-addon nobkg"}, "Search in"), 
									React.createElement("button", {type: "button", className: "btn btn-default", onClick: this.toggleCorpusSelection}, 
										this.props.corpora.getSelectedMessage(), " ", React.createElement("span", {className: "caret"})
									)
							), 

							React.createElement("div", {className: "input-group", style: {marginRight:10}}, 
								
								React.createElement("span", {className: "input-group-addon nobkg"}, "of"), 
								
								React.createElement("div", {className: "input-group-btn"}, 
									React.createElement("button", {className: "form-control btn btn-default", 
											'aria-expanded': "false", 'data-toggle': "dropdown"}, 
										this.state.language[1], " ", React.createElement("span", {className: "caret"})
									), 
									React.createElement("ul", {ref: "languageDropdownMenu", className: "dropdown-menu"}, 
										React.createElement("li", {key: this.anyLanguage[0]}, " ", React.createElement("a", {tabIndex: "-1", href: "#", 
												onClick: this.setLanguage.bind(this, this.anyLanguage)}, 
											this.anyLanguage[1])
										), 
											_.pairs(this.props.languageMap).sort(function(l1, l2){
												return l1[1].localeCompare(l2[1]);
											}).map(function(l) {
												var desc = l[1] + " [" + l[0] + "]";
												return React.createElement("li", {key: l[0]}, " ", React.createElement("a", {tabIndex: "-1", href: "#", 
													onClick: this.setLanguage.bind(this, l)}, desc));
											}.bind(this))
										
									)
								), 

								React.createElement("div", {className: "input-group-btn"}, 
									React.createElement("ul", {ref: "layerDropdownMenu", className: "dropdown-menu"}, 
										 	layers.map(function(l) { 
												return React.createElement("li", {key: l.id}, " ", React.createElement("a", {tabIndex: "-1", href: "#", 
													onClick: this.setLayer.bind(this, l.id)}, " ", l.name, " "));
											}.bind(this))
										
									), 								
									React.createElement("button", {className: "form-control btn btn-default", 
											'aria-expanded': "false", 'data-toggle': "dropdown"}, 
										layer.name, " ", React.createElement("span", {className: "caret"})
									)
								)

							), 

							React.createElement("div", {className: "input-group"}, 
								React.createElement("span", {className: "input-group-addon nobkg"}, "and show up to"), 
								React.createElement("div", {className: "input-group-btn"}, 
									React.createElement("input", {type: "number", className: "form-control input", min: "10", max: "250", step: "5", 
										onChange: this.setNumberOfResults, value: this.state.numberOfResults, 
										onKeyPress: this.stop})
								), 
								React.createElement("span", {className: "input-group-addon nobkg"}, "hits")
							)
						)
					)
				), 

	            React.createElement(Modal, {ref: "corporaModal", title: "Collections"}, 
					React.createElement(CorpusView, {corpora: this.props.corpora, languageMap: this.props.languageMap})
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

	listItem: function(it) {
		return React.createElement("li", null, " ", it[0], ":", 
					 typeof(it[1]) === "object" ? 
						React.createElement("ul", null, _.pairs(it[1]).map(this.listItem)) : 
						it[1]
					
				);
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
		return React.createElement("ul", null, _.pairs(stats).map(this.listItem));
	},

	render: function() {
		return	(
			React.createElement("div", null, 
				React.createElement("div", {className: "top-gap"}, 
					React.createElement("h1", null, "Statistics"), 
					React.createElement("h2", null, "Last Scan"), 
					this.renderStatistics(this.state.lastScanStats), 
					React.createElement("h2", null, "Search"), 
					this.renderStatistics(this.state.searchStats)
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


React.render(React.createElement(Main, null), document.getElementById('reactMain') );
})();
