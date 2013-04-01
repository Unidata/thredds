<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>


   <c:set var="baseUrl" value="${pageContext.request.contextPath}" />
   <c:set var="GRIB1" value="GRIB-1" />
   <c:set var="GRIB2" value="GRIB-2" />
   <sec:authorize var="loggedIn" access="isAuthenticated()" />
   <c:choose>
    <c:when test="${loggedIn}">
     <c:set var="authUserName">
      <sec:authentication property="principal.username" /> 
     </c:set>
    </c:when>
   </c:choose>
