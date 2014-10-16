/** @jsx React.DOM */

window.MyReact = {};
var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;

window.MyReact.Panel = React.createClass({
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
		return 	<div key={this.props.key} className="bs-callout bs-callout-info"> 
					<div className="panel">
						<div className="panel-heading unselectable" onClick={this.toggleState}>
							<p className="panel-title unselectable">
								<span className={chevron} style={chevronStyle}></span>&nbsp;
								{this.props.header}
							</p>
						</div>
							<div className={"panel-collapse collapse " + (open ? "in":"")}>
								<div className="panel-body">{this.props.children}</div>
							</div>
					</div>
				</div>;
	}
});

window.MyReact.PanelGroup = React.createClass({
	render: function() {
		return	<div className="panel-group">
					{this.props.children}
				</div>;
	},
});

