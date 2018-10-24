---
title: Troubleshooting Problems
last_updated: 2018-04-02
sidebar: tdsTutorial_sidebar
toc: false
permalink: troubleshooting_problems.html
---

## Check log files for errors

The first step of troubleshooting any type of problem should involve looking at available log data.
Both Tomcat and the TDS provide a variety of logs that record errors and messages sent by your server.

Useful log files

| Log File | Description |
| `${tomcat.home}/logs/catalina.{date}.log` | Tomcat standard out. This is where Tomcat prints messages about Tomcat startup and uncaught exceptions from any webapp. |
| `${tomcat.home}/logs/localhost.{date}.log` | This is where log messages from Tomcat are sent. |
| `${tds.content.root.path}/logs/serverStartup.log` | This is where TDS logs messages about TDS startup. |
| `${tds.content.root.path}/logs/catalogInit.log` | Errors and warnings in your catalog configuration files are shown here. Look at this closely every time you change your config catalogs. |
| `${tomcat.home}/logs/access.{date}.log` | This is where Tomcat access logs are put, if you have enabled them. You need to manage these by removing them, say, once a month. |
| `${tds.content.root.path}/thredds/logs/threddsServlet.log.{date}` | Each request that the TDS responds to gets logged here. Errors and warnings that are associated with a request are logged here. You need to manage these by removing them, say, once a month. |
| `${tds.content.root.path}/thredds/logs/featureCollectionScan.log` | Log messages from feature collections. |
| `${tds.content.root.path}/thredds/logs/fc.<collection_name>.log` | Log messages from feature collections. |

## Common errors in the TDS Configuration Catalogs

### Duplicate path(s)

If you use the same `path` in more than one data root:

~~~xml
<datasetRoot path="testdup" location="C:/data/" />
<datasetScan name="duplicate path" path="testdup" location="/home/workshop/data/" 
             serviceName="dodsServer" />
~~~

you will get an error message in `${tds.content.root.path}/thredds/logs/catalogInit.log`:

~~~bash
Error: already have dataRoot =<testdup> mapped to directory= <C:/data/> wanted to map to=</home/workshop/data/> in catalog
~~~

### Duplicate id(s)

If you use the same `id` in more than one dataset:

~~~xml
<dataset name="Test Single Dataset 2" ID="testDataset2" serviceName="odap" 
         urlPath="test/testData2.grib2" dataType="Grid" />
<dataset name="Test Single Dataset 3" ID="testDataset2" serviceName="odap" 
         urlPath="test/testData3.grib3" dataType="Grid" />
~~~

you will get an error message in `${tds.content.root.path}/thredds/logs/catalogInit.log`:

~~~bash
WARNING: Duplicate id on  'THREDDS Catalog Name/Test Single Dataset 3' id= 'testDataset2'
Data directory doesn't exist
~~~

### Data directory doesn't exist

If you refer to a non-existant directory:

~~~xml
<datasetRoot path="sage" location="C:/data/notexist/" />
<datasetScan path="sage" location="C:/data/notexist/" />
~~~

you will get an error message in `${tds.content.root.path}/thredds/logs/catalogInit.log`:

~~~bash
Data Root =sage directory= <C:/data/notexist /> does not exist
~~~

or:

~~~bash
Invalid InvDatasetScan <path=testAll; scanLocation=bad/content/testdata>: CrawlableDataset for scanLocation does not exist.
    ... Dropping this datasetScan [testAll].
~~~

### DatasetScan has missing or invalid service

If you refer to a non-existant `service`, or omit a `service`:

~~~xml
<datasetScan name="bad service name" path="segundo" location="C:/data/" serviceName="badd" />
<datasetScan name="no service name" path="tertiary" location="C:/data/" />
~~~

you will get an error message in `${tds.content.root.path}/thredds/logs/catalogInit.log`:

~~~bash
**Error: DatasetScan (Top Dataset/bad service name ): must have a default service
**Error: DatasetScan (Top Dataset/no service name): must have a default service
~~~

### Data not compatible with service

If you use a `service` on a file which that `service` can\'t deal with:

~~~xml
<dataset name="Image of my Data" urlPath="images/labyrinth.jpg" serviceName="dodsServer" dataType="Grid" />
~~~

You won't get an error in `catalogInit.log`, ut when you try to access it:

* Browser: `DODServlet ERROR: Cant read C:/data/images/labyrinth.jpg: not a valid NetCDF file.`
* netCDF-Java client: `Lexical error at line 1, column 1. Encountered: "" (0), after : "" Data file does not exist`

### Data file does not exist

If you refer to a non-existant file in a `dataset`:

~~~xml
<dataset name="My Data" ID="Y" urlPath="images/labyrinth.nc" serviceName="dodsServer" dataType="Grid" />
~~~

You won't get an error in `catalogInit.log` but when you try to access it via OPeNDAP:

* Browser: `message = "Cant find images/labyrinth.nc"`
* netCDF-Java client: `"Cant find images/labyrinth.nc"`

If you try to access it via an HTTP server:

* Browser: `Error 404 The requested resource () is not available.`
* netCDF-Java client: `Error 404 Not Found`

### `DatasetScan` points to an empty directory, or filters out all files in the directory.

No warning - you simply wont see any datasets in that `DatasetScan`.

### Aggregation scan points to an empty directory

No warning, but when you try to access the dataset, client gets:

~~~bash
Error {     
  code = 500;      
  message = "There are no datasets in the aggregation DatasetCollectionManager { 
  collectionName='/data/goes/**/.gini' recheck=15.0 min    dir=/data/goes/ filter=WildcardMatchOnPath{wildcard=*.gini$ regexp=.*\.gini$}";  
};
~~~

and the `threddsServlet.log` has:

~~~
SEVERE: path= /ncmlTest.html 
   java.lang.IllegalStateException: There are no datasets in the aggregation DatasetCollectionManager{   
   collectionName='/data/goes/**/.gini' recheck=15.0 min dir=/data/goes/ filter=WildcardMatchOnPath{wildcard=*.gini$   regexp=.*\.gini$}
~~~