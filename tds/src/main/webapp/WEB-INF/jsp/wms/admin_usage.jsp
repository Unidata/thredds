<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql"  prefix="sql"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt"%>

<%-- prepare the SQL queries that we will use on this page.
     Note the cunning use of nullif() to allow us to count
     total GetMap requests and cache hits in the same query.
     Also we use count(1) instead of count(*) to increase performance --%>
<sql:query var="numEntries" dataSource="${usageLogger.dataSource}">
select count(1) as count from usage_log
</sql:query>
<sql:query var="getMapRequestsByClient" dataSource="${usageLogger.dataSource}">
select client_hostname, count(1) as count, count(nullif(used_cache, false)) as used_cache
from usage_log where wms_operation = 'GetMap' group by client_hostname order by count desc
</sql:query>
<sql:query var="getMapRequestsByUserAgent" dataSource="${usageLogger.dataSource}">
select client_user_agent, count(1) as count, count(nullif(used_cache, false)) as used_cache
from usage_log where wms_operation = 'GetMap' group by client_user_agent order by count desc
</sql:query>
<sql:query var="getMapRequestsByReferrer" dataSource="${usageLogger.dataSource}">
select client_referrer, count(1) as count, count(nullif(used_cache, false)) as used_cache
from usage_log where wms_operation = 'GetMap' group by client_referrer order by count desc
</sql:query>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Usage Monitor</title>
    </head>
    <body>
    
    <h1>ncWMS Usage Monitor</h1>
    <p>This page contains summary usage information.  If you need more detail you can
    download the whole usage log (containing ${numEntries.rows[0].count} entries)
    in CSV format (e.g. for Microsoft Excel) by clicking <a href="downloadUsageLog">here</a>.</p>
    
    <h2>GetMap requests by client</h2>
    <table border="1">
        <tr>
            <th>Client hostname/IP</th>
            <th>Country of origin</th>
            <th>Number of GetMap requests</th>
            <th>Number of cache hits</th>
            <th>Percentage cache hits</th>
        </tr>
        <c:set var="total" value="0"/>
        <c:set var="cache_hits" value="0"/>
        <c:forEach var="row" items="${getMapRequestsByClient.rows}">
            <tr>
                <td>${row.client_hostname}</td>
                <td>
                    <a href="http://www.hostip.info">
                        <img height=15" src="http://api.hostip.info/flag.php?ip=${row.client_hostname}" alt="IP Address Lookup">
                    </a>
                </td>
                <td>${row.count}</td>
                <td>${row.used_cache}</td>
                <td><fmt:formatNumber value="${row.used_cache / row.count}" type="percent" minFractionDigits="2"/></td>
                <c:set var="total" value="${total + row.count}"/>
                <c:set var="cache_hits" value="${cache_hits + row.used_cache}"/>
            </tr>
        </c:forEach>
        <tr>
            <th>TOTAL (check)</th>
            <th>${total}</th>
            <th>${cache_hits}</th>
            <th><fmt:formatNumber value="${cache_hits / total}" type="percent" minFractionDigits="2"/></th>
        </tr>
    </table>
    
    <h2>GetMap requests by user agent</h2>
    <table border="1">
        <tr>
            <th>Client user agent (browser)</th>
            <th>Number of GetMap requests</th>
            <th>Number of cache hits</th>
            <th>Percentage cache hits</th>
        </tr>
        <c:set var="total" value="0"/>
        <c:set var="cache_hits" value="0"/>
        <c:forEach var="row" items="${getMapRequestsByUserAgent.rows}">
            <tr>
                <td>${row.client_user_agent}</td>
                <td>${row.count}</td>
                <td>${row.used_cache}</td>
                <td><fmt:formatNumber value="${row.used_cache / row.count}" type="percent" minFractionDigits="2"/></td>
                <c:set var="total" value="${total + row.count}"/>
                <c:set var="cache_hits" value="${cache_hits + row.used_cache}"/>
            </tr>
        </c:forEach>
        <tr>
            <th>TOTAL (check)</th>
            <th>${total}</th>
            <th>${cache_hits}</th>
            <th><fmt:formatNumber value="${cache_hits / total}" type="percent" minFractionDigits="2"/></th>
        </tr>
    </table>
    
    <h2>GetMap requests by referrer</h2>
    <table border="1">
        <tr>
            <th>Referrer</th>
            <th>Number of GetMap requests</th>
            <th>Number of cache hits</th>
            <th>Percentage cache hits</th>
        </tr>
        <c:set var="total" value="0"/>
        <c:set var="cache_hits" value="0"/>
        <c:forEach var="row" items="${getMapRequestsByReferrer.rows}">
            <tr>
                <td>${row.client_referrer}</td>
                <td>${row.count}</td>
                <td>${row.used_cache}</td>
                <td><fmt:formatNumber value="${row.used_cache / row.count}" type="percent" minFractionDigits="2"/></td>
                <c:set var="total" value="${total + row.count}"/>
                <c:set var="cache_hits" value="${cache_hits + row.used_cache}"/>
            </tr>
        </c:forEach>
        <tr>
            <th>TOTAL (check)</th>
            <th>${total}</th>
            <th>${cache_hits}</th>
            <th><fmt:formatNumber value="${cache_hits / total}" type="percent" minFractionDigits="2"/></th>
        </tr>
    </table>

    
    </body>
</html>
