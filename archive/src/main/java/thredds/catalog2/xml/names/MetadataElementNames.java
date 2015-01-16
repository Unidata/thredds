package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by
 * the "metadata" element in the THREDDS InvCatalog
 * specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class MetadataElementNames
{
  private MetadataElementNames(){}

  public static final QName MetadataElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri( ),
                       "metadata");
  public static final QName MetadataElement_Inherited
          = new QName( XMLConstants.NULL_NS_URI,
                       "inherited");
  public static final QName MetadataElement_XlinkTitle
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "title");
  public static final QName MetadataElement_XlinkHref
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "href");
  public static final QName MetadataElement_XlinkType
          = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                       "type");
  public static final QName MetadataElement_metadataType
          = new QName( XMLConstants.NULL_NS_URI,
                       "metadataType");
}