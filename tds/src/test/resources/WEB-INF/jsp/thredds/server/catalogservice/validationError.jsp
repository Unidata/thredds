<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="${standardCssUrl}"/>" type="text/css"/>
  <title>Catalog Service Request - Catalog Has Fatal Error</title>
</head>

<body>
<c:import url="/WEB-INF/jsp/siteHeader.jsp" />
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

<c:import url="/WEB-INF/jsp/webappFooter.jsp" />

</body>
</html>