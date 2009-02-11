<%@ page session="false"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<table width="100%">
  <tr>
    <td>
      <c:if test="${not empty siteUrl}">
        <a href="${siteUrl}">
      </c:if>
      <img src="${siteLogoPath}" alt="${siteLogoAlt}"
           align="left" valign="top" hspace="10" vspace="2">
      <h1>${serverName}</h1>
      <c:if test="${not empty siteUrl}">
        </a>
      </c:if>
    </td>
  </tr>
</table>
