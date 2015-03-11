/** @jsx React.DOM */
(function() {
"use strict";

var NO_MORE_RECORDS_DIAGNOSTIC_URI = "info:srw/diagnostic/1/61";

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
var ModalMixin = window.MyReact.ModalMixin;
var Modal = window.MyReact.Modal;

var multipleLanguageCode = "mul"; // see ISO-693-3

var layers = [
	{
		id: "text",
		name: "Text Resources",
		searchPlaceholder: "Elephant",
		searchLabel: "Search text",
		searchLabelBkColor: "#fed",
		className: '',
	},
	{
		id: "sampa",
		name: "Phonetic Transcriptions",
		searchPlaceholder: "stA:z",
		searchLabel: "SAMPA query",
		searchLabelBkColor: "#eef",
		disabled: true,
	},
];
var layerMap = {
	text: layers[0],
	sampa: layers[1], 
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
		corpus.priority = 1; // used for ordering search results in corpus view 
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

function encodeQueryData(data)
{
	var ret = [];
	for (var d in data) {
		ret.push(encodeURIComponent(d) + "=" + encodeURIComponent(data[d]));
	}
	return ret.join("&");
}


var AggregatorPage = window.MyAggregator.AggregatorPage = React.createClass({displayName: 'AggregatorPage',
	propTypes: {
		ajax: PT.func.isRequired
	},

	nohits: { 
		results: null,
	},
	anyLanguage: [multipleLanguageCode, "Any Language"],

	getInitialState: function () {
		return {
			corpora: new Corpora([], this.updateCorpora),
			languageMap: {},
			weblichtLanguages: [],
			query: "",
			language: this.anyLanguage,
			languageFilter: 'byMeta',
			searchLayerId: "text",
			numberOfResults: 10,

			searchId: null,
			timeout: 0,
			hits: this.nohits,

			zoomedCorpusHit: null,
		};
	},

	componentDidMount: function() {
		this.props.ajax({
			url: 'rest/init',
			success: function(json, textStatus, jqXHR) {
				if (this.isMounted()) {
					this.setState({
						corpora : new Corpora(json.corpora, this.updateCorpora),
						languageMap: json.languages,
						weblichtLanguages: json.weblichtLanguages,
					});
				}
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
		var selectedIds = this.state.corpora.getSelectedIds();
		// console.log("searching in the following corpora:", selectedIds);
		this.props.ajax({
			url: 'rest/search',
			type: "POST",
			data: {
				layer: this.state.searchLayerId,
				language: this.state.language[0],
				query: query,
				numberOfResults: this.state.numberOfResults,
				corporaIds: selectedIds,
			},
			success: function(searchId, textStatus, jqXHR) {
				// console.log("search ["+query+"] ok: ", searchId, jqXHR);
				var timeout = 250;
				setTimeout(this.refreshSearchResults, timeout);
				this.setState({ searchId: searchId, timeout: timeout });
			}.bind(this),
		});
	},
	nextResults: function(corpusId) {
		// console.log("searching next results in corpus:", corpusId);
		this.props.ajax({
			url: 'rest/search/'+this.state.searchId,
			type: "POST",
			data: {
				corpusId: corpusId,
				numberOfResults: this.state.numberOfResults,
			},
			success: function(searchId, textStatus, jqXHR) {
				// console.log("search ["+query+"] ok: ", searchId, jqXHR);
				var timeout = 250;
				setTimeout(this.refreshSearchResults, timeout);
				this.setState({ searchId: searchId, timeout: timeout });
			}.bind(this),
		});
	},

	refreshSearchResults: function() {
		if (!this.state.searchId || !this.isMounted()) {
			return;
		}
		this.props.ajax({
			url: 'rest/search/'+this.state.searchId,
			success: function(json, textStatus, jqXHR) {
				var timeout = this.state.timeout;
				if (json.inProgress) {
					if (timeout < 10000) {
						timeout = 1.5 * timeout;
					}
					setTimeout(this.refreshSearchResults, timeout);
					// console.log("new search in: " + this.timeout + "ms");
				} else {
					console.log("search ended; hits:", json);
				}
				var corpusHit = this.state.zoomedCorpusHit;
				if (corpusHit) {
					for (var resi = 0; resi < json.results.length; resi++) {
						var res = json.results[resi];
						if (res.corpus.id === corpusHit.corpus.id) {
							corpusHit = res;
							break;
						}
					}
				}
				this.setState({ hits: json, timeout: timeout, zoomedCorpusHit: corpusHit});
			}.bind(this),
		});
	},

	getExportParams: function(corpusId, format, filterLanguage) {
		var params = corpusId ? {corpusId:corpusId}:{};
		if (format) params.format = format;
		if (filterLanguage) {
			params.filterLanguage = filterLanguage;
		} else if (this.state.languageFilter === 'byGuess' || this.state.languageFilter === 'byMetaAndGuess') {
			params.filterLanguage = this.state.language[0];
		}
		return encodeQueryData(params);
	},

	getDownloadLink: function(corpusId, format) {
		return 'rest/search/'+this.state.searchId+'/download?' + 
			this.getExportParams(corpusId, format);
	},

	getToWeblichtLink: function(corpusId, forceLanguage) {
		return 'rest/search/'+this.state.searchId+'/toWeblicht?' + 
			this.getExportParams(corpusId, null, forceLanguage);
	},

	setLanguageAndFilter: function(languageObj, languageFilter) {
		this.state.corpora.setVisibility(this.state.searchLayerId, 
			languageFilter === 'byGuess' ? multipleLanguageCode : languageObj[0]);
		this.setState({
			language: languageObj, 
			languageFilter: languageFilter, 
			corpora: this.state.corpora, // === this.state.corpora.update();
		});
	},

	setLayer: function(layerId) {
		this.state.corpora.setVisibility(layerId, this.state.language[0]);
		this.setState({
			searchLayerId: layerId, 
			hits: this.nohits, 
			searchId: null,
			corpora: this.state.corpora, // === this.state.corpora.update();
		});
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
		var noLangFiltering = this.state.languageFilter === 'byMeta';
		var langCode = this.state.language[0];
		var results = null, inProgress = 0, hits = 0;
		if (this.state.hits.results) {
			results = this.state.hits.results.map(function(corpusHit) { 
				return {
					corpus: corpusHit.corpus,
					inProgress: corpusHit.inProgress,
					exception: corpusHit.exception,
					diagnostics: corpusHit.diagnostics,
					kwics: noLangFiltering ? corpusHit.kwics :
						corpusHit.kwics.filter(function(kwic) {
							return kwic.language === langCode || 
							       langCode === multipleLanguageCode || 
							       langCode === null; 
						}),
				};
			});
			for (var i = 0; i < results.length; i++) {
				var result = results[i];
				if (result.inProgress) {
					inProgress++;
				}
				if (result.kwics.length > 0) {
					hits ++;
				}
			}
		}
		return {
			results: results,
			hits: hits,
			inProgress: inProgress,
		};
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

	toggleResultModal: function(e, corpusHit) {
		$(this.refs.resultModal.getDOMNode()).modal();
		this.setState({zoomedCorpusHit: corpusHit});
		e.preventDefault();
		e.stopPropagation();
	},

	onQuery: function(event) {
		this.setState({query: event.target.value});
	},

	handleKey: function(event) {
		if (event.keyCode==13) {
			this.search();
		}
	},

	renderZoomedResultTitle: function(corpusHit) {
		if (!corpusHit) return React.createElement("span", null);
		var corpus = corpusHit.corpus;
		return React.createElement("h3", {style: {fontSize:'1em'}}, 
					corpus.title, 
					 corpus.landingPage ? 
						React.createElement("a", {href: corpus.landingPage, onClick: this.stop, style: {fontSize:12}}, 
							React.createElement("span", null, " – Homepage "), 
							React.createElement("i", {className: "glyphicon glyphicon-home"})
						): false
				);
	},

	render: function() {
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
								tabIndex: "1", onChange: this.onQuery, onKeyDown: this.handleKey}), 
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
												var cls = l.disabled ? 'disabled':'';
												var handler = function() { if (!l.disabled) this.setLayer(l.id); }.bind(this);
												return React.createElement("li", {key: l.id, className: cls}, " ", React.createElement("a", {tabIndex: "-1", href: "#", 
													onClick: handler}, " ", l.name, " "));
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

				React.createElement(Modal, {ref: "corporaModal", title: React.createElement("span", null, "Collections")}, 
					React.createElement(CorpusView, {corpora: this.state.corpora, languageMap: this.state.languageMap})
				), 

				React.createElement(Modal, {ref: "languageModal", title: React.createElement("span", null, "Select Language")}, 
					React.createElement(LanguageSelector, {anyLanguage: this.anyLanguage, 
									  languageMap: this.state.languageMap, 
									  selectedLanguage: this.state.language, 
									  languageFilter: this.state.languageFilter, 
									  languageChangeHandler: this.setLanguageAndFilter})
				), 

				React.createElement(Modal, {ref: "resultModal", title: this.renderZoomedResultTitle(this.state.zoomedCorpusHit)}, 
					React.createElement(ZoomedResult, {corpusHit: this.state.zoomedCorpusHit, 
								  nextResults: this.nextResults, 
								  getDownloadLink: this.getDownloadLink, 
								  getToWeblichtLink: this.getToWeblichtLink, 
								  searchedLanguage: this.state.language, 
								  weblichtLanguages: this.state.weblichtLanguages, 
								  languageMap: this.state.languageMap})
				), 

				React.createElement("div", {className: "top-gap"}, 
					React.createElement(Results, {collhits: this.filterResults(), 
							 toggleResultModal: this.toggleResultModal, 
							 getDownloadLink: this.getDownloadLink, 
							 getToWeblichtLink: this.getToWeblichtLink, 
							 searchedLanguage: this.state.language})
				)
			)
			);
	},
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

var ResultMixin = window.MyReact.ResultMixin = {
	// getDefaultProps: function(){
	// 	return {hasPopover: true};
	// },
 
	getInitialState: function () {
		return { 
			displayKwic: false,
		};
	},

	toggleKwic: function() {
		this.setState({displayKwic:!this.state.displayKwic});
	},

	renderPanelTitle: function(corpus) {
		return	React.createElement("div", {className: "inline"}, 
					React.createElement("span", {className: "corpusName"}, " ", corpus.title), 
					React.createElement("span", {className: "institutionName"}, " — ", corpus.institution.name)
				);
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

	renderDiagnostic: function(d, key) {
		if (d.uri === NO_MORE_RECORDS_DIAGNOSTIC_URI) {
			return false;
		}
		return 	React.createElement("div", {className: "alert alert-warning", key: key}, 
					React.createElement("div", null, "Diagnostic: ", d.message)
				); 
	},

	renderDiagnostics: function(corpusHit) {
		if (!corpusHit.diagnostics || corpusHit.diagnostics.length === 0) {
			return false;
		}
		return corpusHit.diagnostics.map(this.renderDiagnostic);
	},

	renderErrors: function(corpusHit) {
		var xc = corpusHit.exception;
		if (!xc) {
			return false;
		}
		return 	(
			React.createElement("div", {className: "alert alert-danger", role: "alert"}, 
				React.createElement("div", null, "Exception: ", xc.message), 
				 xc.cause ? React.createElement("div", null, "Caused by: ", xc.cause) : false
			)
		);
	},

	renderPanelBody: function(corpusHit) {
		var fulllength = {width:"100%"};
		if (this.state.displayKwic) {
			return 	React.createElement("div", null, 
						this.renderErrors(corpusHit), 
						this.renderDiagnostics(corpusHit), 
						React.createElement("table", {className: "table table-condensed table-hover", style: fulllength}, 
							React.createElement("tbody", null, corpusHit.kwics.map(this.renderRowsAsKwic))
						)
					);
		} else {
			return	React.createElement("div", null, 
						this.renderErrors(corpusHit), 
						this.renderDiagnostics(corpusHit), 
						corpusHit.kwics.map(this.renderRowsAsHits)
					);
		}
	},

	renderDisplayKWIC: function() {
		return 	React.createElement("div", {className: "inline btn-group", style: {display:"inline-block"}}, 
					React.createElement("label", {forHtml: "inputKwic", className: "btn btn-flat"}, 
						 this.state.displayKwic ? 
							React.createElement("input", {id: "inputKwic", type: "checkbox", value: "kwic", checked: true, onChange: this.toggleKwic}) :
							React.createElement("input", {id: "inputKwic", type: "checkbox", value: "kwic", onChange: this.toggleKwic}), 
						
						" " + ' ' +
						"Display as Key Word In Context"
					)
				);
	},

	renderDownloadLinks: function(corpusId) {
		return (
			React.createElement("div", {className: "dropdown"}, 
				React.createElement("button", {className: "btn btn-flat", 'aria-expanded': "false", 'data-toggle': "dropdown"}, 
					React.createElement("span", {className: "glyphicon glyphicon-download-alt", 'aria-hidden': "true"}), 
					" ", " Download ", " ", 
					React.createElement("span", {className: "caret"})
				), 
				React.createElement("ul", {className: "dropdown-menu"}, 
					React.createElement("li", null, " ", React.createElement("a", {href: this.props.getDownloadLink(corpusId, "csv")}, 
							" ", " As CSV file")), 
					React.createElement("li", null, " ", React.createElement("a", {href: this.props.getDownloadLink(corpusId, "excel")}, 
							" ", " As Excel file")), 
					React.createElement("li", null, " ", React.createElement("a", {href: this.props.getDownloadLink(corpusId, "tcf")}, 
							" ", " As TCF file")), 
					React.createElement("li", null, " ", React.createElement("a", {href: this.props.getDownloadLink(corpusId, "text")}, 
							" ", " As Plain Text file"))
				)
			)
		);
	},

	renderToWeblichtLinks: function(corpusId, forceLanguage, error) {
		return (
			React.createElement("div", {className: "dropdown"}, 
				React.createElement("button", {className: "btn btn-flat", 'aria-expanded': "false", 'data-toggle': "dropdown"}, 
					React.createElement("span", {className: "glyphicon glyphicon-export", 'aria-hidden': "true"}), 
					" ", " Use Weblicht ", " ", 
					React.createElement("span", {className: "caret"})
				), 
				React.createElement("ul", {className: "dropdown-menu"}, 
					React.createElement("li", null, 
						error ? 
							React.createElement("div", {className: "alert alert-danger", style: {margin:10, width:200}}, error) :
							React.createElement("a", {href: this.props.getToWeblichtLink(corpusId, forceLanguage), target: "_blank"}, " ", 
								"Send to Weblicht")
						
					)
				)
			)
		);
	},

};

var ZoomedResult = React.createClass({displayName: 'ZoomedResult',
	propTypes: {
		corpusHit: PT.object,
		nextResults: PT.func.isRequired,
		languageMap: PT.object.isRequired,
		weblichtLanguages: PT.array.isRequired,
		searchedLanguage: PT.array.isRequired,
		getDownloadLink: PT.func.isRequired,
		getToWeblichtLink: PT.func.isRequired,
	},
	mixins: [ResultMixin],

	getInitialState: function() {
		return {
			inProgress: false,
		};
	},

	componentWillReceiveProps: function() {
		this.setState({inProgress: false});
	},

	nextResults: function(e) {
		this.setState({inProgress: true});
		this.props.nextResults(this.props.corpusHit.corpus.id);
	},

	renderLanguages: function(languages) {
		return languages
				.map(function(l) { return this.props.languageMap[l]; }.bind(this))
				.sort()
				.join(", ");
	},

	renderMoreResults:function(){
		if (this.state.inProgress || this.props.corpusHit.inProgress) 
			return React.createElement("span", {style: {fontStyle:'italic'}}, "Retrieving results, please wait...");

		var moreResults = true;
		for (var i = 0; i < this.props.corpusHit.diagnostics.length; i++) {
			var d = this.props.corpusHit.diagnostics[i];
			if (d.uri === NO_MORE_RECORDS_DIAGNOSTIC_URI) {
				moreResults = false;
				break;
			}
		}
		if (!moreResults)
			return React.createElement("span", {style: {fontStyle:'italic'}}, "No other results available for this query");
		return	React.createElement("button", {className: "btn btn-default", onClick: this.nextResults}, 
					React.createElement("span", {className: "glyphicon glyphicon-option-horizontal", 'aria-hidden': "true"}), " More Results"
				);
	},

	render: function() {
		var corpusHit = this.props.corpusHit;
		if (!corpusHit) {
			return false;
		}

		var forceLanguage = null, wlerror = null;
		if (this.props.weblichtLanguages.indexOf(this.props.searchedLanguage[0]) < 0) {
			// the search language is either AnyLanguage or unsupported 
			if (this.props.searchedLanguage[0] === multipleLanguageCode) {
				if (corpusHit.corpus.languages && corpusHit.corpus.languages.length === 1) {
					forceLanguage = corpusHit.corpus.languages[0];
				} else {
					var langs = corpusHit.kwics.map(function(kwic) {return kwic.language;});
					langs = _.uniq(langs.filter(function(l){ return l !== null; }));
					console.log("languages:", langs);
					if (langs.length === 1) {
						forceLanguage = langs[0];
					}
				}
			} 
			if (!forceLanguage) {
				wlerror = "Cannot use WebLicht: unsupported language ("+this.props.searchedLanguage[1]+")";
			}
		}
		var corpus = corpusHit.corpus;
		return 	React.createElement("div", null, 
					React.createElement(ReactCSSTransitionGroup, {transitionName: "fade"}, 
						React.createElement("div", {className: "corpusDescription"}, 
							React.createElement("p", null, React.createElement("i", {className: "fa fa-institution"}), " ", corpus.institution.name), 
							corpus.description ? 
								React.createElement("p", null, React.createElement("i", {className: "glyphicon glyphicon-info-sign"}), " ", corpus.description): false, 
							React.createElement("p", null, React.createElement("i", {className: "fa fa-language"}), " ", this.renderLanguages(corpus.languages))
						), 
						React.createElement("div", {style: {marginBottom:2}}, 
							React.createElement("div", {className: "float-right"}, 
								React.createElement("div", null, 
									 this.renderDisplayKWIC(), 
									React.createElement("div", {className: "inline"}, " ", this.renderDownloadLinks(corpusHit.corpus.id), " "), 
									React.createElement("div", {className: "inline"}, " ", this.renderToWeblichtLinks(corpus.id, forceLanguage, wlerror), " ")
								)
							), 
							React.createElement("div", {style: {clear:'both'}})
						), 
						React.createElement("div", {className: "panel"}, 
							React.createElement("div", {className: "panel-body corpusResults"}, this.renderPanelBody(corpusHit))
						), 

						React.createElement("div", {style: {textAlign:'center', marginTop:10}}, 
							 this.renderMoreResults() 
						)

					)
				);
	},
});

var Results = React.createClass({displayName: 'Results',
	propTypes: {
		collhits: PT.object.isRequired,
		searchedLanguage: PT.array.isRequired,
		toggleResultModal: PT.func.isRequired,
		getDownloadLink: PT.func.isRequired,
		getToWeblichtLink: PT.func.isRequired,
	},
	mixins: [ResultMixin],

	renderPanelInfo: function(corpusHit) {
		var corpus = corpusHit.corpus;
		var inline = {display:"inline-block"};
		return	React.createElement("div", null, 
					" ", 
					React.createElement("div", {style: inline}, 
						React.createElement("button", {className: "btn btn-default zoomResultButton", 
								onClick: function(e){this.props.toggleResultModal(e,corpusHit)}.bind(this)}, 
								React.createElement("span", {className: "glyphicon glyphicon-eye-open"}), " View"
						)
					)
				);
	},

	renderResultPanel: function(corpusHit) {
		if (corpusHit.kwics.length === 0 && 
			!corpusHit.exception &&
			corpusHit.diagnostics.length === 0) {
				return false;
		}
		return 	React.createElement(Panel, {key: corpusHit.corpus.id, 
						title: this.renderPanelTitle(corpusHit.corpus), 
						info: this.renderPanelInfo(corpusHit)}, 
					this.renderPanelBody(corpusHit)
				);
	},

	renderProgressMessage: function() {
		var collhits = this.props.collhits;
		var done = collhits.results.length - collhits.inProgress;
		var msg = collhits.hits + " matching collections found in " + done + " searched collections";
		var percents = Math.round(100 * collhits.hits / collhits.results.length);
		var styleperc = {width: percents+"%"};
		return 	React.createElement("div", {style: {marginTop:10}}, 
					React.createElement("div", null, msg), 
					collhits.inProgress > 0 ? 
						React.createElement("div", {className: "progress", style: {marginBottom:10}}, 
							React.createElement("div", {className: "progress-bar progress-bar-striped active", role: "progressbar", 
								'aria-valuenow': percents, 'aria-valuemin': "0", 'aria-valuemax': "100", style: styleperc}), 
							percents > 2 ? false :
								React.createElement("div", {className: "progress-bar progress-bar-striped active", role: "progressbar", 
									'aria-valuenow': "100", 'aria-valuemin': "0", 'aria-valuemax': "100", 
									style: {width: '100%', backgroundColor:'#888'}})
							
						) : 
						false
				);
	},

	render: function() {
		var collhits = this.props.collhits;
		if (!collhits.results) {
			return false;
		}
		var showprogress = collhits.inProgress > 0;
		return 	React.createElement("div", null, 
					React.createElement(ReactCSSTransitionGroup, {transitionName: "fade"}, 
						 showprogress ? this.renderProgressMessage() : React.createElement("div", {style: {height:20}}), 
						React.createElement("div", {style: {marginBottom:2}}, 
							 showprogress ? false : 
								React.createElement("div", {className: "float-left"}, " ", collhits.hits + " matching collections found", " "), 
							
							 collhits.hits === 0 ? false : 
								React.createElement("div", {className: "float-right"}, 
									React.createElement("div", null, 
										 this.renderDisplayKWIC(), 
										 collhits.inProgress === 0 ? 
											React.createElement("div", {className: "inline"}, " ", this.renderDownloadLinks(), " ")
											:false
										
									)
								), 
							
							React.createElement("div", {style: {clear:'both'}})
						), 
						collhits.results.map(this.renderResultPanel)
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

	uniq: function(a) {
		var r = [];
		for (var i = 0; i < a.length; i++) {
			if (r.indexOf(a[i]) < 0) {
				r.push(a[i]);
			}
		}
		return r;
	},
};

})();
