/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
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
// $Id: TestRealTimeUpdate.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dt.trajectory;

import junit.framework.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.ma2.Array;

import java.io.IOException;
import java.util.Date;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 7, 2006 10:10:45 AM
 */
public class TestRealTimeUpdate extends TestCase
{

  private String testDataFileOut = "test.TrajectoryRealTimeUpdate.tmp.nc";

  public TestRealTimeUpdate( String name )
  {
    super( name );
  }

  protected void setUp()
  {
  }

  /**
   * Test ...
   */
  public void testMuckWithNcfSynchExtend()
  {
    NetcdfFile ncf = null;
    try
    {
      ncf = NetcdfFile.open( testDataFileOut );
    }
    catch ( IOException e )
    {
      assertTrue( "Couldn't open file <" + testDataFileOut + ">: " + e.getMessage(),
                  false );
      return;
    }

    Dimension timeDim = ncf.getRootGroup().findDimension( "time");
    int timeDimSize = timeDim.getLength();
    Variable timeVar = ncf.getRootGroup().findVariable( "time");
    Variable latVar = ncf.getRootGroup().findVariable( "latitude");
    Variable lonVar = ncf.getRootGroup().findVariable( "longitude");
    Variable altVar = ncf.getRootGroup().findVariable( "altitude");

    Attribute timeUnits = timeVar.findAttribute( "units");
    String newUnits = "test unit string";
    System.out.println( "Switching time units from \"" + timeUnits.toString() + " \" to \"" + newUnits + "\"" );
    timeVar.addAttribute( new Attribute( "units", newUnits) );

    Array latArray;
    try
    {
      latArray = latVar.read();
    }
    catch ( IOException e )
    {
      assertTrue( "Couldn't read latitude data: " + e.getMessage(),
                  false );
      return;
    }

    try { Thread.sleep( 5000);
    }
    catch ( InterruptedException e ) {
    }

    boolean extended;
    try
    {
      extended = ncf.syncExtend();
    }
    catch ( IOException e )
    {
      assertTrue( "Couldn't syncExtend() file <" + testDataFileOut + ">: " + e.getMessage(),
                  false );
      return;
    }

    if ( ! extended )
    {
      System.out.println( "Did not extend file <" + testDataFileOut + ">." );
    }

    Dimension timeDim2 = ncf.getRootGroup().findDimension( "time");
    if ( timeDim == timeDim2 )
      System.out.println( "Time dimension the same." );
    else
      System.out.println( "Time dimension not the same." );

    System.out.println( "Initial time dim size = " + timeDimSize );
    System.out.println( "New time dim size = " + timeDim2.getLength() );
    System.out.println( "New time dim size (1) = " + timeDim.getLength() );

    System.out.println( "Time var units string: " + timeVar.findAttribute( "units").toString() );

    if ( latVar == ncf.getRootGroup().findVariable( "latitude"))
      System.out.println( "Lat var the same." );
    else
      System.out.println( "Lat var not the same." );
  }

  public void testTwo()
  {
    TrajectoryObsDataset trajDs;
    try
    {
      //trajDs = TrajectoryObsDatasetFactory.open( testDataFileOut );
      StringBuilder errlog = new StringBuilder();
      trajDs = (TrajectoryObsDataset) TypedDatasetFactory.open(FeatureType.TRAJECTORY, testDataFileOut, null, errlog);
    }
    catch ( IOException e )
    {
      assertTrue( "Couldn't open TrajectoryObsDataset <" + testDataFileOut + ">: " + e.getMessage(),
                  false );
      return;
    }

    Date startDate = trajDs.getStartDate();
    Date endDate = trajDs.getEndDate();
    System.out.println( "Dataset start date=" + startDate );
    System.out.println( "Dataset end date  =" + endDate );
    TrajectoryObsDatatype traj = trajDs.getTrajectory( "trajectory data");
    Date trajEndDate;
    Array presArray;
    try
    {
      trajEndDate = traj.getTime( traj.getNumberPoints() - 1);
      presArray = traj.getData( traj.getNumberPoints() - 1, "HADS_A");
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to read last time or pressure from <" + testDataFileOut + ">: " + e.getMessage(),
                  false );
      return;
    }
    System.out.println( "Traj end date  =" + trajEndDate );
    System.out.println( "Pressure end value=" + presArray.getFloat( presArray.getIndex()));


    try
    {
      Thread.sleep( 10000 );
    }
    catch ( InterruptedException e )
    {
    }

    if ( ! trajDs.syncExtend() )
      System.out.println( "File not extended." );
    else
      System.out.println( "File extended" );

    startDate = trajDs.getStartDate();
    endDate = trajDs.getEndDate();
    System.out.println( "Start date=" + startDate );
    System.out.println( "End date  =" + endDate );

    try
    {
      trajEndDate = traj.getTime( traj.getNumberPoints() - 1 );
      presArray = traj.getData( traj.getNumberPoints() - 1, "HADS_A" );
    }
    catch ( IOException e )
    {
      assertTrue( "Failed to read last time or pressure from <" + testDataFileOut + ">: " + e.getMessage(),
                  false );
      return;
    }
    System.out.println( "Traj end date  =" + trajEndDate );
    System.out.println( "Pressure end value=" + presArray.getFloat( presArray.getIndex() ) );
  }
}
/*
 * $Log: TestRealTimeUpdate.java,v $
 * Revision 1.1  2006/02/09 22:57:12  edavis
 * Add syncExtend() method to TrajectoryObsDataset.
 *
 */