/** @jsx React.DOM */

var PT = React.PropTypes;
var ReactCSSTransitionGroup = window.React.addons.CSSTransitionGroup;
// own components
var Panel = window.MyReact.Panel;


/////////////////////////////////

var LanguageSelection = React.createClass({
	propTypes: {
		languages: PT.array.isRequired,
	},

	render: function() {
		var options = this.props.languages.map(function(lang) {
			var desc = lang.name + " [" + lang.code + "]";
			return <option value={lang.code} key={lang.code}>{desc}</option>;
		});
		var style={width:"240px"};
		return	<div className="form-group">
					<select className="form-control" type="select" style={style}>
						<option value="ALL" key="ALL">All languages</option>
						{options}
					</select>
				</div>;
	}
});

/////////////////////////////////

var HitNumber = React.createClass({
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
			<div className="input-group"  style={fifty}>
				<input id="hits" type="number" className="input" name="maxResults" min="10" max="50" 
					value={this.props.numberOfResults} onChange={this.handleChange}></input>
			</div> );
	}
});

/////////////////////////////////

var SearchBox = React.createClass({
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

	search: function() {
		this.props.search(this.state.query);
	},

	render: function() {
		return 	<div className="input-group">
					<input name="query" type="text" className="form-control input-lg search" 
						value={this.state.query} placeholder="Search" tabIndex="1" 
						onChange={this.handleChange}></input>
					<div className="input-group-btn">
						<button className="btn btn-default input-lg search" type="submit" tabIndex="2" onClick={this.search}>
							<i className="glyphicon glyphicon-search"></i>
						</button>
					</div>
				</div>;
	}
});

/////////////////////////////////

var CorpusView = React.createClass({
	propTypes: {
		corpora: PT.array.isRequired,
	},

	renderCheckbox: function(checked, label, onChangeFn) {
		return	<div className="form-group">
					<div className="checkbox">
						{ checked ?
							<input type="checkbox" checked onChange={onChangeFn} /> : 
							<input type="checkbox" onChange={onChangeFn} />
						}
						<label>{label}</label>
					</div>
				</div>;
	},

	renderCorpora: function() {
		var that = this;
		return this.props.corpora.map(function(corpus) {
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
			return	<div className="row" style={topline} key={corpus.displayTerm}>
						<div className="col-sm-2">{that.renderCheckbox(corpus.checked, corpus.displayTerm, toggle)}</div>
						<div className="col-sm-6">
							<p>{corpus.description}</p>
						</div>
						<div className="col-sm-4">
							<p>	<span style={bold}>Institution:</span> <span style={spaced}>{corpus.institution.name}</span></p>
							<p>	<span style={bold}>Language:</span> <span style={spaced}>{corpus.languages}</span></p>
							<p>	<span>{corpus.numberOfRecords ? (corpus.numberOfRecords+" records") : ""}</span></p>
						</div>
					</div>;
		});
	},

	render: function() {
		return	<div className="container">
					<div className="row">
						<div className="col-sm-2"><h3>Collection</h3></div>
						<div className="col-sm-10"><h3>Description</h3></div>
					</div>
					{this.renderCorpora()}
				</div>;
	}
});

/////////////////////////////////

var Results = React.createClass({
	propTypes: {
		requests: PT.array.isRequired,
		results: PT.array.isRequired,
	},

	renderResultPanels: function(corpusHit) {
		function renderRows(hit,i) {
			function renderTextFragments(tf, idx) {
				return <span key={idx} className={tf.hit?"keyword":""}>{tf.text}</span>;
			}
			return	<p key={i}>
						{hit.fragments.map(renderTextFragments)}
					</p>;
		}

		console.log(corpusHit);
		if (corpusHit.kwics.length === 0) {
			return <span key={corpusHit.corpus.displayName}></span>;
		}
		return 	<Panel header={corpusHit.corpus.displayName} key={corpusHit.corpus.displayName}>
					{corpusHit.kwics.map(renderRows)}
				</Panel>;
	},

	renderProgressBar: function() {
		var percents = 100 * this.props.results.length / (this.props.requests.length + this.props.results.length);
		var sperc = Math.round(percents);
		var styleperc = {width: sperc+"%"};
		return this.props.requests.length > 0 ? 
			<div className="progress">
  				<div className="progress-bar progress-bar-striped active" role="progressbar" 
  					aria-valuenow={sperc} aria-valuemin="0" aria-valuemax="100" style={styleperc} />
			</div> : <span />;
	},

	renderMessage: function() {
		var noHits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length === 0; });
		return noHits.length > 0 ? (noHits.length + " other collections returned no results") : "";
	},

	render: function() {
		var margintop = {marginTop:"10px"};
		return 	<div> 
					<ReactCSSTransitionGroup transitionName="fade">
						{this.props.results.map(this.renderResultPanels)}
						<div key="-message-" style={margintop}>{this.renderMessage()} </div>
						<div key="-progress-" style={margintop}>{this.renderProgressBar()}</div>
					</ReactCSSTransitionGroup>
				</div>;
	}
});

window.MyAggregator = {
	CorpusView: CorpusView,
	LanguageSelection: LanguageSelection,
	HitNumber: HitNumber,
	SearchBox: SearchBox,
	Results: Results,
};
