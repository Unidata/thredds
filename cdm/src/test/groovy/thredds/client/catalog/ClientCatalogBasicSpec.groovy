package thredds.client.catalog

import org.junit.experimental.categories.Category
import org.slf4j.Logger
import spock.lang.Specification
import spock.lang.Unroll
import ucar.unidata.util.test.TestDir
import ucar.unidata.util.test.category.NeedsExternalResource

/**
 * @author cwardgar
 * @since 2015-10-12
 */
class ClientCatalogBasicSpec extends Specification {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger("testLogger");

    @Unroll
    def "test local catalog [#catFrag]"() {
        expect:
        for (Dataset ds : TestClientCatalog.open(catFrag).getDatasetsLocal())
            testDatasets(ds);

        where:
        catFrag << [ "enhancedCat.xml", "TestAlias.xml", "testMetadata.xml",
                     "nestedServices.xml", "TestHarvest.xml", "TestFilter.xml" ]
    }

    @Unroll
    @Category(NeedsExternalResource.class)
    def "test remote catalog [#catFrag]"() {
        expect:
        for (Dataset ds : TestClientCatalog.open(catFrag).getDatasetsLocal())
            testDatasets(ds);

        where:
        catFrag << ["http://"+ TestDir.threddsTestServer+"/thredds/catalog.xml",
                    "http://"+ TestDir.threddsTestServer+"/thredds/catalog/nws/metar"
                    + "/ncdecoded-test/catalog.xml?dataset=nws/metar/ncdecoded-test"
                    + "/Metar_Station_Data_-_Test_fc.cdmr"]
    }

    public void testDatasets(Dataset d) {
        testAccess(d);
        testProperty(d);
        testDocs(d);
        testMetadata(d);
        testContributors(d);
        testKeywords(d);
        testProjects(d);
        testPublishers(d);
        testVariables(d);

        for (Dataset ds : d.getDatasetsLocal()) {
            testDatasets(ds);
        }
    }

    public void testAccess(Dataset d) {
        for (Access a : d.getAccess()) {
            assert a.getService() != null;
            assert a.getUrlPath() != null;
            assert a.getDataset().equals(d);
            testService(a.getService());
        }
    }

    public static void testProperty(Dataset d) {
        for (Property p : d.getProperties()) {
            logger.debug("{}", p);
        }
    }

    public static void testDocs(Dataset d) {
        for (Documentation doc : d.getDocumentation()) {
            logger.debug("{}", doc);
        }
    }


    public static void testService(Service s) {
        List<Service> n = s.getNestedServices();
        if (n == null) return;
        if (s.getType() == ServiceType.Compound)
            assert n.size() > 0;
        else
            assert n.size() == 0;
    }


    public static void testMetadata(Dataset d) {
        for (ThreddsMetadata.MetadataOther m : d.getMetadataOther()) {
            logger.debug("{}", m.xlinkHref)
        }
    }

    public static void testContributors(Dataset d) {
        for (ThreddsMetadata.Contributor m : d.getContributors()) {
            logger.debug("{}", m.getName());
        }
    }

    public static void testKeywords(Dataset d) {
        for (ThreddsMetadata.Vocab m : d.getKeywords()) {
            logger.debug("{}", m.getText());
        }
    }

    public static void testProjects(Dataset d) {
        for (ThreddsMetadata.Vocab m : d.getProjects()) {
            logger.debug("{}", m.getText());
        }
    }

    public static void testPublishers(Dataset d) {
        for (ThreddsMetadata.Source m : d.getPublishers()) {
            logger.debug("{}", m.getName());
        }
    }

    public static void testVariables(Dataset d) {
        for (ThreddsMetadata.VariableGroup m : d.getVariables()) {
            logger.debug("{}", m.getVocabulary());
        }
    }
}
