/** @jsx React.DOM */

var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;
var ReactTransitionGroup = React.addons.TransitionGroup;

window.MyReact = {};
window.MyReact.Panel = React.createClass({displayName: 'Panel',
	propTypes: {
		key:  PT.oneOfType([PT.string, PT.number]).isRequired,
		header: PT.string.isRequired,
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
		var chevronStyle={fontSize:12};
		return 	React.createElement("div", {key: this.props.key, className: "bs-callout bs-callout-info"}, 
					React.createElement("div", {className: "panel"}, 
						React.createElement("div", {className: "panel-heading unselectable", onClick: this.toggleState}, 
							React.createElement("p", {className: "panel-title unselectable"}, 
								React.createElement("span", {className: chevron, style: chevronStyle}), " ", 
								this.props.header
							)
						), 
						React.createElement("div", {className: "panel-body"}, 
							React.createElement(ReactTransitionGroup, {transitionName: "display"}, 
								this.state.open ? this.props.children : false
							)
						)
					)
				);
	}
});

window.MyReact.PanelGroup = React.createClass({displayName: 'PanelGroup',
	render: function() {
		return	React.createElement("div", {className: "panel-group"}, " ", this.props.children, " ");
	},
});
