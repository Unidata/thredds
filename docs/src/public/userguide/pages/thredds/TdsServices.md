---
title: TDS Services
last_updated: 2018-10-18
sidebar: tdsTutorial_sidebar
toc: false
permalink: services_ref.html
---

## Data Services

### Standard Data Services

The TDS has a set of Standard Data Services that are always available (unless explicitly disabled) and can be referenced from any configuration catalog:

* cdmRemote
* cdmrFeatureGrid
* dap4
* httpService
* resolver
* netcdfSubsetGrid
* netcdfSubsetPoint
* opendap
* wcs
* wms
* iso
* ncml
* uddc
 
### Available Services

The TDS configures the appropriate set of standard data services for each dataType/featureType.
You can configure the available data services globally, and they will be applied to all datasets of that dataType/featureType.
If you do not specify a service for a dataset, but you do specify its dataType/featureType, then the datatype-appropriate services will be enabled for it.

### User defined Services

You can still define your own services, either globally in the root catalog, or locally in any configuration catalog.
* Services placed in the root catalog are global and can be referenced in any other config catalog.
* Services placed in any other catalog are local, are used only in that catalog, and override (by name) any global services.


## Server Information Services
<table width="95%" border="2">
  <tbody><tr>
    <th width="15">Name</th>
    <th width="50%">TDS Configuration</th>
    <th width="35%">Description</th>
  </tr>
  <tr>
    <td>Server Information (HTML)</td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Enabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td><code>/thredds/serverInfo.html</code></td>
        </tr>
      </tbody></table>
    </td>
    <td rowspan="3">
      Provide human and machine readable access to information about the server
      installation. E.g., an abstract and a list of keywords summarizing the
      services and data available on the server, contact information and other
      information about the group hosting the server, and the version of the
      THREDDS Data Server (TDS) running.
    </td>
  </tr>
  <tr>
    <td>Server Information (XML)</td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Enabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td><code>/thredds/serverInfo.xml</code></td>
        </tr>
      </tbody></table>
    </td>
  </tr>
  <tr>
    <td>Server Version Information (Text)</td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Enabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td><code>/thredds/serverVersion.txt</code></td>
        </tr>
      </tbody></table>
    </td>
  </tr>
</tbody></table>

<h3><a name="catalogServices">Catalog Services</a></h3>
<table width="95%" border="2">
  <tbody><tr>
    <th width="15">Name</th>
    <th width="50%">TDS Configuration</th>
    <th width="35%">Description</th>
  </tr>
  <tr>
    <td>THREDDS&nbsp;Catalog Services</td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Enabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td>
            <code>/thredds/catalog.{xml|html}</code><br>
            <code>/thredds/catalog/*/catalog.{xml|html}</code><br>
            <code>/thredds/*/*.{xml|html}</code>
          </td>
        </tr>
      </tbody></table>
    </td>
    <td rowspan="2">
      Provide subsetting and HTML conversion services for THREDDS catalogs. Catalogs
      served by the TDS can be subset and/or viewed as HTML. Remote catalogs, if allowed/enabled,
      can be validated, displayed as HTML, or subset.
      <ul>
        <li>More details are available <a href="CatalogService.html">here</a>.
        </li>
        <li>Services for remote catalogs can be enabled with the TDS Configuration
          File (<a href="ThreddsConfigXMLFile.html#Remote">threddsConfig.xml</a>).
        </li>
      </ul>
    </td>
  </tr>
  <tr>
    <td>Remote THREDDS Catalog Service</td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Disabled</td>
        </tr>
        <tr>
          <td>Access&nbsp;Point</td>
          <td><code>/thredds/remoteCatalogService</code></td>
        </tr>
      </tbody></table>
    </td>
  </tr>
</tbody></table>

<h3><a name="metadataServices">Metadata Services</a></h3>
<table border="2">
  <tbody><tr>
    <th width="187">Name</th>
    <th width="632">TDS Configuration</th>
    <th width="450">Description</th>
  </tr>
  <tr>
    <td><a name="ISO">ISO</a></td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Enabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td><code>/thredds/iso/*</code></td>
        </tr>
        <tr>
          <th colspan="2">Catalog Service Configuration<br>
            (exact values <a href="#tdsServiceElemRequirements">required</a>)
          </th>
        </tr>
        <tr>
          <td>Service Type</td>
          <td><strong>ISO</strong></td>
        </tr>
        <tr>
          <td>Service Base URL</td>
          <td><strong>/thredds/iso/</strong></td>
        </tr>
      </tbody></table>
    </td>
    <td>Provide ISO 19115 metadata representation of a dataset's structure and metadata
      <ul>
        <li>More details are available <a href="ncISO.html">here</a>.
        </li>
        <li>Enable ncISO with the TDS Configuration File
          (<a href="ThreddsConfigXMLFile.html#ncISO">threddsConfig.xml</a>).
        </li>
      </ul>
    </td>
  </tr>
  <tr>
    <td><a name="NCML">NCML</a></td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Enabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td><code>/thredds/ncml/*</code></td>
        </tr>
        <tr>
          <th colspan="2">Catalog Service Configuration<br>
            (exact values <a href="#tdsServiceElemRequirements">required</a>)
          </th>
        </tr>
        <tr>
          <td>Service Type</td>
          <td><strong>NCML</strong></td>
        </tr>
        <tr>
          <td>Service Base URL</td>
          <td><strong>/thredds/ncml/</strong></td>
        </tr>
      </tbody></table>
    </td>
    <td>Provide NCML representation of a dataset
      <ul>
        <li>More details are available <a href="ncISO.html">here</a>.
        </li>
        <li>Enable ncISO with the TDS Configuration File
          (<a href="ThreddsConfigXMLFile.html#ncISO">threddsConfig.xml</a>).
        </li>
      </ul>
    </td>
  </tr>
  <tr>
    <td><a name="UDDC">UDDC</a></td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Enabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td><code>/thredds/uddc/*</code></td>
        </tr>
        <tr>
          <th colspan="2">Catalog Service Configuration<br>
            (exact values <a href="#tdsServiceElemRequirements">required</a>)
          </th>
        </tr>
        <tr>
          <td>Service Type</td>
          <td><strong>UDDC</strong></td>
        </tr>
        <tr>
          <td>Service Base URL</td>
          <td><strong>/thredds/uddc/</strong></td>
        </tr>
      </tbody></table>
    </td>
    <td>Provide an evaluation of how well the metadata contained in a dataset conforms
      to the <a href="http://wiki.esipfed.org/index.php/Category:Attribute_Conventions_Dataset_Discovery">
        NetCDF Attribute Convention for Data Discovery (NACDD)</a>
      <ul>
        <li>More details are available <a href="ncISO.html">here</a>.
        </li>
        <li>Enable ncISO with the TDS Configuration File
          (<a href="ThreddsConfigXMLFile.html#ncISO">threddsConfig.xml</a>).
        </li>
      </ul>
    </td>
  </tr>
</tbody></table>

<h3><a name="dataAccessServices">Data Access Services</a></h3>
<table width="95%" border="2">
<tbody><tr>
  <th width="15">Name</th>
  <th width="50%">TDS Configuration</th>
  <th width="35%">Description</th>
</tr>
<tr>
  <td><a name="DAP2">OPeNDAP DAP2</a></td>
  <td>
    <table width="100%">
      <tbody><tr>
        <th colspan="2">Basic Configuration</th>
      </tr>
      <tr>
        <td>Default&nbsp;Availability</td>
        <td>Enabled</td>
      </tr>
      <tr>
        <td>Access Point</td>
        <td><code>/thredds/dodsC/*</code></td>
      </tr>
      <tr>
        <th colspan="2">Catalog Service Configuration<br>
          (exact values <a href="#tdsServiceElemRequirements">required</a>)
        </th>
      </tr>
      <tr>
        <td>Service Type</td>
        <td><strong>OPeNDAP</strong></td>
      </tr>
      <tr>
        <td>Service Base URL</td>
        <td><strong>/thredds/dodsC/</strong></td>
      </tr>
    </tbody></table>
  </td>
  <td>OPeNDAP DAP2 data access protocol.
    <ul>
      <li>Several configuration options are available
        (<a href="../reference/ThreddsConfigXMLFile.html#opendap">details</a>).
      </li>
    </ul>
  </td>
</tr>
<tr>
  <td><a name="NCSS">NetCDF Subset Service</a></td>
  <td>
    <table width="100%">
      <tbody><tr>
        <th colspan="2">Basic Configuration</th>
      </tr>
      <tr>
        <td>Default&nbsp;Availability</td>
        <td>Enabled</td>
      </tr>
      <tr>
        <td>Access Point</td>
        <td><code>/thredds/ncss/*</code></td>
      </tr>
      <tr>
        <th colspan="2">Catalog Service Configuration<br>
          (exact values <a href="#tdsServiceElemRequirements">required</a>)
        </th>
      </tr>
      <tr>
        <td>Service Type</td>
        <td><strong>NetcdfSubset</strong></td>
      </tr>
      <tr>
        <td>Service Base URL</td>
        <td><strong>/thredds/ncss/</strong></td>
      </tr>
    </tbody></table>
  </td>
  <td>NetCDF Subset Service: a data access protocol.
    <ul>
      <li>More details are available <a href="NetcdfSubsetServiceReference.html">here</a>.
      </li>
      <li>Enable NCSS and set other configuration options with the TDS
        Configuration File
        (<a href="ThreddsConfigXMLFile.html#ncss">threddsConfig.xml</a>).
        More setup and configuration details are available
        <a href="NetcdfSubsetServiceConfigure.html">here</a>.
      </li>
    </ul>
  </td>
</tr>
<tr>
  <td><a name="cdmremote">CDM Remote</a></td>
  <td>
    <table width="100%">
      <tbody><tr>
        <th colspan="2">Basic Configuration</th>
      </tr>
      <tr>
        <td>Default&nbsp;Availability</td>
        <td>Enabled</td>
      </tr>
      <tr>
        <td>Access Point</td>
        <td><code>/thredds/cdmremote/*</code></td>
      </tr>
      <tr>
        <th colspan="2">Catalog Service Configuration<br>
          (exact values <a href="#tdsServiceElemRequirements">required</a>)
        </th>
      </tr>
      <tr>
        <td>Service Type</td>
        <td><strong>cdmremote</strong></td>
      </tr>
      <tr>
        <td>Service Base URL</td>
        <td><strong>/thredds/cdmremote/</strong></td>
      </tr>
    </tbody></table>
  </td>
  <td>cdmremote/ncstream data access service. This service is enabled by
    default. It is automatically enabled when an appropriate
    FeatureCollection is used.
    <ul>
      <li>More details are available
        <a href="../../netcdf-java/reference/stream/CdmRemote.html">here</a>.
      </li>
    </ul>
  </td>
</tr>
<tr>
  <td><a name="WCS">OGC Web Coverage Service (WCS)</a></td>
  <td>
    <table width="100%">
      <tbody><tr>
        <th colspan="2">Basic Configuration</th>
      </tr>
      <tr>
        <td>Default&nbsp;Availability</td>
        <td>Enabled</td>
      </tr>
      <tr>
        <td>Access Point</td>
        <td><code>/thredds/wcs/*</code></td>
      </tr>
      <tr>
        <th colspan="2">Catalog Service Configuration<br>
          (exact values <a href="#tdsServiceElemRequirements">required</a>)
        </th>
      </tr>
      <tr>
        <td>Service Type</td>
        <td><strong>WCS</strong></td>
      </tr>
      <tr>
        <td>Service Base URL</td>
        <td><strong>/thredds/wcs/</strong></td>
      </tr>
    </tbody></table>
  </td>
  <td>OGC WCS supports access to geospatial data as "coverages".
    <ul>
      <li>More details about the OGC WCS are available
        <a href="http://www.opengeospatial.org/standards/wcs">here</a>.
      </li>
      <li>Enable OGC WCS and set other configuration options with the TDS
        Configuration File
        (<a href="ThreddsConfigXMLFile.html#wcs">threddsConfig.xml</a>).
        More setup, configuration, and implementation details for the TDS's
        OGC WCS implementation are available <a href="WCS.html">here</a>.
      </li>
    </ul>
  </td>
</tr>
<tr>
  <td><a name="WMS">OGC Web Map Service (WMS)</a></td>
  <td>
    <table width="100%">
      <tbody><tr>
        <th colspan="2">Basic Configuration</th>
      </tr>
      <tr>
        <td>Default&nbsp;Availability</td>
        <td>Enabled</td>
      </tr>
      <tr>
        <td>Access Point</td>
        <td><code>/thredds/wms/*</code></td>
      </tr>
      <tr>
        <th colspan="2">Catalog Service Configuration<br>
          (exact values <a href="#tdsServiceElemRequirements">required</a>)
        </th>
      </tr>
      <tr>
        <td>Service Type</td>
        <td><strong>WMS</strong></td>
      </tr>
      <tr>
        <td>Service Base URL</td>
        <td><strong>/thredds/wms/</strong></td>
      </tr>
    </tbody></table>
  </td>
  <td>OGC WMS supports access to georegistered map images from geoscience datasets.
    <ul>
      <li>More details about the OGC WMS are available
        <a href="http://www.opengeospatial.org/standards/wms">here</a>.
      </li>
      <li>Enable OGC WMS and set other configuration options with the TDS
        Configuration File
        (<a href="ThreddsConfigXMLFile.html#wms">threddsConfig.xml</a>).
        More setup, configuration, and implementation details for the TDS's
        OGC WMS implementation are available <a href="WMS.html">here</a>.
        Including a link to configuration information for the underlying
        WMS implementation (ncWMS: "<a href="WMS.html">Detailed ncWMS Configuration</a>")
      </li>
    </ul>
  </td>
</tr>
<tr>
  <td><a name="HTTP">HTTP File Download</a></td>
  <td>
    <table width="100%">
      <tbody><tr>
        <th colspan="2">Basic Configuration</th>
      </tr>
      <tr>
        <td>Default&nbsp;Availability</td>
        <td>Enabled</td>
      </tr>
      <tr>
        <td>Access Point</td>
        <td><code>/thredds/fileServer/*</code></td>
      </tr>
      <tr>
        <th colspan="2">Catalog Service Configuration<br>
          (exact values <a href="#tdsServiceElemRequirements">required</a>)
        </th>
      </tr>
      <tr>
        <td>Service Type</td>
        <td><strong>HTTPServer</strong></td>
      </tr>
      <tr>
        <td>Service Base URL</td>
        <td><strong>/thredds/fileServer/</strong></td>
      </tr>
    </tbody></table>
  </td>
  <td>HTTP File Download (HTTP byte ranges are supported)
    <ul>
      <li>Files accessed through the HTTP file download have their file
        handles cached by default. Configuration settings for this caching
        can be set with the TDS Configuration File
        (<a href="ThreddsConfigXMLFile.html#FileCache">threddsConfig.xml</a>).
      </li>
    </ul>
  </td>
</tr>
  <tr>
    <td><a name="SOS">OGC Sensor Observation Service (SOS)</a></td>
    <td>
      <table width="100%">
        <tbody><tr>
          <th colspan="2">Basic Configuration</th>
        </tr>
        <tr>
          <td>Default&nbsp;Availability</td>
          <td>Disabled</td>
        </tr>
        <tr>
          <td>Access Point</td>
          <td><code>/thredds/sos/*</code></td>
        </tr>
        <tr>
          <th colspan="2">Catalog Service Configuration<br>
            (exact values <a href="#tdsServiceElemRequirements">required</a>)
          </th>
        </tr>
        <tr>
          <td>Service Type</td>
          <td><strong>Sensor Observation Service (SOS)</strong></td>
        </tr>
        <tr>
          <td>Service Base URL</td>
          <td><strong>/thredds/sos/</strong></td>
        </tr>
      </tbody></table>
    </td>
    <td>Sensor Observation Service
      <ul>
        <li>The SOS standard is applicable to use cases in which sensor data
          needs to be managed in an interoperable way. For more information,
          see the <a href="http://www.opengeospatial.org/standards/sos">OGC</a>
          SOS page, or the ncSOS
          <a href="https://github.com/asascience-open/ncSOS/wiki">wiki</a>,
          maintained by the developers of the ncSOS plugin, Applied Science
          Associates.
        </li>
      </ul>
    </td>
  </tr>
</tbody></table>

<hr width="100%">

<h2><a name="tdsServiceElemRequirements">TDS Requirements for THREDDS Catalog <code>service</code> Elements</a></h2>

<p>
  Since the TDS provides data access services at predefined URL base paths, services
  whose access is listed as a THREDDS Catalog <strong>service</strong> element:
</p>
<ul>
  <li>must use the appropriate value for the <strong>serviceType</strong> attribute</li>
  <li>must use the appropriate value for the service <strong>base</strong> URL attribute</li>
  <li>may use any value (unique to the catalog) for the service <strong>name</strong> attribute</li>
</ul>

<h3>Examples of All Individual Services</h3>

<p>Note: The required <strong>serviceType</strong> and <strong>base</strong> values are
  shown in bold.</p>

<h4>OPeNDAP</h4>
<pre>&lt;service name="odap" serviceType="<strong>OPeNDAP</strong>" base="<strong>/thredds/dodsC/</strong>"/&gt;</pre>

<h4>NetCDF Subset Service</h4>
<pre>&lt;service name="ncss" serviceType="<strong>NetcdfSubset</strong>" base="<strong>/thredds/ncss/</strong>"/&gt;</pre>

<h4>WCS</h4>
<pre> &lt;service name="wcs" serviceType="<strong>WCS</strong>" base="<strong>/thredds/wcs/</strong>"/&gt;</pre>

<h4>WMS</h4>
<pre> &lt;service name="wms" serviceType="<strong>WMS</strong>" base="<strong>/thredds/wms/</strong>"&nbsp;/&gt;</pre>

<h4>HTTP Bulk File Service</h4>
<pre>&lt;service name="fileServer" serviceType="<strong>HTTPServer</strong>" base="<strong>/thredds/fileServer/"</strong>&nbsp;/&gt;</pre>

<h4>ncISO</h4>
<pre>&lt;service name="iso" serviceType="<strong>ISO</strong>" base="<strong>/thredds/iso/"</strong>&nbsp;/&gt;</pre>
<pre>&lt;service name="ncml" serviceType="<strong>NCML</strong>" base="<strong>/thredds/ncml/"</strong>&nbsp;/&gt;</pre>
<pre>&lt;service name="uddc" serviceType="<strong>UDDC</strong>" base="<strong>/thredds/uddc/"</strong>&nbsp;/&gt;</pre>


<h4>SOS</h4>
<pre>&lt;service name="sos" serviceType="<strong>SOS</strong>" base="<strong>/thredds/sos/"</strong>&nbsp;/&gt;</pre>

<h3><a name="compoundExample">Example compound <code>service</code> Element</a></h3>

<pre>&lt;service name="all" serviceType="Compound" base=""&gt;
    &lt;service name="HTTPServer" serviceType="<strong>HTTPServer</strong>" base="<strong>/thredds/fileServer/</strong>"/&gt;
    &lt;service name="opendap" serviceType="<strong>OPENDAP</strong>" base="<strong>/thredds/dodsC/</strong>"/&gt;
    &lt;service name="ncss" serviceType="<strong>NetcdfSubset</strong>" base="<strong>/thredds/ncss/</strong>"/&gt;
    &lt;service name="cdmremote" serviceType="<strong>CdmRemote</strong>" base="<strong>/thredds/cdmremote/</strong>"/&gt;

    &lt;service name="wcs" serviceType="<strong>WCS</strong>" base="<strong>/thredds/wcs/</strong>"/&gt;
    &lt;service name="wms" serviceType="<strong>WMS</strong>" base="<strong>/thredds/wms/</strong>"/&gt;

    &lt;service name="iso" serviceType="<strong>ISO</strong>" base="<strong>/thredds/iso/</strong>"/&gt;
    &lt;service name="ncml" serviceType="<strong>NCML</strong>" base="<strong>/thredds/ncml/</strong>"/&gt;
    &lt;service name="uddc" serviceType="<strong>UDDC</strong>" base="<strong>/thredds/uddc/</strong>"/&gt;

    &lt;service name="sos" serviceType="<strong>SOS</strong>" base="<strong>/thredds/sos/</strong>"/&gt;

  &lt;/service&gt;
</pre>