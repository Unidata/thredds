<%@tag description="Displays a single Layer in the menu" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@attribute name="id" required="true" description="Unique ID for this layer"%>
<%@attribute name="label" required="true" description="Title for this layer"%>
<%@attribute name="server" description="Optional URL to the ncWMS server providing this layer"%>
<json:object>
    <json:property name="id" value="${id}"/>
    <json:property name="label" value="${label}"/>
    <c:if test="${not empty server}">
        <json:property name="server" value="${server}"/>
    </c:if>
</json:object>