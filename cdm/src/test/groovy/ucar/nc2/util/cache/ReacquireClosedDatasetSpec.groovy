package ucar.nc2.util.cache

import spock.lang.Specification
import ucar.nc2.dataset.NetcdfDataset
import ucar.unidata.test.util.TestDir

/**
 * Tests caching behavior when datasets are closed and then reacquired.
 *
 * @author cwardgar
 * @since 2016-01-02
 */
class ReacquireClosedDatasetSpec extends Specification {
    def "reacquire"() {
        setup: 'Initialize cache'
        NetcdfDataset.initNetcdfFileCache(100, 150, 12 * 60);
        String location = TestDir.cdmLocalTestDataDir + "jan.nc"

        when: 'Acquire and close dataset 4 times'
        (1..4).each { println ''
            NetcdfDataset.acquireDataset(location, null).close()
        }

        and: 'Query cache stats'
        Formatter formatter = new Formatter()
        NetcdfDataset.netcdfFileCache.showStats(formatter)

        then: 'The cache will have recorded 1 miss (1st trial) and 3 hits (subsequent trials)'
        // This is kludgy, but FileCache doesn't provide getHits() or getMisses() methods.
        formatter.toString().trim() ==~ /hits= 3 miss= 1 nfiles= \d+ elems= \d+/

        // Prior to a bug fix in FileCache, this would record 0 hits and 4 misses.
    }
}
