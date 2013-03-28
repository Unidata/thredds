<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="table.list.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
<%@ include file="/WEB-INF/views/include/nav.jsp" %>
   <h3><spring:message code="table.list.title"/></h3>

   <table> 
    <c:choose>
     <c:when test="${fn:length(tables) gt 0}">
      <thead>
       <tr>
        <th>
         Action
        </th>
        <th>
         Title
        </th>
        <th>
         Description
        </th>
        <th>
         Table Type
        </th>
        <th>
         Checksum
        </th>
        <th>
         Owner
        </th>
        <th>
         Date Created
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
          <form id="FORM" action="${baseUrl}/table/<c:out value="${table.checksum}" />" method="GET">
           <input type="submit" value="<spring:message code="table.view.title"/>" />        
          </form>
         </td>
         <td>
          <c:out value="${table.title}" />
         </td>
         <td>
          <c:out value="${table.description}" />
         </td>
         <td>
          <c:out value="${table.tableType}" />
         </td>
         <td>
          <c:out value="${table.checksum}" />
         </td>
         <td>
          <c:forEach items="${users}" var="entry">
           <c:choose>
            <c:when test="${entry.key == table.userId}">
             <c:out value="${entry.value.fullName}" />
            </c:when>
           </c:choose>
          </c:forEach>
         </td>
         <td>
          <fmt:formatDate value="${table.dateCreated}" type="BOTH" dateStyle="default"/>
         </td>
        </tr>
       </tbody>
      </c:forEach>
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






