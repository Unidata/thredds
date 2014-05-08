<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html"/>
  <xsl:template match="/">
    <html>
      <head>
        <title>ForcastModelRun Inventory </title>
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
        
        <h1>ForecastModelRun Inventory</h1>
        <h2>Dataset= <xsl:value-of select="forecastModelRunCollectionInventory/@dataset"/></h2>

        <h3>Grids:</h3>
        <ol>
          <xsl:for-each select="forecastModelRunCollectionInventory/variable">
            <li><a href="{@name}"><xsl:value-of select="@name"/></a> (<xsl:value-of select="@count"/>)</li>
          </xsl:for-each>
        </ol>
        <hr/>

        <h3>Number of 2D records present (or)  Number present / Number expected.</h3>
        
        <table border="1">
          <tr>
            <td/>
            <th colspan="100">Run Time</th>
          </tr>
          <tr>
            <th>Valid Time</th>
            <xsl:for-each select="forecastModelRunCollectionInventory/run">
              <td>
                <xsl:value-of select="@date"/>
              </td>
            </xsl:for-each>
          </tr>
          <xsl:for-each select="forecastModelRunCollectionInventory/forecastTime">
            <tr>
              <td>
                <xsl:value-of select="@date"/>
              </td>
              <xsl:for-each select="./runTime">
                <xsl:choose>
                  <xsl:when test="@percent"><th bgcolor="#ff5555"><xsl:value-of select="@count"  /></th></xsl:when>
                  <xsl:when test="@count"><th bgcolor="#00ff00"><xsl:value-of select="@count"  /></th></xsl:when>
                  <xsl:otherwise><th/></xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </tr>
          </xsl:for-each>
        </table>
        
        <br/>
        Show as  <a href="?Offset"> Runtime vs. Offset</a>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
