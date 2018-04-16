---
title: Catalog References
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: client_catalog_references.html
---

It is very useful to break up large catalogs into pieces and separately maintain each piece.
One way to do this is to build each piece as a separate and logically-complete catalog, then create a master catalog using _catalog references_:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" name="Top Catalog"
         xmlns:xlink="http://www.w3.org/1999/xlink"> <!-- 1 -->
   <dataset name="Realtime data from IDD" ID="IDD">  <!-- 2 -->
     <catalogRef xlink:href="idd/forecastModels.xml" xlink:title="Forecast Model Data" name=""/> <!-- 3 -->
     <catalogRef xlink:href="idd/forecastProdsAndAna.xml" xlink:title="Forecast Products and Analyses" name=""/> <!-- 3 -->
     <catalogRef xlink:href="idd/obsData.xml" xlink:title="Observation Data" name=""/> <!-- 3 -->
     <catalogRef xlink:href="idd/radars.xml" xlink:title="Radar Data" name=""/> <!-- 3 -->
     <catalogRef xlink:href="idd/satellite.xml" xlink:title="Satellite Data" name=""/> <!-- 3 -->
   </dataset>

  <catalogRef xlink:title="Far Away University catalog" xlink:href="http://www.farAway.edu/thredds/catalog.xml" />    <!-- 4 -->
</catalog>
~~~

* <1> We declare the `xlink` `namespace` in the `catalog` element.
* <2> The _collection_ (or _container_) dataset logically contains the `catalogRefs`, which are thought of as nested datasets whose contents are the contents of the external catalog.
* <3> Here are several `catalogRef` elements, each with a link to an external catalog, using the xlink:href attribute. 
  The `xlink:title` is used as the name of the dataset.
  We need a `name` attribute (in order to validate, for obscure reasons), but it is ignored.
  The `xlink:href` attributes are [relative URLs](http://www.webreference.com/html/tutorial2/3.html){:target="_blank"} and are resolved against the catalog URL. 
  For example, if the URL of the client catalog, as shown above, is:

  <http://thredds.ucar.edu/thredds/catalog.xml>{:target="_blank"}

  then the resolved URL of the first `catalogRef` will be:

  <http://thredds.ucar.edu/thredds/idd/forecastModels.xml>{:target="_blank"}

* <4> `catalogRefs` needn't point to local catalogs only; this one points to a remote one at Far Away University.
* The `metadata` elements with `inherited="true"` are NOT not copied across `catalogRefs`.
  The catalog that a `catalogRef` refers to is stand-alone in that sense.
