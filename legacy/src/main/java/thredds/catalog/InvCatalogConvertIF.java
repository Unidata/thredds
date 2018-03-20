/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.catalog;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Converts JDOM tree to Inventory Catalog Objects.
 *
 * @see InvCatalogFactory
 * @author John Caron
 */

public interface InvCatalogConvertIF {

  /**
   * Create an InvCatalogImpl from a parsed document tree.
   *
   * @param fac use this factory
   * @param doc : a parsed document tree.
   * @param uri : the URI for the document.
   * @return an InvCatalog object
   */
  public InvCatalogImpl parseXML( InvCatalogFactory fac, org.jdom2.Document doc, java.net.URI uri);

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param catalog write this catalog
   * @param os write to this OutputStream
   * @throws java.io.IOException on error
   */
  public void writeXML(InvCatalogImpl catalog, java.io.OutputStream os) throws java.io.IOException;
  public void writeXML(InvCatalogImpl catalog, OutputStream os, boolean raw) throws IOException;
}