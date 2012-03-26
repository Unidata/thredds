package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import javax.xml.stream.XMLStreamException;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.Collection;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.CatalogXmlUtils;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog.DataFormatType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
//@RunWith(Parameterized.class)
public class ParseThreddsMetadataCreatedDate
{
//    @Parameterized.Parameters
//    public static Collection<Object[]> junk()
//    {
//
//    }

    @Test
    public void parseDataFormat()
            throws URISyntaxException,
                   XMLStreamException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseDataFormat.xml";

        String mdXml = "<dataFormat>NEXRAD2</dataFormat>";

        parseDataFormatHelper( docBaseUriString, mdXml );
    }

    @Test
    public void parseDataFormatWrapped()
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseDataFormatWrapped.xml";
        String mdXml = "<metadata><dataFormat>NEXRAD2</dataFormat></metadata>";

        parseDataFormatHelper( docBaseUriString, mdXml );
    }

    @Test
    public void parseDataFormatInherited()
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseDataFormatInherited.xml";
        String mdXml = "<metadata inherited='true'><dataFormat>NEXRAD2</dataFormat></metadata>";

        parseDataFormatHelper( docBaseUriString, mdXml );
    }

    private void assertCreatDateAsExpected( ThreddsMetadataBuilder.DatePointBuilder datePointBuilder )
    {

    }

    private void parseDataFormatHelper( String docBaseUriString, String mdXml )
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        URI docBaseUri = new URI( docBaseUriString );
        String catalogXml = CatalogXmlUtils.wrapThreddsXmlInContainerDataset( mdXml );

        CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogXml );

        assertNotNull( catBuilder );

        DatasetBuilder dsBldr = CatalogXmlUtils.assertCatalogWithContainerDatasetAsExpected( catBuilder, docBaseUri );
        ThreddsMetadataBuilder tmdBldr = dsBldr.getThreddsMetadataBuilder();
        DataFormatType dataFormat = tmdBldr.getDataFormat();
        assertEquals( dataFormat, DataFormatType.NEXRAD2 );
    }

}
