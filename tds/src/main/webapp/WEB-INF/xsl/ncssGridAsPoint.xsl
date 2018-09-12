<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
    <xsl:output method="html" encoding="utf-8" indent="yes"/>

    <!-- Gets the tds context as a xslt parameter -->
    <xsl:param name="tdsContext"></xsl:param>
    <xsl:param name="gridWKT"></xsl:param>

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

    <xsl:variable name="olcssPath">
        <xsl:value-of select="concat($tdsContext,'/js/lib/OpenLayers-2.13.1/theme/default/style.css')"></xsl:value-of>
    </xsl:variable>

    <xsl:variable name="logoPath">
        <xsl:value-of select="concat($tdsContext,'/unidataLogo.gif')"></xsl:value-of>
    </xsl:variable>

    <xsl:template match="/">
        <xsl:variable name="hasTimeAxis">
            <xsl:value-of select="count(/gridForm/timeSet/time)"/>
        </xsl:variable>

        <xsl:variable name="hasVertAxis">
            <xsl:value-of select="count(gridForm/timeSet/vertSet/vert/values)"/>
        </xsl:variable>

        <html>
            <head>
                <title>NetCDF Subset Service for Grids as Point Data</title>

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

                <xsl:element name="link">
                    <xsl:attribute name="rel">StyleSheet</xsl:attribute>
                    <xsl:attribute name="type">text/css</xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$olcssPath"></xsl:value-of>
                    </xsl:attribute>
                </xsl:element>


                <script type="text/javascript">
                    var context = '<xsl:value-of select="$tdsContext"></xsl:value-of>';
                    var gridWKT = '<xsl:value-of select="$gridWKT"></xsl:value-of>';

                    var Ncss ={};

                    Ncss.debug = true;

                    Ncss.log = function(message) {
                        if (Ncss.debug) {
                            console.log(message);
                        }
                    };

                    //Dynamic load of the javascript files
                    (function(){
                        //jQuery
                        var headTag = document.getElementsByTagName("head")[0];
                        var jQueryfile = document.createElement('script');
                        jQueryfile.setAttribute("type", "text/javascript");
                        jQueryfile.setAttribute("src", context+"/js/lib/jquery-3.2.1.slim.js");
                        headTag.appendChild(jQueryfile);

                        //OpenLayers.js
                        var olfile = document.createElement('script');
                        olfile.setAttribute("type", "text/javascript");
                        olfile.setAttribute("src",
                        context+"/js/lib/OpenLayers-2.13.1/OpenLayers.js");
                        headTag.appendChild(olfile);

                        //ncssApp.js
                        var jsfile = document.createElement('script');
                        jsfile.setAttribute("type", "text/javascript");
                        jsfile.setAttribute("src", context+"/js/ncss/ncssApp.js");
                        var headTag = document.getElementsByTagName("head")[0];
                        headTag.appendChild(jsfile);

                        //gridAsPointForm.js
                        var jsfile = document.createElement('script');
                        jsfile.setAttribute("type", "text/javascript");
                        jsfile.setAttribute("src", context+"/js/ncss/gridAsPointForm.js");
                        var headTag = document.getElementsByTagName("head")[0];
                        headTag.appendChild(jsfile);
                    })();
                </script>
            </head>

            <body onload="Ncss.initGridAsPoint()">
                <!-- Header -->
                <div id="header">
                    <div id="dataset">
                        <h1>
                            NCSS Grids As Point Data (
                            <a href="dataset.html">Gridded Dataset</a>
                            )
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
                            <span id="datasetPath" class="black">
                                <xsl:value-of select="gridForm/@location"/>
                            </span>
                            (
                            <a href="dataset.xml">Gridded Dataset Description</a>
                            )
                        </h2>
                        <h3>
                            <span>Base Time:</span>
                            <span class="black">
                                <xsl:value-of select="gridForm/TimeSpan/begin"/>
                            </span>
                        </h3>
                    </div>

                    <form method="GET" action="{gridForm/attribute::location}">
                        <!-- table border="0" cellpadding="4" cellspacing="2" -->
                        <table class="simple">
                            <tr valign="top">
                                <td class="leftCol">
                                    <xsl:if test="count(/gridForm/ensemble) &gt; 0">

                                        <xsl:variable name="ensVals">
                                            <xsl:value-of select="/gridForm/ensemble/values"/>
                                        </xsl:variable>
                                        <xsl:variable name="tokenizedEnsVals" select="tokenize($ensVals,' ')"/>
                                        <h1>Ensemble members (<xsl:value-of select="count($tokenizedEnsVals)"/>):
                                        </h1>
                                        <xsl:for-each select="$tokenizedEnsVals">
                                            <xsl:variable name="innerCounter" select="position()"/>
                                            <xsl:value-of select="."/>
                                            <xsl:if test="$innerCounter!=count($tokenizedEnsVals)">
                                                <xsl:text>,</xsl:text>
                                            </xsl:if>

                                        </xsl:for-each>
                                        <br/>
                                        <span>(Subsetting on ensemble dimensions is not yet supported and all members
                                            will be returned)
                                        </span>
                                        <br/>
                                    </xsl:if>
                                    <br/>
                                    You must select at least one Variable and a Lat/Lon location.
                                    <h3>Select Variable(s):</h3>

                                    <xsl:for-each select="gridForm/timeSet">

                                        <xsl:if test="time">
                                            <xsl:if test="time/values/@npts &lt; 100">
                                                <strong>Variables with available Times:</strong>
                                                <xsl:value-of select="time/values"/>
                                                <xsl:for-each select="time/attribute[@name='units']">
                                                    <em>
                                                        <xsl:value-of select="@value"/>
                                                    </em>
                                                </xsl:for-each>
                                            </xsl:if>
                                            <xsl:if test="time/values/@npts &gt; 99">
                                                <strong>
                                                    Variables with Time coordinate
                                                    <xsl:value-of select="time/@name"/>
                                                </strong>
                                            </xsl:if>
                                        </xsl:if>

                                        <blockquote>
                                            <xsl:for-each select="vertSet">
                                                <xsl:if test="vert">
                                                    <strong>
                                                        with Vertical Levels (
                                                        <xsl:value-of select="vert/@name"/>
                                                        ) :
                                                    </strong>

                                                    <xsl:value-of select="vert/values"/>
                                                    <xsl:for-each select="vert/attribute[@name='units']">
                                                        <em>
                                                            <xsl:value-of select="@value"/>
                                                        </em>
                                                    </xsl:for-each>
                                                </xsl:if>
                                                <br/>
                                                <xsl:for-each select="grid">
                                                    <input onchange="Ncss.buildAccessUrl()" type="checkbox" name="var"
                                                           value="{@name}"/>
                                                    <xsl:value-of select="@name"/>
                                                    =
                                                    <xsl:value-of select="@desc"/>
                                                    <br/>
                                                </xsl:for-each>
                                                <br/>

                                            </xsl:for-each>
                                        </blockquote>
                                        <br/>


                                    </xsl:for-each>

                                </td>

                                <td class="rightCol">
                                    <h3>Choose Lat/Lon Location:</h3>

                                    <div id="gridPreviewFrame">
                                        <div id="gridPreview">
                                            <!-- grid preview... -->
                                        </div>
                                    </div>
                                    <br clear="all"/>

                                    <div class="borderLightGrey">
                                        <label class="sized_big">Latitude:</label>
                                        <input onchange="Ncss.buildAccessUrl()" type="text" name="latitude" size="10"/>
                                        <br/>
                                        <label class="sized_big">Longitude:</label>
                                        <input onchange="Ncss.buildAccessUrl()" type="text" name="longitude" size="10"/>
                                        <br/>
                                        <span class="bold">Bounding box, in decimal degrees
                                            (initial extents are approximate):</span>
                                        <div class="top">
                                            <span>north</span>
                                            <br/>
                                            <input onchange="Ncss.buildAccessUrl()" disabled="disabled" type="text"
                                                   name="north" size="8" value="{gridForm/LatLonBox/north}"/>
                                        </div>
                                        <div>
                                            west
                                            <input onchange="Ncss.buildAccessUrl()" disabled="disabled" type="text"
                                                   name="west" size="8" value="{gridForm/LatLonBox/west}"/>
                                            <input onchange="Ncss.buildAccessUrl()" disabled="disabled" type="text"
                                                   name="east" size="8" value="{gridForm/LatLonBox/east}"/>
                                            east
                                        </div>
                                        <div class="top">
                                            <input onchange="Ncss.buildAccessUrl()" disabled="disabled" type="text"
                                                   name="south" size="8" value="{gridForm/LatLonBox/south}"/>
                                            <br/>
                                            <span>south</span>
                                        </div>
                                    </div>

                                    <xsl:if test="$hasTimeAxis>0">
                                        <h3>Choose Time Subset:</h3>
                                        <div id="inputTimeRange" class="selected">
                                            <span class="bold">Time range</span>
                                        </div>
                                        <div id="inputSingleTime" class="unselected">
                                            <span class="bold">Single time</span>
                                        </div>

                                        <div id="timeInput" class="clear">
                                            <div id="temporalSubset">
                                                <!-- Time range -->
                                                <div id="timeRangeSubset" class="absoluteTopLeft borderLightGrey">
                                                    <label class="sized">Start:</label>
                                                    <input onchange="Ncss.buildAccessUrl()" type="text"
                                                           name="time_start" size="21"
                                                           value="{gridForm/TimeSpan/begin}"/>
                                                    <input onchange="Ncss.buildAccessUrl()" type="hidden"
                                                           disabled="disabled" name="dis_time_start" size="21"
                                                           value="{gridForm/TimeSpan/begin}"/>
                                                    <br/>
                                                    <label class="sized">End:</label>
                                                    <input onchange="Ncss.buildAccessUrl()" type="text" name="time_end"
                                                           size="21" value="{gridForm/TimeSpan/end}"/>
                                                    <input onchange="Ncss.buildAccessUrl()" type="hidden"
                                                           disabled="disabled" name="dis_time_end" size="21"
                                                           value="{gridForm/TimeSpan/end}"/>
                                                    <!-- label class="sized">Stride:</label>
                                                    <input type="text" name="timeStride" size="5" value="1" /-->
                                                    <br/>
                                                    <span class="blueLink" id="resetTimeRange">reset to full extension
                                                    </span>
                                                </div>
                                                <div id="singleTimeSubset"
                                                     class="hidden absoluteTopLeft borderLightGrey">
                                                    <label class="sized">Time:</label>
                                                    <input onchange="Ncss.buildAccessUrl()" type="text" name="time"
                                                           size="21" disabled="disabled"
                                                           value="{gridForm/TimeSpan/end}"/>
                                                </div>
                                            </div>
                                        </div>
                                    </xsl:if>

                                    <xsl:if test="$hasVertAxis>0">
                                        <h3>Choose Vertical Level:</h3>
                                        <div class="borderLightGrey">
                                            <label class="sized">Level:</label>
                                            <input onchange="Ncss.buildAccessUrl()" type="text" name="vertCoord"
                                                   size="10"/>
                                            <br/>
                                        </div>
                                    </xsl:if>

                                    <h3>Choose Output Format:</h3>
                                    <div class="borderLightGrey">
                                        <label class="sized">Format:</label>
                                        <select onchange="Ncss.buildAccessUrl()" name="accept" size="1">
                                            <xsl:for-each select="gridForm/AcceptList/GridAsPoint/accept">
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
                                    <h3>NCSS Request URL:</h3>
                                    <pre id="urlBuilder"/>
                                </td>
                            </tr>
                            <tr>
                                <td colspan="2" class="center">
                                    <input class="button" type="submit" value="Submit"/>
                                    <input class="button" type="button" onclick="Ncss.resetForm()" value="Reset"/>
                                </td>
                            </tr>
                        </table>
                    </form>
                </div>
                <hr/>
                <h3>
                    <a href="http://www.unidata.ucar.edu/software/thredds/current/tds/reference/NetcdfSubsetServiceReference.html">
                        NetCDF Subset Service Documentation
                    </a>
                </h3>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
