package ucar.nc2.ncml4;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.nc2.*;

public class TestRemoteCrawlableDataset extends TestCase {

  public TestRemoteCrawlableDataset( String name) {
    super(name);
  }

  public void testNcmlDirect() throws IOException {
    String filename = "file:./"+TestNcML.topDir + "remote/aggCrawlableDataset.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestRemoteCrawlableDataset.open "+ ncfile);
  }
}

