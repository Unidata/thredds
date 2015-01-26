package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by the
 * "catalogRef" element in the THREDDS InvCatalog specification
 * (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogRefElementNames
{
  private CatalogRefElementNames(){}

  public static final QName CatalogRefElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "catalogRef" );
  public static final QName CatalogRefElement_XlinkTitle
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "title" );
  public static final QName CatalogRefElement_XlinkHref
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "href" );
  public static final QName CatalogRefElement_XlinkType
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "type" );
}