package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestCompare;

import java.io.*;
import java.nio.channels.WritableByteChannel;

import junit.framework.TestCase;


public class TestNetcdfStream extends TestCase {

  public TestNetcdfStream(String name) {
    super(name);
  }

  public void testCompare() throws IOException {
    //doOne("metars/Surface_METAR_20070326_0000.nc");
    //doOne("rotatedPole/eu.mn.std.fc.d12z20070820.nc");
    //doOne("gempak/nmcbob.shp.nc");
    doOne("gempak/19330101_sao.gem");
  }

  void doOne(String name) throws IOException {
    String file = "C:/data/"+name;
    String remote = "http://localhost:8080/thredds/ncstream/stream/" + name;

    doOne(file, remote);
  }

  void doOne(String file, String remote) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(file, null);
    NetcdfFile ncfileRemote = new NcStreamRemote(remote, null);
    TestCompare.compareFiles(ncfile, ncfileRemote, true, false, false);
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
      writer.streamAll(fos, wbc);
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
