<%@ page import="thredds.catalog.InvCatalogImpl" %>
<%@ page import="thredds.catalog.InvDatasetImpl" %>
<%@ page import="org.springframework.web.util.HtmlUtils" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Enumeration" %>
<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<c:set var="cat" value="${catalog}" scope="request"/>

<%
  Enumeration<String> e = pageContext.getAttributeNamesInScope( PageContext.REQUEST_SCOPE );
  for ( ; e.hasMoreElements(); )
  {
    String s = e.nextElement();
    String c = pageContext.getAttribute( s ).getClass().toString();
%>
    <td><%= s %> : <%= c %></td>
<%
  }
                                           
//  InvCatalogImpl catalog = (InvCatalogImpl) pageContext.getAttribute( "cat" );
//  String catUri = HtmlUtils.htmlEscape( catalog.getUriString());
//  String catName = catalog.getName();
//  List childrenDs = catalog.getDatasets();
//  InvDatasetImpl onlyChild = null;
//  if ( childrenDs.size() == 1 )
//  {
//    onlyChild = (InvDatasetImpl) childrenDs.get( 0 );
//    if ( catName == null )
//      catName = onlyChild.getName();
//  }
//  if ( catName != null)
//    catName = HtmlUtils.htmlEscape( catName);
%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="icon" href="/thredds/favicon.ico" type="image/x-icon"/>
  <link rel="stylesheet" href="/thredds/tdsCat.css" type="text/css"/>
  <title>Catalog: <%--= catName != null ? catName : catUri --%></title>
</head>

<body>
<%-- //if ( catName != null ) { %>
  <h1>Catalog: <%= catName %><br><%= catUri %></h1>
<% //} else { %>
  <h1>Catalog: <%= catUri %></h1>
<% //} --%>


  <hr size="1" noshade="noshade">
  <table width="95%" cellspacing="0" cellpadding="5" align="center">
    <tr>
      <th align="left">Dataset</th>
      <th align="center">Size</th>
      <th align="right">Last Modified</th>
    </tr>
    <c:set var="curDsListParam" value="${catalog.datasets}" scope="request"/>
    <c:set var="curLevelParam" value="0" scope="request"/>
    <c:import url="dataset.jsp" />
    <c:remove var="curDsListParam" scope="request"/>
    <c:remove var="curLevelParam" scope="request"/>
  </table>
  <hr size="1" noshade="noshade">
  <h3>
    ${webappName} [Version ${webappVersion}]
    <a href="${docsPath}">Documentation</a>
  </h3>
</body>
</html>