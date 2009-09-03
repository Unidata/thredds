package thredds.catalog;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.Catalog;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CrawlMotherlodeModelsMemory
{
    public static void main( String[] args )
            throws URISyntaxException,
                   ThreddsXmlParserException,
                   BuilderException
    {
        String modelsCatUrlString = "http://motherlode.ucar.edu:8080/thredds/idd/models.xml";

       // InvCatalogImpl invCatalog = LargeCatalogReadUtils.parseCatalogIntoInvCatalogImpl( modelsCatUrlString );
        CatalogBuilder catalogBuilder = LargeCatalogReadUtils.parseCatalogIntoBuilder( modelsCatUrlString );

       // LargeCatalogReadUtils.measureSize( invCatalog );

       // LargeCatalogReadUtils.measureSize( catalogBuilder );

        Catalog catalog = catalogBuilder.build();

       // LargeCatalogReadUtils.measureSize( catalog );

    }

}
