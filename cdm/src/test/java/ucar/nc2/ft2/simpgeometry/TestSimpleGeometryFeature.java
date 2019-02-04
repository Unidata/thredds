package ucar.nc2.ft2.simpgeometry;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.CoverageReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static ucar.nc2.ft2.simpgeometry.GeometryType.LINE;
import static ucar.nc2.ft2.simpgeometry.GeometryType.POINT;
import static ucar.nc2.ft2.simpgeometry.GeometryType.POLYGON;
import static org.mockito.BDDMockito.*;

import ucar.nc2.ft2.simpgeometry.CFLine;
import ucar.nc2.ft2.simpgeometry.CFPoint;
import ucar.nc2.ft2.simpgeometry.CFPolygon;
import ucar.nc2.ft2.simpgeometry.GeometryType;
import ucar.nc2.ft2.simpgeometry.Line;
import ucar.nc2.ft2.simpgeometry.Point;
import ucar.nc2.ft2.simpgeometry.Polygon;
import ucar.nc2.ft2.simpgeometry.SimpleGeometryFeature;
import ucar.nc2.ft2.simpgeometry.adapter.SimpleGeometryCS;

public class TestSimpleGeometryFeature {

    private String name = "name";
    private DataType dt;
    private List<Attribute> att = new ArrayList<Attribute>();
    private String coordSysName = "coordsysname";
    private String units = "units";
    private String description = "desc";
    private Object user;
    private GeometryType geometry;



    @Test (expected = RuntimeException.class)
    public void testSetCoordSysNull() {
        SimpleGeometryFeature cov = new SimpleGeometryFeature(name, dt, att, coordSysName, units, description, user, geometry);
        SimpleGeometryCS cs = mock(SimpleGeometryCS.class);
        cov.setCoordSys(cs);
        cov.setCoordSys(cov.getCoordSys());
    }

    @Test
    public void testReadPoint() throws IOException, InvalidRangeException {

        int index = 1;
        SimpleGeometryCS cs = mock(SimpleGeometryCS.class);

        Point point = mock(CFPoint.class);
        given(cs.getPoint(name, index)).willReturn(point);
        SimpleGeometryFeature cov = new SimpleGeometryFeature(name, dt, att, coordSysName, units, description, user, POINT);
        cov.setCoordSys(cs);
        Assert.assertEquals(cov.readGeometry(index), cs.getPoint(name, index));
    }

    @Test
    public void testReadLine() throws IOException, InvalidRangeException {

        int index = 1;
        SimpleGeometryCS cs = mock(SimpleGeometryCS.class);

        Line line = mock(CFLine.class);
        given(cs.getLine(name, index)).willReturn(line);
        SimpleGeometryFeature cov = new SimpleGeometryFeature(name, dt, att, coordSysName, units, description, user, LINE);
        cov.setCoordSys(cs);
        Assert.assertEquals(cov.readGeometry(index), cs.getLine(name, index));

    }

    @Test
    public void testReadPolygon() throws IOException, InvalidRangeException {

        int index = 1;
        SimpleGeometryCS cs = mock(SimpleGeometryCS.class);

        Polygon polygon = mock(CFPolygon.class);
        given(cs.getPolygon(name, index)).willReturn(polygon);
        SimpleGeometryFeature cov = new SimpleGeometryFeature(name, dt, att, coordSysName, units, description, user, POLYGON);
        cov.setCoordSys(cs);
        Assert.assertEquals(cov.readGeometry(index), cs.getPolygon(name, index));

    }



}
