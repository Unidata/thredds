// $Id$
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.catalog;

import java.io.*;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A documentation object: text, HTML or an Xlink.
 * TODO: XHTML
 *
 * @author john caron
 * @version $Revision$ $Date$
 */


public class InvDocumentation {
  private String href, title, type, inlineContent;
  private URI uri; // resolved


  /**
   * Constructor.
   * @param href :  href of documentation, may be null.
   * @param uri : resolved URL, or null
   * @param title : Xlink title, may be null.
   * @param type : user-defined InvDocumentation type
   * @param inlineContent : optional inline contents.
   */
  public InvDocumentation( String href, URI uri, String title, String type, String inlineContent) {
    this.href = href;
    this.uri = uri;
    this.title = title;
    this.type = type;
    this.inlineContent = inlineContent;

    if ((uri !=null) && (title == null))
      this.title = uri.toString();
  }

  /** user defined type */
  public String getType() { return type; }
  public void setType(String type) {
    this.type = type;
    hashCode = 0;
  }

  /** if it has an XLink */
  public boolean hasXlink() { return uri != null; }

  /** if its a XLink, get the absolute URI */
  public URI getURI() { return uri; }

  /** if its a XLink, get the title, to display the link to the user. */
  public String getXlinkTitle() { return title; }
  public void setXlinkTitle(String title) { this.title = title; }

  /** if its a XLink, get the href, to display the link to the user. */
  public String getXlinkHref() { return href; }
  public void setXlinkHref(String href) throws URISyntaxException {
    this.href = href;
    this.uri = new URI(href);
  }

  /** if its a XLink, get its content. This triggers a URL read the first time. */
  public String getXlinkContent() throws java.net.MalformedURLException, java.io.IOException {
    if (content != null) return content;
    if (uri == null) return "";

    URL url =  uri.toURL();
    InputStream is = url.openStream();
    ByteArrayOutputStream  os = new ByteArrayOutputStream( is.available());

    // copy to string
    byte[] buffer = new byte[1024];
    while (true) {
      int bytesRead = is.read(buffer);
      if (bytesRead == -1) break;
      os.write(buffer, 0, bytesRead);
    }
    is.close();

    content = os.toString();
    return content;
  }
  private String content = null;

  /** Get inline content as a string, else null if there is none*/
  public String getInlineContent() { return inlineContent; }
  public void setInlineContent(String s) {
    this.inlineContent = s;
    hashCode = 0;
  }

  /** string representation */
  public String toString() {
    if (hasXlink())
      return "<"+uri+"> <"+title+"> <"+type+">" + ((content == null) ? "" : " <"+content+">");
    else
      return "<"+inlineContent+">";
  }

  /** InvDocumentation elements with same values are equal. */
   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof InvDocumentation)) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (null != getURI())
        result = 37*result + getURI().hashCode();
      if (null != getInlineContent())
        result = 37*result + getInlineContent().hashCode();
      if (null != getXlinkTitle())
        result = 37*result + getXlinkTitle().hashCode();
      if (null != getType())
        result = 37*result + getType().hashCode();
      hashCode = result;
    }
    return hashCode;
  }
  private volatile int hashCode = 0; // Bloch, item 8

  // for bean editing
  public InvDocumentation() {}
  static public String hiddenProperties() { return "inlineContent type URI xlinkContent"; }
  static public String editableProperties() { return "xlinkTitle xlinkHref"; }
}