<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="user.list.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
<%@ include file="/WEB-INF/views/include/nav.jsp" %>
   <h3><spring:message code="user.list.title"/></h3>

   <p><a href="${baseUrl}/user/create">Create new user</a></p>

   <table> 
    <c:choose>
     <c:when test="${fn:length(users) gt 0}">
      <thead>
       <tr>
        <th>
         Action
        </th>
        <th>
         User Name
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
       </tr>
      </thead>
      <tbody>
       <c:forEach items="${users}" var="user">    
        <tr>
         <td>
          <form id="FORM" action="${baseUrl}/user/<c:out value="${user.userName}" />" method="GET">
           <input type="submit" value="<spring:message code="user.view.title"/>" />        
          </form>
         </td>
         <td>
          <c:out value="${user.userName}" />
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
          <fmt:formatDate value="${user.dateCreated}" type="BOTH" dateStyle="default"/>
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
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
