---
title: Read over HTTP
last_updated: 2018-10-10
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: ncj_read_over_http.html
---

## Remote CDM files from an HTTP Server

`NetcdfFile` can open remote files served over HTTP, if the HTTP server allows _range requests_ for a resource, indicated by the [Accept-Ranges](http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.5){:target="_blank"} header (optional), and returns a [Content-Length](http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.13){:target="_blank"} header (required).
Therefore, the server must not return the data as \"chunked\" transfer-coding, such as gzip.
Note this must happen for both the `GET` and the `HEAD` HTTP requests.

Performance will be strongly affected by the file format and the data access pattern.
This is because each request for data will incur one or more file access requests.
Each file access request becomes an HTTP request, incurring the cost of one round-trip message to the server.
Generally, the `netcdf-3` format (not `netcdf-4`) is most likely to give good performance, especially when reading large amounts of non-record data per request.
Use OPeNDAP or CDMRemote for more consistent performance.

The THREDDS Data Server enables range requests by default.
You may have to configure other HTTP servers.
Following are our notes on how to do so.

### Enabling Accept-Ranges response header in Apache HTTP Server

1. Make sure `mod_headers` is either compiled into the httpd binary (from the commandline run: $APACHE_HOME/bin/httpd -l & look for mod_headers) or loaded as a DSO module (look in httpd.conf for mod_headers).
2. Add the following directive to apply to the web content you need (e.g., it can be at the server-level, vhost-level, `<Directory></Directory>`, and `.htaccess`): `Header set Accept-Ranges bytes`
   Example:
   ~~~bash
   <Directory "/opt/local_htdocs">
      Options FollowSymLinks Indexes
      AllowOverride None
      Order allow,deny
      Allow from all
      Header set Accept-Ranges bytes
   </Directory>
   ~~~
3. Restart apache
4. (optional): you can test the result by using the Live Http Headers extension in firefox.

For more information see <http://httpd.apache.org/docs/2.0/mod/mod_headers.html>{:target="_blank"}

### Troubleshooting
1. Add [Live HTTP Headers](http://livehttpheaders.mozdev.org/){:target="_blank"} plug-in to Firefox.
2. Try a URL in firefox:
   `https://thredds.ucar.edu/thredds/fileServer/nws/metar/ncdecoded/files/Surface_METAR_20181010_0000.nc`
   (this file is probably timed off, but you can go to <https://thredds.ucar.edu/thredds/catalog/nws/metar/ncdecoded/files/catalog.html>{:target="_blank"} to find a current one)
   You should get something like the following in the Live Headers window:
   ~~~bash
   GET /thredds/fileServer/station/metar/Surface_METAR_20090209_0000.nc HTTP/1.1
      Host: motherlode.ucar.edu:8080
      User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.0.6) Gecko/2009011913 Firefox/3.0.6 (.NET CLR 3.5.30729)
      Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
      Accept-Language: en-us,en;q=0.5
      Accept-Encoding: gzip,deflate
      Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
      Keep-Alive: 300
      Connection: keep-alive
   HTTP/1.x 200 OK
      Server: Apache-Coyote/1.1
      Last-Modified: Mon, 09 Feb 2009 20:56:13 GMT
      Accept-Ranges: bytes
      Content-Type: application/x-netcdf
      Content-Length: 44414600
      Date: Mon, 09 Feb 2009 20:56:14 GMT
   ~~~
   In particular you should see these two headers:
   ~~~bash
    Accept-Ranges: bytes

    Content-Length: 12345
   ~~~
3. Try the same thing with the server you want to test.