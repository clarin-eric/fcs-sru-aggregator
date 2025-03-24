"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var PanelGroup = createReactClass({
  //fixme! - class PanelGroup extends React.Component {
  render: function () {
    return <div className="panel-group"> {this.props.children} </div>;
  },
});

module.exports = PanelGroup;
