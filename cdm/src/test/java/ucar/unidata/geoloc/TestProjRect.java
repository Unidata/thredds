/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

/** Test ncml value element in the JUnit framework. */
@RunWith(Parameterized.class)
public class TestProjRect {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private double x1, x2, y1, y2;
    private ProjectionRect projectionRect;

    @Parameterized.Parameters(name = "[{0}, {1}, {2}, {3}]")
    public static Collection projectionRectsInits() {
        Object[][] data = new Object[][] {
                {-1, -1, 1, 1},
                {1, 1, -1, -1},
                {-1, 1, 1, -1},
                {1, -1, -1, 1}};
        return Arrays.asList(data);
    }

    public TestProjRect(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    }

    @Before
    public void setUp() {
        this.projectionRect = new ProjectionRect(x1, y1, x2, y2);
    }

    @Test
    public void testGetX() {
        // getX() should give the x value for the upper left corner
        double getX = projectionRect.getX();
        double getMinX = projectionRect.getMinX();
        double getMaxX = projectionRect.getMaxX();

        assertEquals(getX, getMinX, 0);
        assertNotEquals(getX, getMaxX);
    }

    @Test
    public void testGetY() {
        // getX() should give the y value for the upper left corner
        double getY = projectionRect.getY();
        double getMinY = projectionRect.getMinY();
        double getMaxY = projectionRect.getMaxY();

        assertNotEquals(getY, getMaxY);
        assertEquals(getY, getMinY, 0);
    }

    @Test
    public void testGetWidth1() {
        // getX() should give the y value for the upper left corner
        double minX = projectionRect.getMinX();
        double maxX = projectionRect.getMaxX();
        double testWidth = maxX - minX;
        double width = projectionRect.getWidth();
        assertEquals(testWidth, width, 0);
    }

    @Test
    public void testGetHeight1() {
        // getX() should give the y value for the upper left corner
        double minY = projectionRect.getMinY();
        double maxY = projectionRect.getMaxY();
        double testHeight = maxY - minY;
        double height = projectionRect.getHeight();
        assertEquals(testHeight, height, 0);
    }

    @Test
    public void testGetWidth2() {
        // getX() should give the y value for the upper left corner
        double minX = projectionRect.getX();
        double maxX = projectionRect.getMaxX();
        double testWidth = maxX - minX;
        double width = projectionRect.getWidth();
        assertEquals(testWidth, width, 0);

    }

    @Test
    public void testGetHeight2() {
        // getX() should give the y value for the upper left corner
        double minY = projectionRect.getY();
        double maxY = projectionRect.getMaxY();
        double testHeight = maxY - minY;
        double height = projectionRect.getHeight();
        assertEquals(testHeight, height, 0);
    }

    @Test
    public void testGetLowerLeftPoint() {
        ProjectionPoint getllp = projectionRect.getLowerLeftPoint();
        double llx = projectionRect.getMinX();
        double lly = projectionRect.getMinY();
        double urx = projectionRect.getMaxX();
        double ury = projectionRect.getMaxY();

        assertEquals(llx, getllp.getX(), 0);
        assertEquals(lly, getllp.getY(), 0);
        assertNotEquals(urx, getllp.getX());
        assertNotEquals(ury, getllp.getY());
    }

    @Test
    public void testGetUpperRightPoint() {
        ProjectionPoint geturp = projectionRect.getUpperRightPoint();

        double llx = projectionRect.getMinX();
        double lly = projectionRect.getMinY();
        double urx = projectionRect.getMaxX();
        double ury = projectionRect.getMaxY();

        assertEquals(urx, geturp.getX(), 0);
        assertEquals(ury, geturp.getY(), 0);
        assertNotEquals(llx, geturp.getX());
        assertNotEquals(lly, geturp.getY());
    }

    @Test
    public void testSetX() {

        double x = projectionRect.getX();
        double x2 = x * x + 1d;
        projectionRect.setX(x2);

        assertEquals(x2, projectionRect.getX(),0);
        assertEquals(x2, projectionRect.getMinX(), 0);
        assertNotEquals(x, x2);
    }

    @Test
    public void testSetY() {

        double y = projectionRect.getY();
        double y2 = y * y + 1d;
        projectionRect.setY(y2);

        assertEquals(y2, projectionRect.getY(),0);
        assertEquals(y2, projectionRect.getMinY(), 0);
        assertNotEquals(y, y2);
    }

    @Test
    public void testSetWidth() {

        double width = projectionRect.getWidth();
        double width2 = width + 10d;
        projectionRect.setWidth(width2);

        assertEquals(width2, projectionRect.getWidth(),0);
        assertNotEquals(width, width2);
    }

    @Test
    public void testSetHeight() {

        double height = projectionRect.getHeight();
        double height2 = height + 10d;
        projectionRect.setHeight(height2);

        assertEquals(height2, projectionRect.getHeight(),0);
        assertNotEquals(height, height2);
    }

    @Test
    public void testContainsPoint() {
        // contains the center point? -> YES
        assert(projectionRect.contains(new ProjectionPointImpl(projectionRect.getCenterX(),
                projectionRect.getCenterY())));
        // contains a point outside the rectangle? -> NO
        assert(!projectionRect.contains(new ProjectionPointImpl((projectionRect.getMinX() - projectionRect.getWidth()),
                (projectionRect.getMinY() - projectionRect.getHeight()))));
        assert(!projectionRect.contains(new ProjectionPointImpl((projectionRect.getMaxX() + projectionRect.getWidth()),
                (projectionRect.getMaxY() + projectionRect.getHeight()))));
        // contains a point on the rectangle border -> YES
        assert(projectionRect.contains(new ProjectionPointImpl(projectionRect.getMinPoint())));
    }

    private ProjectionRect scaleShiftRect(double scaleFactor, double deltaX, double deltaY) {
        // quick and dirty method to scale and shift a rectangle, based on projectionRect
        double centerX = projectionRect.getCenterX();
        double centerY = projectionRect.getCenterY();
        double width = projectionRect.getWidth();
        double height = projectionRect.getHeight();

        double testMinX = (centerX + deltaX) - scaleFactor * (width/2);
        double testMinY = (centerY + deltaY) - scaleFactor * (height/2);

        return new ProjectionRect(new ProjectionPointImpl(testMinX, testMinY),
                scaleFactor * width,
                scaleFactor * height);
    }

    @Test
    public void testContainsRect() {
        // contains a bigger rect? -> NO
        assert(!projectionRect.contains(scaleShiftRect(2.0, 0, 0)));
        // contains a smaller rect? -> YES
        assert(projectionRect.contains(scaleShiftRect(0.5, 0, 0)));
        // contains the same rect? -> YES
        assert(projectionRect.contains(scaleShiftRect(1.0, 0, 0)));

        // contains a bigger rect, offset by 0.1? -> NO
        assert(!projectionRect.contains(scaleShiftRect(2.0, 0.1, 0.1)));
        // contains a smaller rect, offset by 0.1? -> YES
        assert(projectionRect.contains(scaleShiftRect(0.5, 0.1, 0.1)));
        // contain the same rect, offset by 0.1? -> NO
        assert(!projectionRect.contains(scaleShiftRect(1.0, 0.1, 0.1)));

    }
}
