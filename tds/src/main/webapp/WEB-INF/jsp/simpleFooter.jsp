<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<hr size="1" noshade="noshade">

<c:if test="${not empty installationUrl}">
  <a href="${installationUrl}">
</c:if>
${installationName}
<c:if test="${not empty installationUrl}">
  </a>
</c:if>

<c:if test="${not empty hostInstName}">
  at
  <c:if test="${not empty hostInstUrl}">
    <a href="${hostInstUrl}">
  </c:if>
   ${hostInstName}
  <c:if test="${not empty hostInstUrl}">
    </a>
  </c:if>
</c:if>

<br>
Powered by
<c:if test="${not empty webappUrl}">
  <a href="${webappUrl}">
</c:if>
${webappName}
<c:if test="${not empty webappUrl}">
  </a>
</c:if>

[Version ${webappVersion} - ${webappVersionBuildDate}]
<a href="${webappDocsPath}">Documentation</a>
