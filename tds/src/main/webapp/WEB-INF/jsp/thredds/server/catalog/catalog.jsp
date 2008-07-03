<%@ page import="thredds.catalog.InvCatalogImpl" %>
<%@ page import="thredds.catalog.InvDatasetImpl" %>
<%@ page import="org.springframework.web.util.HtmlUtils" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Enumeration" %>
<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<c:set var="cat" value="${catalog}" scope="request"/>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%
  InvCatalogImpl catalog = (InvCatalogImpl) request.getAttribute( "catalog" );
  String catName = (String) request.getAttribute( "catName");
  String catUri = (String) request.getAttribute( "catUri");
  // isLocalCatalog
  String webappName = (String) request.getAttribute( "webappName");
  String webappVersion = (String) request.getAttribute( "webappVersion");
  String webappBuildDate = (String) request.getAttribute( "webappBuildDate");
  String webappDocsPath = (String) request.getAttribute( "webappDocsPath");
%>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="icon" href="/thredds/favicon.ico" type="image/x-icon"/>
  <link rel="stylesheet" href="/thredds/tdsCat.css" type="text/css"/>
  <title>Catalog: <%= catUri %></title>
</head>

<body>

<%--
<h2>Request Attributes</h2>
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
      <h2>PageContext (app scope) Attributes</h2>
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
      <h2>PageContext (page scope) Attributes</h2>
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
      <h2>PageContext (req scope) Attributes</h2>
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
--%>
<% if ( catName != null ) { %>
  <h1>Catalog: <%= catName %><br><%= catUri %></h1>
<% } else { %>
  <h1>Catalog: <%= catUri %></h1>
<% } %>
                 

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