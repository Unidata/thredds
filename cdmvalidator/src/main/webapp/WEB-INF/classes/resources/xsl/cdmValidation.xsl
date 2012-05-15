<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html"/>
  <xsl:template match="/">
    <html>
      <head>
        <LINK REL="StyleSheet" HREF="/cdmvalidator/tds.css" TYPE="text/css"/>
        <title>CDM Validation</title>
      </head>
      <body bgcolor="#FFFFFF">

        <table width="100%">
          <tr>
            <td>
              <img src="/cdmvalidator/unidataLogo.gif" alt="Unidata" align="left" valign="top" />
              <h3>
                <strong>
                  <a href="/cdmvalidator/validate.html">CDM Validator</a>
                </strong>
              </h3>
            </td>
          </tr>
        </table>
        <hr/>
        <h1>Common Data Model Coordinate System Validation</h1>
        <h2>Dataset=
          <xsl:value-of select="netcdfDatasetInfo/@location"/>
        </h2>
        <h3>Summary:</h3>
        <ul>
          <xsl:for-each select="netcdfDatasetInfo/userAdvice">
            <li>
              <xsl:value-of select="."/>
            </li>
          </xsl:for-each>
        </ul>
        <h3>Convention=
          <xsl:value-of select="netcdfDatasetInfo/convention/@name"/>
        </h3>

        <h3>Coordinate Axes</h3>

        <table border="1">
          <tr>
            <th>Name</th>
            <th>Declaration</th>
            <th>AxisType</th>
            <th>units</th>
            <th>udunits</th>
            <th>regular</th>
          </tr>
          <xsl:for-each select="netcdfDatasetInfo/axis">
            <xsl:sort select="@name"/>
            <tr>
              <td>
                <strong>
                  <xsl:value-of select="@name"/>
                </strong>
              </td>
              <td>
                <xsl:value-of select="@decl"/>
              </td>
              <td>
                <xsl:value-of select="@type"/>
              </td>
              <td>
                <xsl:value-of select="@units"/>
              </td>
              <td>
                <xsl:value-of select="@udunits"/>
              </td>
              <td>
                <xsl:value-of select="@regular"/>
              </td>
            </tr>
          </xsl:for-each>
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
          <xsl:for-each select="netcdfDatasetInfo/gridCoordSystem">
            <xsl:sort select="@name"/>
            <tr>
              <td>
                <xsl:value-of select="@name"/>
              </td>
              <td>
                <xsl:value-of select="@horizX"/>
              </td>
              <td>
                <xsl:value-of select="@horizY"/>
              </td>
              <td>
                <xsl:value-of select="@vertical"/>
              </td>
              <td>
                <xsl:value-of select="@time"/>
              </td>
            </tr>
          </xsl:for-each>
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
          <xsl:for-each select="netcdfDatasetInfo/grid">
            <xsl:sort select="@name"/>
            <tr>
              <td>
                <strong>
                  <xsl:value-of select="@name"/>
                </strong>
              </td>
              <td>
                <xsl:value-of select="@decl"/>
              </td>
              <td>
                <xsl:value-of select="@units"/>
              </td>
              <td>
                <xsl:value-of select="@udunits"/>
              </td>
              <td>
                <xsl:value-of select="@coordSys"/>
              </td>
            </tr>
          </xsl:for-each>
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
          <xsl:for-each select="netcdfDatasetInfo/variable">
            <xsl:sort select="@name"/>
            <tr>
              <td>
                <strong>
                  <xsl:value-of select="@name"/>
                </strong>
              </td>
              <td>
                <xsl:value-of select="@decl"/>
              </td>
              <td>
                <xsl:value-of select="@units"/>
              </td>
              <td>
                <xsl:value-of select="@udunits"/>
              </td>
              <td>
                <xsl:value-of select="@coordSys"/>
              </td>
            </tr>
          </xsl:for-each>
        </table>

        <hr/>
        <a href="validateHelp.html">Validation Help</a>

      </body>
    </html>

  </xsl:template>

  <xsl:template match="userAdvice">
    <h3>Summary=
      <xsl:value-of select="."/>
    </h3>
  </xsl:template>

</xsl:stylesheet>
