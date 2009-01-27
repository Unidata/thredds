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

/**
 * A metadata element: structured XML element containing info about associated dataset or catalog.
 *
 * @author john caron
 * @see InvMetadata for public interface
 */

public class InvMetadata {
  private InvDataset dataset;
  private String title, type;
  private String xlinkHref;
  private java.net.URI xlinkUri = null;
  private String namespaceURI, prefix;
  private boolean isInherited;
  private boolean isThreddsMetadata = true;
  private MetadataConverterIF converter = null;
  private Object contentObject = null;
  private ThreddsMetadata tm = null;

  private StringBuilder log = new StringBuilder();

  /**
   * Constructor for elements with Xlinks.
   *
   * @param dataset           : dataset that contains the metadata.
   * @param xlinkHref         : URI of xlink, may be null.
   * @param title             : xlink title, may be null.
   * @param type              : metadata type
   * @param namespaceURI      : namespace URI string of the element, use null for THREDDS
   * @param prefix            : namespace prefix of the element, use null for THREDDS
   * @param inherited         : if inherited
   * @param isThreddsMetadata : is threddsMetadata
   * @param converter         : converter for creating the content object, may be null
   */
  public InvMetadata(InvDataset dataset, String xlinkHref, String title, String type,
                     String namespaceURI, String prefix, boolean inherited, boolean isThreddsMetadata,
                     MetadataConverterIF converter) {
    this.dataset = dataset;
    this.xlinkHref = xlinkHref;

    this.title = title;
    this.type = type;
    this.namespaceURI = namespaceURI;
    this.prefix = prefix;
    this.isInherited = inherited;
    this.isThreddsMetadata = isThreddsMetadata;
    this.converter = converter;
  }

  /**
   * Constructor for elements with inline content.
   *
   * @param dataset           : dataset that contains the metadata.
   * @param mtype             : metadata type
   * @param namespaceURI      : namespace URI string of the element, can use null for THREDDS
   * @param namespacePrefix   : namespace prefix of the element, use null for THREDDS
   * @param inherited         : if inherited
   * @param isThreddsMetadata : is threddsMetadata
   * @param converter         : the metadata converter.
   * @param contentObject     : content object.
   */
  public InvMetadata(InvDataset dataset, String mtype, String namespaceURI,
                     String namespacePrefix, boolean inherited, boolean isThreddsMetadata,
                     MetadataConverterIF converter, Object contentObject) {
    this.dataset = dataset;
    this.type = mtype;
    this.namespaceURI = namespaceURI;
    this.prefix = namespacePrefix;
    this.isInherited = inherited;
    this.isThreddsMetadata = isThreddsMetadata;
    this.converter = converter;
    this.contentObject = contentObject;
    if (isThreddsMetadata)
      tm = (ThreddsMetadata) contentObject;
    init = true;
  }

  /**
   * Constructor using an existing ThreddsMetadata object.
   *
   * @param dataset   : dataset that contains the metadata.
   * @param inherited : if inherited
   * @param tm        : content object.
   */
  public InvMetadata(InvDataset dataset, boolean inherited, ThreddsMetadata tm) {
    this.dataset = dataset;
    this.isInherited = inherited;
    this.isThreddsMetadata = true;
    this.contentObject = tm;
    this.tm = tm;
    init = true;
  }

  /**
   * Get the parent dataset of this InvMetadata
   * @return the parent dataset of this InvMetadata
   */
  public InvDataset getParentDataset() {
    return this.dataset;
  }

  /**
   * Get the converter; may be null.
   * @return the converter or null
   */
  public MetadataConverterIF getConverter() {
    return converter;
  }

  /**
   * Get the metadata type.
   * @return the metadata type.
   */
  public String getMetadataType() {
    return type;
  }

  /**
   * Get the namespace.
   * @return the namespace.
   */
  public String getNamespaceURI() {
    return namespaceURI;
  }

  /**
   * Get the prefix mapped to the namespace.
   * @return the prefix mapped to the namespace.
   */
  public String getNamespacePrefix() {
    return prefix;
  }

  /**
   * if it has an XLink
   * @return true if it has an XLink
   */
  public boolean hasXlink() {
    return xlinkHref != null;
  }

  /**
   * if its a XLink, get the xlink:href String
   * @return the xlink:href or null
   */
  public String getXlinkHref() {
    return xlinkHref;
  }

  /**
   * if its a XLink, get the xlink:href as an absolute URI; may be null
   * @return the xlink:href as an absolute URI, or null
   */
  public java.net.URI getXlinkURI() {
    return xlinkUri;
  }

  /**
   * if its a XLink, get the xlink:title attribute
   * @return xlink:title or null
   */
  public String getXlinkTitle() {
    return title;
  }

  /**
   * if it is inherited
   * @return true if it is inherited
   */
  public boolean isInherited() {
    return isInherited;
  }

  /**
   * if this element contains ThreddsMetadata
   * @return true if this element contains ThreddsMetadata
   */
  public boolean isThreddsMetadata() {
    return isThreddsMetadata;
  }

  /**
   * set if this element contains ThreddsMetadata
   * @param isThreddsMetadata true if this element contains ThreddsMetadata
   */
  public void setThreddsMetadata(boolean isThreddsMetadata) {
    this.isThreddsMetadata = isThreddsMetadata;
  }

  /**
   * set the namespace URI
   * @param namespaceURI set the namespace URI
   */
  public void setNamespaceURI(String namespaceURI) {
    this.namespaceURI = namespaceURI;
    hashCode = 0;
  }

  /**
   * get the content object, may be null
   * @return the content object, or null
   */
  public Object getContentObject() {
    finish();
    return contentObject;
  }

  /**
   * set the internal ThreddsMetadata; this holds elements from THREDDS namespace
   * @param tmd the internal ThreddsMetadata object
   */
  public void setThreddsMetadata(ThreddsMetadata tmd) {
    tm = tmd;
  }

  /**
   * get the internal ThreddsMetadata, if isThreddsMetadata() is true
   * @return  the internal ThreddsMetadata, or null
   */
  public ThreddsMetadata getThreddsMetadata() {
    return tm;
  }


  /**
   * Finish getting the metadata if necessary.
   * If this is an XLink, this will trigger a read of the href the first time called.
   */
  public void finish() {
    if (init) return;
    init = true;
    if (xlinkHref == null) return;

    xlinkHref = xlinkHref.trim();
    try {
      this.xlinkUri = dataset.getParentCatalog().resolveUri(xlinkHref);
    } catch (java.net.URISyntaxException e) {
      log.append(" ** Error: Bad URL in metadata href = ").append(xlinkHref).append("\n");
      return;
    }

    // open and read the referenced catalog XML
    try {
      if (converter == null) {
        log.append("  **InvMetadata on = (").append(this).append("): has no converter\n");
        return;
      }

      contentObject = converter.readMetadataContentFromURL(dataset, xlinkUri);
      if (isThreddsMetadata)
        tm = (ThreddsMetadata) contentObject;

    } catch (java.io.IOException e) {
      log.append("  **InvMetadata on = (").append(xlinkUri).append("): Exception (").append(e.getMessage()).append(")\n");
      // e.printStackTrace();
    }
  }

  private boolean init = false;

  boolean check(StringBuilder out) {
    boolean isValid = true;

    if (log.length() > 0) {
      isValid = false;
      out.append(log);
    }

    if ((contentObject != null) && (converter != null))
      isValid &= converter.validateMetadataContent(contentObject, out);

    return isValid;
  }

  /**
   * InvMetadata elements with same values are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvMetadata)) return false;
    return o.hashCode() == this.hashCode();
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (null != getNamespaceURI())
        result = 37 * result + getNamespaceURI().hashCode();
      if (null != getXlinkHref())
        result = 37 * result + getXlinkHref().hashCode();
      if (null != getXlinkTitle())
        result = 37 * result + getXlinkTitle().hashCode();
      if (null != getMetadataType())
        result = 37 * result + getMetadataType().hashCode();
      result = 37 * result + (isInherited() ? 1 : 0);
      // if (isThreddsMetadata)
      //  result = 37*result + getThreddsMetadata().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0; // Bloch, item 8

  /** String representation
   public String toString() {
   return " xlink = "+getXlinkHref()+" mtype= "+type+" namespaceURI="+namespaceURI
   +" isThredds= "+isThreddsMetadata  +" inherit= "+isInherited();
   } */

}