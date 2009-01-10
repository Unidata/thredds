<%@tag description="Displays a dataset as a set of layers in the menu. The dataset must be hosted on this server." pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/tld/wms/MenuMaker" prefix="menu"%>
<%@attribute name="label" description="Optional: can be used to override the title of the dataset"%>
<%@attribute name="dataset" required="true" type="ucar.nc2.dt.GridDataset" description="The dataset object to display"%> 

<%-- We only display the dataset if it is ready, otherwise the call to dataset.layers
     will fail --%>

<%-- ORIGINAL

<c:set var="title" value="${dataset.title}"/>
<c:if test="${not empty label}">
    <c:set var="title" value="${label}"/>
</c:if>
<c:if test="${dataset.ready}">
    <menu:folder label="${title}">
        <c:forEach items="${dataset.layers}" var="layer">
            <menu:layer id="${layer.layerName}" label="${layer.title}"/>
        </c:forEach>
    </menu:folder>
</c:if>

--%>

<c:set var="title" value="${dataset.title}"/>
<c:if test="${not empty label}">
    <c:set var="title" value="${label}"/>
</c:if>
<menu:folder label="${title}">
    <c:forEach items="${layers}" var="layer">
        <menu:layer id="${layer.layerName}" label="${layer.title}"/>
    </c:forEach>
</menu:folder>
