<%@page session="false"%><%@page contentType="text/xml"%><%@page pageEncoding="UTF-8"%><?xml version="1.0" encoding="UTF-8"?>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<serverInformation>
    <name><c:out value="${serverInfo.name}"/></name>
    <!--logoUrl><c:out value="${serverInfo.logoUrl}"/></logoUrl-->
    <!--logoAltText><c:out value="${serverInfo.logoAltText}"/></logoAltText-->
    <webapp>
        <name><c:out value="${webappName}"/></name>
        <version><c:out value="${webappVersion}"/></version>
        <versionBuildDate><c:out value="${webappVersionBuildDate}"/></versionBuildDate>
    </webapp>

    <abstract><c:out value="${serverInfo.summary}"/></abstract>
    <keywords><c:out value="${serverInfo.keywords}"/></keywords>

    <contact>
        <name><c:out value="${serverInfo.contactName}"/></name>
        <organization><c:out value="${serverInfo.contactOrganization}"/></organization>
        <email><c:out value="${serverInfo.contactEmail}"/></email>
        <c:if test="${not empty serverInfo.contactPhone}">
            <phone><c:out value="${serverInfo.contactPhone}"/></phone>
        </c:if>
    </contact>
    <hostInstitution>
        <name><c:out value="${serverInfo.hostInstitutionName}"/></name>
        <webSite><c:out value="${serverInfo.hostInstitutionWebSite}"/></webSite>
        <!--logoUrl><c:out value="${serverInfo.hostInstitutionLogoUrl}"/></logoUrl-->
        <!--logoAltText><c:out value="${serverInfo.hostInstitutionLogoAltText}"/></logoAltText-->
    </hostInstitution>
</serverInformation>
