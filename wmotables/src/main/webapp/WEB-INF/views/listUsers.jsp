<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="user.list.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
   <h3><spring:message code="user.list.title"/></h3>
   <p><spring:message code="user.list.message"/></p>


   <c:choose>
    <c:when test="${error != null}">
     <p class="error"><b><c:out value="${error}" /></b></p>
    </c:when>
   </c:choose>

   <table class="list"> 
    <c:choose>
     <c:when test="${fn:length(users) gt 0}">
      <thead>
       <tr>
        <th>
         <spring:message code="user.userName"/>
        </th>
        <th>
         <spring:message code="user.fullName"/>
        </th>
        <th>
         <spring:message code="user.emailAddress"/>
        </th>
        <th>
         <spring:message code="user.center"/>
        </th>  
        <th>
         <spring:message code="user.subCenter"/>
        </th>       
        <th>
         <spring:message code="user.dateCreated"/> 
        </th>
       </tr>
      </thead>
      <tbody>
       <c:forEach items="${users}" var="user">    
        <tr>
         <td>
          <a href="${baseUrl}/user/<c:out value="${user.userName}" />">
           <c:out value="${user.userName}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/user/<c:out value="${user.userName}" />">
          <c:out value="${user.fullName}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/user/<c:out value="${user.userName}" />">
          <c:out value="${user.emailAddress}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/user/<c:out value="${user.userName}" />">
          <c:out value="${user.center}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/user/<c:out value="${user.userName}" />">
          <c:out value="${user.subCenter}" />
          </a>
         </td>
         <td>
          <fmt:formatDate value="${user.dateCreated}" type="BOTH" dateStyle="default"/>
         </td>
        </tr>
       </c:forEach>
      </tbody>
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
