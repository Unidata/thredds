<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="table.view.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
   <h3><spring:message code="table.view.title"/></h3>
   <p><spring:message code="table.view.message"/></p>

   <c:choose>
    <c:when test="${error != null}">
     <p class="error"><b><c:out value="${error}" /></b></p>
    </c:when>
   </c:choose>

   <table> 
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
       <spring:message code="table.originalName"/>
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
      <th>
       <spring:message code="table.tableActions"/> 
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
     <tr
      <c:choose>
       <c:when test="${table.visibility < 1}">
         class="hidden"
       </c:when>
      </c:choose>
     >
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
       <c:out value="${table.localVersion}" />
      </td>
      <td>
       <c:out value="${table.center}" />
      </td>
      <td>
       <c:out value="${table.subCenter}" />
      </td>
      <td>
       <c:out value="${table.originalName}" />
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
       <form action="${baseUrl}/table/view/<c:out value="${table.checksum}" />" method="GET">
        <input class="action view" type="submit" value="View" />        
       </form>

       <form action="${baseUrl}/table/download/<c:out value="${table.checksum}" />" method="GET">
        <input class="action download" type="submit" value="Download" />        
       </form>
      </td>

      <c:choose>
       <c:when test="${loggedIn}">
        <c:choose>
         <c:when test="${user.userName eq authUserName}">
          <td>
           <form action="${baseUrl}/table/update/<c:out value="${table.checksum}" />" method="GET">
            <input class="action edit" type="submit" value="<spring:message code="table.update.title"/>" />        
           </form>
           <form action="${baseUrl}/table/hide" method="POST">
            <input type="hidden" name="tableId" value="<c:out value="${table.tableId}" />"/>
            <input type="hidden" name="visibility" value="<c:out value="${table.visibility}" />"/>
            <input class="action hide" type="submit" 
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
         </c:when>
         <c:otherwise>
          <sec:authorize access="hasRole('ROLE_ADMIN')">
           <td>
            <form action="${baseUrl}/table/update/<c:out value="${table.checksum}" />" method="GET">
             <input class="action edit" type="submit" value="Edit" />        
            </form>
            <form action="${baseUrl}/table/hide" method="POST">
             <input type="hidden" name="tableId" value="<c:out value="${table.tableId}" />"/>
             <input type="hidden" name="visibility" value="<c:out value="${table.visibility}" />"/>
             <input class="action hide" type="submit" 
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
          </sec:authorize>
         </c:otherwise>
        </c:choose>
       </c:when>
      </c:choose> 

     </tr>
    </tbody>
   </table> 
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>






