---
title: TDS and ncISO - Metadata Services
last_updated: 2018-10-22
sidebar: tdsTutorial_sidebar
toc: false
permalink: iso_metadata.html
---

The TDS distribution includes the [ncISO](https://geo-ide.noaa.gov/wiki/index.php?title=NcISO){:target="_blank"} package from NOAA/Environmental Data Management (many thanks to Dave Neufeld and Ted Habermann).

## ncISO Services
ncISO supports three new services for datasets:
* `NCML`: an NcML representation of the dataset's structure and metadata;
* `ISO`: an ISO 19115 metadata representation of the dataset; and
* `UDDC`: an evaluation of how well the metadata contained in the dataset conforms to the [NetCDF Attribute Convention for Data Discovery (NACDD)](https://www.unidata.ucar.edu/software/thredds/current/netcdf-java/metadata/DataDiscoveryAttConvention.html){:target="_blank"} ( see the [NOAA/EDM page on NACDD}(https://geo-ide.noaa.gov/wiki/index.php?title=NetCDF_Attribute_Convention_for_Dataset_Discovery){:target="_blank"}). 

## Enabling ncISO Services

The ncISO services are enabled by default. These services can be disabled for locally served datasets by including the following in the `threddsConfig.xml` file:

~~~xml
<NCISO>
  <ncmlAllow>false</ncmlAllow>
  <uddcAllow>false</uddcAllow>
  <isoAllow>false</isoAllow>
</NCISO>
~~~

## Providing ncISO Services for Datasets

Once ncISO is enabled, datasets can be configured to have the three ncISO services in the TDS catalog configuration files similar to the way other services are configured.
The service element's serviceType and base attribute values must be as follows: 

~~~xml
<service name="ncml" serviceType="NCML" base="/thredds/ncml/"/>
<service name="uddc" serviceType="UDDC" base="/thredds/uddc/"/>
<service name="iso" serviceType="ISO" base="/thredds/iso/"/>
~~~

The dataset to be served must reference a containing compound service by the service name.
For instance, if a compound service named "all" contained all three services listed above: 

~~~xml
<dataset ID="sample" name="Sample Data" urlPath="sample.nc">
  <serviceName>all</serviceName>
</dataset>
~~~