---
title: Dataset Source Plugin
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: dataset_source_plugin.html
---

In order for the TDS to serve data through any of the subsetting protocols like `OPenDAP`, `WCS` or the Netcdf Subsetting Service(`NCSS`), it must be able to read the data into the Common Data Model.
When the data is contained in a file, this is done with an I/O Service Provider (`IOSP`).
When the dataset depends on request parameters that are passed to the TDS by the client, then a more general interface is needed, since the `IOSP` does not have access to the `HttpServletRequest` object.

## Create a DatasetSource implementation class

Your class must implement the `thredds.servlet.DatasetSource` interface:

~~~java
public interface DatasetSource {

  public boolean isMine( HttpServletRequest req);

  public NetcdfFile getNetcdfFile( HttpServletRequest req, HttpServletResponse res) throws IOException;
}
~~~

The implementor of `DatasetSource` must quickly determine if it can service the request by returning `true` or `false` from the `isMine` method.
If `true`, then it must be able to return a `NetcdfFile` from the `getNetcdfFile()`.
Any caching or performance optimizations must be handled by the `DatasetSource` implementor.
It is usually important for performance reasons to cache the `NetcdfFile` object for subsequent access, since clients typically access a `NetcdfFile` several or many times, not just once.

If the `getNetcdfFile` method encounters an error processing the request, it should set the return code on the `HttpResponse` and `return null`.
For example:

~~~java
 response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
~~~

By returning null, the calling routine assumes that you have sent the response yourself, and will terminate processing without further action.

Example:
~~~java
public class DatasetSourceExample implements thredds.servlet.DatasetSource {
  static final String prefix =  "/special/";
  static final int prefixLen =  prefix.length();

  public boolean isMine(HttpServletRequest req) {
    String path = req.getPathInfo();
    return path.startsWith(prefix);
  }

  public NetcdfFile getNetcdfFile(HttpServletRequest req, HttpServletResponse res) throws IOException {
    String path = req.getPathInfo().substring(prefixLen);
    DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(path);
    if (match == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, path);
      return null;
    }

    int pos = match.remaining.lastIndexOf('.');
    String filename = match.remaining.substring(0, pos);
    File file = new File(match.dirLocation+filename);
    if (!file.exists()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, match.dirLocation+filename);
      return null;
    }

    NetcdfFile ncfile = NetcdfDataset.openFile(file.getPath(), null);
    ncfile.addAttribute(null, new Attribute("Special", req.getRequestURI()));
    ncfile.finish();
    return ncfile;
  }
}
~~~

## Loading your class at runtime

You must place your `DatasetSource` class into the `${tomcat_home}/webapps/thredds/WEB-INF/lib` or `${tomcat_home}/webapps/thredds/WEB-INF/classes` directory.

Then tell the TDS to load it by adding a line to the `${tds.content.root.path}/thredds/threddsConfig.xml` file, for example:

~~~xml
<datasetSource>my.package.DatasetSourceImpl</datasetSource>
~~~

## Adding to the Thredds Configuration Catalog

In the above example, `DatasetSourceExample` will _claim_ any request whose path starts with `*/special/*`.
In the Thredds Configuration Catalog, you will add datasets which have that path.

A simple example:

~~~xml
<service name="thisDODS" serviceType="OpenDAP" base="/thredds/dodsC/" />
<dataset name="Test ExampleDataSource" ID="testDataset" serviceName="thisDODS" urlPath="special/my/testData.foo" />
~~~

The dataset will have a URL of `http://server:port/thredds/dodsC/special/my/testData.foo`. 
Its path will be `/special/my/testData.foo`, so `DatasetSourceExample` will claim it, and will be required to return a `NetcdfFile` object.

The value that you put into `urlPath` will be used by TDS clients.
You can use anything you want, as long as your `DatasetSource` recognizes the path and correctly returns the corresponding `NetcdfFile`.
