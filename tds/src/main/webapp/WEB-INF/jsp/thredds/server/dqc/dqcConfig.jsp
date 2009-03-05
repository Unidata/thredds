<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="${standardCssUrl}"/>" type="text/css"/>
  <title>DQC Service - Configuration</title>
</head>

<body>
<c:import url="/WEB-INF/jsp/siteHeader.jsp" />

<h1>DQC Service - Configuration</h1>
<h2>Available Datasets</h2>
<table border="1">
<tbody>
  <tr>
    <th> Name</th>
    <th> Description</th>
    <th> DQC Document</th>
  </tr>

  <c:forEach var="curItem" items="${dqcConfigItems}">
    <tr>
      <td>${curItem.name}</td>
      <td>${curItem.description}</td>
      <td><a href="${contextPath}${servletPath}/${curItem.name}.xml">${curItem.name}</a></td>
    </tr>
  </c:forEach>

</tbody>
</table>

<p>
This listing is also available as a
  <a href="${contextPath}${servletPath}/catalog.xml">THREDDS catalog</a>.
</p>
<c:import url="/WEB-INF/jsp/webappFooter.jsp" />


</body>
</html>