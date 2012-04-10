package thredds.server.config;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import thredds.servlet.DataRootHandlerTest;
import thredds.servlet.HtmlWriterTest;


@RunWith(Suite.class)
@SuiteClasses(value={TdsContextTest.class, DataRootHandlerTest.class, HtmlWriterTest.class})
public class ServerConfigTestSuit {

}
