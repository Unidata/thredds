package thredds.server.config;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import thredds.mock.web.MockTdsContextLoader;
import ucar.unidata.util.test.category.NeedsContentRoot;

import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/WEB-INF/applicationContext.xml"},loader=MockTdsContextLoader.class)
@Category(NeedsContentRoot.class)
public class TdsContextTest {

	@Autowired
 private TdsUpdateConfigBean tdsUpdateConfig;

	@Autowired
 private TdsContext tdsContext;

	/* @Test
	public void testInit() {
		System.out.printf("%s%n", tdsContext);
		//All the initialization was done
		//serverInfo, htmlConfig, wmsConfig are initialized by TdsConfigMapper after ThreddConfig reads the threddsServer.xml file
		assertNotNull( tdsContext.getServerInfo() );
		assertNotNull( tdsContext.getHtmlConfig() );
		assertNotNull( tdsContext.getWmsConfig() );
	}  */

	@Test
	public void testVersionRetrieval() {
		String stableKey = "stable";
		String devKey = "development";
		String version = tdsContext.getVersionInfo();

		Map<String, String> latestVersionInfo = tdsUpdateConfig.getLatestVersionInfo(version);

		// is not empty
		assert(!latestVersionInfo.isEmpty());
		// contains the stable key and the stable version is not empty
		assert(latestVersionInfo.containsKey(stableKey));
		assert(!latestVersionInfo.get(stableKey).isEmpty());
		// contains the dev key and the dev version is not empty
		assert(latestVersionInfo.containsKey(devKey));
		assert(!latestVersionInfo.get(devKey).isEmpty());
	}

}
