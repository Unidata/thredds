package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.TestAll;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.nio.channels.WritableByteChannel;

import junit.framework.TestCase;


public class TestNetcdfStream extends TestCase {
  String serverRoot = "C:/data/";
  
  public TestNetcdfStream(String name) {
    super(name);
  }

  public void testCompare() throws IOException {
    doOne("C:/data/formats/netcdf3/standardVar.nc");
  }

  public void testScan() throws IOException {
    /* scanDir("C:/data/", new FileFilter() {
      public boolean accept(File pathname) {
        //return !pathname.getPath().endsWith(".xml") && !pathname.getPath().endsWith(".gbx");
        return pathname.getPath().endsWith(".nc");
      }
    }); */

    scanDir("C:\\data\\formats\\gempak\\surface", new FileFilter() {
      public boolean accept(File pathname) {
        //return !pathname.getPath().endsWith(".xml") && !pathname.getPath().endsWith(".gbx");
        return pathname.getPath().endsWith(".gem");
      }
    });
  }

  void scanDir(String dirName, FileFilter ff) throws IOException {
    TestAll.actOnAll( dirName, ff, new TestAll.Act() {
      public int doAct(String filename) throws IOException {
        doOne(filename);
        return 1;
      }
    });

  }

  void doOne(String filename) throws IOException {
    String name = StringUtil.substitute(filename.substring(serverRoot.length()), "\\", "/");
    String remote = "http://localhost:8080/thredds/cdmremote/testCdmRemote/" + name;
    compare(filename, remote);
  }

  void compare(String file, String remote) throws IOException {
    System.out.printf("---------------------------\n");
    NetcdfFile ncfile = NetcdfDataset.openFile(file, null);
    NetcdfFile ncfileRemote = new CdmRemote(remote);
    CompareNetcdf.compareFiles(ncfile, ncfileRemote, false, false, false);
    System.out.printf("compare %s ok %n", file);
    CompareNetcdf.compareFiles(ncfile, ncfileRemote, true, true, true);
    System.out.printf("compare data %s ok %n", file);
    ncfile.close();
    ncfileRemote.close();
  }

  //////////////////////////////////////////////////////////////
  public static void main2(String[] args) {
    try {
      String filename = "src/test/data/feb.nc";
      //String filename = "src/test/data/dmsp/F14200307192230.s.OIS";
      NetcdfFile ncfile = NetcdfFile.open(filename);
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);

      File file = new File("C:/temp/out.ncs");
      FileOutputStream fos = new FileOutputStream(file);
      WritableByteChannel wbc = fos.getChannel();
      writer.streamAll( wbc);
      wbc.close();

      NcStreamReader reader = new NcStreamReader();      
      InputStream is = new BufferedInputStream( new FileInputStream(file));
      NetcdfFile ncfileBack = reader.readStream(is, null);
      CompareNetcdf.compareFiles(ncfile, ncfileBack, false, true, false);

      /*

      NcStreamProto.Stream proto = ncstream.nc2proto(ncfile);

      byte[] s = proto.toByteArray();
      System.out.println(" len= " + s.length);
      NcStreamProto.Stream readBack = NcStreamProto.Stream.parseFrom(s);
      ncstream.show(readBack);

      NetcdfFile ncfileBack = ncstream.proto2nc(readBack);

      System.out.println("ncfileBack= " + ncfileBack);

      TestCompare.compareFiles(ncfile, ncfileBack, false, true, false); */

      ncfile.close();


    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
