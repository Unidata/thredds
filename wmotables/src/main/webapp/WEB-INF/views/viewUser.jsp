<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="user.view.title"/>: <c:out value="${user.userName}" /></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
   <h3><spring:message code="user.view.title"/>: <c:out value="${user.userName}" /></h3>
   <p><spring:message code="user.view.message"/></p>

   <c:choose>
    <c:when test="${error != null}">
     <p class="error"><b><c:out value="${error}" /></b></p>
    </c:when>
   </c:choose>


   <table>    
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
       <c:out value="${user.userName}" />
      </td>
      <td>
       <c:out value="${user.fullName}" />
      </td>
      <td>
       <c:out value="${user.emailAddress}" />
      </td>
      <td>
       <c:out value="${user.center}" />
      </td>  
      <td>
       <c:out value="${user.subCenter}" />
      </td>   
      <td>
       <fmt:formatDate value="${user.dateCreated}" type="BOTH" dateStyle="default"/>
      </td>
      <c:choose>
       <c:when test="${loggedIn}">
        <c:choose>
         <c:when test="${user.userName eq authUserName}">
          <td>
           <form action="${baseUrl}/user/update/<c:out value="${user.userName}" />" method="GET">
            <input class="action edit" type="submit" value="<spring:message code="user.update.title"/>" />        
           </form>
           <sec:authorize access="hasRole('ROLE_ADMIN')">
            <form action="${baseUrl}/user/delete" method="POST">
             <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
             <input class="action delete" type="submit" value="<spring:message code="user.delete.title"/>"/>        
            </form>
           </sec:authorize>
          </td>
         </c:when>
         <c:otherwise>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
           <td>
            <form action="${baseUrl}/user/update/<c:out value="${user.userName}" />" method="GET">
             <input class="action edit" type="submit" value="<spring:message code="user.update.title"/>" />        
            </form>
            <form action="${baseUrl}/user/delete" method="POST">
             <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
             <input class="action delete" type="submit" value="<spring:message code="user.delete.title"/>"/>        
            </form>
          </sec:authorize>
         </c:otherwise>
        </c:choose>
       </c:when>
      </c:choose>
     </tr>
    </tbody>
   </table> 

   <c:choose>
    <c:when test="${loggedIn}">
     <c:choose>
      <c:when test="${user.userName eq authUserName}">
       <p><a href="${baseUrl}/table/create/<c:out value="${user.userName}" />">Create new table</a></p>
      </c:when>
     </c:choose>
    </c:when>
   </c:choose> 
  
   <h3><spring:message code="user.view.table.title"/>: <c:out value="${user.userName}" /></h3>
   <table class="list"> 
    <c:choose>
     <c:when test="${fn:length(tables) gt 0}">
      <thead>
       <tr>
        <th>
         <spring:message code="table.title"/>
        </th>
        <th>
         <spring:message code="table.description"/>
        </th>
        <th>
         <spring:message code="table.tableType"/>
        </th>
        <th>
         <spring:message code="table.localVersion"/>
        </th>
        <th>
         <spring:message code="table.center"/>
        </th>
        <th>
         <spring:message code="table.subCenter"/>
        </th>
        <th>
         <spring:message code="table.checksum"/>
        </th>
        <th>
         <spring:message code="table.dateCreated"/> 
        </th>
       </tr>
      </thead>
      <tbody>
       <c:forEach items="${tables}" var="table">    
       <tr
        <c:choose>
         <c:when test="${table.visibility < 1}">
           class="hidden"
         </c:when>
        </c:choose>
       >
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <c:out value="${table.title}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <c:out value="${table.description}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <c:out value="${table.tableType}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <c:out value="${table.localVersion}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <c:out value="${table.center}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <c:out value="${table.subCenter}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <c:out value="${table.checksum}" />
          </a>
         </td>
         <td>
          <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
           <fmt:formatDate value="${table.dateCreated}" type="BOTH" dateStyle="default"/></a>
         </td>
        </tr>
       </c:forEach>
      </tbody>
     </c:when>
     <c:otherwise>
      <tr>
       <td>
        <spring:message code="table.list.none"/>
       </td>
      </tr>
     </c:otherwise>
    </c:choose>
   </table> 

<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
