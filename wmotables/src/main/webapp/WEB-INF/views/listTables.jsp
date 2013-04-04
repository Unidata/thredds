<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="table.list.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
   <h3><spring:message code="table.list.title"/></h3>
   <p><spring:message code="table.list.message"/></p>

   <c:choose>
    <c:when test="${error != null}">
     <p class="error"><b><c:out value="${error}" /></b></p>
    </c:when>
   </c:choose>

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
         <spring:message code="table.center"/>
        </th>
        <th>
         <spring:message code="table.subCenter"/>
        </th>
        <th>
         <spring:message code="table.checksum"/>
        </th>
        <th>
         <spring:message code="table.owner"/>
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
          <c:forEach items="${users}" var="entry">
           <c:choose>
            <c:when test="${entry.key == table.userId}">
             <a href="${baseUrl}/table/<c:out value="${table.checksum}" />">
              <c:out value="${entry.value.userName}" />
             </a>
            </c:when>
           </c:choose>
          </c:forEach>
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






