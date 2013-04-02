<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <c:choose>
     <c:when test="${table != null}">
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
    <c:when test="${table != null}">
     <h3><spring:message code="table.update.title"/></h3>
     <p><spring:message code="table.update.message"/></p>
    </c:when>
    <c:otherwise>
     <h3><spring:message code="table.create.title"/></h3>
     <p><spring:message code="table.create.message"/></p>
    </c:otherwise>
   </c:choose>

   
   <form action="${baseUrl}/table/<c:out value="${formAction}" />" method="POST" enctype="multipart/form-data">
    <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
    <c:choose>
     <c:when test="${table != null}">
      <input type="hidden" name="tableId" value="<c:out value="${table.tableId}" />"/>
     </c:when>
    </c:choose>   

    <table> 
     <thead>
      <tr>
       <c:choose>
        <c:when test="${table == null}">
         <th>
          Upload Table
         </th>
        </c:when>
       </c:choose>
       <th>
        Title
       </th>
       <th>
        Description
       </th>
       <th>
        Version
       </th>
       <th>
        Table Type
       </th>
       <c:choose>
        <c:when test="${table != null}">
         <th>
          Checksum
         </th>
        </c:when>
       </c:choose>
       <th>
        Owner
       </th>
       <c:choose>
        <c:when test="${table != null}">
         <th>
           Date Created
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
        <c:when test="${table == null}">
         <td>
          <input type="file" name="file" value=""/>
         </td>
        </c:when>
       </c:choose>
       <td>
        <input type="text" 
              name="title" 
              <c:choose>
               <c:when test="${table != null}">
                value="<c:out value="${table.title}" />"
               </c:when>
               <c:otherwise>
                 value=""
               </c:otherwise>
              </c:choose>
         />
       </td>
       <td>
        <input type="text" 
              name="description" 
              <c:choose>
               <c:when test="${table != null}">
                value="<c:out value="${table.description}" />"
               </c:when>
               <c:otherwise>
                 value=""
               </c:otherwise>
              </c:choose>
         />
       </td>
       <td>
        <input type="text" 
              name="version" 
              <c:choose>
               <c:when test="${table != null}">
                value="<c:out value="${table.version}" />"
               </c:when>
               <c:otherwise>
                 value=""
               </c:otherwise>
              </c:choose>
         />
       </td>
       <td>
               <select name="tableType">
        <option value="BUFR"
         <c:choose>
          <c:when test="${table != null}">
           <c:choose>
            <c:when test="${table.tableType eq BUFR}">
             selected
            </c:when>
           </c:choose>
          </c:when>
         </c:choose>
        >BUFR</option>
        <option value="GRIB-1"
         <c:choose>
          <c:when test="${table != null}">
           <c:choose>
            <c:when test="${table.tableType eq GRIB1}">
             selected
            </c:when>
           </c:choose>
          </c:when>
         </c:choose>
        >GRIB-1</option>
        <option value="GRIB-2"
         <c:choose>
          <c:when test="${table != null}">
           <c:choose>
            <c:when test="${table.tableType eq GRIB2}">
             selected
            </c:when>
           </c:choose>
          </c:when>
         </c:choose>
        >GRIB-2</option>
        <option value="other"
         <c:choose>
          <c:when test="${table != null}">
           <c:choose>
            <c:when test="${table.tableType eq other}">
             selected
            </c:when>
           </c:choose>
          </c:when>
         </c:choose>
        >other</option>
       </select>
       </td>
       <c:choose>
        <c:when test="${table != null}">
         <td>
          <c:out value="${table.checksum}" />
         </td>
        </c:when>
       </c:choose>
       <td>
        <c:out value="${user.userName}" />
       </td>
       <c:choose>
        <c:when test="${table != null}">
         <td>
          <fmt:formatDate value="${table.dateCreated}" type="BOTH" dateStyle="default"/>
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
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>






