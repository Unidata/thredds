---
title: Customizing TDS
last_updated: 2018-06-30
sidebar: tdsTutorial_sidebar
toc: false
permalink: /customizing_tds.html
---

TDS provides a customizable user interface using [Thymeleaf](https://www.thymeleaf.org/) Java template engine.
The pages which currently support customization are:
 * Catalog
 * Dataset access

UI customization can be implemented through the contribution of both CSS stylesheets and Thymeleaf HTML templates.

## CSS Stylesheets

Custom stylesheets are specified in the `threddsConfig.xml` configuration file.
There are three properties within the `htmlSetup` element used to define stylesheets:

~~~xml
<htmlSetup>
   <standardCssUrl>standard.css</standardCssUrl>
   <catalogCssUrl>catalog.css</catalogCssUrl>
   <datasetCssUrl>dataset.css</datasetCssUrl>
</htmlSetup>
~~~

Values given for `catalogCssUrl` and `datasetCssUrl` will be applied as stylesheets on the Catalog and Dataset
access HTML pages, respectively. A path supplied to `standardCssUrl` will be used in all generated HTML pages.

Note: Paths to CSS files may be absolute or relative, relative paths must be relative to the webapp URL,
 i.e. http://server:port/thredds/.

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

'template/tdstemplateFragments.html'
~~~html
<div th:fragment="header">Your header content here</div>
~~~

Note: The above example will replace the default TDS header with our own and exclude all default style for TDS headers.
To include TDS style, the header fragment must invoke the classes `header` and `header-buffer` as follows:

~~~html
<div th:fragment="header">
  <div class="header-buffer"></div>
  <div class="header">Your header content here</div>
</div>
~~~

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

