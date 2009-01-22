package ucar.nc2.dt.point;

import junit.framework.*;

import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.units.DateUnit;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Date;
import java.util.Iterator;

/** Test StationObsDataset adapters in the JUnit framework. */

public class TestStationDataset extends TestCase {
  String topDir = ucar.nc2.TestAll.upcShareTestDataDir+ "station/";

  public TestStationDataset( String name) {
    super(name);
  }

  public void testMadis() throws IOException {
    String filename = ucar.nc2.TestAll.upcShareTestDataDir+"point/netcdf/madis.nc";
    StringBuilder sbuff = new StringBuilder();
    long start = System.currentTimeMillis();
    PointObsDataset pods = (PointObsDataset) TypedDatasetFactory.open( FeatureType.POINT, filename, null, sbuff);
    long took = System.currentTimeMillis() - start;
    System.out.println(" open madis as point dataset "+filename+" "+sbuff+" took "+took);

    start = System.currentTimeMillis();
    int bufferSize = 163840;
    Iterator  dataIterator = pods.getDataIterator(bufferSize);
    while (dataIterator.hasNext()) {
      dataIterator.next();
    }
    took = System.currentTimeMillis() - start;
    System.out.println(" first ok took "+took+" with bufferSize "+bufferSize);

    start = System.currentTimeMillis();
    dataIterator = pods.getDataIterator(bufferSize);
    while (dataIterator.hasNext()) {
      dataIterator.next();
    }
    took = System.currentTimeMillis() - start;
    System.out.println(" second ok took "+took);
  }

  public void utestMultidimStationObsDataset() throws IOException {
    testAllMethods( topDir+"misc/multidim.nc");
  }

  public void testUnidataStationObsDataset() throws IOException {
    testAllMethods( topDir+"ldm/metar/Surface_METAR_20060325_0000.nc");
  }

  public void utestMetarDataset() throws IOException {
    long start = System.currentTimeMillis();

    ThreddsDataFactory fac = new ThreddsDataFactory();
    ThreddsDataFactory.Result result = fac.openDatatype( "thredds:resolve:http://motherlode.ucar.edu:9080/thredds/idd/metar?returns=DQC", null);
    if ( result.fatalError )
    {
      System.out.println( "TestStationDataset.testMetarDataset():\n" + result.errLog.toString() );
      assert false;
    }
    StationObsDataset sod = (StationObsDataset) result.tds;
    assert sod != null;

    long took = System.currentTimeMillis() - start;
    System.out.println(" open took = "+took+" msec");
    start = System.currentTimeMillis();

    //StationObsDataset sod = (StationObsDataset) PointObsDatasetFactory.open( topDir+"ldm/Surface_METAR_20060701_0000.nc", null, null);
    DataIterator iter = sod.getDataIterator(0);
    double sum = 0.0;
    int count = 0;
    while (iter.hasNext()) {
      PointObsDatatype obs = (PointObsDatatype) iter.nextData();
      StructureData sdata = obs.getData();
      sum += sdata.convertScalarDouble("wind_speed");
      count++;
    }
    took = System.currentTimeMillis() - start;
    System.out.println(" read took = "+took+" msec");
    System.out.println("sum= "+sum+" count = "+count);
  }

  public void testNdbcStationObsDataset() throws IOException {
    testAllMethods( topDir+"ndbc/41001h1976.nc");
  }

  public void testMadisStationObsDataset() throws IOException {
    testAllMethods( topDir+"madis/metar.20040604_1600.nc");
    testAllMethods( topDir+"madis/sao.20040604_2100.nc");
    testAllMethods( topDir+"madis/mesonet1.20050502_2300");
    testAllMethods( topDir+"madis/coop.20040824_0900.gz");
    testAllMethods( topDir+"madis/hydro.20040824_0400.gz");
    testAllMethods( topDir+"madis/maritime.20040824_1000.gz");
    testAllMethods( topDir+"madis/radiometer.20040824_1000.gz");
  }

  public void utestMadisAll() throws IOException {
    String dataAlldir = "C:/data/madis";

    File dir = new File(dataAlldir);
    doOneFromEach(dir);
  }

  private void doOneFromEach(File dir) throws IOException {

    File[] files = dir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      if (file.isDirectory())
        doOneFromEach(file);
      else {
        System.out.println("\ndoOneFromEach="+file.getPath());
        try {
          PointObsDataset pod = (PointObsDataset) TypedDatasetFactory.open(FeatureType.POINT, file.getPath(), null, new StringBuilder());
          //if (null != pobs) testAllMethods(pobs);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return;
      }
    }
 }


  public void utestUnidataStationObsDataset2() throws IOException {
    testAllMethods( topDir+"ldm/20050626_metar.nc");
    testAllMethods( topDir+"ldm/20050727_metar.nc");
  }

  public void testOldUnidataStationObsDataset() throws IOException {
    testAllMethods( topDir+"ldm-old/04061912_buoy.nc");
    testAllMethods( topDir+"ldm-old/2004061915_metar.nc");
    testAllMethods( topDir+"ldm-old/04061900_syn.nc");
  }

  private void testAllMethods(String location) throws IOException {
    StringBuilder sbuff = new StringBuilder();
    StationObsDataset sod = (StationObsDataset) TypedDatasetFactory.open(FeatureType.STATION, location, null, sbuff);
    assert sod != null : sbuff.toString();

    System.out.println("-----------");
    System.out.println(sod.getDetailInfo());

    Date d1 = sod.getStartDate();
    Date d2 = sod.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals( d2);

    DateUnit du = sod.getTimeUnits();
    assert null != du;

    double startVal = du.makeValue( d1);
    double endVal = du.makeValue( d2);
    assert startVal <= endVal;

    Class dataClass = sod.getDataClass();
    assert dataClass == StationObsDatatype.class;

    List dataVars =  sod.getDataVariables();
    assert dataVars != null;
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) dataVars.get(i);
      assert null != sod.getDataVariable( v.getShortName());
    }

    List stations = sod.getStations();
    assert null != stations;
    assert 0 < stations.size();
    System.out.println(" stations = "+stations.size());
    int n = stations.size();
    testStation( sod, (Station) stations.get(0));
    if (n > 3) {
      testStation( sod, (Station) stations.get(n-1));
      testStation( sod, (Station) stations.get((n-1)/2));
    }

    // make a new bb
    LatLonRect bb = sod.getBoundingBox();
    assert null != bb;
    double h = bb.getUpperRightPoint().getLatitude() - bb.getLowerLeftPoint().getLatitude();
    LatLonRect bb2 = new LatLonRect(bb.getLowerLeftPoint(), bb.getWidth()/2, h/2);

    List<Station> stationsBB= sod.getStations( bb2);
    assert null != stationsBB;
    assert stationsBB.size() <= stations.size();
    System.out.println(" bb2 stations = "+stationsBB.size());

    List data = sod.getData( bb2);
    testData( sod.getTimeUnits(), data.iterator());

    // make a new data range
    double diff = endVal - startVal;
    Date startRange = du.makeDate( startVal + .25 * diff);
    Date endRange = du.makeDate( startVal + .75 * diff);
    data = sod.getData( bb2, startRange, endRange);
    testData( sod.getTimeUnits(), data.iterator());

    data = sod.getData( stationsBB, startRange, endRange);
    testData( sod.getTimeUnits(), data.iterator());

    DataIterator dataIter = sod.getDataIterator(0);
    int iterCount = testData( sod.getTimeUnits(), dataIter);
    System.out.println(" getData size= "+iterCount+" getDataCount= "+sod.getDataCount());
    assert iterCount == sod.getDataCount() : " iterCount = "+iterCount+" getDataCount= "+sod.getDataCount();

    int stationDataCount = 0;
    stations = sod.getStations();
    for (int i = 0; i < stations.size(); i++) {
      Station station = (Station) stations.get(i);
      List stationData = sod.getData( station);
      stationDataCount += stationData.size();
    }
    assert iterCount == stationDataCount : " iterCount= "+iterCount+" stationDataCount= "+stationDataCount;

    sod.close();
  }

  private int testData( DateUnit timeUnit, Iterator dataIter) throws java.io.IOException {
    int count = 0;
    while(dataIter.hasNext()) {
      Object data = dataIter.next();
      assert data instanceof StationObsDatatype;
      StationObsDatatype pobs = (StationObsDatatype) data;

      EarthLocation loc = pobs.getLocation();
      if (loc == null)
        System.out.println("barf");
      assert loc != null;
      Station s = pobs.getStation();
      assert s != null;

      assert null != pobs.getNominalTimeAsDate();
      assert null != pobs.getObservationTimeAsDate();

      assert timeUnit.makeDate( pobs.getNominalTime()).equals( pobs.getNominalTimeAsDate());
      assert timeUnit.makeDate( pobs.getObservationTime()).equals( pobs.getObservationTimeAsDate());

      StructureData sdata = pobs.getData();
      assert null != sdata;
      testData( sdata);
      count++;
    }
    return count;
  }

  private void testStation( StationObsDataset sod, Station s) throws IOException {

    assert sod.getStation( s.getName()).equals(s);

    List dataList = sod.getData( s);
    int n = sod.getStationDataCount( s);
    assert n == dataList.size() : n+ " != "+ dataList.size();
    System.out.println(" station "+s.getName()+" has "+n+" data records");

    Class dataClass = sod.getDataClass();
    assert dataClass == StationObsDatatype.class;

    if (n > 0) {
      assert dataClass.isInstance( dataList.get(0));
      StationObsDatatype data = (StationObsDatatype) dataList.get(0);
      StructureData sdata = data.getData();
      StructureMembers members = sdata.getStructureMembers();
      int size = members.getStructureSize();
      System.out.println(" structureSize= "+size+" total size = "+(sod.getDataCount() *size));

      List dataVars = sod.getDataVariables();
      List dataMembers = sdata.getMembers();
      assert dataMembers.size() >= dataVars.size();
      System.out.println(" dataMembers ="+dataMembers.size()+" dataVars= "+dataVars.size());

      for (int i = 0; i < dataVars.size(); i++) {
        VariableSimpleIF tdv = (VariableSimpleIF) dataVars.get(0);
        StructureMembers.Member member = members.findMember( tdv.getShortName());
        assert null != member : "cant find "+tdv.getShortName();

        member.getDataType().equals( tdv.getDataType());
        Array adata = sdata.getArray(member);
        adata.getShape().equals( member.getShape());
      }
    }
  }

  private void testData( StructureData sdata) {

    for (StructureMembers.Member member : sdata.getMembers()) {
      DataType dt = member.getDataType();
      if (dt == DataType.FLOAT) {
        sdata.getScalarFloat(member);
        sdata.getJavaArrayFloat(member);
      } else if (dt == DataType.DOUBLE) {
        sdata.getScalarDouble(member);
        sdata.getJavaArrayDouble(member);
      } else if (dt == DataType.BYTE) {
        sdata.getScalarByte(member);
        sdata.getJavaArrayByte(member);
      } else if (dt == DataType.SHORT) {
        sdata.getScalarShort(member);
        sdata.getJavaArrayShort(member);
      } else if (dt == DataType.INT) {
        sdata.getScalarInt(member);
        sdata.getJavaArrayInt(member);
      } else if (dt == DataType.LONG) {
        sdata.getScalarLong(member);
        sdata.getJavaArrayLong(member);
      } else if (dt == DataType.CHAR) {
        sdata.getScalarChar(member);
        sdata.getJavaArrayChar(member);
        sdata.getScalarString(member);
      } else if (dt == DataType.STRING) {
        sdata.getScalarString(member);
      }

      if (dt.isNumeric()) {
        sdata.convertScalarDouble(member.getName());
      }

    }
  }


}
