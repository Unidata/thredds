// $Id:Doradeiosp.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.dorade;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.atd.dorade.*;


import java.io.*;
import java.util.*;
//import java.awt.image.BufferedImage;

/**
 * IOServiceProvider implementation abstract base class to read/write "version 3" netcdf files.
 *  AKA "file format version 1" files.
 *
 *  see   concrete class
 */

public class Doradeiosp implements ucar.nc2.IOServiceProvider {

  protected boolean readonly;
  private ucar.nc2.NetcdfFile ncfile;
  private ucar.unidata.io.RandomAccessFile myRaf;
  protected Doradeheader headerParser;

  public static DoradeSweep mySweep = null;
  boolean littleEndian;

  public void setProperties( List iospProperties) { }
  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, java.util.List section)
         throws java.io.IOException, ucar.ma2.InvalidRangeException {

    throw new UnsupportedOperationException("Dorade IOSP does not support nested variables");       
  }
  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf )
    {
        Doradeheader localHeader = new Doradeheader();
        return( localHeader.isValidFile( raf ));
    }


  /////////////////////////////////////////////////////////////////////////////
  // reading

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile file,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    ncfile = file;
    myRaf = raf;

    try {
        mySweep = new DoradeSweep(raf.getLocation());
     } catch (DoradeSweep.DoradeSweepException ex) {
            ex.printStackTrace();

    } catch (java.io.IOException ex) {
            ex.printStackTrace();
    }

    if(mySweep.getScanMode(0)  != ScanMode.MODE_SUR) {
            System.err.println("ScanMode is : " + mySweep.getScanMode(0).getName());
            //System.exit(1);
	}
    try {
       headerParser = new Doradeheader();
       headerParser.read(mySweep, ncfile, null);
    } catch (DoradeSweep.DoradeSweepException e) {
        e.printStackTrace(); 
    }

    ncfile.finish();


  }

  public void open1(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile file,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {

    ncfile = file;

    try {
        mySweep = new DoradeSweep(raf.getLocation());
     } catch (DoradeSweep.DoradeSweepException ex) {
            ex.printStackTrace();

    } catch (java.io.IOException ex) {
            ex.printStackTrace();
    }

    if(mySweep.getScanMode(0)  != ScanMode.MODE_SUR) {
            System.err.println("Skipping:" + raf.getLocation());
            System.exit(1);
	}

    try {
        headerParser = new Doradeheader();
        headerParser.read(mySweep, ncfile, null);
    } catch (DoradeSweep.DoradeSweepException e) {
         e.printStackTrace();
    }

    ncfile.finish();


  }
  public Array readData(ucar.nc2.Variable v2, java.util.List section) throws IOException, InvalidRangeException  {

    Array outputData = null;
    int nSensor = mySweep.getNSensors();
    int nRays = mySweep.getNRays();

    if ( v2.getName().equals( "elevation"))
    {
        float [] elev = mySweep.getElevations();
        outputData = readData1(v2, section, elev);
        //outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), elev);
    }
    else if ( v2.getName().equals( "rays_time"))
    {
        Date [] dd = mySweep.getTimes();
        double [] d  = new double [dd.length];
        for(int i = 0; i < dd.length; i++ )
            d[i] = (double) dd[i].getTime();
        outputData = readData2(v2, section, d);
        //outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), d);
    }
    else if (v2.getName().equals( "azimuth"))
    {
        float [] azim = mySweep.getAzimuths();
        outputData = readData1(v2, section, azim);
        //outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), azim);
    }
    else if (v2.getName().startsWith( "latitudes_"))
    {
        float [] allLats = new float[nSensor*nRays];
        float [] lats = null;
        for (int i = 0; i < nSensor; i++){
           lats = mySweep.getLatitudes(i);
           System.arraycopy(lats, 0, allLats, i*nRays, nRays);
        }
        outputData = readData1(v2, section, allLats);
        //outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), allLats);
    }
    else if (v2.getName().startsWith( "longitudes_"))
    {
        float [] allLons = new float[nSensor*nRays];
        float [] lons = null;
        for (int i = 0; i < nSensor; i++){
           lons = mySweep.getLongitudes(i);
           System.arraycopy(lons, 0, allLons, i*nRays, nRays);
        }
        outputData = readData1(v2, section, allLons);
        //outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), lons);
    }
    else if (v2.getName().startsWith( "altitudes_"))
    {
        float [] allAlts = new float [nSensor*nRays];
        float [] alts = null;
        for (int i = 0; i < nSensor; i++){
           alts = mySweep.getAltitudes(i);
           System.arraycopy(alts, 0, allAlts, i*nRays, nRays);
        }
        outputData = readData1(v2, section, allAlts);
        //outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), alts);
    }
    else if (v2.getName().startsWith( "distance_"))
    {
        float [] dist = null;
        int j = 0;
        for (int i = 0; i < nSensor; i++)  {
            String t  = "" + i;
            if( v2.getName().endsWith(t) ) {
                j = i ;
                break;
            }
        }
        int nc = mySweep.getNCells(j);
        Array data = NetcdfDataset.makeArray( DataType.FLOAT, nc,
          (double) mySweep.getRangeToFirstCell(j), (double) mySweep.getCellSpacing(j));
        dist = (float [])data.get1DJavaArray(Float.class);
        outputData = readData1(v2, section, dist);;

    }
    else if(v2.isScalar())
    {

        float d = 0.0f;

        if( v2.getName().equals("Range_to_First_Cell") )
        {
              d = mySweep.getRangeToFirstCell(0);
        }
        else if (v2.getName().equals("Cell_Spacing"))
        {
              d = mySweep.getCellSpacing(0);
        }
        else if (v2.getName().equals("Fixed_Angle"))
        {
              d = mySweep.getFixedAngle();
        }
        else if (v2.getName().equals("Nyquist_Velocity"))
        {
              d = mySweep.getUnambiguousVelocity(0);
        }
        else if (v2.getName().equals("Unambiguous_Range"))
        {
              d = mySweep.getunambiguousRange(0);
        }
        else if (v2.getName().equals("Radar_Constant"))
        {
              d = mySweep.getradarConstant(0);
        }else if (v2.getName().equals("rcvr_gain"))
        {
              d = mySweep.getrcvrGain(0);
        }
        else if (v2.getName().equals("ant_gain"))
        {
              d = mySweep.getantennaGain(0);
        }
        else if (v2.getName().equals("sys_gain"))
        {
              d = mySweep.getsystemGain(0);
        }
        else if (v2.getName().equals("bm_width"))
        {
              d = mySweep.gethBeamWidth(0);
        }
       float [] dd = new float[1];
       dd[0] = d;
       outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), dd);
    }
    else
    {
        Range radialRange = (Range) section.get(0);
        Range gateRange = (Range) section.get(1);

        Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), Range.getShape( section));
        IndexIterator ii = data.getIndexIterator();

        DoradePARM dp = mySweep.lookupParamIgnoreCase(v2.getName());
        int ncells = dp.getNCells();
        float[] rayValues = new float[ncells];
       /*
        float[] allValues = new float[nRays * ncells];
        for (int r = 0; r < nRays; r++) {
            try {
                rayValues = mySweep.getRayData(dp, r, rayValues);
            } catch (DoradeSweep.DoradeSweepException ex) {
                 ex.printStackTrace();
            }
            System.arraycopy(rayValues, 0, allValues, r*ncells, ncells);
        }    */
        for (int r=radialRange.first(); r<=radialRange.last(); r+= radialRange.stride()) {
            try {
                rayValues = mySweep.getRayData(dp, r, rayValues);
            } catch (DoradeSweep.DoradeSweepException ex) {
                 ex.printStackTrace();
            }
            for (int i=gateRange.first(); i<=gateRange.last(); i+= gateRange.stride()){
                  ii.setFloatNext( rayValues[i]);
            }

        }
        return data;
        //outputData = Array.factory( v2.getDataType().getPrimitiveClassType(), v2.getShape(), allValues);
    }

    return outputData;


  }

  public Array readData1(Variable v2, java.util.List section, float[] values)
  {
      Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), Range.getShape( section));
      IndexIterator ii = data.getIndexIterator();
      Range radialRange = (Range) section.get(0);     // radial range can also be gate range

      for (int r=radialRange.first(); r<=radialRange.last(); r+= radialRange.stride()) {
             ii.setFloatNext( values[r]);
      }

      return data;
  }

  public Array readData2(Variable v2, java.util.List section, double[] values)
  {
      Array data = Array.factory(v2.getDataType().getPrimitiveClassType(), Range.getShape( section));
      IndexIterator ii = data.getIndexIterator();
      Range radialRange = (Range) section.get(0);

      for (int r=radialRange.first(); r<=radialRange.last(); r+= radialRange.stride()) {
             ii.setDoubleNext(values[r]);
      }

      return data;
  }
  // all the work is here, so can be called recursively
  public Array readData(ucar.nc2.Variable v2, long dataPos, int [] origin, int [] shape, int [] stride) throws IOException, InvalidRangeException  {

     return null;
  }
    // for the compressed data read all out into a array and then parse into requested




   // convert byte array to char array
  static protected char[] convertByteToChar( byte[] byteArray) {
    int size = byteArray.length;
    char[] cbuff = new char[size];
    for (int i=0; i<size; i++)
      cbuff[i] = (char) byteArray[i];
    return cbuff;
  }

   // convert char array to byte array
  static protected byte[] convertCharToByte( char[] from) {
    int size = from.length;
    byte[] to = new byte[size];
    for (int i=0; i<size; i++)
      to[i] = (byte) from[i];
    return to;
  }



  protected boolean fill;
  protected HashMap dimHash = new HashMap(50);

  public void flush() throws java.io.IOException {
    myRaf.flush();
  }

  public void close() throws java.io.IOException {
    myRaf.close();
  }

  public boolean syncExtend() { return false; }
  public boolean sync() { return false; }

  /** Debug info for this object. */
  public String toStringDebug(Object o) { return null; }
  public String getDetailInfo() { return ""; }

  public static void main(String args[]) throws Exception, IOException, InstantiationException, IllegalAccessException {
    String fileIn = "/home/yuanho/dorade/swp.1020511015815.SP0L.573.1.2_SUR_v1";
    //String fileIn = "c:/data/image/Dorade/n0r_20041013_1852";
    ucar.nc2.NetcdfFile.registerIOProvider( ucar.nc2.iosp.dorade.Doradeiosp.class);
    ucar.nc2.NetcdfFile ncf = ucar.nc2.NetcdfFile.open(fileIn);

    //List alist = ncf.getGlobalAttributes();
    ucar.unidata.io.RandomAccessFile file = new ucar.unidata.io.RandomAccessFile(fileIn, "r");

    //open1(file, null, null);
    //ucar.nc2.Variable v = ncf.findVariable("BaseReflectivity");

    //ncf.close();


  }


}

/* Change History:
   $Log: Doradeiosp.java,v $
   Revision 1.12  2006/04/19 20:24:49  yuanho
   radial dataset sweep for all radar dataset

   Revision 1.11  2006/04/03 22:59:18  caron
   IOSP.readNestedData() remove flatten, handle flatten=false in NetcdfFile.readMemberData(); this allows IOSPs to be simpler
   add metar decoder from Robb's thredds.servlet.ldm package

   Revision 1.10  2006/01/17 23:07:13  caron
   *** empty log message ***

   Revision 1.9  2006/01/04 00:02:31  caron
   dods src under our CVS
   forecastModelRun aggregation
   substitute M3IOVGGrid for M3IO coordSysBuilder
   iosp setProperties uses list.
   use jdom 1.0

   Revision 1.8  2005/12/15 00:29:11  caron
   *** empty log message ***

   Revision 1.7  2005/12/09 04:24:37  caron
   Aggregation
   caching
   sync

   Revision 1.6  2005/08/08 22:44:55  yuanho
   spelling bug fix

   Revision 1.5  2005/07/25 22:20:06  caron
   add iosp.synch()

   Revision 1.4  2005/07/12 21:22:57  yuanho
   remove static

   Revision 1.3  2005/06/11 18:42:00  caron
   no message

   Revision 1.2  2005/05/23 21:52:55  caron
   add getDetailInfo() to IOSP for error/debug info

   Revision 1.1  2005/04/26 19:39:06  yuanho
   iosp for dorade format radar data

   Revision 1.4  2005/01/05 22:47:14  caron
   no message

   Revision 1.3  2004/10/15 23:18:44  yuanho
   Dorade projection update

   Revision 1.2  2004/10/14 17:14:31  caron
   add Dorade reader
   add imageioreader for PNG

   Revision 1.1  2004/10/13 22:58:14  yuanho
   no message

   Revision 1.6  2004/08/17 19:20:04  caron
   2.2 alpha (2)

   Revision 1.5  2004/08/17 00:09:13  caron
   *** empty log message ***

   Revision 1.4  2004/08/16 20:53:45  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:17  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:10  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */