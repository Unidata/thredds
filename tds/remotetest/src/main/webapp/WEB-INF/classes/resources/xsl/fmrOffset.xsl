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
            <li><a href="{@name}/"><xsl:value-of select="@name"/></a>
              <xsl:choose>
                <xsl:when test="@percent"> <strong> (<xsl:value-of select="@count"/>)</strong></xsl:when>
                <xsl:otherwise> (<xsl:value-of select="@count"/>)</xsl:otherwise>
              </xsl:choose>
            </li>
          </xsl:for-each>
        </ol>
        <hr/>

        <h3>Number of 2D records present (or)  Number present / Number expected.</h3>
        
        <table border="1" >
          <tr>
            <td/>
            <th colspan="100">Valid Time offset  (hours)</th>
          </tr>
          <tr>
            <th>Run Time</th>
            <th>Total</th>
            <xsl:for-each select="forecastModelRunCollectionInventory/offsetTime">
              <th>
                <xsl:value-of select="@hours"/>
              </th>
            </xsl:for-each>
          </tr>
          
          <xsl:for-each select="forecastModelRunCollectionInventory/run">
            <tr>
              <th><xsl:value-of select="@date"  /></th>
 
              <td align="center">              
                <xsl:value-of select="@count"/>
              </td>  
              
              <xsl:for-each select="./offset">
                  <xsl:choose>
                    <xsl:when test="@percent"> <td align="center"  bgcolor="#ff5555"> <strong> <xsl:value-of select="@count"/></strong></td></xsl:when>
                    <xsl:otherwise> <td align="center"  bgcolor="#00ff00"> <xsl:value-of select="@count"/> </td></xsl:otherwise>
                  </xsl:choose>        
              </xsl:for-each>
            </tr>
          </xsl:for-each>
        </table>
        
        <br/>  
        Show as <a href="?Matrix">Valid Time vs. Run Time</a>
        
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
