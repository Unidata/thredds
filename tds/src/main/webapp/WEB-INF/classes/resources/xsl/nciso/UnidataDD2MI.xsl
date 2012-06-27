<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmd="http://www.isotc211.org/2005/gmd"
  xmlns:gmi="http://www.isotc211.org/2005/gmi" xmlns:srv="http://www.isotc211.org/2005/srv" xmlns:gmx="http://www.isotc211.org/2005/gmx" xmlns:gsr="http://www.isotc211.org/2005/gsr" xmlns:gss="http://www.isotc211.org/2005/gss"
  xmlns:gts="http://www.isotc211.org/2005/gts" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:nc="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" exclude-result-prefixes="nc">
  <xd:doc xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" scope="stylesheet">
    <xd:desc>
      <xd:p><xd:b>Modified on:</xd:b> May 10, 2012</xd:p>
      <xd:p><xd:b>Version:</xd:b>2.3</xd:p>
      <xd:p><xd:b>Author:</xd:b>ted.habermann@noaa.gov</xd:p>
      <xd:p/>
    </xd:desc>
  </xd:doc>
  <xsl:variable name="stylesheetVersion" select="'2.3'"/>
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
  <xsl:strip-space elements="*"/>
  <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'"/>
  <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
  <xsl:key name="coverageTypes" match="nc:variable" use="nc:attribute[@name='coverage_type']/@value"/>
  <xsl:variable name="globalAttributeCnt" select="count(/nc:netcdf/nc:attribute)"/>
  <xsl:variable name="variableAttributeCnt" select="count(/nc:netcdf/nc:variable/nc:attribute)"/>
  <xsl:variable name="standardNameCnt" select="count(/nc:netcdf/nc:variable/nc:attribute[@name='standard_name'])"/>
  <xsl:variable name="dimensionCnt" select="count(/nc:netcdf/nc:dimension)"/>
  <!-- Identifier Fields: 4 possible -->
  <xsl:variable name="id" as="xs:string*" select="(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='id']/@value,
      /nc:netcdf/nc:attribute[@name='id']/@value)"/>
  <xsl:variable name="identifierNameSpace" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='naming_authority']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='authority']/@value)"/>
  <xsl:variable name="metadataConvention" as="xs:string*" select="/nc:netcdf/nc:attribute[@name='Metadata_Conventions']/@value"/>
  <xsl:variable name="metadataLink" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='Metadata_Link']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@name='xlink']/@value)"/>
  <!-- Service Fields: 4 possible -->
  <xsl:variable name="thredds_netcdfsubsetCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='nccs_service'])"/>
  <xsl:variable name="thredds_opendapCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='opendap_service'])"/>
  <xsl:variable name="thredds_wcsCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wcs_service'])"/>
  <xsl:variable name="thredds_wmsCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wms_service'])"/>
  <xsl:variable name="thredds_sosCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='sos_service'])"/>
  <xsl:variable name="thredds_httpCnt" select="count(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='httpserver_service'])"/>
  <xsl:variable name="serviceTotal" select="$thredds_netcdfsubsetCnt + $thredds_opendapCnt + $thredds_wcsCnt + $thredds_wmsCnt + $thredds_sosCnt + $thredds_httpCnt"/>
  <xsl:variable name="serviceMax">5</xsl:variable>
  <!-- Text Search Fields: 7 possible -->
  <xsl:variable name="title" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='title']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='full_name']/@value)"/>
  <xsl:variable name="summary" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='summary']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='Summary']/@value)"/>
  <xsl:variable name="keywords" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='keywords']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='keywords']/@value)"/>
  <xsl:variable name="keywordsVocabulary" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='keywords_vocabulary']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='keywords']/nc:attribute[@name='vocab']/@value)"/>
  <xsl:variable name="stdNameVocabulary" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='standard_name_vocabulary']/@value)"/>
  <xsl:variable name="comment" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='comment']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:document/nc:attribute[@type='comment']/@value)"/>
  <xsl:variable name="history" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='history']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:document/nc:attribute[@type='history']/@value)"/>
  <!-- Extent Search Fields: 17 possible -->
  <xsl:variable name="geospatial_lat_min" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_min']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lat_min']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_lat_min']/@value)"/>
  <xsl:variable name="geospatial_lat_max" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_max']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lat_max']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_lat_max']/@value)"/>
  <xsl:variable name="geospatial_lon_min" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lon_min']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lon_min']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_lon_min']/@value)"/>
  <xsl:variable name="geospatial_lon_max" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lon_max']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_lon_max']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_lon_max']/@value)"/>
  <xsl:variable name="timeStart" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_start']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='time_coverage_start']/@value,
    /nc:netcdf/nc:attribute[@name='time_coverage_start']/@value)"/>
  <xsl:variable name="timeEnd" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_end']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='time_coverage_end']/@value,
    /nc:netcdf/nc:attribute[@name='time_coverage_end']/@value)"/>
  <xsl:variable name="timeStartCnt" select="count($timeStart)"/>
  <xsl:variable name="timeEndCnt" select="count($timeEnd)"/>
  <xsl:variable name="verticalMin" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_min']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_vertical_min']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_vertical_min']/@value)"/>
  <xsl:variable name="verticalMax" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_max']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='geospatial_vertical_max']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_vertical_max']/@value)"/>
  <xsl:variable name="geospatial_lat_units" as="xs:string*"
    select="(//nc:variable[@name=$latitudeVariableName]/nc:attribute[@name='units']/@value,
    /nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_units']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_lat_units']/@value)"/>
  <xsl:variable name="geospatial_lon_units" as="xs:string*"
    select="(//nc:variable[@name=$longitudeVariableName]/nc:attribute[@name='units']/@value,
    /nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_units']/@value,
    //nc:attribute[@name='geospatial_lon_units']/@value)"/>
  <xsl:variable name="temporalUnits" as="xs:string*"
    select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_units']/@value, //nc:variable[@name=$timeVariableName]/nc:attribute[@name='units']/@value,
    //nc:attribute[@name='time_coverage_units']/@value)"/>
  <xsl:variable name="verticalUnits" as="xs:string*"
    select="(//nc:variable[@name=$verticalVariableName]/nc:attribute[@name='units']/@value,
    /nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_units']/@value,
    //nc:attribute[@name='geospatial_vertical_units']/@value)"/>
  <xsl:variable name="geospatial_lat_resolution" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lat_resolution']/@value,
    //nc:attribute[@name='geospatial_lat_resolution']/@value)"/>
  <xsl:variable name="geospatial_lon_resolution" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_lon_resolution']/@value,
    //nc:attribute[@name='geospatial_lon_resolution']/@value)"/>
  <xsl:variable name="timeResolution" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_resolution']/@value,
    //nc:attribute[@name='time_coverage_resolution']/@value)"/>
  <xsl:variable name="timeDuration" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='time_coverage_duration']/@value,
    //nc:attribute[@name='time_coverage_duration']/@value)"/>
  <xsl:variable name="verticalResolution" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_resolution']/@value,
    //nc:attribute[@name='geospatial_vertical_resolution']/@value)"/>
  <xsl:variable name="verticalPositive" as="xs:string*" select="(/nc:netcdf/nc:group[@name='CFMetadata']/nc:attribute[@name='geospatial_vertical_positive']/@value,
    /nc:netcdf/nc:attribute[@name='geospatial_vertical_positive']/@value)"/>
  <!-- dimension variables -->
  <xsl:variable name="longitudeVariableName" select="//nc:variable[nc:attribute[@name='units' and @value='degrees_east']]/@name"/>
  <xsl:variable name="latitudeVariableName" select="//nc:variable[nc:attribute[@name='units' and @value='degrees_north']]/@name"/>
  <xsl:variable name="verticalVariableName" select="//nc:variable[nc:attribute[@name='positive' and (@value='up' or @value='down')]]/@name"/>
  <xsl:variable name="timeVariableName" select="//nc:variable[nc:attribute[@name='standard_name' and lower-case(@value)='time']]/@name"/>
  <!--  Extent Totals -->
  <xsl:variable name="extentTotal" select="count($geospatial_lat_min) + count($geospatial_lat_max) + count($geospatial_lon_min) + count($geospatial_lon_max) + count($timeStart) + count($timeEnd) + count($verticalMin) + count($verticalMax)"/>
  <xsl:variable name="extentMax">8</xsl:variable>
  <xsl:variable name="otherExtentTotal"
    select="count($geospatial_lat_resolution) + count($geospatial_lat_units) + count($geospatial_lon_resolution) + count($geospatial_lon_units) + count($timeResolution) + count($timeDuration) + count($verticalUnits) + count($verticalResolution) + count($verticalPositive)"/>
  <xsl:variable name="otherExtentMax">9</xsl:variable>
  <!-- Responsible Party Fields: 14 possible -->
  <xsl:variable name="creatorName" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='creator_name']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='creators']/nc:group[@name='creator']/nc:attribute[@name='name']/@value)"/>
  <xsl:variable name="creatorURL" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='creator_url']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='creators']/nc:group[@name='creator']/nc:attribute[@name='url']/@value)"/>
  <xsl:variable name="creatorEmail" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='creator_email']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='creators']/nc:group[@name='creator']/nc:attribute[@name='email']/@value)"/>
  <xsl:variable name="creatorDate" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='date_created']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='dates']/nc:attribute[@type='created']/@value)"/>
  <xsl:variable name="modifiedDate" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='date_modified']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='dates']/nc:attribute[@type='modified']/@value)"/>
  <xsl:variable name="issuedDate" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='date_issued']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='dates']/nc:attribute[@type='issued']/@value)"/>
  <xsl:variable name="institution" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='institution']/@value)"/>
  <xsl:variable name="project" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='project']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='projects']/@value)"/>
  <xsl:variable name="acknowledgment" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='acknowledgment']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='funding']/@value)"/>
  <xsl:variable name="dateCnt" select="count($creatorDate) + count($modifiedDate) + count($issuedDate)"/>
  <xsl:variable name="creatorTotal" select="count($creatorName) + count($creatorURL) + count($creatorEmail) + count($creatorDate) + count($modifiedDate) + count($issuedDate) + count($institution) + count($project) + count($acknowledgment)"/>
  <xsl:variable name="creatorMax">9</xsl:variable>
  <!--  -->
  <xsl:variable name="contributorName" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='contributor_name']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='contributors']/nc:group[@name='contributor']/nc:attribute[@name='name']/@value)"/>
  <xsl:variable name="contributorRole" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='contributor_role']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='contributors']/nc:group[@name='contributor']/nc:attribute[@name='role']/@value)"/>
  <xsl:variable name="contributorTotal" select="count($contributorName) + count($contributorRole)"/>
  <xsl:variable name="contributorMax">2</xsl:variable>
  <!--  -->
  <xsl:variable name="publisherName" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='publisher_name']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='publishers']/nc:group[@name='publisher']/nc:attribute[@name='name']/@value)"/>
  <xsl:variable name="publisherURL" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='publisher_url']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='publishers']/nc:group[@name='publisher']/nc:attribute[@name='url']/@value)"/>
  <xsl:variable name="publisherEmail" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='publisher_email']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='publishers']/nc:group[@name='publisher']/nc:attribute[@name='email']/@value)"/>
  <xsl:variable name="publisherTotal" select="count($publisherName) + count($publisherURL) + count($publisherEmail)"/>
  <xsl:variable name="publisherMax">3</xsl:variable>
  <!--  -->
  <xsl:variable name="responsiblePartyCnt" select="count($creatorName) + count($contributorName) + count($publisherName)"/>
  <xsl:variable name="responsiblePartyTotal" select="$creatorTotal + $contributorTotal + $publisherTotal"/>
  <xsl:variable name="responsiblePartyMax">14</xsl:variable>
  <!-- Other Fields: 2 possible -->
  <xsl:variable name="cdmType" as="xs:string*" select="(/nc:netcdf/nc:attribute[@name='cdm_data_type']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='data_type']/@value)"/>
  <xsl:variable name="processingLevel" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='processing_level']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='processing_level']/@value)"/>
  <xsl:variable name="license" as="xs:string*"
    select="(/nc:netcdf/nc:attribute[@name='license']/@value,
    /nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='rights']/@value)"/>
  <xsl:variable name="otherTotal" select="count($cdmType) + count($processingLevel) + count($license)"/>
  <xsl:variable name="otherMax">3</xsl:variable>
  <xsl:variable name="spiralTotal" select="$extentTotal + $otherExtentTotal + $otherTotal + $responsiblePartyTotal"/>
  <xsl:variable name="spiralMax" select="$otherMax + $creatorMax + $extentMax + $responsiblePartyMax"/>
  <!--                        -->
  <!--    Write ISO Metadata  -->
  <!--                        -->
  <xsl:template match="/">
    <gmi:MI_Metadata>
      <xsl:attribute name="xsi:schemaLocation">
        <xsl:value-of select="'http://www.isotc211.org/2005/gmi http://www.ngdc.noaa.gov/metadata/published/xsd/schema.xsd'"/>
      </xsl:attribute>
      <gmd:fileIdentifier>
        <xsl:call-template name="writeCharacterString">
          <xsl:with-param name="stringToWrite" select="$id[1]"/>
        </xsl:call-template>
      </gmd:fileIdentifier>
      <gmd:language>
        <xsl:call-template name="writeCodelist">
          <xsl:with-param name="codeListName" select="'gmd:LanguageCode'"/>
          <xsl:with-param name="codeListValue" select="'eng'"/>
        </xsl:call-template>
      </gmd:language>
      <gmd:characterSet>
        <xsl:call-template name="writeCodelist">
          <xsl:with-param name="codeListName" select="'gmd:MD_CharacterSetCode'"/>
          <xsl:with-param name="codeListValue" select="'UTF8'"/>
        </xsl:call-template>
      </gmd:characterSet>
      <gmd:hierarchyLevel>
        <xsl:call-template name="writeCodelist">
          <xsl:with-param name="codeListName" select="'gmd:MD_ScopeCode'"/>
          <xsl:with-param name="codeListValue" select="'dataset'"/>
        </xsl:call-template>
      </gmd:hierarchyLevel>
      <xsl:if test="$serviceTotal">
        <gmd:hierarchyLevel>
          <xsl:call-template name="writeCodelist">
            <xsl:with-param name="codeListName" select="'gmd:MD_ScopeCode'"/>
            <xsl:with-param name="codeListValue" select="'service'"/>
          </xsl:call-template>
        </gmd:hierarchyLevel>
      </xsl:if>
      <!-- metadata contact is creator -->
      <xsl:call-template name="writeResponsibleParty">
        <xsl:with-param name="tagName" select="'gmd:contact'"/>
        <xsl:with-param name="testValue" select="$creatorTotal"/>
        <xsl:with-param name="individualName" select="$creatorName[1]"/>
        <xsl:with-param name="organisationName" select="$institution[1]"/>
        <xsl:with-param name="email" select="$creatorEmail[1]"/>
        <xsl:with-param name="url" select="$creatorURL[1]"/>
        <xsl:with-param name="roleCode" select="'pointOfContact'"/>
      </xsl:call-template>
      <gmd:dateStamp>
        <gco:Date>
          <xsl:value-of select="/nc:netcdf/nc:group[@name='NCISOMetadata']/nc:attribute[@name='metadata_creation']/@value"/>
        </gco:Date>
      </gmd:dateStamp>
      <gmd:metadataStandardName>
        <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for imagery and gridded data</gco:CharacterString>
      </gmd:metadataStandardName>
      <gmd:metadataStandardVersion>
        <gco:CharacterString>ISO 19115-2:2009(E)</gco:CharacterString>
      </gmd:metadataStandardVersion>
      <gmd:spatialRepresentationInfo>
        <xsl:choose>
          <xsl:when test="count($longitudeVariableName) + count($latitudeVariableName) + count($verticalVariableName) + count($timeVariableName)">
            <gmd:MD_GridSpatialRepresentation>
              <gmd:numberOfDimensions>
                <gco:Integer>
                  <xsl:value-of select="(count($longitudeVariableName) > 0) + (count($latitudeVariableName) > 0) + (count($verticalVariableName) > 0) + (count($timeVariableName) > 0)"/>
                </gco:Integer>
              </gmd:numberOfDimensions>
              <xsl:if test="count($longitudeVariableName)">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'column'"/>
                  <xsl:with-param name="dimensionUnits" select="$geospatial_lon_units[1]"/>
                  <xsl:with-param name="dimensionResolution" select="$geospatial_lon_resolution[1]"/>
                  <xsl:with-param name="dimensionSize" select="/nc:netcdf/nc:dimension[contains(@name,$longitudeVariableName)]/@length"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="count($latitudeVariableName)">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'row'"/>
                  <xsl:with-param name="dimensionUnits" select="$geospatial_lat_units[1]"/>
                  <xsl:with-param name="dimensionResolution" select="$geospatial_lat_resolution[1]"/>
                  <xsl:with-param name="dimensionSize" select="/nc:netcdf/nc:dimension[contains(@name,$latitudeVariableName)]/@length"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="count($verticalVariableName)">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'vertical'"/>
                  <xsl:with-param name="dimensionUnits" select="$verticalUnits[1]"/>
                  <xsl:with-param name="dimensionResolution" select="$verticalResolution[1]"/>
                  <xsl:with-param name="dimensionSize" select="/nc:netcdf/nc:dimension[contains(@name,$verticalVariableName)]/@length"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="count($timeVariableName)">
                <xsl:call-template name="writeDimension">
                  <xsl:with-param name="dimensionType" select="'temporal'"/>
                  <xsl:with-param name="dimensionUnits" select="$temporalUnits[1]"/>
                  <xsl:with-param name="dimensionResolution" select="$timeResolution[1]"/>
                  <xsl:with-param name="dimensionSize" select="/nc:netcdf/nc:dimension[contains(@name,$timeVariableName)]/@length"/>
                </xsl:call-template>
              </xsl:if>
              <gmd:cellGeometry>
                <xsl:call-template name="writeCodelist">
                  <xsl:with-param name="codeListName" select="'gmd:MD_CellGeometryCode'"/>
                  <xsl:with-param name="codeListValue" select="'area'"/>
                </xsl:call-template>
              </gmd:cellGeometry>
              <gmd:transformationParameterAvailability gco:nilReason="unknown"/>
            </gmd:MD_GridSpatialRepresentation>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="gco:nilReason">
              <xsl:value-of select="'missing'"/>
            </xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
      </gmd:spatialRepresentationInfo>
      <gmd:identificationInfo>
        <gmd:MD_DataIdentification id="DataIdentification">
          <gmd:citation>
            <gmd:CI_Citation>
              <gmd:title>
                <xsl:call-template name="writeCharacterString">
                  <xsl:with-param name="stringToWrite" select="$title[1]"/>
                </xsl:call-template>
              </gmd:title>
              <xsl:choose>
                <xsl:when test="$dateCnt">
                  <xsl:if test="count($creatorDate)">
                    <xsl:call-template name="writeDate">
                      <xsl:with-param name="testValue" select="count($creatorDate)"/>
                      <xsl:with-param name="dateToWrite" select="$creatorDate[1]"/>
                      <xsl:with-param name="dateType" select="'creation'"/>
                    </xsl:call-template>
                  </xsl:if>
                  <xsl:if test="count($issuedDate)">
                    <xsl:call-template name="writeDate">
                      <xsl:with-param name="testValue" select="count($issuedDate)"/>
                      <xsl:with-param name="dateToWrite" select="$issuedDate[1]"/>
                      <xsl:with-param name="dateType" select="'issued'"/>
                    </xsl:call-template>
                  </xsl:if>
                  <xsl:if test="count($modifiedDate)">
                    <xsl:call-template name="writeDate">
                      <xsl:with-param name="testValue" select="count($modifiedDate)"/>
                      <xsl:with-param name="dateToWrite" select="$modifiedDate[1]"/>
                      <xsl:with-param name="dateType" select="'revision'"/>
                    </xsl:call-template>
                  </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                  <gmd:date>
                    <xsl:attribute name="gco:nilReason">
                      <xsl:value-of select="'missing'"/>
                    </xsl:attribute>
                  </gmd:date>
                </xsl:otherwise>
              </xsl:choose>
              <gmd:identifier>
                <xsl:choose>
                  <xsl:when test="count($id)">
                    <gmd:MD_Identifier>
                      <xsl:if test="$identifierNameSpace[1]">
                        <gmd:authority>
                          <gmd:CI_Citation>
                            <gmd:title>
                              <gco:CharacterString>
                                <xsl:value-of select="$identifierNameSpace[1]"/>
                              </gco:CharacterString>
                            </gmd:title>
                            <gmd:date>
                              <xsl:attribute name="gco:nilReason">
                                <xsl:value-of select="'inapplicable'"/>
                              </xsl:attribute>
                            </gmd:date>
                          </gmd:CI_Citation>
                        </gmd:authority>
                      </xsl:if>
                      <gmd:code>
                        <!--  Just use THREDDs id as it's guaranteed to be unique -->
                        <gco:CharacterString>
                          <xsl:value-of select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:attribute[@name='id']/@value"/>
                        </gco:CharacterString>
                      </gmd:code>
                    </gmd:MD_Identifier>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:attribute name="gco:nilReason">
                      <xsl:value-of select="'missing'"/>
                    </xsl:attribute>
                  </xsl:otherwise>
                </xsl:choose>
              </gmd:identifier>
              <xsl:if test="$creatorTotal">
                <xsl:call-template name="writeResponsibleParty">
                  <xsl:with-param name="tagName" select="'gmd:citedResponsibleParty'"/>
                  <xsl:with-param name="testValue" select="$creatorTotal"/>
                  <xsl:with-param name="individualName" select="$creatorName[1]"/>
                  <xsl:with-param name="organisationName" select="$institution[1]"/>
                  <xsl:with-param name="email" select="$creatorEmail[1]"/>
                  <xsl:with-param name="url" select="$creatorURL[1]"/>
                  <xsl:with-param name="roleCode" select="'originator'"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="$contributorTotal">
                <xsl:call-template name="writeResponsibleParty">
                  <xsl:with-param name="tagName" select="'gmd:citedResponsibleParty'"/>
                  <xsl:with-param name="testValue" select="$contributorTotal"/>
                  <xsl:with-param name="individualName" select="$contributorName[1]"/>
                  <xsl:with-param name="organisationName"/>
                  <xsl:with-param name="email"/>
                  <xsl:with-param name="url"/>
                  <xsl:with-param name="roleCode" select="/nc:netcdf/nc:attribute[@name='contributor_role']/@value"/>
                </xsl:call-template>
              </xsl:if>
              <xsl:if test="$comment">
                <gmd:otherCitationDetails>
                  <xsl:call-template name="writeCharacterString">
                    <xsl:with-param name="stringToWrite" select="$comment[1]"/>
                  </xsl:call-template>
                </gmd:otherCitationDetails>
              </xsl:if>
            </gmd:CI_Citation>
          </gmd:citation>
          <gmd:abstract>
            <xsl:call-template name="writeCharacterString">
              <xsl:with-param name="stringToWrite" select="$summary[1]"/>
            </xsl:call-template>
          </gmd:abstract>
          <xsl:if test="count($acknowledgment)">
            <gmd:credit>
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="stringToWrite" select="$acknowledgment[1]"/>
              </xsl:call-template>
            </gmd:credit>
          </xsl:if>
          <!-- point of contact is creator -->
          <xsl:call-template name="writeResponsibleParty">
            <xsl:with-param name="tagName" select="'gmd:pointOfContact'"/>
            <xsl:with-param name="testValue" select="$creatorTotal"/>
            <xsl:with-param name="individualName" select="$creatorName[1]"/>
            <xsl:with-param name="organisationName" select="$institution[1]"/>
            <xsl:with-param name="email" select="$creatorEmail[1]"/>
            <xsl:with-param name="url" select="$creatorURL[1]"/>
            <xsl:with-param name="roleCode" select="'pointOfContact'"/>
          </xsl:call-template>
          <xsl:if test="count($keywords)">
            <gmd:descriptiveKeywords>
              <gmd:MD_Keywords>
                <xsl:variable name="keywordDelimiter">
                  <xsl:choose>
                    <xsl:when test="(contains($keywords[1],',') or contains($keywords[1],'&gt;'))">
                      <xsl:value-of select="','"/>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="' '"/>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:variable>
                <xsl:for-each select="tokenize($keywords[1],$keywordDelimiter)">
                  <gmd:keyword>
                    <gco:CharacterString>
                      <xsl:value-of select="."/>
                    </gco:CharacterString>
                  </gmd:keyword>
                </xsl:for-each>
                <gmd:type>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_KeywordTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'theme'"/>
                  </xsl:call-template>
                </gmd:type>
                <gmd:thesaurusName>
                  <gmd:CI_Citation>
                    <gmd:title>
                      <xsl:call-template name="writeCharacterString">
                        <xsl:with-param name="stringToWrite" select="$keywordsVocabulary[1]"/>
                      </xsl:call-template>
                    </gmd:title>
                    <gmd:date>
                      <xsl:attribute name="gco:nilReason">
                        <xsl:value-of select="'unknown'"/>
                      </xsl:attribute>
                    </gmd:date>
                  </gmd:CI_Citation>
                </gmd:thesaurusName>
              </gmd:MD_Keywords>
            </gmd:descriptiveKeywords>
          </xsl:if>
          <xsl:if test="count($project)">
            <gmd:descriptiveKeywords>
              <gmd:MD_Keywords>
                <gmd:keyword>
                  <gco:CharacterString>
                    <xsl:value-of select="$project[1]"/>
                  </gco:CharacterString>
                </gmd:keyword>
                <gmd:type>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_KeywordTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'project'"/>
                  </xsl:call-template>
                </gmd:type>
                <gmd:thesaurusName>
                  <xsl:attribute name="gco:nilReason">
                    <xsl:value-of select="'unknown'"/>
                  </xsl:attribute>
                </gmd:thesaurusName>
              </gmd:MD_Keywords>
            </gmd:descriptiveKeywords>
          </xsl:if>
          <xsl:if test="count($publisherName)">
            <gmd:descriptiveKeywords>
              <gmd:MD_Keywords>
                <gmd:keyword>
                  <gco:CharacterString>
                    <xsl:value-of select="$publisherName[1]"/>
                  </gco:CharacterString>
                </gmd:keyword>
                <gmd:type>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_KeywordTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'dataCenter'"/>
                  </xsl:call-template>
                </gmd:type>
                <gmd:thesaurusName>
                  <xsl:attribute name="gco:nilReason">
                    <xsl:value-of select="'unknown'"/>
                  </xsl:attribute>
                </gmd:thesaurusName>
              </gmd:MD_Keywords>
            </gmd:descriptiveKeywords>
          </xsl:if>
          <xsl:if test="$standardNameCnt">
            <gmd:descriptiveKeywords>
              <gmd:MD_Keywords>
                <xsl:for-each select="/nc:netcdf/nc:variable/nc:attribute[@name='standard_name']">
                  <gmd:keyword>
                    <gco:CharacterString>
                      <xsl:value-of select="./@value"/>
                    </gco:CharacterString>
                  </gmd:keyword>
                </xsl:for-each>
                <gmd:type>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_KeywordTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'theme'"/>
                  </xsl:call-template>
                </gmd:type>
                <gmd:thesaurusName>
                  <gmd:CI_Citation>
                    <gmd:title>
                      <xsl:call-template name="writeCharacterString">
                        <xsl:with-param name="stringToWrite" select="$stdNameVocabulary[1]"/>
                      </xsl:call-template>
                    </gmd:title>
                    <gmd:date gco:nilReason="unknown"/>
                  </gmd:CI_Citation>
                </gmd:thesaurusName>
              </gmd:MD_Keywords>
            </gmd:descriptiveKeywords>
          </xsl:if>
          <xsl:if test="count($license)">
            <gmd:resourceConstraints>
              <gmd:MD_LegalConstraints>
                <gmd:useLimitation>
                  <gco:CharacterString>
                    <xsl:value-of select="$license[1]"/>
                  </gco:CharacterString>
                </gmd:useLimitation>
              </gmd:MD_LegalConstraints>
            </gmd:resourceConstraints>
          </xsl:if>
          <xsl:if test="count($project)">
            <gmd:aggregationInfo>
              <gmd:MD_AggregateInformation>
                <gmd:aggregateDataSetName>
                  <gmd:CI_Citation>
                    <gmd:title>
                      <gco:CharacterString>
                        <xsl:value-of select="$project[1]"/>
                      </gco:CharacterString>
                    </gmd:title>
                    <gmd:date gco:nilReason="inapplicable"/>
                  </gmd:CI_Citation>
                </gmd:aggregateDataSetName>
                <gmd:associationType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_AssociationTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'largerWorkCitation'"/>
                  </xsl:call-template>
                </gmd:associationType>
                <gmd:initiativeType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_InitiativeTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'project'"/>
                  </xsl:call-template>
                </gmd:initiativeType>
              </gmd:MD_AggregateInformation>
            </gmd:aggregationInfo>
          </xsl:if>
          <xsl:if test="count($cdmType)">
            <gmd:aggregationInfo>
              <gmd:MD_AggregateInformation>
                <gmd:aggregateDataSetIdentifier>
                  <gmd:MD_Identifier>
                    <gmd:authority>
                      <gmd:CI_Citation>
                        <gmd:title>
                          <gco:CharacterString>Unidata Common Data Model</gco:CharacterString>
                        </gmd:title>
                        <gmd:date gco:nilReason="inapplicable"/>
                      </gmd:CI_Citation>
                    </gmd:authority>
                    <gmd:code>
                      <gco:CharacterString>
                        <xsl:value-of select="$cdmType[1]"/>
                      </gco:CharacterString>
                    </gmd:code>
                  </gmd:MD_Identifier>
                </gmd:aggregateDataSetIdentifier>
                <gmd:associationType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_AssociationTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'largerWorkCitation'"/>
                  </xsl:call-template>
                </gmd:associationType>
                <gmd:initiativeType>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:DS_InitiativeTypeCode'"/>
                    <xsl:with-param name="codeListValue" select="'project'"/>
                  </xsl:call-template>
                </gmd:initiativeType>
              </gmd:MD_AggregateInformation>
            </gmd:aggregationInfo>
          </xsl:if>
          <gmd:language>
            <gco:CharacterString>eng</gco:CharacterString>
          </gmd:language>
          <gmd:topicCategory>
            <gmd:MD_TopicCategoryCode>climatologyMeteorologyAtmosphere</gmd:MD_TopicCategoryCode>
          </gmd:topicCategory>
          <gmd:extent>
            <xsl:choose>
              <xsl:when test="$extentTotal">
                <gmd:EX_Extent>
                  <xsl:attribute name="id">
                    <xsl:value-of select="'boundingExtent'"/>
                  </xsl:attribute>
                  <xsl:if test="count($geospatial_lat_min) + count($geospatial_lon_min) + count($geospatial_lat_max) + count($geospatial_lon_max)">
                    <gmd:geographicElement>
                      <gmd:EX_GeographicBoundingBox id="boundingGeographicBoundingBox">
                        <gmd:extentTypeCode>
                          <gco:Boolean>1</gco:Boolean>
                        </gmd:extentTypeCode>
                        <gmd:westBoundLongitude>
                          <gco:Decimal>
                            <xsl:value-of select="$geospatial_lon_min[1]"/>
                          </gco:Decimal>
                        </gmd:westBoundLongitude>
                        <gmd:eastBoundLongitude>
                          <gco:Decimal>
                            <xsl:value-of select="$geospatial_lon_max[1]"/>
                          </gco:Decimal>
                        </gmd:eastBoundLongitude>
                        <gmd:southBoundLatitude>
                          <gco:Decimal>
                            <xsl:value-of select="$geospatial_lat_min[1]"/>
                          </gco:Decimal>
                        </gmd:southBoundLatitude>
                        <gmd:northBoundLatitude>
                          <gco:Decimal>
                            <xsl:value-of select="$geospatial_lat_max[1]"/>
                          </gco:Decimal>
                        </gmd:northBoundLatitude>
                      </gmd:EX_GeographicBoundingBox>
                    </gmd:geographicElement>
                  </xsl:if>
                  <xsl:if test="count($timeStart) + count($timeEnd)">
                    <gmd:temporalElement>
                      <gmd:EX_TemporalExtent>
                        <xsl:attribute name="id">
                          <xsl:value-of select="'boundingTemporalExtent'"/>
                        </xsl:attribute>
                        <gmd:extent>
                          <gml:TimePeriod gml:id="{generate-id()}">
                            <gml:description>
                              <xsl:value-of select="$temporalUnits[1]"/>
                            </gml:description>
                            <gml:beginPosition>
                              <xsl:value-of select="$timeStart[1]"/>
                            </gml:beginPosition>
                            <gml:endPosition>
                              <xsl:value-of select="$timeEnd[1]"/>
                            </gml:endPosition>
                          </gml:TimePeriod>
                        </gmd:extent>
                      </gmd:EX_TemporalExtent>
                    </gmd:temporalElement>
                  </xsl:if>
                  <xsl:if test="count($verticalMin) + count($verticalMax)">
                    <gmd:verticalElement>
                      <gmd:EX_VerticalExtent>
                        <gmd:minimumValue>
                          <gco:Real>
                            <xsl:choose>
                              <xsl:when test="$verticalPositive[1] = 'down'">
                                <xsl:value-of select="$verticalMin[1] * -1"/>
                              </xsl:when>
                              <xsl:otherwise>
                                <xsl:value-of select="$verticalMin[1]"/>
                              </xsl:otherwise>
                            </xsl:choose>
                          </gco:Real>
                        </gmd:minimumValue>
                        <gmd:maximumValue>
                          <gco:Real>
                            <xsl:choose>
                              <xsl:when test="$verticalPositive[1] = 'down'">
                                <xsl:value-of select="$verticalMax[1] * -1"/>
                              </xsl:when>
                              <xsl:otherwise>
                                <xsl:value-of select="$verticalMax[1]"/>
                              </xsl:otherwise>
                            </xsl:choose>
                          </gco:Real>
                        </gmd:maximumValue>
                        <gmd:verticalCRS>
                          <xsl:attribute name="gco:nilReason">
                            <xsl:value-of select="'missing'"/>
                          </xsl:attribute>
                        </gmd:verticalCRS>
                      </gmd:EX_VerticalExtent>
                    </gmd:verticalElement>
                  </xsl:if>
                </gmd:EX_Extent>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="gco:nilReason">
                  <xsl:value-of select="'missing'"/>
                </xsl:attribute>
              </xsl:otherwise>
            </xsl:choose>
          </gmd:extent>
        </gmd:MD_DataIdentification>
      </gmd:identificationInfo>
      <xsl:if test="$thredds_opendapCnt">
        <xsl:call-template name="writeService">
          <xsl:with-param name="serviceID" select="'OPeNDAP'"/>
          <xsl:with-param name="serviceTypeName" select="'THREDDS OPeNDAP'"/>
          <xsl:with-param name="serviceOperationName" select="'OPeNDAP Client Access'"/>
          <xsl:with-param name="operationURL" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='opendap_service']/@value"/>
          <xsl:with-param name="operationNode" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='opendap_service']" as="node()"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:if test="$thredds_wcsCnt">
        <xsl:call-template name="writeService">
          <xsl:with-param name="serviceID" select="'OGC-WCS'"/>
          <xsl:with-param name="serviceTypeName" select="'Open Geospatial Consortium Web Coverage Service (WCS)'"/>
          <xsl:with-param name="serviceOperationName" select="'GetCapabilities'"/>
          <xsl:with-param name="operationURL" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wcs_service']/@value"/>
          <xsl:with-param name="operationNode" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wcs_service']" as="node()"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:if test="$thredds_wmsCnt">
        <xsl:call-template name="writeService">
          <xsl:with-param name="serviceID" select="'OGC-WMS'"/>
          <xsl:with-param name="serviceTypeName" select="'Open Geospatial Consortium Web Map Service (WMS)'"/>
          <xsl:with-param name="serviceOperationName" select="'GetCapabilities'"/>
          <xsl:with-param name="operationURL" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wms_service']/@value"/>
          <xsl:with-param name="operationNode" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='wms_service']" as="node()"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:if test="$thredds_sosCnt">
        <xsl:call-template name="writeService">
          <xsl:with-param name="serviceID" select="'OGC-SOS'"/>
          <xsl:with-param name="serviceTypeName" select="'Open Geospatial Consortium Sensor Observation Service (SOS)'"/>
          <xsl:with-param name="serviceOperationName" select="'GetCapabilities'"/>
          <xsl:with-param name="operationURL" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='sos_service']/@value"/>
          <xsl:with-param name="operationNode" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='sos_service']" as="node()"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:if test="$thredds_netcdfsubsetCnt">
        <xsl:call-template name="writeService">
          <xsl:with-param name="serviceID" select="'THREDDS_NetCDF_Subset'"/>
          <xsl:with-param name="serviceTypeName" select="'THREDDS NetCDF Subset Service'"/>
          <xsl:with-param name="serviceOperationName" select="'NetCDFSubsetService'"/>
          <xsl:with-param name="operationURL" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='nccs_service']/@value"/>
          <xsl:with-param name="operationNode" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='nccs_service']" as="node()"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:if test="$thredds_httpCnt">
        <xsl:call-template name="writeService">
          <xsl:with-param name="serviceID" select="'THREDDS_HTTP_Service'"/>
          <xsl:with-param name="serviceTypeName" select="'THREDDS HTTP Service'"/>
          <xsl:with-param name="serviceOperationName" select="'FileHTTPService'"/>
          <xsl:with-param name="operationURL" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='httpserver_service']/@value"/>
          <xsl:with-param name="operationNode" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='httpserver_service']" as="node()"/>
        </xsl:call-template>
      </xsl:if>
      <!-- Output variables with coverage_type -->
      <xsl:for-each select="//nc:variable[generate-id() = 
      generate-id(key('coverageTypes',nc:attribute[@name='coverage_type']/@value)[1])]">
        <gmd:contentInfo>
          <gmi:MI_CoverageDescription>
            <gmd:attributeDescription>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'unknown'"/>
              </xsl:attribute>
            </gmd:attributeDescription>
            <gmd:contentType>
              <xsl:call-template name="writeCodelist">
                <xsl:with-param name="codeListName" select="'gmd:MD_CoverageContentTypeCode'"/>
                <xsl:with-param name="codeListValue" select="nc:attribute[@name='coverage_type']/@value"/>
              </xsl:call-template>
            </gmd:contentType>
            <xsl:for-each select="key('coverageTypes',nc:attribute[@name='coverage_type']/@value)">
              <xsl:call-template name="writeVariableDimensions">
                <xsl:with-param name="variableName" select="./@name"/>
                <xsl:with-param name="variableLongName" select="./nc:attribute[@name='long_name']/@value"/>
                <xsl:with-param name="variableStandardName" select="./nc:attribute[@name='standard_name']/@value"/>
                <xsl:with-param name="variableType" select="./@type"/>
                <xsl:with-param name="variableUnits" select="./nc:attribute[@name='units']/@value"/>
              </xsl:call-template>
            </xsl:for-each>
          </gmi:MI_CoverageDescription>
        </gmd:contentInfo>
      </xsl:for-each>
      <!-- Output variables with no coverage_type -->
      <xsl:if test="count(//nc:variable[not(nc:attribute/@name='coverage_type')])">
        <gmd:contentInfo>
          <gmi:MI_CoverageDescription>
            <gmd:attributeDescription>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'unknown'"/>
              </xsl:attribute>
            </gmd:attributeDescription>
            <gmd:contentType>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'unknown'"/>
              </xsl:attribute>
            </gmd:contentType>
            <xsl:for-each select="//nc:variable[not(nc:attribute/@name='coverage_type')]">
              <xsl:call-template name="writeVariableDimensions">
                <xsl:with-param name="variableName" select="./@name"/>
                <xsl:with-param name="variableLongName" select="./nc:attribute[@name='long_name']/@value"/>
                <xsl:with-param name="variableStandardName" select="./nc:attribute[@name='standard_name']/@value"/>
                <xsl:with-param name="variableType" select="./@type"/>
                <xsl:with-param name="variableUnits" select="./nc:attribute[@name='units']/@value"/>
              </xsl:call-template>
            </xsl:for-each>
          </gmi:MI_CoverageDescription>
        </gmd:contentInfo>
      </xsl:if>
      <!-- distributor is netCDF publisher -->
      <xsl:if test="$publisherTotal or $thredds_opendapCnt">
        <gmd:distributionInfo>
          <gmd:MD_Distribution>
            <gmd:distributor>
              <gmd:MD_Distributor>
                <xsl:choose>
                  <xsl:when test="$publisherTotal">
                    <xsl:call-template name="writeResponsibleParty">
                      <xsl:with-param name="tagName" select="'gmd:distributorContact'"/>
                      <xsl:with-param name="testValue" select="$publisherTotal"/>
                      <xsl:with-param name="individualName"/>
                      <xsl:with-param name="organisationName" select="$publisherName[1]"/>
                      <xsl:with-param name="email" select="$publisherEmail[1]"/>
                      <xsl:with-param name="url" select="$publisherURL[1]"/>
                      <xsl:with-param name="urlName" select="'URL for the data publisher'"/>
                      <xsl:with-param name="urlDescription" select="'This URL provides contact information for the publisher of this dataset'"/>
                      <xsl:with-param name="roleCode" select="'publisher'"/>
                    </xsl:call-template>
                  </xsl:when>
                  <xsl:otherwise>
                    <gmd:distributorContact gco:nilReason="missing"/>
                  </xsl:otherwise>
                </xsl:choose>
                <gmd:distributorFormat>
                  <gmd:MD_Format>
                    <gmd:name>
                      <gco:CharacterString>OPeNDAP</gco:CharacterString>
                    </gmd:name>
                    <gmd:version gco:nilReason="unknown"/>
                  </gmd:MD_Format>
                </gmd:distributorFormat>
                <xsl:if test="$thredds_opendapCnt">
                  <gmd:distributorTransferOptions>
                    <gmd:MD_DigitalTransferOptions>
                      <gmd:onLine>
                        <gmd:CI_OnlineResource>
                          <gmd:linkage>
                            <gmd:URL>
                              <xsl:value-of select="concat(/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='opendap_service']/@value,'.html')"/>
                            </gmd:URL>
                          </gmd:linkage>
                          <gmd:name>
                            <gco:CharacterString>File Information</gco:CharacterString>
                          </gmd:name>
                          <gmd:description>
                            <gco:CharacterString>This URL provides a standard OPeNDAP html interface for selecting data from this dataset. Change the extension to .info for a description of the dataset.</gco:CharacterString>
                          </gmd:description>
                          <gmd:function>
                            <gmd:CI_OnLineFunctionCode codeList="http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode" codeListValue="download">download</gmd:CI_OnLineFunctionCode>
                          </gmd:function>
                        </gmd:CI_OnlineResource>
                      </gmd:onLine>
                    </gmd:MD_DigitalTransferOptions>
                  </gmd:distributorTransferOptions>
                  <!-- Climate Weather Toolkit Transfer option-->
                  <gmd:distributorTransferOptions>
                    <gmd:MD_DigitalTransferOptions>
                      <gmd:onLine>
                        <gmd:CI_OnlineResource>
                          <gmd:linkage>
                            <gmd:URL>
                              <xsl:value-of
                                select="concat('http://www.ncdc.noaa.gov/oa/wct/wct-jnlp-beta.php?singlefile=',/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='services']/nc:attribute[@name='opendap_service']/@value)"/>
                            </gmd:URL>
                          </gmd:linkage>
                          <gmd:name>
                            <gco:CharacterString>Viewer Information</gco:CharacterString>
                          </gmd:name>
                          <gmd:description>
                            <gco:CharacterString>This URL provides an NCDC climate and weather toolkit view of an OPeNDAP resource.</gco:CharacterString>
                          </gmd:description>
                          <gmd:function>
                            <gmd:CI_OnLineFunctionCode codeList="http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#CI_PresentationFormCode" codeListValue="mapDigital"
                              >mapDigital</gmd:CI_OnLineFunctionCode>
                          </gmd:function>
                        </gmd:CI_OnlineResource>
                      </gmd:onLine>
                    </gmd:MD_DigitalTransferOptions>
                  </gmd:distributorTransferOptions>
                </xsl:if>
              </gmd:MD_Distributor>
            </gmd:distributor>
          </gmd:MD_Distribution>
        </gmd:distributionInfo>
      </xsl:if>
      <xsl:if test="normalize-space($history[1])!=''">
        <gmd:dataQualityInfo>
          <gmd:DQ_DataQuality>
            <gmd:scope>
              <gmd:DQ_Scope>
                <gmd:level>
                  <xsl:call-template name="writeCodelist">
                    <xsl:with-param name="codeListName" select="'gmd:MD_ScopeCode'"/>
                    <xsl:with-param name="codeListValue" select="'dataset'"/>
                  </xsl:call-template>
                </gmd:level>
              </gmd:DQ_Scope>
            </gmd:scope>
            <gmd:lineage>
              <gmd:LI_Lineage>
                <gmd:statement>
                  <xsl:call-template name="writeCharacterString">
                    <xsl:with-param name="stringToWrite" select="$history[1]"/>
                  </xsl:call-template>
                </gmd:statement>
              </gmd:LI_Lineage>
            </gmd:lineage>
          </gmd:DQ_DataQuality>
        </gmd:dataQualityInfo>
      </xsl:if>
      <gmd:metadataMaintenance>
        <gmd:MD_MaintenanceInformation>
          <gmd:maintenanceAndUpdateFrequency gco:nilReason="unknown"/>
          <gmd:maintenanceNote>
            <gco:CharacterString>This record was translated from NcML using UnidataDD2MI.xsl Version <xsl:value-of select="$stylesheetVersion"/></gco:CharacterString>
          </gmd:maintenanceNote>
        </gmd:MD_MaintenanceInformation>
      </gmd:metadataMaintenance>
    </gmi:MI_Metadata>
  </xsl:template>
  <xsl:template name="writeCodelist">
    <xsl:param name="codeListName"/>
    <xsl:param name="codeListValue"/>
    <xsl:variable name="codeListLocation" select="'http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml'"/>
    <xsl:element name="{$codeListName}">
      <xsl:attribute name="codeList">
        <xsl:value-of select="$codeListLocation"/>
        <xsl:value-of select="'#'"/>
        <xsl:value-of select="substring-after($codeListName,':')"/>
      </xsl:attribute>
      <xsl:attribute name="codeListValue">
        <xsl:value-of select="$codeListValue"/>
      </xsl:attribute>
      <xsl:value-of select="$codeListValue"/>
    </xsl:element>
  </xsl:template>
  <xsl:template name="writeCharacterString">
    <xsl:param name="stringToWrite"/>
    <xsl:choose>
      <xsl:when test="$stringToWrite">
        <gco:CharacterString>
          <xsl:value-of select="$stringToWrite"/>
        </gco:CharacterString>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="gco:nilReason">
          <xsl:value-of select="'missing'"/>
        </xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="writeDate">
    <xsl:param name="testValue"/>
    <xsl:param name="dateToWrite"/>
    <xsl:param name="dateType"/>
    <xsl:if test="$testValue">
      <xsl:choose>
        <xsl:when test="contains(translate($dateToWrite,' ','T'), 'T' ) ">
          <gmd:date>
            <gmd:CI_Date>
              <gmd:date>
                <gco:DateTime>
                  <xsl:value-of select="translate($dateToWrite,' ','T')"/>
                </gco:DateTime>
              </gmd:date>
              <gmd:dateType>
                <xsl:call-template name="writeCodelist">
                  <xsl:with-param name="codeListName" select="'gmd:CI_DateTypeCode'"/>
                  <xsl:with-param name="codeListValue" select="$dateType"/>
                </xsl:call-template>
              </gmd:dateType>
            </gmd:CI_Date>
          </gmd:date>
        </xsl:when>
        <xsl:otherwise>
          <gmd:date>
            <gmd:CI_Date>
              <gmd:date>
                <gco:Date>
                  <xsl:value-of select="$dateToWrite"/>
                </gco:Date>
              </gmd:date>
              <gmd:dateType>
                <xsl:call-template name="writeCodelist">
                  <xsl:with-param name="codeListName" select="'gmd:CI_DateTypeCode'"/>
                  <xsl:with-param name="codeListValue" select="$dateType"/>
                </xsl:call-template>
              </gmd:dateType>
            </gmd:CI_Date>
          </gmd:date>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
  <xsl:template name="writeResponsibleParty">
    <xsl:param name="tagName"/>
    <xsl:param name="testValue"/>
    <xsl:param name="individualName"/>
    <xsl:param name="organisationName"/>
    <xsl:param name="email"/>
    <xsl:param name="url"/>
    <xsl:param name="urlName"/>
    <xsl:param name="urlDescription"/>
    <xsl:param name="roleCode"/>
    <xsl:choose>
      <xsl:when test="$testValue">
        <xsl:element name="{$tagName}">
          <gmd:CI_ResponsibleParty>
            <gmd:individualName>
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="stringToWrite" select="$individualName"/>
              </xsl:call-template>
            </gmd:individualName>
            <gmd:organisationName>
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="stringToWrite" select="$organisationName"/>
              </xsl:call-template>
            </gmd:organisationName>
            <gmd:contactInfo>
              <xsl:choose>
                <xsl:when test="$email or $url">
                  <gmd:CI_Contact>
                    <xsl:if test="$email">
                      <gmd:address>
                        <gmd:CI_Address>
                          <gmd:electronicMailAddress>
                            <gco:CharacterString>
                              <xsl:value-of select="$email"/>
                            </gco:CharacterString>
                          </gmd:electronicMailAddress>
                        </gmd:CI_Address>
                      </gmd:address>
                    </xsl:if>
                    <xsl:if test="$url">
                      <gmd:onlineResource>
                        <gmd:CI_OnlineResource>
                          <gmd:linkage>
                            <gmd:URL>
                              <xsl:value-of select="$url"/>
                            </gmd:URL>
                          </gmd:linkage>
                          <gmd:protocol>
                            <gco:CharacterString>http</gco:CharacterString>
                          </gmd:protocol>
                          <gmd:applicationProfile>
                            <gco:CharacterString>web browser</gco:CharacterString>
                          </gmd:applicationProfile>
                          <gmd:name>
                            <gco:CharacterString>
                              <xsl:value-of select="$urlName"/>
                            </gco:CharacterString>
                          </gmd:name>
                          <gmd:description>
                            <gco:CharacterString>
                              <xsl:value-of select="$urlDescription"/>
                            </gco:CharacterString>
                          </gmd:description>
                          <gmd:function>
                            <xsl:call-template name="writeCodelist">
                              <xsl:with-param name="codeListName" select="'gmd:CI_OnLineFunctionCode'"/>
                              <xsl:with-param name="codeListValue" select="'information'"/>
                            </xsl:call-template>
                          </gmd:function>
                        </gmd:CI_OnlineResource>
                      </gmd:onlineResource>
                    </xsl:if>
                  </gmd:CI_Contact>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:attribute name="gco:nilReason">
                    <xsl:value-of select="'missing'"/>
                  </xsl:attribute>
                </xsl:otherwise>
              </xsl:choose>
            </gmd:contactInfo>
            <gmd:role>
              <xsl:call-template name="writeCodelist">
                <xsl:with-param name="codeListName" select="'gmd:CI_RoleCode'"/>
                <xsl:with-param name="codeListValue" select="$roleCode"/>
              </xsl:call-template>
            </gmd:role>
          </gmd:CI_ResponsibleParty>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="{$tagName}">
          <xsl:attribute name="gco:nilReason">missing</xsl:attribute>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="writeDimension">
    <xsl:param name="dimensionName"/>
    <xsl:param name="dimensionType"/>
    <xsl:param name="dimensionUnits"/>
    <xsl:param name="dimensionResolution"/>
    <xsl:param name="dimensionSize"/>
    <gmd:axisDimensionProperties>
      <gmd:MD_Dimension>
        <xsl:if test="$dimensionName">
          <xsl:attribute name="id">
            <xsl:value-of select="$dimensionName"/>
          </xsl:attribute>
        </xsl:if>
        <gmd:dimensionName>
          <xsl:call-template name="writeCodelist">
            <xsl:with-param name="codeListName" select="'gmd:MD_DimensionNameTypeCode'"/>
            <xsl:with-param name="codeListValue" select="$dimensionType"/>
          </xsl:call-template>
        </gmd:dimensionName>
        <xsl:choose>
          <xsl:when test="$dimensionSize">
            <gmd:dimensionSize>
              <gco:Integer>
                <xsl:value-of select="$dimensionSize"/>
              </gco:Integer>
            </gmd:dimensionSize>
          </xsl:when>
          <xsl:otherwise>
            <gmd:dimensionSize>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'unknown'"/>
              </xsl:attribute>
            </gmd:dimensionSize>
          </xsl:otherwise>
        </xsl:choose>
        <gmd:resolution>
          <xsl:choose>
            <xsl:when test="contains($dimensionResolution,' ')">
              <gco:Measure>
                <xsl:attribute name="uom">
                  <xsl:value-of select="substring-after($dimensionResolution,' ')"/>
                </xsl:attribute>
                <xsl:value-of select="substring-before($dimensionResolution,' ')"/>
              </gco:Measure>
            </xsl:when>
            <xsl:when test="contains($dimensionResolution,'PT')">
              <gco:Measure>
                <xsl:attribute name="uom">
                  <xsl:if test="substring($dimensionResolution, 4)='S'">
                    <xsl:value-of select="translate(substring($dimensionResolution, 4),$uppercase, $smallcase)"/>
                  </xsl:if>
                  <xsl:if test="substring($dimensionResolution, 4)='M'">minutes</xsl:if>
                </xsl:attribute>
                <xsl:value-of select="substring($dimensionResolution,3,1)"/>
              </gco:Measure>
            </xsl:when>
            <xsl:when test="$dimensionUnits and $dimensionResolution">
              <gco:Measure>
                <xsl:attribute name="uom">
                  <xsl:value-of select="$dimensionUnits"/>
                </xsl:attribute>
                <xsl:value-of select="$dimensionResolution"/>
              </gco:Measure>
            </xsl:when>
            <xsl:when test="$dimensionUnits and not($dimensionResolution)">
              <xsl:attribute name="gco:nilReason">missing</xsl:attribute>
            </xsl:when>
            <xsl:when test="not($dimensionUnits) and $dimensionResolution">
              <gco:Measure>
                <xsl:attribute name="uom">
                  <xsl:value-of select="'unknown'"/>
                </xsl:attribute>
                <xsl:value-of select="$dimensionResolution"/>
              </gco:Measure>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'missing'"/>
              </xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
        </gmd:resolution>
      </gmd:MD_Dimension>
    </gmd:axisDimensionProperties>
  </xsl:template>
  <xsl:template name="writeVariableDimensions">
    <xsl:param name="variableName"/>
    <xsl:param name="variableLongName"/>
    <xsl:param name="variableStandardName"/>
    <xsl:param name="variableType"/>
    <xsl:param name="variableUnits"/>
    <gmd:dimension>
      <gmd:MD_Band>
        <gmd:sequenceIdentifier>
          <gco:MemberName>
            <gco:aName>
              <gco:CharacterString>
                <xsl:value-of select="$variableName"/>
              </gco:CharacterString>
            </gco:aName>
            <gco:attributeType>
              <gco:TypeName>
                <gco:aName>
                  <gco:CharacterString>
                    <xsl:value-of select="$variableType"/>
                  </gco:CharacterString>
                </gco:aName>
              </gco:TypeName>
            </gco:attributeType>
          </gco:MemberName>
        </gmd:sequenceIdentifier>
        <gmd:descriptor>
          <xsl:call-template name="writeCharacterString">
            <xsl:with-param name="stringToWrite">
              <xsl:value-of select="$variableLongName"/>
              <xsl:if test="$variableStandardName">
                <xsl:value-of select="concat(' (',$variableStandardName,')')"/>
              </xsl:if>
            </xsl:with-param>
          </xsl:call-template>
        </gmd:descriptor>
        <xsl:if test="$variableUnits">
          <gmd:units>
            <xsl:attribute name="xlink:href">
              <xsl:value-of select="'http://example.org/someUnitsDictionary.xml#'"/>
              <xsl:value-of select="encode-for-uri($variableUnits)"/>
            </xsl:attribute>
          </gmd:units>
        </xsl:if>
      </gmd:MD_Band>
    </gmd:dimension>
  </xsl:template>
  <xsl:template name="writeVariableRanges">
    <xsl:param name="variableName"/>
    <xsl:param name="variableLongName"/>
    <xsl:param name="variableStandardName"/>
    <xsl:param name="variableType"/>
    <xsl:param name="variableUnits"/>
    <xsl:if test="nc:attribute[contains(@name,'flag_')]">
      <xsl:variable name="flag_masks_seq" select="tokenize(normalize-space(nc:attribute[@name='flag_masks']/@value),'\s')"/>
      <xsl:variable name="flag_values_seq" select="tokenize(normalize-space(nc:attribute[@name='flag_values']/@value),'\s')"/>
      <xsl:variable name="flag_names_seq" select="tokenize(normalize-space(nc:attribute[@name='flag_names']/@value),'\s')"/>
      <xsl:variable name="flag_meanings_seq" select="tokenize(normalize-space(nc:attribute[@name='flag_meanings']/@value),'\s')"/>
      <xsl:for-each select="$flag_values_seq">
        <gmi:rangeElementDescription>
          <gmi:MI_RangeElementDescription>
            <gmi:name>
              <gco:CharacterString>
                <xsl:value-of select="subsequence($flag_names_seq,position(),1)"/>
              </gco:CharacterString>
            </gmi:name>
            <gmi:definition>
              <gco:CharacterString>
                <xsl:value-of select="subsequence($flag_meanings_seq,position(),1)"/>
              </gco:CharacterString>
            </gmi:definition>
            <gmi:rangeElement>
              <gco:Record>
                <xsl:value-of select="translate(subsequence($flag_values_seq,position(),1),',','')"/>
              </gco:Record>
            </gmi:rangeElement>
          </gmi:MI_RangeElementDescription>
        </gmi:rangeElementDescription>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>
  <xsl:template name="writeService">
    <xsl:param name="serviceID"/>
    <xsl:param name="serviceTypeName"/>
    <xsl:param name="serviceOperationName"/>
    <xsl:param name="operationURL"/>
    <xsl:param name="operationNode"/>
    <gmd:identificationInfo>
      <xsl:element name="srv:SV_ServiceIdentification">
        <xsl:attribute name="id">
          <xsl:value-of select="$serviceID"/>
        </xsl:attribute>
        <gmd:citation>
          <gmd:CI_Citation>
            <gmd:title>
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="stringToWrite" select="$title[1]"/>
              </xsl:call-template>
            </gmd:title>
            <xsl:choose>
              <xsl:when test="$dateCnt">
                <xsl:if test="count($creatorDate)">
                  <xsl:call-template name="writeDate">
                    <xsl:with-param name="testValue" select="count($creatorDate)"/>
                    <xsl:with-param name="dateToWrite" select="$creatorDate[1]"/>
                    <xsl:with-param name="dateType" select="'creation'"/>
                  </xsl:call-template>
                </xsl:if>
                <xsl:if test="count($issuedDate)">
                  <xsl:call-template name="writeDate">
                    <xsl:with-param name="testValue" select="count($issuedDate)"/>
                    <xsl:with-param name="dateToWrite" select="$issuedDate[1]"/>
                    <xsl:with-param name="dateType" select="'issued'"/>
                  </xsl:call-template>
                </xsl:if>
                <xsl:if test="count($modifiedDate)">
                  <xsl:call-template name="writeDate">
                    <xsl:with-param name="testValue" select="count($modifiedDate)"/>
                    <xsl:with-param name="dateToWrite" select="substring($modifiedDate[1],1,10)"/>
                    <xsl:with-param name="dateType" select="'revision'"/>
                  </xsl:call-template>
                </xsl:if>
              </xsl:when>
              <xsl:otherwise>
                <gmd:date>
                  <xsl:attribute name="gco:nilReason">
                    <xsl:value-of select="'missing'"/>
                  </xsl:attribute>
                </gmd:date>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="$creatorTotal">
              <xsl:call-template name="writeResponsibleParty">
                <xsl:with-param name="tagName" select="'gmd:citedResponsibleParty'"/>
                <xsl:with-param name="testValue" select="$creatorTotal"/>
                <xsl:with-param name="individualName" select="$creatorName[1]"/>
                <xsl:with-param name="organisationName" select="$institution[1]"/>
                <xsl:with-param name="email" select="$creatorEmail[1]"/>
                <xsl:with-param name="url" select="$creatorURL[1]"/>
                <xsl:with-param name="roleCode" select="'originator'"/>
              </xsl:call-template>
            </xsl:if>
            <xsl:if test="$contributorTotal">
              <xsl:call-template name="writeResponsibleParty">
                <xsl:with-param name="tagName" select="'gmd:citedResponsibleParty'"/>
                <xsl:with-param name="testValue" select="$contributorTotal"/>
                <xsl:with-param name="individualName" select="$contributorName[1]"/>
                <xsl:with-param name="organisationName"/>
                <xsl:with-param name="email"/>
                <xsl:with-param name="url"/>
                <xsl:with-param name="roleCode" select="/nc:netcdf/nc:attribute[@name='contributor_role']/@value"/>
              </xsl:call-template>
            </xsl:if>
          </gmd:CI_Citation>
        </gmd:citation>
        <gmd:abstract>
          <xsl:choose>
            <xsl:when test="count(/nc:netcdf/nc:attribute[@name='summary']) > 0">
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="stringToWrite" select="/nc:netcdf/nc:attribute[@name='summary']/@value"/>
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="writeCharacterString">
                <xsl:with-param name="stringToWrite" select="/nc:netcdf/nc:group[@name='THREDDSMetadata']/nc:group[@name='documentation']/nc:group[@name='document']/nc:attribute[@type='summary']/@value"/>
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </gmd:abstract>
        <srv:serviceType>
          <gco:LocalName>
            <xsl:value-of select="$serviceTypeName"/>
          </gco:LocalName>
        </srv:serviceType>
        <srv:extent>
          <xsl:choose>
            <xsl:when test="$extentTotal">
              <gmd:EX_Extent>
                <xsl:if test="count($geospatial_lat_min) + count($geospatial_lon_min) + count($geospatial_lat_max) + count($geospatial_lon_max)">
                  <gmd:geographicElement>
                    <gmd:EX_GeographicBoundingBox>
                      <gmd:extentTypeCode>
                        <gco:Boolean>1</gco:Boolean>
                      </gmd:extentTypeCode>
                      <gmd:westBoundLongitude>
                        <gco:Decimal>
                          <xsl:value-of select="$geospatial_lon_min[1]"/>
                        </gco:Decimal>
                      </gmd:westBoundLongitude>
                      <gmd:eastBoundLongitude>
                        <gco:Decimal>
                          <xsl:value-of select="$geospatial_lon_max[1]"/>
                        </gco:Decimal>
                      </gmd:eastBoundLongitude>
                      <gmd:southBoundLatitude>
                        <gco:Decimal>
                          <xsl:value-of select="$geospatial_lat_min[1]"/>
                        </gco:Decimal>
                      </gmd:southBoundLatitude>
                      <gmd:northBoundLatitude>
                        <gco:Decimal>
                          <xsl:value-of select="$geospatial_lat_max[1]"/>
                        </gco:Decimal>
                      </gmd:northBoundLatitude>
                    </gmd:EX_GeographicBoundingBox>
                  </gmd:geographicElement>
                </xsl:if>
                <xsl:if test="count($timeStart) + count($timeEnd)">
                  <gmd:temporalElement>
                    <gmd:EX_TemporalExtent>
                      <gmd:extent>
                        <gml:TimePeriod gml:id="{generate-id($operationNode)}">
                          <gml:beginPosition>
                            <xsl:value-of select="$timeStart[1]"/>
                          </gml:beginPosition>
                          <gml:endPosition>
                            <xsl:value-of select="$timeEnd[1]"/>
                          </gml:endPosition>
                        </gml:TimePeriod>
                      </gmd:extent>
                    </gmd:EX_TemporalExtent>
                  </gmd:temporalElement>
                </xsl:if>
                <xsl:if test="count($verticalMin) + count($verticalMax)">
                  <gmd:verticalElement>
                    <gmd:EX_VerticalExtent>
                      <gmd:minimumValue>
                        <gco:Real>
                          <xsl:choose>
                            <xsl:when test="$verticalPositive[1] = 'down'">
                              <xsl:value-of select="$verticalMin[1] * -1"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="$verticalMin[1]"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        </gco:Real>
                      </gmd:minimumValue>
                      <gmd:maximumValue>
                        <gco:Real>
                          <xsl:choose>
                            <xsl:when test="$verticalPositive[1] = 'down'">
                              <xsl:value-of select="$verticalMax[1] * -1"/>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:value-of select="$verticalMax[1]"/>
                            </xsl:otherwise>
                          </xsl:choose>
                        </gco:Real>
                      </gmd:maximumValue>
                      <gmd:verticalCRS>
                        <xsl:attribute name="gco:nilReason">
                          <xsl:value-of select="'missing'"/>
                        </xsl:attribute>
                      </gmd:verticalCRS>
                    </gmd:EX_VerticalExtent>
                  </gmd:verticalElement>
                </xsl:if>
              </gmd:EX_Extent>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="gco:nilReason">
                <xsl:value-of select="'missing'"/>
              </xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
        </srv:extent>
        <srv:couplingType>
          <srv:SV_CouplingType codeList="http://www.tc211.org/ISO19139/resources/codeList.xml#SV_CouplingType" codeListValue="tight">tight</srv:SV_CouplingType>
        </srv:couplingType>
        <srv:containsOperations>
          <srv:SV_OperationMetadata>
            <srv:operationName>
              <gco:CharacterString>
                <xsl:value-of select="$serviceOperationName"/>
              </gco:CharacterString>
            </srv:operationName>
            <srv:DCP gco:nilReason="unknown"/>
            <srv:connectPoint>
              <gmd:CI_OnlineResource>
                <gmd:linkage>
                  <gmd:URL>
                    <xsl:value-of select="$operationURL"/>
                  </gmd:URL>
                </gmd:linkage>
                <gmd:name>
                  <gco:CharacterString>
                    <xsl:value-of select="$serviceID"/>
                  </gco:CharacterString>
                </gmd:name>
                <gmd:description>
                  <gco:CharacterString>
                    <xsl:value-of select="$serviceTypeName"/>
                  </gco:CharacterString>
                </gmd:description>
                <gmd:function>
                  <gmd:CI_OnLineFunctionCode codeList="http://www.ngdc.noaa.gov/metadata/published/xsd/schema/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode" codeListValue="download">download</gmd:CI_OnLineFunctionCode>
                </gmd:function>
              </gmd:CI_OnlineResource>
            </srv:connectPoint>
          </srv:SV_OperationMetadata>
        </srv:containsOperations>
        <srv:operatesOn xlink:href="#DataIdentification"/>
      </xsl:element>
    </gmd:identificationInfo>
  </xsl:template>
</xsl:stylesheet>
