<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <c:choose>
     <c:when test="${user != null}">
      <spring:message code="user.update.title"/>:
      <c:out value="${user.userName}" />
     </c:when>
     <c:otherwise>
      <spring:message code="user.create.title"/>
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
    <c:when test="${user != null}">
     <spring:message code="user.update.title"/>:
     <c:out value="${user.userName}" />
    </c:when>
    <c:otherwise>
     <spring:message code="user.create.title"/>
    </c:otherwise>
   </c:choose>
   </h3>
   <p>Note: all fields are required.</p>

   <form id="FORM" action="${baseUrl}/user/<c:out value="${formAction}" />" method="POST">
    <c:choose>
     <c:when test="${user != null}">
      <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
      <input type="hidden" name="userName" value="<c:out value="${user.userName}" />"/>
     </c:when>
    </c:choose>    
    <ul>   
     <c:choose>
      <c:when test="${user == null}">
       <li>
        <label>
         User Name:<br />
         <input type="text" name="userName" value="" />
        </label>
       </li>
      </c:when>
     </c:choose>
     <li>
      <label>
       Full Name<br />
       <input type="text" 
              name="fullName" 
              <c:choose>
               <c:when test="${user != null}">
                value="<c:out value="${user.fullName}" />"
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
       Email Address:<br />
       <input type="text" 
              name="emailAddress" 
              <c:choose>
               <c:when test="${user != null}">
                value="<c:out value="${user.emailAddress}" />"
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
       Affiliation<br />
       <input type="text" 
              name="affiliation" 
              <c:choose>
               <c:when test="${user != null}">
                value="<c:out value="${user.affiliation}" />"
               </c:when>
               <c:otherwise>
                 value=""
               </c:otherwise>
              </c:choose>
         />
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
