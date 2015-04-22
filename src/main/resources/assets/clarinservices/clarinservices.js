(function() {
"use strict";

var service_list = [
	[
		{
			href: 'http://www.clarin.eu/portal',
			image: 'https://www.clarin.eu/sites/default/files/portal_small.png',
			title: "CLARIN portal",
			description: "Quick introduction to CLARIN, giving an impression about what's currently available",
		},
		{
			href: 'http://www.clarin.eu/services/depositing',
			image: 'http://www.clarin.eu/sites/default/files/long-term-archive.jpg',
			title: "Depositing services",
			description: "Store language resources in a sustainable repository at a CLARIN centre",
		},
		{
			href: 'http://www.clarin.eu/vlo',
			image: 'https://www.clarin.eu/sites/default/files/vlo_small.png',
			title: "Virtual Language Observatory",
			description: "Discover language resources using a faceted browser or a map",
		},
	],
	[
		{
			href: 'http://www.clarin.eu/content/easy-access-protected-resources',
			image: 'http://www.clarin.eu/sites/default/files/federated-identity.jpg',
			title: "Easy federated access",
			description: "Get easy access to protected resources, with your institutional username and password.",
		},
		{
			href: 'http://www.clarin.eu/content/web-services',
			image: 'https://www.clarin.eu/sites/default/files/webservice.png',
			title: "Web services and applications",
			description: "Explore and analyze language data with a wide variety of tools",
		},
		{
			href: 'http://www.clarin.eu/content/virtual-collections',
			image: 'https://www.clarin.eu/sites/default/files/virtualcollection.png',
			title: "Virtual Collections",
			description: "Create your own digital bookmarks, ideal for citing data sets.",
		},
	],
	[
		{
			href: 'http://www.clarin.eu/content/language-resource-inventory',
			image: 'https://www.clarin.eu/sites/default/files/lrt-inventory_small.png',
			title: "Language Resource Inventory",
			description: "Submit and access information about language resources relevant to your research."
		},
		{
			href: 'http://www.clarin.eu/content/content-search',
			image: 'https://www.clarin.eu/sites/default/files/search.png',
			title: "Content Search (prototype version)",
			description: "Search different corpora with a single search engine"
		},
		{
			href: 'http://www.clarin.eu/contact',
			image: 'https://www.clarin.eu/sites/default/files/questionmark.png',
			title: "Consulting Services",
			description: "Searching for a specific data set or application? Wondering how CLARIN can assist your research? Feel free to contact us!"
		},
	],
];

var entityMap = {
	"&": "&amp;",
	"<": "&lt;",
	">": "&gt;",
	'"': '&quot;',
	"'": '&#39;',
	"/": '&#x2F;'
};

function escapeHtml(string) {
	return String(string).replace(/[&<>"'\/]/g, function (s) {
		return entityMap[s];
	});
}

function renderCell(service) {
	var href = escapeHtml(service.href);
	var image = escapeHtml(service.image);
	var title = escapeHtml(service.title);
	var description = escapeHtml(service.description);

	var p_image	=	"<img src='" + image + "'>";
	var p_title	=	"<div>" + title + "</div>";

	return "<td><a href='" + href + "'><div>" +
				p_image + p_title + /* p_description + */
			"</div></a></td>";
}

function renderRow(services) {
	return "<tr>" + services.map(renderCell).join('') + "</tr>";
}

function renderDropdown(service_table) {
	var rows = service_list.map(renderRow).join('');
	var tablediv = "<div><table><tbody>" + rows + "</tbody></table></div>";
	return '<div class="dropdown">' +
		'<button class="btn dropdown-toggle" type="button"'+
				' id="dropdown-clarinservices" data-toggle="dropdown" aria-expanded="true">'+
			// '<span class="glyphicon glyphicon-th"></span>'+
			'<img src="img/moreapp60.png"></img>'+
		'</button>'+
		'<ul class="dropdown-menu" role="menu" aria-labelledby="dropdown-clarinservices">'+
			tablediv +
		'</ul>'+
	'</div>';
}

document.getElementById('clarinservices').innerHTML = renderDropdown(service_list);

})();
