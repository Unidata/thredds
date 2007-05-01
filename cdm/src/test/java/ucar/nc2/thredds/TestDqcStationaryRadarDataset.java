package ucar.nc2.thredds;

import thredds.catalog.query.Station;

import java.util.*;
import java.net.URI;
import java.io.IOException;

import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.units.DateUnit;
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
    DqcStationaryRadarDataset ds;
    List stns;


    public void testDqc() throws IOException {

        try {
            ds = DqcStationaryRadarDataset.factory("test", dqc_location, errlog);
        } catch (java.io.IOException e) {
                throw new IOException( e.getMessage()+"\n");
        }
        System.out.println(" errs= "+errlog);

        stns = ds.getStations();
        assert null != stns;
        //System.out.println(" nstns= "+stns.size());

    }

    public void testDqcRadarStation() throws IOException {
        if(ds== null)
            testDqc();
        
        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stns;
        List absList2 = stn.getDqcRadarStationURIs("1hour");
        assert null != absList2;
       // List ulist = stn.getDqcRadarStationURIs(null, null, 0, 0, 0, 0);
       // assert null != ulist;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;
        Date ts = (Date) DateUnit.getStandardOrISO((String)tlist.get(1));
        assert null != ts;
        URI stURL = stn.getDqcRadarDatasetURI( ts);
        assert null != stURL;


    }
    
    public void testDqcRadarDatasetURIs() throws IOException {
        if(ds== null)
            testDqc();

        DqcStationaryRadarDataset.DqcRadarStation stn = (DqcStationaryRadarDataset.DqcRadarStation)(stns.get(1));
        assert null != stn;
        List absList = stn.getDqcRadarStationURIs("1hour");
        assert null != absList;
        List ulist = stn.getDqcRadarStationURIs(null, null, 3600, 60*60, 500, 500 );
        assert null != ulist;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;
        List data = stn.getDqcRadarStationURIs(null, null, 3600, 60*60, 500, 500 );
        assert null != data;

        Iterator it = stn.getDqcRadarStationDatasets("1hour");
        assert null != it;
        while(it.hasNext()) {
            RadialDatasetSweep rds = (RadialDatasetSweep)it.next();
            assert null != rds;
        }


    }

    public void testDqcRadarDatasetATimes() throws IOException {
        if(ds== null)
            testDqc();

        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stn;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;
        Date ts = DateUnit.getStandardOrISO((String)tlist.get(1));
        assert null != ts;
        URI stURL = stn.getDqcRadarDatasetURI(ts);
        assert null != stURL;
        RadialDatasetSweep rds = stn.getDqcRadarDataset(ts);
        assert null != rds;
    }

    public void testDqcRadarDatasetTimesRange() throws IOException {
        if(ds== null)
            testDqc();

        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stn;
        List rList = stn.getDqcRadarStationTimes();
        assert null != rList;
        Date ts0 = DateUnit.getStandardOrISO((String)rList.get(5));
        Date ts1 = DateUnit.getStandardOrISO((String)rList.get(1));
        List subList = stn.getDqcRadarStationTimes(ts0, ts1);
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
        List data = stn.getDqctRadarStationDatasets(ts0, ts1, 3600, 60*60, 500, 500 );
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

        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stn;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;
        int sz = tlist.size();
        Date ts = DateUnit.getStandardOrISO((String)tlist.get(1));

        URI stURL = stn.getDqcRadarDatasetURI( ts);
        assert null != stURL;
        Date tsn = DateUnit.getStandardOrISO((String)tlist.get(0));
        Date ts1 = DateUnit.getStandardOrISO((String)tlist.get(sz-1));
        List dList = ds.getDataURIs("KABX", ts1, tsn, 3600, 60*60, 500, 500 );
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
