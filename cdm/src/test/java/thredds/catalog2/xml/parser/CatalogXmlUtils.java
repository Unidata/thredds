package thredds.catalog2.xml.parser;

/**
 * Utility methods for generating catalog XML. 
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogXmlUtils
{
  private CatalogXmlUtils(){}

  public static String getCatalogWithSingleAccessibleDataset()
  {
    return wrapThreddsXmlInCatalog( "  <dataset name='Test1' urlPath='test/test1.nc' serviceName='odap' />" );
  }

  public static String wrapThreddsXmlInCatalog( String threddsXml )
  {
    StringBuilder sb = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'" )
            .append( "         xmlns:xlink='http://www.w3.org/1999/xlink'" )
            .append( "         name='thredds.catalog2.xml.parser.CatalogXmlUtils'" )
            .append( "         version='1.0.2'>\n" )
            .append( "  <service name='all' serviceType='Compound' base='/'>\n" )
            .append( "    <service name='odap' serviceType='OPENDAP' base='/thredds/dodsC/' />\n" )
            .append( "    <service name='wcs' serviceType='WCS' base='/thredds/wcs/' />\n" )
            .append( "    <service name='wms' serviceType='WMS' base='/thredds/wms/' />\n" )
            .append( "  </service>\n" )
            .append(    threddsXml )
            .append( "</catalog>" );

    return sb.toString();
  }

  public static String wrapThreddsXmlInCatalogDataset( String threddsXml )
  {

    StringBuilder sb = new StringBuilder()
            .append( "  <dataset name='container dataset'>\n" )
            .append(      threddsXml )
            .append( "  </dataset>\n" );

    return wrapThreddsXmlInCatalog( sb.toString() );
  }

  public static String wrapThreddsXmlInCatalogDatasetMetadata( String threddsXml )
  {
    return wrapThreddsXmlInCatalogDataset( "<metadata>" + threddsXml + "</metadata>" );
  }

  public static String wrapThreddsXmlInCatalogDatasetMetadataInherited( String threddsXml )
  {
    return wrapThreddsXmlInCatalogDataset( "<metadata inherited='true'>" + threddsXml + "</metadata>" );
  }

}
