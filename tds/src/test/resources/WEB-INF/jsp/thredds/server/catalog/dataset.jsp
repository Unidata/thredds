<%@ page import="thredds.catalog.InvDatasetImpl" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="thredds.catalog.InvDataset" %>
<%@ page import="thredds.catalog.InvCatalogRef" %>
<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<%--
<c:set var="curDsList" value="${requestScope.curDsListParam}" />
<c:set var="curLevel" value="${requestScope.curLevelParam}" />
--%>
<%
  List<InvDatasetImpl> curDsList = (List<InvDatasetImpl>) request.getAttribute( "curDsListParam" );
  int curLevel = Integer.parseInt( (String) request.getAttribute( "curLevelParam" ));

  for ( InvDatasetImpl curDs : curDsList )
  {
    if ( curDs instanceof InvCatalogRef )
    {

    }
    else
    {
      
    }
%>

<%
  }
%>
<tr>
  <c:forEach var="curDs" items="${dsList}">
<%
  //InvDatasetImpl curDataset = (InvDatasetImpl) pageContext.getAttribute( "curDs" );

%>
    <%--td align="left" style="padding-left: ${level * 10}px"--%>

  </c:forEach>
    <%-- ToDo If curDs instanceof InvCatalogRef --%>
    <%-- If ! (curDs instanceof InvCatalogRef) --%>

<%--
  List datasetList = (List) pageContext.getAttribute( "dsList" );
  if ( curDataset.hasNestedDatasets())
  {
--%>
      <img src="/thredds/folder.gif" alt="folder">
      [<a href="">Folder</a>]
<%--
  }
  if ( curDataset.hasAccess())
  {
--%>
      [<a href="">Dataset</a>]
<%--
  }
--%>
<%--
    ${dataset.name}
  </td>
--%>
  <%--td align="center">${dataset.dataSize}</td>
  <td align="right">${dataset.lastModifiedDate}</td>
  <c:forEach var="curDs" items="${dataset.datasets}">
    <c:set var="curDsListParam" value="${curDs}" scope="request"/>
    <c:set var="curLevelParam" value="${level+1}" scope="request"/>
    <c:import url="dataset.jsp" />
    <c:remove var="curDsParam" scope="request"/>
    <c:remove var="curLevelParam" scope="request"/>
  </c:forEach--%>

</tr>