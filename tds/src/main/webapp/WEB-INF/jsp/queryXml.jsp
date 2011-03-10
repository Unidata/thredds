<%@page contentType="text/xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         name="<c:out value="${name}"/>" 
         version="1.0.1">
  <service name="OPENDAP" serviceType="OPENDAP"
           base="<c:out value="${base}"/>"/>
    <dataset name="<c:out value="${dname}"/>"
          collectionType="TimeSeries"
          ID="<c:out value="${ID}"/>" >
    <metadata inherited="true">
      <dataType>Radial</dataType>
      <dataFormat><c:out value="${type}"/></dataFormat>
      <serviceName>OPENDAP</serviceName>
      <documentation><c:out value="${documentation}"/></documentation>
    </metadata>
    <c:forEach var="dataset" items="${datasets}">
    <dataset name="<c:out value="${dataset.name}"/>"
         ID="<c:out value="${dataset.ID}"/>"
         urlPath="<c:out value="${dataset.urlPath}"/>" >
         <date type="start of ob"><c:out value="${dataset.date}"/></date>
    </dataset>
    </c:forEach>

    </dataset>
</catalog>

