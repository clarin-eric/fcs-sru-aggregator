"use strict";
import classNames from "classnames";
import SearchResourceBox from "./searchresourcebox.jsx";
import PropTypes from "prop-types";
import createReactClass from "create-react-class";

var PT = PropTypes;

var ResourceView = createReactClass({
  //fixme! - class ResourceView extends React.Component {
  propTypes: {
    resources: PT.object.isRequired,
    languageMap: PT.object.isRequired,
  },

  getInitialState() {
    const resources = this.props.resources;
    const resourcesGroupedByInstitute = this.updateResourcesGroupedByInstitute(resources);
    const resourcesGroupedByLanguage = this.updateResourcesGroupedByLanguage(resources);

    return {
      viewSelected: false,  // only show the selected resources
      //showDisabled: false,  // don't hide items with {visible = false} // implemented, but out commented feature...
      viewGroupedByInstitution: false,  // group by institution, then as usual
      viewGroupedByLanguage: false,  // group by (single) language, then as usual
      resourcesGroupedByInstitute: resourcesGroupedByInstitute,  // group info (is-expanded, resources list)
      resourcesGroupedByLanguage: resourcesGroupedByLanguage,  // group info (with language as key) (is-expanded, resources list)
      resources: resources,  // cached but unused, just to check for updates
    }
  },

  componentWillReceiveProps(nextProps) {
    // console.debug("componentWillReceiveProps", nextProps);
    const resources = nextProps.resources;
    if (this.props.resources == resources) {
      return;
    }
    const resourcesGroupedByInstitute = this.updateResourcesGroupedByInstitute(resources);
    const resourcesGroupedByLanguage = this.updateResourcesGroupedByLanguage(resources);
    this.setState({
      resources: this.props.resources,
      resourcesGroupedByInstitute: resourcesGroupedByInstitute,
      resourcesGroupedByLanguage: resourcesGroupedByLanguage,
    });
  },

  updateResourcesGroupedByInstitute: function (resources) {
    const resourcesGroupedByInstitute = {};
    resources.resources.forEach(resource => {
      const institute = resource.institution;
      if (!resourcesGroupedByInstitute.hasOwnProperty(institute)) {
        resourcesGroupedByInstitute[institute] = { expanded: true, resources: [] };
      }
      resourcesGroupedByInstitute[institute].resources.push(resource);
    });
    //console.debug(resourcesGroupedByInstitute);
    return resourcesGroupedByInstitute;
  },

  updateResourcesGroupedByLanguage: function (resources) {
    const resourcesGroupedByLanguage = {};
    resources.resources.forEach(resource => {
      resource.languages.forEach(language => {
        if (!resourcesGroupedByLanguage.hasOwnProperty(language)) {
          resourcesGroupedByLanguage[language] = { expanded: true, resources: [] };
        }
        resourcesGroupedByLanguage[language].resources.push(resource);
      });
    });
    //console.debug(resourcesGroupedByLanguage);
    return resourcesGroupedByLanguage;
  },

  toggleSelection: function (resource, e) {
    var s = !resource.selected;
    this.props.resources.recurseResource(resource, function (r) { r.selected = s; });
    this.props.resources.update();
    this.stop(e);
  },

  toggleViewSelected(evt) {
    this.setState((st) => ({ viewSelected: !st.viewSelected }));
  },

  toggleShowDisabled(evt) {
    this.setState((st) => ({ showDisabled: !st.showDisabled }));
  },

  toggleViewGroupByInstitution(evt) {
    this.setState((st) => ({
      viewGroupedByInstitution: !st.viewGroupedByInstitution,
      viewGroupedByLanguage: false,
    }));
  },

  toggleViewGroupByLanguage(evt) {
    this.setState((st) => ({
      viewGroupedByInstitution: false,
      viewGroupedByLanguage: !st.viewGroupedByLanguage,
    }));
  },

  toggleDescExpansion: function (resource) {
    resource.descExpanded = !resource.descExpanded;
    this.props.resources.update();
  },

  toggleExpansion: function (resource) {
    resource.expanded = !resource.expanded;
    this.props.resources.update();
  },

  toggleExpansionGrouped: function (groupedResources) {
    groupedResources.expanded = !groupedResources.expanded;
    this.setState({
      resourcesGroupedByInstitute: this.state.resourcesGroupedByInstitute,
      resourcesGroupedByLanguage: this.state.resourcesGroupedByLanguage,
    });
  },

  selectAll: function (value) {
    // select all _visible_
    this.props.resources.recurse(function (c) { c.visible ? c.selected = value : false });
    this.props.resources.update();
  },

  selectAllFromList: function (resources, value) {
    // like selectAll(), just for list of resources
    this.props.resources.recurseResources(resources, function (r) { r.visible ? r.selected = value : false });
    this.props.resources.update();
  },

  selectAllShown: function (value) {
    // select only visible/shown resources, i.e. resources that are shown in dialog, possibly filtered due to query
    this.props.resources.recurse(function (r) { r.visible && r.priority > 0 ? r.selected = value : false });
    this.props.resources.update();
  },

  searchResource: function (query) {
    // sort fn: descending priority, stable sort
    var sortFn = function (a, b) {
      if (b.priority === a.priority) {
        return b.index - a.index; // stable sort
      }
      return b.priority - a.priority;
    };

    query = query.toLowerCase();
    if (!query) {
      this.props.resources.recurse(function (resource) { resource.priority = 1; });
      this.props.resources.update();
      return;
    }

    // clean up all priorities
    this.props.resources.recurse(function (resource) {
      resource.priority = 0;
    });

    // find priority for each resource
    var querytokens = query.split(" ").filter(function (x) { return x.length > 0; });
    this.props.resources.recurse(function (resource) {
      var title = resource.title;
      querytokens.forEach(function (qtoken) {
        if (title && title.toLowerCase().indexOf(qtoken) >= 0) {
          resource.priority++;
        }
        if (resource.description && resource.description.toLowerCase().indexOf(qtoken) >= 0) {
          resource.priority++;
        }
        if (resource.institution &&
          resource.institution.toLowerCase().indexOf(qtoken) >= 0) {
          resource.priority++;
        }
        if (resource.languages) {
          resource.languages.forEach(function (lang) {
            if (lang.toLowerCase().indexOf(qtoken) >= 0) {
              resource.priority++;
            }
          });
          resource.languages.forEach(function (lang) {
            if (this.props.languageMap[lang].toLowerCase().indexOf(qtoken) >= 0) {
              resource.priority++;
            }
          }.bind(this));
        }
      }.bind(this));
    }.bind(this));

    // ensure parents of visible resources are also visible; maximum depth = 3
    var isVisibleFn = function (resource) { return resource.priority > 0; };
    var parentBooster = function (resource) {
      if (resource.priority <= 0 && resource.subResources) {
        if (resource.subResources.some(isVisibleFn)) {
          resource.priority = 0.5;
        }
      }
    };
    for (var i = 3; i > 0; i--) {
      this.props.resources.recurse(parentBooster);
    }

    this.props.resources.recurse(function (resource) { resource.subResources.sort(sortFn); });
    this.props.resources.resources.sort(sortFn);

    // display
    this.props.resources.update();
  },

  stop: function (e) {
    e.stopPropagation();
  },

  getMinMaxPriority: function () {
    var min = 1, max = 0;
    this.props.resources.recurse(function (c) {
      if (c.priority < min) min = c.priority;
      if (max < c.priority) max = c.priority;
    });
    return [min, max];
  },

  renderCheckbox: function (resource) {
    return <button className="btn btn-default">
      {resource.selected ?
        <span className="glyphicon glyphicon-check" aria-hidden="true" /> :
        <span className="glyphicon glyphicon-unchecked" aria-hidden="true" />
      }
    </button>;
  },

  renderExpansion: function (resource) {
    if (!resource.subResources || resource.subResources.length === 0) {
      return false;
    }
    return <div className="expansion-handle" onClick={this.toggleExpansion.bind(this, resource)}>
      <a>
        {resource.expanded ?
          <span className="glyphicon glyphicon-minus" aria-hidden="true" /> :
          <span className="glyphicon glyphicon-plus" aria-hidden="true" />
        }
        {resource.expanded ? " Collapse " : " Expand "} ({resource.subResources.length} subresources)
      </a>
    </div>;
  },

  renderExpansionGrouped: function (groupedResources) {
    if (!groupedResources.resources || groupedResources.resources.length === 0) {
      return false;
    }

    var selectedCount = 0;
    this.props.resources.recurseResources(groupedResources.resources, c => {
      if (c.selected && c.visible) selectedCount++;
    });

    return <div className="expansion-handle" onClick={this.toggleExpansionGrouped.bind(this, groupedResources)}>
      <a>
        {groupedResources.expanded ?
          <span className="glyphicon glyphicon-minus" aria-hidden="true" /> :
          <span className="glyphicon glyphicon-plus" aria-hidden="true" />
        }
        {groupedResources.expanded ? " Collapse " : " Expand "} ({groupedResources.resources.length} root resource{groupedResources.resources.length != 1 ? "s" : ""}, {selectedCount} (sub)resource{selectedCount != 1 ? "s" : ""} selected)
      </a>
    </div>;
  },

  renderSelectionButtonsGrouped: function (resources) {
    return (<div className="float-right inline" style={{ paddingTop: "1.5em" }}>
      <button className="btn btn-default" style={{ marginRight: 10 }} onClick={this.selectAllFromList.bind(this, resources, true)}>
        {" Select all"}</button>
      <button className="btn btn-default" style={{ marginRight: 20 }} onClick={this.selectAllFromList.bind(this, resources, false)}>
        {" Deselect all"}</button>
    </div>);
  },

  renderLanguages: function (languages) {
    return languages
      .map(function (l) { return this.props.languageMap[l]; }.bind(this))
      .sort()
      .join(", ");
  },

  shouldShowItem(level, resource) {
    if (this.state.viewSelected && !resource.selected) {
      return false;
    }
    if (!this.state.showDisabled && !resource.visible) {
      return false;
    }
    // normal search filter.
    if (level === 0 && resource.priority <= 0) {
      return false;
    }

    return true;
  },

  renderFilteredMessage() {
    var total = 0;
    var visible = 0;
    this.props.resources.recurse((resource) => {
      if (resource.visible || this.state.showDisabled) {
        total++;
        if (this.shouldShowItem(0, resource)) {
          visible++;
        }
      }
    });
    if (visible === total) {
      return false;
    }
    if (visible === 0) {
      return false; // we do have an "empty" message anyway
    }
    return <div> Showing {visible} out of {total} (sub)resource{total != 1 ? "s" : ""}. </div>;
  },

  renderResource: function (level, minmaxp, resource) {
    if (!this.shouldShowItem(level, resource)) {
      return;
    }

    var indent = { marginLeft: level * 50 };
    var resourceContainerClass = "resource-container " + (resource.priority > 0 ? "" : "dimmed");

    var hue = 120 * resource.priority / minmaxp[1];
    var color = minmaxp[0] === minmaxp[1] ? 'transparent' : 'hsl(' + hue + ', 50%, 50%)';
    var priorityStyle = { paddingBottom: 4, paddingLeft: 2, borderBottom: '3px solid ' + color };
    var expansive = resource.descExpanded ? { overflow: 'hidden' }
      : { whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' };
    var isResourceRestricted = resource.availabilityRestriction && resource.availabilityRestriction !== "NONE";
    var canRessourceBeAccessed = !isResourceRestricted || window.MyAggregator.isLoggedIn;
    var availabilityRestrictionIconClasses = "fa " + (canRessourceBeAccessed ? "fa-unlock" : "fa-lock");
    return <div className={resourceContainerClass} key={resource.id}>
      <div className="row resource" onClick={this.toggleDescExpansion.bind(this, resource)}>
        <div className="col-sm-1 vcenter">
          <div className="inline" style={priorityStyle} onClick={this.toggleSelection.bind(this, resource)}>
            {this.renderCheckbox(resource)}
          </div>
        </div>
        <div className="col-sm-8 vcenter">
          <div style={indent}>
            <h3 style={expansive}>
              {resource.title}
              {isResourceRestricted ? <i className={availabilityRestrictionIconClasses} style={{ marginLeft: '6px', marginRight: '2px' }} title="This resource requires authentication!" /> : null}
              {resource.landingPage ?
                <a href={resource.landingPage} onClick={this.stop}>
                  <span style={{ fontSize: 12 }}> – Homepage </span>
                  <i className="glyphicon glyphicon-home" />
                </a> : false}
            </h3>


            <p style={expansive}>{resource.description}</p>
            {this.renderExpansion(resource)}
          </div>
        </div>
        <div className="col-sm-3 vcenter">
          <p style={expansive}>
            <i className="fa fa-institution" /> {resource.institution}
          </p>
          <p style={expansive}>
            <i className="fa fa-language" /> {this.renderLanguages(resource.languages)}
          </p>
        </div>
      </div>
      {resource.expanded ? resource.subResources.map(this.renderResource.bind(this, level + 1, minmaxp)) : false}
    </div>;
  },

  renderCorpList() {
    var minmaxp = this.getMinMaxPriority();

    const corpListRender = [];

    // this is so we get a non-undefined items .length in corpListRender.
    this.props.resources.resources.forEach(c => {
      var rend = this.renderResource(0, minmaxp, c);
      if (rend) corpListRender.push(rend);
    });

    return <div className="resourceview-resources">
      {corpListRender.length > 0 ? corpListRender :
        <h3 className="aligncenter">{
          this.state.viewSelected ? "No resources selected yet!" : "No resources found."
        }</h3>
      }
    </div>

  },

  renderCorpListGroupedByInstitution() {
    var minmaxp = this.getMinMaxPriority();

    const groupedListRender = [];
    Object.entries(this.state.resourcesGroupedByInstitute).forEach(([institution, groupedResources]) => {
      const corpListRender = [];
      // this is so we get a non-undefined items .length in corpListRender.
      groupedResources.resources.forEach(c => {
        var rend = this.renderResource(0, minmaxp, c);
        if (rend) corpListRender.push(rend);
      });
      if (corpListRender.length > 0) {
        groupedListRender.push(<div className="resourceview-resources">
          {this.renderSelectionButtonsGrouped(groupedResources.resources)}
          <h3 style={{ paddingTop: "0.5em" }}><i class="fa fa-institution" /> {institution}</h3>
          {this.renderExpansionGrouped(groupedResources)}
          {groupedResources.expanded ? corpListRender : false}
        </div>);
      }
    });

    return <div className="resourceview-institutions">
      {groupedListRender.length > 0 ? groupedListRender :
        <h3 className="aligncenter">{
          this.state.viewSelected ? "No resources selected yet!" : "No resources found."
        }</h3>
      }
    </div>
  },

  renderCorpListGroupedByLanguage() {
    var minmaxp = this.getMinMaxPriority();

    const groupedListRender = [];
    Object.entries(this.state.resourcesGroupedByLanguage).forEach(([language, groupedResources]) => {
      const corpListRender = [];
      // this is so we get a non-undefined items .length in corpListRender.
      groupedResources.resources.forEach(c => {
        var rend = this.renderResource(0, minmaxp, c);
        if (rend) corpListRender.push(rend);
      });
      if (corpListRender.length > 0) {
        groupedListRender.push(<div className="resourceview-resources">
          {this.renderSelectionButtonsGrouped(groupedResources.resources)}
          <h3 style={{ paddingTop: "0.5em" }}><i class="fa fa-language" /> {this.props.languageMap[language]} [{language}]</h3>
          {this.renderExpansionGrouped(groupedResources)}
          {groupedResources.expanded ? corpListRender : false}
        </div>);
      }
    });

    return <div className="resourceview-languages">
      {groupedListRender.length > 0 ? groupedListRender :
        <h3 className="aligncenter">{
          this.state.viewSelected ? "No resources selected yet!" : "No resources found."
        }</h3>
      }
    </div>
  },

  render() {
    var selectedCount = 0;
    //var disabledCount = 0;
    this.props.resources.recurse(c => {
      if (c.selected && c.visible) selectedCount++;
      //if (c.selected) selectedCount++;
      //if (!c.visible) disabledCount++;
    });

    var renderResourcesFn = null;
    if (this.state.viewGroupedByInstitution) {
      renderResourcesFn = this.renderCorpListGroupedByInstitution;
    } else if (this.state.viewGroupedByLanguage) {
      renderResourcesFn = this.renderCorpListGroupedByLanguage;
    } else {
      renderResourcesFn = this.renderCorpList;
    }

    return <div style={{ margin: "0 30px" }}>
      <div className="row">
        {/*
        <div className="float-left inline">
          <h3 style={{marginTop: 0}}>
            {this.props.resources.getSelectedMessage()}
          </h3>
        </div>
        */}

        <div className="float-left inline resourceview-filter-buttons">
          <div className="btn-group btn-group-toggle" >

            <label className={"btn btn-light btn " + (this.state.viewSelected ? 'active' : 'inactive')} onClick={this.toggleViewSelected} title="View selected resources">
              <span className={this.state.viewSelected ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} /> View selected ({selectedCount})
            </label>
            {/*
            <label className={"btn btn-light btn-sm " + (this.state.showDisabled ? 'active':'inactive')} onClick={this.toggleShowDisabled} label="Toggle showing of resources disabled in this search mode">
              <span className={this.state.showDisabled ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} />  Show disabled ({disabledCount})
            </label>
            */}
            <label className={"btn btn-light btn"} style={{ paddingRight: "0ex", pointerEvents: "none" }}>Group by </label>
            <label className={"btn btn-light btn " + (this.state.viewGroupedByInstitution ? 'active' : 'inactive')} style={{ paddingRight: "0.5ex", paddingLeft: "0.5ex" }} onClick={this.toggleViewGroupByInstitution} title="Group resources by institution">
              <span className={this.state.viewGroupedByInstitution ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} /> Institution
            </label>
            <label className={"btn btn-light btn " + (this.state.viewGroupedByLanguage ? 'active' : 'inactive')} style={{ paddingLeft: "0.5ex" }} onClick={this.toggleViewGroupByLanguage} title="Group resources by language">
              <span className={this.state.viewGroupedByLanguage ? "glyphicon glyphicon-check" : "glyphicon glyphicon-unchecked"} /> Language
            </label>
          </div>
        </div>

        <div className="float-right inline">
          <button className="btn btn-default" style={{ marginRight: 10 }} onClick={this.selectAll.bind(this, true)}>
            {" Select all"}</button>
          <button className="btn btn-default" style={{ marginRight: 10 }} onClick={this.selectAllShown.bind(this, true)}>
            {" Select visible"}</button>
          <button className="btn btn-default" style={{ marginRight: 20 }} onClick={this.selectAll.bind(this, false)}>
            {" Deselect all"}</button>
        </div>
        <div className="float-right inline">
          <div className="inline" style={{ marginRight: 20 }} >
            <SearchResourceBox search={this.searchResource} />
          </div>
        </div>
      </div>
      <div className="row" style={{ marginBottom: 15 }}>
        {this.renderFilteredMessage()}
      </div>
      {renderResourcesFn()}
    </div>;
  }
});

module.exports = ResourceView;
