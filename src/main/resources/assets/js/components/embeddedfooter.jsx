"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var EmbeddedFooter = createReactClass({
//fixme! - class EmbeddedFooter extends React.Component { 
    propTypes: {
	URLROOT: PT.string.isRequired,
    },

	render: function() {
		return (
			<div className="container" style={{textAlign:'center'}}>
				<div className="row">
					<div style={{position:'relative', float:'right'}}>
						<div className="rightist" style={{position:'absolute', right:0, width:170}}>
							<a href={this.props.URLROOT + "/"} target="_blank" tabIndex="-1">
								<img width="28px" height="28px" src="img/magglass1.png"/>
								<header className="inline float-left"> Content Search </header>
							</a>
						</div>
					</div>
				</div>
			</div>
		);
	}
});

module.exports = EmbeddedFooter;
