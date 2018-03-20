/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

/**
 * Converts JDOM Element to Objects holding metadata content.
 *
 * @see InvCatalogConvertIF
 * @see InvCatalogFactory
 * @author John Caron
 */

public interface MetadataConverterIF {

   /**
    * Create an InvMetadata content object from an org.jdom2.Element.
    *
    * @param dataset : the containing dataset
    * @param mdataElement : the <metadata> element
    * @return an object representing the metadata content. The type depends on the metadata handler.
    */
  public Object readMetadataContent( InvDataset dataset, org.jdom2.Element mdataElement);

  /**
   * Create an InvMetadata content object from an XML document at a named URL.
   *
   * @param dataset : the containing dataset
   * @param uri : the URI that the XML doc is at.
   * @return an object representing the metadata content. The type depends on the metadata handler.
   *
   * @throws java.io.IOException on read error
   */
  public Object readMetadataContentFromURL( InvDataset dataset, java.net.URI uri) throws java.io.IOException;


   /**
    * Serialize the InvMetadata content object to a org.jdom2.Element and add ro the <metadata> element.
    *
    * @param mdataElement : the org.w3c.dom.Element to add to
    * @param contentObject : the InvMetadata content object
    */
  public void addMetadataContent( org.jdom2.Element mdataElement, Object contentObject);

  /**
   * Validate internal data structures.
   * @param contentObject : the content object
   * @param out : print errors here
   * @return true if no fatal validation errors.
   */
  public boolean validateMetadataContent(Object contentObject, StringBuilder out);


}
