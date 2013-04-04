<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <c:choose>
     <c:when test="${formAction == 'update'}">
      <spring:message code="table.update.title"/>:
      <c:out value="${table.checksum}" />
     </c:when>
     <c:otherwise>
      <spring:message code="table.create.title"/>
     </c:otherwise>
    </c:choose>
   </title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>


   <c:choose>
    <c:when test="${formAction == 'update'}">
     <h3><spring:message code="table.update.title"/></h3>
     <p><spring:message code="table.update.message"/></p>
    </c:when>
    <c:otherwise>
     <h3><spring:message code="table.create.title"/></h3>
     <p><spring:message code="table.create.message"/></p>
    </c:otherwise>
   </c:choose>


   <c:choose>
    <c:when test="${error != null}">
     <p class="error"><b><c:out value="${error}" /></b></p>
    </c:when>
   </c:choose>
   
   <form:form action="${baseUrl}/table/${formAction}" commandName="table" method="POST" enctype="multipart/form-data">
    <input type="hidden" name="userId" value="${user.userId}"/>
    <c:choose>
     <c:when test="${formAction == 'update'}">
      <form:hidden path="tableId" />
     </c:when>
    </c:choose>   

    <table> 
     <thead>
      <tr>
       <c:choose>
        <c:when test="${formAction == 'create'}">
         <th>
          Upload Table  <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.uploadTable.description"/>"/>
         </th>
        </c:when>
       </c:choose>
       <th>
        Title <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.title.description"/>"/>
       </th>
       <th>
        Description <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.description.description"/>"/>
       </th>
       <th>
        Version <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.version.description"/>"/>
        <small class="deemph">(optional)</small>
       </th>
       <th>
        Table Type <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.tableType.description"/>"/>
       </th>
       <c:choose>
        <c:when test="${formAction == 'update'}">
         <th>
          Checksum <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.checksum.description"/>"/>
         </th>
        </c:when>
       </c:choose>
       <th>
        Owner <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.owner.description"/>"/>
       </th>
       <c:choose>
        <c:when test="${formAction == 'update'}">
         <th>
           Date Created <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.dateCreated.description"/>"/>
         </th>
        </c:when>
       </c:choose>
       <th>
        Action
       </th>
      </tr>
     </thead>


     <tbody>   
      <tr>
       <c:choose>
        <c:when test="${formAction == 'create'}">
         <td>
          <form:errors path="file" cssClass="error" />
          <input type="file" name="file"/>
         </td>
        </c:when>
       </c:choose>
       <td>
        <form:errors path="title" cssClass="error" />
        <form:input path="title"/>
       </td>
       <td>
        <form:errors path="description" cssClass="error" />
        <form:textarea path="description" rows="2" cols="30" />
       </td>
       <td>
        <form:errors path="version" cssClass="error" />
        <form:input path="version"/>
       </td>
       <td>
        <form:errors path="tableType" cssClass="error" />
        <form:select path="tableType" items="${tableTypeList}" />
       </td>
       <c:choose>
        <c:when test="${formAction == 'update'}">
         <td>
          <c:out value="${table.checksum}" />
         </td>
        </c:when>
       </c:choose>
       <td>
        <c:out value="${user.userName}" />
       </td>
       <c:choose>
        <c:when test="${formAction == 'update'}">
         <td>
          <fmt:formatDate value="${table.dateCreated}" type="BOTH" dateStyle="default"/>
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
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>






