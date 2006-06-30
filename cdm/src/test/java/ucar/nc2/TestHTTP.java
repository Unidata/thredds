package ucar.nc2;

import junit.framework.*;

import ucar.ma2.*;
import java.io.IOException;
import java.util.*;

/** Test remote netcdf over HTTP in the JUnit framework. */

public class TestHTTP extends TestCase {
  String testDir= "http://motherlode.ucar.edu/test/";

  public TestHTTP( String name) {
    super(name);
  }

  public void testNC2() throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(testDir+"mydata1.nc");
    assert ncfile != null;

    assert(null != ncfile.findDimension("lat"));
    assert(null != ncfile.findDimension("lon"));

    assert("face".equals(ncfile.findAttValueIgnoreCase(null, "yo", "barf")));

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    assert("K".equals(ncfile.findAttValueIgnoreCase(temp, "units", "barf")));

    Attribute att = temp.findAttribute("scale");
    assert( null != att);
    assert( att.isArray());
    assert( 3 == att.getLength());
    assert( 3 == att.getNumericValue(2).intValue());

    att = temp.findAttribute("versionD");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1.2 == att.getNumericValue().doubleValue());

    att = temp.findAttribute("versionF");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1.2f == att.getNumericValue().floatValue());
    assert( close(1.2, att.getNumericValue().doubleValue()));

    att = temp.findAttribute("versionI");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1 == att.getNumericValue().intValue());

    att = temp.findAttribute("versionS");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 2 == att.getNumericValue().shortValue());

    att = temp.findAttribute("versionB");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 3 == att.getNumericValue().byteValue());

    // read
    Array A = temp.read();

    int i,j;
    Index ima = A.getIndex();
    int[] shape = A.getShape();

    // write
    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getDouble(ima.set(i,j)) == (double) (i*1000000+j*1000));
      }
    }

    //System.out.println( "ncfile = "+ ncfile);
    System.out.println( "*****************  Test HTTP done");

  }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  public List makeList() throws IOException {
    ArrayList list = new ArrayList();
    list.add("http://motherlode.ucar.edu/dods/casestudies/2004Aug03/2004080301_metar.nc");
    list.add("http://motherlode.ucar.edu/dods/casestudies/2004Aug03/2004080302_metar.nc");
    list.add("http://motherlode.ucar.edu/dods/casestudies/2004Aug03/2004080303_metar.nc");
    list.add("http://motherlode.ucar.edu/dods/casestudies/2004Aug03/2004080304_metar.nc");
    list.add("http://motherlode.ucar.edu/dods/casestudies/2004Aug03/2004080305_metar.nc");
    list.add("http://motherlode.ucar.edu/dods/casestudies/2004Aug03/2004080306_metar.nc");
    return list;
  }

  // HTTP = 4300 HTTP2 = 5500 msec 20-25% slower
  public void testOpenDataset() throws IOException {
    long start = System.currentTimeMillis();
    long totalBytes = 0;

    List locs = makeList();
    for (Iterator iter = locs.iterator(); iter.hasNext(); ) {
      String loc = (String) iter.next();
      ucar.nc2.dataset.NetcdfDataset.open( loc);
    }

    totalBytes /= 1000;
    System.out.println("**testOpenDataset took= "+(System.currentTimeMillis()-start)+" msec ");
  }

  public void testOpenGrid() throws IOException {
    long start = System.currentTimeMillis();
    long totalBytes = 0;

    List locs = makeList();
    for (Iterator iter = locs.iterator(); iter.hasNext(); ) {
      String loc = (String) iter.next();
      ucar.nc2.dataset.grid.GridDataset.open( loc);
    }

    totalBytes /= 1000;
    System.out.println("**testOpenGrid took= "+(System.currentTimeMillis()-start)+" msec ");
  }

  public void utestReadAll() throws IOException {
    long start = System.currentTimeMillis();
    long totalBytes = 0;

    List locs = makeList();
    for (Iterator iter = locs.iterator(); iter.hasNext(); ) {
      String loc = (String) iter.next();
      totalBytes += readAllData(loc);
      break;
    }

    totalBytes /= 1000;
    System.out.println("**That took= "+(System.currentTimeMillis()-start)+" msec for "+totalBytes+" Kbytes");
  }

  private long readAllData( String location) throws IOException {
    System.out.println("------Open "+location);
    NetcdfFile ncfile = NetcdfFile.open( location);

    long total = 0;
    for (java.util.Iterator iter = ncfile.getVariables().iterator(); iter.hasNext(); ) {
      Variable v = (Variable) iter.next();
      long nbytes = v.getSize() * v.getElementSize();
      System.out.println("  Try to read variable "+v.getName()+" "+nbytes);
      v.read();
      total += v.getSize() * v.getElementSize();
   }
    ncfile.close();
    return total;
  }

}
