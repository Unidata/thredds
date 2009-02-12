<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<hr size="1" noshade="noshade">

<c:if test="${not empty webappUrl}">
  <a href="${webappUrl}">
</c:if>
<img src="${webappLogoPath}" alt="${webappLogoAlt}" align="left">
${webappName}
<c:if test="${not empty webappUrl}">
  </a>
</c:if>

[Version ${webappVersion} - ${webappBuildDate}]
<a href="${webappDocsPath}">Documentation</a>
