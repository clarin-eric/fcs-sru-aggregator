/** @jsx React.DOM */

var PT = React.PropTypes;
// react bootstrap components
var RBAccordion = window.ReactBootstrap.Accordion;
var RBPanel = window.ReactBootstrap.Panel;
var RBDropdownButton = window.ReactBootstrap.DropdownButton;
var RBMenuItem = window.ReactBootstrap.MenuItem;
var RBNavbar = window.ReactBootstrap.Navbar;
var RBNav = window.ReactBootstrap.Nav;
var RBNavItem = window.ReactBootstrap.NavItem;
var RBInput = window.ReactBootstrap.Input;
var RBButton = window.ReactBootstrap.Button;
var RBModal = window.ReactBootstrap.Modal;
var RBModalTrigger = window.ReactBootstrap.ModalTrigger;
var RBOverlayMixin = window.ReactBootstrap.OverlayMixin;
var RBProgressBar = window.ReactBootstrap.ProgressBar;
// own components
var Panel = window.MyReact.Panel;

var CorpusSelectionModal = React.createClass({
	propTypes: {
		corpora: PT.array.isRequired,
	},

	getInitialState: function () {
		return {
			isModalOpen: true,
		};
	},

	hide: function() {
		this.setState({isModalOpen:false});
	},

	renderCorpora: function() {
		var that = this;
		return this.props.corpora.map(function(corpus) {
			var toggle = function() {
				corpus.checked = !corpus.checked;
				that.setState({corpora : corpora});
			};
			var input = corpus.checked ? 
				(<RBInput type="checkbox" checked label={corpus.displayName} onChange={toggle}/>) :
				(<RBInput type="checkbox" label={corpus.displayName} onChange={toggle}/>);
			var records = corpus.numberOfRecords ? (corpus.numberOfRecords+" records") : "";
			var bold = {fontWeight:"bold"};
			var spaced = {marginRight:"20px"};
			return	<tr>
						<td>{input}</td>
						<td><p>{corpus.description}</p>
							<p>	<span style={bold}>Institution:</span> <span style={spaced}>{corpus.institution.name}</span>
								<span style={bold}>Language:</span> <span style={spaced}>{corpus.languages}</span>
								<span>{records}</span>
							</p>
						</td>
					</tr>;
		});
	},

	render: function() {
		if (!this.state.isModalOpen) {
			return <span/>;
		}
		return this.transferPropsTo(
				<RBModal title="Collection selection" onRequestHide={this.hide}>
					<div className="modal-body">
						<div className="container">
							<form>
							<table className="table table-condensed table-striped table-responsive">
								<thead>
									<tr>
										<th>Collection</th>
										<th>Description</th>
									</tr>
								</thead>
								<tbody> {this.renderCorpora()} </tbody>
							</table>
							</form>
						</div>
					</div>
					<div className="modal-footer">
						<RBButton onClick={this.hide}>Close</RBButton>
					</div>
				</RBModal>
		);
	}
});
var CorpusSelection = React.createClass({
	propTypes: {
		corpora: PT.array.isRequired,
	},

	render: function() {
		var style={width:"240px"};
		return	<RBModalTrigger modal={<CorpusSelectionModal corpora={this.props.corpora}/>}>
					<RBButton style={style} onClick={this.handleClick}>
						All available corpora<span className="caret"></span>
					</RBButton>
				</RBModalTrigger>;
	}
});


var LanguageSelection = React.createClass({
	propTypes: {
		languages: PT.array.isRequired,
	},

	render: function() {
		var options = this.props.languages.map(function(lang) {
			var desc = lang.name + " [" + lang.code + "]";
			console.log(desc);
			return <option value={lang.code}>{desc}</option>;
		});
		var style={width:"240px"};
		return	<RBInput type="select" defaultValue="ALL" style={style}>
					<option value="ALL">All languages</option>
					{options}
				</RBInput>;
	}
});

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
					value={this.props.numberOfResults}></input>
			</div> );
	}
});

var Search = React.createClass({
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

var Results = React.createClass({
	propTypes: {
		requests: PT.array.isRequired,
		results: PT.array.isRequired,
	},

	render: function() {
		var fulllength = {width:"100%"};
		var margintop = {marginTop:"10px"};
		var sleft={textAlign:"left", verticalAlign:"middle", width:"50%"};
		var scenter={textAlign:"center", verticalAlign:"middle", maxWidth:"50%"};
		var sright={textAlign:"right", verticalAlign:"middle", maxWidth:"50%"};

		var resultPanels = this.props.results.map(function(corpusHit) {
			var rows = corpusHit.kwics.map(function(hit,i) {
				return	<tr key={i}>
							<td style={sright}>{hit.left}</td>
							<td style={scenter} className="keyword">{hit.keyword}</td>
							<td style={sleft}>{hit.right}</td>
						</tr>;
			});
			if (corpusHit.kwics.length === 0) {
				return <span></span>;
			}
			return 	<Panel header={corpusHit.corpus.displayName} key={corpusHit.corpus.displayName}>
						<table key="0" className="table table-condensed table-hover" style={fulllength}>
							<tbody>{rows}</tbody>
						</table>
					</Panel>;
		});
		var noHits = this.props.results.filter(function(corpusHit) { return corpusHit.kwics.length === 0; });
		var message = noHits.length > 0 ? (noHits.length + " other collections have been searched without success") : "";
		var percents = 100 * this.props.results.length / (this.props.requests.length + this.props.results.length);
		var progress = this.props.requests.length > 0 ? <RBProgressBar active now={percents} /> : <span />;
		return 	<div> 
					{resultPanels} 
					<div style={margintop}>{message} </div>
					<div style={margintop}>{progress}</div>
				</div>;
	}
});

var Container = React.createClass({
	getInitialState: function () {
		return {
			corpora:[],
			languages:[],
			hits: { 
				requests: [],
				results: [],
			},
			numberOfResults: 10,
			searchId: null,
			timer: null,
		};
	},

	refreshCorpora: function() {
		var that = this;
		jQuery.ajax({
			url: 'rest/corpora',
			success: function(json, textStatus, jqXHR) {
				that.setState({corpora:json});
			},
			error: function(jqXHR, textStatus, error) {
				console.log("corpora err", jqXHR, textStatus, error);
			},
		});
	},

	refreshLanguages: function() {
		var that = this;
		jQuery.ajax({
			url: 'rest/languages',
			success: function(json, textStatus, jqXHR) {
				that.setState({languages:json});
			},
			error: function(jqXHR, textStatus, error) {
				console.log("languages err", jqXHR, textStatus, error);
			},
		});
	},

	search: function(query) {
		var that = this;
		jQuery.ajax({
			url: 'rest/search',
			type: "POST",
			data: {
				query: query
			},
			success: function(searchId, textStatus, jqXHR) {
				console.log("search ["+query+"] ok: ", searchId);
				that.setState({searchId : searchId});
				that.setState({timerId : setInterval(that.refreshSearchResults, 1000)});
			},
			error: function(jqXHR, textStatus, error) {
				console.log("search ["+query+"] err: ", jqXHR, textStatus, error);
			},
		});
	},

	setNumberOfResults: function(x) {
		this.setState({numberOfResults:x});
	},

	refreshSearchResults: function() {
		console.log("refreshing search results");
		var that = this;
		jQuery.ajax({
			url: 'rest/search/'+that.state.searchId,
			success: function(json, textStatus, jqXHR) {
				console.log("search result ok:", json);
				if (json.requests.length === 0) {
					clearInterval(that.state.timerId);
					console.log("cleaned up timer");
				}
				that.setState({hits:json});
			},
			error: function(jqXHR, textStatus, error) {
				console.log("search result err", jqXHR, textStatus, error);
			},
		});
	},

	render: function() {
		var margin = {marginTop:"0", padding:"20px"};
		var inline = {display:"inline-block", margin:"0 5px 0 0"};
		var inlinew = {display:"inline-block", margin:"0 5px 0 0", width:"240px;"};
		return (
			<div>
				<div className="center-block top-gap">
					<Search search={this.search} />
				</div>
				<div className="center-block aligncenter">
					<div style={margin}>
					<form className="form-inline" role="form">
						<label htmlFor="dropdownCorpus" className="muted">search in </label>
						<div id="corpusSelection" style={inlinew}>
							<CorpusSelection corpora={this.state.corpora} />
						</div>
						<label htmlFor="dropdownLanguage" className="muted"> for results in </label>
						<div id="languageSelection" style={inlinew}>
							<LanguageSelection languages={this.state.languages} />
						</div>
						<label className="muted"> and show maximum </label>
						<div style={inline}>
							<HitNumber onChange={this.setNumberOfResults} numberOfResults={this.state.numberOfResults} />
						</div>
						<label htmlFor="hits" className="muted"> hits</label>
					</form>
					</div>
				</div>


				<div id="results" className="top-gap">
					<Results requests={this.state.hits.requests} results={this.state.hits.results} />
				</div>
			</div> );
	}
});

var container = React.renderComponent(<Container />, document.getElementById('reactMain') );
container.refreshCorpora();
setTimeout(container.refreshLanguages, 3000);
