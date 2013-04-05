<%@ include file="/WEB-INF/views/include/tablibs.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 <html>
  <head>
   <title><spring:message code="global.title"/>: WMO Tables Specification</title>
<%@ include file="/WEB-INF/views/include/resources.jsp" %>
  </head>
  <body> 
<%@ include file="/WEB-INF/views/include/header.jsp" %>
  <h3>WMO Tables Web Service Specification</h3>
  <h5>WEB Interface</h5>
  <table>
   <thead>
    <tr>
     <th width="242">Function</th>
     <th width="400">URL</th>
     <th width="61">Verb</th>
     <th width="85">Authenticate</th>
     <th width="100">Contoller</th>
     <th width="166">Send/Return</th>
    </tr>
   </thead>
   <tbody>
    <tr>
     <td>List all users</td>
     <td>http://server/wmotables/user</td>
     <td>GET</td>
     <td>&nbsp;</td>
     <td>UserController</td>
     <td>/WEB-INF/views/listUsers.jsp</td>
    </tr>
    <tr>
     <td>View user {userId} (and tables owned by {userId})</td>
     <td>http://server/wmotables/user/{userId}</td>
     <td>GET</td>
     <td>&nbsp;</td>
     <td>UserController</td>
     <td>/WEB-INF/views/viewUsers.jsp</td>
    </tr>
    <tr>
     <td>Create new user (display form)</td>
     <td>http://server/wmotables/user/create</td>
     <td>GET</td>
     <td>Yes</td>
     <td>UserController</td>
     <td>/WEB-INF/views/userForm.jsp</td>
    </tr>
    <tr>
     <td>Create new user</td>
     <td>http://server/wmotables/user/create</td>
     <td>POST</td>
     <td>Yes</td>
     <td>UserController</td>
     <td>redirect to /WEB-INF/views/viewUser.jsp</td>
    </tr>
    <tr>
     <td>Update user {userId} (display form)</td>
     <td>http://server/wmotables/user/update/{userId}</td>
     <td>GET</td>
     <td>Yes</td>
     <td>UserController</td>
     <td>/WEB-INF/views/userForm.jsp</td>
    </tr>
    <tr>
     <td>Update user {userId}</td>
     <td>http://server/wmotables/user/update/{userId}</td>
     <td>POST</td>
     <td>Yes</td>
     <td>UserController</td>
     <td>redirect to /WEB-INF/views/viewUser.jsp</td>
    </tr>
    <tr>
     <td>Delete user</td>
     <td>http://server/wmotables/user/delete</td>
     <td>POST</td>
     <td>Yes</td>
     <td>UserController</td>
     <td>redirect to /WEB-INF/views/listUsers.jsp</td>
    </tr>
    <tr>
     <td>List all tables</td>
     <td>http://server/wmotables/table</td>
     <td>GET</td>
     <td>&nbsp;</td>
     <td>TableController</td>
     <td>/WEB-INF/views/listTables.jsp</td>
    </tr>
    <tr>
     <td>View table {tableId} metadata</td>
     <td>http://server/wmotables/table/{tableId}</td>
     <td>GET</td>
     <td>&nbsp;</td>
     <td>TableController</td>
     <td>/WEB-INF/views/viewTable.jsp </td>
    </tr>
    <tr>
     <td>View table {tableId}</td>
     <td>http://server/wmotables/table/view/{tableId}</td>
     <td>GET</td>
     <td>&nbsp;</td>
     <td>TableController</td>
     <td>{tableId}</td>
    </tr>
    <tr>
     <td>Download table {tableId}</td>
     <td>http://server/wmotables/table/download/{tableId}</td>
     <td>GET</td>
     <td>&nbsp;</td>
     <td>TableController</td>
     <td>{tableId}</td>
    </tr>
    <tr>
     <td>Add table owned by {userId} (display form)</td>
     <td>http://server/wmotables/table/create/{userId}</td>
     <td>GET</td>
     <td>Yes</td>
     <td>TableController</td>
     <td>/WEB-INF/views/tableForm.jsp</td>
    </tr>
    <tr>
     <td>Add table owned by {userId}</td>
     <td>http://server/wmotables/table/create</td>
     <td>POST</td>
     <td>Yes</td>
     <td>TableController</td>
     <td>redirect to /WEB-INF/views/viewTable.jsp</td>
    </tr>
    <tr>
     <td>Update table {tableId} (display form)</td>
     <td>http://server/wmotables/table/update/{tableId} </td>
     <td>GET</td>
     <td>Yes</td>
     <td>TableController</td>
     <td>/WEB-INF/views/tableForm.jsp</td>
    </tr>
    <tr>
     <td>Update table {tableId}</td>
     <td>http://server/wmotables/table/update</td>
     <td>POST</td>
     <td>Yes</td>
     <td>TableController</td>
     <td>redirect to /WEB-INF/views/viewTable.jsp </td>
    </tr>
    <tr>
     <td>Hide table {tableId}</td>
     <td>http://server/wmotables/table/hide</td>
     <td>POST</td>
     <td>Yes</td>
     <td>TableController</td>
     <td>redirect to /WEB-INF/views/viewTable.jsp </td>
    </tr>


   </tbody>
  </table>



  <h5>REST Interface</h5>
  <table>
   <thead>
    <tr>
     <th width="242">Function</th>
     <th width="400">URL</th>
     <th width="61">Request Method</th>
     <th width="85">Authenticate</th>
     <th width="166">Send/Return</th>
    </tr>
   </thead>
   <tbody>
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
     <td>Add table owned by {userId}</td>
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
   </tbody>
  </table>

   <h5>Unique ID</h5>
   <p>Each table is assigned a unique id based on the <a href="http://en.wikipedia.org/wiki/MD5">MD5 hashcode</a> of the table. This means that even if the table only changes by one byte, it will be assigned a different ID. MD5 is a 128-bit (16 byte, 32 hex digits) hash. NOTE: maybe we should use SHA-2 ??</p>
   
<%@ include file="/WEB-INF/views/include/footer.jsp" %>
  </body>
 </html>
