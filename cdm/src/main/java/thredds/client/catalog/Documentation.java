/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import ucar.nc2.constants.CDM;

import javax.annotation.concurrent.Immutable;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * Client catalog documentation element
 *
 * @author caron
 * @since 1/9/2015
 */
@Immutable
public class Documentation {
  private final String href, title, type, inlineContent;
  private final URI uri; // resolved

  /**
   * Constructor.
   *
   * @param href          : href of documentation, may be null.
   * @param uri           : absolute URL, or null
   * @param title         : Xlink title, may be null.
   * @param type          : user-defined InvDocumentation type
   * @param inlineContent : optional inline contents.
   */
  public Documentation(String href, URI uri, String title, String type, String inlineContent) {
    this.href = href;
    this.uri = uri;
    this.type = type;
    this.inlineContent = inlineContent;

    if (title != null)
      this.title = title;
    else if (uri != null)
      this.title = uri.toString();
    else
      this.title = null;
  }

  public String getType() {
    return type;
  }

  public boolean hasXlink() {
    return uri != null;
  }

  /**
   * if its a XLink, get the absolute URI
   *
   * @return the XLink URI, else null
   */
  public URI getURI() {
    return uri;
  }

  /**
   * if its a XLink, get the title, to display the link to the user.
   *
   * @return the XLink title, else null
   */
  public String getXlinkTitle() {
    return title;
  }

  /**
   * if its a XLink, get the href, to display the link to the user.
   *
   * @return the XLink href, or null
   */
  public String getXlinkHref() {
    return href;
  }

  public String getInlineContent() {
    return inlineContent;
  }


  /**
   * Get inline content as a string, else null if there is none
   *
   * @return inline content as a string, else null
   */
  public String readXlinkContent() throws java.io.IOException {
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

    return new String(os.toByteArray(), CDM.utf8Charset);
  }

  @Override
  public String toString() {
    return "Documentation{" +
            "href='" + href + '\'' +
            ", title='" + title + '\'' +
            ", type='" + type + '\'' +
            ", inlineContent='" + inlineContent + '\'' +
            ", uri=" + uri +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Documentation that = (Documentation) o;

    if (href != null ? !href.equals(that.href) : that.href != null) return false;
    if (inlineContent != null ? !inlineContent.equals(that.inlineContent) : that.inlineContent != null) return false;
    if (title != null ? !title.equals(that.title) : that.title != null) return false;
    if (type != null ? !type.equals(that.type) : that.type != null) return false;
    if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = href != null ? href.hashCode() : 0;
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (inlineContent != null ? inlineContent.hashCode() : 0);
    result = 31 * result + (uri != null ? uri.hashCode() : 0);
    return result;
  }
}
