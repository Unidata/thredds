package ucar.nc2.ft2.simpgeometry;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ucar.nc2.ft2.simpgeometry.CFPoint;
import ucar.nc2.ft2.simpgeometry.Point;

/**
 * Tests for simple construction of a CF Point.
 * 
 * @author wchen@usgs.gov
 *
 */
public class TestCFPoint {

	private static final double delt = 0.00;
	private static final int testsize = 8;
	private Random rnd = new Random();
	
	@Test
	public void testPointSingle() {
		CFPoint pt = new CFPoint(0.1, 0.3, null, null, null);
		Assert.assertEquals(0.1, pt.getX(), delt);
		Assert.assertEquals(0.3, pt.getY(), delt);
		Assert.assertEquals(null, pt.getNext());
		Assert.assertEquals(null, pt.getPrev());
	}
	
	@Test
	public void testPtBckwd() {		
		
		CFPoint pt[] = new CFPoint[testsize];
		double refX[] = new double[testsize];
		double refY[] = new double[testsize];
		
		// Make the points
		for(int i = 0; i < testsize; i++) {
			double x = rnd.nextDouble();
			double y = rnd.nextDouble();
			
			if(i == 0) {
				pt[i] = new CFPoint(x, y, null, null, null);
			}
			
			else {
				pt[i] = new CFPoint(x, y, pt[i-1], null, null);
			}
			
			refX[i] = x;
			refY[i] = y;
		}
		
		// Forward
		Point testPt = pt[0];
		int k = 0;
				
		while(testPt != null) {
			Assert.assertEquals(refX[k], testPt.getX(), delt);
			Assert.assertEquals(refY[k], testPt.getY(), delt);
			testPt = testPt.getNext();
			k++;
		}
		
		// Backward
		testPt = pt[testsize - 1];
		k = testsize - 1;
			
		while(testPt != null){
				if(refX[k] != testPt.getX()) Assert.assertEquals(refX[k], testPt.getX(), delt);
				if(refY[k] != testPt.getY()) Assert.assertEquals(refY[k], testPt.getY(), delt);
				testPt = testPt.getPrev();	
				k--;
		}
	
		
	}
	
	@Test
	public void testPtFwd() {
	
		// Make some more points test backwards setting
		CFPoint pt[] = new CFPoint[testsize];
		double refX[] = new double[testsize];
		double refY[] = new double[testsize];
		
		for(int i = 0; i < testsize; i++){
			
			double x = rnd.nextDouble();
			double y = rnd.nextDouble();
				
			if(i == 0) {
				pt[i] = new CFPoint(x, y, null, null, null);
			}
				
			else {
				pt[i] = new CFPoint(x, y, pt[i - 1], null, null);
			}
				
			refX[i] = x;
			refY[i] = y;
		}
		
		/* Test them out, very similar test
		 * Except this time, the points are backwards order
		 */
		Point testPt = pt[0];
		int k = 0;
		
		// Backward
		testPt = pt[0];
		k = 0;
						
		while(testPt != null) {
			Assert.assertEquals(refX[k], testPt.getX(), delt);
			Assert.assertEquals(refY[k], testPt.getY(), delt);
			testPt = testPt.getPrev();
			k++;
		}
				
		// Forward
		testPt = pt[testsize - 1];
		k = testsize - 1;
		
		while(testPt != null) {
			
			Assert.assertEquals(refX[k], testPt.getX(), delt);
			Assert.assertEquals(refY[k], testPt.getY(), delt);	
			testPt = testPt.getNext();	
			k--;
		}
		
	}
}
