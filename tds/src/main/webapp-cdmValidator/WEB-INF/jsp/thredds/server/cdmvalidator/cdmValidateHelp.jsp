<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="${standardCssUrl}"/>" type="text/css"/>

<style type="text/css">
<!--
.style1 {color: #990000}
-->
</style>

  <title>CDM Validation Help</title>
</head>

<body>
<c:import url="/WEB-INF/jsp/siteHeader.jsp" />

    <hr />
    <h1>Common Data Model Coordinate System Validation Help </h1>
    <p><em>last updated Feb 07, 2006 </em></p>
    <h3>Dataset= ecmwf_era-40_subset.nc</h3>
    <blockquote>
      <p class="style1">This is the name of the uploaded file. </p>
    </blockquote>
    <h3>Summary</h3>
    <p> No CoordSysBuilder found - using default (GDV). No 'Conventions' global attribute. </p>
    <blockquote>
      <p class="style1">If there are problems that the Validator was able to recognize, a summary of them is here. The absence of any diagnostic message here may mean that your file is ok, or it may mean that the Validator simply didnt recognize  the problem(s). </p>
      <p class="style1">In this example, the global attribute &quot;<em>Convention</em>&quot;  was not found, and no other <em><strong>CoordSysBuilder</strong></em> was found that understood this file, so the Validator is using the default. Each different <a href="http://www.unidata.ucar.edu/software/netcdf/docs/conventions.html">Convention</a>, such as CF-1.0, COARDS, etc. has its own <em><strong>CoordSysBuilder </strong></em>class in the <a href="http://www.unidata.ucar.edu/software/netcdf-java/">NetCDF-Java</a> library.</p>
    </blockquote>
    <h3>Convention=CF-1.0</h3>
    <blockquote>
      <p class="style1">This is the name of the <em><strong>CoordSysBuilder</strong></em> used by the Validator. </p>
    </blockquote>
    <h3>Coordinate Axes</h3>
    <table border="1">
      <tr>
        <th width="65">Name</th>

        <th width="181">Declaration</th>
        <th width="64">AxisType</th>
        <th width="209">units</th>
        <th width="48">udunits</th>
        <th width="48">regular</th>
      </tr>
      <tr>
        <td>

          <strong>latitude</strong>
        </td>
        <td>float latitude(latitude=73)</td>
        <td>Lat</td>
        <td>degrees_north</td>
        <td>0.0174533 rad</td>

        <td>1.003</td>
      </tr>
      <tr>
        <td>
          <strong>longitude</strong>
        </td>
        <td>float longitude(longitude=144)</td>
        <td>Lon</td>

        <td>degrees_east</td>
        <td>0.0174533 rad</td>
        <td>2.005</td>
      </tr>
      <tr>
        <td>
          <strong>time</strong>
        </td>

        <td>int time(time=62)</td>
        <td>Time</td>
        <td>hours since 1900-01-01 00:00:0.0</td>
        <td>date</td>
        <td>12</td>
      </tr>
    </table>
    <blockquote>
      <p class="style1">These are the Coordinate Axes found in this file. Coordinate Systems are built from these.</p>
      <ul>
        <li class="style1"><strong>Name</strong> : The Variable's name</li>
        <li class="style1"><strong>Declaration</strong>: data type, name, and dimensions</li>
        <li class="style1"><strong>AxisType</strong>: The Validator trys to assign an AxisType to each Coordinate Axis.</li>
        <li class="style1"><strong>units</strong>: taken from the variable's <em>units</em> attribute </li>
        <li class="style1"><strong>udunits</strong>: if this is a valid <a href="http://www.unidata.ucar.edu/software/udunits/">udunit</a>, show the <em>canonical representation</em> (in the example above, degrees are converted to radians. If its a valid <em>date</em> or <em>time</em> udunit, then &quot;date&quot; or &quot;time&quot; is shown.</li>
        <li class="style1"><strong>regular</strong>: If the axis is one-dimensional and evenly spaced, then it is a <em>regular axis</em>, and the size of its spacing is shown. WCS datasets are restricted to those in which the Lat, Lon (or GeoX, GeoY) axes are regular. </li>
      </ul>
    </blockquote>
    <h3>Grid Coordinate Systems</h3>

    <table border="1">
      <tr>
        <th>Name</th>
        <th>X</th>
        <th>Y</th>
        <th>Vertical</th>
        <th>Time</th>

      </tr>
      <tr>
        <td>time-latitude-longitude</td>
        <td>longitude</td>
        <td>latitude</td>
        <td />
        <td>time</td>

      </tr>
    </table>
    <blockquote>
      <p class="style1">These are the Coordinate Systems found in the file. This version of the Validator is only looking for gridded, georeferencing Coordinate Systems, which have an X, Y, and optional Z and T coordinate axes.</p>
    </blockquote>
    <h3>Grid variables</h3>
    <table border="1">
      <tr>
        <th>Name</th>
        <th>Declaration</th>

        <th>units</th>
        <th>udunits</th>
        <th>CoordSys</th>
      </tr>
      <tr>
        <td>
          <strong>blh</strong>

        </td>
        <td>double blh(time=62, latitude=73, longitude=144)</td>
        <td>m</td>
        <td>m</td>
        <td>time-latitude-longitude</td>
      </tr>
      <tr>
        <td>

          <strong>lcc</strong>
        </td>
        <td>double lcc(time=62, latitude=73, longitude=144)</td>
        <td>(0 - 1)</td>
        <td>false</td>
        <td>time-latitude-longitude</td>

      </tr>
      <tr>
        <td>
          <strong>p10u</strong>
        </td>
        <td>double p10u(time=62, latitude=73, longitude=144)</td>
        <td>m s**-1</td>

        <td>false</td>
        <td>time-latitude-longitude</td>
      </tr>
      <tr>
        <td>

          <strong>p2d</strong>
        </td>
        <td>double p2d(time=62, latitude=73, longitude=144)</td>
        <td>K</td>
        <td>K</td>
        <td>time-latitude-longitude</td>

      </tr>
    </table>
    <blockquote>
      <p class="style1">This is the list of data variables that have georeferencing Coordinate Systems, and are viewable through the IDV. If this list is empty, then either this dataset does not have any grids in it, or the Validator was unable to recognize them, possibly because the file does not properly implement the Convention.</p>
      <p class="style1">In this example, note that &quot;<em>(0 - 1)</em>&quot; and &quot;<em>m s**-1</em>&quot; are not valid udunits.  It is recommended that data variables use udunits, but its not necessarily fatal if they don't. &quot;<em>(0 - 1)</em>&quot; probably indicates a dimensionless flag whose value is 0 or 1. The &quot;correct&quot; udunit would be an empty string. The<em> &quot;m s**-1&quot; </em>unit should be changed to<em>&quot;m s-1&quot; </em>or<em> &quot;m/s&quot; </em></p>
  </blockquote>
    <h3>Non-Grid variables</h3>
    <table border="1">
      <tr>
        <th>Name</th>
        <th>Declaration</th>

        <th>units</th>
        <th>udunits</th>
        <th>CoordSys</th>
      </tr>
    </table>
	
	    <blockquote>
	      <p class="style1">Any variables that are not Coordinate Axes or Grid variables are listed here. </p>
  </blockquote>
	    <hr>
	    <h2>Example Problem Datasets </h2>
	    <h3>Summary:</h3>
        <ul>
          <li>No gridded data variables were found.</li>
          <li>Coordinate Axis lat does not have an assigned AxisType.</li>
          <li>Coordinate Axis lon does not have an assigned AxisType</li>
  </ul>
    <h3>Convention=CF-1.0</h3>
    <h3>Coordinate Axes</h3>
    <table border="1">
      <tr>
        <th>Name</th>

        <th>Declaration</th>
        <th>AxisType</th>
        <th>units</th>
        <th>udunits</th>
      </tr>
      <tr>
        <td>

          <strong>lat</strong>
        </td>
        <td>float lat(lat=182)</td>
        <td />
        <td>degrees north</td>
        <td>0.017453292519943295 rad</td>
      </tr>

      <tr>
        <td>
          <strong>lon</strong>
        </td>
        <td>float lon(lon=362)</td>
        <td />
        <td>degrees east</td>

        <td>0.017453292519943295 rad</td>
      </tr>
      <tr>
        <td>
          <strong>time</strong>
        </td>
        <td>float time(time=12)</td>

        <td>Time</td>
        <td>days since 0000-01-01 00:00:00</td>
        <td>date</td>
      </tr>
    </table>
    <h3>Grid Coordinate Systems</h3>
    <table border="1">

      <tr>
        <th>Name</th>
        <th>X</th>
        <th>Y</th>
        <th>Vertical</th>
        <th>Time</th>

      </tr>
    </table>
    <h3>Grid variables</h3>
    <table border="1">
      <tr>
        <th>Name</th>
        <th>Declaration</th>

        <th>units</th>
        <th>udunits</th>
        <th>CoordSys</th>
      </tr>
    </table>
    <h3>Non-Grid variables</h3>
    <table border="1">

      <tr>
        <th>Name</th>
        <th>Declaration</th>
        <th>units</th>
        <th>udunits</th>
        <th>CoordSys</th>

      </tr>
      <tr>
        <td>
          <strong>date</strong>
        </td>
        <td>int date(time=12)</td>
        <td>yyyymmdd</td>

        <td>time</td>
        <td />
      </tr>
      <tr>
        <td>
          <strong>datesec</strong>
        </td>
        <td>int datesec(time=12)</td>

        <td>seconds</td>
        <td>time</td>
        <td />
      </tr>
      <tr>
        <td>
          <strong>ifrac</strong>

        </td>
        <td>float ifrac(time=12, lat=182, lon=362)</td>
        <td />
        <td />
        <td>lat-lon-time</td>
      </tr>
      <tr>
        <td>

          <strong>sst</strong>
        </td>
        <td>float sst(time=12, lat=182, lon=362)</td>
        <td>degrees C</td>
        <td>0.017453292519943295 A.rad.s</td>
        <td>lat-lon-time</td>

      </tr>
    </table>
    <blockquote>
      <p class="style1">This is supposed to be CF-1.0, but no grid variables are being found. The problem is a bit subtle, but is indicated by the fact that the <em>lat</em> and <em>lon</em> Coordinate Axes do not have an assigned AxisType. The units look good, dont, they? On closer inspection, one sees that &quot;degrees north&quot; should be &quot;degrees_north&quot;, and &quot;degrees east&quot; should be &quot;degrees east&quot;, according to CF-1.0 sections 4.1 and 4.2. Fixing that will make this dataset correctly conform to the <a href="http://www.cgd.ucar.edu/cms/eaton/cf-metadata/index.html">CF-1.0 specification</a>. </p>
      <p class="style1">The other problem here is that &quot;degrees C&quot; is being misinterpeted as a unit of Radian - Coulombs (Ampere-second)! The correct udunit for this is <strong><em>Celsius</em></strong>. </p>
    </blockquote>

<c:import url="/WEB-INF/jsp/webappFooter.jsp" />

  </body>
</html>


