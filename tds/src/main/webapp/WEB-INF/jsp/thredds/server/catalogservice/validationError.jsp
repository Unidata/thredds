<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="/tds.css"/>" type="text/css"/>
  <title>Catalog Service Request - Catalog Has Fatal Error</title>
</head>

<body>
<%--<c:set var="siteLogoPath" value="${catalog.datasets}" scope="request"/>--%>
<%--<c:set var="siteLogoAlt" value="" scope="request"/>--%>
<c:import url="/siteHeader.jsp" />
<%--<c:remove var="curDsListParam" scope="request"/>--%>
<%--<c:remove var="curLevelParam" scope="request"/>--%>
<hr>
<h2>Catalog Service Request - Catalog Has Fatal Error</h2>

<p>Catalog [${catalogUrl}] has fatal errors:</p>

<pre style="margin-left: 40px;">
  ${message}
</pre>
<hr>
<%--<pre>--%>
  <%--<c:choose>--%>
    <%--<c:when test="${catalogAsString != null}">--%>
     <%--${catalogAsString}--%>
    <%--</c:when>--%>
    <%--<c:otherwise>--%>
      <%--Error reading URL [${catalogUrl}]: ${catalogReadError}--%>
    <%--</c:otherwise>--%>
  <%--</c:choose>--%>
<%--</pre>--%>
<%--<hr>--%>

</body>
</html>