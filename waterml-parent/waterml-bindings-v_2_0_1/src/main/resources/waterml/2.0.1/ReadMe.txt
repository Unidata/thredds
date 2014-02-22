OGC(r) WaterML schema - ReadMe.txt
==================================

OGC(r) WaterML Encoding Standard
-------------------------------------------------------------------

WaterML is an OGC® Encoding Standard for the representation of hydrological
observations data with a specific focus on time series structures. WaterML2.0
is implemented as an application schema of the Geography Markup Language
version 3.2.1, making use of the OGC Observations & Measurements standards.
WaterML2.0 is designed as an extensible schema to allow encoding of data to be
used in a variety of exchange scenarios. Example areas of usage are: exchange
of data for operational hydrological monitoring programs; supporting operation
of infrastructure (e.g. dams, supply systems); cross-border exchange of
observational data; release of data for public dissemination; enhancing
disaster management through data exchange; and exchange in support of national
reporting. The core aspect of the model is in the correct, precise description
of time series. Interpretation of time series relies on understanding the
nature of the process that generated them. This standard provides the framework
under which time series can be exchanged with appropriate metadata to allow
correct machine interpretation and thus correct use for further analysis.
Existing systems should be able to use this model as a conceptual 'bridge'
between existing schema or systems, allowing consistency of the data to
maintained.

More information may be found at
 http://www.opengeospatial.org/standards/is

The most current schema are available at http://schemas.opengis.net/ .

-----------------------------------------------------------------------

2012-10-18  David Valentine
  * v2.0.1: Updated waterml/2.0 version to 2.0.1 
    + 2.0/vocabulary.sch: Correct typo of xml-rules.sch
    + 2.0/xml-rules.sch: Correct typo of xml-rules.sch
    + 2.0/measurement-timeseries-tvp.sch, 2.0/categorical-timeseries-tvp.sch:
      Fix context of rule so both my be active at the same time

2012-09-07  Peter Taylor
  * v2.0.0: Post WaterML 2.0.0 as waterml/2.0 from OGC 10-126r3

 Note: check each OGC numbered document for detailed changes.

-----------------------------------------------------------------------

Policies, Procedures, Terms, and Conditions of OGC(r) are available
  http://www.opengeospatial.org/ogc/legal/ .

Copyright (c) 2012 Open Geospatial Consortium.

-----------------------------------------------------------------------

