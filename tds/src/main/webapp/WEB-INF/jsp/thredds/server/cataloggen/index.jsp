<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="${standardCssUrl}"/>" type="text/css"/>
  <title>CatalogGen Server Configuration</title>
</head>

<body>
  <h1>CatalogGen Server Configuration</h1>
  <hr>
  <h2>Currently Scheduled Tasks</h2>

    <table border="1">
      <tr>
        <th>Task Name</th>
        <th>Config Filename</th>
        <th>Results Filename</th>
        <th>Period (minutes)</th>
        <th>Initial Delay (minutes)</th>
        <th>Edit/Delte Task</th>
      </tr>
      <c:forEach var="curTask" items="${config.taskConfigList}">
        <tr>
          <td>${curTask.name}</td>
          <td><a href="./${curTask.configDocName}">${curTask.configDocName}</a></td>
          <td>${curTask.resultFileName}</td>
          <td>${curTask.periodInMinutes}</td>
          <td>${curTask.delayInMinutes}</td>
          <td>[Edit][Delete]</td>
        </tr>
      </c:forEach>
    </table>




</body>
</html>