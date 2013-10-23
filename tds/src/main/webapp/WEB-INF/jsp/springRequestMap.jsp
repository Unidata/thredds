<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>Spring RequestMaps</title>
</head>
<body>

<div class="container">
  <div class="container">
    <h1>Spring MVC RequestMaps</h1>
    <c:forEach items="${handlerMethods}" var="entry">
      <div>
        <hr>
        <p><strong>${entry.value}</strong></p>
      </div>
      <div class="span-3 colborder">
        <p>
          <span class="alt">Patterns:</span><br>
          <c:if test="${not empty entry.key.patternsCondition.patterns}">
            ${entry.key.patternsCondition.patterns}
          </c:if>
        </p>
      </div>
    </c:forEach>
  </div>
</div>

</body>
</html>