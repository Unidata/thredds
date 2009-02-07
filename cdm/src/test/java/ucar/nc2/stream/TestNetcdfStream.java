package ucar.nc2.stream;

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestCompare;

import java.io.*;
import java.nio.channels.WritableByteChannel;


public class TestNetcdfStream {


  //////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    try {
      String filename = "src/test/data/feb.nc";
      //String filename = "src/test/data/dmsp/F14200307192230.s.OIS";
      NetcdfFile ncfile = NetcdfFile.open(filename);
      NcStream ncstream = new NcStream(ncfile);

      File file = new File("C:/temp/out.ncs");
      FileOutputStream fos = new FileOutputStream(file);
      WritableByteChannel wbc = fos.getChannel();
      ncstream.stream(wbc);
      wbc.close();

      InputStream is = new BufferedInputStream( new FileInputStream(file));
      NetcdfFile ncfileBack = ncstream.readStream(is);
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
