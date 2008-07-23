package thredds.catalog2.xml.parser;

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
               "http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.2.xsd");

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
    for ( CatalogNamespace curNs : CatalogNamespace.values() )
    {
      if ( curNs.namespaceUri.equals( namespaceUri ) )
        return curNs;
    }
    return null;
  }

  public static CatalogNamespace getNamespaceBySystemId( String systemId )
  {
    for ( CatalogNamespace curNs : CatalogNamespace.values() )
    {
      if ( curNs.systemId.equals( systemId ) )
        return curNs;
    }
    return null;
  }
}
