/** @jsx React.DOM */
(function() {
"use strict";

var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;
var ReactTransitionGroup = React.addons.TransitionGroup;

window.MyReact = {};

var JQuerySlide = React.createClass({displayName: 'JQuerySlide',
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
 
var JQueryFade = React.createClass({displayName: 'JQueryFade',
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

window.MyReact.ErrorPane = React.createClass({displayName: 'ErrorPane',
	propTypes: {
		errorMessages: PT.array.isRequired,
	},

	renderErrorMessage: function(errorMessage, index) {
		return errorMessage ? 
			React.createElement(JQueryFade, {key: index}, 
				React.createElement("div", {key: index, className: "errorMessage"}, errorMessage)
			) :
			false;
	},

	render: function() {
		return	React.createElement("div", {className: "container errorDiv"}, 
					React.createElement("div", {className: "row errorRow"}, 
						React.createElement(ReactTransitionGroup, {component: "div"}, 
							this.props.errorMessages.map(this.renderErrorMessage)
						)
					)
				);
	}
});


window.MyReact.Modal = React.createClass({displayName: 'Modal',
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
			React.createElement("div", {onClick: this.handleClick, className: "modal fade", role: "dialog", 'aria-hidden': "true"}, 
				React.createElement("div", {className: "modal-dialog"}, 
					React.createElement("div", {className: "modal-content"}, 
						React.createElement("div", {className: "modal-header"}, 
							React.createElement("button", {type: "button", className: "close", 'data-dismiss': "modal"}, 
								React.createElement("span", {'aria-hidden': "true"}, "×"), 
								React.createElement("span", {className: "sr-only"}, "Close")
							), 
							React.createElement("h2", {className: "modal-title"}, this.props.title)
						), 
						React.createElement("div", {className: "modal-body"}, 
							this.props.children
						), 
						React.createElement("div", {className: "modal-footer"}, 
							React.createElement("button", {type: "button", className: "btn btn-default", 'data-dismiss': "modal"}, "Close")
						)
					)
				)
			)
		);
	}
});


var PopoverMixin = {
	getDefaultProps: function(){
		return {hasPopover: true};
	},
 
	componentDidMount: function() {
		var content;
		if (Array.isArray(this.props.children))
			content = this.props.children.map(React.renderToString).join(" ");
		else 
			content = React.renderToString(this.props.children);
		$(this.getDOMNode()).popover({
			content: content,
			animation: this.props.animation,
			placement: this.props.placement,
			title: this.props.title,
			trigger: 'click',
			html: true,
		});
	}
};

window.MyReact.InfoPopover = React.createClass({displayName: 'InfoPopover',
	propTypes: {
		title: PT.string.isRequired,
	},
	mixins: [PopoverMixin],

	handleClick: function(e) {
		e.stopPropagation();
	},

	render: function() {
		var inline = {display:"inline-block"};
		return	React.createElement("button", {style: inline, className: "btn btn-default btn-xs", onClick: this.handleClick}, 
					React.createElement("span", {className: "glyphicon glyphicon-info-sign"})
				);
	}
});


window.MyReact.Panel = React.createClass({displayName: 'Panel',
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
		return 	React.createElement("div", {className: "bs-callout bs-callout-info"}, 
					React.createElement("div", {className: "panel"}, 
						React.createElement("div", {className: "panel-heading unselectable row", onClick: this.toggleState}, 
							React.createElement("div", {className: "panel-title unselectable col-sm-11"}, 
								React.createElement("span", {className: chevron, style: chevronStyle}), " ", 
								this.props.title
							), 
							React.createElement("div", {style: right}, 
								this.props.info
							)
						), 
						 this.state.open ? 
							React.createElement("div", {className: "panel-body"}, this.props.children) : 
							false
					)
				);
	}
});

window.MyReact.PanelGroup = React.createClass({displayName: 'PanelGroup',
	render: function() {
		return	React.createElement("div", {className: "panel-group"}, " ", this.props.children, " ");
	},
});

})();
