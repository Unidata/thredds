// $Id: InvMetadata.java,v 1.14 2005/04/27 21:34:09 caron Exp $
/*
 * Copyright 2002 Unidata Program Center/University Corporation for
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

/**
 * A metadata element: structured XML element containing info about associated dataset or catalog.
 *
 * @see InvMetadata for public interface
 *
 * @author john caron
 * @version $Revision: 1.14 $ $Date: 2005/04/27 21:34:09 $
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

  private StringBuffer log = new StringBuffer();
  private boolean debug = false;

  /**
   * Constructor for elements with Xlinks.
   *
   * @param dataset : dataset that contains the metadata.
   * @param xlinkHref : URI of xlink, may be null.
   * @param title : xlink title, may be null.
   * @param type : metadata type
   * @param namespaceURI : namespace URI string of the element, use null for THREDDS
   * @param prefix : namespace prefix of the element, use null for THREDDS
   * @param inherited : if inherited
   * @param isThreddsMetadata : is threddsMetadata
   * @param converter : converter for creating the content object, may be null
   */
  public InvMetadata( InvDataset dataset, String xlinkHref, String title, String type,
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
   * @param dataset : dataset that contains the metadata.
   * @param mtype : metadata type
   * @param namespaceURI : namespace URI string of the element, can use null for THREDDS
   * @param namespacePrefix : namespace prefix of the element, use null for THREDDS
   * @param inherited : if inherited
   * @param isThreddsMetadata : is threddsMetadata
   * @param converter : the metadata converter.
   * @param contentObject : content object.
   */
  public InvMetadata(InvDataset dataset, String mtype, String namespaceURI,
                     String namespacePrefix, boolean inherited, boolean isThreddsMetadata,
                     MetadataConverterIF converter, Object contentObject )  {
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
   * @param dataset : dataset that contains the metadata.
   * @param inherited : if inherited
   * @param tm : content object.
   */
  public InvMetadata(InvDataset dataset, boolean inherited, ThreddsMetadata tm )  {
     this.dataset = dataset;
     this.isInherited = inherited;
     this.isThreddsMetadata = true;
     this.contentObject = tm;
     this.tm = tm;
     init = true;
  }

  /** Return the parent dataset of this InvMetadata */
  public InvDataset getParentDataset() { return this.dataset; }

    /** Get the converter; may be null. */
  public MetadataConverterIF getConverter() { return converter; }

    /** Get the metadata type. */
  public String getMetadataType() { return type; }

    /** Get the namespace. */
  public String getNamespaceURI() { return namespaceURI; }

    /** Get the prefix mapped to the namespace. */
  public String getNamespacePrefix() { return prefix; }

    /** if it has an XLink */
  public boolean hasXlink() { return xlinkHref != null; }

  /** if its a XLink, get the xlink:href as an absolute URI; may be null */
  public String getXlinkHref() {  return xlinkHref; }

  /** if its a XLink, get the xlink:href as an absolute URI; may be null */
  public java.net.URI getXlinkURI() {  return xlinkUri; }

  /** if its a XLink, get the xlink:title attribute */
  public String getXlinkTitle() { return title; }

    /** if it is inherited */
  public boolean isInherited() { return isInherited; }

    /** if this element contains ThreddsMetadata */
  public boolean isThreddsMetadata() { return isThreddsMetadata; }
    /** set if this element contains ThreddsMetadata */
  public void setThreddsMetadata(boolean isThreddsMetadata) { this.isThreddsMetadata = isThreddsMetadata; }
    /** set the namespace URI */
  public void setNamespaceURI(String namespaceURI) {
    this.namespaceURI = namespaceURI;
    hashCode = 0;
  }

  /** get the content object, may be null */
  public Object getContentObject() {
    finish();
    return contentObject;
  }

  /** set the internal ThreddsMetadata; this holds elements from THREDDS namespace */
  public void setThreddsMetadata(ThreddsMetadata tmd) { tm = tmd; }
  /** get the internal ThreddsMetadata, may be null */
  public ThreddsMetadata getThreddsMetadata() { return tm; }


  /** Finish getting the metadata if necessary.
   *  If this is an XLink, this will trigger a read of the href the first time called.
   */
  public void finish() {
    if (init) return;
    init = true;
    if (xlinkHref == null) return;

    xlinkHref = xlinkHref.trim();
    try {
      this.xlinkUri = dataset.getParentCatalog().resolveUri(xlinkHref);
    } catch ( java.net.URISyntaxException e) {
      log.append(" ** Error: Bad URL in metadata href = "+xlinkHref+"\n");
      return;
    }

    // open and read the referenced catalog XML
    try {
      if (converter == null) {
        log.append("  **InvMetadata on = ("+this+"): has no converter\n");
        return;
      }

      contentObject = converter.readMetadataContentFromURL( dataset, xlinkUri);
      if (isThreddsMetadata)
        tm = (ThreddsMetadata) contentObject;

    } catch (java.io.IOException e) {
      log.append("  **InvMetadata on = ("+xlinkUri+"): Exception ("+e.getMessage()+")\n");
      // e.printStackTrace();
    }
  }
  private boolean init = false;

  boolean check(StringBuffer out) {
    boolean isValid = true;

    if (log.length() > 0) {
      isValid = false;
      out.append( log);
    }

    if ((contentObject != null) && (converter != null))
      isValid &= converter.validateMetadataContent(contentObject, out);

    return isValid;
  }

  /** InvMetadata elements with same values are equal. */
   public boolean equals(Object o) {
     if (this == o) return true;
     if (!(o instanceof InvMetadata)) return false;
     return o.hashCode() == this.hashCode();
  }

  /** Override Object.hashCode() to implement equals. */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (null != getNamespaceURI())
        result = 37*result + getNamespaceURI().hashCode();
      if (null != getXlinkHref())
        result = 37*result + getXlinkHref().hashCode();
      if (null != getXlinkTitle())
        result = 37*result + getXlinkTitle().hashCode();
      if (null != getMetadataType())
        result = 37*result + getMetadataType().hashCode();
      result = 37*result + (isInherited() ? 1 : 0);
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

/**
 * $Log: InvMetadata.java,v $
 * Revision 1.14  2005/04/27 21:34:09  caron
 * cleanup DirectoryScanner, InvDatasetScan
 *
 * Revision 1.13  2005/04/20 00:05:36  caron
 * *** empty log message ***
 *
 * Revision 1.12  2004/12/15 00:11:45  caron
 * 2.2.05
 *
 * Revision 1.11  2004/11/30 23:08:45  edavis
 * Add ToDo comment.
 *
 * Revision 1.10  2004/06/09 00:27:25  caron
 * version 2.0a release; cleanup javadoc
 *
 * Revision 1.9  2004/06/04 22:28:44  caron
 * convertTover1; get ver 06 inheritence right
 *
 * Revision 1.8  2004/05/21 05:57:31  caron
 * release 2.0b
 *
 * Revision 1.7  2004/05/20 22:45:52  edavis
 * Add new constructor that takes a contentObject.
 *
 * Revision 1.6  2004/05/13 15:58:00  caron
 * release 2.0a
 *
 * Revision 1.5  2004/05/11 23:30:28  caron
 * release 2.0a
 *
 * Revision 1.4  2004/03/19 20:12:52  caron
 * trim URLs
 *
 * Revision 1.3  2004/03/05 23:35:47  caron
 * rel 1.3.1 javadoc
 *
 * Revision 1.2  2004/02/20 00:49:50  caron
 * 1.3 changes
 *
 */