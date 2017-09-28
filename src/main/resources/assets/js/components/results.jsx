"use strict";
import classNames from "classnames";
import ResultMixin from "./resultmixin.jsx";
import Panel from "./panel.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";
import {CSSTransition, TransitionGroup} from "react-transition-group";

var PT = PropTypes;

var Results = createReactClass({
	propTypes: {
		collhits: PT.object.isRequired,
		searchedLanguage: PT.array.isRequired,
		toggleResultModal: PT.func.isRequired,
		getDownloadLink: PT.func.isRequired,
		getToWeblichtLink: PT.func.isRequired,
	        queryTypeId: PT.string.isRequired, 
	},
	mixins: [ResultMixin],

	renderPanelInfo: function(corpusHit) {
		var corpus = corpusHit.corpus;
		var inline = {display:"inline-block"};
		return	(<div>
					{" "}
					<div style={inline}>
						<button className="btn btn-default zoomResultButton"
								onClick={function(e){this.props.toggleResultModal(e,corpusHit)}.bind(this)}>
								<span className="glyphicon glyphicon-eye-open"/> View
						</button>
					</div>
				</div>);
	},

	renderResultPanel: function(corpusHit) {
		if (corpusHit.kwics.length === 0 &&
			!corpusHit.exception &&
			corpusHit.diagnostics.length === 0) {
				return false;
		}
		return 	(<CSSTransition key={corpusHit.corpus.id} classNames="fade" timeout={{enter: 200, exit: 200}}><Panel key={corpusHit.corpus.id}
						title={this.renderPanelTitle(corpusHit.corpus)}
						info={this.renderPanelInfo(corpusHit)}>
					{this.renderPanelBody(corpusHit)}
				</Panel></CSSTransition>);
	},

	renderProgressMessage: function() {
		var collhits = this.props.collhits;
		var done = collhits.results.length - collhits.inProgress;
		var msg = collhits.hits + " matching collections found in " + done + " searched collections";
		var percents = Math.round(100 * collhits.hits / collhits.results.length);
		var styleperc = {width: percents+"%"};
		return 	(<div style={{marginTop:10}}>
					<div>{msg}</div>
					{collhits.inProgress > 0 ?
						<div className="progress" style={{marginBottom:10}}>
							<div className="progress-bar progress-bar-striped active" role="progressbar"
								aria-valuenow={percents} aria-valuemin="0" aria-valuemax="100" style={styleperc} />
							{percents > 2 ? false :
								<div className="progress-bar progress-bar-striped active" role="progressbar"
									aria-valuenow='100' aria-valuemin="0" aria-valuemax="100"
									style={{width: '100%', backgroundColor:'#888'}} />
							}
						</div> :
						false}
				</div>);
	},

	render: function() {
		var collhits = this.props.collhits;
		if (!collhits.results) {
			return false;
		}
		var showprogress = collhits.inProgress > 0;
	     
		return 	(<div>
				{ showprogress ? this.renderProgressMessage() : <div style={{height:20}} />}
				<div style={{marginBottom:2}}>
					{ showprogress ? false :
						<div className="float-left"> {collhits.hits + " matching collections found"} </div>
					}
					{ collhits.hits === 0 ? false :
						<div className="float-right">
							<div>
							   {this.renderDisplayKWIC()}
							   {this.props.queryTypeId !== "fcs" ? "" : this.renderDisplayADV()}
								{ collhits.inProgress === 0 ?
											<div className="inline"> {this.renderDownloadLinks()} </div>
											:false
										}
									</div>
								</div>
							}
							<div style={{clear:'both'}}/>
						</div>
		    <TransitionGroup>
						{collhits.results.map(this.renderResultPanel)}
		    </TransitionGroup>
				</div>);
	}
});

var _ = window._ = window._ || {
	keys: function() {
		var ret = [];
		for (var x in o) {
			if (o.hasOwnProperty(x)) {
				ret.push(x);
			}
		}
		return ret;
	},

	pairs: function(o){
		var ret = [];
		for (var x in o) {
			if (o.hasOwnProperty(x)) {
				ret.push([x, o[x]]);
			}
		}
		return ret;
	},

	values: function(o){
		var ret = [];
		for (var x in o) {
			if (o.hasOwnProperty(x)) {
				ret.push(o[x]);
			}
		}
		return ret;
	},

	uniq: function(a) {
		var r = [];
		for (var i = 0; i < a.length; i++) {
			if (r.indexOf(a[i]) < 0) {
				r.push(a[i]);
			}
		}
		return r;
	},
};

module.exports = Results;
