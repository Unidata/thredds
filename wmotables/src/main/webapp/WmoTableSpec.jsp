<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 <html>
  <head>
   <title><spring:message code="global.title"/>: WMO Tables Specification</title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
<h1>WMO Tables Web Service Specification</h1>
<h2>REST URLs</h2>
<table width="755" border="1">
  <tr>
    <th scope="col">Function</th>
    <th scope="col">URL</th>
  </tr>
  <tr>
    <td>Browse all </td>
    <td>http://server/wmotables/user/</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
</table>
<p>&nbsp;</p>
<h2>Unique ID</h2>
<p>Each table is assigned a unique id based on the <a href="http://en.wikipedia.org/wiki/MD5">MD5 hashcode</a> of the table. This means that even if the table only changes by one byte, it will be assigned a different ID. MD5 is a 128-bit (16 byte, 32 hex digits) hash. NOTE: maybe we should use SHA-2 ??</p>
   
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
