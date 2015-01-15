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


var AggregatorPage = window.MyAggregator.AggregatorPage = React.createClass({
	propTypes: {
		ajax: PT.func.isRequired
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
			corpora: new Corpora([], this.updateCorpora),
			languageMap: {},
			language: this.anyLanguage,
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
					// console.log("search ended");
				}
				this.setState({hits:json});
				// console.log("hits:", json);
			}.bind(this),
		});
	},

	setLanguage: function(languageObj) {
		this.state.corpora.setVisibility(this.state.searchLayerId, languageObj[0]);
		this.setState({language: languageObj});
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

							<div className="input-group">
								
								<span className="input-group-addon nobkg" >Search for</span>
								
								<div className="input-group-btn">
									<button className="form-control btn btn-default" 
											aria-expanded="false" data-toggle="dropdown">
										{this.state.language[1]} <span className="caret"/>
									</button>
									<ul ref="languageDropdownMenu" className="dropdown-menu">
										<li key={this.anyLanguage[0]}> <a tabIndex="-1" href="#" 
												onClick={this.setLanguage.bind(this, this.anyLanguage)}>
											{this.anyLanguage[1]}</a>
										</li>
										{	_.pairs(this.state.languageMap).sort(function(l1, l2){
												return l1[1].localeCompare(l2[1]);
											}).map(function(l) {
												var desc = l[1] + " [" + l[0] + "]";
												return <li key={l[0]}> <a tabIndex="-1" href="#" 
													onClick={this.setLanguage.bind(this, l)}>{desc}</a></li>;
											}.bind(this))
										}
									</ul>
								</div>

								<div className="input-group-btn">
									<ul ref="layerDropdownMenu" className="dropdown-menu">
										{ 	layers.map(function(l) { 
												return <li key={l.id}> <a tabIndex="-1" href="#" 
													onClick={this.setLayer.bind(this, l.id)}> {l.name} </a></li>;
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
								<span className="input-group-addon nobkg">in</span>
									<button type="button" className="btn btn-default" onClick={this.toggleCorpusSelection}>
										{this.state.corpora.getSelectedMessage()} <span className="caret"/>
									</button>
							</div>							

							<div className="input-group">
								<span className="input-group-addon nobkg">and show up to</span>
								<div className="input-group-btn">
									<input type="number" className="form-control input" min="10" max="250" step="5"
										style={{width:54}}
										onChange={this.setNumberOfResults} value={this.state.numberOfResults} 
										onKeyPress={this.stop}/>
								</div>
								<span className="input-group-addon nobkg">hits</span>
							</div>
						</form>
					</div>
				</div>

	            <Modal ref="corporaModal" title="Collections">
					<CorpusView corpora={this.state.corpora} languageMap={this.state.languageMap} />
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



/////////////////////////////////

var SearchBox = React.createClass({
	propTypes: {
		search: PT.func.isRequired,
		placeholder: PT.string.isRequired,
	},

	getInitialState: function () {
		return {
			query: "",
		};
	},

	handleChange: function(event) {
    	this.setState({query: event.target.value});
	},

	handleKey: function(event) {
    	if (event.keyCode==13) {
    		this.search();
    	}
	},

	search: function() {
		this.props.search(this.state.query);
	},

	render: function() {
		return 	<input className="form-control input-lg search" 
					name="query" 
					type="text"
					value={this.state.query} 
					placeholder={this.props.placeholder} 
					tabIndex="1" 
					onChange={this.handleChange}
					onKeyDown={this.handleKey} />  ;
	}
});

/////////////////////////////////

var Results = React.createClass({
	propTypes: {
		requests: PT.array.isRequired,
		results: PT.array.isRequired,
	},

	getInitialState: function () {
		return { displayKwic: false };
	},

	toggleKwic: function() {
		this.setState({displayKwic:!this.state.displayKwic});
	},

	renderRowLanguage: function(hit) {
		return <span style={{fontFace:"Courier",color:"black"}}>{hit.language}</span>;
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
		var sleft={textAlign:"left", verticalAlign:"middle", width:"50%"};
		var scenter={textAlign:"center", verticalAlign:"middle", maxWidth:"50%"};
		var sright={textAlign:"right", verticalAlign:"middle", maxWidth:"50%"};
		return	<tr key={i} className="hitrow">
					<td>{this.renderRowLanguage(hit)}</td>
					<td style={sright}>{hit.left}</td>
					<td style={scenter} className="keyword">{hit.keyword}</td>
					<td style={sleft}>{hit.right}</td>
				</tr>;
	},

	renderPanelTitle: function(corpus) {
		var inline = {display:"inline-block"};
		return	<div style={inline}>
					<span className="corpusName"> {corpus.title ? corpus.title : corpus.displayName}</span>
					<span className="institutionName"> — {corpus.institution.name}</span>
				</div>;
	},

	renderPanelInfo: function(corpus) {
		var inline = {display:"inline-block"};
		return	<div>
					<InfoPopover placement="left" 
							title={corpus.title ? corpus.title : corpus.displayName}>
						<dl className="dl-horizontal">
							<dt>Institution</dt>
							<dd>{corpus.institution.name}</dd>

							{corpus.description ? <dt>Description</dt>:false}
							{corpus.description ? <dd>{corpus.description}</dd>: false}

							{corpus.landingPage ? <dt>Landing Page</dt> : false }
							{corpus.landingPage ? 
								<dd><a href={corpus.landingPage}>{corpus.landingPage}</a></dd>:
								false}

							<dt>Languages</dt>
							<dd>{corpus.languages.join(", ")}</dd>
						</dl>
					</InfoPopover>
					{" "}
					<div style={inline}>
						<button className="btn btn-default btn-xs" onClick={this.zoom}>
							<span className="glyphicon glyphicon-fullscreen"/>
						</button>
					</div>
				</div>;
	},

	renderPanelBody: function(corpusHit) {
		var fulllength = {width:"100%"};		
		if (this.state.displayKwic) {
			return 	<table className="table table-condensed table-hover" style={fulllength}>
						<tbody>{corpusHit.kwics.map(this.renderRowsAsKwic)}</tbody>
					</table>;
		} else {
			return	<div>{corpusHit.kwics.map(this.renderRowsAsHits)}</div>;
		}
	},

	renderResultPanels: function(corpusHit) {
		if (corpusHit.kwics.length === 0) {
			return false;
		}
		return 	<Panel key={corpusHit.corpus.displayName} 
						title={this.renderPanelTitle(corpusHit.corpus)} 
						info={this.renderPanelInfo(corpusHit.corpus)}>
					{this.renderPanelBody(corpusHit)}
				</Panel>;
	},

	renderProgressBar: function() {
		var percents = 100 * this.props.results.length / (this.props.requests.length + this.props.results.length);
		var sperc = Math.round(percents);
		var styleperc = {width: sperc+"%"};
		return this.props.requests.length > 0 ? 
			<div className="progress" style={{marginBottom:10}}>
  				<div className="progress-bar progress-bar-striped active" role="progressbar" 
  					aria-valuenow={sperc} aria-valuemin="0" aria-valuemax="100" style={styleperc} />
			</div> : 
			<span />;
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
		return	<div key="-option-KWIC-" className="row">
					<div className="float-right" style={{marginRight:17}}>
						<div className="btn-group" style={{display:"inline-block"}}>
							<label forHtml="inputKwic" className="btn-default">
								{ this.state.displayKwic ? 
									<input id="inputKwic" type="checkbox" value="kwic" checked onChange={this.toggleKwic} /> :
									<input id="inputKwic" type="checkbox" value="kwic" onChange={this.toggleKwic} />
								}
								&nbsp;
								Display as Key Word In Context
							</label>
						</div>
					</div>
				</div>;
	},

	render: function() {
		var hits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length > 0; }).length;
		var margintop = {marginTop:"10px"};
		var margin = {marginTop:"0", padding:"20px"};
		var inlinew = {display:"inline-block", margin:"0 5px 0 0", width:"240px;"};
		var right= {float:"right"};
		return 	<div> 
					<ReactCSSTransitionGroup transitionName="fade">
						<div key="-searching-message-" style={margintop}>{this.renderSearchingMessage()} </div>
						<div key="-found-message-" style={margintop}>{this.renderFoundMessage(hits)} </div>
						<div key="-progress-" style={margintop}>{this.renderProgressBar()}</div>
						{hits > 0 ? this.renderKwicCheckbox() : false}
						{this.props.results.map(this.renderResultPanels)}
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
};

})();
