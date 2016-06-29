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

//var queryTypes = [
var layers = [
	{
		id: "cql",
		name: "Text layer Contextual Query Language (CQL)",
		searchPlaceholder: "Elephant",
		searchLabel: "Text layer CQL query",
		searchLabelBkColor: "#fed",
		className: '',
	},
	{
		id: "fcs",
		name: "Multi-layer Federated Content Search Query Language (FCS-QL)",
		searchPlaceholder: "[word = 'annotation'][word = 'focused']",
		searchLabel: "Multi-layer FCS query",
		searchLabelBkColor: "#efd",
		disabled: false,
	},
];

var layerMap = {
     	cql: layers[0],
     	fcs: layers[1],
};
// var layers = [
// 	{
// 		id: "text",
// 		name: "Text Resources",
// 		searchPlaceholder: "Elephant",
// 		searchLabel: "Search text",
// 		searchLabelBkColor: "#fed",
// 		className: '',
// 	},
// 	{
// 		id: "sampa",
// 		name: "Phonetic Transcriptions",
// 		searchPlaceholder: "stA:z",
// 		searchLabel: "SAMPA query",
// 		searchLabelBkColor: "#eef",
// 		disabled: true,
// 	},
// 	{
// 		id: "lemma",
// 		name: "Lemma",
// 		searchPlaceholder: "|person|",
// 		searchLabel: "Lemma query",
// 		searchLabelBkColor: "#eff",
// 		disabled: false,
// 	},
// 	{
// 		id: "pos",
// 		name: "Part-of-Speech",
// 		searchPlaceholder: "PROPN",
// 		searchLabel: "PoS query",
// 		searchLabelBkColor: "#efe",
// 		disabled: false,
// 	},
// 	{
// 		id: "orth",
// 		name: "Orthographic Transcriptions",
// 		searchPlaceholder: "stA:z",
// 		searchLabel: "Orthographic query",
// 		searchLabelBkColor: "#eef",
// 		disabled: true,
// 	},
// 	{
// 		id: "norm",
// 		name: "Normalized Orthographic Transcriptions",
// 		searchPlaceholder: "stA:z",
// 		searchLabel: "Normalized Orthographic query",
// 		searchLabelBkColor: "#eef",
// 		disabled: true,
// 	},
// ];
// var layerMap = {
// 	text: layers[0],
// 	sampa: layers[1],
// 	lemma: layers[2],
// 	pos: layers[3],
// 	orth: layers[4],
// 	norm: layers[5],
// };

function getQueryVariable(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) == variable) {
            return decodeURIComponent(pair[1]);
        }
    }
    return null;
}

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
	//if (layerId !== "text") {
	//	return false;
	//}
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

Corpora.prototype.setAggregationContext = function(endpoints2handles) {
	var selectSubTree = function(select, corpus) {
		corpus.selected = select;
		this.recurseCorpora(corpus.subCorpora, function(c) { c.selected = corpus.selected; });
	};

	this.corpora.forEach(selectSubTree.bind(this, false));

	var corporaToSelect = [];
	_.pairs(endpoints2handles).forEach(function(endp){
		var endpoint = endp[0];
		var handles = endp[1];
		handles.forEach(function(handle){
			this.recurse(function(corpus){
				if (corpus.handle === handle) {
					corporaToSelect.push(corpus);
				}
			}.bind(this));
		}.bind(this));
	}.bind(this));

	corporaToSelect.forEach(selectSubTree.bind(this, true));
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


var AggregatorPage = window.MyAggregator.AggregatorPage = React.createClass({
	propTypes: {
		ajax: PT.func.isRequired,
		error: PT.func.isRequired,
		embedded: PT.bool,
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
			queryType: getQueryVariable('queryType') ||'cql',
			query: getQueryVariable('query') || '',
			language: this.anyLanguage,
			languageFilter: 'byMeta',
			//fixme!
			searchLayerId: getQueryVariable('queryType') ||'cql',
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
					var corpora = new Corpora(json.corpora, this.updateCorpora);
					window.MyAggregator.corpora = json.corpora;
					this.setState({
						corpora : corpora,
						languageMap: json.languages,
						weblichtLanguages: json.weblichtLanguages,
						query: this.state.query || json.query || '',
					});

					// // for testing aggregation context
					// json['x-aggregation-context'] = {
					// 	'EKUT': ["http://hdl.handle.net/11858/00-1778-0000-0001-DDAF-D"]
					// };

					if (json['x-aggregation-context']) {
						window.MyAggregator.xAggregationContext = json["x-aggregation-context"];
						corpora.setAggregationContext(json["x-aggregation-context"]);
						if (!corpora.getSelectedIds().length) {
							this.props.error("Cannot find the required collection, will search all collections instead");
							corpora.recurse(function(corpus) { corpus.selected = true; });
						}
						corpora.update();
					}

					if (getQueryVariable('mode') === 'search' ||
						json.mode === 'search') {
							window.MyAggregator.mode = 'search';
							this.search();
					}
				}
			}.bind(this),
		});
	},

	updateCorpora: function(corpora) {
		this.setState({corpora:corpora});
	},

	search: function() {
		var query = this.state.query;
		var queryType = this.state.queryType;
		if (!query || this.props.embedded) {
			this.setState({ hits: this.nohits, searchId: null });
			return;
		}
		var selectedIds = this.state.corpora.getSelectedIds();
		if (!selectedIds.length) {
			this.props.error("Please select a collection to search into");
			return;
		}

		// console.log("searching in the following corpora:", selectedIds);
		// console.log("searching with queryType:", queryType);
		this.props.ajax({
			url: 'rest/search',
			type: "POST",
			data: {
				layer: this.state.searchLayerId,
				language: this.state.language[0],
				queryType: queryType,
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
			queryType: layerId,
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
		if (!corpusHit) return <span/>;
		var corpus = corpusHit.corpus;
		return <h3 style={{fontSize:'1em'}}>
					{corpus.title}
					{ corpus.landingPage ?
						<a href={corpus.landingPage} onClick={this.stop} style={{fontSize:12}}>
							<span> – Homepage </span>
							<i className="glyphicon glyphicon-home"/>
						</a>: false}
				</h3>;
	},

	renderSearchButtonOrLink: function() {
		if (this.props.embedded) {
			var query = this.state.query;
			var queryType = this.state.queryType;
			var newurl = !query ? "#" :
				(window.MyAggregator.URLROOT + "?" + encodeQueryData({queryType:queryType, query:query, mode:'search'}));
			return (
				<a className="btn btn-default input-lg" style={{paddingTop:13}}
					type="button" target="_blank" href={newurl}>
					<i className="glyphicon glyphicon-search"></i>
				</a>
			);
		}
		return (
			<button className="btn btn-default input-lg" type="button" onClick={this.search}>
				<i className="glyphicon glyphicon-search"></i>
			</button>
		);
	},

	render: function() {
		var layer = layerMap[this.state.searchLayerId];
		return	(
			<div className="top-gap">
				<div className="row">
					<div className="aligncenter" style={{marginLeft:16, marginRight:16}}>
						<div className="input-group">
							<span className="input-group-addon" style={{backgroundColor:layer.searchLabelBkColor}}>
								{layer.searchLabel}
							</span>

							<input className="form-control input-lg search" name="query" type="text"
								value={this.state.query} placeholder={this.props.placeholder}
								tabIndex="1" onChange={this.onQuery} onKeyDown={this.handleKey} />
							<div className="input-group-btn">
								{this.renderSearchButtonOrLink()}
							</div>
						</div>
					</div>
				</div>

				<div className="wel" style={{marginTop:20}}>
					<div className="aligncenter" >
						<form className="form-inline" role="form">

							<div className="input-group">

								<span className="input-group-addon nobkg" >Search for</span>

								<div className="input-group-btn">
									<button className="form-control btn btn-default"
											onClick={this.toggleLanguageSelection}>
										{this.state.language[1]} <span className="caret"/>
									</button>
									<span/>
								</div>
								<div className="input-group-btn hidden-xxs">
									<ul ref="layerDropdownMenu" className="dropdown-menu">
										{ 	layers.map(function(l) {
												var cls = l.disabled ? 'disabled':'';
												var handler = function() { if (!l.disabled) this.setLayer(l.id); }.bind(this);
												return <li key={l.id} className={cls}> <a tabIndex="-1" href="#"
													onClick={handler}> {l.name} </a></li>;
											}.bind(this))
										}
									</ul>
									<button className="form-control btn btn-default"
											aria-expanded="false" data-toggle="dropdown" >
										{layer.name} <span className="caret"/>
									</button>
								</div>

							</div>

							<div className="input-group hidden-xs">
								<span className="input-group-addon nobkg">in</span>
								<button type="button" className="btn btn-default" onClick={this.toggleCorpusSelection}>
									{this.state.corpora.getSelectedMessage()} <span className="caret"/>
								</button>
							</div>

							<div className="input-group hidden-xs hidden-sm">
								<span className="input-group-addon nobkg">and show up to</span>
								<div className="input-group-btn">
									<input type="number" className="form-control input" min="10" max="250"
										style={{width:60}}
										onChange={this.setNumberOfResults} value={this.state.numberOfResults}
										onKeyPress={this.stop}/>
								</div>
								<span className="input-group-addon nobkg">hits per endpoint</span>
							</div>

						</form>
					</div>
				</div>

				<Modal ref="corporaModal" title={<span>Collections</span>}>
					<CorpusView corpora={this.state.corpora} languageMap={this.state.languageMap} />
				</Modal>

				<Modal ref="languageModal" title={<span>Select Language</span>}>
					<LanguageSelector anyLanguage={this.anyLanguage}
									  languageMap={this.state.languageMap}
									  selectedLanguage={this.state.language}
									  languageFilter={this.state.languageFilter}
									  languageChangeHandler={this.setLanguageAndFilter} />
				</Modal>

				<Modal ref="resultModal" title={this.renderZoomedResultTitle(this.state.zoomedCorpusHit)}>
					<ZoomedResult corpusHit={this.state.zoomedCorpusHit}
								  nextResults={this.nextResults}
								  getDownloadLink={this.getDownloadLink}
								  getToWeblichtLink={this.getToWeblichtLink}
								  searchedLanguage={this.state.language}
								  weblichtLanguages={this.state.weblichtLanguages}
								  languageMap={this.state.languageMap} />
				</Modal>

				<div className="top-gap">
					<Results collhits={this.filterResults()}
							 toggleResultModal={this.toggleResultModal}
							 getDownloadLink={this.getDownloadLink}
							 getToWeblichtLink={this.getToWeblichtLink}
							 searchedLanguage={this.state.language}/>
				</div>
			</div>
			);
	},
});



/////////////////////////////////

var LanguageSelector = React.createClass({
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
		return	<div key={lang[0]}>
					<a tabIndex="-1" href="#" style={style} onClick={this.selectLang.bind(this, lang)}>{desc}</a>
				</div>;
	},

	renderRadio: function(option) {
		return	this.props.languageFilter === option ?
				<input type="radio" name="filterOpts" value={option} checked onChange={this.setFilter.bind(this, option)}/>
				: <input type="radio" name="filterOpts" value={option} onChange={this.setFilter.bind(this, option)} />;
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

		return	<div>
					<div className="row">
						<div className="col-sm-4">{l1}</div>
						<div className="col-sm-4">{l2}</div>
						<div className="col-sm-4">{l3}</div>
						<div className="col-sm-12" style={{marginTop:10, marginBottom:10, borderBottom:"1px solid #eee"}}/>
					</div>
					<form className="form" role="form">
						<div className="input-group">
							<div>
							<label style={{color:'black'}}>
								{ this.renderRadio('byMeta') }{" "}
								Use the collections{"'"} specified language to filter results
							</label>
							</div>
							<div>
							<label style={{color:'black'}}>
								{ this.renderRadio('byGuess') }{" "}
								Filter results by using a language detector
							</label>
							</div>
							<div>
							<label style={{color:'black'}}>
								{ this.renderRadio('byMetaAndGuess') }{" "}
								First use the collections{"'"} specified language then also use a language detector
							</label>
							</div>
						</div>
					</form>
				</div>;
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
		return	<div className='inline'>
					<span className="corpusName"> {corpus.title}</span>
					<span className="institutionName"> — {corpus.institution.name}</span>
				</div>;
	},

	renderRowLanguage: function(hit) {
		return false; //<span style={{fontFace:"Courier",color:"black"}}>{hit.language} </span> ;
	},

	renderRowsAsHits: function(hit,i) {
		function renderTextFragments(tf, idx) {
			return <span key={idx} className={tf.hit?"keyword":""}>{tf.text}</span>;
		}
		return	<p key={i} className="hitrow">
					{this.renderRowLanguage(hit)}
					{hit.fragments.map(renderTextFragments)}
				</p>;
	},

	renderRowsAsKwic: function(hit,i) {
		var sleft={textAlign:"left", verticalAlign:"top", width:"50%"};
		var scenter={textAlign:"center", verticalAlign:"top", maxWidth:"50%"};
		var sright={textAlign:"right", verticalAlign:"top", maxWidth:"50%"};
		return	<tr key={i} className="hitrow">
					<td>{this.renderRowLanguage(hit)}</td>
					<td style={sright}>{hit.left}</td>
					<td style={scenter} className="keyword">{hit.keyword}</td>
					<td style={sleft}>{hit.right}</td>
				</tr>;
	},

	renderRowsAsAdv: function(hit,i) {
		var sleft={textAlign:"left", verticalAlign:"top", width:"50%"};
		var scenter={textAlign:"center", verticalAlign:"top", maxWidth:"50%"};
		var sright={textAlign:"right", verticalAlign:"top", maxWidth:"50%"};
		return	<tr key={i} className="hitrow">
					<td>{this.renderRowLanguage(hit)}</td>
					<td style={sright}>{hit.left}</td>
					<td style={scenter} className="keyword">{hit.keyword}</td>
					<td style={sleft}>{hit.right}</td>
				</tr>;
	},

	renderDiagnostic: function(d, key) {
		if (d.uri === NO_MORE_RECORDS_DIAGNOSTIC_URI) {
			return false;
		}
		return 	<div className="alert alert-warning" key={key}>
					<div>{d.message}</div>
				</div>;
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
			<div className="alert alert-danger" role="alert">
				<div>Exception: {xc.message}</div>
				{ xc.cause ? <div>Caused by: {xc.cause}</div> : false}
			</div>
		);
	},

	renderPanelBody: function(corpusHit) {
		var fulllength = {width:"100%"};
		if (this.state.displayKwic) {
			return 	<div>
						{this.renderErrors(corpusHit)}
						{this.renderDiagnostics(corpusHit)}
						<table className="table table-condensed table-hover" style={fulllength}>
							<tbody>{corpusHit.kwics.map(this.renderRowsAsKwic)}</tbody>
						</table>
					</div>;
		} else {
			return	<div>
						{this.renderErrors(corpusHit)}
						{this.renderDiagnostics(corpusHit)}
						{corpusHit.kwics.map(this.renderRowsAsHits)}
					</div>;
		}
	},

	renderDisplayKWIC: function() {
		return 	<div className="inline btn-group" style={{display:"inline-block"}}>
					<label forHtml="inputKwic" className="btn btn-flat">
						{ this.state.displayKwic ?
							<input id="inputKwic" type="checkbox" value="kwic" checked onChange={this.toggleKwic} /> :
							<input id="inputKwic" type="checkbox" value="kwic" onChange={this.toggleKwic} />
						}
						&nbsp;
						Display as Key Word In Context
					</label>
				</div>;
	},

	renderDisplayADV: function() {
		return 	<div className="inline btn-group" style={{display:"inline-block"}}>
					<label forHtml="inputKwic" className="btn btn-flat">
						{ this.state.displayKwic ?
							<input id="inputKwic" type="checkbox" value="kwic" checked onChange={this.toggleKwic} /> :
							<input id="inputKwic" type="checkbox" value="kwic" onChange={this.toggleKwic} />
						}
						&nbsp;
						Display as AdvancedDataView
					</label>
				</div>;
	},

	renderDownloadLinks: function(corpusId) {
		return (
			<div className="dropdown">
				<button className="btn btn-flat" aria-expanded="false" data-toggle="dropdown">
					<span className="glyphicon glyphicon-download-alt" aria-hidden="true"/>
					{" "} Download {" "}
					<span className="caret"/>
				</button>
				<ul className="dropdown-menu">
					<li> <a href={this.props.getDownloadLink(corpusId, "csv")}>
							{" "} As CSV file</a></li>
					<li> <a href={this.props.getDownloadLink(corpusId, "excel")}>
							{" "} As Excel file</a></li>
					<li> <a href={this.props.getDownloadLink(corpusId, "tcf")}>
							{" "} As TCF file</a></li>
					<li> <a href={this.props.getDownloadLink(corpusId, "text")}>
							{" "} As Plain Text file</a></li>
				</ul>
			</div>
		);
	},

	renderToWeblichtLinks: function(corpusId, forceLanguage, error) {
		return (
			<div className="dropdown">
				<button className="btn btn-flat" aria-expanded="false" data-toggle="dropdown">
					<span className="glyphicon glyphicon-export" aria-hidden="true"/>
					{" "} Use Weblicht {" "}
					<span className="caret"/>
				</button>
				<ul className="dropdown-menu">
					<li>
						{error ?
							<div className="alert alert-danger" style={{margin:10, width:200}}>{error}</div> :
							<a href={this.props.getToWeblichtLink(corpusId, forceLanguage)} target="_blank">{" "}
								Send to Weblicht</a>
						}
					</li>
				</ul>
			</div>
		);
	},

};

var ZoomedResult = React.createClass({
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
			forceUpdate: 1, // hack to force an update, used when searching for next results
		};
	},

	nextResults: function(e) {
		this.props.corpusHit.inProgress = true;
		this.setState({forceUpdate: this.state.forceUpdate+1});
		this.props.nextResults(this.props.corpusHit.corpus.id);
	},

	renderLanguages: function(languages) {
		return languages
				.map(function(l) { return this.props.languageMap[l]; }.bind(this))
				.sort()
				.join(", ");
	},

	renderMoreResults:function(){
		if (this.props.corpusHit.inProgress)
			return <span style={{fontStyle:'italic'}}>Retrieving results, please wait...</span>;

		var moreResults = true;
		for (var i = 0; i < this.props.corpusHit.diagnostics.length; i++) {
			var d = this.props.corpusHit.diagnostics[i];
			if (d.uri === NO_MORE_RECORDS_DIAGNOSTIC_URI) {
				moreResults = false;
				break;
			}
		}
		if (!moreResults)
			return <span style={{fontStyle:'italic'}}>No other results available for this query</span>;
		return	<button className="btn btn-default" onClick={this.nextResults}>
					<span className="glyphicon glyphicon-option-horizontal" aria-hidden="true"/> More Results
				</button>;
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
		return 	<div>
					<ReactCSSTransitionGroup transitionName="fade">
						<div className='corpusDescription'>
							<p><i className="fa fa-institution"/> {corpus.institution.name}</p>
							{corpus.description ?
								<p><i className="glyphicon glyphicon-info-sign"/> {corpus.description}</p>: false}
							<p><i className="fa fa-language"/> {this.renderLanguages(corpus.languages)}</p>
						</div>
						<div style={{marginBottom:2}}>
							<div className="float-right">
								<div>
									{ this.renderDisplayKWIC() }
									<div className="inline"> {this.renderDownloadLinks(corpusHit.corpus.id)} </div>
									<div className="inline"> {this.renderToWeblichtLinks(corpus.id, forceLanguage, wlerror)} </div>
								</div>
							</div>
							<div style={{clear:'both'}}/>
						</div>
						<div className="panel">
							<div className="panel-body corpusResults">{this.renderPanelBody(corpusHit)}</div>
						</div>

						<div style={{textAlign:'center', marginTop:10}}>
							{ this.renderMoreResults() }
						</div>

					</ReactCSSTransitionGroup>
				</div>;
	},
});

var Results = React.createClass({
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
		return	<div>
					{" "}
					<div style={inline}>
						<button className="btn btn-default zoomResultButton"
								onClick={function(e){this.props.toggleResultModal(e,corpusHit)}.bind(this)}>
								<span className="glyphicon glyphicon-eye-open"/> View
						</button>
					</div>
				</div>;
	},

	renderResultPanel: function(corpusHit) {
		if (corpusHit.kwics.length === 0 &&
			!corpusHit.exception &&
			corpusHit.diagnostics.length === 0) {
				return false;
		}
		return 	<Panel key={corpusHit.corpus.id}
						title={this.renderPanelTitle(corpusHit.corpus)}
						info={this.renderPanelInfo(corpusHit)}>
					{this.renderPanelBody(corpusHit)}
				</Panel>;
	},

	renderProgressMessage: function() {
		var collhits = this.props.collhits;
		var done = collhits.results.length - collhits.inProgress;
		var msg = collhits.hits + " matching collections found in " + done + " searched collections";
		var percents = Math.round(100 * collhits.hits / collhits.results.length);
		var styleperc = {width: percents+"%"};
		return 	<div style={{marginTop:10}}>
					<div>{msg}</div>
					{collhits.inProgress > 0 ?
						<div className="progress" style={{marginBottom:10}}>
							<div className="progress-bar progress-bar-striped active" role="progressbar"
								aria-valuenow={percents} aria-valuemin="0" aria-valuemax="100" style={styleperc} />
							{percents > 2 ? false :
								<div className="progress-bar progress-bar-striped active" role="progressbar"
									aria-valuenow='100' aria-valuemin="0" aria-valuemax="100"
									style={{width: '100%', backgroundColor:'#888'}} />
							}
						</div> :
						false}
				</div>;
	},

	render: function() {
		var collhits = this.props.collhits;
		if (!collhits.results) {
			return false;
		}
		var showprogress = collhits.inProgress > 0;
		return 	<div>
					<ReactCSSTransitionGroup transitionName="fade">
						{ showprogress ? this.renderProgressMessage() : <div style={{height:20}} />}
						<div style={{marginBottom:2}}>
							{ showprogress ? false :
								<div className="float-left"> {collhits.hits + " matching collections found"} </div>
							}
							{ collhits.hits === 0 ? false :
								<div className="float-right">
									<div>
										{ this.renderDisplayKWIC() }
										{ collhits.inProgress === 0 ?
											<div className="inline"> {this.renderDownloadLinks()} </div>
											:false
										}
									</div>
								</div>
							}
							<div style={{clear:'both'}}/>
						</div>
						{collhits.results.map(this.renderResultPanel)}
					</ReactCSSTransitionGroup>
				</div>;
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
