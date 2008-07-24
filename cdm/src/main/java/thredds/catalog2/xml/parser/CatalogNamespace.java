package thredds.catalog2.xml.parser;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public enum CatalogNamespace
{
  CATALOG_1_0( "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0",
               "/resources/thredds/schemas/InvCatalog.1.0.2.xsd",
               "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.2.xsd"),
  CATALOG_0_6( "http://www.unidata.ucar.edu/thredds",
               "/resources/thredds/schemas/InvCatalog.0.6.xsd",
               "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.0.6.xsd"),
  XLINK( "http://www.w3.org/1999/xlink",
         "/resources/thredds/schemas/xlink.xsd",
         // "/resources/schemas/xlink/1.0.0/xlinks.xsd",
         "");

  private String namespaceUri;
  private String resourceName;
  private String systemId;

  CatalogNamespace( String namespaceUri, String resourceName, String systemId )
  {
    this.namespaceUri = namespaceUri;
    this.resourceName = resourceName;
    this.systemId = systemId;
  }

  public String getNamespaceUri()
  {
    return this.namespaceUri;
  }

  public String getResourceName()
  {
    return this.resourceName;
  }

  public String getSystemId()
  {
    return this.systemId;
  }

  public static CatalogNamespace getNamespace( String namespaceUri )
  {
    if ( namespaceUri == null ) return null;
    for ( CatalogNamespace curNs : CatalogNamespace.values() )
    {
      if ( curNs.namespaceUri.equals( namespaceUri ) )
        return curNs;
    }
    return null;
  }

  public static CatalogNamespace getNamespaceBySystemId( String systemId )
  {
    if ( systemId == null ) return null;
    for ( CatalogNamespace curNs : CatalogNamespace.values() )
    {
      if ( curNs.systemId.equals( systemId ) )
        return curNs;
    }
    return null;
  }

  public static InputStream resolveNamespace( String namespaceUri,
                                              String systemId )
          throws IOException
  {
    CatalogNamespace ns = CatalogNamespace.getNamespace( namespaceUri);
    if ( ns == null )
      ns = CatalogNamespace.getNamespaceBySystemId( systemId );

    if ( ns == null )
      return null;

    InputStream inStream = null;
    if ( ns.getResourceName() != null )
      inStream = CatalogNamespace.class.getClassLoader().getResourceAsStream( ns.getResourceName() );
    if ( inStream == null && ns.getSystemId() != null )
      return null;

    return null;
  }

}
