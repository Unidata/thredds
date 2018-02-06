/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package thredds.util;

/**
 * Content Types (MIME types)
 *
 * @author caron
 * @since 10/20/13
 */
public enum ContentType {
  binary("application/octet-stream", null),
  csv("text/plain", "UTF-8"),
  html("text/html", "UTF-8"),
  jnlp("application/x-java-jnlp-file", null),
  json("application/json", null),
  ncstream("application/octet-stream", null),
  netcdf("application/x-netcdf", null),
  ogc_exception("application/vnd.ogc.se_xml", "UTF-8"),
  png("image/png", null),
  text("text/plain", "UTF-8"),
  xml("application/xml", "UTF-8"),
  xmlwms("text/xml", "iso-8859-1");

  public final static String HEADER = "Content-Type";

  public static ContentType findContentTypeFromFilename(String filename) {
    if (filename.endsWith(".csv"))
      return ContentType.csv;
    else if (filename.endsWith(".html"))
      return ContentType.html;
    else if (filename.endsWith(".xml"))
      return ContentType.xml;
    else if (filename.endsWith(".txt") || filename.endsWith(".log") || filename.endsWith(".out"))
      return ContentType.text;
    else if (filename.endsWith(".nc"))
      return ContentType.netcdf;
    else if (filename.indexOf(".log.") > 0)
      return ContentType.text;

    return  null;
  }

  private final String name;
  private final String charset;

  ContentType(String name, String charset) {
    this.name = name;
    this.charset = charset;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getContentHeader() {
    return (charset == null) ? name : name + ";charset=" + charset;
  }
}
