"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";
import {CSSTransition, TransitionGroup} from "react-transition-group";

var PT = PropTypes;

var QueryInput = createReactClass({
    //fixme! - class QueryInput extends React.Component {
    propTypes: {
	searchedLanguage: PT.array,
	queryTypeId: PT.string.isRequired, 
	query: PT.string,
	embedded: PT.bool.isRequired,
	placeholder: PT.string,
	onChange: PT.func.isRequired,
	onQuery: PT.func.isRequired,
	onKeyDown: PT.func.isRequired
    },

    render: function() {
	//if (this.props.queryTypeId == "cql") {
	    return (
		<input className="form-control input-lg search" 
		       id="query-cql" name="query-cql" type="text"
		       value={this.props.query} placeholder={this.props.placeholder}
		       tabIndex="1" onChange={this.props.onChange} 
		       //onQuery={this.props.onQuery}
		       onKeyDown={this.props.onKeyDown} 
		       ref="cqlOrEmbeddedQuery"/>
	    );
	// } else if (this.props.embedded && this.props.queryTypeId == "fcs") {
	//     return (
	// 	<textarea className="form-control input-lg search"
	// 	       id="query-fcs" name="query-fcs"
	// 	       type="text" rows="1"
	// 	       value={this.props.query} placeholder={this.props.placeholder}
	// 	       tabIndex="1" onChange={this.props.onChange} 
	// 	       //onQuery={this.props.onQuery}
	// 	       onKeyDown={this.props.onKeyDown} 
	// 	       ref="fcsOrEmbeddedQuery" />
	//     );
	// }
	// return (<div id="adv_query_input_group" className="input-group-addon">
	// 	    <ADVTokens
	//                 query={this.props.query}
	//                 ref="fcsGQB"
	//             />
	// </div>);
    }
});

var ADVTokens = createReactClass({

    propTypes: {
	query: PT.string
    },

    getInitialState: function () {
	return { 
	    tokenCounter: 1,
	    tokens: ["token1"] 
	};
    },

    addADVToken: function() {
	var i = this.state.tokenCounter + 1;
	this.state.tokens.push('token' + i);
	this.setState({tokenCounter: i, tokens: this.state.tokens});
    },
    
    removeADVToken: function(id) {
	var tokens = this.state.tokens;
	var i = tokens.indexOf(id);
	if (tokens.length > 1) {
	    var one = tokens;
	    var two = one.slice(0, i - 1)
			 .concat(one.slice(i));;
	    this.setState({tokens: two});
	}
    },

    render: function() {
	var i = 0;
	var tokens = this.state.tokens.map(function (token, i) {
	    return (
	       <CSSTransition key={i} classNames="token" timeout={{enter: 250, exit: 250}}>
		  <ADVToken 
			key={token}
			parentToken={token}
		        handleRemoveADVToken={this.removeADVToken} />
	       </CSSTransition>);
	}.bind(this));

	return (<div>
	    <TransitionGroup>{tokens}</TransitionGroup>
		<button className="btn btn-xs btn-default image_button insert_token" type="button" onClick={this.addADVToken} ref="addToken">
		    <i className="glyphicon glyphicon-plus"></i>
		</button>
	</div>);
    }
});

var ADVToken = createReactClass({
    propTypes: {
	parentToken: PT.string.isRequired,
	handleRemoveADVToken: PT.func.isRequired,
    },
    render: function() {
	return (<div className="token query_token inline btn-group" style={{display:"inline-block"}}>
	    <div className="token_header">
	       <button className="btn btn-xs btn-default image_button close_btn" type="button" onClick={this.props.handleRemoveADVToken(this.props.parentToken)} ref="removeToken">
	          <i className="glyphicon glyphicon-remove-circle" />
	       </button>
	       <div style={{clear:"both"}} />
	       </div>
	       <div className="args">
	       { /* and.query_arg* and token_footer */ }
	         <ANDQueryArgs />
	       <div className="lower_footer">
	       </div>
	    </div>
	</div>);
    }
});

var ADVTokenMenu = createReactClass({
	getInitialState: function() {
	    return {"hideRepeatMenu": true};
	},

	toggleRepeatMenu: function(e) {
	    this.setState({"hideRepeatMenu": !this.state.hideRepeatMenu});
	    e.preventDefault();
	},
	
	render: function() {
	    return (<div>
	    <button className="btn btn-xs btn-default image_button repeat_menu" onClick={this.toggleRepeatMenu} ref="repeatMenu">
		<i className="fa fa-cog" />
	    </button>
	    <div id="repeatMenu" className={"repeat hide-" + this.state.hideRepeatMenu}>
		<span>repeat</span>
		<input type="number" id="repeat1" value={this.state.repeat1} ref="repeat1"/>
		<span>to</span>
		<input type="number" id="repeat2" value={this.state.repeat2} ref="repeat2"/>
		<span>times</span>
	    </div>
	    </div>);
	}
    });

    var ANDQueryArgs = createReactClass({

	getInitialState: function() {
	    return {
		andCounter: 1,
		ands: ["and1"]
	    };
	},
	
	setADVTokenLayer: function(layer) {
	    //fixme! - check agains valid layers
	    return;
	},

	addADVAnd: function() {
	    var i = this.state.andCounter + 1;
	    this.state.ands.push('and' + i);
	    this.setState({andCounter: i, ands: this.state.ands});

	},

	removeADVAnd: function(id) {
	    var ands = this.state.ands;
	    var i = ands.indexOf(id);
	    if (ands.length > 1) {
		var one = ands;
		var two = one.slice(0, i - 1)
			     .concat(one.slice(i));;
		this.setState({ands: two});
	    }
	},

	renderANDTokenFooter: function () {
	    return (<div className="token_footer">
		<button className="btn btn-xs btn-default image_button insert_arg" onClick={this.addADVAnd} ref="addAndButton">
		    <i className="glyphicon glyphicon-plus"/>
		</button>
		<ADVTokenMenu/>
		<div style={{clear:"both"}}/>
	    </div>);
	},

	renderANDQueryArg: function (and) {
	    return (<div className="and query_arg">
		<span className="hidden">and</span>
		<ANDQueryORArgs 
		numAnds={this.state.ands.length}
		parentAnd={and}
		handleRemoveADVAnd={this.removeADVAnd}/>
	    </div>);
	},
	
	render: function () {
	    var andQueryArgs = this.state.ands.map(function (and, i) {
	    return (
		<CSSTransition key={i} classNames="fade" timeout={{enter: 200, exit: 200}}>
		   <div key={and}>{this.renderANDQueryArg(and)}</div>
		</CSSTransition>);
	    }.bind(this));
	    return (<div>
		<TransitionGroup>
		   {andQueryArgs}
		</TransitionGroup>
		{this.renderANDTokenFooter()}
		</div>);
    }
});

var ANDQueryORArgs = createReactClass({
    propTypes: {
	numAnds: PT.number.isRequired,
	parentAnd: PT.string.isRequired,
	handleRemoveADVAnd: PT.func.isRequired,
    },
    getInitialState: function() {
	return {
	    orCounter: 1,
	    ors: [{id: "or1", layerType: "string:lemma", placeholder: "Bagdad"}]
	};
    },

    //shouldComponentUpdate: function (nextProps, nextState) {
    //	return nextState.ors.length > 1; //!== this.state.ors.length;
    //},

    setADVTokenOp: function(op) {
	//fixme! - check agains valid layers
	return;
    },

    setADVInputDefault: function(or) {
	//fixme! - disable SearchButton if not atleast 1 token is in the query filter
	return;
    },

    validateADV: function(value) {
	//fixme! - disable SearchButton if not atleast 1 token is in the query filter
	return;
    },

    addADVOr: function(e) {
	var i = this.state.orCounter + 1;
	this.state.ors.push({id: 'or' + i, layerType: "string:pos", placeholder: "PROPN"});
	this.setState({orCounter: i, ors: this.state.ors});
    },

    removeADVOr: function(id, e) {
	var ors = this.state.ors;
	var i = ors.indexOf(id);
	if (ors.length > 1) {
	    var one = ors;
	    var two = one.slice(0, i - 1)
			 .concat(one.slice(i));;
	    this.setState({ors: two});
	} else if (ors.length === 1 && this.props.numAnds > 1) {
	    this.props.handleRemoveADVAnd(this.props.parentAnd);
	}
    },

    render: function () {
	var orArgs = this.state.ors.map(function (or, i) {
	    return ( 		
		<CSSTransition key={i} classNames="fade" timeout={{enter: 200, exit: 200}}>
		   <ORArg key={or.id} 
	                  data={or} 
                          handleRemoveADVOr={this.removeADVOr}
	                  handleSetADVInputDefault={this.setADVInputDefault}
	                  handleSetADVTokenOp={this.setADVTokenOp}
	                  handleValidateADV={this.validateADV}
	           />
		</CSSTransition>
	    )
	}.bind(this));
	return (<div>
	    <div className="or_container">
	       <TransitionGroup>
	          {orArgs}
	       </TransitionGroup>
	    </div>
	    <div className="arg_footer">
		<span className="link" onClick={this.addADVOr} ref={'addOR' + this.props.numAnds}>or</span>
		<div style={{clear:"both"}}/>
	    </div>
	</div>);
    }
});

var ORArg = createReactClass({
    propTypes: {
	data: PT.object.isRequired,
	handleRemoveADVOr: PT.func.isRequired,
	handleSetADVInputDefault: PT.func.isRequired,
	handleSetADVTokenOp: PT.func.isRequired,
	handleValidateADV: PT.func.isRequired,
    },

    render: function() {
	return (<div className="or or_arg">
	    <div className="left_col" >
		<button className="btn btn-xs btn-default image_button remove_arg" onClick={this.props.handleRemoveADVOr.bind(null, this.props.data.id)} ref={'removeADVOr_' + this.props.data.id}>
		    <i className="glyphicon glyphicon-minus"></i>
		</button>
	    </div>
	    <div className="right_col inline_block" style={{display:"inline-block"}}> { /* , margin-left: "5px" */ }
		<div className="arg_selects lemma">
		    <select className="arg_type" onChange={this.props.handleSetADVInputDefault("or")} defaultValue={this.props.data.layerType} ref={'ANDLayerType_' + this.props.data.id}>
			{ /* onChange={this.handleSetADVTokenLayer("value")} */}
			<optgroup label="word">
			    { /* ::before */ }
			    <option value="string:word" label="word">word</option>
			</optgroup>
			<optgroup label="wordAttribute">
			    { /* ::before */ }
			    <option value="string:pos">part-of-speech</option>
			    <option value="string:lemma">lemma</option>
			</optgroup>
			<optgroup label="textAttribute">
			    <option value="string:_.text_language" label="language">language</option>
			</optgroup>
		    </select>
		    <select className="arg_opts" defaultValue="string:contains" onChange={this.props.handleSetADVTokenOp("op")}>
			<option value="string:contains" label="is">is</option>
			<option value="string:not contains" label="is not">is not</option>
		    </select>
		</div>
		<div className="arg_val_container">
	 	    <input id={'inputADV_' + this.props.data.id} type="text" defaultValue={this.props.data.placeholder} onChange={this.props.handleValidateADV} ref={'textEntry_' + this.props.data.id}/>
		</div>
	       <select> 
		<option label="PROPN" value="string:PROPN">Proper Noun</option>
	       </select>
	    </div>
	    </div>);
    }
});

module.exports = QueryInput;
