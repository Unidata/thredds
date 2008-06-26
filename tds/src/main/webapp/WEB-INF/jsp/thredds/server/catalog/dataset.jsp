<%@ page import="thredds.catalog.InvDatasetImpl" %>
<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<c:set var="dataset" value="${requestScope.curDsParam}"/>
<c:set var="level" value="${requestScope.curLevelParam}"/>
<tr>
  <td align="left" style="padding-left: ${level * 10}px">

<%
  InvDatasetImpl curDataset = (InvDatasetImpl) pageContext.getAttribute( "dataset" );
  if ( curDataset.hasNestedDatasets())
  {
%>
      <img src="/thredds/folder.gif" alt="folder">
      [<a href="">Folder</a>]
<%
  }
  if ( curDataset.hasAccess())
  {
%>
      [<a href="">Dataset</a>]
<%
  }
%>
    ${dataset.name}
  </td>
  <td align="center">${dataset.dataSize}</td>
  <td align="right">${dataset.lastModifiedDate}</td>
  <c:forEach var="curDs" items="${dataset.datasets}">
    <c:set var="curDsParam" value="${curDs}" scope="request"/>
    <c:set var="curLevelParam" value="${level+1}" scope="request"/>
    <c:import url="dataset.jsp" />
    <c:remove var="curDsParam" scope="request"/>
    <c:remove var="curLevelParam" scope="request"/>
  </c:forEach>

</tr>