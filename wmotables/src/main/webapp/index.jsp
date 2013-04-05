<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 <html>
  <head>
   <title><spring:message code="global.title"/></title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
  <h3><spring:message code="global.title"/></h3>
  <p>This is a prototype web service for registering <a href="http://www.wmo.int/pages/prog/www/WMOCodes/WMO306_vI2/LatestVERSION/LatestVERSION.html">WMO Tables</a> for use in table driven data formats such as GRIB and BUFR. </p>
  <p>Every table  is assigned a unique ID that can be written into the data record to indicate unambiguously which table was used when writing the record. Software reading the data record can use the ID to fetch the table from this web service.  Tables are always owned by a user, and only a registered user can add or delete tables. All tables uploaded here are  freely readable by anyone. </p>
  <ul>
   <li><a href="/wmotables/table">Browse WMO Tables</a></li>
   <li>Register as a user by <a href="mailto:wmotables@unidata.ucar.edu">sending email</a> to us</li>
   <li><a href="wmoTableSpec.jsp">Technical Specifications</a></li>
   <li><a href="summary.jsp">Background</a> and <a href="links.jsp">Links</a></li>
  </ul>
  
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
