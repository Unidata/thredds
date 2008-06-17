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
      <!-- ToDo If curDs instanceof InvCatalogRef
      <!-- If ! (curDs instanceof InvCatalogRef)
      <c:set var="curDsParam" value="${curDs}" scope="request"/>
      <c:set var="curLevelParam" value="0" scope="request"/>
      <c:import url="dataset.jsp" />
      <c:remove var="curDsParam" scope="request"/>
      <c:remove var="curLevelParam" scope="request"/>
    </c:forEach>
  </table>
  <hr size="1" noshade="noshade">
  <h3>
    ${webappName} [Version ${webappVersion}]
    <a href="${docsPath}">Documentation</a>
  </h3>
</body>
</html>