var defaultDocsetName = "default";

function makePageTitle(packageName) {
  return packageName + " Documentation";
}

function setPageTitle(pageTitle) {
  $(document).prop('title', pageTitle);
}

function makeMainHeading(mainHeadingText) {
  let $body = $("body");
  let $titleElement = $("<h1>", {id: "title", className: "title"});
  $titleElement.text(mainHeadingText);
  return $body.append($titleElement);
}

function makeDocsetTitle(docsetName) {
  let $docsetTitle = $("<h2>", {id: docsetName+"-title", className: "docsetTitle"});
  $docsetTitle.text(docsetName)
  return $docsetTitle
}

function initDocsetElement(docsetName, guideDiv, docsetListElements) {
  let $div = $("<div>", {id: docsetName});
  $div.appendTo(guideDiv);
  if (docsetName !== defaultDocsetName) {
    let $docsetTitle = makeDocsetTitle(docsetName)
    $div.append($docsetTitle);
  }
  let $ul = $("<ul>", {id: docsetName+"-links", class: "docset-links"});
  $div.append($ul);
  docsetListElements.set(docsetName, $ul);
}

function makeDocsetLinkItem(version, status, docsetProps) {
  let linkText = "v" + version;
  if (typeof status !== 'undefined' && status !== '') {
    linkText = linkText + " (" + status + ")";
  }
  let $docsetLink = $("<a>", {href: docsetProps.baseUrl});
  $docsetLink.text(linkText);
  let $listEntry = $("<li>", {class: "docset-link"});
  return $listEntry.append($docsetLink);
}

function generateDocLinks() {
  // Get version info from static JSON file
  $.getJSON("version-info.json", function(data) {
    var pageTitle = makePageTitle(data.packageName);
    setPageTitle(pageTitle)
    var $title = makeMainHeading(pageTitle);

    // hashmap to hold list elements (by docset name)
    var docsetListElements = new Map();
    var $topLevelGuideDiv = $("<div>", {id: "guides"});
    $title.append($topLevelGuideDiv);

    // make a place for docsets with no name to live
    // ensures they will be listed before those with a name
    initDocsetElement(defaultDocsetName, $topLevelGuideDiv, docsetListElements);

    $.each(data.releases, function(index, rel) {
      let docsetVer = rel.version;
      let docsetStat = rel.status;
      //console.log(rel.version);
      $.each(rel.docsets, function(docset, docsetProps) {
        if (docset === "") {
          docsetName = defaultDocsetName;
        } else {
          docsetName = docsetProps.docsetName;
        }

        if (!docsetListElements.has(docsetName)) {
          initDocsetElement(docsetName, $topLevelGuideDiv, docsetListElements);
        }

        let $ul = docsetListElements.get(docsetName);
        $ul.append(makeDocsetLinkItem(docsetVer, docsetStat, docsetProps))
      });
    });
  });
}