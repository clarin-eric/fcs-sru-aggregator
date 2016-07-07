(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
"use strict";

(function () {
	"use strict";

	var PT = React.PropTypes;
	var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;
	var ReactTransitionGroup = React.addons.TransitionGroup;

	window.MyReact = {};

	var JQuerySlide = React.createClass({
		displayName: "JQuerySlide",

		componentWillEnter: function componentWillEnter(callback) {
			var el = jQuery(ReactDOM.findDOMNode(this));
			el.css("display", "none");
			el.slideDown(500, callback);
			$el.slideDown(function () {
				callback();
			});
		},
		componentWillLeave: function componentWillLeave(callback) {
			var $el = jQuery(ReactDOM.findDOMNode(this));
			$el.slideUp(function () {
				callback();
			});
		},
		render: function render() {
			return this.transferPropsTo(this.props.component({ style: { display: 'none' } }));
		}
	});
	window.MyReact.JQuerySlide = JQuerySlide;

	var JQueryFade = React.createClass({
		displayName: "JQueryFade",

		componentWillEnter: function componentWillEnter(callback) {
			var el = jQuery(ReactDOM.findDOMNode(this));
			el.css("display", "none");
			el.fadeIn(500, callback);
		},
		componentWillLeave: function componentWillLeave(callback) {
			jQuery(ReactDOM.findDOMNode(this)).fadeOut(500, callback);
		},
		render: function render() {
			return this.props.children;
		}
	});
	window.MyReact.JQueryFade = JQueryFade;

	window.MyReact.ErrorPane = React.createClass({
		displayName: "ErrorPane",

		propTypes: {
			errorMessages: PT.array.isRequired
		},

		renderErrorMessage: function renderErrorMessage(errorMessage, index) {
			return errorMessage ? React.createElement(
				JQueryFade,
				{ key: index },
				React.createElement(
					"div",
					{ key: index, className: "errorMessage" },
					errorMessage
				)
			) : false;
		},

		render: function render() {
			return React.createElement(
				"div",
				{ className: "container errorDiv" },
				React.createElement(
					"div",
					{ className: "row errorRow" },
					React.createElement(
						ReactTransitionGroup,
						{ component: "div" },
						this.props.errorMessages.map(this.renderErrorMessage)
					)
				)
			);
		}
	});

	window.MyReact.ModalMixin = {
		componentDidMount: function componentDidMount() {
			$(ReactDOM.findDOMNode(this)).modal({ background: true, keyboard: true, show: false });
		},
		componentWillUnmount: function componentWillUnmount() {
			$(ReactDOM.findDOMNode(this)).off('hidden');
		},
		handleClick: function handleClick(e) {
			e.stopPropagation();
		},
		renderModal: function renderModal(title, content) {
			return React.createElement(
				"div",
				{ onClick: this.handleClick, className: "modal fade", role: "dialog", "aria-hidden": "true" },
				React.createElement(
					"div",
					{ className: "modal-dialog" },
					React.createElement(
						"div",
						{ className: "modal-content" },
						React.createElement(
							"div",
							{ className: "modal-header" },
							React.createElement(
								"button",
								{ type: "button", className: "close", "data-dismiss": "modal" },
								React.createElement(
									"span",
									{ "aria-hidden": "true" },
									"×"
								),
								React.createElement(
									"span",
									{ className: "sr-only" },
									"Close"
								)
							),
							React.createElement(
								"h2",
								{ className: "modal-title" },
								title
							)
						),
						React.createElement(
							"div",
							{ className: "modal-body" },
							content
						),
						React.createElement(
							"div",
							{ className: "modal-footer" },
							React.createElement(
								"button",
								{ type: "button", className: "btn btn-default", "data-dismiss": "modal" },
								"Close"
							)
						)
					)
				)
			);
		}
	};

	window.MyReact.Modal = React.createClass({
		displayName: "Modal",

		propTypes: {
			title: PT.object.isRequired
		},
		componentDidMount: function componentDidMount() {
			$(ReactDOM.findDOMNode(this)).modal({ background: true, keyboard: true, show: false });
		},
		componentWillUnmount: function componentWillUnmount() {
			$(ReactDOM.findDOMNode(this)).off('hidden');
		},
		handleClick: function handleClick(e) {
			e.stopPropagation();
		},
		render: function render() {
			return React.createElement(
				"div",
				{ onClick: this.handleClick, className: "modal fade", role: "dialog", "aria-hidden": "true" },
				React.createElement(
					"div",
					{ className: "modal-dialog" },
					React.createElement(
						"div",
						{ className: "modal-content" },
						React.createElement(
							"div",
							{ className: "modal-header" },
							React.createElement(
								"button",
								{ type: "button", className: "close", "data-dismiss": "modal" },
								React.createElement(
									"span",
									{ "aria-hidden": "true" },
									"×"
								),
								React.createElement(
									"span",
									{ className: "sr-only" },
									"Close"
								)
							),
							React.createElement(
								"h2",
								{ className: "modal-title" },
								this.props.title
							)
						),
						React.createElement(
							"div",
							{ className: "modal-body" },
							this.props.children
						),
						React.createElement(
							"div",
							{ className: "modal-footer" },
							React.createElement(
								"button",
								{ type: "button", className: "btn btn-default", "data-dismiss": "modal" },
								"Close"
							)
						)
					)
				)
			);
		}
	});

	var PopoverMixin = window.MyReact.PopoverMixin = {
		getDefaultProps: function getDefaultProps() {
			return { hasPopover: true };
		},

		componentDidMount: function componentDidMount() {
			this.refresh();
		},
		componentDidUpdate: function componentDidUpdate() {
			this.refresh();
		},

		refresh: function refresh() {
			$(ReactDOM.findDOMNode(this)).popover('destroy');

			var content;
			if (Array.isArray(this.props.children)) content = this.props.children.map(React.renderToString).join("");else content = React.renderToString(this.props.children);
			// console.log("children: ", this.props.children);
			// console.log("content: ", content);
			$(ReactDOM.findDOMNode(this)).popover({
				content: content,
				animation: this.props.animation,
				placement: this.props.placement,
				title: this.props.title,
				trigger: 'focus',
				html: true
			});
		},

		componentWillUnmount: function componentWillUnmount() {
			$(ReactDOM.findDOMNode(this)).popover('destroy');
		}
	};

	window.MyReact.Popover = React.createClass({
		displayName: "Popover",

		propTypes: {
			placement: PT.string,
			title: PT.string,
			triggerButtonClass: PT.string,
			triggerButtonContent: PT.element.isRequired
		},
		mixins: [PopoverMixin],

		handleClick: function handleClick(e) {
			e.stopPropagation();
		},

		render: function render() {
			return React.createElement(
				"button",
				{ className: this.props.triggerButtonClass, onClick: this.handleClick },
				this.props.triggerButtonContent
			);
		}
	});

	window.MyReact.InfoPopover = React.createClass({
		displayName: "InfoPopover",

		propTypes: {
			title: PT.string.isRequired
		},
		mixins: [PopoverMixin],

		handleClick: function handleClick(e) {
			e.stopPropagation();
		},

		render: function render() {
			var inline = { display: "inline-block" };
			return React.createElement(
				"button",
				{ style: inline, className: "btn btn-default btn-xs", onClick: this.handleClick },
				React.createElement("span", { className: "glyphicon glyphicon-info-sign" })
			);
		}
	});

	window.MyReact.Panel = React.createClass({
		displayName: "Panel",

		propTypes: {
			title: PT.object.isRequired,
			info: PT.object.isRequired
		},

		getInitialState: function getInitialState() {
			return {
				open: true
			};
		},

		toggleState: function toggleState(e) {
			this.setState({ open: !this.state.open });
		},

		render: function render() {
			var chevron = "glyphicon glyphicon-chevron-" + (this.state.open ? "down" : "right");
			return React.createElement(
				"div",
				{ className: "bs-callout bs-callout-info" },
				React.createElement(
					"div",
					{ className: "panel" },
					React.createElement(
						"div",
						{ className: "panel-heading unselectable row", onClick: this.toggleState },
						React.createElement(
							"div",
							{ className: "panel-title unselectable col-sm-11" },
							React.createElement("span", { className: chevron, style: { fontSize: 12 } }),
							" ",
							this.props.title
						),
						React.createElement(
							"div",
							{ className: "float-right" },
							this.props.info
						)
					),
					this.state.open ? React.createElement(
						"div",
						{ className: "panel-body" },
						this.props.children
					) : false
				)
			);
		}
	});

	window.MyReact.PanelGroup = React.createClass({
		displayName: "PanelGroup",

		render: function render() {
			return React.createElement(
				"div",
				{ className: "panel-group" },
				" ",
				this.props.children,
				" "
			);
		}
	});
})();

},{}]},{},[1]);
