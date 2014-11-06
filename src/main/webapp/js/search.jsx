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

	handleKey: function(event) {
    	if (event.keyCode==13) {
    		this.search();
    	}
	},

	search: function() {
		this.props.search(this.state.query);
	},

	render: function() {
		return 	<div className="input-group">
					<input name="query" type="text" className="form-control input-lg search" 
						value={this.state.query} placeholder="Search" tabIndex="1" 
						onChange={this.handleChange}
						onKeyDown={this.handleKey}
						></input>
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
		return	<div style={topline} key={corpus.displayName}>
					<div className="row">
						<div className="col-sm-2">{that.renderCheckbox(corpus.checked, corpus.displayName, toggle)}</div>
						<div className="col-sm-6">
							<p>{level} {corpus.description}</p>
						</div>
						<div className="col-sm-4">
							<p>	<span style={bold}>Institution:</span> <span style={spaced}>{corpus.institution.name}</span></p>
							<p>	<span style={bold}>Language:</span> <span style={spaced}>{corpus.languages}</span></p>
							<p>	<span>{corpus.numberOfRecords ? (corpus.numberOfRecords+" records") : ""}</span></p>
						</div>
					</div>
					{corpus.subCorpora.map(this.renderCorpus.bind(this,level+1))}
				</div>;
	},

	render: function() {
		return	<div className="container">
					<div className="row">
						<div className="col-sm-2"><h3>Collection</h3></div>
						<div className="col-sm-10"><h3>Description</h3></div>
					</div>
					{this.props.corpora.map(this.renderCorpus.bind(this,0))}
				</div>;
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

	renderResultPanels: function(corpusHit) {
		function renderRowsAsHits(hit,i) {
			function renderTextFragments(tf, idx) {
				return <span key={idx} className={tf.hit?"keyword label label-primary":""}>{tf.text}</span>;
			}
			return	<p key={i}>
						{hit.fragments.map(renderTextFragments)}
					</p>;
		}

		function renderRowsAsKwic(hit,i) {
			var sleft={textAlign:"left", verticalAlign:"middle", width:"50%"};
			var scenter={textAlign:"center", verticalAlign:"middle", maxWidth:"50%"};
			var sright={textAlign:"right", verticalAlign:"middle", maxWidth:"50%"};
			return	<tr key={i}>
						<td style={sright}>{hit.left}</td>
						<td style={scenter} className="keyword">{hit.keyword}</td>
						<td style={sleft}>{hit.right}</td>
					</tr>;
		}

		if (corpusHit.kwics.length === 0) {
			return false;
		}
		var fulllength = {width:"100%"};		
		var body = this.state.displayKwic ? 
			<table className="table table-condensed table-hover" style={fulllength}>
				<tbody>{corpusHit.kwics.map(renderRowsAsKwic)}</tbody>
			</table> :
			<div>{corpusHit.kwics.map(renderRowsAsHits)}</div>;
		return 	<Panel corpus={corpusHit.corpus} key={corpusHit.corpus.displayName}>
					{body}
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
			</div> : 
			<span />;
	},

	renderMessage: function() {
		var noHits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length === 0; });
		return noHits.length > 0 ? (noHits.length + " other collections did not return any results") : "";
	},

	renderKwicCheckbox: function() {
		var inline = {display:"inline-block"};
		return	<div className="row">
					<div className="col-sm-3 col-sm-offset-9">
						<div className="btn-group" style={inline}>
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
		var margintop = {marginTop:"10px"};
		var margin = {marginTop:"0", padding:"20px"};
		var inlinew = {display:"inline-block", margin:"0 5px 0 0", width:"240px;"};
		var right= {float:"right"};
		return 	<div> 
					{this.props.results.length > 0 ? this.renderKwicCheckbox() : false}
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
