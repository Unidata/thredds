<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>

<!DOCTYPE HTML>
 <html>
  <head>
   <title>
    <spring:message code="global.title"/>:
    <spring:message code="fatal.error.title"/>
   </title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
   <h1><spring:message code="global.title"/></h1>

   <h3><spring:message code="fatal.error.title"/></h3>
   <p><spring:message code="fatal.error.message"/></p>
    
   <pre>
   <c:out value="${message}" />
   </pre>

  </body>
 </html>
