/** @jsx React.DOM */
(function() {
"use strict";

window.MyAggregator = window.MyAggregator || {};

var React = window.React;
var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.CSSTransitionGroup;

var CorpusSelection = window.MyAggregator.CorpusSelection;
var HitNumber = window.MyAggregator.HitNumber;
var CorpusView = window.MyAggregator.CorpusView;
var Popover = window.MyReact.Popover;
var InfoPopover = window.MyReact.InfoPopover;
var Panel = window.MyReact.Panel;
var Modal = window.MyReact.Modal;

var multipleLanguageCode = "mul"; // see ISO-693-3

var layers = [
	{
		id: "sampa",
		name: "Phonetic Transcriptions",
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
		return x.title.toLowerCase().localeCompare(y.title.toLowerCase()); 
	};

	this.recurse(function(corpus) { corpus.subCorpora.sort(sortFn); });
	this.corpora.sort(sortFn);

	this.recurse(function(corpus, index) {
		corpus.visible = true; // visible in the corpus view
		corpus.selected = true; // selected in the corpus view
		corpus.expanded = false; // not expanded in the corpus view
		corpus.priority = 1; // priority in corpus view
		corpus.index = index; // original order, used for stable sort
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
		if (false === fn(corpus, index)) {
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


var AggregatorPage = window.MyAggregator.AggregatorPage = React.createClass({displayName: 'AggregatorPage',
	propTypes: {
		ajax: PT.func.isRequired
	},

	timeout: 0,
	nohits: { 
		requests: [],
		results: [],
	},
	anyLanguage: [multipleLanguageCode, "Any Language"],

	getInitialState: function () {
		return {
			corpora: new Corpora([], this.updateCorpora),
			languageMap: {},
			query: "",
			language: this.anyLanguage,
			languageFilter: 'byMeta',
			searchLayerId: "text",
			numberOfResults: 10,

			searchId: null,
			hits: this.nohits,
		};
	},

	componentDidMount: function() {
		this.refreshCorpora();
		this.refreshLanguages();
	},

	refreshCorpora: function() {
		this.props.ajax({
			url: 'rest/corpora',
			success: function(json, textStatus, jqXHR) {
				this.setState({corpora : new Corpora(json, this.updateCorpora)});
			}.bind(this),
		});
	},

	refreshLanguages: function() {
		this.props.ajax({
			url: 'rest/languages',
			success: function(json, textStatus, jqXHR) {
				this.setState({languageMap : json});
			}.bind(this),
		});
	},

	updateCorpora: function(corpora) {
		this.setState({corpora:corpora});
	},

	search: function() {
		var query = this.state.query;
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
				corporaIds: this.state.corpora.getSelectedIds(),
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
					console.log("search ended; hits:", json);
				}
				this.setState({hits:json});
			}.bind(this),
		});
	},

	setLanguageAndFilter: function(languageObj, languageFilter) {
		this.state.corpora.setVisibility(this.state.searchLayerId, 
			languageFilter === 'byGuess' ? multipleLanguageCode : languageObj[0]);
		this.setState({language: languageObj, languageFilter: languageFilter});
		this.state.corpora.update();
	},

	setLayer: function(layerId) {
		this.state.corpora.setVisibility(layerId, this.state.language[0]);
		this.state.corpora.update();
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
		e.stopPropagation();
	},

	filterResults: function() {
		var langCode = this.state.language[0];
		return this.state.hits.results.map(function(corpusHit) { 
			return {
				corpus: corpusHit.corpus,
				startRecord: corpusHit.startRecord,
				endRecord: corpusHit.endRecord,
				exception: corpusHit.exception,
				diagnostics: corpusHit.diagnostics,
				searchString: corpusHit.searchString,
				kwics: corpusHit.kwics.filter(function(kwic){
					return kwic.language === langCode || langCode === multipleLanguageCode || langCode === null; 
				}),
			};
		});
	},

	toggleLanguageSelection: function(e) {
		$(this.refs.languageModal.getDOMNode()).modal();
		e.preventDefault();
		e.stopPropagation();
	},

	toggleCorpusSelection: function(e) {
		$(this.refs.corporaModal.getDOMNode()).modal();
		e.preventDefault();
		e.stopPropagation();
	},

	handleChange: function(event) {
    	this.setState({query: event.target.value});
	},

	handleKey: function(event) {
    	if (event.keyCode==13) {
    		this.search();
    	}
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

							React.createElement("input", {className: "form-control input-lg search", name: "query", type: "text", 
								value: this.state.query, placeholder: this.props.placeholder, 
								tabIndex: "1", onChange: this.handleChange, onKeyDown: this.handleKey}), 
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

							React.createElement("div", {className: "input-group"}, 
								
								React.createElement("span", {className: "input-group-addon nobkg"}, "Search for"), 
								
								React.createElement("div", {className: "input-group-btn"}, 
									React.createElement("button", {className: "form-control btn btn-default", 
											onClick: this.toggleLanguageSelection}, 
										this.state.language[1], " ", React.createElement("span", {className: "caret"})
									), 
									React.createElement("span", null)
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
								React.createElement("span", {className: "input-group-addon nobkg"}, "in"), 
								React.createElement("button", {type: "button", className: "btn btn-default", onClick: this.toggleCorpusSelection}, 
									this.state.corpora.getSelectedMessage(), " ", React.createElement("span", {className: "caret"})
								)
							), 							

							React.createElement("div", {className: "input-group"}, 
								React.createElement("span", {className: "input-group-addon nobkg"}, "and show up to"), 
								React.createElement("div", {className: "input-group-btn"}, 
									React.createElement("input", {type: "number", className: "form-control input", min: "10", max: "250", 
										style: {width:60}, 
										onChange: this.setNumberOfResults, value: this.state.numberOfResults, 
										onKeyPress: this.stop})
								), 
								React.createElement("span", {className: "input-group-addon nobkg"}, "hits")
							)
						)
					)
				), 

	            React.createElement(Modal, {ref: "corporaModal", title: "Collections"}, 
					React.createElement(CorpusView, {corpora: this.state.corpora, languageMap: this.state.languageMap})
	            ), 

	            React.createElement(Modal, {ref: "languageModal", title: "Select Language"}, 
					React.createElement(LanguageSelector, {anyLanguage: this.anyLanguage, 
									  languageMap: this.state.languageMap, 
									  selectedLanguage: this.state.language, 
									  languageFilter: this.state.languageFilter, 
									  languageChangeHandler: this.setLanguageAndFilter})
	            ), 

				React.createElement("div", {className: "top-gap"}, 
					React.createElement(Results, {requests: this.state.hits.requests, 
					         results: this.filterResults(), 
					         searchedLanguage: this.state.language})
				)
			)
			);
	},
	render: function() {
		return this.renderAggregator();
	}
});



/////////////////////////////////

var LanguageSelector = React.createClass({displayName: 'LanguageSelector',
	propTypes: {
		anyLanguage: PT.array.isRequired,
		languageMap: PT.object.isRequired,
		selectedLanguage: PT.array.isRequired,
		languageFilter: PT.string.isRequired,
		languageChangeHandler: PT.func.isRequired,
	},
	mixins: [React.addons.LinkedStateMixin],

	selectLang: function(language) {
		this.props.languageChangeHandler(language, this.props.languageFilter);
	},

	setFilter: function(filter) {
		this.props.languageChangeHandler(this.props.selectedLanguage, filter);
	},

	renderLanguageObject: function(lang) {
		var desc = lang[1] + " [" + lang[0] + "]";
		var style = {
			whiteSpace: "nowrap",
			fontWeight: lang[0] === this.props.selectedLanguage[0] ? "bold":"normal",
		};
		return	React.createElement("div", {key: lang[0]}, 
					React.createElement("a", {tabIndex: "-1", href: "#", style: style, onClick: this.selectLang.bind(this, lang)}, desc)
				);
	},

	renderRadio: function(option) {
		return	this.props.languageFilter === option ? 
				React.createElement("input", {type: "radio", name: "filterOpts", value: option, checked: true, onChange: this.setFilter.bind(this, option)})
				: React.createElement("input", {type: "radio", name: "filterOpts", value: option, onChange: this.setFilter.bind(this, option)});
	},

	render: function() {
		var languages = _.pairs(this.props.languageMap)
		                 .sort(function(l1, l2){return l1[1].localeCompare(l2[1]); });
		languages.unshift(this.props.anyLanguage);
		languages = languages.map(this.renderLanguageObject);
		var third = Math.round(languages.length/3);
		var l1 = languages.slice(0, third);
		var l2 = languages.slice(third, 2*third);
		var l3 = languages.slice(2*third, languages.length);

		return	React.createElement("div", null, 
					React.createElement("div", {className: "row"}, 
						React.createElement("div", {className: "col-sm-4"}, l1), 
						React.createElement("div", {className: "col-sm-4"}, l2), 
						React.createElement("div", {className: "col-sm-4"}, l3), 
						React.createElement("div", {className: "col-sm-12", style: {marginTop:10, marginBottom:10, borderBottom:"1px solid #eee"}})
					), 
					React.createElement("form", {className: "form", role: "form"}, 
						React.createElement("div", {className: "input-group"}, 
							React.createElement("div", null, 
							React.createElement("label", {style: {color:'black'}}, 
								 this.renderRadio('byMeta'), " ", 
		  						"Use the collections", "'", " specified language to filter results" 
							)
							), 
							React.createElement("div", null, 
							React.createElement("label", {style: {color:'black'}}, 
								 this.renderRadio('byGuess'), " ", 
		  						"Filter results by using a language detector" 
							)
							), 
							React.createElement("div", null, 
							React.createElement("label", {style: {color:'black'}}, 
								 this.renderRadio('byMetaAndGuess'), " ", 
		  						"First use the collections", "'", " specified language then also use a language detector"
							)
							)
						)
					)
				);
	}
});

/////////////////////////////////

var Results = React.createClass({displayName: 'Results',
	propTypes: {
		requests: PT.array.isRequired,
		results: PT.array.isRequired,
		searchedLanguage: PT.array.isRequired,
	},

	getInitialState: function () {
		return { 
			displayKwic: false,
		};
	},

	toggleKwic: function() {
		this.setState({displayKwic:!this.state.displayKwic});
	},

	renderRowLanguage: function(hit) {
		return false; //<span style={{fontFace:"Courier",color:"black"}}>{hit.language} </span> ;
	},

	renderRowsAsHits: function(hit,i) {
		function renderTextFragments(tf, idx) {
			return React.createElement("span", {key: idx, className: tf.hit?"keyword":""}, tf.text);
		}
		return	React.createElement("p", {key: i, className: "hitrow"}, 
					this.renderRowLanguage(hit), 
					hit.fragments.map(renderTextFragments)
				);
	},

	renderRowsAsKwic: function(hit,i) {
		var sleft={textAlign:"left", verticalAlign:"top", width:"50%"};
		var scenter={textAlign:"center", verticalAlign:"top", maxWidth:"50%"};
		var sright={textAlign:"right", verticalAlign:"top", maxWidth:"50%"};
		return	React.createElement("tr", {key: i, className: "hitrow"}, 
					React.createElement("td", null, this.renderRowLanguage(hit)), 
					React.createElement("td", {style: sright}, hit.left), 
					React.createElement("td", {style: scenter, className: "keyword"}, hit.keyword), 
					React.createElement("td", {style: sleft}, hit.right)
				);
	},

	renderPanelTitle: function(corpus) {
		var inline = {display:"inline-block"};
		return	React.createElement("div", {style: inline}, 
					React.createElement("span", {className: "corpusName"}, " ", corpus.title), 
					React.createElement("span", {className: "institutionName"}, " — ", corpus.institution.name)
				);
	},

	renderPanelInfo: function(corpus) {
		var inline = {display:"inline-block"};
		return	React.createElement("div", null, 
					React.createElement(InfoPopover, {placement: "left", title: corpus.title}, 
						React.createElement("dl", {className: "dl-horizontal"}, 
							React.createElement("dt", null, "Institution"), 
							React.createElement("dd", null, corpus.institution.name), 

							corpus.description ? React.createElement("dt", null, "Description"):false, 
							corpus.description ? React.createElement("dd", null, corpus.description): false, 

							corpus.landingPage ? React.createElement("dt", null, "Landing Page") : false, 
							corpus.landingPage ? 
								React.createElement("dd", null, React.createElement("a", {href: corpus.landingPage}, corpus.landingPage)):
								false, 

							React.createElement("dt", null, "Languages"), 
							React.createElement("dd", null, corpus.languages.join(", "))
						)
					), 
					" ", 
					React.createElement("div", {style: inline}, 
						React.createElement("button", {className: "btn btn-default btn-xs", onClick: this.zoom}, 
							React.createElement("span", {className: "glyphicon glyphicon-fullscreen"})
						)
					)
				);
	},

	renderDiagnostics: function(corpusHit) {
		if (!corpusHit.diagnostics || corpusHit.diagnostics.length === 0) {
			return false;
		}

		return corpusHit.diagnostics.map(function(d) {
			return 	React.createElement("div", {className: "alert alert-danger", role: "alert", key: d.dgnUri}, 
						d.dgnMessage, ": ", d.dgnDiagnostic
					);
		});
	},

	renderPanelBody: function(corpusHit) {
		var fulllength = {width:"100%"};
		if (this.state.displayKwic) {
			return 	React.createElement("div", null, 
						this.renderDiagnostics(corpusHit), 
						React.createElement("table", {className: "table table-condensed table-hover", style: fulllength}, 
							React.createElement("tbody", null, corpusHit.kwics.map(this.renderRowsAsKwic))
						)
					);
		} else {
			return	React.createElement("div", null, 
						this.renderDiagnostics(corpusHit), 
						corpusHit.kwics.map(this.renderRowsAsHits)
					);
		}
	},

	renderResultPanels: function(corpusHit) {
		if (corpusHit.kwics.length === 0 &&
			corpusHit.diagnostics.length === 0) {
			return false;
		}
		return 	React.createElement(Panel, {key: corpusHit.corpus.title, 
						title: this.renderPanelTitle(corpusHit.corpus), 
						info: this.renderPanelInfo(corpusHit.corpus)}, 
					this.renderPanelBody(corpusHit)
				);
	},

	renderProgressBar: function() {
		var percents = 100 * this.props.results.length / (this.props.requests.length + this.props.results.length);
		var sperc = Math.round(percents);
		var styleperc = {width: sperc+"%"};
		return this.props.requests.length > 0 ? 
			React.createElement("div", {className: "progress", style: {marginBottom:10}}, 
  				React.createElement("div", {className: "progress-bar progress-bar-striped active", role: "progressbar", 
  					'aria-valuenow': sperc, 'aria-valuemin': "0", 'aria-valuemax': "100", style: styleperc})
			) : 
			React.createElement("span", null);
	},

	renderSearchingMessage: function() {
		return false;
		// if (this.props.requests.length === 0)
		// 	return false;
		// return "Searching in " + this.props.requests.length + " collections...";
	},

	renderFoundMessage: function(hits) {
		if (this.props.results.length === 0)
			return false;
		var total = this.props.results.length;
		return hits + " collections with results found in " + total + " searched collections";
	},

	renderKwicCheckbox: function() {
		return	React.createElement("div", {className: "float-right", style: {marginRight:17}}, 
					React.createElement("div", {className: "btn-group", style: {display:"inline-block"}}, 
						React.createElement("label", {forHtml: "inputKwic", className: "btn-default"}, 
							 this.state.displayKwic ? 
								React.createElement("input", {id: "inputKwic", type: "checkbox", value: "kwic", checked: true, onChange: this.toggleKwic}) :
								React.createElement("input", {id: "inputKwic", type: "checkbox", value: "kwic", onChange: this.toggleKwic}), 
							
							" " + ' ' +
							"Display as Key Word In Context"
						)
					)
				);
	},

	render: function() {
		var hits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length > 0; }).length;
		var margintop = {marginTop:"10px"};
		var margin = {marginTop:"0", padding:"20px"};
		var inlinew = {display:"inline-block", margin:"0 5px 0 0", width:"240px;"};
		var right= {float:"right"};
		return 	React.createElement("div", null, 
					React.createElement(ReactCSSTransitionGroup, {transitionName: "fade"}, 
						React.createElement("div", {key: "-searching-message-", style: margintop}, this.renderSearchingMessage(), " "), 
						React.createElement("div", {key: "-found-message-", style: margintop}, this.renderFoundMessage(hits), " "), 
						React.createElement("div", {key: "-progress-", style: margintop}, this.renderProgressBar()), 
						hits > 0 ? 
							React.createElement("div", {key: "-option-KWIC-", className: "row"}, 
								this.renderKwicCheckbox()
							)
						 	: false, 
						this.props.results.map(this.renderResultPanels)
					)
				);
	}
});

var _ = window._ = window._ || {
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

	values: function(o){
		var ret = [];
		for (var x in o) {
			if (o.hasOwnProperty(x)) {
				ret.push(o[x]);
			}
		}
		return ret;
	},
};

})();
