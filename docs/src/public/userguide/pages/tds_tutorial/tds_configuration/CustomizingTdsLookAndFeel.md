---
title: Customizing the TDS Look and Feel
last_updated: 2019-01-22
sidebar: tdsTutorial_sidebar
toc: false
permalink: customizing_tds_look_and_feel.html
---

TDS provides a extensible and customizable user interface using [Thymeleaf](https://www.thymeleaf.org/) Java template engine.
The pages which currently support customization are:
 * Catalog
 * Dataset access

UI customization can be implemented through the contribution of both CSS stylesheets and Thymeleaf HTML templates.
Additionally, TDS administrators may register Jupyter Notebooks as dataset viewers using the Jupyter Notebook service.

## CSS Stylesheets

To customize TDS using CSS, custom CSS documents should be placed inside tds.content.root.path/thredds/public.

TDS is configured to use the CSS documents supplied in the `public` directory in `threddsConfig.xml`.
There are three properties within the `htmlSetup` element used to define stylesheets:

~~~xml
<htmlSetup>
   <standardCssUrl>standard.css</standardCssUrl>
   <catalogCssUrl>catalog.css</catalogCssUrl>
   <datasetCssUrl>dataset.css</datasetCssUrl>
   <openDapCssUrl>tdsDap.css</openDapCssUrl>
</htmlSetup>
~~~

CSS documents given for `catalogCssUrl` and `datasetCssUrl` elements will be applied on the Catalog and Dataset
access HTML pages, respectively.
A CSS document supplied to `standardCssUrl` will be used in all generated HTML pages.
The OPeNDAP HTML form is treated special - the only CSS document applied to this page is defined by the `openDapCssUrl` element.

## Thymeleaf Templates

When TDS is deployed, a `templates` directory is created within the main `content` directory (`tds.content.root.path`).
The Thymeleaf template resolver used by TDS will search this directory for user-supplied template fragments each time
a customizable HTML page is requested.

Pages are customizable at plug-in points defined by the tag `ext:`, which instructs the template resolver to look for
externally supplied template fragments. Some of the plug-in points provide defaults when no user-supplied template is
available (such as the main TDS header and footer, whereas other plug-in allow for additional content. See the
 [Thymeleaf documentation] (https://www.thymeleaf.org/doc/articles/layouts.html) for an overview of natural templating
 using Thymeleaf and fragments. A full list of current plug-in points for user-supplied fragments can be found in the below sections.

### Overwriting a Default
To contribute a template fragment, place the `fragment` element in `templates/tdsTemplateFragments.html`.

*Example: Overwriting the default header*

'template/tdsTemplateFragments.html'
~~~html
<div th:fragment="header">Your header content here</div>
~~~

Note: The templating system will automatically attach default TDS CSS properties to custom headers and footers;
to avoid this behavior, users must provide their own overrides through custom stylesheets.

Current default fragments which may be overridden are:
  * `header`
  * `footer`

### Contributing Additional Content Sections
Additional content sections may be contributed the same way as overridable defaults, but will only render as HTML element
if a user-contributed template fragment is found.

*Example: Adding content to the bottom of the catalog*

`templates/tdsTemplateFragments.html`
~~~html
<div th:fragment="datasetCustomContentBottom">
    <div>Your bottom content goes here.</div>
</div>
~~~

*Example: Contributing multiple fragments*

`templates/tdsTemplateFragments.html`
~~~html
<div th:fragment="datasetCustomContentBottom">
    <div th:replace="~{ext:additionalFragments/myFragments :: mySectionHeader}"/>
    <div th:replace="~{ext:additionalFragments/myFragments :: mySectionContent}"/>
</div>
~~~

`templates/additionalFragments/myFragments.html`
~~~html
    <div th:fragment="mySectionHeader" class="section-header">My Section Name</div>
    <div th:fragment="mySectionContent" class="section-content">Your contributed content here.</div>
~~~

In the second example, we have defined our own fragments in a separate file, `myFragments.html`. Fragments which correspond
to a plug-in point, such as `catalogCustomContentTop` must be within the file `tdsTemplateFragments`, however the main
fragments may reference paths to other template files by using the `ext:` tag.

Note: The classes `section-header` and `section-content` apply the default TDS style for content panes.

Current contributable sections are:
  * `catalogCustomContentTop` - additional content placed at the top of catalog pages
  * `catalogCustomContentBottom` - additional content placed at the bottom of catalog pages
  * `datasetCustomContentBottom` - additional content placed at the bottom of dataset access pages

### Contributing Additional Content Tabs
Contributing tabbed content requires two fragments, one for the tab button and another for the content.
Each tab button must implement the click event handler `switchTab(buttonElement, contentElementId, groupId)`.

*Example:*

`templates/tdsTemplateFragments.html`
~~~html
<div th:fragment="customInfoTabButtons">
   <div class="tab-button info" onclick="switchTab(this, 'custom1', 'info')">Custom1</div>
   <div class="tab-button info" onclick="switchTab(this, 'custom2', 'info')">Custom2</div>
</div>

<div th:fragment="customInfoTabContent">
   <div class="tab-content info" id="custom1">This is one contributed tab pane...</div>
   <div class="tab-content info" id="custom2">..and this is a second!</div>
</div>
~~~

In the above example, the `tab-button` and `tab-content` classes apply the same style to the contributed tabs as the
default tabs. The `info` class groups the contributed tabs with the other tabs in the information tab pane.
To group a contributed tab with the access tab pane, use the `access` class.
Note: Multiple custom tabs may be contributed by grouping them within the fragment tags.

Current contributable tabs are:
  * `customAccessTabButtons/customAccessTabContent` - adds tabs to the tab pane holding the "Access" and "Preview" views
  * `customInfoTabButtons/customInfoTabContent` - adds tabs to the tab pane holding view with information about the dataset

### Using TDS Properties in Contributed Templates
Information from the server is passed to the templated pages through a data model. The properties made available to
the template parser are:

~~~java
{
  String googleTracking,
  String serverName,
  String logoUrl,
  String logoAlt,
  String installName,
  String installUrl,
  String webappName,
  String wabappUrl,
  String webappVersion,
  String webappBuildTimestamp,
  String webappDocsUrl,
  String contextPath,
  String hostInst,
  String hostInstUrl
}
~~~

Additionally, the catalog page is passed the properties `boolean rootCatalog`, which is set to `true` only on the top-level catalog page, and `List<CatalogItemContext> items`, a set of items in the Catalog
defined as `CatalogItemContext` data contracts:

~~~java
class CatalogItemContext {

  String getDisplayName();

  int getLevel();

  String getDataSize();

  String getLastModified();

  String getIconSrc();

  String getHref();
}
~~~

Similarly, the dataset page is passed the property `DatasetContext dataset`, a data contract defining the properties
of the dataset:

~~~java
class DatasetContext {

  String getName();

  String getCatUrl();

  String getCatName();

  List<Map<String, String>> get Documentation();

  List<Map<String, String>> getAccess();

  List<Map<String, String>> getContributors();

  List<Map<String, String>> getKeywords();

  List<Map<String, String>> getDates();

  List<Map<String, String>> getProjects();

  List<Map<String, String>> getCreators();

  List<Map<String, String>> getPublishers();

  List<Map<String, String>> getVariables();

  String getVaraiableMapLink();

  Map<String, Object> getGeospatialCoverage();

  Map<String, Object> getTimeCoverage();

  List<Map<String, String>> getMetadata();

  List<Map<String, String>> getProperties();

  Map<String, Object> getAllContext();

  Object getContextItem(String key);

  List<Map<String, String>> getViewerLinks();
}
~~~

*Example:* Add a section to a dataset view which links to the host institution site and
displays a table of all properties returned by `getAllContext()`.

`templates/tdsTemplateFragments.html`
~~~html
<div th:fragment="datasetCustomContentBottom">
    <h3>Properties of
      <th:block th:text="${dataset.getName()}"
       - hosted by <a th:href="${hostInstUrl}" th:text="${hostInst}"></a>
    </h3>
    <table class="property-table">
        <tr th:each="prop : ${dataset.getAllContext()}">
            <td><em th:text="${prop.key}"/><td th:text="${prop.value}"/>
        </tr>
    </table>
</div>
~~~

### Adding TDS Properties to Templates
Don't see what you're looking for?
If the properties exposed to the template parser do not meet your needs,
you are encouraged to update the above data models by submitting a pull request to
[https://github.com/Unidata/thredds](https://github.com/Unidata/thredds). The data models are defined and populated in
[`CatalogViewContextParser.java'](https://github.com/Unidata/thredds/blob/5.0.0/tds/src/main/java/thredds/server/catalogservice/CatalogViewContextParser.java).

## Jupyter Notebooks

### About
The goal of the Jupyter Notebook service is to provide an method of interacting with and visualizing TDS datasets without
large data transfers. When the Notebook service is enabled, requests to the service will return a Notebook (ipynb file)
which demos accessing the requested dataset via [Siphon](https://unidata.github.io/siphon/latest/api/index.html). Notebook files
may be viewed in Jupyter Notebook or JupyterLab and edited by the end user to explore capabilities of the dataset and Siphon.

### Enable/Disable Notebook service
By default, the Jupyter Notebook service is enabled. If no contributed Notebook viewers are found, the TDS will supply
a default viewer for accessing all datasets in the system. This default can be found in `notebooks/jupyter_viewer.ipynb`
in the content directory.

To disable the Notebook service, add the following property to `threddsConfig.xml`:

~~~xml
  <JupyterNotebookService>
    <allow>false</allow>
  </JupyterNotebookService>
~~~

To configure the Notebook service, add the following properties to `threddsConfig.xml`:

~~~xml
  <JupyterNotebookService>
    <allow>true</allow>
    <maxAge>60</scour>
    <maxFiles>100</maxFiles>
  </JupyterNotebookService>
~~~

Where `<maxAge>` defines how long a a mapping between a dataset and a Notebook should be stored after the last access,
and `<maxFile>` defines the maximum number of mapping which can be stored at one time.

### Contribute Notebooks
  * To add a Notebook viewers to the TDS Notebook service, place `ipynb` files in the `notebooks` folder within the
  content directory (Note: To register new Notebook viewers, the server must be restarted with the new files in the
  notebook firectory, TDS will not process new Notebooks while active.)
  * To map a Notebook viewer to a subset of datasets, include the following in the Notebook's metadata:

~~~
  "metadata": {
  ...
    "viewer_info": {
      "accept_datasetIDs": [],
      "accept_catalogs": [],
      "accept_dataset_types": [],
      "accept_all" : false,
      "order": 1
    }
  }
~~~

All `viewer_info` properties are optional. Multiple properties may be used to register a single Notebook.

* `accept_datasetIDs` - Accepts a list of dataset IDs for which the Notebook is valid.
* `accept_catalogs` - Accepts a list of catalog names or URLs which contain datasets for which the Notebook is valid.
* `accept_dataset_types`: Accepts a list of  feature types for which the Notebook is valid (e.g. Grid, Point).
* `accept_all` - If true, indicates the Notebook is valid for all datasets.
* `order` - In the case that more than one Notebook is valid for a given dataset, `order` will be used to determine
which Notebook is returned.

If no `viewer_info` is included in the Notebook metadata, the following default will be supplied:

~~~
  "metadata": {
  ...
    "viewer_info": {
      "accept_all" : true,
      "order": INT_MAX
    }
  }
~~~
