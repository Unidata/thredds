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
          <spring:message code="table.uploadTable"/>  <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.uploadTable.description"/>"/>
         </th>
        </c:when>
       </c:choose>
       <th>
        <spring:message code="table.tableType"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.tableType.description"/>"/>
       </th>
       <th>
        <spring:message code="table.localVersion"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.localVersion.description"/>"/>
       </th>

       <th>
        <spring:message code="table.center"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.center.description"/>"/>
       </th>
       <th>
        <spring:message code="table.subCenter"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.subCenter.description"/>"/>
       </th>
       <th>
        <spring:message code="table.title"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.title.description"/>"/>
       </th>
       <th>
        <spring:message code="table.description"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.description.description"/>"/>
       </th>
       <c:choose>
        <c:when test="${formAction == 'update'}">
         <th>
          <spring:message code="table.checksum"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.checksum.description"/>"/>
         </th>
        </c:when>
       </c:choose>
       <c:choose>
        <c:when test="${formAction == 'update'}">
         <th>
           <spring:message code="table.dateCreated"/> <img src="${baseUrl}/<spring:message code="help.path"/>" alt="<spring:message code="table.dateCreated.description"/>"/>
         </th>
        </c:when>
       </c:choose>
       <th>
        <spring:message code="form.action.title"/>
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
        <form:errors path="tableType" cssClass="error" />
        <form:select path="tableType" items="${tableTypeList}" />
       </td>
       <td>
        <form:errors path="localVersion" cssClass="error" />
        <form:input path="localVersion"/>
       </td>
       <td>
        <form:errors path="center" cssClass="error" />
        <form:input path="center"/>
       </td>
       <td>
        <form:errors path="subCenter" cssClass="error" />
        <form:input path="subCenter"/>
       </td>
       <td>
        <form:errors path="title" cssClass="error" />
        <form:input path="title"/>
       </td>
       <td>
        <form:errors path="description" cssClass="error" />
        <form:textarea path="description" rows="2" cols="30" />
       </td>
       <c:choose>
        <c:when test="${formAction == 'update'}">
         <td>
          <c:out value="${table.checksum}" />
         </td>
        </c:when>
       </c:choose>
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






