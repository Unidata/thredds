<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<table width="100%">
  <tr>
    <td>
      <c:if test="${not empty installationLogoUrl}">
      <img src="${installationLogoUrl}" alt="${installationLogoAlt}"
           align="left" valign="top" hspace="10" vspace="2">
      </c:if>

      <h3><strong><c:if test="${not empty installationUrl}"><a href="${installationUrl}"></c:if>
      ${installationName}
      <c:if test="${not empty installationUrl}"></a></c:if></strong></h3>
      <h3><strong><c:if test="${not empty webappUrl}"><a href="${webappUrl}"></c:if>
      ${webappName}
        <c:if test="${not empty webappUrl}"></a></c:if></strong></h3>
    </td>
  </tr>
</table>
