function init(horizExtentWKT) {
    initMap(horizExtentWKT);
    addEventListeners();
    resetForm();
}

function initMap(horizExtentWKT) {
    if (typeof OpenLayers === "undefined") {
        console.log("Could not load OpenLayers Javascript library, likely because there's no internet connection.");
        return;
    }

    var wmsLayer = new OpenLayers.Layer.WMS("OpenLayers WMS", "http://vmap0.tiles.osgeo.org/wms/vmap0",
        { layers: 'basic' }, { wrapDateLine: true }
    );

    var vectorLayer = new OpenLayers.Layer.Vector({
        renderers: [ "Canvas" ]
    });

    var wkt = new OpenLayers.Format.WKT();
    vectorLayer.addFeatures(wkt.read(horizExtentWKT));

    var map = new OpenLayers.Map('map');  // Attach to the element with the 'map' id.
    map.addLayers([ wmsLayer, vectorLayer ]);

    map.zoomToExtent(new OpenLayers.Bounds(
        vectorLayer.getDataExtent().left,
        vectorLayer.getDataExtent().bottom,
        vectorLayer.getDataExtent().right,
        vectorLayer.getDataExtent().top));

    var mousePosition = new OpenLayers.Control.MousePosition();
    mousePosition.numDigits = 2;
    mousePosition.separator = ", ";
    mousePosition.prefix = "<span style='background-color: rgba(255, 255, 255, 0.5)'>(";
    mousePosition.suffix = ")</span>";
    mousePosition.emptyString = `${mousePosition.prefix}Mouse is not over map${mousePosition.suffix}`;

    map.addControl(mousePosition);
}

function addEventListeners() {
    // Any time an input is changed or a button is clicked, rebuild the access URL.
    for (let inputTag of getDescendantsWithTagNames(document.body, ["input", "button", "select", "textarea"])) {
        var inputTagName = inputTag.tagName.toLowerCase();
        var inputTagType = inputTag.getAttribute("type");

        if (inputTagName == "select") {
            inputTag.addEventListener("change", buildAccessUrl)
        } else if (inputTagName == "input" && inputTagType == "text" || inputTagName == "textarea") {
            inputTag.addEventListener("input", buildAccessUrl)
        } else {
            // inputTag is some kind of button.
            if (!inputTag.classList.contains("accordionButton")) {  // Don't buildAccessUrl() for accordion buttons.
                inputTag.addEventListener("click", buildAccessUrl);
            }
        }
    }
}

function resetForm() {
    document.getElementById("form").reset();

    clickAllButtonsWithClass("defaultButton");
    clickAllButtonsWithClass("resetButton");
    setAllAccordionsVisible(false);
}

// Programmatically click all buttons that have the specified class.
function clickAllButtonsWithClass(className) {
    for (let button of document.getElementsByClassName(className)) {
        button.click();
    }
}

//////////////////////////////////////////////////////////////////////

function buildAccessUrl() {
    // Exclude the NCSS page (usually named something like "/dataset.html") from the end of the URL, leaving only
    // the URL of the dataset.
    var datasetUrl = document.URL.substring(0, document.URL.lastIndexOf("/"));

    var formData = new FormData(document.getElementById("form"));
    var queryString = "";

    for (const entry of formData) {
        if (queryString != "") {
            queryString += "&";
        }

        // We intentionally do no filtering of elements, even ones with no values, because we want to display
        // EXACTLY the URL that the form will submit.
        queryString += `${entry[0]}=${entry[1]}`
    };

    var accessUrl = encodeURI(`${datasetUrl}?${queryString}`);
    document.getElementById("urlBuilder").innerHTML = accessUrl;
}

//////////////////////////////////////////////////////////////////////

function toggleAccordion(accordionButton) {
    var buttonIsActive = accordionButton.classList.contains("active");
    setAccordionVisible(accordionButton, !buttonIsActive);
}

function setAccordionVisible(accordionButton, visible) {
    // accordionIcon is a child of accordionButton.
    var accordionIcon = accordionButton.getElementsByClassName("accordionIcon")[0];
    // accordionElem is a sibling of accordionButton.
    var accordionElem = accordionButton.parentNode.getElementsByTagName("ul")[0];

    if (visible) {
        accordionButton.classList.add("active");     // If "active" is already in classList, this is a no-op.
        accordionIcon.innerHTML = "&#x25b2;";        // Unicode up arrow.
        accordionElem.style.display = "block";       // Show accordionElem.
    } else {
        accordionButton.classList.remove("active");  // If "active" isn't in classList, this is a no-op.
        accordionIcon.innerHTML = "&#x25bc;";        // Unicode down arrow.
        accordionElem.style.display = "none";        // Hide accordionElem.
    }
}

function setAllAccordionsVisible(visible) {
    for (let accordionButton of document.getElementsByClassName("accordionButton")) {
        setAccordionVisible(accordionButton, visible);
    }
}

//////////////////////////////////////////////////////////////////////

function selectTab(selectedTabPane, clickedTabButton) {
    // Remove the "active" class from all buttons that are siblings of clickedTabButton.
    for (let siblingTabButton of clickedTabButton.parentNode.getElementsByTagName("button")) {
        siblingTabButton.classList.remove("active")
    }

    // Make clickedTabButton active.
    clickedTabButton.classList.add("active");

    // Hide the tab panes that are siblings of selectedTabPane and disable their input and textarea elements.
    for (let siblingTabPane of selectedTabPane.parentNode.getElementsByClassName("tabPane")) {
        siblingTabPane.style.display = "none";
        setDescendantsWithTagNamesDisabled(siblingTabPane, ["input", "textarea"], true);
    }

    // Make selectedTabPane visible and enable its input and textarea elements.
    selectedTabPane.style.display = "block";
    setDescendantsWithTagNamesDisabled(selectedTabPane, ["input", "textarea"], false);
}

// Enable or disable all elements with the specified tag names contained in the parent.
// The elements are searched for recursively.
function setDescendantsWithTagNamesDisabled(parent, tagNames, disabled) {
    for (let inputDescendant of getDescendantsWithTagNames(parent, tagNames)) {
        inputDescendant.disabled = disabled;
    }
}

// Recursively retrieve all tags with the specified names that parent contains.
function getDescendantsWithTagNames(parent, tagNames) {
    var ret = [];
    for (let descendant of parent.querySelectorAll('*')) {
        for (let tagName of tagNames) {
            if (descendant.tagName === tagName.toUpperCase()) {
                ret.push(descendant);
            }
        }
    }
    return ret;
}

//////////////////////////////////////////////////////////////////////

function setInputValues(inputsMap) {
    for (inputName in inputsMap) {
        var elementsWithName = document.getElementsByName(inputName);

        console.assert(elementsWithName.length == 1,
            `Expected to find exactly 1 element named '${inputName}', but found ${elementsWithName.length}.`);
        console.assert(elementsWithName[0].tagName.toUpperCase() == "INPUT",
            `Expected element named '${inputName}' to be an INPUT tag, not '${elementsWithName[0].tagName}'.`);

        elementsWithName[0].value = inputsMap[inputName];
    }
}
