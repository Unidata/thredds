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
// $Id: TestDODSStructureForSequence.java 51 2006-07-12 17:13:13Z caron $
package ucar.nc2.dods;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.nc2.Structure;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;

/**
 * Test DODS Sequence access using DODSStructure.
 */
public class TestDODSStructureForSequence extends TestCase
{
  public TestDODSStructureForSequence( String name)
  {
    super(name);
  }

  public void testConstrainedAccess()
  {
    // URL for the JPL QuikSCAT DODS File Server (DFS).
    String dfsURL = "http://dods.jpl.nasa.gov/dods-bin/nph-dods/catalogs/quikscat/L2B/quikscat_L2.dat";
    String seqName = "QuikSCAT_L2B";
    String catTitleAttName = "DODS_Global.DODS_Title";
    String catAllowedDateRangeStartAttName = "DODS_Global.DODS_StartDate";
    String catAllowedDateRangeEndAttName = "DODS_Global.DODS_EndDate";

    // Connect to the DFS.
    DODSNetcdfFile dfs = null;
    try
    {
      dfs = new DODSNetcdfFile( dfsURL);
    }
    catch (IOException e)
    {
      assertTrue( "Unexpected IOException connecting to JPL DODS File Server <" + dfsURL + ">: " + e.getMessage(),
                  false);
    }

    // Get the DODS structure for this DFS catalog.
    DODSStructure struct = (DODSStructure) dfs.findVariable( seqName);
    if ( struct == null)
    {
      assertTrue( "Did not find \"" + seqName + "\" variable on JPL DFS.",
                  false);
    }


    //String ce = "date_time(\"alkjdf\",\"eriu\")&" + seqName + ".longitude>";
    String ce = "&" + seqName + ".longitude>359.9" + "&" + seqName + ".longitude<359.9205";
    StructureDataIterator dodsIt;
    try
    {
      dodsIt = struct.getStructureIterator( ce);
    }
    catch ( IOException e )
    {
      assertTrue( "Unexpected IOException getting structure iterator for constraint <" + ce + ">: " + e.getMessage(),
                  false);
      return;
    }

    entryTest( dodsIt, new BasicEntry( 2000, 86, 5, 55, 27, 566, 359.9182F, 3999, 1624, "http://dods.jpl.nasa.gov/cgi-bin/nph-dods/pub/ocean_wind/quikscat/L2B/data/2000/086/QS_S2B03999.20001670430.Z"),
               0.0001F);
    entryTest( dodsIt, new BasicEntry( 2003, 276, 5, 55, 16, 791, 359.9203F, 22331, 1624, "http://dods.jpl.nasa.gov/cgi-bin/nph-dods/pub/ocean_wind/quikscat/L2B/data/2003/276/QS_S2B22331.20032762156.Z"),
               0.0001F);
  }

  private void entryTest( StructureDataIterator dodsIt, BasicEntry expectedEntry, float longDelta )
  {

    StructureData curData;
    try
    {
      if ( ! dodsIt.hasNext())
        assertTrue( "DODS result does not contain expected entry.", false);
      curData = dodsIt.next();
    }
    catch ( IOException e )
    {
      assertTrue( "Unexpected IOException reading from structure iterator: " + e.getMessage(),
                false);
      return;
    }
    int year = curData.getScalarInt( "year");
    int day = curData.getScalarInt( "day");
    int hours = curData.getScalarInt( "hours");
    int minutes = curData.getScalarInt( "minutes");
    int seconds = curData.getScalarInt( "seconds");
    int m_seconds = curData.getScalarInt( "m_seconds");
    float longitude = curData.getScalarFloat( "longitude");
    int rev_num = curData.getScalarInt( "rev_num");
    int wvc_rows = curData.getScalarInt( "wvc_rows");
    String dodsUrl = curData.getScalarString( "DODS_URL");
//    System.out.println( "year = " + year );
//    System.out.println( "day = " + day );
//    System.out.println( "hours = " + hours );
//    System.out.println( "minutes = " + minutes );
//    System.out.println( "longitude = " + longitude );
//    System.out.println( "dodsUrl = " + dodsUrl );
    if ( ( year != expectedEntry.getYear()) ||
         ( day != expectedEntry.getDay()) ||
         ( hours != expectedEntry.getHours()) ||
         ( minutes != expectedEntry.getMinutes()) ||
         ( seconds != expectedEntry.getSeconds()) ||
         ( m_seconds != expectedEntry.getM_seconds()) ||
         ( longitude > expectedEntry.getLongitude() + longDelta ) ||
         ( longitude < expectedEntry.getLongitude() - longDelta ) ||
         ( rev_num != expectedEntry.getRev_num()) ||
         ( wvc_rows != expectedEntry.getWvc_rows()) ||
         ( ! dodsUrl.equals( expectedEntry.getDodsUrl()) ) )
    {

      System.out.println( "year = " + year + " should be "+ expectedEntry.getYear());
      System.out.println( "day = " + day + " should be "+ expectedEntry.getDay());
      System.out.println( "hours = " + hours + " should be "+ expectedEntry.getHours());
      System.out.println( "minutes = " + minutes + " should be "+ expectedEntry.getMinutes());
      System.out.println( "longitude = " + longitude + " should be > "+ expectedEntry.getLongitude() + longDelta);
      System.out.println( "dodsUrl = " + dodsUrl + " should be "+ expectedEntry.getDodsUrl());

      assertTrue( "Current entry <" +
                  year + "-" + day +
                  "T" + hours + ":" + minutes +
                  ":" + seconds + ":" + m_seconds +
                  " Long: " + longitude + " revNum: " + rev_num +
                  " wvcRows: " + wvc_rows + " dodsUrl: " + dodsUrl +
                  "> is not as expected <" +
                  expectedEntry.getYear() + "-" + expectedEntry.getDay() +
                  "T" + expectedEntry.getHours() + ":" + expectedEntry.getMinutes() +
                  ":" + expectedEntry.getSeconds() + ":" + expectedEntry.getM_seconds() +
                  " Long: " + expectedEntry.getLongitude() + " revNum: " + expectedEntry.getRev_num() +
                  " wvcRows: " + expectedEntry.getWvc_rows() + " dodsUrl: " + expectedEntry.getDodsUrl() +
                  ">.",
                  false);

    }
  }
  
  private class BasicEntry
  {
    private int year;
    private int day;
    private int hours;
    private int minutes;
    private int seconds;
    private int m_seconds;
    private float longitude;
    private int rev_num;
    private int wvc_rows;
    private String dodsUrl;

    public BasicEntry( int year, int day, int hours, int minutes, int seconds,
                       int m_seconds, float longitude, int rev_num, int wvc_rows,
                       String dodsUrl )
    {
      this.year = year;
      this.day = day;
      this.hours = hours;
      this.minutes = minutes;
      this.seconds = seconds;
      this.m_seconds = m_seconds;
      this.longitude = longitude;
      this.rev_num = rev_num;
      this.wvc_rows = wvc_rows;
      this.dodsUrl = dodsUrl;
    }

    public int getYear()
    {
      return year;
    }

    public void setYear( int year )
    {
      this.year = year;
    }

    public int getDay()
    {
      return day;
    }

    public void setDay( int day )
    {
      this.day = day;
    }

    public int getHours()
    {
      return hours;
    }

    public void setHours( int hours )
    {
      this.hours = hours;
    }

    public int getMinutes()
    {
      return minutes;
    }

    public void setMinutes( int minutes )
    {
      this.minutes = minutes;
    }

    public int getSeconds()
    {
      return seconds;
    }

    public void setSeconds( int seconds )
    {
      this.seconds = seconds;
    }

    public int getM_seconds()
    {
      return m_seconds;
    }

    public void setM_seconds( int m_seconds )
    {
      this.m_seconds = m_seconds;
    }

    public float getLongitude()
    {
      return longitude;
    }

    public void setLongitude( float longitude )
    {
      this.longitude = longitude;
    }

    public int getRev_num()
    {
      return rev_num;
    }

    public void setRev_num( int rev_num )
    {
      this.rev_num = rev_num;
    }

    public int getWvc_rows()
    {
      return wvc_rows;
    }

    public void setWvc_rows( int wvc_rows )
    {
      this.wvc_rows = wvc_rows;
    }

    public String getDodsUrl()
    {
      return dodsUrl;
    }

    public void setDodsUrl( String dodsUrl )
    {
      this.dodsUrl = dodsUrl;
    }

    
  }
}

/*
 * $Log: TestDODSStructureForSequence.java,v $
 * Revision 1.5  2006/02/16 23:02:44  caron
 * *** empty log message ***
 *
 * Revision 1.4  2006/02/06 21:17:06  caron
 * more fixes to dods parsing.
 * ArraySequence.flatten()
 * ncml.xml use default namespace. Only way I can get ncml in catalog to validate.
 * ThreddsDataFactory refactor
 *
 * Revision 1.3  2005/07/25 00:07:12  caron
 * cache debugging
 *
 * Revision 1.2  2005/05/11 00:10:10  caron
 * refactor StuctureData, dt.point
 *
 * Revision 1.1  2004/11/19 21:42:28  edavis
 * Test how DODSStructure handles reading DODS sequence (uses JPL QuikSCAT DODS File Server for test server).
 *
 */