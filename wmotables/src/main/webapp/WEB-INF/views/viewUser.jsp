<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="user.view.title"/>: <c:out value="${user.userName}" /></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
<%@ include file="/WEB-INF/views/include/nav.jsp" %>
   <h3><spring:message code="user.view.title"/>: <c:out value="${user.userName}" /></h3>
   <table class="tablesorter">    
    <thead>
     <tr>
      <th colspan="2">
       Actions
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
     <tr>
      <td>
       <form id="FORM" action="${baseUrl}/user/<c:out value="${user.userName}" />/update" method="GET">
        <input type="submit" value="<spring:message code="user.update.title"/>" />        
       </form>
      </td>
      <td>
       <form id="FORM" action="${baseUrl}/user/delete" method="POST">
        <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
        <input type="submit" value="<spring:message code="user.delete.title"/>" disabled/>        
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
   </table> 
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
