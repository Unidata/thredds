<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="${standardCssUrl}"/>" type="text/css"/>
  <title>Catalog Service Request - Catalog Validation</title>
</head>

<body>
<c:import url="/WEB-INF/jsp/siteHeader.jsp" />
<hr>
<h2>Catalog Service Request - Catalog Validation</h2>

<p>Catalog [${catalogUrl}] validation message:</p>

<pre style="margin-left: 40px;">
  ${message}
</pre>

<c:import url="/WEB-INF/jsp/webappFooter.jsp" />

</body>
</html>