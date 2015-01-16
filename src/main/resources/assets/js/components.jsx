/** @jsx React.DOM */
(function() {
"use strict";

var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;
var ReactTransitionGroup = React.addons.TransitionGroup;

window.MyReact = {};

var JQuerySlide = React.createClass({
	componentWillEnter: function(callback){
		var el = jQuery(this.getDOMNode());
		el.css("display", "none");
		el.slideDown(500, callback);
		$el.slideDown(function(){
			callback();
		});
	},
	componentWillLeave: function(callback){
		var $el = jQuery(this.getDOMNode());
		$el.slideUp(function(){
			callback();
		});
	},
	render: function(){
		return this.transferPropsTo(this.props.component({style: {display: 'none'}}));
	}
});
window.MyReact.JQuerySlide = JQuerySlide;
 
var JQueryFade = React.createClass({
	componentWillEnter: function(callback){
		var el = jQuery(this.getDOMNode());
		el.css("display", "none");
		el.fadeIn(500, callback);
	},
	componentWillLeave: function(callback){
		jQuery(this.getDOMNode()).fadeOut(500, callback);
	},
	render: function() {
		return this.props.children;
	}
});
window.MyReact.JQueryFade = JQueryFade;

window.MyReact.ErrorPane = React.createClass({
	propTypes: {
		errorMessages: PT.array.isRequired,
	},

	renderErrorMessage: function(errorMessage, index) {
		return errorMessage ? 
			<JQueryFade key={index}>
				<div key={index} className="errorMessage">{errorMessage}</div>
			</JQueryFade> :
			false;
	},

	render: function() {
		return	<div className="container errorDiv">
					<div className="row errorRow">
						<ReactTransitionGroup component="div">
							{this.props.errorMessages.map(this.renderErrorMessage)}
						</ReactTransitionGroup>
					</div>
				</div>;
	}
});


window.MyReact.Modal = React.createClass({
	propTypes: {
		title: PT.string.isRequired,
	},
	componentDidMount: function() {
		$(this.getDOMNode()).modal({background: true, keyboard: true, show: false});
	},
	componentWillUnmount: function() {
		$(this.getDOMNode()).off('hidden');
	},
	handleClick: function(e) {
		e.stopPropagation();
	},
	render: function() {
		return (
			<div onClick={this.handleClick} className="modal fade" role="dialog" aria-hidden="true">
				<div className="modal-dialog">
					<div className="modal-content">
						<div className="modal-header">
							<button type="button" className="close" data-dismiss="modal">
								<span aria-hidden="true">&times;</span>
								<span className="sr-only">Close</span>
							</button>
							<h2 className="modal-title">{this.props.title}</h2>
						</div>
						<div className="modal-body">
							{this.props.children}
						</div>
						<div className="modal-footer">
							<button type="button" className="btn btn-default" data-dismiss="modal">Close</button>
						</div>
					</div>
				</div>
			</div>
		);
	}
});


var PopoverMixin = window.MyReact.PopoverMixin = {
	getDefaultProps: function(){
		return {hasPopover: true};
	},
 
	componentDidMount: function() {
		this.refresh();
	},
	componentDidUpdate: function() {
		this.refresh();
	},

	refresh: function() {
		$(this.getDOMNode()).popover('destroy');

		var content;
		if (Array.isArray(this.props.children))
			content = this.props.children.map(React.renderToString).join(" ");
		else 
			content = React.renderToString(this.props.children);
		// console.log("children: ", this.props.children);
		// console.log("content: ", content);
		$(this.getDOMNode()).popover({
			content: content,
			animation: this.props.animation,
			placement: this.props.placement,
			title: this.props.title,
			trigger: 'click',
			html: true,
		});
	},

	componentWillUnmount: function() {
		$(this.getDOMNode()).popover('destroy');
	},	
};

window.MyReact.Popover = React.createClass({
	propTypes: {
		placement: PT.string,
		title: PT.string,
		triggerButtonClass: PT.string,
		triggerButtonContent: PT.element.isRequired
	},
	mixins: [PopoverMixin],

	handleClick: function(e) {
		e.stopPropagation();
	},

	render: function() {
		return	<button className={this.props.triggerButtonClass} onClick={this.handleClick}>
					{this.props.triggerButtonContent}
				</button>;
	}
});

window.MyReact.InfoPopover = React.createClass({
	propTypes: {
		title: PT.string.isRequired,
	},
	mixins: [PopoverMixin],

	handleClick: function(e) {
		e.stopPropagation();
	},

	render: function() {
		var inline = {display:"inline-block"};
		return	<button style={inline} className="btn btn-default btn-xs" onClick={this.handleClick}>
					<span className="glyphicon glyphicon-info-sign"/>
				</button>;
	}
});


window.MyReact.Panel = React.createClass({
	propTypes: {
		title:PT.object.isRequired,
		info:PT.object.isRequired,
	},

	getInitialState: function() {
		return {
			open: true,
		};
	},

	toggleState: function(e) {
		this.setState({open: !this.state.open});
	},

	render: function() {
		var chevron = "glyphicon glyphicon-chevron-" + (this.state.open ? "down":"right");
		var chevronStyle = {fontSize:12};
		var right = {float:"right"};
		return 	<div className="bs-callout bs-callout-info"> 
					<div className="panel">
						<div className="panel-heading unselectable row" onClick={this.toggleState}>
							<div className="panel-title unselectable col-sm-11">
								<span className={chevron} style={chevronStyle} />&nbsp;
								{this.props.title}
							</div>
							<div style={right}>
								{this.props.info}
							</div>
						</div>
						{ this.state.open ? 
							<div className="panel-body">{this.props.children}</div> : 
							false}
					</div>
				</div>;
	}
});

window.MyReact.PanelGroup = React.createClass({
	render: function() {
		return	<div className="panel-group"> {this.props.children} </div>;
	},
});

})();
