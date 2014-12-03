/** @jsx React.DOM */

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

var CorpusView = React.createClass({
	propTypes: {
		corpora: PT.object.isRequired,
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
			querytokens.forEach(function(qtoken){
				if (corpus.displayName && corpus.displayName.toLowerCase().indexOf(qtoken) >= 0) {
					corpus.priority ++;
					// console.log(corpus.displayName, "name ++");
				}
				if (corpus.description && corpus.description.toLowerCase().indexOf(qtoken) >= 0) {
					corpus.priority ++;
					// console.log(corpus.displayName, "desc ++");
				}
				if (corpus.institution && corpus.institution.name && 
						corpus.institution.name.toLowerCase().indexOf(qtoken) >= 0) {
					corpus.priority ++;
					// console.log(corpus.displayName, "inst ++");
				}
				if (corpus.languages){
					corpus.languages.forEach(function(lang){
						if (lang.toLowerCase().indexOf(qtoken) >= 0){
							corpus.priority ++;
							// console.log(corpus.displayName, "lang ++");
						}
					});
				}
			});
		});

		// ensure root corpora have nonnull priority
		this.props.corpora.recurse(function(corpus){
			if (corpus.subCorpora) {
				corpus.subCorpora.forEach(function(subcorpus){
					if (subcorpus.priority > 0 && corpus.priority === 0)
						corpus.priority ++;
				});
			}
		});

		// order (descending priority)
		var sortFn = function(a, b){
			if (b.priority === a.priority) {
				return b.index - a.index; // make it a stable sort
			}
			return b.priority - a.priority;
		};

		this.props.corpora.recurse(function(corpus){
			if (corpus.subCorpora)
				corpus.subCorpora.sort(sortFn);
		});
		this.props.corpora.corpora.sort(sortFn);

		// display
		this.props.corpora.update();
		// console.log("corpus search done", query);
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

	renderCorpus: function(level, corpus) {
		var indent = {marginLeft:level*50};
		var corpusContainerClass = "corpus-container "+(corpus.priority>0?"":"dimmed");
		return	<div className={corpusContainerClass} key={corpus.displayName}>
					<div className="row corpus">
						<div className="col-sm-1 vcenter" onClick={this.toggleSelection.bind(this,corpus)}>
							{this.renderCheckbox(corpus)}
						</div>
						<div className="col-sm-8 vcenter">
							<div style={indent}>
								<h3>{corpus.displayName} :{corpus.priority}</h3>
								<p>{corpus.description}</p>
								{this.renderExpansion(corpus)}
							</div>
						</div>
						<div className="col-sm-3 vcenter">
							<p><i className="fa fa-institution"/> {corpus.institution.name}</p>
							<p><i className="fa fa-language"/> {corpus.languages.join(" ")}</p>
						</div>
					</div>
					{corpus.expanded ? corpus.subCorpora.map(this.renderCorpus.bind(this,level+1)) : false}
				</div>;
	},

	render: function() {
		var rightspace = { marginRight: 20 };
		var lateralspace = { margin: "0 30px" };
		return	<div style={lateralspace}>
					<div className="row">
						<div className="float-right">
							<div className="inline" style={rightspace} >
								<SearchCorpusBox search={this.searchCorpus}/>
							</div>
							<button className="btn btn-default" style={rightspace} onClick={this.selectAll.bind(this,true)}>
								<span className="glyphicon glyphicon-check" aria-hidden="true"/>
								{" Select all"}</button>
							<button className="btn btn-default" style={rightspace} onClick={this.selectAll.bind(this,false)}>
								<span className="glyphicon glyphicon-unchecked" aria-hidden="true"/>
								{" Deselect all"}</button>
						</div>
					</div>
					{this.props.corpora.corpora.map(this.renderCorpus.bind(this,0))}
				</div>;
	}
});

/////////////////////////////////

if (!window.MyAggregator) {
	window.MyAggregator = {};
}
window.MyAggregator.CorpusView = CorpusView;
