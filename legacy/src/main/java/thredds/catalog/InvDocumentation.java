/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

import ucar.nc2.constants.CDM;

import java.io.*;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A documentation object: text, HTML or an Xlink.
 * TODO: XHTML
 *
 * @author john caron
 */


public class InvDocumentation {
  private String href, title, type, inlineContent;
  private URI uri; // resolved


  /**
   * Constructor.
   *
   * @param href          : href of documentation, may be null.
   * @param uri           : resolved URL, or null
   * @param title         : Xlink title, may be null.
   * @param type          : user-defined InvDocumentation type
   * @param inlineContent : optional inline contents.
   */
  public InvDocumentation(String href, URI uri, String title, String type, String inlineContent) {
    this.href = href;
    this.uri = uri;
    this.title = title;
    this.type = type;
    this.inlineContent = inlineContent;

    if ((uri != null) && (title == null))
      this.title = uri.toString();
  }

  /**
   * @return documentation type
   */
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
    hashCode = 0;
  }

  /**
   * @return true if it has an XLink
   */
  public boolean hasXlink() {
    return uri != null;
  }

  /**
   * if its a XLink, get the absolute URI
   * @return the XLink URI, else null
   */
  public URI getURI() {
    return uri;
  }

  /**
   * if its a XLink, get the title, to display the link to the user.
   * @return the XLink title, else null
   */
  public String getXlinkTitle() {
    return title;
  }

  public void setXlinkTitle(String title) {
    this.title = title;
  }

  /**
   * if its a XLink, get the href, to display the link to the user.
   * @return the XLink href, or null
   */
  public String getXlinkHref() {
    return href;
  }

  public void setXlinkHref(String href) throws URISyntaxException {
    this.href = href;
    this.uri = new URI(href);
  }

  /**
   * if its a XLink, get its content. This triggers a URL read the first time.
   * @return the XLink content
   * @throws java.io.IOException on read error
   */
  public String getXlinkContent() throws java.io.IOException {
    if (content != null) return content;
    if (uri == null) return "";

    URL url = uri.toURL();
    InputStream is = url.openStream();
    ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());

    // copy to string
    byte[] buffer = new byte[1024];
    while (true) {
      int bytesRead = is.read(buffer);
      if (bytesRead == -1) break;
      os.write(buffer, 0, bytesRead);
    }
    is.close();

    content = new String(os.toByteArray(), CDM.utf8Charset);
    return content;
  }

  private String content = null;

  /**
   * Get inline content as a string, else null if there is none
   * @return  inline content as a string, else null
   */
  public String getInlineContent() {
    return inlineContent;
  }

  public void setInlineContent(String s) {
    this.inlineContent = s;
    hashCode = 0;
  }

  /**
   * string representation
   */
  public String toString() {
    if (hasXlink())
      return "<" + uri + "> <" + title + "> <" + type + ">" + ((content == null) ? "" : " <" + content + ">");
    else
      return "<" + inlineContent + ">";
  }

  /**
   * InvDocumentation elements with same values are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvDocumentation)) return false;
    return o.hashCode() == this.hashCode();
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (null != getURI())
        result = 37 * result + getURI().hashCode();
      if (null != getInlineContent())
        result = 37 * result + getInlineContent().hashCode();
      if (null != getXlinkTitle())
        result = 37 * result + getXlinkTitle().hashCode();
      if (null != getType())
        result = 37 * result + getType().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0; // Bloch, item 8

  // for bean editing
  public InvDocumentation() {
  }

  static public String hiddenProperties() {
    return "inlineContent type URI xlinkContent";
  }

  static public String editableProperties() {
    return "xlinkTitle xlinkHref";
  }
}