package ucar.nc2.thredds;

import thredds.catalog.query.Station;

import java.util.*;
import java.net.URI;
import java.io.IOException;

import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.units.DateUnit;
import ucar.unidata.util.DateSelectionInfo;
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
    String dqc_location = "http://motherlode.ucar.edu:9080/thredds/idd/radarLevel2";
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
        List absList2 = ds.getRadarStationTimes(stn.getValue(), ts0, ts1);
        assert null != absList2;
        List ulist = ds.getDataURIs(stn.getValue(),new DateSelectionInfo(ts0, ts1, 0, 0, 0, 0));
        assert null != ulist;
        ulist = ds.getDataURIs(stn.getValue(), new DateSelectionInfo(ts0, ts1, 3600, 0, 0, 0));
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

        List ulist = ds.getDataURIs(stn.getValue(), new DateSelectionInfo(null, null, 3600, 60*60, 500, 500) );
        assert null != ulist;
        List tlist = ds.getRadarStationTimes(stn.getValue(), null, null);
        assert null != tlist;
        List data = ds.getDataURIs(stn.getValue(), new DateSelectionInfo(null, null, 3600, 60*60, 500, 500) );
        assert null != data;
        List data1 = ds.getDataURIs(stn.getValue(), new DateSelectionInfo(null, null, 3600, 60*60, 200, 200) );
        assert null != data1;
        List data2 = ds.getDataURIs(stn.getValue(), new DateSelectionInfo(null, null, 3600, 60*60, 200, 0) );
        assert null != data2;
        List data3 = ds.getDataURIs(stn.getValue(), new DateSelectionInfo(null, null, 3600, 60*60, 0, 200) );
        assert null != data3;
        List data4 = ds.getDataURIs(stn.getValue(), new DateSelectionInfo(null, null, 3600, 60*60, 100, 100) );
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
        List data = ds.getData(stn.getValue(), new DateSelectionInfo(ts0, ts1, 3600, 60*60, 500, 500) );
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
        List dList = ds.getDataURIs("KABX", new DateSelectionInfo(ts1, tsn, 3600, 60*60, 500, 500) );
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
