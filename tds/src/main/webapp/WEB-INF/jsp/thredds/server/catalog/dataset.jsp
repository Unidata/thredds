<%@ page import="thredds.catalog.InvDatasetImpl" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Enumeration" %>
<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<c:set var="dsList" value="${requestScope.curDsListParam}"/>
<c:set var="level" value="${requestScope.curLevelParam}"/>

<br>
<h2>DS - Request Attributes</h2>
<%
  Enumeration<String> e1 = request.getAttributeNames();
  for ( ; e1.hasMoreElements(); )
  {
    String s = e1.nextElement();
    Object att = request.getAttribute( s );
    if ( att != null )
    {
      String c = att.getClass().toString();
%>
      <p><%= s %> : <%= c %></p>
<%
    }
  }
%>
      <h2>DS - PageContext (app scope) Attributes</h2>
<%
  Enumeration<String> e = pageContext.getAttributeNamesInScope( PageContext.APPLICATION_SCOPE );
  for ( ; e.hasMoreElements(); )
  {
    String s = e.nextElement();
    Object att = pageContext.getAttribute( s );
    String c = att != null ? att.getClass().toString() : "--";
%>
    <p><%= s %> : <%= c %></p>
<%
  }
%>
      <h2>DS - PageContext (page scope) Attributes</h2>
<%
  e = pageContext.getAttributeNamesInScope( PageContext.PAGE_SCOPE );
  for ( ; e.hasMoreElements(); )
  {
    String s = e.nextElement();
    Object att = pageContext.getAttribute( s );
    String c = att != null ? att.getClass().toString() : "--";
%>
    <p><%= s %> : <%= c %></p>
<%
  }
%>
      <h2>DS - PageContext (req scope) Attributes</h2>
<%
  e = pageContext.getAttributeNamesInScope( PageContext.REQUEST_SCOPE );
  for ( ; e.hasMoreElements(); )
  {
    String s = e.nextElement();
    Object att = pageContext.getAttribute( s );
    String c = att != null ? att.getClass().toString() : "--";
%>
    <p><%= s %> : <%= c %></p>
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