"use strict";
import classNames from "classnames";
import PopoverMixin from "./popovermixin.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var Popover = createReactClass({
  propTypes: {
    placement: PT.string,
    title: PT.string,
    triggerButtonClass: PT.string,
    triggerButtonContent: PT.element.isRequired
  },
  mixins: [PopoverMixin],

  handleClick: function (e) {
    e.stopPropagation();
  },

  render: function () {
    return <button className={this.props.triggerButtonClass} onClick={this.handleClick}>
      {this.props.triggerButtonContent}
    </button>;
  }
});

module.exports = Popover;
