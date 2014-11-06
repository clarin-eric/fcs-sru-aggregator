/** @jsx React.DOM */

var PT = React.PropTypes;

var SearchBox = window.MyAggregator.SearchBox;
var CorpusSelection = window.MyAggregator.CorpusSelection;
var LanguageSelection = window.MyAggregator.LanguageSelection;
var HitNumber = window.MyAggregator.HitNumber;
var Results = window.MyAggregator.Results;

var globals = {};

var Main = React.createClass({
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
			showCorpora: false,
		};
	},

	refreshCorpora: function() {
		var that = this;
		jQuery.ajax({
			url: 'rest/corpora',
			success: function(json, textStatus, jqXHR) {
				that.setState({corpora:json});
				console.log("corpora", json);
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
				globals.timeout = 250;
				setTimeout(that.refreshSearchResults, globals.timeout);
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
		var that = this;
		jQuery.ajax({
			url: 'rest/search/'+that.state.searchId,
			success: function(json, textStatus, jqXHR) {
				if (json.requests.length === 0) {
					console.log("search ended");
				} else {
					globals.timeout = 1.5 * globals.timeout;
					setTimeout(that.refreshSearchResults, globals.timeout);
					// console.log("new search in: " + globals.timeout+ "ms");
				}
				that.setState({hits:json});
				console.log("hits:", json);
			},
			error: function(jqXHR, textStatus, error) {
				console.log("search result err", jqXHR, textStatus, error);
			},
		});
	},

	toggleCorpusSelection: function(e) {
		this.setState({showCorpora:!this.state.showCorpora});
		e.preventDefault();
		e.stopPropagation();
	},

	renderCorpusSelection: function() {
		var style={width:"240px"};
		return	<button type="button" className="btn btn-default" style={style} onClick={this.toggleCorpusSelection}>
					All available corpora<span className="caret"></span>
				</button>;
	},

	render: function() {
		var margin = {marginTop:"0", padding:"20px"};
		var inline = {display:"inline-block", margin:"0 5px 0 0"};
		var inlinew = {display:"inline-block", margin:"0 5px 0 0", width:"240px;"};
		return	<div>
					<div className="center-block top-gap">
						<SearchBox search={this.search} />
					</div>
					<div className="center-block aligncenter">
						<div style={margin}>
							<form className="form-inline" role="form">
								<label className="muted">search in </label>
								<div id="corpusSelection" style={inlinew}>
									{this.renderCorpusSelection()}
								</div>
								<label className="muted"> for results in </label>
								<div id="languageSelection" style={inlinew}>
									<LanguageSelection languages={this.state.languages} />
								</div>
								<label className="muted"> and show maximum </label>
								<div style={inline}>
									<HitNumber onChange={this.setNumberOfResults} numberOfResults={this.state.numberOfResults} />
								</div>
								<label className="muted"> hits</label>
							</form>
						</div>
					</div>

					<div className="top-gap">
						{ this.state.showCorpora ? 
							<CorpusView corpora={this.state.corpora} /> :
							<Results requests={this.state.hits.requests} results={this.state.hits.results} />
						}
					</div>
				</div> ;
	}
});

(function() {
	var container = React.render(<Main />, document.getElementById('reactMain') );
	container.refreshCorpora();
	container.refreshLanguages();
})();
