package ucar.nc2.iosp.nexrad2;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.io.File;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Oct 2, 2007
 * Time: 10:30:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestNexrad2HiResolution extends TestCase {

    public TestNexrad2HiResolution( String name) {
        super(name);
    }

    public void testRead() throws IOException {
        long start = System.currentTimeMillis();
        doDirectory(TestAll.upcShareTestDataDir + "radar/nexrad/newLevel2/testfiles", false);
        //doDirectory("/upc/share/testdata/radar/nexrad/newLevel2/testfiles", false);
        long took = System.currentTimeMillis() - start;
        System.out.println("that took = "+took+" msec");
      }

      private void doDirectory(String dirName, boolean alwaysUncompress) throws IOException {

        File dir = new File(dirName);
        File[] files = dir.listFiles();
        if (alwaysUncompress) {
          for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String path = file.getPath();
            if (!path.endsWith(".bz2"))
              file.delete();
          }
        }

        for (int i = 0; i < files.length; i++) {
          File file = files[i];
          String path = file.getPath();
          if (!path.endsWith(".bz2")) continue;

          if (file.isDirectory())
            doDirectory(path, alwaysUncompress);
          else {
            NetcdfFile ncfile = NetcdfDataset.openFile(path, null);
            testRead(ncfile);
            testCoordSystem(ncfile);
          }

        }
      }

      private void testRead( NetcdfFile nexrad2) throws IOException {
        System.out.println(nexrad2.getLocation());

        Dimension scanR = nexrad2.findDimension("scanR");
        assert null != scanR;
        Dimension scanR_HI = nexrad2.findDimension("scanR_HI");
        assert null != scanR_HI;
        Dimension scanV = nexrad2.findDimension("scanV");
        assert null != scanV;
        Dimension scanV_HI = nexrad2.findDimension("scanV_HI");
        assert null != scanV_HI;

        assert scanR.getLength() == scanV.getLength();

        Variable elevR =  nexrad2.findVariable("elevationR");
        assert elevR != null;
        Array elevRdata = elevR.read();
        Variable elevR_HI =  nexrad2.findVariable("elevationR_HI");
        assert elevR_HI != null;
        Array elevR_HIdata = elevR_HI.read();
        assert elevR_HIdata != null;

        Variable elevV =  nexrad2.findVariable("elevationV");
        assert elevV != null;
        Array elevVdata = elevV.read();
        Variable elevV_HI =  nexrad2.findVariable("elevationV_HI");
        assert elevV_HI != null;
        Array elevV_HIdata = elevV_HI.read();
        assert elevV_HIdata != null;

        assert elevRdata.getSize() ==  elevVdata.getSize();

        Variable v =  nexrad2.findVariable("Reflectivity");
        assert v != null;
        Array data = v.read();

        v =  nexrad2.findVariable("RadialVelocity");
        assert v != null;
        data = v.read();

        v =  nexrad2.findVariable("SpectrumWidth");
        assert v != null;
        data = v.read();

        v =  nexrad2.findVariable("Reflectivity_HI");
        assert v != null;
        data = v.read();

        v =  nexrad2.findVariable("RadialVelocity_HI");
        assert v != null;
        data = v.read();

        v =  nexrad2.findVariable("SpectrumWidth_HI");
        assert v != null;
        if(v != null)
            data = v.read();
      }

      private void testCoordSystem( NetcdfFile nexrad2) throws IOException {
        Dimension scanR = nexrad2.findDimension("scanR");
        assert null != scanR;
        Dimension scanR_HI = nexrad2.findDimension("scanR_HI");
        assert null != scanR_HI;
        Dimension scanV = nexrad2.findDimension("scanV");
        assert null != scanV;
        Dimension scanV_HI = nexrad2.findDimension("scanV_HI");
        assert null != scanV_HI;

        assert scanR.getLength() == scanV.getLength();

        Variable elevR =  nexrad2.findVariable("elevationR");
        assert elevR != null;
        Array elevRdata = elevR.read();
        IndexIterator elevRiter = elevRdata.getIndexIterator();
        Variable elevR_HI =  nexrad2.findVariable("elevationR_HI");
        assert elevR_HI != null;
        Array elevRdataHI = elevR_HI.read();
        IndexIterator elevRiterHI = elevRdataHI.getIndexIterator();

        Variable elevV =  nexrad2.findVariable("elevationV");
        assert elevV != null;
        Array elevVdata = elevV.read();
        IndexIterator elevViter = elevVdata.getIndexIterator();
        Variable elevV_HI =  nexrad2.findVariable("elevationV_HI");
        assert elevV_HI != null;
        Array elevVdataHI = elevV.read();
        IndexIterator elevViterHI = elevVdataHI.getIndexIterator();

        assert elevRdata.getSize() ==  elevVdata.getSize();

        int count = 0;
        boolean ok = true;
        while (elevRiter.hasNext()) {
          if (elevRiter.getFloatNext() != elevViter.getFloatNext()) {
            ok = false;
            System.out.println(count+" "+elevRiter.getFloatCurrent()+" != "+elevViter.getFloatCurrent());
          }
          count++;
        }
        count = 0;
        while (elevRiterHI.hasNext()) {
          if (Float.isNaN(elevRiterHI.getFloatNext())) {
            ok = false;
            System.out.println("elevationR_HI contains Float.NAN " + count );
          }
          count++;
        }
        count = 0;
        while (elevViterHI.hasNext()) {
          if (Float.isNaN(elevViterHI.getFloatNext())) {
            ok = false;
            System.out.println("elevationV_HI contains Float.NAN " + count );
          }
          count++;
        }

        assert ok;
      }



}
