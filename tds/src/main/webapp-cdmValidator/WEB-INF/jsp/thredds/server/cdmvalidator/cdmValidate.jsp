<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="<c:url value="${standardCssUrl}"/>" type="text/css"/>
  <title>CDM Validation</title>
</head>

<body>
<c:import url="/WEB-INF/jsp/siteHeader.jsp" />


<h1>Common Data Model Coordinate System Validation</h1>

<p>
  The
  <a href="http://www.unidata.ucar.edu/software/netcdf/CDM/CDM.html">Common Data Model</a>
  (CDM) Coordinate System Validator  identifies  georeferencing Coordinate
  Systems in a CDM file (e.g. NetCDF), using CF-1.0, COARDS or other
  <a href="http://www.unidata.ucar.edu/software/netcdf/docs/conventions.html">Conventions</a>
  that it knows about. This accurately indicates what will be viewable
  in applications like the
  <a href="http://www.unidata.ucar.edu/software/idv/">IDV</a>
  that use the CDM
  <a href="http://www.unidata.ucar.edu/software/netcdf-java/">Netcdf-Java library</a>.
  It currently only works on gridded data.
</p>

<hr>
<FORM ENCTYPE='multipart/form-data' method='POST' action='<c:url value="validate"/>'>
<p>
  <strong>Your Name: </strong>
  <input type="text" name="username">
</p>

<p>Please enter or browse to a local filename:</p>
<p><strong>Filename: </strong>
  <INPUT NAME='uploadFile' TYPE='file' size="80"><INPUT TYPE='submit' VALUE='Validate' /> 
  <input type="checkbox" name="xml" value="true"/>
    return XML </p>
</FORM>

<form method="GET" action='<c:url value="validate"/>'>
<p>OR enter the URL of a file accessible through an HTTP Server or OPeNDAP:</p>
<p><strong>URL: </strong>
  <INPUT NAME='URL' TYPE='text' id="URL" size="80">
<input type="submit" value="Validate"/>
<input type="checkbox" name="xml" value="true"/>
return XML
</form>
 
<hr>
<h3>Useful links: </h3>
<ul>
  <li><a href="<c:url value='validateHelp.html'/>">CDM validation output help</a> </li>
  <li><a href="http://titania.badc.rl.ac.uk/cgi-bin/cf-checker.pl">CF-1.0 Compliance checker</a> </li>
</ul>

<c:import url="/WEB-INF/jsp/webappFooter.jsp" />

</body>
</html>