// $Id: MetadataConverterIF.java 48 2006-07-12 16:15:40Z caron $
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

/**
 * Converts JDOM Element to Objects holding metadata content.
 *
 * @see InvCatalogConvertIF
 * @see InvCatalogFactory
 * @author John Caron
 * @version $Id: MetadataConverterIF.java 48 2006-07-12 16:15:40Z caron $
 */

public interface MetadataConverterIF {

   /**
    * Create an InvMetadata content object from an org.jdom.Element.
    *
    * @param dataset : the containing dataset
    * @param mdataElement : the <metadata> element
    * @return an object representing the metadata content. The type depends on the metadata handler.
    */
  public Object readMetadataContent( InvDataset dataset, org.jdom.Element mdataElement);

  /**
   * Create an InvMetadata content object from an XML document at a named URL.
   *
   * @param dataset : the containing dataset
   * @param uri : the URI that the XML doc is at.
   * @return an object representing the metadata content. The type depends on the metadata handler.
   *
   * @throws java.io.IOException
   */
  public Object readMetadataContentFromURL( InvDataset dataset, java.net.URI uri) throws java.io.IOException;


   /**
    * Serialize the InvMetadata content object to a org.jdom.Element and add ro the <metadata> element.
    *
    * @param mdataElement : the org.w3c.dom.Element to add to
    * @param contentObject : the InvMetadata content object
    */
  public void addMetadataContent( org.jdom.Element mdataElement, Object contentObject);

  /**
   * Validate internal data structures.
   * @param contentObject : the content object
   * @param out : print errors here
   * @return true if no fatal validation errors.
   */
  public boolean validateMetadataContent(Object contentObject, StringBuffer out);


}
