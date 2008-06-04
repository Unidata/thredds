<%@ include file="/WEB-INF/jsp/includes.jsp" %>

<tr>
  <th align="left">${dataset.name}</th>
  <th align="center">${dataset.dataSize}</th>
  <th align="right">${dataset.lastModifiedDate}</th>
  <c:forEach var="curDs" items="${dataset.datasets}">
    <c:import url="dataset.jsp">
      <c:param name="dataset" value="curDs" />
    </c:import>
  </c:forEach>

</tr>