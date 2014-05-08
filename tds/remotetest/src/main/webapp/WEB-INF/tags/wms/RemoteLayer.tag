<%@tag description="Displays a single Layer in the menu" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@attribute name="layerName" required="true" description="Full ID of layer within the dataset"%>
<%@attribute name="label" required="true" description="Specifies the title for this layer"%>
<%@attribute name="server" required="true" description="URL to the ncWMS server providing this layer"%>
<%-- A layer on a remote server.  In this case we are unable to detect the readiness
     or error state of the containing dataset --%>
<json:object>
    <json:property name="id" value="${layerName}"/>
    <json:property name="label" value="${label}"/>
    <json:property name="server" value="${server}"/>
</json:object>