<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>

<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <spring:message code="table.update.title"/>
   </title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
   <h1><spring:message code="global.title"/></h1>

   <h3><spring:message code="table.update.title"/></h3>

   <p>Note: All fields are required</p>

   <form id="FORM" action="${baseUrl}update" method="POST" enctype="multipart/form-data">
    <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
    <input type="hidden" name="tableId" value="<c:out value="${table.tableId}" />"/>
    <ul>   
     <li>
      <label>
       Hide table
       <c:choose>
        <c:when test="${table.visibility > 0}">
         <input type="checkbox" name="visibility" value="0"/>
         <input type="hidden" name="visibility" value="1"/>
        </c:when>
        <c:otherwise>
         <input type="checkbox" name="visibility" value="1" checked/>
         <input type="hidden" name="visibility" value="0"/>
        </c:otherwise>
       </c:choose>
      </label>
     </li>
     <li>
      <label>
       Title<br />
       <input type="text" name="title" value="<c:out value="${table.title}" />"/>
      </label>
     </li>
     <li>
      <label>
       Description<br />
       <input type="text" name="description" value="<c:out value="${table.description}" />"/>
      </label>
     </li>
     <li>
      <label>
       Original Name<br />
       <input type="text" value="<c:out value="${table.originalName}" />" disabled="disabled"/>
      </label>
     </li>
     <li>
      <label>
       Version<br />
       <input type="text" name="version" value="<c:out value="${table.version}" />"/>
      </label>
     </li>
     <li>
      <label>
       MD5 Checksum<br />
       <input type="text" value="<c:out value="${table.md5}" />" disabled="disabled"/>
      </label>
     </li>
     <li>
      <input type="submit" value="update table" />
     </li> 
    </ul>
   
   </form>
  </body>
 </html>
