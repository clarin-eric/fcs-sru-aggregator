"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var LanguageSelector = createReactClass({
	propTypes: {
		anyLanguage: PT.array.isRequired,
		languageMap: PT.object.isRequired,
		selectedLanguage: PT.array.isRequired,
		languageFilter: PT.string.isRequired,
		languageChangeHandler: PT.func.isRequired,
	},

	selectLang: function(language) {
		this.props.languageChangeHandler(language, this.props.languageFilter);
	},

	setFilter: function(filter) {
		this.props.languageChangeHandler(this.props.selectedLanguage, filter);
	},

	renderLanguageObject: function(lang) {
		var desc = lang[1] + " [" + lang[0] + "]";
		var style = {
			whiteSpace: "nowrap",
			fontWeight: lang[0] === this.props.selectedLanguage[0] ? "bold":"normal",
		};
		return	(<div key={lang[0]}>
					<a tabIndex="-1" href="#" style={style} onClick={this.selectLang.bind(this, lang)}>{desc}</a>
				</div>);
	},

	renderRadio: function(option) {
		return	this.props.languageFilter === option ?
				<input type="radio" name="filterOpts" value={option} checked onChange={this.setFilter.bind(this, option)}/>
				: <input type="radio" name="filterOpts" value={option} onChange={this.setFilter.bind(this, option)} />;
	},

	render: function() {
		var languages = _.pairs(this.props.languageMap)
						 .sort(function(l1, l2){return l1[1].localeCompare(l2[1]); });
		languages.unshift(this.props.anyLanguage);
		languages = languages.map(this.renderLanguageObject);
		var third = Math.round(languages.length/3);
		var l1 = languages.slice(0, third);
		var l2 = languages.slice(third, 2*third);
		var l3 = languages.slice(2*third, languages.length);

		return	(<div>
					<div className="row">
						<div className="col-sm-4">{l1}</div>
						<div className="col-sm-4">{l2}</div>
						<div className="col-sm-4">{l3}</div>
						<div className="col-sm-12" style={{marginTop:10, marginBottom:10, borderBottom:"1px solid #eee"}}/>
					</div>
					<form className="form" role="form">
						<div className="input-group">
							<div>
							<label style={{color:'black'}}>
								{ this.renderRadio('byMeta') }{" "}
								Use the collections{"'"} specified language to filter results
							</label>
							</div>
							<div>
							<label style={{color:'black'}}>
								{ this.renderRadio('byGuess') }{" "}
								Filter results by using a language detector
							</label>
							</div>
							<div>
							<label style={{color:'black'}}>
								{ this.renderRadio('byMetaAndGuess') }{" "}
								First use the collections{"'"} specified language then also use a language detector
							</label>
							</div>
						</div>
					</form>
				</div>);
	}
});

module.exports = LanguageSelector;
