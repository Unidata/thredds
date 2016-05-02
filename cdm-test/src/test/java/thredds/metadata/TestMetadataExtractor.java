package thredds.metadata;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.ThreddsMetadata;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.thredds.MetadataExtractorAcdd;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateRange;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test MetadataExtractor and MetadataExtractorAcdd
 *
 * @author caron
 * @since 11/1/13
 */
@Category(NeedsCdmUnitTest.class)
public class TestMetadataExtractor {

  @Test
  public void testMetadataExtractorAcdd() throws IOException, URISyntaxException {
    String filename = TestDir.cdmUnitTestDir + "agg/pointFeatureCollection/netCDFbuoydata/BOD001_000_20050627_20051109.nc";
    NetcdfFile ncfile = NetcdfFile.open(filename);

    InvCatalogImpl cat = new InvCatalogImpl("testAcdd", "1.0.5", new URI("file:baseURI"));
    InvDatasetImpl ds = new InvDatasetImpl(null, "testAcdd");
    ds.finish();
    MetadataExtractorAcdd acdd = new MetadataExtractorAcdd(Attribute.makeMap(ncfile.getGlobalAttributes()), ds, ds.getLocalMetadataInheritable());
    acdd.extract();
    ncfile.close();

    cat.addDataset(ds);
    cat.finish();
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory(false);
    System.out.printf("%s%n", fac.writeXML(cat));

    assertEquals(ds.getDataType(), FeatureType.STATION);

    assert ds.getDocumentation("title").startsWith("Seawater temperature data collected from Bodega Head");
    assert ds.getDocumentation("summary").startsWith("These seawater data are collected");
    assert ds.getDocumentation("history").startsWith("2012-03-06 09:41:58 UTC:");
    assert ds.getDocumentation("comment").startsWith("Supplementary information: West Coast Observing");
    assert ds.getDocumentation("rights").startsWith("Please cite Gulf of the Farallones National");

    DateRange tc = ds.getTimeCoverage();
    assert tc != null;
    assertEquals(tc.getStart().getCalendarDate(), CalendarDate.parseISOformat(null, "2005-06-27T21:48:00"));
    assertEquals(tc.getEnd().getCalendarDate(), CalendarDate.parseISOformat(null, "2005-11-09T00:53:59"));

    ThreddsMetadata.GeospatialCoverage geo = ds.getGeospatialCoverage();
    assert geo != null;
    assert geo.getEastWestRange().getSize() == 0.0;
    assert geo.getNorthSouthRange().getSize() == 0.0;

    List<ThreddsMetadata.Source> creators = ds.getCreators();
    assert creators.size() == 1;
    ThreddsMetadata.Source creator = creators.get(0);
    assert creator.getName().startsWith("Data Manager");
    assert creator.getEmail().equals("Data Manager (bmldata@ucdavis.edu)");
    assert creator.getUrl().equals("http://bml.ucdavis.edu");

    List<ThreddsMetadata.Source> publishers = ds.getPublishers();
    assert publishers.size() == 1;
    ThreddsMetadata.Source pub = publishers.get(0);
    assert pub.getName().startsWith("US NATIONAL OCEANOGRAPHIC DATA CENTER");
    assert pub.getEmail().equals("NODC.Services@noaa.gov");
    assert pub.getUrl().equals("http://www.nodc.noaa.gov/");

    List<ThreddsMetadata.Vocab> vocab = ds.getKeywords();
    assert vocab.size() == 5;
    ThreddsMetadata.Vocab voc = vocab.get(0);
    assert voc.getVocabulary().startsWith("GCMD Earth Science Keywords. Version 5.3.3");

  }
}
