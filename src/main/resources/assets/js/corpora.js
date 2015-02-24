/** @jsx React.DOM */
(function() {
"use strict";

window.MyAggregator = window.MyAggregator || {};

var PT = React.PropTypes;
var ReactCSSTransitionGroup = window.React.addons.CSSTransitionGroup;
// own components
var Panel = window.MyReact.Panel;


/////////////////////////////////

var CorpusView = window.MyAggregator.CorpusView = React.createClass({displayName: 'CorpusView',
	propTypes: {
		corpora: PT.object.isRequired,
		languageMap: PT.object.isRequired,
	},

	toggleSelection: function (corpus, e) {
		var s = !corpus.selected;
		this.props.corpora.recurseCorpus(corpus, function(c) { c.selected = s; });
		this.props.corpora.update();
		this.stop(e);
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

		query = query.toLowerCase();
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
		var querytokens = query.split(" ").filter(function(x){ return x.length > 0; });
		this.props.corpora.recurse(function(corpus){
			var title = corpus.title;
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

		// ensure parents of visible corpora are also visible; maximum depth = 3
		var isVisibleFn = function(corpus){ return corpus.priority > 0; };
		var parentBooster = function(corpus){
			if (corpus.priority <= 0 && corpus.subCorpora) {
				if (corpus.subCorpora.some(isVisibleFn)) {
					corpus.priority = 0.5;
				}
			}
		};
		for (var i = 3; i > 0; i --) {
			this.props.corpora.recurse(parentBooster);
		}

		this.props.corpora.recurse(function(corpus) { corpus.subCorpora.sort(sortFn); });
		this.props.corpora.corpora.sort(sortFn);

		// display
		this.props.corpora.update();
	},

	stop: function(e) {
		e.stopPropagation();
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
		return 	React.createElement("div", {className: "expansion-handle", style: {}}, 
					React.createElement("a", null, 
						corpus.expanded ?
							React.createElement("span", {className: "glyphicon glyphicon-minus", 'aria-hidden': "true"}):
							React.createElement("span", {className: "glyphicon glyphicon-plus", 'aria-hidden': "true"}), 
						
						corpus.expanded ? " Collapse ":" Expand ", " (", corpus.subCorpora.length, " subcollections)"
					)
				);
	},

	renderLanguages: function(languages) {
		return languages
				.map(function(l) { return this.props.languageMap[l]; }.bind(this))
				.sort()
				.join(", ");
	},

	renderFilteredMessage: function() {
		var total = 0;
		var visible = 0;
		this.props.corpora.recurse(function(corpus){
			if (corpus.visible) {
				total ++;
				if (corpus.priority > 0) {
					visible++;
				}
			}
		});
		if (visible === total) {
			return false;
		}
		return 	React.createElement("div", null, " Showing ", visible, " out of ", total, " (sub)collections. ");
	},

	renderCorpus: function(level, minmaxp, corpus) {
		if (!corpus.visible || corpus.priority <= 0) {
			return false;
		}

		var indent = {marginLeft:level*50};
		var corpusContainerClass = "corpus-container "+(corpus.priority>0?"":"dimmed");

		var hue = 120 * corpus.priority / minmaxp[1];
		var color = minmaxp[0] === minmaxp[1] ? 'transparent' : 'hsl('+hue+', 50%, 50%)';
		var priorityStyle = {paddingBottom: 4, paddingLeft: 2, borderBottom: '3px solid '+color };
		var expansive = corpus.expanded ? {overflow:'hidden'} 
			: {whiteSpace:'nowrap', overflow:'hidden', textOverflow: 'ellipsis'};
		return	React.createElement("div", {className: corpusContainerClass, key: corpus.title}, 
					React.createElement("div", {className: "row corpus", onClick: this.toggleExpansion.bind(this, corpus)}, 
						React.createElement("div", {className: "col-sm-1 vcenter"}, 
								React.createElement("div", {className: "inline", style: priorityStyle, onClick: this.toggleSelection.bind(this,corpus)}, 
									this.renderCheckbox(corpus)
								)
						), 
						React.createElement("div", {className: "col-sm-8 vcenter"}, 
							React.createElement("div", {style: indent}, 
								React.createElement("h3", {style: expansive}, 
									corpus.title, 
									 corpus.landingPage ? 
										React.createElement("a", {href: corpus.landingPage, onClick: this.stop}, 
											React.createElement("span", {style: {fontSize:12}}, " – Homepage "), 
											React.createElement("i", {className: "glyphicon glyphicon-home"})
										): false
								), 


								React.createElement("p", {style: expansive}, corpus.description), 
								this.renderExpansion(corpus)
							)
						), 
						React.createElement("div", {className: "col-sm-3 vcenter"}, 
							React.createElement("p", {style: expansive}, 
								React.createElement("i", {className: "fa fa-institution"}), " ", corpus.institution.name
							), 
							React.createElement("p", {style: expansive}, 
								React.createElement("i", {className: "fa fa-language"}), " ", this.renderLanguages(corpus.languages)
							)
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
							React.createElement("button", {className: "btn btn-default", style: { marginRight: 10}, onClick: this.selectAll.bind(this,true)}, 
								" Select all"), 
							React.createElement("button", {className: "btn btn-default", style: { marginRight: 20}, onClick: this.selectAll.bind(this,false)}, 
								" Deselect all")
						), 
						React.createElement("div", {className: "float-right inline"}, 
							React.createElement("div", {className: "inline", style: { marginRight: 20}}, 
								React.createElement(SearchCorpusBox, {search: this.searchCorpus}), 
								this.renderFilteredMessage()
							)
						)
					), 
					
					this.props.corpora.corpora.map(this.renderCorpus.bind(this, 0, minmaxp))
				);
	}
});

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
		var query = event.target.value;
		this.setState({query: query});

		if (query.length === 0 || 2 <= query.length) {
			this.props.search(query);
		}
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

})();
