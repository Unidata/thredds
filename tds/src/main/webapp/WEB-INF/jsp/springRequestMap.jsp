<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Spring RequestMaps</title>
</head>
<body>

  <h1>Spring MVC RequestMaps</h1>
  <pre>
  <c:forEach items="${handlerMethods}" var="entry">
    ${entry.key.patternsCondition.patterns}: ${entry.value.method.declaringClass.name}.${entry.value.method.name}()</c:forEach>
  </pre>

  <c:forEach items="${handlerMethods}" var="entry">
      <p><strong>${entry.value.method.declaringClass.name}.${entry.value.method.name}()</strong>: ${entry.key.patternsCondition.patterns}</p>
  </c:forEach>
  <hr/>


</body>
</html>