<%@ page session="false"%>
<%@ page contentType="text/html; charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>

  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Edit Task</title>
  </head>
  <body>
   <h1>Edit Task</h1>
   <ul>
   <c:forEach items="${rants}" var="rant">
     <li><c:out value="${rant}"/>
    </li>
   </c:forEach>
   </ul>
  </body>
</html>