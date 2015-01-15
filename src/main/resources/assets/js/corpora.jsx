/** @jsx React.DOM */
(function() {
"use strict";

window.MyAggregator = window.MyAggregator || {};

var PT = React.PropTypes;
var ReactCSSTransitionGroup = window.React.addons.CSSTransitionGroup;
// own components
var Panel = window.MyReact.Panel;


/////////////////////////////////

var SearchCorpusBox = React.createClass({
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
		return 	<div className="form-group">
					<input className="form-control search search-collection" type="text" 
						value={this.state.query} placeholder="Search for collection" 
						onChange={this.handleChange} />
				</div>;
	}
});

var CorpusView = window.MyAggregator.CorpusView = React.createClass({
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
		return	<button className="btn btn-default">
					{ corpus.selected ?
						<span className="glyphicon glyphicon-check" aria-hidden="true"/> :
						<span className="glyphicon glyphicon-unchecked" aria-hidden="true"/>
					}
				</button>;
	},

	renderExpansion: function(corpus) {
		if (!corpus.subCorpora || corpus.subCorpora.length === 0) {
			return false;
		}
		return 	<div className="expansion-handle" onClick={this.toggleExpansion.bind(this,corpus)}>
					<a> {corpus.expanded ?
							<span className="glyphicon glyphicon-collapse-down" aria-hidden="true"/>:
							<span className="glyphicon glyphicon-expand" aria-hidden="true"/>
						} 
						{corpus.expanded ? " Collapse ":" Expand "} {corpus.subCorpora.length} subcollections
					</a>
				</div>;
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
		return	<div className={corpusContainerClass} key={corpus.displayName}>
					<div className="row corpus">
						<div className="col-sm-1 vcenter" onClick={this.toggleSelection.bind(this,corpus)}>
							<div style={priorityStyle}>
								{this.renderCheckbox(corpus)}
							</div>
						</div>
						<div className="col-sm-8 vcenter">
							<div style={indent}>
								<h3>{corpus.title ? corpus.title : corpus.displayName} </h3>
								<p>{corpus.description}</p>
								{this.renderExpansion(corpus)}
							</div>
						</div>
						<div className="col-sm-3 vcenter">
							<p><i className="fa fa-institution"/> {corpus.institution.name}</p>
							<p><i className="fa fa-language"/> {this.renderLanguages(corpus.languages)}</p>
							{ corpus.landingPage ? 
								<p><i className="fa fa-home"/> <a href={corpus.landingPage}>{corpus.landingPage}</a></p> : 
								false }
						</div>
					</div>
					{corpus.expanded ? corpus.subCorpora.map(this.renderCorpus.bind(this, level+1, minmaxp)) : false}
				</div>;
	},

	render: function() {
		var minmaxp = this.getMinMaxPriority();
		return	<div style={{margin: "0 30px"}}>
					<div className="row">
						<div className="float-left inline">
							<h3 style={{marginTop:10}}>
								{this.props.corpora.getSelectedMessage()}
							</h3>
						</div>
						<div className="float-right inline">
							<div className="inline" style={{ marginRight: 20 }} >
								<SearchCorpusBox search={this.searchCorpus}/>
							</div>
							<button className="btn btn-default" style={{ marginRight: 10 }} onClick={this.selectAll.bind(this,true)}>
								{" Select all"}</button>
							<button className="btn btn-default" style={{ marginRight: 20 }} onClick={this.selectAll.bind(this,false)}>
								{" Deselect all"}</button>
						</div>
					</div>
					{this.props.corpora.corpora.map(this.renderCorpus.bind(this, 0, minmaxp))}
				</div>;
	}
});

})();
