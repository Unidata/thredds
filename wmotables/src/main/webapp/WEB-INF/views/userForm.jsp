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
    <c:when test="${user != null}">
     <h3><spring:message code="user.update.title"/>:
     <c:out value="${user.userName}" /></h3>
     <p><spring:message code="user.update.message"/></p>
    </c:when>
    <c:otherwise>
     <h3><spring:message code="user.create.title"/></h3>
     <p><spring:message code="user.create.message"/></p>
    </c:otherwise>
   </c:choose>
   </h3>
   <form action="${baseUrl}/user/<c:out value="${formAction}" />" method="POST">
    <c:choose>
     <c:when test="${user != null}">
      <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
      <input type="hidden" name="userName" value="<c:out value="${user.userName}" />"/>
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
        <c:when test="${user != null}">
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
            Actions
           </th>
          </c:when>
          <c:otherwise>
           <sec:authorize access="hasRole('ROLE_ADMIN')">
            <th>
             Actions
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
         <c:when test="${user != null}">
          <c:out value="${user.userName}" />
         </c:when>
         <c:otherwise>
           <input type="text" name="userName" value="" />
         </c:otherwise>
        </c:choose>        
       </td>
       <td>
        <input type="text" 
              name="fullName" 
              <c:choose>
               <c:when test="${user != null}">
                value="<c:out value="${user.fullName}" />"
               </c:when>
               <c:otherwise>
                 value=""
               </c:otherwise>
              </c:choose>
         />
       </td>
       <td>
        <input type="text" 
              name="emailAddress" 
              <c:choose>
               <c:when test="${user != null}">
                value="<c:out value="${user.emailAddress}" />"
               </c:when>
               <c:otherwise>
                 value=""
               </c:otherwise>
              </c:choose>
         />
       </td>
       <td>
        <input type="text" 
              name="affiliation" 
              <c:choose>
               <c:when test="${user != null}">
                value="<c:out value="${user.affiliation}" />"
               </c:when>
               <c:otherwise>
                 value=""
               </c:otherwise>
              </c:choose>
         />
       </td>
       <c:choose>
        <c:when test="${user != null}">
         <td>
          <fmt:formatDate value="${user.dateCreated}" type="BOTH" dateStyle="default"/>
         </td>
        </c:when>
       </c:choose>
       <td>
        <input type="submit" value="<c:out value="${formAction}" />" />
       </td>
      </tr>
     </tbody>
    </table> 
   </form>
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
