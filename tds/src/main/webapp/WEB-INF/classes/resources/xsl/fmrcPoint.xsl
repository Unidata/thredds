<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>CDM Remote Subset Service Forms Interface</title>
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
        <h1>CDM Remote Subset Service</h1>
        <h2>Point Dataset:
          <xsl:value-of select="capabilities/@location"/>
        </h2>
        <em>
          <a href="?req=capabilities">Dataset Description</a>
        </em>
        <hr/>

        <form method="GET" action="{capabilities/attribute::location}">
          <input type="hidden" name="req" value="dataForm"/>
          <table border="0" cellpadding="4" cellspacing="2">
            <tr valign="top">
              <td>

                <h3>Select Variable(s):</h3>
                <input type="radio" name="variables" value="all" checked="checked">
                  <b>All</b>
                </input>
                <br/>
                <input type="radio" name="variables" value="some">
                  <b>Only:</b>
                </input>
                <blockquote>
                  <xsl:for-each select="capabilities/variable">
                    <input type="checkbox" name="var" value="{@name}"/>
                    <xsl:value-of select="@name"/>
                    <br/>

                  </xsl:for-each>
                </blockquote>
              </td>

              <td>
                <h3>Choose Spatial Subset:</h3>
                <input type="radio" name="spatial" value="all" checked="checked">
                  <b>All</b>
                </input>
                <br/>
                <input type="radio" name="spatial" value="bb">
                  <b>Bounding Box (decimal degrees):</b>
                  <blockquote>
                    <blockquote>
                      <p>North</p>
                      <p>
                        <input type="text" name="north" size="10" value="{capabilities/LatLonBox/north}"/>
                      </p>
                    </blockquote>
                  </blockquote>
                  West
                  <input type="text" name="west" size="10" value="{capabilities/LatLonBox/west}"/>
                  <input type="text" name="east" size="10" value="{capabilities/LatLonBox/east}"/>
                  East
                  <blockquote>
                    <blockquote>
                      <p>
                        <input type="text" name="south" size="10" value="{capabilities/LatLonBox/south}"/>
                      </p>
                      <p>South</p>
                    </blockquote>
                  </blockquote>
                </input>
                <br/>

                <input type="radio" name="spatial" value="point">
                  <b>Closest to this location (decimal degrees):</b>
                  <blockquote>
                    Latitude:
                    <input type="text" name="latitude" size="10"/>
                    <br/>
                    Longitude:
                    <input type="text" name="longitude" size="10"/>
                    <br/>
                  </blockquote>
                </input>
                <br/>

                <h3>Choose Time Subset:</h3>
                <input type="radio" name="temporal" value="all" checked="checked">
                  <b>All</b>
                </input>
                <br/>
                <input type="radio" name="temporal" value="range">
                  <b>Time Range:</b>
                  <blockquote>
                    Starting:
                    <input type="text" name="time_start" size="20" value="{capabilities/TimeSpan/begin}"/>
                    <br/>
                    Ending:
                    <input type="text" name="time_end" size="20" value="{capabilities/TimeSpan/end}"/>
                    <br/>
                  </blockquote>
                </input>

                <input type="radio" name="temporal" value="point">
                  <b>Specific Time (closest):</b>
                  <blockquote>
                    Time:
                    <input type="text" name="time" size="20" value="{capabilities/TimeSpan/begin}"/>
                    <br/>
                  </blockquote>
                </input>
                <br/>

                <h3>Choose Output Format:</h3>
                <blockquote>
                  <select name="accept" size="1">
                    <xsl:for-each select="capabilities/AcceptList/accept">
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
          <div align="center">
            <table width="600" border="0">
              <tr align="center">
                <input type="submit" value="Submit"/>
                <input type="reset" value="Reset"/>
              </tr>
            </table>
          </div>
        </form>
        <hr/>
        <h3>
          <a href="http://www.unidata.ucar.edu/software/netcdf-java/stream/CdmRemote.html">CDM Remote Documentation</a>
        </h3>
      </body>
    </html>

  </xsl:template>
</xsl:stylesheet>
