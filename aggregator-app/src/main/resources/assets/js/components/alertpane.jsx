"use strict";
import classNames from "classnames";
import JQueryFade from "./jqueryfade.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";
import { CSSTransition, TransitionGroup } from "react-transition-group";

var PT = PropTypes;

var AlertPane = createReactClass({
  //fixme! - class AlertPane extends React.Component {
  propTypes: {
    alerts: PT.array.isRequired,
  },

  renderAlertMessage: function (alert, index) {
    return alert ?
      (<CSSTransition key={alert.type + "-" + index} classNames="fade" timeout={{ enter: 200, exit: 200 }}>
        <div key={alert.type + "-" + index} className={("info" === alert.type) ? "infoMessage" : "errorMessage"}>{alert.msg}</div>
      </CSSTransition>) :
      false;
  },

  render: function () {
    return <div className="container alertDiv">
      <div className="row alertRow">
        <TransitionGroup component="div">
          {this.props.alerts.map(this.renderAlertMessage)}
        </TransitionGroup>
      </div>
    </div>;
  }
});

module.exports = AlertPane;
