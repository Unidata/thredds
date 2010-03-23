
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
import java.util.Formatter;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.units.DateRange;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 *
 * @author tkunicki
 */
public class StandardPointFeatureIteratorIssue {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        FeatureDataset fd = null;
        try {
            Formatter formatter = new Formatter(System.err);
            fd = FeatureDatasetFactoryManager.open(
                FeatureType.STATION, "/machine/dev/tdsAgg/cdm/src/test/java/StandardPointFeatureIteratorIssue.ncml", null, formatter);
            if (fd != null && fd instanceof FeatureDatasetPoint) {
                FeatureDatasetPoint fdp = (FeatureDatasetPoint)fd;
                FeatureCollection fc = fdp.getPointFeatureCollectionList().get(0);
                if (fc != null && fc instanceof StationTimeSeriesFeatureCollection) {
                    StationTimeSeriesFeatureCollection stsfc =
                            (StationTimeSeriesFeatureCollection)fc;
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
                            System.out.println(s);
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
            if (fd != null) {  try { fd.close(); } catch (IOException e) { } }
        }
    }
}
