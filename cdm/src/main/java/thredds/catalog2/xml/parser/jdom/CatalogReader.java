package thredds.catalog2.xml.parser.jdom;

import thredds.catalog2.Catalog;
import org.jdom.Document;
import org.jdom.Element;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogReader
{
  public Catalog readCatalog( Document doc, URI baseURI )
  {
    return readCatalog( doc.getRootElement(), baseURI);
  }

  public Catalog readCatalog( Element catElem, URI baseURI )
  {
    return null;
  }
}
