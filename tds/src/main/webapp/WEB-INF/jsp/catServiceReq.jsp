<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="/tds.css"/>" type="text/css"/>
  <title>Catalog Service Request</title>
</head>

<body>
  <h1>Catalog Service Request</h1>
  <hr>
  <ul>
    <li>path=[${path}]</li>
    <li>catalog=[${catalog}]</li>
    <li>dataset=[${dataset}]</li>
    <li>command=[${command}]</li>
    <li>view=[${view}]</li>
    <li>debug=[${debug}]</li>

  </ul>

</body>
</html>