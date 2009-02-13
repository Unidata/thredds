<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<table width="100%">
  <tr>
    <td>
      <c:if test="${not empty installationUrl}">
        <a href="${installationUrl}">
      </c:if>
      <img src="${installationLogoUrl}" alt="${installationLogoAlt}"
           align="left" valign="top" hspace="10" vspace="2">
      <h1>${installationName}</h1>
      <c:if test="${not empty installationUrl}">
        </a>
      </c:if>
    </td>
    <td>
      <c:if test="${not empty hostInstUrl}">
        <a href="${hostInstUrl}">
      </c:if>
      <img src="${hostInstLogoUrl}" alt="${hostInstLogoAlt}"
           align="top" valign="top" hspace="10" vspace="2">
      <c:if test="${not empty hostInstName}">
      ${hostInstName}
      </c:if>
      <c:if test="${not empty hostInstUrl}">
        </a>
      </c:if>
    </td>
  </tr>
</table>
