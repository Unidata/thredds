<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>: 
    <c:choose>
     <c:when test="${user != null}">
      <spring:message code="table.list.title.owner"/>
       <c:out value="${user.fullName}" />
     </c:when>
     <c:otherwise>
      <spring:message code="table.list.title"/>
     </c:otherwise>
    </c:choose>
   </title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
   <h1><spring:message code="global.title"/></h1>

   <c:choose>
    <c:when test="${user != null}">
     <h3>
      <spring:message code="table.list.title.owner"/>
      <a href="mailto:<c:out value="${user.emailAddress}" />"><c:out value="${user.fullName}" /></a>, <c:out value="${user.affiliation}" />
     </h3>
     <p><a href="${baseUrl}<c:out value="${user.userId}" />/create"><spring:message code="table.create.title"/></a></p>
     <p><a href="${baseUrl}"><spring:message code="table.list.title"/></a></p>
     <p><a href="${baseUrl}users"><spring:message code="user.list.title"/></a></p>
     </c:when>
     <c:otherwise>
      <h3><spring:message code="table.list.title"/></h3>
     </c:otherwise>
    </c:choose>


   <table class="tablesorter">   
    <c:choose>
     <c:when test="${fn:length(tables) gt 0}">
      <thead>
       <tr>
        <th colspan="2">
         Actions
        </th>
        <th>
         Owner
        </th>
        <th>
         Title
        </th>
        <th>
         Description
        </th>
        <th>
         MD5 Checksum
        </th>
        <th>
         Original Name
        </th>
        <th>
         Version
        </th>
        <th>
         Date Created
        </th>
        <th>
         View File
        </th>
       </tr>
      </thead>
      <tbody>
       <c:forEach items="${tables}" var="table">
        <c:choose>
         <c:when test="${table.visibility > 0}">
          <tr>
           <td>
            <form action="${baseUrl}<c:out value="${table.userId}" />/<c:out value="${table.tableId}" />/update" method="GET">
             <button type="submit" value="update">update</button>
            </form>
           </td>
           <td>
            <form action="${baseUrl}hide" method="POST">
             <input type="hidden" name="tableId" value="<c:out value="${table.tableId}" />"/>
             <input type="hidden" name="userId" value="<c:out value="${table.userId}" />"/>
             <input type="hidden" name="visibility" value="0"/>
             <button type="submit" value="hide">hide</button>
            </form>
            </a>
           </td>
           <td>
            <c:forEach items="${users}" var="entry">
             <c:choose>
              <c:when test="${entry.key == table.userId}">
               <a href="${baseUrl}<c:out value="${table.userId}" />">
                <c:out value="${entry.value.fullName}" />
               </a>
              </c:when>
             </c:choose>
            </c:forEach>
           </td>
           <td>
            <c:out value="${table.title}" />
           </td>
           <td>
            <c:out value="${table.description}" />
           </td>
           <td>
            <c:out value="${table.md5}" />
           </td>
           <td>
            <c:out value="${table.originalName}" />
           </td>
           <td>
            <c:out value="${table.version}" />
           </td>
           <td>
            <c:out value="${table.dateCreated}" />
           </td>
           <td>
            <a href="${baseUrl}tables/<c:out value="${table.md5}" />"><img src="${baseUrl}resources/img/view.png" alt="view file"/></a>
           </td>
          </tr>
         </c:when>
         <c:otherwise>
          <tr>
           <td>
            <c:out value="${table.tableId}" /> is hidden
           </td>
          </tr>
         </c:otherwise>
        </c:choose>
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
  </body>
 </html>
