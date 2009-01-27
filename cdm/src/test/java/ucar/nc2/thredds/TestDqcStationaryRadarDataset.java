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
package ucar.nc2.thredds;

import thredds.catalog.query.Station;

import java.util.*;
import java.net.URI;
import java.io.IOException;

import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.units.DateUnit;
import ucar.unidata.util.DateSelectionInfo;
import ucar.unidata.util.DateSelection;
import junit.framework.TestCase;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 17, 2007
 * Time: 2:42:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestDqcStationaryRadarDataset extends TestCase {
    StringBuffer errlog = new StringBuffer();
    String dqc_location = "http://motherlode.ucar.edu:8080/thredds/idd/radarLevel2";
    DqcRadarDatasetCollection ds;
    List stns;


    public void testDqc() throws IOException {

        try {
            ds = ucar.nc2.thredds.DqcRadarDatasetCollection.factory("test", dqc_location, errlog);
        } catch (java.io.IOException e) {
                throw new IOException( e.getMessage()+"\n");
        }

        stns = ds.getStations();
        assert null != stns;
        //System.out.println(" nstns= "+stns.size());
        if(errlog.length() > 0)
             System.out.println(" errs= "+errlog);
        else
            System.out.println("success\n");
    }

    public void testDqcRadarStation() throws IOException {
        if(ds== null)
            testDqc();
        
        Station stn = ds.getStation("KFTG");
        assert null != stns;
        long now = System.currentTimeMillis();
        long yday0 = now - (36*60*60*1000);
        long yday1 = now - (30*60*60*1000);
        Date ts0 = new Date(yday0);
        Date ts1 = new Date(yday1);
        DateSelection dateS = new DateSelection(ts0, ts1);
        dateS.setInterval((double)3600*0);
        dateS.setRoundTo((double)3600*0);
        dateS.setPreRange((double)500*0);
        dateS.setPostRange((double)500*0);
        List absList2 = ds.getRadarStationTimes(stn.getValue(), ts0, ts1);
        assert null != absList2;
        List ulist = ds.getDataURIs(stn.getValue(), dateS);
        assert null != ulist;
        dateS.setInterval((double)3600*1000);
        ulist = ds.getDataURIs(stn.getValue(), dateS);
        assert null != ulist;
        List tlist = ds.getRadarStationTimes(stn.getValue(), null, null);
        assert null != tlist;
        Date ts = DateUnit.getStandardOrISO((String)tlist.get(1));
        assert null != ts;
        URI stURL = ds.getRadarDatasetURI(stn.getValue(),ts);
        assert null != stURL;


    }
    
    public void testDqcRadarDatasetURIs() throws IOException {
        if(ds== null)
            testDqc();

        Station stn = (thredds.catalog.query.Station)(stns.get(1));
        assert null != stn;
        DateSelection dateS = new DateSelection();
        dateS.setInterval((double)3600*1000);
        dateS.setRoundTo((double)3600*1000);
        dateS.setPreRange((double)500*1000);
        dateS.setPostRange((double)500*1000);
        List ulist = ds.getDataURIs(stn.getValue(), dateS );
        assert null != ulist;
        List tlist = ds.getRadarStationTimes(stn.getValue(), null, null);
        assert null != tlist;
        List data = ds.getDataURIs(stn.getValue(), dateS );
        assert null != data;
        dateS.setPreRange((double)200*1000);
        dateS.setPostRange((double)200*1000);
        List data1 = ds.getDataURIs(stn.getValue(), dateS );
        assert null != data1;
        dateS.setPostRange((double)200*0);
        List data2 = ds.getDataURIs(stn.getValue(), dateS );
        assert null != data2;
        dateS.setPreRange((double)200*0);
        dateS.setPostRange((double)200*1000);
        List data3 = ds.getDataURIs(stn.getValue(), dateS );
        assert null != data3;
        dateS.setPreRange((double)100*1000);
        dateS.setPostRange((double)100*1000);
        List data4 = ds.getDataURIs(stn.getValue(), dateS );
        assert null != data4;

    }

    public void testDqcRadarDatasetATimes() throws IOException {
        if(ds== null)
            testDqc();

        Station stn = (thredds.catalog.query.Station)ds.getStation("KFTG");
        assert null != stn;
        List tlist = ds.getRadarStationTimes(stn.getValue(), null, null);
        assert null != tlist;
        Date ts = DateUnit.getStandardOrISO((String)tlist.get(1));
        assert null != ts;
        URI stURL = ds.getRadarDatasetURI(stn.getValue(),ts);
        assert null != stURL;
        RadialDatasetSweep rds = ds.getRadarDataset(stn.getValue(),ts);
        assert null != rds;
    }

    public void testDqcRadarDatasetTimesRange() throws IOException {
        if(ds== null)
            testDqc();

        Station stn = (thredds.catalog.query.Station)ds.getStation("KFTG");
        assert null != stn;
        List rList = ds.getRadarStationTimes(stn.getValue(), null, null);
        assert null != rList;
        Date ts0 = DateUnit.getStandardOrISO((String)rList.get(5));
        Date ts1 = DateUnit.getStandardOrISO((String)rList.get(1));
        List subList = ds.getRadarStationTimes(stn.getValue(),ts0, ts1);
        assert null != subList;

        Iterator it1 = subList.iterator();
                while(it1.hasNext()) {
                    Date result = DateUnit.getStandardOrISO((String)it1.next());
                    assert null != result;
        }

        long now = System.currentTimeMillis();
        long yday0 = now - (36*60*60*1000);
        long yday1 = now - (24*60*60*1000);
        ts0 = new Date(yday0);
        ts1 = new Date(yday1);
        DateSelection dateS = new DateSelection(ts0, ts1);
        dateS.setInterval((double)3600*1000);
        dateS.setRoundTo((double)3600*1000);
        dateS.setPreRange((double)500*1000);
        dateS.setPostRange((double)500*1000);
        List data = ds.getData(stn.getValue(), dateS);
        assert null != data;
        Iterator it = data.iterator();

        while(it.hasNext()) {
            RadialDatasetSweep rds = (RadialDatasetSweep)it.next();
            assert null != rds;
        }


    }

    public void testDqcRadarDatasetATimesGetData() throws IOException {
        if(ds== null)
            testDqc();

        Station stn = (thredds.catalog.query.Station)ds.getStation("KFTG");
        assert null != stn;
        List tlist = ds.getRadarStationTimes(stn.getValue(), null, null);
        assert null != tlist;
        int sz = tlist.size();
        Date ts = DateUnit.getStandardOrISO((String)tlist.get(1));

        URI stURL = ds.getRadarDatasetURI(stn.getValue(), ts);
        assert null != stURL;
        Date tsn = DateUnit.getStandardOrISO((String)tlist.get(0));
        Date ts1 = DateUnit.getStandardOrISO((String)tlist.get(sz-1));
        DateSelection dateS = new DateSelection(ts1, tsn);
        dateS.setInterval((double)3600*1000);
        dateS.setRoundTo((double)3600*1000);
        dateS.setPreRange((double)500*1000);
        dateS.setPostRange((double)500*1000);
        List dList = ds.getDataURIs("KABX", dateS );
        Iterator it = dList.iterator();
        while(it.hasNext()) {
            URI result = (URI)it.next();
            assert null != result;
        }

    }


     public String getISOTime(Date d){
        java.text.SimpleDateFormat isoDateTimeFormat;
        isoDateTimeFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));

        return isoDateTimeFormat.format(d);
    }
}
