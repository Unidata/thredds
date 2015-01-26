package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by
 * the "catalog" element in the THREDDS InvCatalog
 * specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogElementNames
{
  private CatalogElementNames(){}

  public static final QName CatalogElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "catalog" );
  public static final QName CatalogElement_Name
          = new QName( XMLConstants.NULL_NS_URI,
                       "name" );
  public static final QName CatalogElement_Expires
          = new QName( XMLConstants.NULL_NS_URI,
                       "expires" );
  public static final QName CatalogElement_LastModified
          = new QName( XMLConstants.NULL_NS_URI,
                       "lastModified" );
  public static final QName CatalogElement_Version
          = new QName( XMLConstants.NULL_NS_URI,
                       "version" );
}
