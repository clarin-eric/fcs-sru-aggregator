"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var Footer = createReactClass({
//fixme! - class Footer extends React.Component {
    propTypes: {
	VERSION: PT.string.isRequired,
    },

    toAbout: function(e) {
	main.toAbout(true);
	e.preventDefault();
	e.stopPropagation();
    },

    render: function() {
	    return (
		<div className="container" style={{textAlign:'center'}}>
		   <div className="row">
		      <div style={{position:'relative', float:'left'}}>
		         <div className="leftist" style={{position:'absolute'}}>
		            <div>
		               <a title="about" href="about" onClick={this.toAbout}>About</a>
		            </div>
		           <div style={{color:'#777'}}>{this.props.VERSION}</div>
		         </div>
		      </div>
		      <a title="CLARIN ERIC" href="https://www.clarin.eu/">
		         <img src="img/clarindLogo.png" alt="CLARIN ERIC logo" style={{height:60}}/>
		      </a>
		      <div style={{position:'relative', float:'right'}}>
		         <div className="rightist" style={{position:'absolute', right:'0'}}>
		            <a title="contact" href="mailto:fcs@clarin.eu">Contact</a>
		         </div>
		      </div>
		 </div>
	      </div>
	    );
    }
});

module.exports = Footer;
