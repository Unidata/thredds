<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>

<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <spring:message code="table.create.title"/>
   </title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
   <h1><spring:message code="global.title"/></h1>

   <h3><spring:message code="table.create.title"/></h3>

   <p>Note: All fields are required</p>

   <form id="FORM" action="${baseUrl}create" method="POST" enctype="multipart/form-data">
    <c:choose>
     <c:when test="${user != null}">
      <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
     </c:when>
    </c:choose>
    <ul>   
     <li>
      <label>
       Upload your table:<br />
       <input type="file" name="file" value=""/>
      </label>
     </li>
     <li>
      <label>
       Title<br />
       <input type="text" name="title" value=""/>
      </label>
     </li>
     <li>
      <label>
       Description<br />
       <input type="text" name="description" value=""/>
      </label>
     </li>
     <li>
      <label>
       Version<br />
       <input type="text" name="version" value=""/>
      </label>
     </li>
     <li>
      <input type="submit" value="create new table" />
     </li> 
    </ul>
   </form>
  </body>
 </html>
