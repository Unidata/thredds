/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft;

import ucar.unidata.geoloc.*;

import visad.*;

import java.util.*;

public class TestPointCDM {

  public static void main(String[] args) throws Exception {
    String file = "C:/data/ft/station/09052812.sf"; // C:/data/formats/gempak/surface/20090524_sao.gem";
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
        times.add(new DateTime(po.getNominalTimeAsDate()));
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

