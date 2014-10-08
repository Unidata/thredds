/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
  netcdf("application/x-netcdf", null),
  netcdf4("application/x-netcdf4", null),
  ogc_exception("application/vnd.ogc.se_xml", "UTF-8"),
  text("text/plain", "UTF-8"),
  xml("application/xml", "UTF-8");
  // xml_app("text/xml; charset=UTF-8");

  public final static String HEADER = "Content-Type";

  private final String name;
  private final String charset;

  private ContentType(String name, String charset) {
    this.name = name;
    this.charset = charset;
  }

  @Override
  public String toString() {
    return name;
  }

  public String getContentHeader() {
    return (charset == null) ? name : name + "; charset=" + charset;
  }
}
