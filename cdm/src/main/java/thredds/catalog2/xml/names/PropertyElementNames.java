package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by
 * the "property" element in the THREDDS InvCatalog
 * specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class PropertyElementNames
{
  private PropertyElementNames(){}

  public static final QName PropertyElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "property" );
  public static final QName PropertyElement_Name
          = new QName( XMLConstants.NULL_NS_URI,
                       "name" );
  public static final QName PropertyElement_Value
          = new QName( XMLConstants.NULL_NS_URI,
                       "value" );
}