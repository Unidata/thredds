package thredds.catalog2.xml.names;

import thredds.catalog2.xml.names.CatalogNamespace;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * XML qualified names for the elements and attributes defined by
 * the "access" element in the THREDDS InvCatalog
 * specification (version 1.0.2).
 *
 * @author edavis
 * @since 4.0
 */
public class AccessElementNames
{
  private AccessElementNames(){}

  public static final QName AccessElement
          = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                       "access" );
  public static final QName AccessElement_ServiceName
          = new QName( XMLConstants.NULL_NS_URI,
                       "serviceName" );
  public static final QName AccessElement_UrlPath
          = new QName( XMLConstants.NULL_NS_URI,
                       "urlPath" );
  public static final QName AccessElement_DataFormat
          = new QName( XMLConstants.NULL_NS_URI,
                       "dataFormat" );
}