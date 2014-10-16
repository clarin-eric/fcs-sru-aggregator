/** @jsx React.DOM */

window.MyReact = {};
var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;

window.MyReact.Panel = React.createClass({displayName: 'Panel',
	propTypes: {
		key: PT.number.isRequired,
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
		var open = this.state.open;
		var chevron = "glyphicon glyphicon-chevron-" + (open ? "down":"right");
		var chevronStyle={fontSize:"12px"};
		return 	React.DOM.div({key: this.props.key, className: "bs-callout bs-callout-info"}, 
					React.DOM.div({className: "panel"}, 
						React.DOM.div({className: "panel-heading unselectable", onClick: this.toggleState}, 
							React.DOM.p({className: "panel-title unselectable"}, 
								React.DOM.span({className: chevron, style: chevronStyle}), " ", 
								this.props.header
							)
						), 
							React.DOM.div({className: "panel-collapse collapse " + (open ? "in":"")}, 
								React.DOM.div({className: "panel-body"}, this.props.children)
							)
					)
				);
	}
});

window.MyReact.PanelGroup = React.createClass({displayName: 'PanelGroup',
	render: function() {
		return	React.DOM.div({className: "panel-group"}, 
					this.props.children
				);
	},
});

