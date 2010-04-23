<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%-- Page for editing the variables in a dataset
     Data (models) passed in to this page:
         dataset      = Dataset to be edited (uk.ac.rdg.resc.ncwms.config.Dataset)
         paletteNames = Names of available colour palettes (Set of String) --%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Edit variables in dataset: ${dataset.id} (admin)</title>
    </head>
    <body>

        <h1>Edit variables in dataset: ${dataset.id} (${dataset.title})</h1>

        <form id="updateVars" action="updateVariables" method="POST">

            <input type="hidden" name="dataset.id" value="${dataset.id}"/>
            
            <table border="1">
                <thead>
                    <tr><th rowspan="2">Variable ID</th><th rowspan="2">Title</th><th colspan="2">Default colour scale range</th><th rowspan="2">Default palette</th><th rowspan="2">Default scaling</th></tr>
                    <tr><th>Min</th><th>Max</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="layer" items="${dataset.layers}">
                        <tr>
                            <td>${layer.id}</td>
                            <td><input type="text" name="${layer.id}.title" value="${layer.title}"/></td>
                            <td><input type="text" name="${layer.id}.scaleMin" value="${layer.approxValueRange.minimum}"/></td>
                            <td><input type="text" name="${layer.id}.scaleMax" value="${layer.approxValueRange.maximum}"/></td>
                            <td>
                                <select name="${layer.id}.palette">
                                    <c:forEach var="paletteName" items="${paletteNames}">
                                        <option value="${paletteName}"<c:if test="${layer.defaultColorPalette.name == paletteName}"> selected="selected"</c:if>>${paletteName}</option>
                                    </c:forEach>
                                </select>
                            </td>
                            <td>
                                <select name="${layer.id}.scaling">
                                    <option value="linear"<c:if test="${not layer.logScaling}"> selected="selected"</c:if>>linear</option>
                                    <option value="logarithmic"<c:if test="${layer.logScaling}"> selected="selected"</c:if>>logarithmic</option>
                                </select>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            
            <br />
            <input type="submit" value="Save" name="save"/>
            <input type="submit" value="Cancel" name="cancel"/>

        </form>

    </body>
</html>
