package thredds.catalog;

import ucar.nc2.util.memory.MemoryCounterAgent;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.Catalog;

import java.net.URISyntaxException;

/**
 * Parse a large catalog into 1) a thredds.catalog.InvCatalogImpl and 2) a thredds.catalog2.builder.CatalogBuilder
 * and measure the approximate memory footprint of both.
 *
 * <p>If running in the cdm/ directory, add the following Java command line option:
 * <pre>
 * -javaagent:src/timing/java/ucar/nc2/util/memory/memoryagent.jar
 * </pre>
 *
 * <p>Running this on 26 Aug 2009 against the catalog resulting from this call:
 * <pre>
 * LargeCatalogReadUtils.createExampleRadarServiceCatalogAsString( 9112 )
 * </pre>
 *
 * <p>resulted in:
 * <pre>
 * InvCatalogImpl, shallow=88, deep=64632944
 * CatalogImpl,    shallow=48, deep= 6099312 
 * </pre>
 *
 * @author edavis
 * @since 4.0
 */
public class LargeCatalogReadMemory
{
    public static void main( String[] args )
            throws URISyntaxException,
                   ThreddsXmlParserException,
                   BuilderException
    {
        String catAsString = LargeCatalogReadUtils.createExampleRadarServiceCatalogAsString( 9112 );
        String docBaseUrlString = "http://motherlode.ucar.edu:9080/thredds/radarServer/nexrad/level2/IDD?stn=KARX&time_start=2009-04-07T:00:00:00Z&time_end=2009-05-22T16:44:39Z";

        InvCatalogImpl invCatalog = LargeCatalogReadUtils.parseCatalogIntoInvCatalogImpl( catAsString, docBaseUrlString );
        CatalogBuilder catalogBuilder = LargeCatalogReadUtils.parseCatalogIntoBuilder( catAsString, docBaseUrlString );

        LargeCatalogReadUtils.measureSize( invCatalog);
        LargeCatalogReadUtils.measureSize( catalogBuilder);

        Catalog catalog = catalogBuilder.build();

        LargeCatalogReadUtils.measureSize( catalog);
    }
}
