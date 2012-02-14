<%@page session="false"%><%@page contentType="text/html"%><%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <title>Server Information</title>
</head>
<body>
<h1>Server Information</h1>

<ul>
    <li>Server:
        <img src='<c:out value="${serverInfo.logoUrl}"/>'
             alt='<c:out value="${serverInfo.logoAltText}"/>'/>
        <c:out value="${serverInfo.name}"/>
    </li>
    <li>Webapp Name: <c:out value="${webappName}"/></li>
    <li>Webapp Version: <c:out value="${webappVersion}"/></li>
    <li>Webapp Version Build Date: <c:out value="${webappVersionBuildDate}"/></li>
    <li>Abstract: <c:out value="${serverInfo.summary}"/></li>
    <li>Keyphrases: <c:out value="${serverInfo.keywords}"/></li>
    <li>Contact:
        <ul>
            <c:if test="${not empty serverInfo.contactName}">
                <li>Name: <c:out value="${serverInfo.contactName}"/></li>
            </c:if>
            <li>Organization: <c:out value="${serverInfo.contactOrganization}"/></li>
            <li>Email: <c:out value="${serverInfo.contactEmail}"/></li>
            <c:if test="${not empty serverInfo.contactPhone}">
                <li>Phone: <c:out value="${serverInfo.contactPhone}"/></li>
            </c:if>
        </ul>
    </li>
    <li>Host Institution:
        <a href='<c:out value="${serverInfo.hostInstitutionWebSite}"/>'>
            <img src='<c:out value="${serverInfo.hostInstitutionLogoUrl}"/>'
                 alt='<c:out value="${serverInfo.hostInstitutionLogoAltText}"/>'/>
            <c:out value="${serverInfo.hostInstitutionName}"/>
        </a>
    </li>
</ul>

</body>
</html>