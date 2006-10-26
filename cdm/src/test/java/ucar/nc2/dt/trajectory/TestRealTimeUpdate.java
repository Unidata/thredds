// $Id: TestRealTimeUpdate.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dt.trajectory;

import junit.framework.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
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
      StringBuffer errlog = new StringBuffer();
      trajDs = (TrajectoryObsDataset) TypedDatasetFactory.open(thredds.catalog.DataType.TRAJECTORY, testDataFileOut, null, errlog);
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