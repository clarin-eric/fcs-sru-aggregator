/*!
 * FCS SRU Aggragator UI
 * @module lib/locales/en/querybuilder.js
 * @version 1.1.1
 * @license GPL-3.0-or-later
 * @see {@link https://github.com/clarin-eric/fcs-sru-aggregator-ui.git}
 */
import{n as e}from"../../rolldown-runtime.js";var t=e({buttonCancel:()=>o,buttonUseQueryAndClose:()=>s,default:()=>f,loading:()=>n,msgNumUnsupportedResources_one:()=>u,msgNumUnsupportedResources_other:()=>d,msgResourcesUnavailable:()=>c,msgResourcesUnavailable_nothing:()=>l,queryInputAriaLabel:()=>a,queryInputPlaceholder:()=>i,title:()=>r}),n=`Trying to load Query Builder extension ...`,r=`Query Builder`,i=`"Elephant"`,a=`Query text input field for visual query builder`,o=`Cancel`,s=`Use Query`,c=`For this query, {{count}} of {{total}} resources will not be available! Certain layers are not supported by some resources.`,l=`For this query, no resources ({{total}}) might be available! Certain layers are not supported by some resources.`,u=`<strong>{{name}}</strong>: 1 unsupported resource`,d=`<strong>{{name}}</strong>: {{count}} unsupported resources`,f={loading:n,title:r,queryInputPlaceholder:i,queryInputAriaLabel:a,buttonCancel:o,buttonUseQueryAndClose:s,msgResourcesUnavailable:c,msgResourcesUnavailable_nothing:l,msgNumUnsupportedResources_one:u,msgNumUnsupportedResources_other:d};export{t};