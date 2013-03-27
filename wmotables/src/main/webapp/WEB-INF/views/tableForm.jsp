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
<%@ include file="/WEB-INF/views/include/nav.jsp" %>

   <h3>
   <c:choose>
    <c:when test="${table != null}">
     <spring:message code="table.update.title"/>:
     <c:out value="${table.checksum}" />
    </c:when>
    <c:otherwise>
     <spring:message code="table.create.title"/>
    </c:otherwise>
   </c:choose>
   </h3>
   <p>* denotes required fields.</p>

   <form id="FORM" action="${baseUrl}/table/<c:out value="${formAction}" />" method="POST" enctype="multipart/form-data">
    <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
    <c:choose>
     <c:when test="${table != null}">
      <input type="hidden" name="tableId" value="<c:out value="${table.tableId}" />"/>
     </c:when>
    </c:choose>   
    <ul> 
     <c:choose>
      <c:when test="${table == null}">
       <li>
        <label>
         * Upload your table:<br />
         <input type="file" name="file" value=""/>
        </label>
       </li>
      </c:when>
     </c:choose>
     <li>
      <label>
       * Title<br />
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
      </label>
     </li>
     <li>
      <label>
       * Description<br />
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
      </label>
     </li>
     <li>
      <label>
       Version<br />
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
      </label>
     </li>
     <li>
      <label>
       Table Type<br />
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
            <c:when test="${table.tableType eq GRIB-1}">
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
            <c:when test="${table.tableType eq GRIB-2}">
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
      </label>
     </li>
     <li>
      <input type="submit" value="<c:out value="${formAction}" />" />
     </li> 
    </ul>
   </form>
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
