package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by
 * the "service" element in the THREDDS InvCatalog
 * specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceElementNames
{
  private ServiceElementNames(){}

  public static final QName ServiceElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "service" );

  public static final QName ServiceElement_Name
          = new QName( XMLConstants.NULL_NS_URI,
                       "name" );
  public static final QName ServiceElement_Base
          = new QName( XMLConstants.NULL_NS_URI,
                       "base" );
  public static final QName ServiceElement_ServiceType
          = new QName( XMLConstants.NULL_NS_URI,
                       "serviceType" );
  public static final QName ServiceElement_Description
          = new QName( XMLConstants.NULL_NS_URI,
                       "desc" );
  public static final QName ServiceElement_Suffix
          = new QName( XMLConstants.NULL_NS_URI,
                       "suffix" );
}