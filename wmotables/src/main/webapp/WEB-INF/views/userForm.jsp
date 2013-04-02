<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <c:choose>
     <c:when test="${user != null}">
      <spring:message code="user.update.title"/>:
      <c:out value="${user.userName}" />
     </c:when>
     <c:otherwise>
      <spring:message code="user.create.title"/>
     </c:otherwise>
    </c:choose>
   </title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>

   <c:choose>
    <c:when test="${formAction == 'update'}">
     <h3><spring:message code="user.update.title"/>:
     <c:out value="${user.userName}" /></h3>
     <p><spring:message code="user.update.message"/></p>
    </c:when>
    <c:otherwise>
     <h3><spring:message code="user.create.title"/></h3>
     <p><spring:message code="user.create.message"/></p>
    </c:otherwise>
   </c:choose>

   <form:form action="${baseUrl}/user/${formAction}" commandName="user" method="POST">
    <c:choose>
     <c:when test="${formAction == 'update'}">
      <form:hidden path="userId" />
     </c:when>
    </c:choose>

    <table>    
     <thead>
      <tr>
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
       <c:choose>
        <c:when test="${user.dateCreated != null}">
         <th>
          Date Created
         </th>
        </c:when>
       </c:choose>
       <c:choose>
        <c:when test="${loggedIn}">
         <c:choose>
          <c:when test="${user.userName eq authUserName}">
           <th>
            Action
           </th>
          </c:when>
          <c:otherwise>
           <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th>
             Action
            </th>
           </sec:authorize>
          </c:otherwise>
         </c:choose>
        </c:when>
       </c:choose> 
      </tr>
     </thead>


     <tbody> 
      <tr>
       <td>
        <c:choose>
         <c:when test="${formAction == update}">
          <c:out value="${user.userName}" />
          <form:hidden path="userName" />
         </c:when>
         <c:otherwise>
           <form:errors path="userName" cssClass="error" />
            <c:out value="${status.userName[0]}"/>
           <form:input path="userName" />
         </c:otherwise>
        </c:choose>        
       </td>
       <td>
        <form:errors path="fullName" cssClass="error" />
        <form:input path="fullName"/>
       </td>
       <td>
        <form:errors path="emailAddress" cssClass="error" />
        <form:input path="emailAddress"/>
       </td>
       <td>
        <form:errors path="affiliation" cssClass="error" />
        <form:input path="affiliation"/>
       </td>
       <c:choose>
        <c:when test="${user.dateCreated != null}">
         <td>
          <fmt:formatDate value="${user.dateCreated}" type="BOTH" dateStyle="default"/>
         </td>
        </c:when>
       </c:choose>
       <td>
        <input type="submit" value="${formAction}" />
       </td>
      </tr>
     </tbody>
    </table> 
   </form:form>
   <c:choose>
    <c:when test="${loggedIn}">
     <c:choose>
      <c:when test="${user.userName eq authUserName}">
       <p><a href="${baseUrl}/table/create/<c:out value="${user.userName}" />">Create new table</a></p>
      </c:when>
     </c:choose>
    </c:when>
   </c:choose>   

<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
