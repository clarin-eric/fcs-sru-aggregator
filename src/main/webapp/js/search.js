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
						onKeyDown: this.handleKey}), 
					React.createElement("div", {className: "input-group-btn"}, 
						React.createElement("button", {className: "btn btn-default input-lg search", type: "submit", tabIndex: "2", onClick: this.search}, 
							React.createElement("i", {className: "glyphicon glyphicon-search"})
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
	},

	getInitialState: function () {
		return { displayKwic: false };
	},

	toggleKwic: function() {
		this.setState({displayKwic:!this.state.displayKwic});
	},

	renderRowsAsHits: function(hit,i) {
		function renderTextFragments(tf, idx) {
			return React.createElement("span", {key: idx, className: tf.hit?"keyword":""}, tf.text);
		}
		return	React.createElement("p", {key: i, className: "hitrow"}, 
					hit.fragments.map(renderTextFragments)
				);
	},

	renderRowsAsKwic: function(hit,i) {
		var sleft={textAlign:"left", verticalAlign:"middle", width:"50%"};
		var scenter={textAlign:"center", verticalAlign:"middle", maxWidth:"50%"};
		var sright={textAlign:"right", verticalAlign:"middle", maxWidth:"50%"};
		return	React.createElement("tr", {key: i, className: "hitrow"}, 
					React.createElement("td", {style: sright}, hit.left), 
					React.createElement("td", {style: scenter, className: "keyword"}, hit.keyword), 
					React.createElement("td", {style: sleft}, hit.right)
				);
	},

	renderPanelBody: function(corpusHit) {
		var fulllength = {width:"100%"};		
		if (this.state.displayKwic) {
			return 	React.createElement("table", {className: "table table-condensed table-hover", style: fulllength}, 
						React.createElement("tbody", null, corpusHit.kwics.map(this.renderRowsAsKwic))
					);
		} else {
			return	React.createElement("div", null, corpusHit.kwics.map(this.renderRowsAsHits));
		}
	},

	renderResultPanels: function(corpusHit) {
		if (corpusHit.kwics.length === 0) {
			return false;
		}
		return 	React.createElement(Panel, {corpus: corpusHit.corpus, key: corpusHit.corpus.displayName}, 
					this.renderPanelBody(corpusHit)
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

	renderPreMessage: function() {
		if (this.props.requests.length === 0)
			return false;
		return "Searching in " + this.props.requests.length + " collections...";
	},

	renderPostMessage: function() {
		if (this.props.results.length === 0)
			return false;
		var hits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length > 0; }).length;
		var total = this.props.results.length;
		return hits + " collections with results found out of " + total + " searched collections";
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
						React.createElement("div", {key: "-premessage-", style: margintop}, this.renderPreMessage(), " "), 
						React.createElement("div", {key: "-progress-", style: margintop}, this.renderProgressBar()), 
						React.createElement("div", {key: "-postmessage-", style: margintop}, this.renderPostMessage(), " ")
					)
				);
	}
});

if (!window.MyAggregator) {
	window.MyAggregator = {};
}
window.MyAggregator.LanguageSelection = LanguageSelection;
window.MyAggregator.HitNumber = HitNumber;
window.MyAggregator.SearchBox = SearchBox;
window.MyAggregator.Results = Results;
