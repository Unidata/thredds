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
package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;

/**
 * 
 * Sometimes all the vars required in the vertical transformation may not have coherent units.
 * This parameterized unit test tests those transformations that make unit checks:
 * 	- AtmosSigma
 * 	- HybridSigma (with P param and with AP param)    
 * 
 *  
 * @author mhermida
 *
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestVerticalTransformWithUnitsConversion {
	
	private String sameUnitsFile;
	private String diffUnitsFile;
	private LatLonPoint point;
	private String var;
	
	public TestVerticalTransformWithUnitsConversion(String sameUnitsFile, String diffUnitsFile, LatLonPoint point, String var){
		
		this.sameUnitsFile=sameUnitsFile;
		this.diffUnitsFile =diffUnitsFile;
		this.point=point;
		this.var=var;
		
	}
	
	@Parameters(name="{0}")
	public static Collection<Object[]> data(){
		Object[][] data = new Object[][]{
				{TestDir.cdmUnitTestDir + "transforms/idv_sigma.ncml", "/share/testdata/cdmUnitTest/transforms/idv_sigma.nc", new LatLonPointImpl(52.85, 27.56), "VNK201302" }, //AtmosSigma
				{TestDir.cdmUnitTestDir + "transforms/HybridSigmaPressure.nc", "/share/testdata/cdmUnitTest/transforms/HybridSigmaPressure.ncml", new LatLonPointImpl( 40.019, -105.293 ), "T"} ,    //HybridSigma with P
				{TestDir.cdmUnitTestDir + "transforms/HIRLAMhybrid.ncml","/share/testdata/cdmUnitTest/transforms/HIRLAMhybrid_hPa.ncml", new LatLonPointImpl( 42.86, -8.55 ), "Relative_humidity_hybrid"}  //HybridSigma with AP
		};

		return Arrays.asList(data);
	}

	
	@Test
	public void shouldGetSameVerticalProfile() throws IOException, InvalidRangeException{
		System.out.printf("Open %s%n", sameUnitsFile);
		NetcdfDataset dsGood = NetcdfDataset.acquireDataset(sameUnitsFile, null);
		GridDataset gdsGood = new GridDataset(dsGood);

		GeoGrid gridGood =  gdsGood.findGridByName(var);		
		ProjectionImpl proj = gridGood.getProjection();
		ProjectionPoint pp = proj.latLonToProj(point);
				
		double[] dataGood = getVertTransformationForPoint(pp, 0, gridGood);
		
		NetcdfDataset dsDiff = NetcdfDataset.acquireDataset(sameUnitsFile, null);
		GridDataset gdsDiff = new GridDataset(dsDiff);

		GeoGrid gridDiff =  gdsDiff.findGridByName(var);		
		proj = gridDiff.getProjection();
		pp = proj.latLonToProj(point);
		
		double[] dataDiff = getVertTransformationForPoint(pp, 0, gridDiff);
		
		assertArrayEquals(dataGood, dataDiff, 0.00001);
		
	}
	
	private double[] getVertTransformationForPoint(ProjectionPoint point, int timeIndex, GeoGrid grid) throws IOException, InvalidRangeException{

		VerticalTransform vt =  grid.getCoordinateSystem().getVerticalTransform();
		//System.out.println(vt.isTimeDependent());
		int[] pointIndices = new int[]{0,0}; 
		
		grid.getCoordinateSystem().findXYindexFromCoord( point.getX(), point.getY(), pointIndices);		
		
		ArrayDouble.D1 dataArr = vt.getCoordinateArray1D(timeIndex, pointIndices[0], pointIndices[1]);
		
		return (double[])dataArr.copyTo1DJavaArray();
	}	
	
}
