"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var InfoPopover = createReactClass({
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

module.exports = InfoPopover;
