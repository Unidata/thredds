package ucar.nc2.grib;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.time.CalendarDate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author iancw
 */
public class TestVerticalAxis
{

    private static byte[] buildPDSBytes(int type, int val1, int val2)
    {
        byte[] pds = new byte[30];
        pds[8] = 1; // parameter number
        pds[9] = (byte) type; // level type
        pds[10] = (byte) val1; // level value 1
        pds[11] = (byte) val2; // level value 2
        pds[12] = 13; // year
        pds[13] = 10; // mo
        pds[14] = 4; // day
        pds[15] = 12; // hour
        pds[16] = 0; // minute
        return pds;
    }


    List<Grib1Record> records;
    Grib1Rectilyser rect;


    @Before
    public void mockRecords()
    {
        final Grib1Customizer cust = Mockito.mock(Grib1Customizer.class);

        records = new ArrayList<Grib1Record>();
        int gdsHash = 1; // Using non-zero allows all our GDS to be identical, 
        // otherwise i'd have to mock out hashCode on the GDS
        Map<String, Boolean> pdsHash = new HashMap<String, Boolean>();
        rect = new Grib1Rectilyser(cust, records, gdsHash, pdsHash);


        Mockito.when(cust.getParamLevel(Matchers.any(Grib1SectionProductDefinition.class))).thenAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock mock) throws Throwable
            {
                Object[] args = mock.getArguments();
                Grib1SectionProductDefinition pds = (Grib1SectionProductDefinition) args[0];
                return new Grib1ParamLevel(cust, pds.getLevelType(), pds.getLevelValue1(), pds.getLevelValue2());
            }
        });
        Mockito.when(cust.getVertUnit(Matchers.anyInt())).thenAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock mock) throws Throwable
            {
                Object[] args = mock.getArguments();
                int levelType = ((Integer) args[0]).intValue();
                boolean posUp = true;
                if (levelType == 107 || levelType == 100)
                {
                    posUp = false;
                }
                return new GribLevelType(levelType, "meters", "datum", posUp);
            }
        });
        Mockito.when(cust.isLayer(Matchers.anyInt())).thenReturn(Boolean.FALSE);
    }


    private void addRecord(byte[] pdsBytes)
    {
        Grib1Record record = Mockito.mock(Grib1Record.class);
        records.add(record);

        Grib1SectionProductDefinition pds = new Grib1SectionProductDefinition(pdsBytes);
        Grib1SectionGridDefinition gds = Mockito.mock(Grib1SectionGridDefinition.class);
        Grib1Gds g = Mockito.mock(Grib1Gds.class);
        Grib1ParamTime time = Mockito.mock(Grib1ParamTime.class);

        Mockito.when(gds.getGDS()).thenReturn(g);
        Mockito.when(record.getGDSsection()).thenReturn(gds);
        Mockito.when(record.getPDSsection()).thenReturn(pds);
        Mockito.when(record.getReferenceDate()).thenReturn(CalendarDate.present());
    }


    @Test
    public void testAGLAxisOrder() throws IOException
    {
        addRecord(buildPDSBytes(105, 0, 0));
        addRecord(buildPDSBytes(105, 2, 0));
        addRecord(buildPDSBytes(105, 1, 0));
        Grib1Rectilyser.Counter stats = new Grib1Rectilyser.Counter();
        rect.make(stats);
        List<VertCoord> vertCoords = rect.getVertCoords();
        Assert.assertEquals(1, vertCoords.size());
        List<VertCoord.Level> levels = vertCoords.get(0).getCoords();
        Assert.assertEquals(3, levels.size());
        for (int i = 1; i < levels.size(); i++)
        {
            VertCoord.Level cur = levels.get(i);
            VertCoord.Level prev = levels.get(i - 1);
            // AGL layers are positive up
            Assert.assertTrue(cur.compareTo(prev) > 0);
        }
    }


    @Test
    public void testSigmaAxisOrder() throws IOException
    {
        addRecord(buildPDSBytes(107, 10925, 0));
        addRecord(buildPDSBytes(107, 0, 0));
        addRecord(buildPDSBytes(107, 2000, 0));
        Grib1Rectilyser.Counter stats = new Grib1Rectilyser.Counter();
        rect.make(stats);
        List<VertCoord> vertCoords = rect.getVertCoords();
        Assert.assertEquals(1, vertCoords.size());
        List<VertCoord.Level> levels = vertCoords.get(0).getCoords();
        Assert.assertEquals(3, levels.size());
        for (int i = 1; i < levels.size(); i++)
        {
            VertCoord.Level cur = levels.get(i);
            VertCoord.Level prev = levels.get(i - 1);
            // Sigma layers are positive down
            Assert.assertTrue(cur.compareTo(prev) < 0);
        }
    }


    @Test
    public void testIsobarAxisOrder() throws IOException
    {
        addRecord(buildPDSBytes(100, 1000, 0));
        addRecord(buildPDSBytes(100, 925, 0));
        addRecord(buildPDSBytes(100, 850, 0));
        Grib1Rectilyser.Counter stats = new Grib1Rectilyser.Counter();
        rect.make(stats);
        List<VertCoord> vertCoords = rect.getVertCoords();
        Assert.assertEquals(1, vertCoords.size());
        List<VertCoord.Level> levels = vertCoords.get(0).getCoords();
        Assert.assertEquals(3, levels.size());
        for (int i = 1; i < levels.size(); i++)
        {
            VertCoord.Level cur = levels.get(i);
            VertCoord.Level prev = levels.get(i - 1);
            // Isobar layers are positive down
            Assert.assertTrue(cur.compareTo(prev) < 0);
        }
    }

}
