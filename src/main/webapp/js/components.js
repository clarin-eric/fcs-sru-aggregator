/** @jsx React.DOM */

var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;
var ReactTransitionGroup = React.addons.TransitionGroup;

window.MyReact = {};

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

var InfoPopover = React.createClass({displayName: 'InfoPopover',
	propTypes: {
		title: PT.string.isRequired,
	},
	mixins: [PopoverMixin],

	handleClick: function(e) {
		e.stopPropagation();
	},

	render: function() {
		return	React.createElement("button", {className: "btn btn-default btn-xs", onClick: this.handleClick}, 
					React.createElement("span", {className: "glyphicon glyphicon-info-sign"})
				);
	}
});


window.MyReact.Panel = React.createClass({displayName: 'Panel',
	propTypes: {
		corpus:PT.object.isRequired,
	},

	getInitialState: function() {
		return {
			open: true,
		};
	},

	toggleState: function(e) {
		this.setState({open: !this.state.open});
	},

	renderBody: function() {
		return this.state.open ? 
					React.createElement("div", {className: "panel-body"}, this.props.children) : 
					false;
	},

	renderInfo: function() {
		return	React.createElement("dl", {className: "dl-horizontal"}, 
					React.createElement("dt", null, "Institution"), 
					React.createElement("dd", null, this.props.corpus.institution.name), 

					this.props.corpus.description ? React.createElement("dt", null, "Description"):false, 
					this.props.corpus.description ? React.createElement("dd", null, this.props.corpus.description): false, 

					this.props.corpus.landingPage ? React.createElement("dt", null, "Landing Page") : false, 
					this.props.corpus.landingPage ? 
						React.createElement("dd", null, React.createElement("a", {href: this.props.corpus.landingPage}, this.props.corpus.landingPage)):
						false, 

					React.createElement("dt", null, "Languages"), 
					React.createElement("dd", null, this.props.corpus.languages.join(", "))
				);
	},

	render: function() {
		var chevron = "glyphicon glyphicon-chevron-" + (this.state.open ? "down":"right");
		var chevronStyle={fontSize:12};
		var right={float:"right"};
		return 	React.createElement("div", {className: "bs-callout bs-callout-info"}, 
					React.createElement("div", {className: "panel"}, 
						React.createElement("div", {className: "panel-heading unselectable row", onClick: this.toggleState}, 
							React.createElement("div", {className: "panel-title unselectable col-sm-11"}, 
								React.createElement("span", {className: chevron, style: chevronStyle}), " ", 
								this.props.corpus.displayName
							), 
							React.createElement("div", {style: right}, 
								React.createElement(InfoPopover, {placement: "left", title: this.props.corpus.displayName}, 
									this.renderInfo()
								)
							)
						), 
						this.renderBody()
					)
				);
	}
});

window.MyReact.PanelGroup = React.createClass({displayName: 'PanelGroup',
	render: function() {
		return	React.createElement("div", {className: "panel-group"}, " ", this.props.children, " ");
	},
});
