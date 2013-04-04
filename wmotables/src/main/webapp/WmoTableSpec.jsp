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
<h2>WEB Interface</h2>
<pre>URL                               Method    Controller              View                                       Description         Public    Admin    User 
 Access    Access   Access 
 ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
   wmotables/                           GET    TableController.java    /WEB-INF/views/index.jsp                   Greeting page        X         X        X
   wmotables/user                       GET    UserController.java     /WEB-INF/views/listUsers.jsp               Lists all users      X         X        X 
   wmotables/user/{userId}              GET    UserController.java     /WEB-INF/views/viewUser.jsp                View user            X         X        X 
   wmotables/user/create                GET    UserController.java     /WEB-INF/views/userForm.jsp                Create user form               X 
   wmotables/user/create                POST   UserController.java     redirect to /WEB-INF/views/viewUser.jsp    View user                      X
   wmotables/user/update/{userId}       GET    UserController.java     /WEB-INF/views/userForm.jsp                Update user form               X        X
   wmotables/user/update                GET    UserController.java     redirect to /WEB-INF/views/viewUser.jsp    View user                      X        X
   wmotables/user/delete                POST   UserController.java     redirect to /WEB-INF/views/listUsers.jsp   Lists all users                X 
   
   wmotables/table                      GET    TableController.java    /WEB-INF/views/listTables.jsp              Lists all tables     X         X        X
   wmotables/table/{tableId}            GET    TableController.java    /WEB-INF/views/viewTable.jsp               View Table           X         X        X
   wmotables/table/download/{tableId}   GET    TableController.java    /WEB-INF/views/viewTable.jsp               Download table file  X         X        X
   wmotables/table/create/{userId}      GET    TableController.java    /WEB-INF/views/tableForm.jsp               Create table form              X        X
   wmotables/table/create               POST   TableController.java    redirect to /WEB-INF/views/viewTable.jsp   View table                     X        X 
   wmotables/table/update/{tableId}     GET    TableController.java    /WEB-INF/views/tableForm.jsp               Update table form              X        X 
   wmotables/table/update               POST   TableController.java    redirect to /WEB-INF/views/viewTable.jsp   View table                     X        X
   wmotables/table/hide                 POST   TableController.java    redirect to /WEB-INF/views/viewTable.jsp   Hide table                     X </pre>
<hr />
<h2>REST Interface</h2>
<table width="988" border="1">
  <tr>
    <th width="242" scope="col">Function</th>
    <th width="400" scope="col">URL</th>
    <th width="61" scope="col">Verb</th>
    <th width="85" scope="col">Authenticate</th>
    <th width="166" scope="col">Send/Return</th>
  </tr>
    <tr>
    <td>List all users</td>
    <td>http://server/wmotables/user/users.xml</td>
    <td>GET</td>
    <td>&nbsp;</td>
    <td>List of user names</td>
    </tr>
  <tr>
    <td>View user {userId} </td>
    <td>http://server/wmotables/user/{userId}.xml</td>
    <td>GET</td>
    <td>&nbsp;</td>
    <td>{userId}.xml</td>
  </tr>
  <tr>
    <td>Update user {userId}</td>
    <td>http://server/wmotables/user/{userId}.xml</td>
    <td>POST</td>
    <td>Yes</td>
    <td>{userId}.xml/</td>
  </tr>
  <tr>
    <td>Add table owned by{userId}</td>
    <td>http://server/wmotables/user/{userId}/addTable</td>
    <td>POST</td>
    <td>Yes</td>
    <td>Table metadata</td>
  </tr>
  <tr>
    <td>List all tables</td>
    <td>http://server/wmotables/table/tables.xml</td>
    <td>GET</td>
    <td>&nbsp;</td>
    <td>List of table metadata</td>
  </tr>
  <tr>
    <td>List all tables owned by {userId}</td>
    <td>http://server/wmotables/table/{tableId}/tables.xml</td>
    <td>GET</td>
    <td>&nbsp;</td>
    <td>List of table metadata</td>
  </tr>
  <tr>
    <td>View table {tableId} metadata</td>
    <td>http://server/wmotables/table/{tableId}/table.xml</td>
    <td>GET</td>
    <td>&nbsp;</td>
    <td>Table metadata</td>
  </tr>
  <tr>
    <td>Download table {tableId}</td>
    <td>http://server/wmotables/table/download/{tableId} </td>
    <td>GET</td>
    <td>&nbsp;</td>
    <td>Original Table</td>
  </tr>
</table>
<p>&nbsp;</p>
<h2>Unique ID</h2>
<p>Each table is assigned a unique id based on the <a href="http://en.wikipedia.org/wiki/MD5">MD5 hashcode</a> of the table. This means that even if the table only changes by one byte, it will be assigned a different ID. MD5 is a 128-bit (16 byte, 32 hex digits) hash. NOTE: maybe we should use SHA-2 ??</p>
   
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
