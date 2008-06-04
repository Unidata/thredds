<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="/tdsCat.css"/>" type="text/css"/>
  <title>${catalog.name}</title>
</head>

<body>
  <h1>Catalog ${catalog.name}</h1>
  <hr size="1" noshade="noshade">
  <table width="95%" cellspacing="0" cellpadding="5" align="center">
    <tr>
      <th align="left">Dataset</th>
      <th align="center">Size</th>
      <th align="right">Last Modified</th>
    </tr>
    <c:forEach var="curDs" items="${catalog.datasets}">
      <c:import url="dataset.jsp">
        <c:param name="dataset" value="curDs" />
      </c:import>
    </c:forEach>
  </table>
  <hr size="1" noshade="noshade">
  <h3>
    ${webappName}[${webappName}]: ${webappVersion}
    <a href="${docsPath}">Documentation</a>
  </h3>
</body>
</html>