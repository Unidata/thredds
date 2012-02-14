<%@page contentType="text/xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<stationsList>
    <c:forEach var="stn" items="${stations}">
    <station id="<c:out value="${stn.id}"/>"
             state="<c:out value="${stn.state}"/>"
             country="<c:out value="${stn.country}"/>">
      <name><c:out value="${stn.name}"/></name>
      <longitude><c:out value="${stn.longitude}"/></longitude>
      <latitude><c:out value="${stn.latitude}"/></latitude>
      <elevation><c:out value="${stn.elevation}"/></elevation>
    </station>
    </c:forEach>

</stationsList>