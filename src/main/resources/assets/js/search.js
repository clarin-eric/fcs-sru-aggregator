/** @jsx React.DOM */
(function() {
"use strict";

var React = window.React;
var PT = React.PropTypes;
var ReactCSSTransitionGroup = window.React.addons.CSSTransitionGroup;
// own components
var InfoPopover = window.MyReact.InfoPopover;
var Panel = window.MyReact.Panel;


/////////////////////////////////

var SearchBox = React.createClass({displayName: 'SearchBox',
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
		return 	React.createElement("input", {className: "form-control input-lg search", 
					name: "query", 
					type: "text", 
					value: this.state.query, 
					placeholder: this.props.placeholder, 
					tabIndex: "1", 
					onChange: this.handleChange, 
					onKeyDown: this.handleKey})  ;
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

	renderPanelTitle: function(corpus) {
		var inline = {display:"inline-block"};
		return	React.createElement("div", {style: inline}, 
					React.createElement("span", {className: "corpusName"}, " ", corpus.title ? corpus.title : corpus.displayName), 
					React.createElement("span", {className: "institutionName"}, " — ", corpus.institution.name)
				);
	},

	renderPanelInfo: function(corpus) {
		var inline = {display:"inline-block"};
		return	React.createElement("div", null, 
					React.createElement(InfoPopover, {placement: "left", 
							title: corpus.title ? corpus.title : corpus.displayName}, 
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
		return 	React.createElement(Panel, {key: corpusHit.corpus.displayName, 
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
		return	React.createElement("div", {key: "-option-KWIC-", className: "row"}, 
					React.createElement("div", {className: "float-right", style: {marginRight:17}}, 
						React.createElement("div", {className: "btn-group", style: {display:"inline-block"}}, 
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
						hits > 0 ? this.renderKwicCheckbox() : false, 
						this.props.results.map(this.renderResultPanels)
					)
				);
	}
});

if (!window.MyAggregator) {
	window.MyAggregator = {};
}
window.MyAggregator.SearchBox = SearchBox;
window.MyAggregator.Results = Results;
})();
