"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var Panel = createReactClass({
//fixme! - class Panel extends React.Component {
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
		return 	<div className="bs-callout bs-callout-info"> 
					<div className="panel">
						<div className="panel-heading unselectable row" onClick={this.toggleState}>
							<div className="panel-title unselectable col-sm-11">
								<span className={chevron} style={{fontSize:12}} />&nbsp;
								{this.props.title}
							</div>
							<div className='float-right'>
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


module.exports = Panel;
