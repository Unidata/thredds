package ucar.nc2.ft2.simpgeometry;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ucar.nc2.ft2.simpgeometry.CFPolygon;
import ucar.nc2.ft2.simpgeometry.Point;
import ucar.nc2.ft2.simpgeometry.Polygon;

/**
 * Tests for simple construction of a CF Polygon.
 * 
 * @author wchen@usgs.gov
 *
 */
public class TestCFPolygon {

	private static final double delt = 0.00;
	private static final int testsize = 8;
	private Random rnd = new Random();
	
	@Test
	public void testPolygonSingle() {
		CFPolygon polygon = new CFPolygon();
		polygon.addPoint(0.2, 0.05);
		Assert.assertEquals(0.2, polygon.getPoints().get(0).getX(), delt);
		Assert.assertEquals(0.05, polygon.getPoints().get(0).getY(), delt);
		Assert.assertEquals(null, polygon.getNext());
		Assert.assertEquals(null, polygon.getPrev());
		Assert.assertEquals(false, polygon.getInteriorRing());
		Assert.assertEquals(1, polygon.getPoints().size());
	}
	
	@Test
	public void testPolygonBckwd() {
		
		CFPolygon poly[] = new CFPolygon[testsize];
		CFPolygon refPoly[] = new CFPolygon[testsize];
		double refX[] = new double[testsize];
		double refY[] = new double[testsize];
		
		/* Test adding a point to
		 * the polygon
		 */
		
		poly[0] = new CFPolygon();
		
		for(int i = 0; i < testsize; i++) {
			refX[i] = rnd.nextDouble();
			refY[i] = rnd.nextDouble();
			poly[0].addPoint(refX[i], refY[i]);
		}
		
		List<Point> pts = poly[0].getPoints();
		
		for(int i = 0; i < testsize; i++) {
			Assert.assertEquals(refX[i], pts.get(i).getX(), delt);
			Assert.assertEquals(refY[i], pts.get(i).getY(), delt);
		}
		
		/* Test multipoly
		 */
		
		for(int i = 0; i < testsize; i++) {
			poly[i] = new CFPolygon();
			poly[i].addPoint(rnd.nextDouble(), rnd.nextDouble());
			
			if(i != 0)
			{
				poly[i].setPrev(poly[i - 1]);
			}
			
			refPoly[i] = poly[i];
		}
		
		
		/* Test
		 * forwards and backwards
		 */
		Polygon cpoly = poly[0];
		int k = 0;
		while(cpoly != null) {
			Assert.assertEquals(refPoly[k], cpoly);
			cpoly = cpoly.getNext();
			k++;
		}
		
		cpoly = poly[testsize - 1];
		k = testsize - 1;
		while(cpoly != null) {
			Assert.assertEquals(refPoly[k], cpoly);
			cpoly = cpoly.getPrev();
			k--;
		}
	}
		
	@Test
	public void testPolygonFwd() {
		
		Polygon poly[] = new CFPolygon[testsize];
		Polygon refPoly[] = new CFPolygon[testsize];
		
		/* Try with set next
		 * 
		 */
		for(int i = 0; i < testsize; i++) {
			poly[i] = new CFPolygon();
			poly[i].addPoint(rnd.nextDouble(), rnd.nextDouble());
			
			if(i != 0) {
				poly[i].setNext(poly[i - 1]);
			}
			
			refPoly[i] = poly[i];
		}
		
		/* Test
		 * forwards and backwards
		 */
		Polygon cpoly = poly[0];
		int k = 0;
		while(cpoly != null) {
			Assert.assertEquals(refPoly[k], cpoly);
			cpoly = cpoly.getPrev();
			k++;
		}
		
		cpoly = poly[testsize - 1];
		k = testsize - 1;
		while(cpoly != null) {
			Assert.assertEquals(refPoly[k], cpoly);
			cpoly = cpoly.getNext();
			k--;
		}
	}

	@Test
	public void testPolygonInteriorRing() {
		// Test interior ring
		CFPolygon testOut = new CFPolygon();
		testOut.setInteriorRing(true);
		Assert.assertEquals(testOut.getInteriorRing(), true);
	}
	
	@Test
	public void testEmptyPolygon() {
		CFPolygon empty = new CFPolygon();
		Assert.assertEquals(null, empty.getData());
		Assert.assertEquals(0, empty.getPoints().size());
	}
}
