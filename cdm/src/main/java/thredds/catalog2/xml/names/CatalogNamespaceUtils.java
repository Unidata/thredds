package thredds.catalog2.xml.names;

import javax.xml.namespace.QName;
import javax.xml.XMLConstants;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 *
 */
public class CatalogNamespaceUtils
{
  private CatalogNamespaceUtils() {}

  public static QName getThreddsCatalogElementQualifiedName( String localName ) {
    return new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), localName);
  }

  public static QName getThreddsCatalogAttributeQName( String localName) {
    return new QName( XMLConstants.NULL_NS_URI, localName );
  }
}
