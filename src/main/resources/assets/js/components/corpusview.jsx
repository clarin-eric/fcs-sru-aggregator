"use strict";
import classNames from "classnames";
import SearchCorpusBox from "./searchcorpusbox.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var CorpusView = createReactClass({
//fixme! - class CorpusView extends React.Component {
	propTypes: {
		corpora: PT.object.isRequired,
		languageMap: PT.object.isRequired,
	},
	
	getInitialState() {
		const corpora = this.props.corpora;
		const corporaGroupedByInstitute = this.updateCorporaGroupedByInstitute(corpora);
		const corporaGroupedByLanguage = this.updateCorporaGroupedByLanguage(corpora);

	    return {
	        viewSelected: false, // only show the selected collections
	        //showDisabled: false, // dont hide items with {visible = false} // implemented, but out commented feature...
	        viewGroupedByInstitution: false, // group by institution, then as usual
	        viewGroupedByLanguage: false, // group by (single) language, then as usual
	        corporaGroupedByInstitute: corporaGroupedByInstitute, // group info (is-expanded, corpora list)
	        corporaGroupedByLanguage: corporaGroupedByLanguage, // group info (with language as key) (is-expanded, corpora list)
	        corpora: corpora, // cached but unused, just to check for updates
	    }
	},

	componentWillReceiveProps(nextProps) {
		// console.debug("componentWillReceiveProps", nextProps);
		const corpora = nextProps.corpora;
		if (this.props.corpora == corpora) {
			return;
		}
		const corporaGroupedByInstitute = this.updateCorporaGroupedByInstitute(corpora);
		const corporaGroupedByLanguage = this.updateCorporaGroupedByLanguage(corpora);
		this.setState({
			corpora: this.props.corpora,
			corporaGroupedByInstitute: corporaGroupedByInstitute,
			corporaGroupedByLanguage: corporaGroupedByLanguage,
		});
	},

	updateCorporaGroupedByInstitute: function (corpora) {
		const corporaGroupedByInstitute = {};
		corpora.corpora.forEach(corpus => {
			const institute = corpus.institution.name;
			if (!corporaGroupedByInstitute.hasOwnProperty(institute)) {
				corporaGroupedByInstitute[institute] = {expanded: true, corpora: []};
			}
			corporaGroupedByInstitute[institute].corpora.push(corpus);
		});
		//console.debug(corporaGroupedByInstitute);
		return corporaGroupedByInstitute;
	},

	updateCorporaGroupedByLanguage: function (corpora) {
		const corporaGroupedByLanguage = {};
		corpora.corpora.forEach(corpus => {
			corpus.languages.forEach(language => {
				if (!corporaGroupedByLanguage.hasOwnProperty(language)) {
					corporaGroupedByLanguage[language] = {expanded: true, corpora: []};
				}
				corporaGroupedByLanguage[language].corpora.push(corpus);
			});
		});
		//console.debug(corporaGroupedByLanguage);
		return corporaGroupedByLanguage;
	},

	toggleSelection: function (corpus, e) {
		var s = !corpus.selected;
		this.props.corpora.recurseCorpus(corpus, function(c) { c.selected = s; });
		this.props.corpora.update();
		this.stop(e);
	},
	
	toggleViewSelected(evt) {
	    this.setState( (st)=> ({viewSelected:!st.viewSelected}) );
	},
	toggleShowDisabled(evt) {
	    this.setState( (st)=> ({showDisabled:!st.showDisabled}) );
	},
	toggleViewGroupByInstitution(evt) {
	    this.setState((st) => ({
			viewGroupedByInstitution: !st.viewGroupedByInstitution,
			viewGroupedByLanguage: false,
		}));
	},
	toggleViewGroupByLanguage(evt) {
		this.setState((st) => ({
			viewGroupedByInstitution: false,
			viewGroupedByLanguage: !st.viewGroupedByLanguage,
		}));
	},

	toggleDescExpansion: function (corpus) {
		corpus.descExpanded = !corpus.descExpanded;
		this.props.corpora.update();
	},
	
	toggleExpansion: function (corpus) {
		corpus.expanded = !corpus.expanded;
		this.props.corpora.update();
	},

	toggleExpansionGrouped: function (groupedCorpora) {
		groupedCorpora.expanded = !groupedCorpora.expanded;
		this.setState({
			corporaGroupedByInstitute: this.state.corporaGroupedByInstitute,
			corporaGroupedByLanguage: this.state.corporaGroupedByLanguage,
		});
	},

	selectAll: function(value) {
	    // select all _visible_
		this.props.corpora.recurse(function(c) { c.visible ? c.selected = value : false });
		this.props.corpora.update();
	},

	selectAllFromList: function (corpora, value) {
		// like selectAll(), just for list of corpora
		this.props.corpora.recurseCorpora(corpora, function (c) { c.visible ? c.selected = value : false });
		this.props.corpora.update();
	},

	selectAllShown: function (value) {
		// select only visible/shown corpora, i.e. corpora that are shown in dialog, possibly filtered due to query
		this.props.corpora.recurse(function (c) { c.visible && c.priority > 0 ? c.selected = value : false });
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
		return 	<div className="expansion-handle" onClick={this.toggleExpansion.bind(this, corpus)}>
					<a>
						{corpus.expanded ?
							<span className="glyphicon glyphicon-minus" aria-hidden="true"/>:
							<span className="glyphicon glyphicon-plus" aria-hidden="true"/>
						} 
						{corpus.expanded ? " Collapse ":" Expand "} ({corpus.subCorpora.length} subcollections)
					</a>
				</div>;
	},

	renderExpansionGrouped: function (groupedCorpora) {
		if (!groupedCorpora.corpora || groupedCorpora.corpora.length === 0) {
			return false;
		}

		var selectedCount = 0;
		this.props.corpora.recurseCorpora(groupedCorpora.corpora, c => {
			if (c.selected && c.visible) selectedCount++;
		});

		return <div className="expansion-handle" onClick={this.toggleExpansionGrouped.bind(this, groupedCorpora)}>
			<a>
				{groupedCorpora.expanded ?
					<span className="glyphicon glyphicon-minus" aria-hidden="true" /> :
					<span className="glyphicon glyphicon-plus" aria-hidden="true" />
				}
				{groupedCorpora.expanded ? " Collapse " : " Expand "} ({groupedCorpora.corpora.length} root collection{groupedCorpora.corpora.length != 1 ? "s" : ""}, {selectedCount} (sub)collections selected)
			</a>
		</div>;
	},

	renderSelectionButtonsGrouped: function (corpora) {
		return (<div className="float-right inline" style={{ paddingTop: "1.5em" }}>
			<button className="btn btn-default" style={{ marginRight: 10 }} onClick={this.selectAllFromList.bind(this, corpora, true)}>
				{" Select all"}</button>
			<button className="btn btn-default" style={{ marginRight: 20 }} onClick={this.selectAllFromList.bind(this, corpora, false)}>
				{" Deselect all"}</button>
		</div>);
	},

	renderLanguages: function(languages) {
		return languages
				.map(function(l) { return this.props.languageMap[l]; }.bind(this))
				.sort()
				.join(", ");
	},
	
	shouldShowItem(level, corpus) {
	    if (this.state.viewSelected && !corpus.selected) {
	        return false;
	    }
	    if (!this.state.showDisabled && !corpus.visible) {
	           return false;
	    }
        // normal search filter.
        if (level === 0 && corpus.priority <= 0) {
		    return false;
	    }
	    
	    return true;
	},

	renderFilteredMessage() {
		var total = 0;
		var visible = 0;
		this.props.corpora.recurse((corpus) => {
			if (corpus.visible || this.state.showDisabled) {
				total ++;
				if (this.shouldShowItem(0, corpus)) {
					visible++;
				}
			}
		});
		if (visible === total) {
			return false;
		}
		if (visible === 0) {
			return false; // we do have an "empty" message anyway
		}
		return 	<div> Showing {visible} out of {total} (sub)collections. </div>;
	},

	renderCorpus: function(level, minmaxp, corpus) {
		if (!this.shouldShowItem(level, corpus)) {
		    return;
		}

		var indent = {marginLeft:level*50};
		var corpusContainerClass = "corpus-container "+(corpus.priority>0?"":"dimmed");

		var hue = 120 * corpus.priority / minmaxp[1];
		var color = minmaxp[0] === minmaxp[1] ? 'transparent' : 'hsl('+hue+', 50%, 50%)';
		var priorityStyle = {paddingBottom: 4, paddingLeft: 2, borderBottom: '3px solid '+color };
		var expansive = corpus.descExpanded ? {overflow:'hidden'} 
			: {whiteSpace:'nowrap', overflow:'hidden', textOverflow: 'ellipsis'};
		return	<div className={corpusContainerClass} key={corpus.id}>
					<div className="row corpus" onClick={this.toggleDescExpansion.bind(this, corpus)}>
						<div className="col-sm-1 vcenter">
								<div className="inline" style={priorityStyle} onClick={this.toggleSelection.bind(this,corpus)}>
									{this.renderCheckbox(corpus)}
								</div>
						</div>
						<div className="col-sm-8 vcenter">
							<div style={indent}>
								<h3 style={expansive}> 
									{corpus.title}
									{ corpus.landingPage ? 
										<a href={corpus.landingPage} onClick={this.stop}>
											<span style={{fontSize:12}}> – Homepage </span>
											<i className="glyphicon glyphicon-home"/>
										</a>: false}
								</h3>


								<p style={expansive}>{corpus.description}</p>
								{this.renderExpansion(corpus)}
							</div>
						</div>
						<div className="col-sm-3 vcenter">
							<p style={expansive}>
								<i className="fa fa-institution"/> {corpus.institution.name}
							</p>
							<p style={expansive}>
								<i className="fa fa-language"/> {this.renderLanguages(corpus.languages)}
							</p>
						</div>
					</div>
					{corpus.expanded ? corpus.subCorpora.map(this.renderCorpus.bind(this, level+1, minmaxp)) : false}
				</div>;
	},
	
	renderCorpList() {
	    var minmaxp = this.getMinMaxPriority();
	    
	    const corpListRender = [];
	       
	    // this is so we get a non-undefined items .length in corpListRender.
	    this.props.corpora.corpora.forEach( c => {
	        var rend = this.renderCorpus(0, minmaxp, c);
	        if (rend) corpListRender.push(rend);
	   });
	   
	   return <div className="corpusview-corpora">
		    { corpListRender.length > 0 ? corpListRender :
                <h3 className="aligncenter">{
                    this.state.viewSelected ? "No collections selected yet!" : "No collections found."
                }</h3>
            }
		</div>
		
	},

	renderCorpListGroupedByInstitution() {
		var minmaxp = this.getMinMaxPriority();

		const groupedListRender = [];
		Object.entries(this.state.corporaGroupedByInstitute).forEach(([institution, groupedCorpora]) => {
			const corpListRender = [];
			// this is so we get a non-undefined items .length in corpListRender.
			groupedCorpora.corpora.forEach(c => {
				var rend = this.renderCorpus(0, minmaxp, c);
				if (rend) corpListRender.push(rend);
			});
			if (corpListRender.length > 0) {
				groupedListRender.push(<div className="corpusview-corpora">
					{this.renderSelectionButtonsGrouped(groupedCorpora.corpora)}
					<h3 style={{ paddingTop: "0.5em" }}><i class="fa fa-institution"/> {institution}</h3>
					{this.renderExpansionGrouped(groupedCorpora)}
					{groupedCorpora.expanded ? corpListRender : false}
				</div>);
			}
		});

		return <div className="corpusview-institutions">
			{groupedListRender.length > 0 ? groupedListRender :
				<h3 className="aligncenter">{
					this.state.viewSelected ? "No collections selected yet!" : "No collections found."
				}</h3>
			}
		</div>
	},

	renderCorpListGroupedByLanguage() {
		var minmaxp = this.getMinMaxPriority();

		const groupedListRender = [];
		Object.entries(this.state.corporaGroupedByLanguage).forEach(([language, groupedCorpora]) => {
			const corpListRender = [];
			// this is so we get a non-undefined items .length in corpListRender.
			groupedCorpora.corpora.forEach(c => {
				var rend = this.renderCorpus(0, minmaxp, c);
				if (rend) corpListRender.push(rend);
			});
			if (corpListRender.length > 0) {
				groupedListRender.push(<div className="corpusview-corpora">
					{this.renderSelectionButtonsGrouped(groupedCorpora.corpora)}
					<h3 style={{ paddingTop: "0.5em" }}><i class="fa fa-language"/> {this.props.languageMap[language]} [{language}]</h3>
					{this.renderExpansionGrouped(groupedCorpora)}
					{groupedCorpora.expanded ? corpListRender : false}
				</div>);
			}
		});

		return <div className="corpusview-languages">
			{groupedListRender.length > 0 ? groupedListRender :
				<h3 className="aligncenter">{
					this.state.viewSelected ? "No collections selected yet!" : "No collections found."
				}</h3>
			}
		</div>
	},
	
	render() {
	    var selectedCount = 0;
	    //var disabledCount = 0;
	    this.props.corpora.recurse( c => {
	        if (c.selected && c.visible) selectedCount++;
	        //if (c.selected) selectedCount++;
	        //if (!c.visible) disabledCount++;
	    });
	    
	    var renderCorporaFn = null;
	    if (this.state.viewGroupedByInstitution) {
	        renderCorporaFn = this.renderCorpListGroupedByInstitution;
	    } else if (this.state.viewGroupedByLanguage) {
	        renderCorporaFn = this.renderCorpListGroupedByLanguage;
	    } else {
	        renderCorporaFn = this.renderCorpList;
	    }

		return	<div style={{margin: "0 30px"}}>
					<div className="row">
					{/*
						<div className="float-left inline">
							<h3 style={{marginTop: 0}}>
								{this.props.corpora.getSelectedMessage()}
							</h3>
						</div>
					*/}
						
						<div className="float-left inline corpusview-filter-buttons">
						    <div className="btn-group btn-group-toggle" >
						    
                              <label className={"btn btn-light btn " + (this.state.viewSelected ? 'active':'inactive')} onClick={this.toggleViewSelected} title="View selected collections">
                                <span className={this.state.viewSelected ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} /> View selected ({selectedCount})
                              </label>
                              {/*
                              <label className={"btn btn-light btn-sm " + (this.state.showDisabled ? 'active':'inactive')} onClick={this.toggleShowDisabled} label="Toggle showing of collections disabled in this search mode">
                                <span className={this.state.showDisabled ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} />  Show disabled ({disabledCount})
                              </label>
                              */}
                              <label className={"btn btn-light btn"} style={{ paddingRight: "0ex", pointerEvents: "none" }}>Group by </label>
                              <label className={"btn btn-light btn " + (this.state.viewGroupedByInstitution ? 'active' : 'inactive')} style={{ paddingRight: "0.5ex", paddingLeft: "0.5ex" }} onClick={this.toggleViewGroupByInstitution} title="Group collections by institution">
                                <span className={this.state.viewGroupedByInstitution ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} /> Institution
                              </label>
                              <label className={"btn btn-light btn " + (this.state.viewGroupedByLanguage ? 'active' : 'inactive')} style={{ paddingLeft: "0.5ex" }} onClick={this.toggleViewGroupByLanguage} title="Group collections by language">
                                <span className={this.state.viewGroupedByLanguage ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} /> Language
                              </label>
                            </div>
						</div>
						
						<div className="float-right inline">
							<button className="btn btn-default" style={{ marginRight: 10 }} onClick={this.selectAll.bind(this,true)}>
								{" Select all"}</button>
							<button className="btn btn-default" style={{ marginRight: 10 }} onClick={this.selectAllShown.bind(this, true)}>
								{" Select visible"}</button>
							<button className="btn btn-default" style={{ marginRight: 20 }} onClick={this.selectAll.bind(this,false)}>
								{" Deselect all"}</button>
						</div>
						<div className="float-right inline">
							<div className="inline" style={{ marginRight: 20 }} >
								<SearchCorpusBox search={this.searchCorpus}/>
								{this.renderFilteredMessage()}
							</div>
						</div>
					</div>
					{renderCorporaFn()}
				</div>;
	}
});

module.exports = CorpusView;
