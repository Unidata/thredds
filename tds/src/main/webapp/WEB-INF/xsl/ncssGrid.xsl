<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html" encoding="utf-8" indent="yes" />

	<!-- Gets the tds context as a xslt parameter -->
	<xsl:param name="tdsContext"></xsl:param>

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
	<xsl:variable name="logoPath">
		<xsl:value-of select="concat($tdsContext,'/unidataLogo.gif')"></xsl:value-of>
	</xsl:variable>

	<xsl:variable name="jQueryPath">
		<xsl:value-of select="concat($tdsContext,'/js/lib/jquery-1.7.2.min.js')"></xsl:value-of>
	</xsl:variable>
	
	<xsl:template match="/">
	    	    
		<html>
			<head>
				<title>NetCDF Subset Service for Grids</title>
				
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
				
			<script type="text/javascript">
		
			var context = '<xsl:value-of select="$tdsContext"></xsl:value-of>';
			var Ncss ={};

			Ncss.debug = true;

			Ncss.log = function(message){
				if(Ncss.debug){
					console.log(message);
				}
			};			
			
			//Dynamic load of the javascript files	
			(function(){
													
				//jQuery
				var headTag = document.getElementsByTagName("head")[0];
				var jQueryfile = document.createElement('script');
				jQueryfile.setAttribute("type", "text/javascript");
				jQueryfile.setAttribute("src", context+"/js/lib/jquery-1.7.2.min.js");				
				headTag.appendChild(jQueryfile);
				
				//ncssApp.js								
				var ncssAppfile = document.createElement('script');
				ncssAppfile.setAttribute("type", "text/javascript");
				ncssAppfile.setAttribute("src", context+"/js/ncss/ncssApp.js");
				var headTag = document.getElementsByTagName("head")[0];
				headTag.appendChild(ncssAppfile);											
				
				//form.js								
				var jsfile = document.createElement('script');
				jsfile.setAttribute("type", "text/javascript");			
				jsfile.setAttribute("src", context+"/js/ncss/gridDatasetForm.js");
				var headTag = document.getElementsByTagName("head")[0];
				headTag.appendChild(jsfile);				
			})();
 
			</script>				
																																																							
			</head>
			
			<body onload="Ncss.initGridDatasetForm()">				
				<!-- Header -->
				<div id="header">
					<div id="dataset">
						<h1>
							NCSS for Grids (<a href="pointDataset.html">Grid as Point Dataset</a>)
						</h1>
					</div>
					<div id="unidata">
						<div id="title">
							<div id="service">
								<span class="bold">THREDDS data server </span>
								<span class="service"> NetCDF Subset Service</span>
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
						<h2><span >Dataset: </span><span class="black"><xsl:value-of select="gridForm/@location" /> </span>(<a href="dataset.xml">Gridded Dataset Description</a>)</h2>
						<h3><span >Base Time:</span><span class="black"><xsl:value-of select="gridForm/TimeSpan/begin" /></span></h3>
					</div>
					<form method="GET" action="{gridForm/attribute::location}">
						<!-- table border="0" cellpadding="4" cellspacing="2" -->
						<table class="simple">
							<tr valign="top">
								<td class="leftCol">

									<h3><span>Select Variable(s):</span></h3>

									<xsl:for-each select="gridForm/timeSet">

										<xsl:if test="time">
											<xsl:if test="time/values/@npts &lt; 100">
												<span class="black bold">Variables with available Times:</span>
												<xsl:value-of select="time/values" />
												<xsl:for-each select="time/attribute[@name='units']">
													<em>
														<xsl:value-of select="@value" />
													</em>
												</xsl:for-each>
											</xsl:if>
											<xsl:if test="time/values/@npts &gt; 99">
												<span class="black bold">
													Variables with Time coordinate
													<xsl:value-of select="time/@name" />
												</span>
											</xsl:if>
										</xsl:if>

										<blockquote>
											<xsl:for-each select="vertSet">
												<xsl:if test="vert">
													<strong>
														with Vertical Levels (
														<xsl:value-of select="vert/@name" />
														) :
													</strong>
													<xsl:value-of select="vert/values" />
													<xsl:for-each select="vert/attribute[@name='units']">
														<em>
															<xsl:value-of select="@value" />
														</em>
													</xsl:for-each>
												</xsl:if>
												<br />
												<xsl:for-each select="grid">
													<input type="checkbox" name="var" value="{@name}" />
													<xsl:value-of select="@name" />
													=
													<xsl:value-of select="@desc" />
													<br />
												</xsl:for-each>
												<br />

											</xsl:for-each>
										</blockquote>
										<br />

									</xsl:for-each>

								</td>

								<td class="rightCol">
									<h3>Choose Spatial Subset:</h3>
									<div id="inputLatLonSubset" class="selected"  ><span class="bold">Lat/lon subset</span></div>
									<div id="inputCoordSubset" class="unselected"><span  class="bold">Coordinate subset</span></div>
										
									<div id="areaInput" class="clear">
										<div id="spatialSubset">

											<!-- lat/lon subsetting -->
											<div id="latlonSubset" class="absoluteTopLeft borderLightGrey">
												<!-- input type="radio" name="latlonSubset" value="bb" checked="checked" /-->
											    <span class="bold">Bounding Box (decimal degrees):  </span>
												<div class="top">
													<span>north</span><br />
													<input type="text" name="north" size="8" value="{gridForm/LatLonBox/north}" />												
												</div>
												<div>
													west <input type="text" name="west" size="8" value="{gridForm/LatLonBox/west}" /> 
 										    		<input type="text" name="east" size="8" value="{gridForm/LatLonBox/east}" /> east 											
												</div>
												<div class="top">
													<input type="text" name="south" size="8" value="{gridForm/LatLonBox/south}" /><br />
													<span>south</span>																							
												</div>												
												<span class="blueLink" id="resetLatLonbbox">reset to full extension</span>
											</div>
													
											<!-- coordinate subsetting -->										
											<div id="coordinateSubset" class="hidden absoluteTopLeft borderLightGrey">
												<!-- input type="radio" name="coordinateSubset" value="bb" checked="checked" / -->
											    <span class="bold">Bounding Box (projection coordinates):</span>											
												<div class="top">
													<span>maxy</span><br />
													<input type="text" disabled="disabled" name="maxy" size="8" value="{gridForm/projectionBox/maxy}" />												
												</div>
												<div>
													minx <input type="text" disabled="disabled" name="minx" size="8" value="{gridForm/projectionBox/minx}" /> 
 										    		<input type="text" disabled="disabled" name="maxx" size="8" value="{gridForm/projectionBox/maxx}" /> maxx 											
												</div>
												<div class="top">
													<input type="text" disabled="disabled" name="miny" size="8" value="{gridForm/projectionBox/miny}" /><br />
													<span>miny</span>																							
												</div>
												<span class="blueLink" id="resetProjbbox">reset to full extension</span>
											</div>																				
										</div>									
									</div>
																		
									<br clear="all" />
									<div class="borderLightGrey">
										<span class="bold">Horizontal Stride:</span>
										<input type="text" name="horizStride" size="5" value="1" />
									</div>
									
									<h3>Choose Time Subset:</h3>
									<div id="inputTimeRange" class="selected"  ><span class="bold">Time range</span></div>
									<div id="inputSingleTime" class="unselected"><span  class="bold">Single time</span></div>
									
									<div id="timeInput" class="clear">
										<div id="temporalSubsetWithStride" >
											<!-- Time range -->
											<div id="timeRangeSubset" class="absoluteTopLeft borderLightGrey">											 
											 <label class="sized">Starting:</label><input type="text" name="time_start" size="21"	value="{gridForm/TimeSpan/begin}" />
											 <label class="sized">Ending:  </label><input type="text" name="time_end" size="21"	value="{gridForm/TimeSpan/end}" />
											 <label class="sized">Stride:</label><input type="text" name="timeStride" size="5" value="1" /><br/>
											 <span class="blueLink" id="resetTimeRange">reset to full extension</span>
											</div>
											<div id="singleTimeSubset" class="hidden absoluteTopLeft borderLightGrey">
												<label class="sized">Time:</label><input type="text" name="time" size="21" value="{gridForm/TimeSpan/end}" />
											</div>	
										</div>
									</div>																			

									<h3>Choose Vertical Level:</h3>									
									<div id="inputSingleLevel" class="selected"  ><span class="bold">Single Level</span></div>
									<div id="inputVerticalStride" class="unselected"><span  class="bold">Vertical Stride</span></div>									
									
									<div id="verticalLevelInput" class="clear">
										<div id="verticalSubset">
											<div id="singleLevel" class="absoluteTopLeft borderLightGrey">
												<label class="sized">Level:</label><input type="text" name="vertCoord" size="10" />
											</div>
											<div id="strideLevels" class="absoluteTopLeft hidden borderLightGrey">
												<label class="sized">Stride:</label><input type="text" name="vertStride" disabled="disabled" value="1" size="10" />
											</div>					
										</div>				
									</div>																										
									<div class="borderLightGrey">
										<span class="bold">Add 2D Lat/Lon to file (if needed for CF compliance)</span>
										<br />
										<input type="checkbox" name="addLatLon" value="true" /><label>Add Lat/Lon variables</label>
									<br />
									</div>
								</td>
							</tr>
							<tr>
								<td colspan="2" class="center"><input class="button" type="submit" value="Submit" /><input class="button" type="reset" value="Reset" /></td>
							</tr>
						</table>
					</form>
					<hr />
				</div>
				<!-- Footer -->
				<h3>
					<a
						href="http://www.unidata.ucar.edu/projects/THREDDS/tech/interfaceSpec/NetcdfSubsetService_4_3.html">NetCDF Subset Service Documentation</a>
				</h3>
									
			</body>															
		</html>

	</xsl:template>
</xsl:stylesheet>
