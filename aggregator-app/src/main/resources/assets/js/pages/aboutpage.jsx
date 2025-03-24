"use strict";
import classNames from "classnames";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var AboutPage = createReactClass({
  //fixme! - class AboutPage extends React.Component { 
  propTypes: {
    toStatistics: PT.func.isRequired,
  },

  toStatistics: function (e) {
    this.props.toStatistics(true);
    e.preventDefault();
    e.stopPropagation();
  },

  render: function () {
    return (<div>
      <div className="top-gap">
        <h1 style={{ padding: 15 }}>About</h1>
        <div className="col-md-6">
          <h3>People</h3>
          <ul>
            <li>Emanuel Dima</li>
            <li>Erik Körner</li>
            <li>Leif-Jöran Olsson</li>
            <li>Yana Panchenko</li>
            <li>Oliver Schonefeld</li>
            <li>Dieter Van Uytvanck</li>
          </ul>

          <h3>Statistics</h3>
          <button type="button" className="btn btn-default btn-lg" onClick={this.toStatistics} >
            <span className="glyphicon glyphicon-cog" aria-hidden="true"> </span>{" "}View server log</button>
        </div>

        <div className="col-md-6">
          <h3>Technology</h3>

          <p>The Aggregator uses the following software components:</p>

          <ul>
            <li><a href="http://dropwizard.io/">Dropwizard</a>{" "}
              (<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
            </li>
            <li><a href="http://eclipse.org/jetty/">Jetty</a>{" "}
              (<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
            </li>
            <li><a href="http://jackson.codehaus.org/">Jackson</a>{" "}
              (<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
            </li>
            <li><a href="https://jersey.java.net/">Jersey</a>{" "}
              (<a href="https://jersey.java.net/license.html#/cddl">CCDL 1.1</a>)
            </li>
            <li><a href="https://github.com/optimaize/language-detector">Optimaize Language Detector</a>{" "}
              (<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
            </li>
            <li><a href="http://poi.apache.org/">Apache POI</a>{" "}
              (<a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>)
            </li>
            <li><a href="http://jopendocument.org/">jOpenDocument</a>{" "}
              (<a href="http://www.gnu.org/licenses/gpl-3.0.txt">GPL 3.0</a>)
            </li>
          </ul>

          <ul>
            <li><a href="http://facebook.github.io/react/">React</a>{" "}
              (<a href="https://github.com/facebook/react/blob/master/LICENSE">MIT license</a>)
            </li>
            <li><a href="http://getbootstrap.com/">Bootstrap</a>{" "}
              (<a href="http://opensource.org/licenses/mit-license.html">MIT license</a>)
            </li>
            <li><a href="http://jquery.com/">jQuery</a>{" "}
              (<a href="http://opensource.org/licenses/mit-license.html">MIT license</a>)
            </li>
            <li><a href="http://glyphicons.com/">GLYPHICONS free</a>{" "}
              (<a href="https://creativecommons.org/licenses/by/3.0/">CC-BY 3.0</a>)
            </li>
            <li><a href="http://fortawesome.github.io/Font-Awesome/">FontAwesome</a>{" "}
              (<a href="http://opensource.org/licenses/mit-license.html">MIT</a>, <a href="http://scripts.sil.org/OFL">SIL Open Font License</a>)
            </li>
          </ul>
          <p>The federated content search icon is made by
            <a href="http://www.freepik.com" title="Freepik"> Freepik </a>
            from
            <a href="http://www.flaticon.com" title="Flaticon"> www.flaticon.com </a>
            and licensed under
            <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0"> CC BY 3.0 </a>
          </p>
        </div>
      </div>
    </div>);
  }
});

module.exports = AboutPage;
