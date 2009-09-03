package thredds.catalog;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.stax.StaxThreddsXmlParser;

import java.net.URISyntaxException;
import java.net.URI;
import java.io.StringReader;

import ucar.nc2.util.memory.MemoryCounterAgent;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class LargeCatalogReadUtils
{
    private LargeCatalogReadUtils() {}

    public static String createExampleRadarServiceCatalogAsString( int numDatasets )
    {
        StringBuilder sb = new StringBuilder();
        sb      .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
                .append( "<catalog xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'\n")
                .append( "         xmlns:xlink='http://www.w3.org/1999/xlink'\n")
                .append( "         name='Radar Level2 datasets in near real time'\n")
                .append( "         version='1.0.1'>\n" )
                .append( "\n" )
                .append( "  <service name='OPENDAP' serviceType='OPENDAP' base='/thredds/dodsC/nexrad/level2/IDD/'/>\n" )
                .append( "  <dataset name='RadarLevel2 datasets for available stations and times' collectionType='TimeSeries'\n")
                .append( "           ID='accept=xml&amp;stn=KARX&amp;time_start=2009-04-07T00:00:00Z&amp;time_end=2009-05-22T16:44:39Z'>\n" )
                .append( "    <metadata inherited='true'>\n" )
                .append( "      <dataType>Radial</dataType>\n" )
                .append( "      <dataFormat>NEXRAD2</dataFormat>\n" )
                .append( "      <serviceName>OPENDAP</serviceName>\n" )
                .append( "\n" )
                .append( "    </metadata>\n" )
                .append( "\n" );

        for ( int i = 0; i < numDatasets - 1; i++ )
        {
            sb      .append( "      <dataset name='Level2_KARX_20090522_1518.ar2v' ID='" ).append( 1209322007 + i ).append( "'\n" )
                    .append( "               urlPath='KARX/20090522/Level2_KARX_20090522_1518.ar2v'>\n" )
                    .append( "        <date type='start of ob'>2009-05-22T15:18:00</date>\n" )
                    .append( "      </dataset>" );
        }

        sb      .append( "      <dataset name=\"Level2_KARX_20090407_0007.ar2v\" ID=\"1616941921\"\n")
                .append( "        urlPath=\"KARX/20090407/Level2_KARX_20090407_0007.ar2v\">\n" )
                .append( "        <date type=\"start of ob\">2009-04-07T00:07:00</date>\n" )
                .append( "      </dataset>\n");


                   sb      .append( "    </dataset>\n" )
                .append( "</catalog>" );

        return sb.toString();
    }

    public static InvCatalogImpl parseCatalogIntoInvCatalogImpl( String docBaseUriString )
            throws URISyntaxException
    {
        InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );

        return fac.readXML( docBaseUriString );
    }

    public static InvCatalogImpl parseCatalogIntoInvCatalogImpl( String docAsString, String docBaseUriString )
            throws URISyntaxException
    {
        InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );

        return fac.readXML( docAsString, new URI( docBaseUriString ) );
    }

    public static CatalogBuilder parseCatalogIntoBuilder( String docAsString, String docBaseUriString )
            throws URISyntaxException, ThreddsXmlParserException
    {
        URI docBaseUri = new URI( docBaseUriString );

        ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
        return cp.parseIntoBuilder( new StringReader( docAsString ), docBaseUri );
    }
    
    public static CatalogBuilder parseCatalogIntoBuilder( String docUriString )
            throws URISyntaxException, ThreddsXmlParserException
    {
        URI docUri = new URI( docUriString );

        ThreddsXmlParser cp = StaxThreddsXmlParser.newInstance();
        return cp.parseIntoBuilder( docUri );
    }

    public static void measureSize( Object o )
    {
        long memShallow = MemoryCounterAgent.sizeOf( o );
        long memDeep = MemoryCounterAgent.deepSizeOf( o );
        System.out.printf( "%s, shallow=%d, deep=%d%n",
                           o.getClass().getSimpleName(),
                           memShallow, memDeep );
    }

}
