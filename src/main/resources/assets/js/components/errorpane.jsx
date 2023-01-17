"use strict";
import classNames from "classnames";
import JQueryFade from "./jqueryfade.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";
import { CSSTransition, TransitionGroup } from "react-transition-group";

var PT = PropTypes;

var ErrorPane = createReactClass({
  //fixme! - class ErrorPane extends React.Component {
  propTypes: {
    errorMessages: PT.array.isRequired,
  },

  renderErrorMessage: function (errorMessage, index) {
    return errorMessage ?
      <JQueryFade key={index}>
        <div key={index} className="errorMessage">{errorMessage}</div>
      </JQueryFade> :
      false;
  },

  render: function () {
    return <div className="container errorDiv">
      <div className="row errorRow">
        <TransitionGroup component="div">
          {this.props.errorMessages.map(this.renderErrorMessage)}
        </TransitionGroup>
      </div>
    </div>;
  }
});

module.exports = ErrorPane;
