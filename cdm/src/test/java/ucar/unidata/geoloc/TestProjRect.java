/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package ucar.unidata.geoloc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

/** Test ncml value element in the JUnit framework. */
@RunWith(Parameterized.class)
public class TestProjRect {

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

}
