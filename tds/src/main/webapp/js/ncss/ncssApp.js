function init(horizExtentWKT) {
    initMap(horizExtentWKT);
    addEventListeners();
    resetForm();
}

function initMap(horizExtentWKT) {
    // See http://openlayers.org/en/latest/apidoc/ol.Map.html

    var mousePositionControl = new ol.control.MousePosition({
        projection: 'EPSG:4326',
        className: 'olControlMousePosition',  // Defined in sidebar.css.
        coordinateFormat: function(coordinate) {
            return ol.coordinate.format(coordinate, '({x}, {y})', 2);
        }
    });

    var controls = [
        new ol.control.Zoom(),
        new ol.control.Rotate(),
        mousePositionControl
    ];

    var wktFeature = new ol.format.WKT().readFeature(horizExtentWKT, {
        // See https://gis.stackexchange.com/a/48952 for definitions of these coordinate systems.
        dataProjection: 'EPSG:4326',
        featureProjection: 'EPSG:3857'
    });

    // horizontalExtent layer is yellow.
    var vectorStyle = new ol.style.Style({
        stroke: new ol.style.Stroke({
            color: '#ffff00',
            width: 1
        }),
        fill: new ol.style.Fill({
            color: 'rgba(255, 255, 0, 0.2)'
        })
    });

    var layers = [
        new ol.layer.Tile({
            source: new ol.source.OSM()
        }),
        new ol.layer.Vector({
            source: new ol.source.Vector({
                features: [wktFeature]
            }),
            style: vectorStyle
        })
    ];

    var map = new ol.Map({
        controls: controls,
        layers: layers,
        target: 'map',
        view: new ol.View({
            center: [0, 0],
            zoom: 1,
            // Restrict panning of the map such that the center cannot be outside of [-180, -85.05, 180, 85.05].
            extent: ol.proj.get("EPSG:3857").getExtent()
        })
    });

    map.getView().fit(wktFeature.getGeometry());  // Zoom map to fit wktFeature.
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
    for (let descendant of getDescendantsWithTagNames(parent, tagNames)) {
        descendant.disabled = disabled;
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
