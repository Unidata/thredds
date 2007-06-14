<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>NetCDF Subset Service for Grids as Point Data</title>
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
        <h1>NetCDF Subset Service for Grids  as Point Data</h1>
        <h2>Dataset: <xsl:value-of select="gridForm/@location"/></h2>
        <h3>Base Time: <xsl:value-of select="gridForm/TimeSpan/begin"/></h3>
        <em>
          <a href="dataset.xml">Gridded Dataset Description</a>
        </em>
        <br/>
        <em>
          <a href="dataset.html">As Gridded Dataset</a>
        </em>
        <hr/>

        <form method="GET" action="{gridForm/attribute::location}">
          <table border="0" cellpadding="4" cellspacing="2">
            <tr valign="top">
              <td>
                You must select at least one Variable and a Lat/Lon location.

                <h3>Select Variable(s):</h3>
                <!--input type="checkbox" name="variables" value="all" checked="checked"> <b>All</b></input><br/>
                <input type="radio" name="variables" value="some"> <b>Only:</b></input !-->

                <xsl:for-each select="gridForm/timeSet">

                  <xsl:if test="time">
                    <xsl:if test="time/values/@npts &lt; 100">
                      <strong>Variables with available Times: </strong>
                      <xsl:value-of select="time/values"/>
                      <xsl:for-each select="time/attribute[@name='units']">
                        <em> <xsl:value-of select="@value"/> </em>
                      </xsl:for-each>
                      </xsl:if>
                  </xsl:if>

                  <blockquote>
                    <xsl:for-each select="vertSet">
                      <xsl:if test="vert">
                        <strong>with Vertical Levels (<xsl:value-of select="vert/@name"/>) :</strong>
                        
                        <xsl:value-of select="vert/values"/>
                        <xsl:for-each select="vert/attribute[@name='units']">
                          <em> <xsl:value-of select="@value"/> </em>
                        </xsl:for-each>
                      </xsl:if>
                      <br/>
                      <xsl:for-each select="grid">
                        <input type="checkbox" name="var" value="{@name}"/>
                        <xsl:value-of select="@name"/>
                        <br/>
                      </xsl:for-each>
                      <br/>
                      
                    </xsl:for-each>
                  </blockquote>
                  <br/>


                </xsl:for-each>

              </td>

              <td>
                <td>
                  <h3>Choose Lat/Lon Location:</h3>
                  <blockquote>
                    Latitude:
                    <input type="text" name="latitude" size="10"/>
                    <br/>
                    Longitude:
                    <input type="text" name="longitude" size="10"/>
                    <br/>
                  </blockquote>
                  <b>Within Bounding Box:</b>
                  <blockquote>
                    North: <xsl:value-of select="gridForm/LatLonBox/north"/><br/>
                    South: <xsl:value-of select="gridForm/LatLonBox/south"/><br/>
                    East: <xsl:value-of select="gridForm/LatLonBox/east"/><br/>
                    West: <xsl:value-of select="gridForm/LatLonBox/west"/><br/>
                  </blockquote>
                  <br/>
                  
                  <h3>Choose Time Subset:</h3>
                  <input type="radio" name="temporal" value="all" checked="checked"> <b>All</b></input>
                  <br/>
                  <input type="radio" name="temporal" value="range"> 
                    <b>Time Range:</b>
                    <blockquote>
                      Starting:
                      <input type="text" name="time_start" size="20" value="{gridForm/TimeSpan/begin}"/>
                      <br/>
                      Ending:
                      <input type="text" name="time_end" size="20" value="{gridForm/TimeSpan/end}"/>
                      <br/>
                    </blockquote>
                  </input>
                  
                  <input type="radio" name="temporal" value="point"> 
                    <b>Specific Time (closest):</b>
                    <blockquote>
                      Time:
                      <input type="text" name="time" size="20" value="{gridForm/TimeSpan/begin}"/>
                      <br/>
                    </blockquote>
                  </input>
                  <br/>
                  
                  <h3>Choose Vertical Level:</h3>
                  <blockquote>
                    Level:
                    <input type="text" name="vertCoord" size="10"/>
                    <br/>
                  </blockquote>
                  <br/>
                  
                  <h3>Choose Output Format:</h3>
                  <blockquote>
                    <select name="accept" size="1">
                      <xsl:for-each select="gridForm/AcceptList/accept">
                        <option>
                          <xsl:value-of select="."/>
                        </option>
                        
                      </xsl:for-each>
                    </select>
                  </blockquote>              
                  <br/>
                <input type="hidden" name="point" value="true"/>
                </td>
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
          <a
            href="http://www.unidata.ucar.edu/projects/THREDDS/tech/interfaceSpec/GridDataSubsetService.html"
            >NetCDF Subset Service Documentation</a>
        </h3>
      </body>
    </html>

  </xsl:template>
</xsl:stylesheet>
