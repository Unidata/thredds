<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <c:choose>
     <c:when test="${formAction == 'update'}">
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

   <c:choose>
    <c:when test="${error != null}">
     <p class="error"><b><c:out value="${error}" /></b></p>
    </c:when>
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
        <spring:message code="user.userName"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="user.userName.description"/>"/>
       </th>
       <th>
        <spring:message code="user.fullName"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="user.fullName.description"/>"/>
       </th>
       <th>
        <spring:message code="user.emailAddress"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="user.emailAddress.description"/>"/>
       </th>
       <th>
        <spring:message code="user.center"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="user.center.description"/>"/>
       </th>
       <th>
        <spring:message code="user.subCenter"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="user.subCenter.description"/>"/>
       </th>
       <c:choose>
        <c:when test="${user.dateCreated != null}">
         <th>
          <spring:message code="user.dateCreated"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="user.dateCreated.description"/>"/>
         </th>
        </c:when>
       </c:choose>
       <c:choose>
        <c:when test="${loggedIn}">
         <c:choose>
          <c:when test="${user.userName eq authUserName}">
           <th>
            <spring:message code="form.action.title"/>
           </th>
          </c:when>
          <c:otherwise>
           <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th>
             <spring:message code="form.action.title"/>
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
         <c:when test="${formAction == 'update'}">
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
        <form:errors path="center" cssClass="error" />
        <form:input path="center"/>
       </td>
       <td>
        <form:errors path="subCenter" cssClass="error" />
        <form:input path="subCenter"/>
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
