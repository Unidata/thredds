<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="user.list.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
   <h1><spring:message code="global.title"/></h1>
   <h3><spring:message code="user.list.title"/></h3>

   <p><a href="${baseUrl}users/create">Create a New User</a></p>
   <table class="tablesorter">    
    <c:choose>
     <c:when test="${fn:length(users) gt 0}">
      <thead>
       <tr>
        <th colspan="3">
         Actions
        </th>
        <th>
         Full Name
        </th>
        <th>
         Email Address
        </th>
        <th>
         Affiliation
        </th>
        <th>
         Date Created
        </th>
        <th>
         Date Modified
        </th>
       </tr>
      </thead>
      <tbody>
       <c:forEach items="${users}" var="user">    
        <tr>
         <td>
          <form action="${baseUrl}<c:out value="${user.userId}" />" method="GET">
           <button type="submit" value="View User Tables">View User Tables</button>
          </form>
         </td>
         <td>
          <form action="${baseUrl}users/<c:out value="${user.userId}" />/update" method="GET">
           <button type="submit" value="Edit">Edit</button>
          </form>
         </td>
         <td>
          <form action="${baseUrl}users/delete" method="POST">
           <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
           <button type="submit" value="Delete">Delete</button>
          </form>
         </td>
         <td>
          <c:out value="${user.fullName}" />
         </td>
         <td>
          <c:out value="${user.emailAddress}" />
         </td>
         <td>
          <c:out value="${user.affiliation}" />
         </td>
         <td>
          <c:out value="${user.dateCreated}" />
         </td>
         <td>
          <c:out value="${user.dateModified}" />
         </td>
        </tr>
       </tbody>
      </c:forEach>
     </c:when>
     <c:otherwise>
      <tr>
       <td>
        <spring:message code="user.list.none"/>
       </td>
      </tr>
     </c:otherwise>
    </c:choose>
   </table> 
  </body>
 </html>
