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
 * Converts JDOM Element to Objects holding metadata content.
 *
 * @see InvCatalogConvertIF
 * @see InvCatalogFactory
 * @author John Caron
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
   * @throws java.io.IOException on read error
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
  public boolean validateMetadataContent(Object contentObject, StringBuilder out);


}
