package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestCompare;

import java.io.*;
import java.nio.channels.WritableByteChannel;


public class TestNetcdfStream {


  static public void main( String args[]) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open("C:\\dev\\tds\\thredds\\tds\\content\\thredds\\public\\testdata/testData.nc", null);
    NetcdfFile ncfileRemote = new NcStreamRemote("http://localhost:8080/thredds/ncstream/test/testData.nc", null);
    TestCompare.compareFiles(ncfile, ncfileRemote, true, true, false);
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
