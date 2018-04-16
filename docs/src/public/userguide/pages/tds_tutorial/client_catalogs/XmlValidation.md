---
title: XML Validation
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_xml_validation.html
---

As catalogs get more complicated, you should check that you haven't made any errors.
There are three components to checking:

* Is the XML well-formed?
* Is it valid against the catalog schema?
* Does it have everything it needs to be read by a THREDDS client?

You can check _well-formedness_ using online tools like [this one](http://www.xmlvalidation.com/){:target="_blank"}.
If you also want to check _validity_ in those tools, you will need to declare the catalog schema location like so:

~~~xml
<catalog name="Validation" xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0
    http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">
  ...
</catalog>
~~~

* The `xmlns:xsi` attribute  declares the schema-instance namespace.
  Just copy it exactly as you see it here.
* The `xsi:schemaLocation` attribute tells your XML validation tool where to find the THREDDS XML schema document.
  Just copy it exactly as you see them here.

Or, you can simply use the [THREDDS Catalog Validation service](http://thredds.ucar.edu/thredds/remoteCatalogValidation.html){:target="_blank"} to check all three components at once.
This service already knows where the schemas are located, so it's not necessary to add that information to the catalog; you only need it if you want to do your own validation.

{%include note.html content="
Reference documentation - The schema referenced in the example can be found here.
However, you'll probably want to study the catalog specification instead, as it is much more digestable.
" %}
