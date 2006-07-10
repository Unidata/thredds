// $Id: InvCatalogConvertIF.java,v 1.6 2006/01/17 01:46:52 caron Exp $
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

import java.io.OutputStream;
import java.io.IOException;

/**
 * Converts JDOM tree to Inventory Catalog Objects.
 *
 * @see InvCatalogFactory
 * @author John Caron
 * @version $Id: InvCatalogConvertIF.java,v 1.6 2006/01/17 01:46:52 caron Exp $
 */

public interface InvCatalogConvertIF {

  /**
   * Create an InvCatalogImpl from a parsed document tree.
   *
   * @param doc : a parsed document tree.
   * @param uri : the URI for the document.
   * @return an InvCatalog object
   */
  public InvCatalogImpl parseXML( InvCatalogFactory fac, org.jdom.Document doc, java.net.URI uri);

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os write to this OutputStream
   * @throws java.io.IOException on error
   */
  public void writeXML(InvCatalogImpl catalog, java.io.OutputStream os) throws java.io.IOException;
  public void writeXML(InvCatalogImpl catalog, OutputStream os, boolean raw) throws IOException;



  /**
   * Error messages are written to this StringBuffer. These are considered non-fatal.
   *
  public void setErrMessageBuffers( StringBuffer errMessages, StringBuffer warnMessages);


  public void setMetadataHash( java.util.HashMap metadataConverters);

  /**
   * Register factory for reading metadata objects of a given type.
   *
   * @param type : use Factory for this MetadataType
   * @param factory : use this factory for this type
   *
  public void registerMetadataConverter(MetadataType type, MetadataConverterIF factory);
  */
}