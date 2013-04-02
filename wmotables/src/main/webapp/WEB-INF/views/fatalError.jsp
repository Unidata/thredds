<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE HTML>
 <html>
  <head>
   <title><spring:message code="global.title"/>: <spring:message code="fatal.error.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
   <h3><spring:message code="fatal.error.title"/></h3>
   <p><spring:message code="fatal.error.message"/></p>    
   <pre>
   <c:out value="${message}" />
   </pre>
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
