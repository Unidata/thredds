/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
 */


public class InvDocumentation {
  private String href, title, type, inlineContent;
  private URI uri; // resolved


  /**
   * Constructor.
   *
   * @param href          :  href of documentation, may be null.
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

    content = os.toString();
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