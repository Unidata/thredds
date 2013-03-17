<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="user.update.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
   <h1><spring:message code="user.update.title"/></h1>

   <p>Note: All fields are required</p>

   <form id="FORM" action="${baseUrl}users/update" method="POST">
    <input type="hidden" name="userId" value="<c:out value="${user.userId}" />"/>
    <ul>   
     <li>
      <label>
       Email Address:<br />
       <input type="text" name="emailAddress" value="<c:out value="${user.emailAddress}" />" disabled="disabled"/>
      </label>
     </li>
     <li>
      <label>
       Full Name<br />
       <input type="text" name="fullName" value="<c:out value="${user.fullName}" />"/>
      </label>
     </li>
     <li>
      <label>
       Affiliation<br />
       <input type="text" name="affiliation" value="<c:out value="${user.affiliation}" />"/>
      </label>
     </li>
     <li>
      <input type="submit" value="update user" />
     </li> 
    </ul>
   </form>
  </body>
 </html>
