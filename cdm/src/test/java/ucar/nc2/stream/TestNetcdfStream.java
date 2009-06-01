package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestCompare;
import ucar.nc2.TestAll;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.StringUtil;

import java.io.*;
import java.nio.channels.WritableByteChannel;

import junit.framework.TestCase;


public class TestNetcdfStream extends TestCase {

  public TestNetcdfStream(String name) {
    super(name);
  }

  public void testCompare() throws IOException {
    //
    doOne("formats/netcdf3/standardVar.nc");
    //doOne("point/uspln_20061023.18");
  }

  public void testScan() throws IOException {
    scanDir("C:/data/", new FileFilter() {
      public boolean accept(File pathname) {
        //return !pathname.getPath().endsWith(".xml") && !pathname.getPath().endsWith(".gbx");
        return pathname.getPath().endsWith(".nc");
      }
    });
  }

  void scanDir(String dirName, FileFilter ff) throws IOException {
    final int dirlen = dirName.length();
    TestAll.actOnAll( dirName, ff, new TestAll.Act() {
      public int doAct(String filename) throws IOException {
        String name = StringUtil.substitute(filename.substring(dirlen), "\\", "/");
        doOne(name);
        return 1;
      }
    });

  }

  void doOne(String name) throws IOException {
    String file = "C:/data/"+name;
    String remote = "http://localhost:8080/thredds/cdmremote/testCdmRemote/" + name;
    doOne(file, remote);
  }

  void doOne(String file, String remote) throws IOException {
    System.out.printf("---------------------------\n");
    NetcdfFile ncfile = NetcdfDataset.openFile(file, null);
    NetcdfFile ncfileRemote = new NcStreamRemote(remote, null);
    TestCompare.compareFiles(ncfile, ncfileRemote, false, false, false);
    System.out.printf("compare %s ok %n", file);
    TestCompare.compareFiles(ncfile, ncfileRemote, true, true, true);
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
      TestCompare.compareFiles(ncfile, ncfileBack, false, true, false);

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
