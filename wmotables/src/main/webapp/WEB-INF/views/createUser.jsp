<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="user.create.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
   <h1><spring:message code="user.create.title"/></h1>

   <p>Note: All fields are required</p>

   <form id="FORM" action="${baseUrl}users/create" method="POST">
    <ul>   
     <li>
      <label>
       Email Address:<br />
       <input type="text" name="emailAddress" value=""/>
      </label>
     </li>
     <li>
      <label>
       Full Name<br />
       <input type="text" name="fullName" value=""/>
      </label>
     </li>
     <li>
      <label>
       Affiliation<br />
       <input type="text" name="affiliation" value=""/>
      </label>
     </li>
     <li>
      <input type="submit" value="create user" />
     </li> 
    </ul>
   </form>
  </body>
 </html>
