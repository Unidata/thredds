<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 <html>
  <head>
   <title><spring:message code="global.title"/>: BUFR Validation</title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
<h1>BUFR Record Validator </h1>

<p><em>version 0.5</em></p>

<p><strong>Upload a file of one or more BUFR records. These must be BUFR edition 2, 3, or 4. </strong></p>
<hr>
<FORM ENCTYPE='multipart/form-data' method='POST' action='validate/post'>
  <p>Please enter or browse to a local filename:</p>

  <p><strong>Filename: </strong>
    <input type='file' name='file' size="80">
    <input type='submit' value='Validate'/>
    <input type="checkbox" name="xml" value="true"/>
    return XML </p>
    <p><strong>Your Name (keeps your files separate from others): </strong>
    <input type="text" name="username">
  </p>
</FORM>

<hr/>

<form method="GET" action='validate/get'>

  OR enter the URL of a file accessible through an HTTP Server:
  <p><strong>URL: </strong>
    <input type='text' name='url' size="80">
    <input type="submit" value="Validate"/>
    <input type="checkbox" name="xml" value="true"/>
    return XML
  <p><strong>Your Name (keeps your files separate from others): </strong>
    <input type="text" name="username">
  </p>
</form>

<hr>
<h3>Useful links: </h3>
<ul>
  <li><a href="index.html">BUFR Tables Home page</a></li>
  <li><a href="validateHelp.html">BUFR Validation help</a></li>
  <li><a href="WebService.html">BUFR Validation as a web service</a> </li>
  <li><a href="http://www.ecmwf.int/products/data/d/check/">ECMWF BUFR/CREX format checker</a></li>
</ul>
   
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
