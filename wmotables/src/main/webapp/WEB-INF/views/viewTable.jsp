<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="table.view.title"/>: <c:out value="${table.checksum}" /></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
<%@ include file="/WEB-INF/views/include/nav.jsp" %>
   <h3><spring:message code="table.view.title"/>: <c:out value="${table.checksum}" /></h3>
   <table> 
    <thead>
     <tr>
      <th colspan="2">
       Actions
      </th>
      <th>
       Title
      </th>
      <th>
       Description
      </th>
      <th>
       Original Name
      </th>
      <th>
       Version
      </th>
      <th>
       Table Type
      </th>
      <th>
       Mime Type
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
      <th>
       View Table
      </th>
     </tr>
    </thead>
    <tbody>   
     <tr
      <c:choose>
       <c:when test="${table.visibility < 1}">
         class="hidden"
       </c:when>
      </c:choose>
     >
      <td>
       <form id="FORM" action="${baseUrl}/table/update/<c:out value="${table.checksum}" />" method="GET">
        <input type="submit" value="<spring:message code="table.update.title"/>" />        
       </form>
      </td>
      <td>
       <form id="FORM" action="${baseUrl}/table/hide" method="POST">
        <input type="hidden" name="tableId" value="<c:out value="${table.tableId}" />"/>
        <input type="hidden" name="visibility" value="<c:out value="${table.visibility}" />"/>
        <input type="submit" 
         <c:choose>
          <c:when test="${table.visibility == 0}">
            value="<spring:message code="table.unhide.title"/>"
          </c:when>
          <c:otherwise>
            value="<spring:message code="table.hide.title"/>"
          </c:otherwise>
         </c:choose>
         />
       </form>
      </td>
      <td>
       <c:out value="${table.title}" />
      </td>
      <td>
       <c:out value="${table.description}" />
      </td>
      <td>
       <c:out value="${table.originalName}" />
      </td>
      <td>
       <c:out value="${table.version}" />
      </td>
      <td>
       <c:out value="${table.tableType}" />
      </td>
      <td>
       <c:out value="${table.mimeType}" />
      </td>
      <td>
       <c:out value="${table.checksum}" />
      </td>
      <td>
       <c:out value="${user.userName}" />
      </td>
      <td>
       <fmt:formatDate value="${table.dateCreated}" type="BOTH" dateStyle="default"/>
      </td>
      <td>
       <a href="${baseUrl}/table/download/<c:out value="${table.checksum}" />">
        <img src="${baseUrl}/resources/img/view.png" alt="View Table">
       </a>
      </td>
     </tr>
    </tbody>
   </table> 
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>






