"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";

var PT = PropTypes;

var ModalMixin = {
  componentDidMount: function () {
    $(ReactDOM.findDOMNode(this)).modal({ background: true, keyboard: true, show: false });
  },
  componentWillUnmount: function () {
    $(ReactDOM.findDOMNode(this)).off('hidden');
  },
  handleClick: function (e) {
    e.stopPropagation();
  },
  renderModal: function (title, content) {
    return (
      <div onClick={this.handleClick} className="modal fade" role="dialog" aria-hidden="true">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header">
              <button type="button" className="close" data-dismiss="modal">
                <span aria-hidden="true">&times;</span>
                <span className="sr-only">Close</span>
              </button>
              <h2 className="modal-title">{title}</h2>
            </div>
            <div className="modal-body">
              {content}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-default" data-dismiss="modal">Close</button>
            </div>
          </div>
        </div>
      </div>
    );
  }
};

module.exports = ModalMixin;
