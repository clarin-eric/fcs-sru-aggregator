/** @jsx React.DOM */

var PT = React.PropTypes;
var ReactCSSTransitionGroup = React.addons.ReactCSSTransitionGroup;
var ReactTransitionGroup = React.addons.TransitionGroup;

var PopoverMixin = {
	getDefaultProps: function(){
		return {hasPopover: true};
	},
 
	componentDidMount: function() {
		var content;
		if (Array.isArray(this.props.children))
			content = this.props.children.map(React.renderToString).join(" ");
		else 
			content = React.renderToString(this.props.children);
		$(this.getDOMNode()).popover({
			content: content,
			animation: this.props.animation,
			placement: this.props.placement,
			title: this.props.title,
			trigger: 'click',
			html: true,
		});
	}
};

var InfoPopover = React.createClass({
	propTypes: {
		title: PT.string.isRequired,
	},
	mixins: [PopoverMixin],

	handleClick: function(e) {
		e.stopPropagation();
	},

	render: function() {
		return	<button className="btn btn-default btn-xs" onClick={this.handleClick}>
					<span className="glyphicon glyphicon-info-sign"/>
				</button>;
	}
});

window.MyReact = {};
window.MyReact.Panel = React.createClass({
	propTypes: {
		corpus:PT.object.isRequired,
	},

	getInitialState: function() {
		return {
			open: true,
		};
	},

	toggleState: function(e) {
		this.setState({open: !this.state.open});
	},

	renderBody: function() {
		return this.state.open ? 
					<div className="panel-body">{this.props.children}</div> : 
					false;
	},

	renderInfo: function() {
		return	<dl className="dl-horizontal">
					<dt>Institution</dt>
					<dd>{this.props.corpus.institution.name}</dd>

					{this.props.corpus.description ? <dt>Description</dt>:false}
					{this.props.corpus.description ? <dd>{this.props.corpus.description}</dd>: false}

					{this.props.corpus.landingPage ? <dt>Landing Page</dt> : false }
					{this.props.corpus.landingPage ? 
						<dd><a href={this.props.corpus.landingPage}>{this.props.corpus.landingPage}</a></dd>:
						false}

					<dt>Languages</dt>
					<dd>{this.props.corpus.languages.join(", ")}</dd>
				</dl>;
	},

	render: function() {
		var chevron = "glyphicon glyphicon-chevron-" + (this.state.open ? "down":"right");
		var chevronStyle={fontSize:12};
		var right={float:"right"};
		return 	<div className="bs-callout bs-callout-info"> 
					<div className="panel">
						<div className="panel-heading unselectable row" onClick={this.toggleState}>
							<div className="panel-title unselectable col-sm-11">
								<span className={chevron} style={chevronStyle} />&nbsp;
								{this.props.corpus.displayName}
							</div>
							<div style={right}>
								<InfoPopover placement="left" title={this.props.corpus.displayName}>
									{this.renderInfo()}
								</InfoPopover>
							</div>
						</div>
						{this.renderBody()}
					</div>
				</div>;
	}
});

window.MyReact.PanelGroup = React.createClass({
	render: function() {
		return	<div className="panel-group"> {this.props.children} </div>;
	},
});
