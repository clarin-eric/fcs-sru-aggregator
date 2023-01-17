"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var Modal = createReactClass({
  //fixme! - class Modal extends React.Component {
  propTypes: {
    title: PT.object.isRequired,
  },
  componentDidMount: function () {
    $(ReactDOM.findDOMNode(this)).modal({ background: true, keyboard: true, show: false });
  },
  componentWillUnmount: function () {
    $(ReactDOM.findDOMNode(this)).off('hidden');
  },
  handleClick: function (e) {
    e.stopPropagation();
  },
  render: function () {
    return (
      <div onClick={this.handleClick} className="modal fade" role="dialog" aria-hidden="true">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header">
              <button type="button" className="close" data-dismiss="modal">
                <span aria-hidden="true">&times;</span>
                <span className="sr-only">Close</span>
              </button>
              <h2 className="modal-title">{this.props.title}</h2>
            </div>
            <div className="modal-body">
              {this.props.children}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-default" data-dismiss="modal">Close</button>
            </div>
          </div>
        </div>
      </div>
    );
  }
});

module.exports = Modal;
