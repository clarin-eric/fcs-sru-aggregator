/** @jsx React.DOM */

var PT = React.PropTypes;
var ReactCSSTransitionGroup = window.React.addons.CSSTransitionGroup;
// own components
var Panel = window.MyReact.Panel;


/////////////////////////////////

var LanguageSelection = React.createClass({displayName: 'LanguageSelection',
	propTypes: {
		languages: PT.array.isRequired,
	},

	render: function() {
		var options = this.props.languages.map(function(lang) {
			var desc = lang.name + " [" + lang.code + "]";
			return React.createElement("option", {value: lang.code, key: lang.code}, desc);
		});
		var style={width:"240px"};
		return	React.createElement("div", {className: "form-group"}, 
					React.createElement("select", {className: "form-control", type: "select", style: style}, 
						React.createElement("option", {value: "ALL", key: "ALL"}, "All languages"), 
						options
					)
				);
	}
});

/////////////////////////////////

var HitNumber = React.createClass({displayName: 'HitNumber',
	propTypes: {
		onChange: PT.func.isRequired,
		numberOfResults: PT.number.isRequired,
	},

	handleChange: function(event) {
		this.props.onChange(event.target.value);
	},

	render: function() {
		var fifty = {width:"50px"};
		return (
			React.createElement("div", {className: "input-group", style: fifty}, 
				React.createElement("input", {id: "hits", type: "number", className: "input", name: "maxResults", min: "10", max: "50", 
					value: this.props.numberOfResults, onChange: this.handleChange})
			) );
	}
});

/////////////////////////////////

var SearchBox = React.createClass({displayName: 'SearchBox',
	propTypes: {
		search: PT.func.isRequired,
	},

	getInitialState: function () {
		return {
			query: ""
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
		return 	React.createElement("div", {className: "input-group"}, 
					React.createElement("input", {name: "query", type: "text", className: "form-control input-lg search", 
						value: this.state.query, placeholder: "Search", tabIndex: "1", 
						onChange: this.handleChange, 
						onKeyDown: this.handleKey
						}), 
					React.createElement("div", {className: "input-group-btn"}, 
						React.createElement("button", {className: "btn btn-default input-lg search", type: "submit", tabIndex: "2", onClick: this.search}, 
							React.createElement("i", {className: "glyphicon glyphicon-search"})
						)
					)
				);
	}
});

/////////////////////////////////

var CorpusView = React.createClass({displayName: 'CorpusView',
	propTypes: {
		corpora: PT.array.isRequired,
	},

	renderCheckbox: function(checked, label, onChangeFn) {
		return	React.createElement("div", {className: "form-group"}, 
					React.createElement("div", {className: "checkbox"}, 
						 checked ?
							React.createElement("input", {type: "checkbox", checked: true, onChange: onChangeFn}) : 
							React.createElement("input", {type: "checkbox", onChange: onChangeFn}), 
						
						React.createElement("label", null, label)
					)
				);
	},

	renderCorpus: function(level, corpus) {
		var that = this;

		if (corpus.subCorpora.length > 0) {
			console.log("big corpus: ", corpus);
		}
		function toggle() {
			corpus.checked = !corpus.checked;
			that.setState({corpora : corpora});
		}
		var bold = {fontWeight:"bold"};
		var spaced = {marginRight:"20px"};
		var topline = {borderTop:"1px solid #ddd", paddingTop:10};
		return	React.createElement("div", {style: topline, key: corpus.displayName}, 
					React.createElement("div", {className: "row"}, 
						React.createElement("div", {className: "col-sm-2"}, that.renderCheckbox(corpus.checked, corpus.displayName, toggle)), 
						React.createElement("div", {className: "col-sm-6"}, 
							React.createElement("p", null, level, " ", corpus.description)
						), 
						React.createElement("div", {className: "col-sm-4"}, 
							React.createElement("p", null, " ", React.createElement("span", {style: bold}, "Institution:"), " ", React.createElement("span", {style: spaced}, corpus.institution.name)), 
							React.createElement("p", null, " ", React.createElement("span", {style: bold}, "Language:"), " ", React.createElement("span", {style: spaced}, corpus.languages)), 
							React.createElement("p", null, " ", React.createElement("span", null, corpus.numberOfRecords ? (corpus.numberOfRecords+" records") : ""))
						)
					), 
					corpus.subCorpora.map(this.renderCorpus.bind(this,level+1))
				);
	},

	render: function() {
		return	React.createElement("div", {className: "container"}, 
					React.createElement("div", {className: "row"}, 
						React.createElement("div", {className: "col-sm-2"}, React.createElement("h3", null, "Collection")), 
						React.createElement("div", {className: "col-sm-10"}, React.createElement("h3", null, "Description"))
					), 
					this.props.corpora.map(this.renderCorpus.bind(this,0))
				);
	}
});

/////////////////////////////////

var Results = React.createClass({displayName: 'Results',
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

	renderResultPanels: function(corpusHit) {
		function renderRowsAsHits(hit,i) {
			function renderTextFragments(tf, idx) {
				return React.createElement("span", {key: idx, className: tf.hit?"keyword label label-primary":""}, tf.text);
			}
			return	React.createElement("p", {key: i}, 
						hit.fragments.map(renderTextFragments)
					);
		}

		function renderRowsAsKwic(hit,i) {
			var sleft={textAlign:"left", verticalAlign:"middle", width:"50%"};
			var scenter={textAlign:"center", verticalAlign:"middle", maxWidth:"50%"};
			var sright={textAlign:"right", verticalAlign:"middle", maxWidth:"50%"};
			return	React.createElement("tr", {key: i}, 
						React.createElement("td", {style: sright}, hit.left), 
						React.createElement("td", {style: scenter, className: "keyword"}, hit.keyword), 
						React.createElement("td", {style: sleft}, hit.right)
					);
		}

		if (corpusHit.kwics.length === 0) {
			return false;
		}
		var fulllength = {width:"100%"};		
		var body = this.state.displayKwic ? 
			React.createElement("table", {className: "table table-condensed table-hover", style: fulllength}, 
				React.createElement("tbody", null, corpusHit.kwics.map(renderRowsAsKwic))
			) :
			React.createElement("div", null, corpusHit.kwics.map(renderRowsAsHits));
		return 	React.createElement(Panel, {corpus: corpusHit.corpus, key: corpusHit.corpus.displayName}, 
					body
				);
	},

	renderProgressBar: function() {
		var percents = 100 * this.props.results.length / (this.props.requests.length + this.props.results.length);
		var sperc = Math.round(percents);
		var styleperc = {width: sperc+"%"};
		return this.props.requests.length > 0 ? 
			React.createElement("div", {className: "progress"}, 
  				React.createElement("div", {className: "progress-bar progress-bar-striped active", role: "progressbar", 
  					'aria-valuenow': sperc, 'aria-valuemin': "0", 'aria-valuemax': "100", style: styleperc})
			) : 
			React.createElement("span", null);
	},

	renderMessage: function() {
		var noHits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length === 0; });
		return noHits.length > 0 ? (noHits.length + " other collections did not return any results") : "";
	},

	renderKwicCheckbox: function() {
		var inline = {display:"inline-block"};
		return	React.createElement("div", {className: "row"}, 
					React.createElement("div", {className: "col-sm-3 col-sm-offset-9"}, 
						React.createElement("div", {className: "btn-group", style: inline}, 
							React.createElement("label", {forHtml: "inputKwic", className: "btn-default"}, 
								 this.state.displayKwic ? 
									React.createElement("input", {id: "inputKwic", type: "checkbox", value: "kwic", checked: true, onChange: this.toggleKwic}) :
									React.createElement("input", {id: "inputKwic", type: "checkbox", value: "kwic", onChange: this.toggleKwic}), 
								
								" " + ' ' +
								"Display as Key Word In Context"
							)
						)
					)
				);
	},

	render: function() {
		var margintop = {marginTop:"10px"};
		var margin = {marginTop:"0", padding:"20px"};
		var inlinew = {display:"inline-block", margin:"0 5px 0 0", width:"240px;"};
		var right= {float:"right"};
		return 	React.createElement("div", null, 
					this.props.results.length > 0 ? this.renderKwicCheckbox() : false, 
					React.createElement(ReactCSSTransitionGroup, {transitionName: "fade"}, 
						this.props.results.map(this.renderResultPanels), 
						React.createElement("div", {key: "-message-", style: margintop}, this.renderMessage(), " "), 
						React.createElement("div", {key: "-progress-", style: margintop}, this.renderProgressBar())
					)
				);
	}
});

window.MyAggregator = {
	CorpusView: CorpusView,
	LanguageSelection: LanguageSelection,
	HitNumber: HitNumber,
	SearchBox: SearchBox,
	Results: Results,
};
