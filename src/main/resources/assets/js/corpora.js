/** @jsx React.DOM */
(function() {
"use strict";

window.MyAggregator = window.MyAggregator || {};

var PT = React.PropTypes;
var ReactCSSTransitionGroup = window.React.addons.CSSTransitionGroup;
// own components
var Panel = window.MyReact.Panel;


/////////////////////////////////

var SearchCorpusBox = React.createClass({displayName: 'SearchCorpusBox',
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
		this.props.search(event.target.value);
		event.stopPropagation();
	},

	handleKey: function(event) {
		if (event.keyCode==13) {
			this.props.search(event.target.value);
		}
	},

	render: function() {
		return 	React.createElement("div", {className: "form-group"}, 
					React.createElement("input", {className: "form-control search search-collection", type: "text", 
						value: this.state.query, placeholder: "Search for collection", 
						onChange: this.handleChange})
				);
	}
});

var CorpusView = window.MyAggregator.CorpusView = React.createClass({displayName: 'CorpusView',
	propTypes: {
		corpora: PT.object.isRequired,
		languageMap: PT.object.isRequired,
	},

	toggleSelection: function (corpus) {
		var s = !corpus.selected;
		this.props.corpora.recurseCorpus(corpus, function(c) { c.selected = s; });
		this.props.corpora.update();
	},

	toggleExpansion: function (corpus) {
		corpus.expanded = !corpus.expanded;
		this.props.corpora.update();
	},

	selectAll: function(value) {
		this.props.corpora.recurse(function(c) { c.selected = value; });
		this.props.corpora.update();
	},

	searchCorpus: function(query) {
		// sort fn: descending priority, stable sort
		var sortFn = function(a, b){
			if (b.priority === a.priority) {
				return b.index - a.index; // stable sort
			}
			return b.priority - a.priority;
		};

		this.props.corpora.recurse(function(corpus) { corpus.subCorpora.sort(sortFn); });
		this.props.corpora.corpora.sort(sortFn);

		query = query.toLowerCase();
		var querytokens = query.split(" ");
		if (!query) {
			this.props.corpora.recurse(function(corpus) {corpus.priority = 1; });
			this.props.corpora.update();
			return;
		}

		// clean up all priorities
		this.props.corpora.recurse(function(corpus) {
			corpus.priority = 0;
		});

		// find priority for each corpus
		this.props.corpora.recurse(function(corpus){
			var title = corpus.title ? corpus.title : corpus.displayName;
			querytokens.forEach(function(qtoken){
				if (title && title.toLowerCase().indexOf(qtoken) >= 0) {
					corpus.priority ++;
				}
				if (corpus.description && corpus.description.toLowerCase().indexOf(qtoken) >= 0) {
					corpus.priority ++;
				}
				if (corpus.institution && corpus.institution.name && 
						corpus.institution.name.toLowerCase().indexOf(qtoken) >= 0) {
					corpus.priority ++;
				}
				if (corpus.languages){
					corpus.languages.forEach(function(lang){
						if (lang.toLowerCase().indexOf(qtoken) >= 0){
							corpus.priority ++;
						}
					});
					corpus.languages.forEach(function(lang){
						if (this.props.languageMap[lang].toLowerCase().indexOf(qtoken) >= 0){
							corpus.priority ++;
						}
					}.bind(this));
				}
			}.bind(this));
		}.bind(this));

		// ensure root corpora have nonnull priority
		this.props.corpora.recurse(function(corpus){
			if (corpus.subCorpora) {
				corpus.subCorpora.forEach(function(subcorpus){
					if (subcorpus.priority > 0 && corpus.priority === 0)
						corpus.priority ++;
				});
			}
		});

		this.props.corpora.recurse(function(corpus) { corpus.subCorpora.sort(sortFn); });
		this.props.corpora.corpora.sort(sortFn);

		// display
		this.props.corpora.update();
		// console.log("corpus search done", query);
	},

	getMinMaxPriority: function() {
		var min = 1, max = 0;
		this.props.corpora.recurse(function(c) { 
			if (c.priority < min) min = c.priority;
			if (max < c.priority) max = c.priority;
		});
		return [min, max];
	},

	renderCheckbox: function(corpus) {
		return	React.createElement("button", {className: "btn btn-default"}, 
					 corpus.selected ?
						React.createElement("span", {className: "glyphicon glyphicon-check", 'aria-hidden': "true"}) :
						React.createElement("span", {className: "glyphicon glyphicon-unchecked", 'aria-hidden': "true"})
					
				);
	},

	renderExpansion: function(corpus) {
		if (!corpus.subCorpora || corpus.subCorpora.length === 0) {
			return false;
		}
		return 	React.createElement("div", {className: "expansion-handle", onClick: this.toggleExpansion.bind(this,corpus)}, 
					React.createElement("a", null, " ", corpus.expanded ?
							React.createElement("span", {className: "glyphicon glyphicon-collapse-down", 'aria-hidden': "true"}):
							React.createElement("span", {className: "glyphicon glyphicon-expand", 'aria-hidden': "true"}), 
						
						corpus.expanded ? " Collapse ":" Expand ", " ", corpus.subCorpora.length, " subcollections"
					)
				);
	},

	renderLanguages: function(languages) {
		return languages
				.map(function(l) { return this.props.languageMap[l]; }.bind(this))
				.sort()
				.join(" ");
	},

	renderCorpus: function(level, minmaxp, corpus) {
		if (!corpus.visible) {
			return false;
		}

		var indent = {marginLeft:level*50};
		var corpusContainerClass = "corpus-container "+(corpus.priority>0?"":"dimmed");

		var hue = 80 * corpus.priority / minmaxp[1];
		if (corpus.priority > 0) { hue += 40; }
		var color = minmaxp[0] === minmaxp[1] ? 'transparent' : 'hsl('+hue+', 50%, 50%)';
		var priorityStyle = {paddingBottom: 4, paddingLeft: 2, borderBottom: '2px solid '+color };
		return	React.createElement("div", {className: corpusContainerClass, key: corpus.displayName}, 
					React.createElement("div", {className: "row corpus"}, 
						React.createElement("div", {className: "col-sm-1 vcenter", onClick: this.toggleSelection.bind(this,corpus)}, 
							React.createElement("div", {style: priorityStyle}, 
								this.renderCheckbox(corpus)
							)
						), 
						React.createElement("div", {className: "col-sm-8 vcenter"}, 
							React.createElement("div", {style: indent}, 
								React.createElement("h3", null, corpus.title ? corpus.title : corpus.displayName, " "), 
								React.createElement("p", null, corpus.description), 
								this.renderExpansion(corpus)
							)
						), 
						React.createElement("div", {className: "col-sm-3 vcenter"}, 
							React.createElement("p", null, React.createElement("i", {className: "fa fa-institution"}), " ", corpus.institution.name), 
							React.createElement("p", null, React.createElement("i", {className: "fa fa-language"}), " ", this.renderLanguages(corpus.languages)), 
							 corpus.landingPage ? 
								React.createElement("p", null, React.createElement("i", {className: "fa fa-home"}), " ", React.createElement("a", {href: corpus.landingPage}, corpus.landingPage)) : 
								false
						)
					), 
					corpus.expanded ? corpus.subCorpora.map(this.renderCorpus.bind(this, level+1, minmaxp)) : false
				);
	},

	render: function() {
		var minmaxp = this.getMinMaxPriority();
		return	React.createElement("div", {style: {margin: "0 30px"}}, 
					React.createElement("div", {className: "row"}, 
						React.createElement("div", {className: "float-left inline"}, 
							React.createElement("h3", {style: {marginTop:10}}, 
								this.props.corpora.getSelectedMessage()
							)
						), 
						React.createElement("div", {className: "float-right inline"}, 
							React.createElement("div", {className: "inline", style: { marginRight: 20}}, 
								React.createElement(SearchCorpusBox, {search: this.searchCorpus})
							), 
							React.createElement("button", {className: "btn btn-default", style: { marginRight: 10}, onClick: this.selectAll.bind(this,true)}, 
								" Select all"), 
							React.createElement("button", {className: "btn btn-default", style: { marginRight: 20}, onClick: this.selectAll.bind(this,false)}, 
								" Deselect all")
						)
					), 
					this.props.corpora.corpora.map(this.renderCorpus.bind(this, 0, minmaxp))
				);
	}
});

})();
