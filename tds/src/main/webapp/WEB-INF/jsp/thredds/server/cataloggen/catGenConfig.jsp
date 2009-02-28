<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="/tds.css"/>" type="text/css"/>
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
    <tr>
      <td>${curItem.name}</td>
      <td><a href="${contextPath}${servletPath}/${curItem.configDocName}">${curItem.configDocName}</a></td>
      <td><a href="${contextPath}${servletPath}/${curItem.resultFileName}">${curItem.resultFileName}</a></td>
      <td>${curItem.periodInMinutes}</td>
      <td>${curItem.delayInMinutes}</td>
    </tr>
  </c:forEach>

</tbody>
</table>

<c:import url="/WEB-INF/jsp/webappFooter.jsp" />


</body>
</html>