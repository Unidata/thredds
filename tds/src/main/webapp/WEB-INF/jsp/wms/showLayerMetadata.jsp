<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Displays the metadata that can't be placed inline in a Capabilities document.
     This file is linked from the Capabilities document via MetadataUrl
     Data (models) passed in to this page:
         layers     = Collection<uk.ac.rdg.resc.ncwms.wms.Layer> whose metadata we wish to display
     TODO: create a schema for this?
     --%>
<LayerMetadata>
    <c:forEach var="layer" items="${layers}">
    <Layer name="${layer.name}">
        <units>${layer.units}</units>
        <defaultStyle>
            <defaultColorScaleRange>${layer.approxValueRange.minimum} ${layer.approxValueRange.maximum}</defaultColorScaleRange>
            <defaultPaletteName>${layer.defaultColorPalette.name}</defaultPaletteName>
            <logScaling>${layer.logScaling}</logScaling>
        </defaultStyle>
    </Layer>
    </c:forEach>
</LayerMetadata>