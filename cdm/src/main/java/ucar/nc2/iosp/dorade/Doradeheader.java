// $Id:Doradeheader.java 63 2006-07-12 21:50:51Z edavis $
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
package ucar.nc2.iosp.dorade;


import ucar.nc2.*;

import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.MAMath;

import ucar.atd.dorade.*;

import java.io.*;
import java.util.*;

public class Doradeheader {
  private boolean debug = false, debugPos = false, debugString = false, debugHeaderSize = false;
  private ucar.nc2.NetcdfFile ncfile;
  private PrintStream out = System.out;
  private HashMap paramMap;
  private float[] lat_min, lat_max, lon_min, lon_max, hi_max, hi_min;

  static public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    try {
      java.io.RandomAccessFile file = raf.getRandomAccessFile();
      if (file == null) return false;
      boolean t = DoradeSweep.isDoradeSweep(file);
      if (!t) return false;
      //DoradeSweep mySweep = new DoradeSweep(raf.getLocation());
    } catch (DoradeSweep.DoradeSweepException ex) {
      ex.printStackTrace();
      return false;
      // } catch (java.io.IOException ex) {
      //      ex.printStackTrace();
      //     return false;
    }
    return true;
  }

  void read(DoradeSweep mySweep, ucar.nc2.NetcdfFile ncfile, PrintStream out) throws IOException, DoradeSweep.DoradeSweepException {

    this.ncfile = ncfile;
    DoradePARM[] parms = mySweep.getParamList();
    int nRays = mySweep.getNRays();

    if (debug) System.out.println(parms.length + " params in file");

    int numSensor = mySweep.getNSensors();
    int [] ncells = new int[numSensor];
    Dimension [] gateDim =new Dimension[numSensor];
    for(int i = 0; i < numSensor; i++) {
        try {
            int j = i + 1;
            ncells[i] = mySweep.getNCells(i);
            gateDim[i] = new Dimension("gate_"+j, ncells[i]);
            ncfile.addDimension( null, gateDim[i]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    ArrayList []  dims =  new ArrayList[numSensor];
    ArrayList  dims1 = new ArrayList();
    ArrayList [] dims2 = new ArrayList[numSensor];
    int nCells =  mySweep.getNCells(0);


  //  Dimension sensorDim = new Dimension("sensor", numSensor, true);
  //  ncfile.addDimension( null, sensorDim);
    Dimension radialDim = new Dimension("radial", nRays);
    ncfile.addDimension( null, radialDim);


    for(int i = 0; i < numSensor; i++) {
         dims[i] = new ArrayList();
         dims2[i] = new ArrayList();
         dims[i].add( radialDim );
         dims[i].add( gateDim[i]);
         dims2[i].add( gateDim[i]);
    }

   // dims1.add( sensorDim);
    dims1.add( radialDim);


    float [][] altitudes = new float [numSensor][];
    float [][] latitudes = new float [numSensor][];
    float [][] longitudes = new float [numSensor][];
    lat_min = new float[numSensor];
    lat_max = new float[numSensor];
    lon_min = new float[numSensor];
    lon_max = new float[numSensor];
    hi_min = new float[numSensor];
    hi_max = new float[numSensor];
    MAMath.MinMax [] latMinMax = new MAMath.MinMax[numSensor];
    MAMath.MinMax [] lonMinMax = new MAMath.MinMax[numSensor];
    MAMath.MinMax [] hiMinMax = new MAMath.MinMax[numSensor];
    boolean [] isMoving = new boolean[numSensor];
    for(int i = 0; i < numSensor; i++) {
        try {
            int nc = mySweep.getNCells(i);
            altitudes[i] =  new float [nRays];
            latitudes[i] =  new float [nRays];
            longitudes[i] = new float [nRays];
            isMoving[i]  = mySweep.sensorIsMoving(i);
            altitudes[i] = mySweep.getAltitudes(i);
            latitudes[i] = mySweep.getLatitudes(i);
            longitudes[i] = mySweep.getLongitudes(i);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        latMinMax[i] = getMinMaxData( latitudes[i]);
        lonMinMax[i] = getMinMaxData( longitudes[i]);
        hiMinMax[i] = getMinMaxData( altitudes[i]);
        float dis = (float )((mySweep.getRangeToFirstCell(i) + (mySweep.getNCells(i)-1) * mySweep.getCellSpacing(i) ) * 1.853 / 111.26 /1000);
        lat_min[i] = (float )(latMinMax[i].min - dis);
        lat_max[i] = (float )(latMinMax[i].max + dis);
        lon_min[i] = (float )(lonMinMax[i].min + dis * Math.cos(latitudes[i][0]));
        lon_max[i] = (float )(lonMinMax[i].max - dis * Math.cos(latitudes[i][0]));
        hi_min[i] = (float )hiMinMax[i].min;
        hi_max[i] = (float )hiMinMax[i].max;
    }

     //adding the global nc attribute
    addNCAttributes(ncfile, mySweep);


    // add elevation coordinate variable
    String vName = "elevation";
    String lName = "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular";
    Attribute att = new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString());
    addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");

    // add azimuth coordinate variable
    vName = "azimuth";
    lName = "azimuth angle in degrees: 0 = true north, 90 = east";
    att = new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString());
    addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");


    // add gate coordinate variable
    for (int i = 0; i < numSensor; i++)  {
        int j = i + 1;
        vName = "distance_"+j;
        lName = "Radial distance to the start of gate";
        att = new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString());
        addParameter(vName, lName, ncfile, dims2[i], att, DataType.FLOAT, "meters");
    }

    // add radial coordinate variable

    for (int i = 0; i < numSensor; i++)  {
        int j = i + 1;
        vName = "latitudes_" + j;
        lName = "Latitude of the instrument " + j;
        att = new Attribute(_Coordinate.AxisType, AxisType.Lat.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");

        vName = "longitudes_" + j;
        lName = "Longitude of the instrument " + j;
        att = new Attribute(_Coordinate.AxisType, AxisType.Lon.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");

        vName = "altitudes_" + j;
        lName = "Altitude in meters (asl) of the instrument " + j;
        att = new Attribute(_Coordinate.AxisType, AxisType.Height.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "meters");

        vName = "rays_time";
        lName = "rays time";
        att = new Attribute(_Coordinate.AxisType, AxisType.Time.toString());
        addParameter(vName, lName, ncfile, dims1, att, DataType.DOUBLE, "milliseconds since 1970-01-01 00:00 UTC");

    }

    vName = "Range_to_First_Cell";
    lName = "Range to the center of the first cell";
    att = new Attribute(_Coordinate.Axes, "latitudes_1 longitudes_1 altitudes_1");
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "meters");

    vName = "Cell_Spacing";
    lName = "Distance between cells";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "meters");


    vName = "Fixed_Angle";
    lName = "Targeted fixed angle for this scan";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "degrees");

    vName = "Nyquist_Velocity";
    lName = "Effective unambigous velocity";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "m/s");

    vName = "Unambiguous_Range";
    lName = "Effective unambigous range";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "meters");

    vName = "Radar_Constant";
    lName = "Radar constant";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "c");

    vName = "rcvr_gain";
    lName = "Receiver Gain";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "db");

    vName = "ant_gain";
    lName = "Antenna Gain";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "db");

    vName = "sys_gain";
    lName = "System Gain";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "db");

    vName = "bm_width";
    lName = "Beam Width";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "degrees");

    /* Variable ct = new Variable(ncfile, null, null, "radialCoordinateTransform");
      ct.setDataType(DataType.CHAR);
      ct.setDimensions(""); // scalar
      ct.addAttribute( new Attribute("transform_type", "Radial"));
      ct.addAttribute( new Attribute("_CoordinateTransformType", "Radial"));
      ct.addAttribute( new Attribute("_CoordinateAxes", "elevation azimuth distance_1"));
      ncfile.addVariable(null, ct);   */

    try {
          for (int p = 0; p < parms.length; p++) {
              String pval = parms[p].getDescription();
              int ptype = parms[p].getBinaryFormat();
              nCells =  parms[p].getNCells();
              int ii = getGateDimsIndex(nCells, gateDim, numSensor);

              if (debug) System.out.println("Param "+ p+ " name "+pval+" and ncel "+ nCells);
              addVariable(ncfile, dims[ii], parms[p]);
           }
      } catch (Exception ex) {
              ex.printStackTrace();
      }

    // finish
    ncfile.finish();

  }

  public MAMath.MinMax getMinMaxData( float[] data) {
      int[] shape = new int[1];
      shape[0] = data.length;
      Array a = Array.factory( DataType.FLOAT.getPrimitiveClassType(), shape, data);
      return MAMath.getMinMax( a);
  }
  int getGateDimsIndex(int cell, Dimension [] dList, int numSensor)
  {
      int j = 0;
      for(int i = 0; i < numSensor; i++) {
          Dimension d = new Dimension("gate_"+i, cell);
          if( dList[i].equals(d)) {
              j = i;
              break;
          }
      }
      return j;
  }


   private void makeCoordinateData(Variable elev, Variable azim, DoradeSweep mySweep) {
    Object ele = (Object) mySweep.getElevations();
    Object azi = (Object) mySweep.getAzimuths();

    Array elevData = Array.factory( elev.getDataType().getPrimitiveClassType(), elev.getShape(), ele);
    Array aziData = Array.factory( azim.getDataType().getPrimitiveClassType(), azim.getShape(), azi);

    elev.setCachedData( elevData, false);
    azim.setCachedData( aziData, false);
  }

  void addParameter(String pName, String longName, NetcdfFile nc, ArrayList dims, Attribute att, DataType dtype, String ut)
  {
      String vName = pName;
      Variable vVar = new Variable(nc, null, null, vName);
      vVar.setDataType(dtype);
      if( dims != null ) vVar.setDimensions(dims);
      else vVar.setDimensions("");
      if(att != null ) vVar.addAttribute(att);
      vVar.addAttribute( new Attribute("units", ut));
      vVar.addAttribute( new Attribute("long_name", longName));
      nc.addVariable(null, vVar);
  }

  void addVariable(NetcdfFile nc, ArrayList dims, DoradePARM dparm)
  {

      Variable v = new Variable(nc, null, null, dparm.getName() );
      v.setDataType( DataType.FLOAT );
      v.setDimensions(dims);
      ncfile.addVariable(null, v);

      v.addAttribute( new Attribute("long_name", dparm.getDescription()));
      v.addAttribute( new Attribute("units", dparm.getUnits()));
      String coordinates = "elevation azimuth distance_1 " + "latitudes_1 longitudes_1 altitudes_1";
      v.addAttribute( new Attribute(_Coordinate.Axes, coordinates));
      /*
      v.addAttribute( new Attribute("missing_value", new Float(dparm.getBadDataFlag())));
      v.addAttribute( new Attribute("_FillValue", new Float(dparm.getBadDataFlag())));
      v.addAttribute( new Attribute("scale_factor", dparm.getUnits()));
      v.addAttribute( new Attribute("polarization", dparm.getUnits()));
      v.addAttribute( new Attribute("Frequencies_GHz", dparm.getUnits()));
      v.addAttribute( new Attribute("InterPulsePeriods_secs", dparm.getUnits()));
      v.addAttribute( new Attribute("ThresholdValue", new Float(dparm.getThresholdValue())));
      v.addAttribute( new Attribute("ThresholdParamName", dparm.getthresholdParamName()));
      v.addAttribute( new Attribute("usedPRTs", new Integer(dparm.getusedPRTs())));
      v.addAttribute( new Attribute("usedFrequencies", new Integer(dparm.getusedFrequencies())));
       */
  }


  void addNCAttributes(NetcdfFile nc, DoradeSweep mySweep) throws DoradeSweep.DoradeSweepException
  {
        nc.addAttribute(null, new Attribute("summary", "Dorade radar data " +
          "from radar " + mySweep.getSensorName(0) + " in the project " + mySweep.getProjectName()));
        nc.addAttribute(null, new Attribute("radar_name", mySweep.getSensorName(0)));
        nc.addAttribute(null, new Attribute("project_name", mySweep.getProjectName()));
        nc.addAttribute(null, new Attribute("keywords_vocabulary", "dorade"));
        nc.addAttribute(null, new Attribute("geospatial_lat_min", new Float(lat_min[0])));
        nc.addAttribute(null, new Attribute("geospatial_lat_max", new Float(lat_max[0])));
        nc.addAttribute(null, new Attribute("geospatial_lon_min", new Float(lon_min[0])));
        nc.addAttribute(null, new Attribute("geospatial_lon_max", new Float(lon_max[0])));
        nc.addAttribute(null, new Attribute("geospatial_vertical_min", new Float(lon_min[0])));
        nc.addAttribute(null, new Attribute("geospatial_vertical_max", new Float(lon_max[0])));
        Date [] dd = mySweep.getTimes();
        nc.addAttribute(null, new Attribute("time_coverage_start",  dd[0].toString()));
        nc.addAttribute(null, new Attribute("time_coverage_end",  dd[dd.length-1].toString()));
        nc.addAttribute(null, new Attribute("Content", "This file contains one scan of remotely sensed data"));
        nc.addAttribute(null, new Attribute("Conventions", _Coordinate.Convention));
        nc.addAttribute(null, new Attribute("format", "Unidata/netCDF/Dorade"));
        nc.addAttribute(null, new Attribute("Radar_Name", mySweep.getSensorName(0)));
        nc.addAttribute(null, new Attribute("Project_name", ""+mySweep.getProjectName()));
        nc.addAttribute(null, new Attribute("VolumeCoveragePatternName", mySweep.getScanMode(0).getName()));
        nc.addAttribute(null, new Attribute("Volume_Number", ""+mySweep.getVolumnNumber()));
        nc.addAttribute(null, new Attribute("Sweep_Number", ""+mySweep.getSweepNumber()));
        nc.addAttribute(null, new Attribute("Sweep_Date", mySweep.getTime().toString()));
        if(mySweep.sensorIsMoving(0) == true)
          nc.addAttribute(null, new Attribute("IsStationary", "0"));
        else
          nc.addAttribute(null, new Attribute("IsStationary", "1"));
  }

  // Return the string of entity ID for the Dorade image file
  DataType getDataType(int format)
  {
      DataType p;

      switch(format)
       {
         case 1:    // 8-bit signed integer format.
              p = DataType.SHORT;
              break;
         case 2:  // 16-bit signed integer format.
              p = DataType.FLOAT;
              break;
         case 3:    // 32-bit signed integer format.
              p = DataType.LONG;
              break;
         case 4:   // 32-bit IEEE float format.
              p = DataType.FLOAT;
              break;
         case 5:   //  16-bit IEEE float format.
              p = DataType.DOUBLE;
              break;
         default:
              p = null;
              break;
       } //end of switch
      return p;
  }
  ////////////////////////////////////////////////////////////////////////////////////////////////////////

  // variable info for reading/writing
  class Vinfo {
    int vsize; // size of array in bytes. if isRecord, size per record.
    long begin; // offset of start of data from start of file
    boolean isRecord; // is it a record variable?
    int nx;
    int ny;
    Vinfo( int vsize, long begin, boolean isRecord, int x, int y) {
      this.vsize = vsize;
      this.begin = begin;
      this.isRecord = isRecord;
      this.nx = x;
      this.ny = y;
    }
  }




}
/* Change History:
   $Log: Doradeheader.java,v $
   Revision 1.5  2006/04/19 20:24:09  yuanho
   radial dataset sweep for all radar dataset

   Revision 1.4  2005/08/08 22:45:32  yuanho
   spelling bug fix

   Revision 1.3  2005/08/03 21:50:45  yuanho
   called IsDoradeSweep to check input file, adding global atts.

   Revision 1.2  2005/05/11 00:10:03  caron
   refactor StuctureData, dt.point

   Revision 1.1  2005/04/26 19:39:06  yuanho
   iosp for dorade format radar data

   Revision 1.8  2004/12/15 22:35:25  caron
   add _unsigned

   Revision 1.7  2004/12/07 22:13:28  yuanho
   add phyElem for 1hour and total precipitation

   Revision 1.6  2004/12/07 22:13:15  yuanho
   add phyElem for 1hour and total precipitation

   Revision 1.5  2004/12/07 01:29:31  caron
   redo convention parsing, use _Coordinate encoding.

   Revision 1.4  2004/10/29 00:14:11  caron
   no message

   Revision 1.3  2004/10/19 15:17:22  yuanho
   Dorade header DxKm update

   Revision 1.2  2004/10/15 23:18:34  yuanho
   Dorade projection update

   Revision 1.1  2004/10/13 22:57:57  yuanho
   no message

   Revision 1.4  2004/08/16 20:53:45  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:17  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:10  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */