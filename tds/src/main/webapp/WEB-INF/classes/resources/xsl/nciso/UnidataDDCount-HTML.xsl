<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:nc="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" xmlns:xs="http://www.w3.org/2001/XMLSchema">
	<xd:doc xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" scope="stylesheet">
		<xd:desc>
			<xd:p><xd:b>Created on:</xd:b> April 14, 2011</xd:p>
			<xd:p><xd:b>Author:</xd:b>ted.habermann@noaa.gov</xd:p>
			<xd:p><xd:b>Modified on:</xd:b> May 26, 2011</xd:p>
			<xd:p><xd:b>Author:</xd:b>david.neufeld@noaa.gov</xd:p>
			<xd:p><xd:b>Modified on:</xd:b> October 6, 2011</xd:p>
			<xd:p><xd:b>Author:</xd:b>ted.habermann@noaa.gov</xd:p>
			<xd:p/>
		</xd:desc>
	</xd:doc>
	<xsl:variable name="rubricVersion" select="'2.0'"/>
	<xsl:output method="xml" indent="yes"/>
	<xsl:template name="showScore">
		<xsl:param name="score"/>
		<xsl:choose>
			<xsl:when test="xs:integer($score)=0">
				<td align="center" bgcolor="FF0033">
					<xsl:value-of select="$score"/>
				</td>
			</xsl:when>
			<xsl:otherwise>
				<td align="center" bgcolor="66CC66">
					<xsl:value-of select="$score"/>
				</td>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="showColumn">
		<xsl:param name="name"/>
		<xsl:param name="total"/>
		<xsl:param name="max"/>
		<xsl:variable name="column">
			<xsl:choose>
				<xsl:when test="$total=0">0</xsl:when>
				<xsl:when test="$total=$max">4</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="floor(number(number($total) * 3 div number($max)))+1"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<tr>
			<td width="20%">
				<a href="#{$name}">
					<xsl:value-of select="$name"/>
				</a>
			</td>
			<xsl:choose>
				<xsl:when test="$column=0">
					<td align="center" bgcolor="CC00CC">X</td>
				</xsl:when>
				<xsl:otherwise>
					<td/>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:choose>
				<xsl:when test="$column=1">
					<td align="center" bgcolor="CC00CC">X</td>
				</xsl:when>
				<xsl:otherwise>
					<td/>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:choose>
				<xsl:when test="$column=2">
					<td align="center" bgcolor="CC00CC">X</td>
				</xsl:when>
				<xsl:otherwise>
					<td/>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:choose>
				<xsl:when test="$column=3">
					<td align="center" bgcolor="CC00CC">X</td>
				</xsl:when>
				<xsl:otherwise>
					<td/>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:choose>
				<xsl:when test="$column=4">
					<td align="center" bgcolor="CC00CC">X</td>
				</xsl:when>
				<xsl:otherwise>
					<td/>
				</xsl:otherwise>
			</xsl:choose>
		</tr>
	</xsl:template>
	<xsl:template name="showStars">
		<xsl:param name="name"/>
		<xsl:param name="total"/>
		<xsl:param name="max"/>
		<xsl:variable name="column">
			<xsl:choose>
				<xsl:when test="$total=0">0</xsl:when>
				<xsl:when test="$total=$max">4</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="floor(number(number($total) * 3 div number($max)))+1"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<a href="#{$name}">
			<xsl:value-of select="$name"/>
		</a>
		<xsl:choose>
			<xsl:when test="$column=0">
				<span class="sprite star_0_0"/>
			</xsl:when>
			<xsl:when test="$column=1">
				<span class="sprite star_1_0"/>
			</xsl:when>
			<xsl:when test="$column=2">
				<span class="sprite star_2_0"/>
			</xsl:when>
			<xsl:when test="$column=3">
				<span class="sprite star_3_0"/>
			</xsl:when>
			<xsl:otherwise>
				<span class="sprite star_4_0"/>
			</xsl:otherwise>
		</xsl:choose>
		<br/>
	</xsl:template>
	<xsl:template match="/">
		<xsl:variable name="globalAttributeCnt" select="count(/nc:netcdf/nc:attribute)"/>
		<xsl:variable name="variableCnt" select="count(/nc:netcdf/nc:variable)"/>
		<xsl:variable name="variableAttributeCnt" select="count(/nc:netcdf/nc:variable/nc:attribute)"/>
		<xsl:variable name="standardNameCnt" select="count(/nc:netcdf/nc:variable/nc:attribute[@name='standard_name'])"/>
		<!-- dimension variables -->
		<xsl:variable name="longitudeVariable" select="//nc:variable[nc:attribute[@name='units' and @value='degrees_east']]"/>
		<xsl:variable name="latitudeVariable" select="//nc:variable[nc:attribute[@name='units' and @value='degrees_north']]"/>
		<xsl:variable name="verticalVariable" select="//nc:variable[nc:attribute[@name='positive' and (@value='up' or @value='down')]]"/>
		<xsl:variable name="timeVariable" select="//nc:variable[nc:attribute[@name='standard_name' and lower-case(@value)='time']]"/>
		<!-- Identifier Fields: 4 possible -->
		<xsl:variable name="id" as="xs:string*" select="(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='id']/@value,
			/nc:netcdf/nc:attribute[@name='id']/@value)"/>
		<xsl:variable name="idCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$id"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="identifierNameSpace" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='naming_authority']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='authority']/@value)"/>
		<xsl:variable name="identifierNameSpaceCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$identifierNameSpace"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="metadataConvention" as="xs:string*" select="/nc:netcdf/nc:attribute[@name='Metadata_Conventions']/@value"/>
		<xsl:variable name="metadataConventionCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$metadataConvention"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="metadataLink" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='Metadata_Link']/@value,/nc:netcdf/nc:attribute[@name='metadata_link']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@name='xlink']/@value)"/>
		<xsl:variable name="metadataLinkCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$metadataLink"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="identifierTotal" select="$idCnt + $identifierNameSpaceCnt + $metadataConventionCnt + $metadataLinkCnt"/>
		<xsl:variable name="identifierMax">4</xsl:variable>
		<!-- Service Fields: 4 possible (this count is not included in the rubric because it only exists in THREDDS -->
		<xsl:variable name="thredds_netcdfsubsetCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='nccs_service'])"/>
		<xsl:variable name="thredds_opendapCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='opendap_service'])"/>
		<xsl:variable name="thredds_wcsCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wcs_service'])"/>
		<xsl:variable name="thredds_wmsCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wms_service'])"/>
		<xsl:variable name="thredds_sosCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='sos_service'])"/>
		<xsl:variable name="serviceTotal" select="$thredds_netcdfsubsetCnt + $thredds_opendapCnt + $thredds_wcsCnt + $thredds_wmsCnt + $thredds_sosCnt"/>
		<xsl:variable name="serviceMax">5</xsl:variable>
		<!-- Text Search Fields: 7 possible -->
		<xsl:variable name="title" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='title']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='full_name']/@value)"/>
		<xsl:variable name="titleCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$title"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="summary" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='summary']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='summary']/@value)"/>
		<xsl:variable name="summaryCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$summary"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="keywords" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='keywords']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='keywords']/nc:attribute[@name='keyword']/@value)"/>
		<xsl:variable name="keywordsCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$keywords"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="keywordsVocabulary" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='keywords_vocabulary']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='keywords']/nc:attribute[@name='vocab']/@value)"/>
		<xsl:variable name="keywordsVocabularyCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$keywordsVocabulary"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="stdNameVocabulary" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='standard_name_vocabulary']/@value)"/>
		<xsl:variable name="stdNameVocabularyCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$stdNameVocabulary"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="comment" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='comment']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='comment']/@value)"/>
		<xsl:variable name="commentCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$comment"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="history" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='history']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='history']/@value)"/>
		<xsl:variable name="historyCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$history"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="textSearchTotal" select="$titleCnt + $summaryCnt + $keywordsCnt + $keywordsVocabularyCnt + $stdNameVocabularyCnt + $commentCnt + $historyCnt"/>
		<xsl:variable name="textSearchMax">7</xsl:variable>
		<!-- Extent Search Fields: 8 possible -->
		<xsl:variable name="geospatial_lat_min" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_min']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lat_min']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lat_min']/@value)"/>
		<xsl:variable name="geospatial_lat_minCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lat_min"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="geospatial_lat_max" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_max']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lat_max']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lat_max']/@value)"/>
		<xsl:variable name="geospatial_lat_maxCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lat_max"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="geospatial_lon_min" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lon_min']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lon_min']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lon_min']/@value)"/>
		<xsl:variable name="geospatial_lon_minCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lon_min"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="geospatial_lon_max" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lon_max']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lon_max']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lon_max']/@value)"/>
		<xsl:variable name="geospatial_lon_maxCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lon_max"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="time_coverage_start" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_start']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='time_coverage_start']/@value,
			/nc:netcdf/nc:attribute[@name='time_coverage_start']/@value)"/>
		<xsl:variable name="time_coverage_startCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$time_coverage_start"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="time_coverage_end" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_end']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='time_coverage_end']/@value,
			/nc:netcdf/nc:attribute[@name='time_coverage_end']/@value)"/>
		<xsl:variable name="time_coverage_endCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$time_coverage_end"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="verticalMin" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_min']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_vertical_min']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_vertical_min']/@value)"/>
		<xsl:variable name="vertical_minCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$verticalMin"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="verticalMax" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_max']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_vertical_max']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_vertical_max']/@value)"/>
		<xsl:variable name="vertical_maxCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$verticalMax"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="extentTotal" select="$geospatial_lat_minCnt + $geospatial_lat_maxCnt + $geospatial_lon_minCnt + $geospatial_lon_maxCnt + $time_coverage_startCnt + $time_coverage_endCnt + $vertical_minCnt + $vertical_maxCnt"/>
		<xsl:variable name="extentMax">8</xsl:variable>
		<!-- Other Extent Information: 10 possible -->
		<xsl:variable name="geospatial_lat_units" as="xs:string*"
			select="(//nc:variable[@name=$latitudeVariable[1]]/nc:attribute[@name='units']/@value,
			/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_units']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lat_units']/@value)"/>
		<xsl:variable name="geospatial_lat_unitsCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lat_units"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="geospatial_lon_units" as="xs:string*"
			select="(//nc:variable[@name=$longitudeVariable[1]]/nc:attribute[@name='units']/@value,
			/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_units']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lon_units']/@value)"/>
		<xsl:variable name="geospatial_lon_unitsCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lon_units"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="temporalUnits" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_units']/@value,
			//nc:variable[@name=$timeVariable[1]]/nc:attribute[@name='units']/@value,
			/nc:netcdf/nc:attribute[@name='time_coverage_units']/@value)"/>
		<xsl:variable name="temporalUnitsCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$temporalUnits"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="verticalUnits" as="xs:string*"
			select="(//nc:variable[@name=$verticalVariable[1]]/nc:attribute[@name='units']/@value,
			/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_units']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_vertical_units']/@value)"/>
		<xsl:variable name="verticalUnitsCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$verticalUnits"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="geospatial_lat_resolution" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_resolution']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lat_resolution']/@value)"/>
		<xsl:variable name="geospatial_lat_resolutionCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lat_resolution"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="geospatial_lon_resolution" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lon_resolution']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_lon_resolution']/@value)"/>
		<xsl:variable name="geospatial_lon_resolutionCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$geospatial_lon_resolution"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="timeResolution" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_resolution']/@value,
			/nc:netcdf/nc:attribute[@name='time_coverage_resolution']/@value)"/>
		<xsl:variable name="timeResolutionCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$timeResolution"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="timeDuration" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_duration']/@value,
			/nc:netcdf/nc:attribute[@name='time_coverage_duration']/@value)"/>
		<xsl:variable name="timeDurationCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$timeDuration"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="verticalResolution" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_resolution']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_vertical_resolution']/@value)"/>
		<xsl:variable name="verticalResolutionCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$verticalResolution"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="verticalPositive" as="xs:string*"
			select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_positive']/@value,
			/nc:netcdf/nc:attribute[@name='geospatial_vertical_positive']/@value)"/>
		<xsl:variable name="verticalPositiveCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$verticalPositive"/>
			</xsl:call-template>
		</xsl:variable>
		<!--  Extent Totals -->
		<xsl:variable name="otherExtentTotal"
			select="$geospatial_lat_resolutionCnt + $geospatial_lat_unitsCnt + $geospatial_lon_resolutionCnt + $geospatial_lon_unitsCnt + $timeResolutionCnt + $timeDurationCnt + $temporalUnitsCnt + $verticalUnitsCnt + $verticalResolutionCnt + $verticalPositiveCnt"/>
		<xsl:variable name="otherExtentMax">10</xsl:variable>
		<!-- Responsible Party Fields: 14 possible -->
		<xsl:variable name="creatorName" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='creator_name']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='creators']/nc:group[@name='creator']/nc:attribute[@name='name']/@value)"/>
		<xsl:variable name="creatorNameCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$creatorName"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="creatorURL" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='creator_url']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='creators']/nc:group[@name='creator']/nc:attribute[@name='url']/@value)"/>
		<xsl:variable name="creatorURLCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$creatorURL"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="creatorEmail" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='creator_email']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='creators']/nc:group[@name='creator']/nc:attribute[@name='email']/@value)"/>
		<xsl:variable name="creatorEmailCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$creatorEmail"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="creatorDate" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='date_created']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='dates']/nc:attribute[@type='created']/@value)"/>
		<xsl:variable name="creatorDateCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$creatorDate"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="modifiedDate" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='date_modified']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='dates']/nc:attribute[@type='modified']/@value)"/>
		<xsl:variable name="modifiedDateCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$modifiedDate"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="issuedDate" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='date_issued']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='dates']/nc:attribute[@type='issued']/@value)"/>
		<xsl:variable name="issuedDateCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$issuedDate"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="institution" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='institution']/@value)"/>
		<xsl:variable name="institutionCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$institution"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="project" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='project']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='projects']/@value)"/>
		<xsl:variable name="projectCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$project"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="acknowledgment" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='acknowledgment']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='funding']/@value)"/>
		<xsl:variable name="acknowledgmentCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$acknowledgment"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="creatorTotal" select="$creatorNameCnt + $creatorURLCnt + $creatorEmailCnt + $creatorDateCnt + $modifiedDateCnt + $issuedDateCnt + $institutionCnt + $projectCnt + $acknowledgmentCnt"/>
		<xsl:variable name="creatorMax">9</xsl:variable>
		<!--  -->
		<xsl:variable name="contributorName" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='contributor_name']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='contributors']/nc:group[@name='contributor']/nc:attribute[@name='name']/@value)"/>
		<xsl:variable name="contributorNameCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$contributorName"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="contributorRole" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='contributor_role']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='contributors']/nc:group[@name='contributor']/nc:attribute[@name='role']/@value)"/>
		<xsl:variable name="contributorRoleCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$contributorRole"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="contributorTotal" select="$contributorNameCnt + $contributorRoleCnt"/>
		<xsl:variable name="contributorMax">2</xsl:variable>
		<!--  -->
		<xsl:variable name="publisherName" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='publisher_name']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='publishers']/nc:group[@name='publisher']/nc:attribute[@name='name']/@value)"/>
		<xsl:variable name="publisherNameCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$publisherName"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="publisherURL" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='publisher_url']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='publishers']/nc:group[@name='publisher']/nc:attribute[@name='url']/@value)"/>
		<xsl:variable name="publisherURLCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$publisherURL"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="publisherEmail" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='publisher_email']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='publishers']/nc:group[@name='publisher']/nc:attribute[@name='email']/@value)"/>
		<xsl:variable name="publisherEmailCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$publisherEmail"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="publisherTotal" select="$publisherNameCnt + $publisherURLCnt + $publisherEmailCnt"/>
		<xsl:variable name="publisherMax">3</xsl:variable>
		<!--  -->
		<xsl:variable name="responsiblePartyTotal" select="$creatorTotal + $contributorTotal + $publisherTotal"/>
		<xsl:variable name="responsiblePartyMax">14</xsl:variable>
		<!-- Other Fields: 2 possible -->
		<xsl:variable name="cdmType" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='cdm_data_type']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='data_type']/@value)"/>
		<xsl:variable name="cdmTypeCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$cdmType"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="processingLevel" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='processing_level']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='processing_level']/@value)"/>
		<xsl:variable name="processingLevelCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$processingLevel"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="license" as="xs:string*"
			select="(/nc:netcdf/nc:attribute[@name='license']/@value,
			/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='rights'])"/>
		<xsl:variable name="licenseCnt">
			<xsl:call-template name="sequenceExists">
				<xsl:with-param name="seq" select="$license"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="otherTotal" select="$cdmTypeCnt + $processingLevelCnt + $licenseCnt"/>
		<xsl:variable name="otherMax">3</xsl:variable>
		<xsl:variable name="spiralTotal" select="$identifierTotal + $textSearchTotal + $extentTotal + $otherExtentTotal + $otherTotal + $responsiblePartyTotal"/>
		<xsl:variable name="spiralMax" select="$identifierMax   + $textSearchMax   + $extentMax   + $otherExtentMax   + $otherMax   + $responsiblePartyMax"/>
		<!-- Define root variable for later access -->
		<xsl:variable name="docRoot" select="/"/>
		<!-- Display Results Fields -->
		<html>
			<style type="text/css">
				table {
				    empty-cells : show;
				}</style>
			<h1>NetCDF Attribute Convention for Dataset Discovery Report</h1>
			<xsl:variable name="titleAttribute" select="/nc:netcdf/nc:attribute[@name='title']"/> The Unidata Attribute Convention for Data Discovery provides recommendations for netCDF attributes that can be added to netCDF files to
			facilitate discovery of those files using standard metadata searches. This tool tests conformance with those recommendations using this <a href="http://www.ngdc.noaa.gov/metadata/published/xsl/UnidataDDCount-HTML.xsl"
				>stylesheet</a>. More <a href="https://geo-ide.noaa.gov/wiki/index.php?title=NetCDF_Attribute_Convention_for_Dataset_Discovery#Conformance_Test">Information on Convention and Tool</a>. <h2> Title: <xsl:value-of
					select="$titleAttribute/@value"/>
			</h2>
			<h2>Total Score: <xsl:value-of select="$spiralTotal"/>/<xsl:value-of select="$spiralMax"/></h2>
			<h2>General File Characteristics</h2>
			<table>
				<tr>
					<td>Number of Global Attributes</td>
					<td>
						<xsl:value-of select="$globalAttributeCnt"/>
					</td>
				</tr>
				<tr>
					<td>Number of Variables</td>
					<td>
						<xsl:value-of select="$variableCnt"/>
					</td>
				</tr>
				<tr>
					<td>Number of Variable Attributes</td>
					<td>
						<xsl:value-of select="$variableAttributeCnt"/>
					</td>
				</tr>
				<tr>
					<td>Number of Standard Names</td>
					<td>
						<xsl:value-of select="$standardNameCnt"/>
					</td>
				</tr>
				<tr>
					<td>Number of Services</td>
					<td>
						<xsl:value-of select="$serviceTotal"/></td>
				</tr>
				<xsl:if test="$longitudeVariable">
					<tr>
						<td>Longitude Variable(s)</td>
						<td>
							<xsl:for-each select="$longitudeVariable">
								<xsl:variable name="longitudeVariableName" select="./@name"/>
								<xsl:if test="position()!=1">, </xsl:if>
								<xsl:value-of select="$longitudeVariableName"/>(<xsl:for-each select="tokenize(./@shape,' ')">
									<xsl:if test="position()!=1">, </xsl:if>
									<xsl:variable name="dimensionName" select="."/>
									<xsl:value-of select="$dimensionName"/>:<xsl:value-of select="$docRoot/nc:netcdf/nc:dimension[@name=$dimensionName]/@length"/></xsl:for-each>) 
							</xsl:for-each>
						</td>
					</tr>
				</xsl:if>
				<xsl:if test="$latitudeVariable">
					<tr>
						<td>Latitude Variable</td>
						<td><xsl:for-each select="$latitudeVariable">
								<xsl:variable name="latitudeVariableName" select="./@name"/>
								<xsl:if test="position()!=1">, </xsl:if>
								<xsl:value-of select="$latitudeVariableName"/>(<xsl:for-each select="tokenize(./@shape,' ')">
									<xsl:if test="position()!=1">, </xsl:if>
									<xsl:variable name="dimensionName" select="."/>
									<xsl:value-of select="$dimensionName"/>:<xsl:value-of select="$docRoot/nc:netcdf/nc:dimension[@name=$dimensionName]/@length"/></xsl:for-each>) 
							</xsl:for-each></td>
					</tr></xsl:if>
				<xsl:if test="$timeVariable"><tr>
						<td>Time Variable</td>
						<td>
							<xsl:for-each select="$timeVariable">
								<xsl:variable name="timeVariableName" select="./@name"/>
								<xsl:if test="position()!=1">, </xsl:if>
								<xsl:value-of select="$timeVariableName"/>(<xsl:for-each select="tokenize(./@shape,' ')">
									<xsl:if test="position()!=1">, </xsl:if>
									<xsl:variable name="dimensionName" select="."/>
									<xsl:value-of select="$dimensionName"/>:<xsl:value-of select="$docRoot/nc:netcdf/nc:dimension[@name=$dimensionName]/@length"/></xsl:for-each>) 
							</xsl:for-each>
						</td>
					</tr>
				</xsl:if>
				<xsl:if test="$verticalVariable"><tr>
						<td>Vertical Variable</td>
						<td>
							<xsl:for-each select="$verticalVariable">
								<xsl:variable name="verticalVariableName" select="./@name"/>
								<xsl:if test="position()!=1">, </xsl:if>
								<xsl:value-of select="$verticalVariableName"/>(<xsl:for-each select="tokenize(./@shape,' ')">
									<xsl:if test="position()!=1">, </xsl:if>
									<xsl:variable name="dimensionName" select="."/>
									<xsl:value-of select="$dimensionName"/>:<xsl:value-of select="$docRoot/nc:netcdf/nc:dimension[@name=$dimensionName]/@length"/></xsl:for-each>) 
							</xsl:for-each> 
						</td>
					</tr></xsl:if>
			</table>
			<table width="95%" border="1" cellpadding="2" cellspacing="2">
				<tr>
					<th>Spiral</th>
					<th>None</th>
					<th>1-33%</th>
					<th>34-66%</th>
					<th>67-99%</th>
					<th>All</th>
				</tr>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Total'"/>
					<xsl:with-param name="total" select="$spiralTotal"/>
					<xsl:with-param name="max" select="$spiralMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Identification'"/>
					<xsl:with-param name="total" select="$identifierTotal"/>
					<xsl:with-param name="max" select="$identifierMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Text Search'"/>
					<xsl:with-param name="total" select="$textSearchTotal"/>
					<xsl:with-param name="max" select="$textSearchMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Extent Search'"/>
					<xsl:with-param name="total" select="$extentTotal"/>
					<xsl:with-param name="max" select="$extentMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Other Extent Information'"/>
					<xsl:with-param name="total" select="$otherExtentTotal"/>
					<xsl:with-param name="max" select="$otherExtentMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Creator Search'"/>
					<xsl:with-param name="total" select="$creatorTotal"/>
					<xsl:with-param name="max" select="$creatorMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Contributor Search'"/>
					<xsl:with-param name="total" select="$contributorTotal"/>
					<xsl:with-param name="max" select="$contributorMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Publisher Search'"/>
					<xsl:with-param name="total" select="$publisherTotal"/>
					<xsl:with-param name="max" select="$publisherMax"/>
				</xsl:call-template>
				<xsl:call-template name="showColumn">
					<xsl:with-param name="name" select="'Other Attributes'"/>
					<xsl:with-param name="total" select="$otherTotal"/>
					<xsl:with-param name="max" select="$otherMax"/>
				</xsl:call-template>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Identification"/>
			<h2>Identification / Metadata Reference Score: <xsl:value-of select="$identifierTotal"/>/<xsl:value-of select="$identifierMax"/></h2>
			<p>As metadata are shared between national and international repositories it is becoming increasing important to be able to unambiguously identify and refer to specific records. This is facilitated by including an identifier in
				the metadata. Some mechanism must exist for ensuring that these identifiers are unique. This is accomplished by specifying the naming authority or namespace for the identifier. It is the responsibility of the manager of the
				namespace to ensure that the identifiers in that namespace are unique. Identifying the Metadata Convention being used in the file and providing a link to more complete metadata, possibly using a different convention, are
				also important.</p>
			<table width="95%" border="1" cellpadding="2" cellspacing="2">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$idCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#id_Attribute">id</a>
						<br/>
					</td>
					<td rowspan="2" valign="top">The combination of the "naming authority" and the "id" should be a globally unique identifier for the dataset.<br/>
					</td>
					<td valign="top">dataset@id</td>
					<td colspan="1" valign="top">/gmi:MI_Metadata/gmd:fileIdentifier/gco:CharacterString<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$identifierNameSpaceCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#naming_authority_Attribute">naming_authority</a>
						<br/>
					</td>
					<td/>
					<td colspan="1" valign="top">/gmi:MI_Metadata/gmd:fileIdentifier/gco:CharacterString<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$metadataConventionCnt"/>
					</xsl:call-template>
					<td valign="top">Metadata_Conventions</td>
					<td valign="top">This attribute should be set to "Unidata Dataset Discovery v1.0" for NetCDF files that follow this convention.</td>
					<td valign="top"/>
					<td valign="top"/>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$metadataLinkCnt"/>
					</xsl:call-template>
					<td valign="top">Metadata_Link or metadata_link</td>
					<td valign="top">This attribute provides a link to a complete metadata record for this dataset or the collection that contains this dataset. <i>This attribute is not included in Version 1 of the Unidata Attribute
							Convention for Data Discovery. It is recommended here because a complete metadata collection for a dataset will likely contain more information than can be included in granule formats. This attribute contains a
							link to that information.</i></td>
					<td valign="top"/>
					<td valign="top"/>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Text Search"/>
			<h2>Text Search Score: <xsl:value-of select="$textSearchTotal"/>/<xsl:value-of select="$textSearchMax"/></h2>
			<p>Text searches are a very important mechanism for data discovery. This group includes attributes that contain descriptive text that could be the target of these searches. Some of these attributes, for example title and
				summary, might also be displayed in the results of text searches.</p>
			<table width="95%" border="1" cellpadding="2" cellspacing="2">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$titleCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#title_Attribute">title</a>
						<br/>
					</td>
					<td valign="top">A short description of the dataset.<br/></td>
					<td valign="top">dataset@name<br/></td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$summaryCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#summary_Attribute">summary</a>
						<br/>
					</td>
					<td valign="top">A paragraph describing the dataset.<br/>
					</td>
					<td valign="top">metadata/documentation[@type="summary"]<br/>
					</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:abstract/gco:CharacterString<br/>
					</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$keywordsCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#keywords_Attribute">keywords</a>
						<br/>
					</td>
					<td valign="top">A comma separated list of key words and phrases.<br/>
					</td>
					<td valign="top">metadata/keyword<br/>
					</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString<br/>
					</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$keywordsVocabularyCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#keywords_vocabulary_Attribute">keywords_vocabulary</a>
						<br/>
					</td>
					<td valign="top">If you are following a guideline for the words/phrases in your "keywords" attribute, put the name of that guideline here.<br/>
					</td>
					<td valign="top">metadata/keyword@vocabulary</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString <br/>
					</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$stdNameVocabularyCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#standard_name_vocabulary_Attribute">standard_name_vocabulary</a>
						<br/>
					</td>
					<td valign="top">The name of the controlled vocabulary from which variable standard names are taken.<br/>
					</td>
					<td valign="top">metadata/variables@vocabulary</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:thesaurusName/gmd:CI_Citation/gmd:title/gco:CharacterString <br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$historyCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#history_Attribute">history</a>
						<br/>
					</td>
					<td valign="top">Provides an audit trail for modifications to the original data.</td>
					<td valign="top"/>
					<td valign="top">/gmi:MI_Metadata/gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:statement/gco:CharacterString</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$commentCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#comment_Attribute">comment</a>
						<br/>
					</td>
					<td valign="top">Miscellaneous information about the data.</td>
					<td valign="top">metadata/documentation<br/>
					</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:supplementalInformation<br/>
					</td>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Extent Search"/>
			<h2>Extent Search Score: <xsl:value-of select="$extentTotal"/>/<xsl:value-of select="$extentMax"/></h2>
			<p>This basic extent information supports spatial/temporal searches that are increasingly important as the number of map based search interfaces increases. Many of the attributes included in this spiral can be calculated from
				the data if the file is compliant with the <a href="http://cf-pcmdi.llnl.gov/">NetCDF Climate and Forecast (CF) Metadata Convention</a>. </p>
			<table width="95%" border="1" cellpadding="2" cellspacing="2">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lat_minCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lat_min_Attribute">geospatial_lat_min</a>
						<br/>
					</td>
					<td rowspan="13" colspan="1" valign="top">Describes a simple latitude, longitude, vertical and temporal bounding box. For a more detailed geospatial coverage, see the <a
							href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#suggested_geospatial">suggested geospatial attributes</a>.<br/> Further refinement of the geospatial bounding box can
						be provided by using these units and resolution attributes.<br/><br/><i>Many of these extent attributes are calculated using the CF-Conventions.</i>
					</td>
					<td valign="top">metadata/geospatialCoverage/northsouth/start<br/></td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:southBoundLatitude/gco:Decimal<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lat_maxCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lat_max_Attribute">geospatial_lat_max</a>
					</td>
					<td valign="top">metadata/geospatialCoverage/northsouth/size</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:northBoundLatitude/gco:Decimal<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lon_minCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lon_min_Attribute">geospatial_lon_min</a>
					</td>
					<td valign="top">metadata/geospatialCoverage/eastwest/start</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:westBoundLongitude/gco:Decimal<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lon_maxCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lon_max_Attribute">geospatial_lon_max</a>
					</td>
					<td valign="top">metadata/geospatialCoverage/eastwest/size</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox/gmd:eastBoundLongitude/gco:Decimal<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$time_coverage_startCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#time_coverage_start_Attribute">time_coverage_start</a>
					</td>
					<td valign="top">metadata/timeCoverage/start</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$time_coverage_endCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#time_coverage_end_Attribute">time_coverage_end</a>
					</td>
					<td valign="top">metadata/timeCoverage/end</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:endPosition</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$vertical_minCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_vertical_min_Attribute">geospatial_vertical_min</a>
						<br/>
					</td>
					<td valign="top">metadata/geospatialCoverage/updown/start</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:verticalElement/gmd:EX_VerticalExtent/gmd:minimumValue/gco:Real</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$vertical_maxCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_vertical_max_Attribute">geospatial_vertical_max</a>
					</td>
					<td valign="top">metadata/geospatialCoverage/updown/size</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:verticalElement/gmd:EX_VerticalExtent/gmd:maximumValue/gco:Real</td>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Other Extent Information"/>
			<h2>Other Extent Information Score: <xsl:value-of select="$otherExtentTotal"/>/<xsl:value-of select="$otherExtentMax"/></h2>
			<p>This information provides more details on the extent attributes than the basic information included in the Extent Spiral. Many of the attributes included in this spiral can be calculated from the data if the file is compliant
				with the <a href="http://cf-pcmdi.llnl.gov/">NetCDF Climate and Forecast (CF) Metadata Convention</a> .</p>
			<table width="95%" border="1" cellpadding="2" cellspacing="2">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lon_unitsCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lon_units_Attribute">geospatial_lon_units</a>
						<br/>
					</td>
					<td valign="top">metadata/geospatialCoverage/eastwest/units</td>
					<td valign="top">/gmi:MI_Metadata/gmd:spatialRepresentationInfo/gmd:MD_Georectified/gmd:axisDimensionProperties/gmd:MD_Dimension/gmd:resolution/gco:Measure/@uom</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lon_resolutionCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lon_resolution_Attribute">geospatial_lon_resolution</a>
					</td>
					<td valign="top">metadata/geospatialCoverage/eastwest/resolution</td>
					<td valign="top">/gmi:MI_Metadata/gmd:spatialRepresentationInfo/gmd:MD_Georectified/gmd:axisDimensionProperties/gmd:MD_Dimension/gmd:resolution/gco:Measure</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lat_unitsCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lat_units_Attribute">geospatial_lat_units</a>
						<br/>
					</td>
					<td valign="top">metadata/geospatialCoverage/northsouth/units</td>
					<td valign="top">/gmi:MI_Metadata/gmd:spatialRepresentationInfo/gmd:MD_Georectified/gmd:axisDimensionProperties/gmd:MD_Dimension/gmd:resolution/gco:Measure/@uom</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$geospatial_lat_resolutionCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_lat_resolution_Attribute">geospatial_lat_resolution</a>
					</td>
					<td valign="top">metadata/geospatialCoverage/northsouth/resolution</td>
					<td valign="top">/gmi:MI_Metadata/gmd:spatialRepresentationInfo/gmd:MD_Georectified/gmd:axisDimensionProperties/gmd:MD_Dimension/gmd:resolution/gco:Measure</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$verticalUnitsCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_vertical_units_Attribute">geospatial_vertical_units</a>
						<br/>
					</td>
					<td valign="top">metadata/geospatialCoverage/updown/units</td>
					<td valign="top" rowspan="3">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:verticalElement/gmd:EX_VerticalExtent/gmd:verticalCRS</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$verticalResolutionCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_vertical_resolution_Attribute">geospatial_vertical_resolution</a>
						<br/>
					</td>
					<td valign="top">metadata/geospatialCoverage/updown/resolution<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$verticalPositiveCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#geospatial_vertical_positive_Attribute">geospatial_vertical_positive</a>
						<br/>
					</td>
					<td valign="top">metadata/geospatialCoverage@zpositive<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$temporalUnitsCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#time_coverage_duration_Attribute">time_coverage_units</a>
					</td>
					<td valign="top">This attribute is calculated using the CF Conventions</td>
					<td valign="top">/gmi:MI_Metadata/gmd:spatialRepresentationInfo/gmd:MD_GridSpatialRepresentation/gmd:axisDimensionProperties/gmd:MD_Dimension/gmd:resolution/gco:Measure/@uom.</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$timeDurationCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#time_coverage_duration_Attribute">time_coverage_duration</a>
					</td>
					<td valign="top">metadata/timeCoverage/duration</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:temporalElement/gmd:EX_TemporalExtent/gmd:extent/gml:TimePeriod/gml:beginPosition provides an ISO8601
						compliant description of the time period covered by the dataset. This standard supports descriptions of <a href="http://en.wikipedia.org/wiki/ISO_8601#Durations">durations</a>.</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$timeResolutionCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#time_coverage_resolution_Attribute">time_coverage_resolution</a>
					</td>
					<td valign="top">metadata/timeCoverage/resolution</td>
					<td/>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Creator Search"/>
			<h2>Creator Search Score: <xsl:value-of select="$creatorTotal"/>/<xsl:value-of select="$creatorMax"/></h2>
			<p>This group includes attributes that could support searches for people/institutions/projects that are responsible for datasets. This information is also critical for the correct attribution of the people and institutions that
				produce datasets.</p>
			<table width="95%" border="1" cellpadding="2" cellspacing="2">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$creatorNameCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#creator_name_Attribute">creator_name</a>
						<br/>
					</td>
					<td rowspan="4" colspan="1" valign="top">The data creator's name, URL, and email. The "institution" attribute will be used if the "creator_name" attribute does not exist.<br/></td>
					<td valign="top">metadata/creator/name<br/></td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString<br/>
						CI_RoleCode="originator"</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$creatorURLCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#creator_url_Attribute">creator_url</a>
						<br/>
					</td>
					<td valign="top">metadata/creator/contact@url<br/></td>
					<td valign="top"
						>/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource/gmd:CI_OnlineResource/gmd:linkage/gmd:URL<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$creatorEmailCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#creator_email_Attribute">creator_email</a>
						<br/>
					</td>
					<td valign="top">metadata/creator/contact@email</td>
					<td valign="top"
						>/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$institutionCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#institution_Attribute">institution</a>
						<br/>
					</td>
					<td valign="top">metadata/creator/name</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$creatorDateCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#date_created_Attribute">date_created</a>
					</td>
					<td valign="top">The date on which the data was created.<br/></td>
					<td valign="top">metadata/date[@type="created"]</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date<br/> /gmd:dateType/gmd:CI_DateTypeCode="creation"</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$modifiedDateCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#date_modified_Attribute">date_modified</a>
						<br/>
					</td>
					<td valign="top">The date on which this data was last modified.<br/></td>
					<td valign="top">metadata/date[@type="modified"]</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date<br/> /gmd:dateType/gmd:CI_DateTypeCode="revision"</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$issuedDateCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#date_issued_Attribute">date_issued</a>
						<br/>
					</td>
					<td valign="top">The date on which this data was formally issued.<br/></td>
					<td valign="top">metadata/date[@type="issued"]</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date/gmd:date/gco:Date<br/> /gmd:dateType/gmd:CI_DateTypeCode="publication"</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$projectCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#project_Attribute">project</a>
						<br/>
					</td>
					<td valign="top">The scientific project that produced the data.<br/></td>
					<td valign="top">metadata/project<br/></td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:aggregationInfo/gmd:MD_AggregateInformation/gmd:aggregateDataSetName/gmd:CI_Citation/gmd:title/gco:CharacterString<br/>
						DS_AssociationTypeCode="largerWorkCitation" and DS_InitiativeTypeCode="project"<br/>and/or<br/>
						/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString with gmd:MD_KeywordTypeCode="project" </td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$acknowledgmentCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#acknowledgement_Attribute">acknowledgment</a>
					</td>
					<td valign="top">A place to acknowledge various type of support for the project that produced this data.<br/></td>
					<td valign="top">metadata/documentation[@type="funding"]</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:credit/gco:CharacterString</td>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Contributor Search"/>
			<h2>Contributor Search Score: <xsl:value-of select="$contributorTotal"/>/<xsl:value-of select="$contributorMax"/></h2>
			<p>This section allows a data provider to include information about those that contribute to a data product in the metadata for the product. This is important for many reasons.</p>
			<table border="1">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$contributorNameCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#contributor_name_Attribute">contributor_name</a>
						<br/>
					</td>
					<td rowspan="2" colspan="1" valign="top">The name and role of any individuals or institutions that contributed to the creation of this data.<br/></td>
					<td valign="top">metadata/contributor<br/></td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$contributorRoleCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#contributor_role_Attribute">contributor_role</a>
						<br/>
					</td>
					<td valign="top">metadata/contributor@role</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:role/gmd:CI_RoleCode<br/> ="principalInvestigator" |
						"author"</td>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Publisher Search"/>
			<h2>Publisher Search Score: <xsl:value-of select="$publisherTotal"/>/<xsl:value-of select="$publisherMax"/></h2>
			<p>This section allows a data provider to include contact information for the publisher of a data product in the metadata for the product.</p>
			<table border="1">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$publisherNameCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#publisher_name_Attribute">publisher_name</a>
						<br/>
					</td>
					<td rowspan="3" colspan="1" valign="top">The data publisher's name, URL, and email. The publisher may be an individual or an institution.</td>
					<td valign="top">metadata/publisher/name<br/></td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString<br/>
						CI_RoleCode="publisher"<br/>and/or<br/> /gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString with
						gmd:MD_KeywordTypeCode="dataCenter" </td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$publisherURLCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#publisher_url_Attribute">publisher_url</a>
						<br/>
					</td>
					<td valign="top">metadata/publisher/contact@url<br/></td>
					<td valign="top"
						>/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource/gmd:CI_OnlineResource/gmd:linkage/gmd:URL<br/>
						CI_RoleCode="publisher"</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$publisherEmailCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#publisher_email_Attribute">publisher_email</a>
						<br/>
					</td>
					<td valign="top">metadata/publisher/contact@email</td>
					<td valign="top"
						>/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString<br/>
						CI_RoleCode="publisher"</td>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<a name="Other Attributes"/>
			<h2>Other Attributes Score: <xsl:value-of select="$otherTotal"/>/<xsl:value-of select="$otherMax"/></h2>
			<p>This group includes attributes that don't seem to fit in the other categories.</p>
			<table width="95%" border="1" cellpadding="2" cellspacing="2">
				<tr>
					<th valign="top">Score</th>
					<th valign="top">Attribute</th>
					<th valign="top">Description</th>
					<th valign="top">THREDDS</th>
					<th valign="top">ISO 19115-2</th>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$processingLevelCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#processing_level_Attribute">processing_level</a>
					</td>
					<td valign="top">A textual description of the processing (or quality control) level of the data.<br/></td>
					<td valign="top">metadata/documentation[@type="processing_level"]</td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$licenseCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#license_Attribute">license</a>
					</td>
					<td valign="top">Describe the restrictions to data access and distribution. </td>
					<td valign="top">metadata/documentation[@type="rights"]</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:useLimitation/gco:CharacterString<br/></td>
				</tr>
				<tr>
					<xsl:call-template name="showScore">
						<xsl:with-param name="score" select="$cdmTypeCnt"/>
					</xsl:call-template>
					<td valign="top">
						<a href="http://www.unidata.ucar.edu/software/netcdf-java/formats/DataDiscoveryAttConvention.html#cdm_data_type_Attribute">cdm_data_type</a>
						<br/>
					</td>
					<td valign="top">The <a href="http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/InvCatalogSpec.html#dataType">THREDDS data type</a> appropriate for this dataset.</td>
					<td valign="top">metadata/dataType</td>
					<td valign="top">/gmi:MI_Metadata/gmd:identificationInfo/gmd:MD_DataIdentification/gmd:spatialRepresentationType/gmd:MD_SpatialRepresentationTypeCode<br/> May need some extensions to this codelist. Current values:
						vector, grid, textTable, tin, stereoModel, video.</td>
				</tr>
			</table>
			<a href="#Identification">Identification</a> | <a href="#Text Search">Text Search</a> | <a href="#Extent Search">Extent Search</a> | <a href="#Other Extent Information">Other Extent Information</a> | <a href="#Creator Search"
				>Creator Search</a> | <a href="#Contributor Search">Contributor Search</a> | <a href="#Publisher Search">Publisher Search</a> | <a href="#Other Attributes">Other Attributes</a>
			<hr/> Rubric Version: <xsl:value-of select="$rubricVersion"/><br/>
			<a href="https://geo-ide.noaa.gov/wiki/index.php?title=NetCDF_Attribute_Convention_for_Dataset_Discovery">More Information</a>
		</html>
	</xsl:template>
	<xsl:template name="sequenceExists">
		<xsl:param name="seq"/>
		<xsl:choose>
			<xsl:when test="exists($seq)">1</xsl:when>
			<xsl:otherwise>0</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
