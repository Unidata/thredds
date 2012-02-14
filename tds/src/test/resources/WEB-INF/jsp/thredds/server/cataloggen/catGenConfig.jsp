<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="${standardCssUrl}"/>" type="text/css"/>
  <title>CatalogGen Service - Configuration</title>
</head>

<body>
<c:import url="/WEB-INF/jsp/siteHeader.jsp" />

<h1>CatalogGen Service - Configuration</h1>

<table border="1">
<tbody>
  <tr>
    <th> Name</th>
    <th> Config Document Name</th>
    <th> Resulting Catalog File Name</th>
    <th> Period (minutes)</th>
    <th> Inital Delay (minutes)</th>
  </tr>

  <c:forEach var="curItem" items="${catGenConfig.taskInfoList}">
    <c:choose>
      <c:when test="${fn:startsWith(curItem.resultFileName, '/')}">
        <c:url var="curResultUrl" value="${curItem.resultFileName}" />
      </c:when>
      <c:otherwise>
        <c:url var="curResultUrl" value="/${catGenResultsDirName}/${curItem.resultFileName}" />
      </c:otherwise>
    </c:choose>

    <tr>
      <td>${curItem.name}</td>
      <td>${curItem.configDocName}</td>
      <td><a href="<c:out value='${curResultUrl}' />">${curItem.resultFileName}</a></td>
      <td>${curItem.periodInMinutes}</td>
      <td>${curItem.delayInMinutes}</td>
    </tr>
  </c:forEach>
</tbody>
</table>

<c:import url="/WEB-INF/jsp/webappFooter.jsp" />

</body>
</html>