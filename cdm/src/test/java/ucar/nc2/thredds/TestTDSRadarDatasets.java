/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
/**
 * User: rkambic
 * Date: Mar 10, 2011
 * Time: 4:27:34 PM
 */

package ucar.nc2.thredds;

import junit.framework.TestCase;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.units.DateUnit;
import ucar.unidata.util.DateSelection;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

public class TestTDSRadarDatasets extends TestCase {

  StringBuffer errlog = new StringBuffer();
  TDSRadarDatasetCollection ds;
  List<ucar.unidata.geoloc.Station> stns;

  public void testDatasetLevel2IDD() throws IOException {
        StringBuffer              errlog      = new StringBuffer();
        String                    ds_location = null;
        TDSRadarDatasetCollection dsc         = null;
        List                      stns        = null;

        String iddUrl =
            "http://motherlode.ucar.edu:8081/thredds/radarServer/nexrad/level2/IDD/dataset.xml";
        dsc = TDSRadarDatasetCollection.factory("test", iddUrl, errlog);
        System.out.println(" errs= " + errlog);
        stns = dsc.getStations();
        System.out.println(" nstns= " + stns.size());

        ucar.unidata.geoloc.Station stn = dsc.getRadarStation("KFTG");  //(StationImpl)stns.get(12);
        System.out.println("stn = " + stn);

        List<String> tl  = dsc.getRadarTimeSpan();
        Date ts1 = DateUnit.getStandardOrISO(tl.get( 0 ));
        Date ts2 = DateUnit.getStandardOrISO(tl.get( 1 ));
        List pd  = dsc.getRadarProducts();
        List<Date> tlist = dsc.getRadarStationTimes(stn.getName(),
                               ts1, ts2);
        int sz = tlist.size();
        assert sz > 2000;

        for (int i = 0; i < 3; i++) {
            Date ts0 = (Date) tlist.get(i);
            RadialDatasetSweep rds = dsc.getRadarDataset(stn.getName(),
                                           ts0);
        }

        Date ts0   = (Date) tlist.get(0);
        URI  stURL = dsc.getRadarDatasetURI(stn.getName(),   ts0);
        assert null != stURL;
        DateSelection dateS = new DateSelection(ts1, ts2);
        dateS.setInterval((double) 3600 * 1000);
        dateS.setRoundTo((double) 3600 * 1000);
        dateS.setPreRange((double) 500 * 1000);
        dateS.setPostRange((double) 500 * 1000);



        for (int i = 0; i < stns.size(); i++) {
            stn = (ucar.unidata.geoloc.Station) stns.get(i);
            List<Date> times = dsc.getRadarStationTimes(
                                   stn.getName(),
                                   new Date(
                                       System.currentTimeMillis()
                                       - 3600 * 1000 * 24 * 100), new Date(
                                           System.currentTimeMillis()));
            if (times.size() > 0) {
                System.err.println(stn + " times:" + times.size() + " "
                                   + times.get(times.size() - 1)
                                   + " - "+ times.get(0) );
            } else {
                System.err.println(stn + " no times");
            }
        }

        Date                       ts = (Date) tlist.get(1);
        java.text.SimpleDateFormat isoDateTimeFormat;
        isoDateTimeFormat =
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String st = isoDateTimeFormat.format(ts);


    }
  
  /**
     * Test dataset nexrad/level3/CCS039
     *
     * @throws IOException _more_
     */
    public void testDatasetCCS039() throws IOException {
        StringBuffer              errlog      = new StringBuffer();
        String                    ds_location = null;
        TDSRadarDatasetCollection dsc         = null;
        List                      stns        = null;

        String ccs039Url =
            //"http://localhost:8080/thredds/radarServer/nexrad/level3/CCS039/dataset.xml";
            "http://motherlode.ucar.edu:8081/thredds/radarServer/nexrad/level3/CCS039/dataset.xml";
            //"http://motherlode.ucar.edu:8080/thredds/radarServer/nexrad/level3/CCS039/dataset.xml";
        dsc = TDSRadarDatasetCollection.factory("test", ccs039Url, errlog);
        System.out.println(" errs= " + errlog);
        stns = dsc.getStations();
        System.out.println(" nstns= " + stns.size());

        ucar.unidata.geoloc.Station stn = dsc.getRadarStation("DVN");  //(StationImpl)stns.get(12);
        System.out.println("stn = " + stn);

        List tl  = dsc.getRadarTimeSpan();
        Date ts1 = DateUnit.getStandardOrISO("1998-06-28T01:01:21Z");
        Date ts2 = DateUnit.getStandardOrISO("1998-07-30T19:01:21Z");
        List pd  = dsc.getRadarProducts();
        List<Date> tlist = dsc.getRadarStationTimes(stn.getName(), "BREF1",
                               ts1, ts2);
        int sz = tlist.size();
        assert sz == 39;

        for (int i = 0; i < 3; i++) {
            Date ts0 = (Date) tlist.get(i);
            RadialDatasetSweep rds = dsc.getRadarDataset(stn.getName(),
                                         "BREF1", ts0);
        }

        Date ts0   = (Date) tlist.get(0);
        URI  stURL = dsc.getRadarDatasetURI(stn.getName(), "BREF1", ts0);
        assert null != stURL;
        DateSelection dateS = new DateSelection(ts1, ts2);
        dateS.setInterval((double) 3600 * 1000);
        dateS.setRoundTo((double) 3600 * 1000);
        dateS.setPreRange((double) 500 * 1000);
        dateS.setPostRange((double) 500 * 1000);



        for (int i = 0; i < stns.size(); i++) {
            stn = (ucar.unidata.geoloc.Station) stns.get(i);
            List<Date> times = dsc.getRadarStationTimes(
                                   stn.getName(),
                                   "BREF1",
                                   new Date(
                                       System.currentTimeMillis()
                                       - 3600 * 1000 * 24 * 100), new Date(
                                           System.currentTimeMillis()));
            if (times.size() > 0) {
                System.err.println(stn + " times:" + times.size() + " "
                                   + times.get(0) + " - "
                                   + times.get(times.size() - 1));
            } else {
                System.err.println(stn + " no times");
            }
        }
 
        Date                       ts = (Date) tlist.get(1);
        java.text.SimpleDateFormat isoDateTimeFormat;
        isoDateTimeFormat =
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String st = isoDateTimeFormat.format(ts);


    }

}
