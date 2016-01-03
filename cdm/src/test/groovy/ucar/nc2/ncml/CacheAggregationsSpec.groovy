package ucar.nc2.ncml

import spock.lang.Specification
import ucar.ma2.Array
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset

/**
 * Tests acquiring aggregated datasets from a file cache.
 *
 * @author cwardgar
 * @since 2015-12-29
 */
class CacheAggregationsSpec extends Specification {
    def setupSpec() {
        // All datasets, once opened, will be added to this cache.
        // Config values copied from CdmInit.
        NetcdfDataset.initNetcdfFileCache(100, 150, 12 * 60);

        // Force NetcdfDataset to reacquire underlying file in order to read a Variable's data, instead of being
        // able to retrieve that data from the Variable's internal cache. OPV-470285 does not manifest on variables
        // with cached data.
        Variable.permitCaching = false;
    }

    def cleanupSpec() {
        // Undo global changes we made in setupSpec() so that they do not affect subsequent test classes.
        NetcdfDataset.shutdown();
        Variable.permitCaching = true;
    }

    // The number of times each dataset will be acquired.
    // Failure, if it occurs, is expected to start happening on the 2nd trial.
    int numTrials = 2

    // Demonstrates eSupport ticket OPV-470285.
    def "union"() {
        setup:
        String filename = "file:./" + TestNcML.topDir + "aggUnion.xml"
        def expecteds = [5.0, 10.0, 15.0, 20.0]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDataset.acquireDataset(filename, null).withCloseable {
                Variable var = it.findVariable('Temperature')
                Array array = var.read('1,1,:')  // Prior to fix, failure happened here on 2nd trial.
                actuals = array.getStorage() as List
            }

            then:
            expecteds == actuals
        }
    }

    def "joinExisting"() {
        setup:
        String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";
        def expecteds = [8420.0, 8422.0, 8424.0, 8426.0]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDataset.acquireDataset(filename, null).withCloseable {
                Variable var = it.findVariable('P')
                Array array = var.read('42,1,:')
                actuals = array.getStorage() as List
            }

            then:
            expecteds == actuals
        }
    }

    def "joinNew"() {
        setup:
        String filename = "file:./"+TestNcML.topDir + "aggSynthetic.xml";
        def expecteds = [110.0, 111.0, 112.0, 113.0]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDataset.acquireDataset(filename, null).withCloseable {
                Variable var = it.findVariable('T')
                Array array = var.read('1,1,:')
                actuals = array.getStorage() as List
            }

            then:
            expecteds == actuals
        }
    }

    def "tiled"() {
        setup:
        String filename = "file:./"+TestNcML.topDir + "tiled/testAggTiled.ncml";
        def expecteds = [202.0, 264.0, 266.0, 268.0]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDataset.acquireDataset(filename, null).withCloseable {
                Variable var = it.findVariable('temperature')
                Array array = var.read('10,10:16:2')
                actuals = array.getStorage() as List
            }

            then:
            expecteds == actuals
        }
    }

    def "fmrc"() {
        setup:
        String filename = "file:./"+TestNcML.topDir + "fmrc/testAggFmrc.ncml";
        def expecteds = [232.0, 232.4, 232.5]
        def actuals

        (1..numTrials).each {
            when:
            NetcdfDataset.acquireDataset(filename, null).withCloseable {
                Variable var = it.findVariable('Temperature_isobaric')
                Array array = var.read(':, 11, 0, 0, 0')
                actuals = array.getStorage() as List
            }

            then:
            expecteds == actuals
        }
    }
}
