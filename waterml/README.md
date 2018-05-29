# waterml

Converts [CDM DSGs](http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/reference/FeatureDatasets/PointFeatures.html)
(currently just Station time series features) to [WaterML 2.0 timeseries](http://www.opengeospatial.org/standards/waterml).

## Inclusion of third-party software

This project contains source code from [ERDDAP](https://coastwatch.pfeg.noaa.gov/erddap/index.html).
The license for ERDDAP is available in `docs/src/private/licenses/third-party/errdap/`.

### Details of use:

We wanted to add the UDUNITS-to-UCUM unit conversion feature from ERRDAP to THREDDS. To that end, we copied
the following files from [the GitHub repository](https://github.com/BobSimons/erddap):
* `erddap/WEB-INF/classes/gov/noaa/pfel/erddap/util/EDUnits.java`
* `erddap/WEB-INF/classes/gov/noaa/pfel/erddap/util/UcumToUdunits.properties`
* `erddap/WEB-INF/classes/gov/noaa/pfel/erddap/util/UdunitsToUcum.properties`
* `erddap/WEB-INF/classes/com/cohort/array/StringArray`
* `erddap/WEB-INF/classes/com/cohort/util/Calendar2.java`
* `erddap/WEB-INF/classes/com/cohort/util/Math2.java`
* `erddap/WEB-INF/classes/com/cohort/util/String2.java`

and moved them to the `ucar.nc2.ogc.erddap.util` package. Also:
* Any code that was unrelated to the unit conversion feature was removed.
* Test code from `EDUnits` was broken off into a separate class.
* The 5 original classes were renamed to begin with the prefix "Errdap".
