   <div id="container">

    <div id="header"> 

     <h1><spring:message code="global.title"/></h1>

     <ul class="nav"> 
      <li><a href="${baseUrl}">Home</a></li>

      <li><a href="${baseUrl}/table"><spring:message code="link.table.list.title"/></a></li>
      <li><a href="${baseUrl}/user"><spring:message code="link.user.list.title"/></a></li>   
      <c:choose>
       <c:when test="${loggedIn}">
        <li><a href="${baseUrl}/table/create/${authUserName}"><spring:message code="link.table.create.title"/></a></li>
        <sec:authorize access="hasRole('ROLE_ADMIN')">
         <li><a href="${baseUrl}/user/create"><spring:message code="link.user.create.title"/></a></li>
        </sec:authorize>
       </c:when>
      </c:choose>  
     </ul>

     <ul class="account"> 
      <c:choose>
       <c:when test="${loggedIn}">
        <li><a href="${baseUrl}/user/${authUserName}"><spring:message code="link.account.title"/> ${authUserName}</a></li>
        <li><a href="${baseUrl}/auth/logout"><spring:message code="link.logout.title"/></a></li>
       </c:when>
       <c:otherwise>
        <li><a href="${baseUrl}/auth/login"><spring:message code="link.login.title"/></a></li>
       </c:otherwise>
      </c:choose>    
 
     </ul>


    </div> <!-- end header -->

     <div id="content">

