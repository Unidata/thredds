/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.ft;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.Formatter;

import ucar.nc2.dt.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateUnit;
import ucar.nc2.VariableSimpleIF;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Class Description.
 *
 * @author caron
 * @since Dec 16, 2008
 */
public class TestPointFeatureTypes  extends TestCase {
  String topDir = ucar.nc2.TestAll.upcShareTestDataDir+ "point/netcdf/";
  public TestPointFeatureTypes( String name) {
    super(name);
  }

  public void testReadAll() throws IOException {
    readAllDir(topDir, null);  
  }

  public void testProblem() throws IOException {
    testPointDataset(topDir+"puffash.ncml", true);
  }

  int readAllDir(String dirName, FileFilter ff) throws IOException {
    int count = 0;

    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return count;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude")) {
        testPointDataset(name, false);
        count++;
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += readAllDir(f.getAbsolutePath(), ff);
    }

    return count;
  }

  private void testPointDataset(String location, boolean show) throws IOException {
    System.out.printf("----------- Read %s %n", location);

    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.POINT, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s = %s %n", location, out);
      return;
    }

    // FeatureDataset
    if (show) {
      fdataset.getDetailInfo(out);
      System.out.printf("%s %n", out);
    }

    Date d1 = fdataset.getStartDate();
    Date d2 = fdataset.getEndDate();
    if ((d1 != null) && (d2 != null))
      assert d1.before(d2) || d1.equals( d2);

    List dataVars =  fdataset.getDataVariables();
    assert dataVars != null;
    for (int i = 0; i < dataVars.size(); i++) {
      VariableSimpleIF v = (VariableSimpleIF) dataVars.get(i);
      assert null != fdataset.getDataVariable( v.getShortName());
    }

    // FeatureDatasetPoint
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      // PointFeatureCollection;
      assert (fc instanceof PointFeatureCollection);
      testPointFeatureCollection((PointFeatureCollection) fc);
    }

    fdataset.close();
  }

  void testPointFeatureCollection( PointFeatureCollection pfc) throws IOException {
    LatLonRect bb = pfc.getBoundingBox();

    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (bb != null)
        assert bb.contains( pf.getLocation().getLatLon());
      count++;
    }

    bb = pfc.getBoundingBox();
    assert bb != null;

    int count2 = 0;
    PointFeatureIterator iter = pfc.getPointFeatureIterator(-1);
    while (iter.hasNext()) {
      PointFeature pf = iter.next();
      if (!bb.contains( pf.getLocation().getLatLon())) {
        assert bb.contains( pf.getLocation().getLatLon()) : bb.toString2() + " does not contains point "+pf.getLocation().getLatLon();
      }
      assert bb.contains( pf.getLocation().getLatLon()) : bb.toString2() + " does not contains point "+pf.getLocation().getLatLon();
      count2++;
    }
    assert count == count2;

    System.out.println(" getData count= "+count+" size= "+pfc.size());
  }

  private int testData( DateUnit timeUnit, Iterator dataIter) throws java.io.IOException {
    int count = 0;
    while(dataIter.hasNext()) {
      Object data = dataIter.next();
      assert data instanceof PointObsDatatype : data.getClass().getName();
      PointObsDatatype pobs = (PointObsDatatype) data;

      ucar.nc2.dt.EarthLocation loc = pobs.getLocation();
      assert loc != null;

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

      if ((dt != DataType.STRING) && (dt != DataType.STRUCTURE)) {
        sdata.convertScalarFloat(member.getName());
      }

    }
  }


}

