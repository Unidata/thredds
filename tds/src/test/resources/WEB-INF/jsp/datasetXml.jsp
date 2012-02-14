<%@page contentType="text/xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         name="Radar Data"
         version="1.0.1">
    
  <service name="radarServer" base="/thredds/radarServer/" serviceType="DQC" />

  <dataset ID="<c:out value="${ID}"/>"  >
    <urlpath><c:out value="${urlPath}"/></urlpath>
    <dataType>RADIAL</dataType>
    <dataFormat><c:out value="${dataFormat}"/></dataFormat>
    <serviceName>radarServer</serviceName>
    <metadata inherited="true">

      <documentation type="summary"><c:out value="${documentation}"/></documentation>

      <TimeSpan>
        <start><c:out value="${tstart}"/></start>
        <end><c:out value="${tend}"/></end>
      </TimeSpan>
      <LatLonBox>
        <north><c:out value="${north}"/></north>
        <south><c:out value="${south}"/></south>
        <east><c:out value="${east}"/></east>
        <west><c:out value="${west}"/></west>
      </LatLonBox>
      <Variables>
        <c:forEach var="variable" items="${variables}">
        <variable name="<c:out value="${variable.name}"/>"
            vocabulary_name="<c:out value="${variable.vname}"/>"
            units="<c:out value="${variable.units}"/>"/>
        </c:forEach>
      </Variables>

    </metadata>

  </dataset>
</catalog>
