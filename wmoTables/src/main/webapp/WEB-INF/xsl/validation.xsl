<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html"/>
  <xsl:template match="/">
    <html>
      <head>
        <title>BUFR Validation Results</title>
      </head>
      <body bgcolor="#FFFFFF">
     
        <h1>BUFR Validation Results</h1>
        <h2>Dataset=<xsl:value-of select="bufrValidation/@fileName"/>
        </h2>

        <table border="1">
          <tr>
            <th>data</th>
            <th>nobs</th>
            <th>WMO Header</th>
            <th>center</th>
            <th>category</th>
            <th>date</th>
            <th>dds</th>
            <th>bitCount</th>
            <th>table</th>
          </tr>
          <xsl:for-each select="bufrValidation/bufrMessage">
            <tr>
              <td> 
                <a href="mess/{/bufrValidation/@fileName}/{@pos}/data.xml"><xsl:value-of select="@record"/></a>
              </td>
              <td>
                <xsl:value-of select="@nobs"/>
              </td>
              <td>
                <xsl:value-of select="WMOheader"/>
              </td>
              <td>
                <xsl:value-of select="center"/>
              </td>
              <td>
                <xsl:value-of select="category"/>
              </td>
              <td>
                <xsl:value-of select="date"/>
              </td>
              <td>
                <a href="mess/{/bufrValidation/@fileName}/{@pos}/dds.txt"><xsl:value-of select="@dds"/></a>
              </td>
              <td>
                <a href="mess/{/bufrValidation/@fileName}/{@pos}/bitCount.txt"><xsl:value-of select="@size"/></a>
              </td>       
              <td>
                <a href="mess/{/bufrValidation/@fileName}/{@pos}/table.txt"><xsl:value-of select="@size"/></a>
              </td>       
            </tr>
          </xsl:for-each>
        </table>

        <a href="../validateHelp.html">Validation Help</a>

      </body>
    </html>

  </xsl:template>

</xsl:stylesheet>
