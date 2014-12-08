/** @jsx React.DOM */

var PT = React.PropTypes;
var ReactCSSTransitionGroup = window.React.addons.CSSTransitionGroup;
// own components
var Panel = window.MyReact.Panel;


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

	renderRowsAsHits: function(hit,i) {
		function renderTextFragments(tf, idx) {
			return <span key={idx} className={tf.hit?"keyword":""}>{tf.text}</span>;
		}
		return	<p key={i} className="hitrow">
					{hit.fragments.map(renderTextFragments)}
				</p>;
	},

	renderRowsAsKwic: function(hit,i) {
		var sleft={textAlign:"left", verticalAlign:"middle", width:"50%"};
		var scenter={textAlign:"center", verticalAlign:"middle", maxWidth:"50%"};
		var sright={textAlign:"right", verticalAlign:"middle", maxWidth:"50%"};
		return	<tr key={i} className="hitrow">
					<td style={sright}>{hit.left}</td>
					<td style={scenter} className="keyword">{hit.keyword}</td>
					<td style={sleft}>{hit.right}</td>
				</tr>;
	},

	renderPanelTitle: function(corpus) {
		var inline = {display:"inline-block"};
		return	<div style={inline}>
					<span className="corpusName"> {corpus.displayName}</span>
					<span className="institutionName"> — {corpus.institution.name}</span>
				</div>;
	},

	renderPanelInfo: function(corpus) {
		var inline = {display:"inline-block"};
		return	<div>
					<InfoPopover placement="left" title={corpus.displayName}>
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
			<div className="progress">
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

	renderFoundMessage: function() {
		if (this.props.results.length === 0)
			return false;
		var hits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length > 0; }).length;
		var total = this.props.results.length;
		return hits + " collections with results found out of " + total + " searched collections";
	},

	renderKwicCheckbox: function() {
		var inline = {display:"inline-block"};
		var marginright = {marginRight:17};
		return	<div key="-option-KWIC-" className="row">
					<div className="float-right" style={marginright}>
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
					<ReactCSSTransitionGroup transitionName="fade">
						<div key="-searching-message-" style={margintop}>{this.renderSearchingMessage()} </div>
						<div key="-found-message-" style={margintop}>{this.renderFoundMessage()} </div>
						<div key="-progress-" style={margintop}>{this.renderProgressBar()}</div>
						{this.props.results.length > 0 ? this.renderKwicCheckbox() : false}
						{this.props.results.map(this.renderResultPanels)}
					</ReactCSSTransitionGroup>
				</div>;
	}
});

if (!window.MyAggregator) {
	window.MyAggregator = {};
}
window.MyAggregator.SearchBox = SearchBox;
window.MyAggregator.Results = Results;
