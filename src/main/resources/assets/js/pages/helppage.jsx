"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var HelpPage = createReactClass({
//fixme! - class HelpPage extends React.Component {
	openHelpDesk: function() {
		window.open('http://support.clarin-d.de/mail/form.php?queue=Aggregator&lang=en',
			'_blank', 'height=560,width=370');
	},

	render: function() {
		return	(
			<div>
				<div className="top-gap">
					<h1>Help</h1>
					<h3>Performing search in Federated Content Search corpora</h3>
					<p>To perform a simple keyword search in all CLARIN Federated Content Search centres
					and their corpora, go to the search field at the top of the page,
					enter your query, and click the 'search' button or press the 'Enter' key.</p>
					<p>To perform an advanced search on multiple annotation layers in CLARIN Federated Content Search centres that support this, switch to Multi-layer Federated Content Search (FCS) in the dropdown list, enter a FCS query in the search field at the top of the page, and click the 'search' button or press the 'Enter' key.</p>

					<p>When the search starts, the page will start filling in with the corpora responses.
					After the entire search process has ended you have the option to download the results
					in various formats.
					</p>

					<p>If you are particularly interested in the results returned by a corpus, you have
					the option to focus only on the results of that corpus, by clicking on the 'Watch' button.
					In this view mode you can also download the results of use the WebLicht processing services
					to further analyse the results.</p>


					<h3>Adjusting search criteria</h3>
					<p>The FCS Aggregator makes possible to select specific corpora based on their name
					or language and to specify the number of search results (hits) per corpus per page.
					The user interface controls that allows to change these options are located
					right below the search fiels on the main page. The current options are
					to filter resources based on their language, to select specific resources, and
					to set the maximum number of hits. In the multi-layer FCS search the supported layers filter on the supported features like, e. g. part of speech or lemma in addition to the other filter options.</p>


					<h3>More help</h3>
					<p>More detailed information on using FCS Aggregator is available at the &nbsp;
					<a href="http://weblicht.sfs.uni-tuebingen.de/weblichtwiki/index.php/FCS_Aggregator">
						Aggregator wiki page
					</a>.
					If you still cannot find an answer to your question,
					or if want to send a feedback, you can write to Clarin-D helpdesk: </p>
					<button type="button" className="btn btn-default btn-lg" onClick={this.openHelpDesk} >
						<span className="glyphicon glyphicon-question-sign" aria-hidden="true"></span>
						&nbsp;HelpDesk
					</button>
				</div>
			</div>
		);
	}
});

module.exports = HelpPage;
