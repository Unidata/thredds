<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

  <xsl:output method="html"/>

  <!-- Gets the tds context as a xslt parameter -->
  <xsl:param name="tdsContext"></xsl:param>

  <!-- Sets the paths that depends on the tdsContext -->
  <xsl:variable name="cssMainPath">
    <xsl:value-of select="concat($tdsContext,'/style/ncss/main.css')"></xsl:value-of>
  </xsl:variable>
  <xsl:variable name="cssLayoutPath">
    <xsl:value-of select="concat($tdsContext,'/style/ncss/layout.css')"></xsl:value-of>
  </xsl:variable>
  <xsl:variable name="cssFormPath">
    <xsl:value-of select="concat($tdsContext,'/style/ncss/form.css')"></xsl:value-of>
  </xsl:variable>

  <xsl:variable name="logoPath">
    <xsl:value-of select="concat($tdsContext,'/unidataLogo.gif')"></xsl:value-of>
  </xsl:variable>

  <xsl:template match="/">


    <html>
      <head>
        <title>Netcdf Subset Service Station Feature Interface</title>

        <xsl:element name="link">
          <xsl:attribute name="rel">StyleSheet</xsl:attribute>
          <xsl:attribute name="type">text/css</xsl:attribute>
          <xsl:attribute name="href">
            <xsl:value-of select="$cssMainPath"></xsl:value-of>
          </xsl:attribute>
        </xsl:element>
        <xsl:element name="link">
          <xsl:attribute name="rel">StyleSheet</xsl:attribute>
          <xsl:attribute name="type">text/css</xsl:attribute>
          <xsl:attribute name="href">
            <xsl:value-of select="$cssLayoutPath"></xsl:value-of>
          </xsl:attribute>
        </xsl:element>
        <xsl:element name="link">
          <xsl:attribute name="rel">StyleSheet</xsl:attribute>
          <xsl:attribute name="type">text/css</xsl:attribute>
          <xsl:attribute name="href">
            <xsl:value-of select="$cssFormPath"></xsl:value-of>
          </xsl:attribute>
        </xsl:element>

        <script type="text/javascript">

          var context = '<xsl:value-of select="$tdsContext"></xsl:value-of>';

          var Ncss ={};

          Ncss.debug = true;

          Ncss.log = function(message){
          if(Ncss.debug){
          console.log(message);
          }
          };

          //Dynamic load of the javascript files
          (function(){

          //jQuery
          var headTag = document.getElementsByTagName("head")[0];
          var jQueryfile = document.createElement('script');
          jQueryfile.setAttribute("type", "text/javascript");
          jQueryfile.setAttribute("src",
          context+"/js/lib/jquery-1.7.2.min.js");
          headTag.appendChild(jQueryfile);

          //ncssApp.js
          var ncssAppfile = document.createElement('script');
          ncssAppfile.setAttribute("type", "text/javascript");
          ncssAppfile.setAttribute("src", context+"/js/ncss/ncssApp.js");
          var headTag = document.getElementsByTagName("head")[0];
          headTag.appendChild(ncssAppfile);

          //form.js
          var jsfile = document.createElement('script');
          jsfile.setAttribute("type", "text/javascript");
          jsfile.setAttribute("src", context+"/js/ncss/stationDatasetForm.js");
          var headTag = document.getElementsByTagName("head")[0];
          headTag.appendChild(jsfile);
          })();
        </script>
      </head>

      <body onload="Ncss.initStationDataset()">
        <!-- Header -->
        <div id="header">
          <div id="dataset">
            <h1>
              NCSS Station Feature
            </h1>
          </div>
          <div id="unidata">
            <div id="title">
              <div id="service">
                <span class="bold">THREDDS data server</span>
                <span class="service">NetCDF Subset Service</span>
              </div>
            </div>
            <div id="logo">
              <span></span>
            </div>
          </div>
        </div>
        <!-- Main content -->
        <div id="container">
          <div id="dataheader">
            <h2>
              <span>Dataset:</span>
              <span class="black">
                <xsl:value-of select="capabilities/@location"/>
              </span>
              (
              <a href="dataset.xml">Dataset Description</a>
              |
              <a href="station.xml">Station list</a>
              )
            </h2>
            <h3>
              <span>Base Time:</span>
              <span class="black">
                <xsl:value-of select="capabilities/TimeSpan/begin"/>
              </span>
            </h3>
          </div>

          <form id="form" method="GET" action="{capabilities/attribute::location}">
            <input type="hidden" name="req" value="station"/>
            <table class="simple">
              <tr valign="top">
                <td class="leftCol">
                  <h3>Select Variable(s):</h3>
                  <xsl:for-each select="capabilities/variable">
                    <input type="checkbox" name="var" value="{@name}"/>
                    <xsl:value-of select="@name"/>
                    <br/>
                  </xsl:for-each>
                </td>
                <td class="rightCol">
                  <h3>Choose Spatial Subset:</h3>
                  <!-- Three tabs: bbox, point, stn list -->
                  <div id="inputBBoxSubset" class="selected">
                    <span class="bold">Bbox subset</span>
                  </div>
                  <div id="inputPointSubset" class="unselected">
                    <span class="bold">Point subset</span>
                  </div>
                  <div id="inputStationList" class="unselected">
                    <span class="bold">Station list</span>
                  </div>

                  <div id="areaInput" class="clear">
                    <div id="spatialSubset">
                      <!-- lat/lon subsetting -->
                      <div id="bboxSubset" class="absoluteTopLeft borderLightGrey">
                        <span class="bold">Bounding Box (decimal degrees):</span>
                        <div class="top">
                          <span>north</span>
                          <br/>
                          <input type="text" name="north" size="8" value="{capabilities/LatLonBox/north}"/>
                          <input type="hidden" disabled="disabled" name="dis_north" size="8" value="{capabilities/LatLonBox/north}"/>
                        </div>
                        <div>
                          west
                          <input type="text" name="west" size="8" value="{capabilities/LatLonBox/west}"/>
                          <input type="hidden" disabled="disabled" name="dis_west" size="8" value="{capabilities/LatLonBox/west}"/>
                          <input type="text" name="east" size="8" value="{capabilities/LatLonBox/east}"/>
                          <input type="hidden" disabled="disabled" name="dis_east" size="8" value="{capabilities/LatLonBox/east}"/>
                          east
                        </div>
                        <div class="top">
                          <input type="text" name="south" size="8" value="{capabilities/LatLonBox/south}"/>
                          <input type="hidden" disabled="disabled" name="dis_south" size="8" value="{capabilities/LatLonBox/south}"/>
                          <br/>
                          <span>south</span>
                        </div>
                        <!--
                        <div>
                          <input type="checkbox" name="disableLLSubset" id="disableLLSubset" />
                          <span>Disable horizontal subsetting</span>
                        </div>
                         -->
                        <span class="blueLink" id="resetLatLonbbox">reset to full extension</span>
                      </div>
                      <!-- Point subsetting -->
                      <div id="pointSubset" class="hidden absoluteTopLeft borderLightGrey">
                        <label class="sized_big">Latitude:  </label>
                        <input disabled="disabled" type="text" name="latitude" size="10"/>
                        <br/>
                        <label class="sized_big">Longitude:  </label>
                        <input disabled="disabled" type="text" name="longitude" size="10"/>
                        <br/>
                        <span class="bold">Within Bounding Box:</span>
                        <div class="top">
                          <span>north</span>
                          <br/>
                          <input disabled="disabled" type="text" name="north" size="8" value="{capabilities/LatLonBox/north}"/>
                        </div>
                        <div>
                          <span>west</span>
                          <input disabled="disabled" type="text" name="west" size="8" value="{capabilities/LatLonBox/west}"/>
                          <input disabled="disabled" type="text" name="east" size="8" value="{capabilities/LatLonBox/east}"/>
                          <span>east</span>
                        </div>
                        <div class="top">
                          <input disabled="disabled" type="text" name="south" size="8" value="{capabilities/LatLonBox/south}"/>
                          <br/>
                          <span>south</span>
                        </div>
                      </div>
                      <!-- List subsetting -->
                      <div id="listSubset" class="hidden absoluteTopLeft borderLightGrey">
                        <input type="checkbox" name="stns_all" value="" id="stns_all"/>
                        <label>All stations</label>
                        <br/>
                        <label>Station list (comma separated):</label>
                        <input type="text" name="stns" id="stns" size="30" disabled="disabled"/>
                        <input type="hidden" name="subset" value="stns" size="30" disabled="disabled"/>
                      </div>
                    </div>
                    <br clear="all"/>
                  </div>
                  <br clear="all"/>

                  <!-- time subsetting -->
                  <h3>Choose Time Subset:</h3>
                  <div id="inputTimeRange" class="selected">
                    <span class="bold">Time range</span>
                  </div>
                  <div id="inputSingleTime" class="unselected">
                    <span class="bold">Single time</span>
                  </div>

                  <div id="timeInput" class="clear">
                    <div id="temporalSubsetWithStride">
                      <!-- Time range -->
                      <div id="timeRangeSubset" class="absoluteTopLeft borderLightGrey">
                        <label class="sized">Start:  </label>
                        <input type="text" name="time_start" size="21" value="{capabilities/TimeSpan/begin}"/>
                        <input type="hidden" disabled="disabled" name="dis_time_start" size="21" value="{capabilities/TimeSpan/begin}"/>
                        <br/>
                        <label class="sized">End:  </label>
                        <input type="text" name="time_end" size="21" value="{capabilities/TimeSpan/end}"/>
                        <input type="hidden" disabled="disabled" name="dis_time_end" size="21" value="{capabilities/TimeSpan/end}"/>
                        <br/>
                        <span class="blueLink" id="resetTimeRange">reset to full extension</span>
                      </div>
                      <div id="singleTimeSubset" class="hidden absoluteTopLeft borderLightGrey">
                        <label class="sized">Time:  </label>
                        <input type="text" name="time" size="21" disabled="disabled" value="{capabilities/TimeSpan/begin}"/>
                      </div>
                    </div>
                  </div>

                  <!-- Output format -->
                  <br clear="all"/>
                  <h3>Choose Output Format:</h3>
                  <div class="borderLightGrey">
                    <label class="sized">Format:  </label>
                    <select name="accept" size="1">
                      <xsl:for-each select="capabilities/AcceptList/accept">
                        <option value="{.}">
                          <xsl:value-of select="@displayName"/>
                        </option>
                      </xsl:for-each>
                    </select>
                  </div>
                </td>
              </tr>
              <tr>
                <td colspan="2" class="center">
                  <input class="button" type="submit" value="Submit"/>
                  <input class="button" type="reset" value="Reset"/>
                </td>
              </tr>
            </table>
          </form>
        </div>
        <hr/>
        <h3>
          <a href="http://www.unidata.ucar.edu/software/thredds/current/tds/reference/NetcdfSubsetServiceReference.html">NetCDF Subset Service Documentation</a>
        </h3>
      </body>
    </html>

  </xsl:template>
</xsl:stylesheet>
