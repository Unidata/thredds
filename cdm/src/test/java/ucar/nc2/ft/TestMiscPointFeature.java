package ucar.nc2.ft;
/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 * @author tkunicki
 */
public class TestMiscPointFeature extends TestCase {

  public TestMiscPointFeature(String name) {
    super(name);
  }

  public void testIterator() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    FeatureDataset fd = null;
    try {
      Formatter formatter = new Formatter(System.err);
      fd = FeatureDatasetFactoryManager.open(FeatureType.STATION, "src/test/data/point/StandardPointFeatureIteratorIssue.ncml", null, formatter);
      if (fd != null && fd instanceof FeatureDatasetPoint) {
        FeatureDatasetPoint fdp = (FeatureDatasetPoint) fd;
        FeatureCollection fc = fdp.getPointFeatureCollectionList().get(0);
        if (fc != null && fc instanceof StationTimeSeriesFeatureCollection) {
          StationTimeSeriesFeatureCollection stsfc =
                  (StationTimeSeriesFeatureCollection) fc;
          // subset criteria not important, just want to get data
          // into flattened representation
          PointFeatureCollection pfc = stsfc.flatten(
                  new LatLonRect(
                          new LatLonPointImpl(-90, -180),
                          new LatLonPointImpl(90, 180)),
                  new DateRange(
                          df.parse("1900-01-01"),
                          df.parse("2100-01-01")));
          PointFeatureIterator pfi = pfc.getPointFeatureIterator(-1);
          try {
            while (pfi.hasNext()) {
              PointFeature pf = pfi.next();
              // the call to cursor.getParentStructure() in
              // in StandardPointFeatureIterator.makeStation()
              // is returning the observation structure, not the
              // station structure since Cursor.currentIndex = 0
              Station s = stsfc.getStation(pf);
              System.out.println("stn= " + s);
            }
          } finally {
            pfi.finish();
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    } finally {
      if (fd != null) {
        try {
          fd.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public void testGempak() throws Exception {
    String file = TestAll.cdmUnitTestDir +  "formats/gempak/surface/09052812.sf";
    Formatter buf = new Formatter();
    FeatureDatasetPoint pods =
        (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
            ucar.nc2.constants.FeatureType.POINT, file, null, buf);
    if (pods == null) {  // try as ANY_POINT
      pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
          ucar.nc2.constants.FeatureType.ANY_POINT, file, null, buf);
    }
    if (pods == null) {
      throw new Exception("can't open file");
    }
    List<FeatureCollection> collectionList =
        pods.getPointFeatureCollectionList();
    if (collectionList.size() > 1) {
      throw new IllegalArgumentException(
          "Can't handle point data with multiple collections");
    }
    boolean sample = true;
    for (int time = 0; time < 2; time++) {
      sample = time < 1;
      FeatureCollection fc = collectionList.get(0);
      PointFeatureCollection collection = null;
      LatLonRect llr = null; // new LatLonRect(new LatLonPointImpl(33.4, -92.2), new LatLonPointImpl(47.9, -75.89));
      System.out.println("llr = " + llr);
      if (fc instanceof PointFeatureCollection) {
        collection = (PointFeatureCollection) fc;
        if (llr != null) {
          collection = collection.subset(llr, null);
        }
      } else if (fc instanceof NestedPointFeatureCollection) {
        NestedPointFeatureCollection npfc =
            (NestedPointFeatureCollection) fc;
        if (llr != null) {
          npfc = npfc.subset(llr);
        }
        collection = npfc.flatten(llr, null);
      } else {
        throw new IllegalArgumentException(
            "Can't handle collection of type "
                + fc.getClass().getName());
      }
      List pos = new ArrayList(100000);
      List times = new ArrayList(100000);
      PointFeatureIterator dataIterator = collection.getPointFeatureIterator(16384);

      while (dataIterator.hasNext()) {
        PointFeature po = (PointFeature) dataIterator.next();
        pos.add(po);
        times.add(po.getNominalTimeAsDate());
        System.out.println("po = " + po);
        if (sample) {
          break;
        }
      }
      int size = pos.size();

      for (int i = 0; i < size; i++) {
        PointFeature po = (PointFeature) pos.get(i);
        ucar.unidata.geoloc.EarthLocation el = po.getLocation();
        System.out.println("el = " + el);
      }
    }
  }
}
