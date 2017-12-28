package thredds.client.catalog

import org.junit.experimental.categories.Category
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Unroll
import thredds.TestOnLocalServer
import ucar.unidata.util.test.category.NeedsCdmUnitTest

/**
 * @author cwardgar
 * @since 2015-10-12
 */
class ClientCatalogBasicSpec extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(ClientCatalogBasicSpec)

    @Unroll
    def "test local catalog [#catFrag]"() {
        expect:
        for (Dataset ds : ClientCatalogUtil.open(catFrag).getDatasetsLocal()) {
            testDatasets(ds)
        }

        where:
        catFrag << [ "enhancedCat.xml", "TestAlias.xml", "testMetadata.xml",
                     "nestedServices.xml", "TestHarvest.xml", "TestFilter.xml" ]
    }

    @Category(NeedsCdmUnitTest)  // For Metar_Station_Data_fc.cdmr.
    @Unroll
    def "test remote catalog [#catFrag]"() {
        expect:
        for (Dataset ds : ClientCatalogUtil.open(catFrag).getDatasetsLocal()) {
            testDatasets(ds)
        }

        where:
        catFrag << [TestOnLocalServer.withHttpPath("catalog.xml"),
                    TestOnLocalServer.withHttpPath("catalog/testStationFeatureCollection/catalog.xml?" +
                                                   "dataset=testStationFeatureCollection/Metar_Station_Data_fc.cdmr")
        ]
    }

    void testDatasets(Dataset d) {
        testAccess(d)
        testProperty(d)
        testDocs(d)
        testMetadata(d)
        testContributors(d)
        testKeywords(d)
        testProjects(d)
        testPublishers(d)
        testVariables(d)

        for (Dataset ds : d.getDatasetsLocal()) {
            testDatasets(ds)
        }
    }

    static void testAccess(Dataset d) {
        for (Access a : d.getAccess()) {
            assert a.getService() != null
            assert a.getUrlPath() != null
            assert a.getDataset().equals(d)
            testService(a.getService())
        }
    }

    static void testProperty(Dataset d) {
        for (Property p : d.getProperties()) {
            logger.debug("{}", p)
        }
    }

    static void testDocs(Dataset d) {
        for (Documentation doc : d.getDocumentation()) {
            logger.debug("{}", doc)
        }
    }

    static void testService(Service s) {
        List<Service> n = s.getNestedServices()
        if (n == null) {
            return
        }
        if (s.getType() == ServiceType.Compound) {
            assert n.size() > 0
        } else {
            assert n.size() == 0
        }
    }

    static void testMetadata(Dataset d) {
        for (ThreddsMetadata.MetadataOther m : d.getMetadataOther()) {
            logger.debug("{}", m.xlinkHref)
        }
    }

    static void testContributors(Dataset d) {
        for (ThreddsMetadata.Contributor m : d.getContributors()) {
            logger.debug("{}", m.getName())
        }
    }

    static void testKeywords(Dataset d) {
        for (ThreddsMetadata.Vocab m : d.getKeywords()) {
            logger.debug("{}", m.getText())
        }
    }

    static void testProjects(Dataset d) {
        for (ThreddsMetadata.Vocab m : d.getProjects()) {
            logger.debug("{}", m.getText())
        }
    }

    static void testPublishers(Dataset d) {
        for (ThreddsMetadata.Source m : d.getPublishers()) {
            logger.debug("{}", m.getName())
        }
    }

    static void testVariables(Dataset d) {
        for (ThreddsMetadata.VariableGroup m : d.getVariables()) {
            logger.debug("{}", m.getVocabulary())
        }
    }
}
