<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
 <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
 <link rel='stylesheet' href='<c:url value="${standardCssUrl}"/>' type='text/css' />
  <title>THREDDS Catalog Validation</title>
</head>
<body>

<c:import url="/WEB-INF/jsp/siteHeader.jsp" />

<h1 align="left">THREDDS Catalog Validation</h1>

<form method="GET" action="remoteCatalogService">
  <input type="hidden" name="command" value="validate">
<p/> Catalog URL:
    <input type="text" name="catalog" size="80"> 
<p/> Options: <input type="checkbox" name="verbose" value="true"/>Verbose
<p/>
<input type="submit" value="Submit"/>
<input type="reset" value="Reset"/>
</form>

<hr width="100%"/>
<c:import url="/WEB-INF/jsp/webappFooter.jsp" />

</body>
</html>