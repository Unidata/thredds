<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>NetCDF Subset Service</title>
      </head>
      <body bgcolor="#FFFFFF">

        <LINK REL="StyleSheet" HREF="/thredds/upc.css" TYPE="text/css"/>
        <table width="100%">
          <tr>
            <td width="95" height="95" align="left">
              <img src="/thredds/unidataLogo.gif" width="95" height="93"/>
            </td>
            <td width="701" align="left" valign="top">
              <table width="303">
                <tr>
                  <td width="295" height="22" align="left" valign="top">
                    <h3>
                      <strong>Thredds Data Server</strong>
                    </h3>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
        </table>

        <hr/>
        <h1>NetCDF Subset Service</h1>
        <h2>Station Dataset:
          <xsl:value-of select="stationObsDataset/@location"/>
        </h2>
        <hr/>

        <form method="GET" action="{stationObsDataset/attribute::location}">
          <table border="0" cellpadding="4" cellspacing="2">
            <tr valign="top">
              <td>

                <h3>Select Variable(s):</h3>

                <blockquote>
                  <xsl:for-each select="stationObsDataset/variable">
                    <input type="checkbox" name="var" value="{@name}"/>
                    <xsl:value-of select="@name"/>
                    <br/>

                  </xsl:for-each>
                </blockquote>

                <blockquote></blockquote>
                <input type="submit" value="Submit"/>
                <input type="reset" value="Reset"/>

              </td>

              <td>
                <h3>Choose Bounding Box, Point or Station List:</h3>
                <h4>Bounding Box (decimal degrees):</h4>
                <blockquote>
                  West Longitude:
                  <input type="text" name="west" size="14" value="{stationObsDataset/LatLonBox/west}"/>
                  <br/>
                  East Longitude:
                  <input type="text" name="east" size="14" value="{stationObsDataset/LatLonBox/east}"/>
                  <br/>
                  North Latitude:
                  <input type="text" name="north" size="12" value="{stationObsDataset/LatLonBox/north}"/>
                  <br/>
                  South Latitude:
                  <input type="text" name="south" size="12" value="{stationObsDataset/LatLonBox/south}"/>
                  <br/>
                </blockquote>
                <br/>

                <h4>Point (decimal degrees):</h4>
                <blockquote>
                  Latitude:
                  <input type="text" name="lat" size="14"/>
                  <br/>
                  Longitude:
                  <input type="text" name="lon" size="14"/>
                  <br/>
                </blockquote>
                <br/>

                <h4>Station List (comma separated)</h4>
                <blockquote>
                  <input type="text" name="stn" size="30"/>
                  <br/>
                </blockquote>
                <br/>

                <h3>Choose Time Range or Time Point:</h3>
                <h4>Time Range:</h4>
                <blockquote>
                  Starting:
                  <input type="text" name="time_start" size="12" value="{stationObsDataset/TimeSpan/begin}"/>
                  <br/>
                  Ending:
                  <input type="text" name="time_end" size="12" value="{stationObsDataset/TimeSpan/end} "/>
                  <br/>
                </blockquote>

                <h4>Time Point:</h4>
                <blockquote>
                  Time:
                  <input type="text" name="time" size="12" value="{stationObsDataset/TimeSpan/begin}"/>
                  <br/>
                </blockquote>
                <br/>

                <h3>Choose Output Format:</h3>
                <blockquote>
                  <select name="accept" size="1">
                    <xsl:for-each select="stationObsDataset/AcceptList/accept">
                      <option>
                        <xsl:value-of select="."/>
                      </option>

                    </xsl:for-each>
                  </select>
                </blockquote>

                <br/>

              </td>
            </tr>
          </table>
        </form>

      </body>
    </html>

  </xsl:template>
</xsl:stylesheet>
