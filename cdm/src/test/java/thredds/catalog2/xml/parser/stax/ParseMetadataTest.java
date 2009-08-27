package thredds.catalog2.xml.parser.stax;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.CatalogXmlUtils;
import thredds.catalog2.builder.*;
import thredds.catalog2.ThreddsMetadata;
import thredds.catalog.DataFormatType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import ucar.nc2.constants.FeatureType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ParseMetadataTest
{
  public ParseMetadataTest() { }

    /*
     * <serviceName>odap</serviceName>  // already done in ParseCatalogTest
     * <dataFormat>NEXRAD2</dataFormat>
     *
     * <dataType>Radial</dataType>
     * <documentation>Some interesting text.</documentation>
     * <metadata xlink:title='good metadata' xlink:href='http://good.metadata/'/>
     * ...
     */

  @Test
  public void parseDataFormat()
          throws URISyntaxException,
                 XMLStreamException,
                 ThreddsXmlParserException
  {
      String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseDataFormat.xml";

      String mdXml = "<dataFormat>NEXRAD2</dataFormat>";

      parseDataFormatHelper( docBaseUriString, mdXml);
  }

    @Test
    public void parseDataFormatWrapped()
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseDataFormatWrapped.xml";
        String mdXml = "<metadata><dataFormat>NEXRAD2</dataFormat></metadata>";

        parseDataFormatHelper( docBaseUriString, mdXml);
    }

    @Test
    public void parseDataFormatInherited()
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseDataFormatInherited.xml";
        String mdXml = "<metadata inherited='true'><dataFormat>NEXRAD2</dataFormat></metadata>";

        parseDataFormatHelper( docBaseUriString, mdXml);
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

    @Test
    public void parseDataType()
            throws URISyntaxException,
                   XMLStreamException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseDataType.xml";

        String mdXml = "<dataType>Radial</dataType>";

        parseDataTypeHelper( docBaseUriString, mdXml );
    }


    private void parseDataTypeHelper( String docBaseUriString, String mdXml )
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        URI docBaseUri = new URI( docBaseUriString );
        String catalogXml = CatalogXmlUtils.wrapThreddsXmlInContainerDataset( mdXml );

        CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogXml );

        assertNotNull( catBuilder );

        DatasetBuilder dsBldr = CatalogXmlUtils.assertCatalogWithContainerDatasetAsExpected( catBuilder, docBaseUri );
        ThreddsMetadataBuilder tmdBldr = dsBldr.getThreddsMetadataBuilder();
        FeatureType dataType = tmdBldr.getDataType();
        assertEquals( dataType, FeatureType.RADIAL );
    }

    @Test
    public void checkCreateDate()
            throws URISyntaxException,
                   ThreddsXmlParserException
    {
        String docBaseUriString = "http://cat2.stax.ParseMetadataTest/parseCreatedDate.xml";
        URI docBaseUri = new URI( docBaseUriString );

        String date = "2009-08-25T12:00";
        String type = ThreddsMetadata.DatePointType.Created.toString();
        String mdXml = "<date type='" + type + "'>" + date + "</date>";
        String catalogXml = CatalogXmlUtils.wrapThreddsXmlInContainerDataset( mdXml );

        CatalogBuilder catBuilder = CatalogXmlUtils.parseCatalogIntoBuilder( docBaseUri, catalogXml );

        assertNotNull( catBuilder );
        DatasetBuilder dsBldr = CatalogXmlUtils.assertCatalogWithContainerDatasetAsExpected( catBuilder, docBaseUri );

        ThreddsMetadataBuilder tmdBldr = dsBldr.getThreddsMetadataBuilder();
        ThreddsMetadataBuilder.DatePointBuilder datePointBuilder = tmdBldr.getCreatedDatePointBuilder();

        assertEquals( datePointBuilder.getDate(), date);
        assertNull( datePointBuilder.getDateFormat());
        assertEquals( ThreddsMetadata.DatePointType.getTypeForLabel( datePointBuilder.getType()),
                      ThreddsMetadata.DatePointType.Created );
    }
}
