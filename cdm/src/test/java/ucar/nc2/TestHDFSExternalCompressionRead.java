/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;
import ucar.unidata.util.test.TestDir;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;

import java.io.*;

/** Test reading from HDFS variable data */

public class TestHDFSExternalCompressionRead extends TestCase {

  private FileSystem fs;
  private MiniDFSCluster cluster;
  private String tmpDir;
  private String uri;
  private String baseFilename = "/KTLX19910720_160529.gz";

  // @Before
  // public void setup() throws Exception {
  //   tmpDir = java.nio.file.Files.createTempDirectory("TestHDFSRead").toString();
  //   File file = new File(tmpDir);
  //   file.deleteOnExit();

  //   System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA);
  //   Configuration conf = new HdfsConfiguration();
  //   conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, tmpDir);

  //   cluster = new MiniDFSCluster.Builder(conf).build();
  //   uri = "hdfs://localhost:" + cluster.getNameNodePort();

  //   fs = FileSystem.get(conf);
  //   Path src = new Path(TestDir.cdmLocalTestDataDir + "/testWrite.nc");
  //   // Path dst = new Path(fs.getHomeDirectory() + "/testWrite.nc");
  //   Path dst = new Path(uri + "/testWrite.nc");
  //   fs.copyFromLocalFile(src, dst);
  // }

  // @After
  // public void cleanup() throws Exception {
  //   cluster.shutdown();
  // }

  public TestHDFSExternalCompressionRead( String name) throws Exception {
    super(name);
    tmpDir = java.nio.file.Files.createTempDirectory("TestHDFSExternalCompressionRead").toString();
    File file = new File(tmpDir);
    file.deleteOnExit();

    System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA);
    Configuration conf = new HdfsConfiguration();
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, tmpDir);

    cluster = new MiniDFSCluster.Builder(conf).build();
    uri = "hdfs://localhost:" + cluster.getNameNodePort();

    fs = FileSystem.get(conf);
    Path src = new Path(TestDir.cdmLocalTestDataDir + baseFilename);
    Path dst = new Path(uri + baseFilename);
    fs.copyFromLocalFile(src, dst);
  }

  public void testNC3Read() throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(uri + baseFilename);

    assert(null != ncfile.findDimension("scanR"));
    assert(null != ncfile.findDimension("gateR"));
    assert(null != ncfile.findDimension("radialR"));

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("Reflectivity")));

    // read array
    Array A;
    try {
      A = temp.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert (A.getRank() == 3);

    int i,j;
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    assert shape[0] == 1;
    assert shape[1] == 366;
    assert shape[2] == 460;

    ncfile.close();
  }
}
