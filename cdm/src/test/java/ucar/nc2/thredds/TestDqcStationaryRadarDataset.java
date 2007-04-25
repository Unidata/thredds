package ucar.nc2.thredds;

import thredds.catalog.query.Station;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.net.URI;
import java.io.IOException;

import ucar.nc2.dt.RadialDatasetSweep;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 17, 2007
 * Time: 2:42:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestDqcStationaryRadarDataset {
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

        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stns;
        List absList2 = stn.getDqcRadarStationURIs("1hour");
        assert null != absList2;
        List ulist = stn.getDqcRadarStationURIs(null, null);
        assert null != ulist;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;
        Date ts = (Date)tlist.get(1);
        assert null != ts;
        URI stURL = stn.getDqcRadarDatasetURI( ts);
        assert null != stURL;

        List data = stn.getData();
        assert null != data;
        Iterator it = data.iterator();

        while(it.hasNext()) {
            RadialDatasetSweep rds = (RadialDatasetSweep)it.next();
            assert null != rds;
        }

    }
    
    public void testDqcRadarDatasetURIs() throws IOException {

        DqcStationaryRadarDataset.DqcRadarStation stn = (DqcStationaryRadarDataset.DqcRadarStation)(stns.get(1));
        assert null != stn;
        List absList = stn.getDqcRadarStationURIs("1hour");
        assert null != absList;
        List ulist = stn.getDqcRadarStationURIs(null, null);
        assert null != ulist;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;

        Iterator it = stn.getDqcRadarStationDatasets("1hour");
        assert null != it;
        List data = stn.getData();
        assert null != data;
        while(it.hasNext()) {
            RadialDatasetSweep rds = (RadialDatasetSweep)it.next();
            assert null != rds;
        }


    }

    public void testDqcRadarDatasetATimes() throws IOException {

        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stn;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;
        Date ts = (Date)tlist.get(1);
        assert null != ts;
        URI stURL = stn.getDqcRadarDatasetURI(ts);
        assert null != stURL;

    }

    public void testDqcRadarDatasetTimesRange() throws IOException {

        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stn;
        List rList = stn.getDqcRadarStationTimes();
        assert null != rList;
        List absList = stn.getDqcRadarStationURIs((String)rList.get(1));
        assert null != absList;
        Iterator it = stn.getDqcRadarStationDatasets((String)rList.get(1));
        assert null != it;

        List subList = stn.getDqcRadarStationTimes((Date)rList.get(3), (Date)rList.get(0));
        assert null != subList;

        Iterator it1 = subList.iterator();
                while(it1.hasNext()) {
                    Date result = (Date)it.next();
                    assert null != result;
        }

    }

    public void testDqcRadarDatasetATimesGetData() throws IOException {

        DqcStationaryRadarDataset.DqcRadarStation stn = ds.getRadarStation("KFTG");
        assert null != stn;
        List tlist = stn.getDqcRadarStationTimes();
        assert null != tlist;
        Date ts = (Date)tlist.get(1);

        URI stURL = stn.getDqcRadarDatasetURI( ts);
        assert null != stURL;

        List dList = ds.getData((ucar.nc2.dt.Station)stns.get(2), (Date)tlist.get(3), (Date)tlist.get(0));
        Iterator it = dList.iterator();
        while(it.hasNext()) {
            URI result = (URI)it.next();
            assert null != result;
        }

    }

    public void testDqcRadarDatasetATimesGetData1() throws IOException {

        DqcStationaryRadarDataset.DqcRadarStation stn =
                (DqcStationaryRadarDataset.DqcRadarStation)(stns.get(2));

        List tlist = stn.getDqcRadarStationTimes();
        int sz = tlist.size();
        List dList = ds.getData((ucar.nc2.dt.Station)stns.get(2), (Date)tlist.get(sz-1), (Date)tlist.get(0), 3600, 500, 500 );

        Iterator it1 = dList.iterator();
        while(it1.hasNext()) {
            RadialDatasetSweep rds = (RadialDatasetSweep)it1.next();
            assert null != rds;
        }
     }

}
