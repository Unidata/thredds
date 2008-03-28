<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>Radar Subset Service</title>
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
        <h1>Radar Subset Service</h1>
        <h2>Dataset:
          <xsl:value-of select="RadarNexrad/@location"/></h2>
        <em><a href="dataset.xml">Dataset Description</a></em>
        <hr/>

        <form method="GET" action="{RadarNexrad/attribute::location}">
          <table border="0" cellpadding="4" cellspacing="2">
            <tr valign="top">
              <td>

                <h3>Select Variable(s):</h3>
                <input type="radio" name="variables" value="all" checked="checked"> <b>All</b></input><br/>
                <input type="radio" name="variables" value="some"> <b>Only:</b></input>
                <blockquote>
                  <xsl:for-each select="RadarNexrad/variable">
                    <input type="checkbox" name="var" value="{@name}"/>
                    <xsl:value-of select="@name"/>
                    <br/>

                  </xsl:for-each>
                </blockquote>
              </td>

              <td>
                <h3>Choose Spatial Subset:</h3>               
                <input type="radio" name="spatial" value="all" checked="checked"> <b>All</b></input>
                <br/>               
                <input type="radio" name="spatial" value="bb">               
                <b>Bounding Box (decimal degrees):</b>
                  <blockquote>
                    <blockquote>                   
                      <p>North</p>
                      <p><input type="text" name="north" size="10" value="{RadarNexrad/LatLonBox/north}"/></p>
                    </blockquote>
                  </blockquote>                  
                  West
                  <input type="text" name="west" size="10" value="{RadarNexrad/LatLonBox/west}"/>
                  <input type="text" name="east" size="10" value="{RadarNexrad/LatLonBox/east}"/> East
                      <blockquote>
                        <blockquote>
                          <p>              
                            <input type="text" name="south" size="10" value="{RadarNexrad/LatLonBox/south}"/>
                          </p>
                          <p>South</p>
                        </blockquote>
                      </blockquote>                  
                 </input>
                <br/>

                  <input type="radio" name="spatial" value="point"> 
                   <b> Closest  to this location  (decimal degrees):</b>
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

                 <input type="radio" name="spatial" value="stns"> 
                <b>Station List (comma separated, no spaces)<a href="stations.xml"> Available Stations</a></b>
                <blockquote>
                  <input type="text" name="stn" size="30"/>
                  <br/>
                </blockquote>
                   </input>
                <br/>

                <h3>Choose Time Subset:</h3>
                <input type="radio" name="temporal" value="all" checked="checked"> <b>All</b></input>
                 <br/>
                <input type="radio" name="temporal" value="range"> 
                <b>Time Range:</b>
                <blockquote>
                  Starting:
                  <input type="text" name="time_start" size="20" value="{RadarNexrad/TimeSpan/begin}"/>
                  <br/>
                  Ending:
                  <input type="text" name="time_end" size="20" value="{RadarNexrad/TimeSpan/end}"/>
                  <br/>
                </blockquote>
                  </input>

                 <input type="radio" name="temporal" value="point"> 
                 <b>Specific Time (closest):</b>
                <blockquote>
                  Time:
                  <input type="text" name="time" size="20" value="{RadarNexrad/TimeSpan/begin}"/>
                  <br/>
                </blockquote>
                   </input>
                <br/>

                <h3>Choose Output Format:</h3>
                <blockquote>
                  <select name="accept" size="1">
                    <xsl:for-each select="RadarNexrad/AcceptList/accept">
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
        <h3><a href="http://www.unidata.ucar.edu/projects/THREDDS/tech/interfaceSpec/RadarLevel2SubsetService.html">Radar Level2 Service Documentation</a></h3>
        <h3><a href="http://www.unidata.ucar.edu/projects/THREDDS/tech/interfaceSpec/RadarLevel3SubsetService.html">Radar Level3 Service Documentation</a></h3>  
      </body>
    </html>

  </xsl:template>
</xsl:stylesheet>
