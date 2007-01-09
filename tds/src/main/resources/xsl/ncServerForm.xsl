<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>NetCDF Server</title>
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
        <h1>NetCDF Grid Subset Server</h1>
        Select Grids,  optionally bounding box and time range. A NetCDF file using CF-1 Conventions is returned.
        <h2>Dataset: 
          <xsl:value-of select="forecastModelRun/@name"/>
        </h2>
        <h3>Base Time:
          <xsl:value-of select="forecastModelRun/@runTime"/>
        </h3>
        <hr/>
        
        <form method="GET" action="{forecastModelRun/attribute::name}">
          <table border="0" cellpadding="4" cellspacing="2">
            <tr valign="top"> 
              <td>
         
             <xsl:for-each select="forecastModelRun/offsetHours">
               <strong> Available Forecast Hours= </strong>(<xsl:value-of select="text()"/>)
               <h3> Select Grid(s):</h3>
               
                  <blockquote>
                      <xsl:for-each select="./variable">
                        <input type="checkbox" name="grid" value="{@name}" /><xsl:value-of select="@name"/><br/>          
                    </xsl:for-each>
                  </blockquote>
                  </xsl:for-each>
              </td>
              
          <td>
            <h3> Bounding Box (decimal degrees):</h3>
            <blockquote>
              West Longitude: <input type="text" name="west" size="14" value="{forecastModelRun/horizBB/attribute::west}"/> <br/>
              East Longitude: <input type="text" name="east" size="14"  value="{forecastModelRun/horizBB/attribute::east}"/> <br/>
              North Latitude: <input type="text" name="north" size="12"  value="{forecastModelRun/horizBB/attribute::north}"/> <br/>
              South Latitude: <input type="text" name="south" size="12"  value="{forecastModelRun/horizBB/attribute::south}"/> <br/>
            </blockquote>
            <br/>
            <h3> Forecast Hours:</h3>
            <blockquote>
              Starting: <input type="text" name="time_start" size="10"  /> <br/>
              Ending: <input type="text" name="time_end" size="10"/> <br/>

            </blockquote>
            
            <br/>
            <strong>Add Lat/Lon variables if needed</strong>
            <input type="checkbox" name="addLatLon"  value="true" />
            <br/>
            <blockquote></blockquote>
            <input type="submit" value="Submit"/>
            <input type="reset" value="Reset"/>
            
          </td>
              </tr>            
            </table>
        </form>

      </body>
    </html>

  </xsl:template>
</xsl:stylesheet>
