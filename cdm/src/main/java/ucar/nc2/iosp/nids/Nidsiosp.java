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
package ucar.nc2.iosp.nids;

import ucar.nc2.constants.DataFormatType;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.AbstractIOServiceProvider;

import ucar.nc2.units.DateUnit;

import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.nio.ByteBuffer;

/**
 * IOServiceProvider implementation abstract base class to read/write "version 3" netcdf files.
 * AKA "file format version 1" files.
 * <p/>
 * see   concrete class
 */

public class Nidsiosp extends AbstractIOServiceProvider {

  protected boolean readonly;
  protected Nidsheader headerParser;

  private int pcode;

  final static int Z_DEFLATED = 8;
  final static int DEF_WBITS = 15;

  // used for writing
  protected int fileUsed = 0; // how much of the file is written to ?
  protected int recStart = 0; // where the record data starts

  protected boolean debug = false, debugSize = false, debugSPIO = false;
  protected boolean showHeaderBytes = false;

  /**
   * checking the file
   *
   * @param raf
   * @return the valid of file checking
   */
  public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    Nidsheader localHeader = new Nidsheader();
    return (localHeader.isValidFile(raf));
  }

  public String getFileTypeId() {
    return DataFormatType.NIDS.getDescription();
  }

  public String getFileTypeDescription() {
    return "NEXRAD Level-III (NIDS) Products";
  }

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);

    headerParser = new Nidsheader();
    headerParser.read(this.raf, ncfile);
    //myInfo = headerParser.getVarInfo();
    pcode = headerParser.pcode;
    ncfile.finish();
  }

  /**
   * Read nested structure data
   *
   * @param v2
   * @param section
   * @return output data
   * @throws java.io.IOException
   * @throws ucar.ma2.InvalidRangeException
   */
  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, Section section)
          throws java.io.IOException, ucar.ma2.InvalidRangeException {

    Variable vp = v2.getParentStructure();
    Object data;
    Array outputData;
    List<Range> ranges = section.getRanges();

    Nidsheader.Vinfo vinfo = (Nidsheader.Vinfo) vp.getSPobject();
    byte[] vdata = headerParser.getUncompData((int) vinfo.doff, 0);
    ByteBuffer bos = ByteBuffer.wrap(vdata);

    if (vp.getShortName().startsWith("VADWindSpeed")) {
      return readNestedWindBarbData(vp.getShortName(), v2.getShortName(), bos, vinfo, ranges);

    } else if (vp.getShortName().startsWith("unlinkedVectorStruct")) {
      return readNestedDataUnlinkVector(vp.getShortName(), v2.getShortName(), bos, vinfo, ranges);

    } else if (vp.getShortName().equals("linkedVectorStruct")) {
      return readNestedLinkedVectorData(vp.getShortName(), v2.getShortName(), bos, vinfo, ranges);

    } else if (vp.getShortName().startsWith("textStruct")) {
      return readNestedTextStringData(vp.getShortName(), v2.getShortName(), bos, vinfo, ranges);

    } else if (vp.getShortName().startsWith("VectorArrow")) {
      return readNestedVectorArrowData(vp.getShortName(), v2.getShortName(), bos, vinfo, ranges);

    } else if (vp.getShortName().startsWith("circleStruct")) {
      return readNestedCircleStructData(vp.getShortName(), v2.getShortName(), bos, vinfo, ranges);

    } else {
      throw new UnsupportedOperationException("Unknown nested variable " + v2.getShortName());
    }

    // return null;
  }

  /**
   * Read the data for each variable passed in
   *
   * @param v2
   * @param section
   * @return output data
   * @throws IOException
   * @throws InvalidRangeException
   */
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    // subset
    Object data;
    Array outputData;
    byte[] vdata;
    Nidsheader.Vinfo vinfo;
    ByteBuffer bos;
    List<Range> ranges = section.getRanges();
    vinfo = (Nidsheader.Vinfo) v2.getSPobject();

    /*
if (vinfo.isZlibed  )
vdata = readCompData(vinfo.hoff, vinfo.doff);
else
vdata = readUCompData(vinfo.hoff, vinfo.doff);

ByteBuffer bos = ByteBuffer.wrap(vdata);     */

    vdata = headerParser.getUncompData((int) vinfo.doff, 0);
    bos = ByteBuffer.wrap(vdata);


    if (v2.getShortName().equals("azimuth")) {
      if(pcode == 176)
         data = readOneScanGenericData(bos, vinfo, v2.getShortName());
      else
         data = readRadialDataAzi(bos, vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("gate")) {
      data = readRadialDataGate(vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("elevation")) {
      data = readRadialDataEle(bos, vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("latitude")) {
      double lat = ncfile.findGlobalAttribute("RadarLatitude").getNumericValue().doubleValue();
      data = readRadialDataLatLonAlt(lat, vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("longitude")) {
      double lon = ncfile.findGlobalAttribute("RadarLongitude").getNumericValue().doubleValue();
      data = readRadialDataLatLonAlt(lon, vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("altitude")) {
      double alt = ncfile.findGlobalAttribute("RadarAltitude").getNumericValue().doubleValue();
      data = readRadialDataLatLonAlt(alt, vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("distance")) {
      data = readDistance(vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("rays_time")) {
      String rt = ncfile.findGlobalAttribute("time_coverage_start").getStringValue();
      java.util.Date pDate = DateUnit.getStandardOrISO(rt);
      double lt = pDate.getTime();
      double[] dd = new double[vinfo.yt];
      for (int radial = 0; radial < vinfo.yt; radial++) {
        dd[radial] = (float) lt;
      }
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), dd);

    } else if (v2.getShortName().startsWith("EchoTop") || v2.getShortName().startsWith("VertLiquid")
            || v2.getShortName().startsWith("BaseReflectivityComp") || v2.getShortName().startsWith("LayerCompReflect")) {
      data = readOneArrayData(bos, vinfo, v2.getShortName());
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().startsWith("PrecipArray")) {
      data = readOneArrayData1(bos, vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().startsWith("Precip") && !vinfo.isRadial) {
      data = readOneArrayData(bos, vinfo, v2.getShortName());
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);

    } else if (v2.getShortName().equals("unlinkedVectorStruct")) {
      outputData = readUnlinkedVectorData(v2.getShortName(), bos, vinfo);
    } else if (v2.getShortName().equals("linkedVectorStruct")) {
      outputData = readLinkedVectorData(v2.getShortName(), bos, vinfo);
    } else if (v2.getShortName().startsWith("textStruct")) {
      outputData = readTextStringData(v2.getShortName(), bos, vinfo);
    } else if (v2.getShortName().startsWith("VADWindSpeed")) {
      outputData = readWindBarbData(v2.getShortName(), bos, vinfo, null);
    } else if (v2.getShortName().startsWith("VectorArrow")) {
      outputData = readVectorArrowData(v2.getShortName(), bos, vinfo);
    } else if (v2.getShortName().startsWith("TabMessagePage")) {
      data = readTabAlphaNumData(bos, vinfo);
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
    } else if (v2.getShortName().startsWith("circleStruct")) {
      outputData = readCircleStructData(v2.getShortName(), bos, vinfo);
    } else if (v2.getShortName().startsWith("hail") || v2.getShortName().startsWith("TVS")) {
      outputData = readGraphicSymbolData(v2.getShortName(), bos, vinfo);
    } else if (v2.getShortName().startsWith("DigitalInstantaneousPrecipitationRate") ) {
        data = readOneScanGenericData(bos, vinfo, v2.getShortName());
        outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
    } else {
      data = readOneScanData(bos, vinfo, v2.getShortName());
      outputData = Array.factory(v2.getDataType().getPrimitiveClassType(), v2.getShape(), data);
    }

    return outputData.sectionNoReduce(ranges);
  }

  /**
   * Read nested graphic symbolic structure data
   *
   * @param name    Variable name,
   * @param m       Structure mumber name,
   * @param bos     data buffer,
   * @param vinfo   variable info,
   * @param section variable section
   * @return the array  of member variable data
   */
  public Array readNestedGraphicSymbolData(String name, StructureMembers.Member m, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                           java.util.List section) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int size = pos.length;
    Structure pdata = (Structure) ncfile.findVariable(name);

    ArrayStructure ma = readCircleStructData(name, bos, vinfo);

    short[] pa = new short[size];
    for (int i = 0; i < size; i++) {
      pa[i] = ma.getScalarShort(i, m);
    }

    Array ay = Array.factory(short.class, pdata.getShape(), pa);
    return ay.sectionNoReduce(section);
  }

  /**
   * Read graphic sysmbol structure data
   *
   * @param name  Variable name
   * @param bos   data buffer,
   * @param vinfo variable info,
   * @return the arraystructure of graphic symbol data
   */
  public ArrayStructure readGraphicSymbolData(String name, ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int[] dlen = vinfo.len;
    int size = pos.length;
    int vlen = 0;

    for(int i=0; i< size ; i++ ){
           vlen = vlen + dlen[i];
    }

    Structure pdata = (Structure) ncfile.findVariable( name);
    StructureMembers members = pdata.makeStructureMembers();
    members.findMember("x_start");
    members.findMember("y_start");

    ArrayStructureW asw = new ArrayStructureW(members, new int[] {vlen});

    int ii = 0;
    for (int i=0; i< size; i++) {
       bos.position( pos[i] );

       for( int j = 0; j < dlen[i]; j++ ) {
         StructureDataW sdata = new StructureDataW(asw.getStructureMembers());
         Iterator memberIter = sdata.getMembers().iterator();

         ArrayFloat.D0 sArray ;

         sArray = new ArrayFloat.D0();
         sArray.set( bos.getShort() / 4.f);
         sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

         sArray = new ArrayFloat.D0();
         sArray.set( bos.getShort() / 4.f );
         sdata.setMemberData((StructureMembers.Member) memberIter.next(), sArray);

         asw.setStructureData(sdata, ii);
         ii++;
       }
    }   //end of for loop

    return asw;
  }

  /**
   * Read nested structure data
   *
   * @param name       Variable name,
   * @param memberName mumber name,
   * @param bos        data buffer,
   * @param vinfo      variable info,
   * @param section    variable section
   * @return the array  of member variable data
   */
  public Array readNestedLinkedVectorData(String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                          java.util.List section) throws IOException, InvalidRangeException {

    Structure pdata = (Structure) ncfile.findVariable(name);
    ArrayStructure ma = readLinkedVectorData(name, bos, vinfo);
    int size = (int) pdata.getSize();
    StructureMembers members = ma.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);

    short[] pa = new short[size];
    for (int i = 0; i < size; i++) {
      pa[i] = ma.getScalarShort(i, m);
    }

    Array ay = Array.factory(short.class, pdata.getShape(), pa);
    return ay.sectionNoReduce(section);

  }

  /**
   * Read linked vector sturcture data
   *
   * @param name  Variable name,
   * @param bos   data buffer,
   * @param vinfo variable info,
   * @return the arraystructure of linked vector data
   */
  public ArrayStructure readLinkedVectorData(String name, ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int[] dlen = vinfo.len;
    bos.position(0);

    int size = pos.length;
    int vlen = 0;
    for (int i = 0; i < size; i++) {
      vlen = vlen + dlen[i];
    }

    Structure pdata = (Structure) ncfile.findVariable(name);
    StructureMembers members = pdata.makeStructureMembers();
    // Structure pdata = new Structure(ncfile, null, null,"unlinkedVector" );
    short istart;
    short jstart;
    short iend;
    short jend;
    short sValue = 0;
    int ii = 0;
    short[][] sArray = new short[5][vlen];
    for (int i = 0; i < size; i++) {
      bos.position(pos[i]);
      if (vinfo.code == 9) {
        sValue = bos.getShort();
      }
      istart = bos.getShort();
      jstart = bos.getShort();

      for (int j = 0; j < dlen[i]; j++) {
        iend = bos.getShort();
        jend = bos.getShort();

        if (vinfo.code == 9) {
          sArray[0][ii] = sValue;
        }
        sArray[1][ii] = istart;
        sArray[2][ii] = jstart;
        sArray[3][ii] = iend;
        sArray[4][ii] = jend;

        ii++;
      }
    }   //end of for loop of read data

    ArrayStructureMA asma = new ArrayStructureMA(members, new int[]{vlen});
    Array data;
    // these are the offsets into the record
    data = Array.factory(short.class, new int[]{vlen}, sArray[0]);
    StructureMembers.Member m = members.findMember("sValue");
    if (m != null) m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[1]);
    m = members.findMember("x_start");
    m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[2]);
    m = members.findMember("y_start");
    m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[3]);
    m = members.findMember("x_end");
    m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[4]);
    m = members.findMember("y_end");
    m.setDataArray(data);
    return asma;
  }

  /**
   * Read nested data
   *
   * @param name       Variable name,
   * @param memberName Structure mumber name,
   * @param bos        Data buffer,
   * @param vinfo      variable info,
   * @param section    variable section
   * @return the array  of member variable data
   */
  public Array readNestedCircleStructData(String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                          java.util.List section) throws IOException, InvalidRangeException {

    Structure pdata = (Structure) ncfile.findVariable(name);
    ArrayStructure ma = readCircleStructData(name, bos, vinfo);
    int size = (int) pdata.getSize();
    StructureMembers members = ma.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);

    short[] pa = new short[size];
    for (int i = 0; i < size; i++) {
      pa[i] = ma.getScalarShort(i, m);
    }

    Array ay = Array.factory(short.class, pdata.getShape(), pa);
    return ay.sectionNoReduce(section);

  }

  /**
   * Read data
   *
   * @param name  Variable name,
   * @param bos   Data buffer,
   * @param vinfo variable info,
   * @return the arraystructure of circle struct data
   */
  public ArrayStructure readCircleStructData(String name, ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int size = pos.length;

    Structure pdata = (Structure) ncfile.findVariable(name);

    int recsize = pos[1] - pos[0]; // each record  must be all the same size
    for (int i = 1; i < size; i++) {
      int r = pos[i] - pos[i - 1];
      if (r != recsize) System.out.println(" PROBLEM at " + i + " == " + r);
    }

    StructureMembers members = pdata.makeStructureMembers();

    members.findMember("x_center").setDataParam(0);
    members.findMember("y_center").setDataParam(2);
    members.findMember("radius").setDataParam(4);
    members.setStructureSize(recsize);
    return new ArrayStructureBBpos(members, new int[]{size}, bos, pos);

  }

  /**
   * Read data
   *
   * @param bos   Data buffer,
   * @param vinfo variable info,
   * @return the array of tab data
   */
  public Object readTabAlphaNumData(ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int plen = vinfo.xt;
    int tablen = vinfo.yt;
    String[] pdata = new String[plen];
    bos.position(0);
    int llen;
    int ipage = 0;
    int icnt = 4;
    StringBuilder sbuf = new StringBuilder();

    while (ipage < plen && (tablen > 128 + icnt)) {
      llen = bos.getShort();
      if (llen == -1) {
        pdata[ipage] = new String(sbuf);
        sbuf = new StringBuilder();
        ipage++;
        icnt = icnt + 2;
        continue;
      }

      byte[] b = new byte[llen];
      bos.get(b);
      String sl = new String(b, CDM.utf8Charset) + "\n";
      sbuf.append(sl);
      icnt = icnt + llen + 2;
    }

    return pdata;
  }

    /**
     * Read one scan radar data
     *
     * @param bos   Data buffer
     * @param vinfo variable info
     * @return the data object of scan data
     */
    // all the work is here, so can be called recursively
    public Object readOneScanGenericData(ByteBuffer bos, Nidsheader.Vinfo vinfo, String vName) throws IOException, InvalidRangeException {
        int npixel = 0;
        short [] pdata = null;

        bos.position(0);
        int numRadials = vinfo.yt;
        float [] angleData = new float[numRadials] ;
        int numBins0;

        for(int k = 0; k < numRadials; k++)
        {
            angleData[k] = bos.getFloat();
            bos.getFloat(); // t1
            bos.getFloat(); // t2
            bos.getInt(); // numBins0
            Nidsheader.readInString(bos);

            numBins0 = bos.getInt();
            if(pdata == null){
                npixel = numRadials * numBins0;
                pdata = new short[npixel];
            }
            for(int l = 0; l < numBins0; l++)
                pdata[k * numBins0 + l] = (short)bos.getInt();
        }

        int offset;
        if (vName.endsWith("_RAW")) {
            return pdata;
        } else if (vName.startsWith("azimuth")  ) {
            return angleData;
        } else if (vName.startsWith("DigitalInstantaneousPrecipitationRate")  ) {

            int[] levels = vinfo.len;
            int scale = levels[0];
            offset = levels[1];
            float[] fdata = new float[npixel];
            for (int i = 0; i < npixel; i++) {
                int ival =  pdata[i];
                if (ival != 0 )
                    fdata[i] = (ival - offset)*1.0f/scale;
                else
                    fdata[i] = Float.NaN;
            }

            return fdata;

        }
        return null;
    }
  /**
   * Read one scan radar data
   *
   * @param bos   Data buffer
   * @param vinfo variable info
   * @return the data object of scan data
   */
  // all the work is here, so can be called recursively
  public Object readOneScanData(ByteBuffer bos, Nidsheader.Vinfo vinfo, String vName) throws IOException, InvalidRangeException {
    int doff = 0;
    int npixel = vinfo.yt * vinfo.xt;
    byte[] odata = new byte[vinfo.xt];
    byte[] pdata = new byte[npixel];
    // byte[] b2 = new byte[2];
    bos.position(0);
    for (int radial = 0; radial < vinfo.yt; radial++) {
      //bos.get(b2, 0, 2);
      //int test = getInt(b2, 0, 2);
      int runLen = bos.getShort();   // getInt(vdata, doff, 2 );
      // int runLen = getInt(b2, 0, 2);
      doff += 2;
      if (vinfo.isRadial) {
        int radialAngle = bos.getShort();
        doff += 2;
        int radialAngleD = bos.getShort();
        doff += 2;
      }
      byte[] rdata = null;
      byte[] bdata = null;

      if (vinfo.xt != runLen) {
        rdata = new byte[runLen * 2];
        bos.get(rdata, 0, runLen * 2);
        doff += runLen * 2;
        bdata = readOneBeamData(rdata, runLen, vinfo.xt, vinfo.level);
      } else {
        rdata = new byte[runLen];

        bos.get(rdata, 0, runLen);
        doff += runLen;
        // sdata = readOneBeamShortData(rdata, runLen, vinfo.xt, vinfo.level);
        bdata = rdata.clone();
      }

      if (vinfo.x0 > 0) {
        for (int i = 0; i < vinfo.x0; i++) {
          odata[i] = 0;
        }
      }

      System.arraycopy(bdata, 0, odata, vinfo.x0, bdata.length);

      // copy into odata
      System.arraycopy(odata, 0, pdata, vinfo.xt * radial, vinfo.xt);

    }   //end of for loop
    int offset = 0;
    if (vName.endsWith("_RAW")) {
      return pdata;
    } else if (vName.startsWith("DifferentialReflectivity") || vName.startsWith("CorrelationCoefficient") ||
            vName.startsWith("DifferentialPhase") ) {

        int[] levels = vinfo.len;
        int scale = levels[0];
        offset = levels[1];
        float isc = vinfo.code;
        float[] fdata = new float[npixel];
        for (int i = 0; i < npixel; i++) {
            int ival =  DataType.unsignedByteToShort(pdata[i]);
            if (ival != 2 && ival != 0 && ival != 1)
                fdata[i] = (ival*isc - offset)/scale;
            else
                fdata[i] = Float.NaN;
        }

        return fdata;

    } else if ( vName.startsWith("DigitalAccumulationArray")  || vName.startsWith("Digital1HourDifferenceAccumulation")
            || vName.startsWith("DigitalStormTotalAccumulation") || vName.startsWith("Accumulation3Hour")
            || vName.startsWith("DigitalTotalDifferenceAccumulation") ) {

        int[] levels = vinfo.len;
        int scale = levels[0];
        offset = levels[1];
        float isc = vinfo.code;
        float[] fdata = new float[npixel];
        for (int i = 0; i < npixel; i++) {
            int ival =  DataType.unsignedByteToShort(pdata[i]);
            if ( ival != 0 && ival != 1)
                fdata[i] = (ival*1.0f - offset*1.0f/isc)/(scale*1.0f);
            else
                fdata[i] = Float.NaN;
        }

        return fdata;

    } else if ( vName.startsWith("HypridHydrometeorClassification")
             || vName.startsWith("HydrometeorClassification")) {

        int[] levels = vinfo.len;
        int scale = levels[0];
        offset = levels[1];
        float[] fdata = new float[npixel];
        for (int i = 0; i < npixel; i++) {
            int ival =  DataType.unsignedByteToShort(pdata[i]);
            if (ival != 0)
                fdata[i] = (float) ival ;
            else
                fdata[i] = Float.NaN;
        }

        return fdata;

    } else if (vName.startsWith("BaseReflectivity") || vName.startsWith("BaseVelocity")) {
      int[] levels = vinfo.len;
      int iscale = vinfo.code;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival = levels[DataType.unsignedByteToShort(pdata[i])];
        if (ival > -9997 && ival != -9866)
          fdata[i] = (float) ival / (float) iscale + (float) offset;
        else
          fdata[i] = Float.NaN;
      }

      return fdata;

    } else if (vName.startsWith("DigitalHybridReflectivity")) {
      int[] levels = vinfo.len;
      int iscale = vinfo.code;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival = levels[DataType.unsignedByteToShort(pdata[i])];
        if (ival != levels[0] && ival != levels[1])
          fdata[i] = (float) ival / (float) iscale + (float) offset;
        else
          fdata[i] = Float.NaN;
      }

      return fdata;

    } else if (vName.startsWith("RadialVelocity") || vName.startsWith("SpectrumWidth")) {
      int[] levels = vinfo.len;
      int iscale = vinfo.code;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival = levels[DataType.unsignedByteToShort(pdata[i])];

        if (ival > -9996 && ival != -9866)
          fdata[i] = (float) ival / (float) iscale + (float) offset;
        else
          fdata[i] = Float.NaN;
      }
      return fdata;

    } else if (vName.startsWith("StormMeanVelocity")) {
      int[] levels = vinfo.len;
      int iscale = vinfo.code;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival = levels[pdata[i]];
        if (ival > -9996)
          fdata[i] = (float) ival / (float) iscale + (float) offset;
        else
          fdata[i] = Float.NaN;
      }
      return fdata;

    } else if (vName.startsWith("Precip") || vName.startsWith("DigitalPrecip")
            || vName.startsWith("OneHourAccumulation") || vName.startsWith("StormTotalAccumulation")) {
      int[] levels = vinfo.len;
      int iscale = vinfo.code;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival;
        if (pdata[i] < 0)
          ival = -9997;
        else
          ival = levels[pdata[i]];
        if (ival > -9996)
          fdata[i] = ((float) ival / (float) iscale + (float) offset);
        else
          fdata[i] = Float.NaN; //100 * ival;
      }
      return fdata;
    }  else if (vName.startsWith("EnhancedEchoTop")) {
      int[] levels = vinfo.len;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival = DataType.unsignedByteToShort(pdata[i]);
        if (ival == 0 || ival == 1)
          fdata[i] = Float.NaN;
        else
          fdata[i] = (float)( ival & levels[0])/ (float) levels[1] - (float) levels[2];
      }
      return fdata;

    } else if (vName.startsWith("DigitalIntegLiquid")) {
      int[] levels = vinfo.len;
      float[] fdata = new float[npixel];
      float a = getHexDecodeValue((short)levels[0]);
      float b = getHexDecodeValue((short)levels[1]);
      float c = getHexDecodeValue((short)levels[3]);
      float d = getHexDecodeValue((short)levels[4]);
      for (int i = 0; i < npixel; i++) {
        int ival = DataType.unsignedByteToShort(pdata[i]);
        if(ival == 0 || ival ==1)
          fdata[i] = Float.NaN;
        else if (ival < 20)
          fdata[i] = (ival - b)/a;
        else {
          float t =  (ival - d)/c;
          fdata[i] = (float) Math.exp(t);
        }
      }
      return fdata;

    }

    /*else if(vName.endsWith( "_Brightness" )){
  float ratio = 256.0f/vinfo.level;

  float [] fdata = new float[npixel];
  for ( int i = 0; i < vinfo.yt * vinfo.xt; i++ ) {
         fdata[i] = pdata[i] * ratio;
   }
  return fdata;

 }  else if ( vName.endsWith( "_VIP" )) {
  int [] levels = vinfo.len;
  int iscale = vinfo.code;
  int [] dvip ={ 0, 30, 40, 45, 50, 55 };
  float [] fdata = new float[npixel];
  for (int i = 0; i < npixel; i++ ) {
    float dbz = levels[pdata[i]] / iscale + offset;
    for (int j = 0; j <= 5; j++ ) {
      if ( dbz > dvip[j] ) fdata[i] = j + 1;
    }
  }
  return fdata;

 }   */
    return null;
  }

  public float getHexDecodeValue(short val) {
      float deco;

      int s = (val >> 15) & 1;
      int e = (val >> 10) & (31);
      int f = (val) & (1023);

      if( e== 0) {
           deco =(float) Math.pow(-1, s) * 2 * (0.f +(float) (f/1024.f)) ;
      } else {
           deco = (float) (Math.pow(-1, s) *Math.pow(2, e-16)*(1 + (f/1024.f)));
      }

      return deco;
  }
  /**
   * read one radial beam data
   *
   * @param ddata
   * @param rLen
   * @param xt
   * @param level
   * @return one beam data array
   * @throws IOException
   * @throws InvalidRangeException
   */

  public byte[] readOneBeamData(byte[] ddata, int rLen, int xt, int level) throws IOException, InvalidRangeException {
    int run;
    byte[] bdata = new byte[xt];

    int nbin = 0;
    int total = 0;
    for (run = 0; run < rLen * 2; run++) {
      int drun = DataType.unsignedByteToShort(ddata[run]) >> 4;
      byte dcode1 = (byte) (DataType.unsignedByteToShort(ddata[run]) & 0Xf);
      for (int i = 0; i < drun; i++) {
        bdata[nbin++] = dcode1;
        total++;
      }
    }

    if (total < xt) {
      for (run = total; run < xt; run++) {
        bdata[run] = 0;
      }
    }

    return bdata;
  }

  /**
   * read one radial beam data
   *
   * @param ddata
   * @param rLen
   * @param xt
   * @param level
   * @return one beam data array
   * @throws IOException
   * @throws InvalidRangeException
   */
  public short[] readOneBeamShortData(byte[] ddata, int rLen, int xt, int level) throws IOException, InvalidRangeException {
    int run;
    short[] sdata = new short[xt];

    int total = 0;

    for (run = 0; run < rLen; run++) {
      short dcode1 = DataType.unsignedByteToShort(ddata[run]);
      sdata[run] = dcode1;
      total++;
    }

    if (total < xt) {
      for (run = total; run < xt; run++) {
        sdata[run] = 0;
      }
    }

    return sdata;
  }

  /**
   * Read nested data
   *
   * @param name       Variable name,
   * @param memberName Structure mumber name,
   * @param bos        Data buffer,
   * @param vinfo      variable info,
   * @param section    variable section
   * @return the array  of member variable data
   */
  public Array readNestedWindBarbData(String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                      java.util.List section) throws IOException, InvalidRangeException {

    Structure pdata = (Structure) ncfile.findVariable(name);
    ArrayStructure ma = readWindBarbData(name, bos, vinfo, null);
    int size = (int) pdata.getSize();
    StructureMembers members = ma.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);

    short[] pa = new short[size];
    for (int i = 0; i < size; i++) {
      pa[i] = ma.getScalarShort(i, m);
    }

    Array ay = Array.factory(short.class, pdata.getShape(), pa);
    return ay.sectionNoReduce(section);

    //return asbb;

  }

  /**
   * Read data
   *
   * @param name  Variable name,
   * @param bos   Data buffer,
   * @param vinfo variable info,
   * @return the arraystructure of wind barb data
   */
  public ArrayStructure readWindBarbData(String name, ByteBuffer bos, Nidsheader.Vinfo vinfo, List sList) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int size = pos.length;

    Structure pdata = (Structure) ncfile.findVariable(name);


    int recsize;
    if (size > 1) {
      recsize = pos[1] - pos[0]; // each record  must be all the same size
      for (int i = 1; i < size; i++) {
        int r = pos[i] - pos[i - 1];
        if (r != recsize) System.out.println(" PROBLEM at " + i + " == " + r);
      }
    } else
      recsize = 1;


    StructureMembers members = pdata.makeStructureMembers();
    members.findMember("value").setDataParam(0); // these are the offsets into the record
    members.findMember("x_start").setDataParam(2);
    members.findMember("y_start").setDataParam(4);
    members.findMember("direction").setDataParam(6);
    members.findMember("speed").setDataParam(8);
    members.setStructureSize(recsize);

    ArrayStructure ay = new ArrayStructureBBpos(members, new int[]{size}, bos, pos);

    return (sList != null) ? (ArrayStructure) ay.sectionNoReduce(sList) : ay;

    // return new ArrayStructureBBpos( members, new int[] { size}, bos, pos);
    //return asbb;

  }

  /**
   * Read nested data
   *
   * @param name       Variable name,
   * @param memberName Structure mumber name,
   * @param bos        Data buffer,
   * @param vinfo      variable info,
   * @param section    variable section
   * @return the array  of member variable data
   */
  public Array readNestedVectorArrowData(String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                         java.util.List section) throws IOException, InvalidRangeException {

    Structure pdata = (Structure) ncfile.findVariable(name);
    ArrayStructure ma = readVectorArrowData(name, bos, vinfo);
    int size = (int) pdata.getSize();
    StructureMembers members = ma.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);


    short[] pa = new short[size];
    for (int i = 0; i < size; i++) {
      pa[i] = ma.getScalarShort(i, m);
    }

    Array ay = Array.factory(short.class, pdata.getShape(), pa);
    return ay.sectionNoReduce(section);

  }

  /**
   * Read data
   *
   * @param name  Variable name,
   * @param bos   Data buffer,
   * @param vinfo variable info,
   * @return the arraystructure of vector arrow data
   */
  public ArrayStructure readVectorArrowData(String name, ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int size = pos.length;
    /* short istart = 0;
short jstart = 0;
short direction = 0;
short arrowvalue = 0;
short arrowHeadValue = 0;    */

    Structure pdata = (Structure) ncfile.findVariable(name);
    int recsize = pos[1] - pos[0]; // each record  must be all the same size
    for (int i = 1; i < size; i++) {
      int r = pos[i] - pos[i - 1];
      if (r != recsize) System.out.println(" PROBLEM at " + i + " == " + r);
    }

    StructureMembers members = pdata.makeStructureMembers();
    members.findMember("x_start").setDataParam(0);
    members.findMember("y_start").setDataParam(2);
    members.findMember("direction").setDataParam(4);
    members.findMember("arrowLength").setDataParam(6);
    members.findMember("arrowHeadLength").setDataParam(8);

    members.setStructureSize(recsize);
    return new ArrayStructureBBpos(members, new int[]{size}, bos, pos);
    /*
    Structure pdata = new Structure(ncfile, null, null,"vectorArrow" );

    Variable ii0 = new Variable(ncfile, null, pdata, "x_start");
    ii0.setDimensions((String)null);
    ii0.setDataType(DataType.SHORT);
    pdata.addMemberVariable(ii0);

    Variable ii1 = new Variable(ncfile, null, pdata, "y_start");
    ii1.setDimensions((String)null);
    ii1.setDataType(DataType.SHORT);
    pdata.addMemberVariable(ii1);

    Variable direct = new Variable(ncfile, null, pdata, "direction");
    direct.setDimensions((String)null);
    direct.setDataType(DataType.SHORT);
    pdata.addMemberVariable(direct);

    Variable speed = new Variable(ncfile, null, pdata, "arrowLength");
    speed.setDimensions((String)null);
    speed.setDataType(DataType.SHORT);
    pdata.addMemberVariable(speed);

    Variable  v = new Variable(ncfile, null, null, "arrowHeadLength");
    v.setDataType(DataType.SHORT);
    v.setDimensions((String)null);
    pdata.addMemberVariable(v);

    StructureMembers members = pdata.makeStructureMembers();
    ArrayStructureW asw = new ArrayStructureW(members, new int[] {size});

    for (int i=0; i< size; i++) {
       bos.position( pos[i]);

       istart = bos.getShort();
       jstart = bos.getShort();
       direction = bos.getShort();
       arrowvalue = bos.getShort();
       arrowHeadValue = bos.getShort();

       ArrayStructureW.StructureDataW sdata = asw.new StructureDataW();
       Iterator memberIter = sdata.getMembers().iterator();

       ArrayObject.D0 sArray = new ArrayObject.D0(Short.class);
       sArray.set(new Short(istart));
       sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

       sArray = new ArrayObject.D0(Short.class);
       sArray.set(new Short(jstart));
       sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

       sArray = new ArrayObject.D0(String.class);
       sArray.set(new Short(direction));
       sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

       sArray = new ArrayObject.D0(Short.class);
       sArray.set(new Short(arrowvalue));
       sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

       sArray = new ArrayObject.D0(String.class);
       sArray.set(new Short(arrowHeadValue));
       sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

       asw.setStructureData(sdata, i);
    }   //end of for loop
    */
    // return asw;

  }

  /**
   * Read nested data
   *
   * @param name       Variable name,
   * @param memberName Structure mumber name,
   * @param bos        Data buffer,
   * @param vinfo      variable info,
   * @param section    variable section
   * @return the array  of member variable data
   */
  public Array readNestedTextStringData(String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                        java.util.List section) throws IOException, InvalidRangeException {

    Structure pdata = (Structure) ncfile.findVariable(name);
    ArrayStructure ma = readTextStringData(name, bos, vinfo);
    int size = (int) pdata.getSize();
    StructureMembers members = ma.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);

    Array ay;
    short[] pa = new short[size];
    String[] ps = new String[size];
    if (m.getName().equalsIgnoreCase("testString")) {
      for (int i = 0; i < size; i++) {
        ps[i] = ma.getScalarString(i, m);
      }

      ay = Array.factory(String.class, pdata.getShape(), ps);

    } else {
      for (int i = 0; i < size; i++) {
        pa[i] = ma.getScalarShort(i, m);
      }

      ay = Array.factory(short.class, pdata.getShape(), pa);
    }
    return ay.sectionNoReduce(section);
  }

  /**
   * Read data
   *
   * @param name  Variable name,
   * @param bos   Data buffer,
   * @param vinfo variable info
   * @return the arraystructure of text string data
   */
  public ArrayStructure readTextStringData(String name, ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int[] sizes = vinfo.len;
    int size = pos.length;

    Structure pdata = (Structure) ncfile.findVariable(name);

    StructureMembers members = pdata.makeStructureMembers();
    if (vinfo.code == 8) {
      members.findMember("strValue").setDataParam(0);
      members.findMember("x_start").setDataParam(2);
      members.findMember("y_start").setDataParam(4);
      members.findMember("textString").setDataParam(6);
    } else {
      members.findMember("x_start").setDataParam(0);
      members.findMember("y_start").setDataParam(2);
      members.findMember("textString").setDataParam(4);
    }

    return new MyArrayStructureBBpos(members, new int[]{size}, bos, pos, sizes);
    //StructureData[] outdata = new StructureData[size];
    // Structure pdata = new Structure(ncfile, null, null,"textdata" );
    /*   short istart = 0;
    short jstart = 0;
    short sValue = 0;

    Variable ii0 = new Variable(ncfile, null, pdata, "x_start");
    ii0.setDimensions((String)null);
    ii0.setDataType(DataType.SHORT);
    pdata.addMemberVariable(ii0);
    Variable ii1 = new Variable(ncfile, null, pdata, "y_start");
    ii1.setDimensions((String)null);
    ii1.setDataType(DataType.SHORT);
    pdata.addMemberVariable(ii1);
    Variable jj0 = new Variable(ncfile, null, pdata, "textString");
    jj0.setDimensions((String)null);
    jj0.setDataType(DataType.STRING);
    pdata.addMemberVariable(jj0);

    if(vinfo.code == 8){
    Variable  v = new Variable(ncfile, null, null, "strValue");
    v.setDataType(DataType.SHORT);
    v.setDimensions((String)null);
    pdata.addMemberVariable(v);
    }

    StructureMembers members = pdata.makeStructureMembers();
    ArrayStructureW asw = new ArrayStructureW(members, new int[] {size});

    for (int i=0; i< size; i++) {
    bos.position( pos[i] - 2);    //re read the length of block
    int strLen = bos.getShort();

    if(vinfo.code == 8) {
    strLen = strLen - 6;
    sValue = bos.getShort();
    } else {
    strLen = strLen - 4;
    }
    byte[] bb = new byte[strLen];
    ArrayStructureW.StructureDataW sdata = asw.new StructureDataW();
    Iterator memberIter = sdata.getMembers().iterator();

    ArrayObject.D0 sArray = new ArrayObject.D0(Short.class);
    istart = bos.getShort();
    jstart = bos.getShort();
    bos.get(bb);
    String tstring = new String(bb);

    sArray.set(new Short(istart));
    sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

    sArray = new ArrayObject.D0(Short.class);
    sArray.set(new Short(jstart));
    sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

    sArray = new ArrayObject.D0(String.class);
    sArray.set(new String(tstring));
    sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);

    if(vinfo.code == 8) {
    sArray = new ArrayObject.D0(Short.class);
    sArray.set(new Short(sValue));
    sdata.setMemberData( (StructureMembers.Member) memberIter.next(), sArray);
    }

    asw.setStructureData(sdata, i);

    }   //end of for loop
    */
    //return asw;

  }

  private static class MyArrayStructureBBpos extends ArrayStructureBBpos {
    int[] size;

    MyArrayStructureBBpos(StructureMembers members, int[] shape, ByteBuffer bbuffer, int[] positions, int[] size) {
      super(members, shape, bbuffer, positions);
      this.size = size;
    }

    /**
     * convert structure member into a string
     *
     * @param recnum
     * @param m
     * @return
     */
    public String getScalarString(int recnum, StructureMembers.Member m) {
      if ((m.getDataType() == DataType.CHAR) || (m.getDataType() == DataType.STRING)) {
        int offset = calcOffsetSetOrder(recnum, m);
        int count = size[recnum];
        byte[] pa = new byte[count];
        int i;
        for (i = 0; i < count; i++) {
          pa[i] = bbuffer.get(offset + i);
          if (0 == pa[i]) break;
        }
        return new String(pa, 0, i, CDM.utf8Charset);
      }

      throw new IllegalArgumentException("Type is " + m.getDataType() + ", must be String or char");
    }

  }

  /**
   * Read nested data
   *
   * @param name       Variable name,
   * @param memberName Structure mumber name,
   * @param bos        Data buffer,
   * @param vinfo      variable info,
   * @param section    variable section
   * @return the array  of member variable data
   */
  public Array readNestedDataUnlinkVector(String name, String memberName, ByteBuffer bos, Nidsheader.Vinfo vinfo,
                                          java.util.List section) throws java.io.IOException, ucar.ma2.InvalidRangeException {

    Structure pdata = (Structure) ncfile.findVariable(name);
    ArrayStructure ma = readUnlinkedVectorData(name, bos, vinfo);
    int size = (int) pdata.getSize();
    StructureMembers members = ma.getStructureMembers();
    StructureMembers.Member m = members.findMember(memberName);

    short[] pa = new short[size];
    for (int i = 0; i < size; i++) {
      pa[i] = ma.getScalarShort(i, m);
    }

    Array ay = Array.factory(short.class, pdata.getShape(), pa);
    return ay.sectionNoReduce(section);
  }

  /**
   * Read data
   *
   * @param name  Variable name,
   * @param bos   Data buffer,
   * @param vinfo variable info,
   * @return the arraystructure of unlinked vector data
   */
  public ArrayStructure readUnlinkedVectorData(String name, ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int[] pos = vinfo.pos;
    int[] dlen = vinfo.len;
    bos.position(0);

    int size = pos.length;
    int vlen = 0;
    for (int i = 0; i < size; i++) {
      vlen = vlen + dlen[i];
    }

    Structure pdata = (Structure) ncfile.findVariable(name);
    StructureMembers members = pdata.makeStructureMembers();

    // Structure pdata = new Structure(ncfile, null, null,"unlinkedVector" );
    short istart;
    short jstart;
    short iend;
    short jend;
    short vlevel;

    ArrayStructureMA asma = new ArrayStructureMA(members, new int[]{vlen});
    int ii = 0;
    short[][] sArray = new short[5][vlen];
    for (int i = 0; i < size; i++) {
      bos.position(pos[i]);
      vlevel = bos.getShort();

      for (int j = 0; j < dlen[i]; j++) {

        istart = bos.getShort();
        jstart = bos.getShort();
        iend = bos.getShort();
        jend = bos.getShort();
        sArray[0][ii] = vlevel;
        sArray[1][ii] = istart;
        sArray[2][ii] = jstart;
        sArray[3][ii] = iend;
        sArray[4][ii] = jend;

        ii++;
      }
    }   //end of for loop

    Array data;
    // these are the offsets into the record
    data = Array.factory(short.class, new int[]{vlen}, sArray[0]);
    StructureMembers.Member m = members.findMember("iValue");
    m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[1]);
    m = members.findMember("x_start");
    m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[2]);
    m = members.findMember("y_start");
    m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[3]);
    m = members.findMember("x_end");
    m.setDataArray(data);
    data = Array.factory(short.class, new int[]{vlen}, sArray[4]);
    m = members.findMember("y_end");
    m.setDataArray(data);
    return asma;

  }


  // all the work is here, so can be called recursively
  public Object readOneArrayData(ByteBuffer bos, Nidsheader.Vinfo vinfo, String vName) throws IOException, InvalidRangeException {
    int doff = 0;
    int offset = 0;
    //byte[] odata = new byte[ vinfo.xt];
    byte[] pdata = new byte[vinfo.yt * vinfo.xt];
    byte[] b2 = new byte[2];
    int npixel = vinfo.yt * vinfo.xt;
    //int t = 0;
    bos.position(0);

    for (int radial = 0; radial < vinfo.yt; radial++) {

      bos.get(b2);
      int runLen = getUInt(b2, 0, 2); //bos.getShort();   //   getInt(vdata, doff, 2 );
      doff += 2;

      byte[] rdata = new byte[runLen];

      int tmpp = bos.remaining();
      bos.get(rdata, 0, runLen);
      doff += runLen;
      byte[] bdata = readOneRowData(rdata, runLen, vinfo.xt);

      // copy into odata
      System.arraycopy(bdata, 0, pdata, vinfo.xt * radial, vinfo.xt);

    }   //end of for loop

    if (vName.endsWith("_RAW")) {
      return pdata;
    } else if (vName.equals("EchoTop") || vName.equals("VertLiquid") || vName.startsWith("Precip")) {
      int[] levels = vinfo.len;
      int iscale = vinfo.code;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival = levels[pdata[i]];
        if (ival > -9996)
          fdata[i] = (float) ival / (float) iscale + (float) offset;
        else
          fdata[i] = Float.NaN;
      }

      return fdata;

    } else if (vName.startsWith("BaseReflectivityComp") || vName.startsWith("LayerCompReflect")) {
      int[] levels = vinfo.len;
      int iscale = vinfo.code;
      float[] fdata = new float[npixel];
      for (int i = 0; i < npixel; i++) {
        int ival = levels[pdata[i]];
        if (ival > -9997)
          fdata[i] = (float) ival / (float) iscale + (float) offset;
        else
          fdata[i] = Float.NaN;
      }
      return fdata;

    }

    return null;

  }

  /**
   * Read data
   *
   * @param bos   is data buffer
   * @param vinfo is variable info
   * @return the data object
   */
  public Object readOneArrayData1(ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int doff = 0;
    //byte[] odata = new byte[ vinfo.xt];

    short[] pdata = new short[vinfo.yt * vinfo.xt];
    //byte[] b2 = new byte[2];
    //int t = 0;
    bos.position(0);

    for (int row = 0; row < vinfo.yt; row++) {
      int runLen = bos.getShort();   //   getInt(vdata, doff, 2 );
      doff += 2;

      byte[] rdata = new byte[runLen];
      int tmpp = bos.remaining();
      bos.get(rdata, 0, runLen);
      doff += runLen;
      short[] bdata;
      if (vinfo.code == 17) {
        bdata = readOneRowData1(rdata, runLen, vinfo.xt);
      } else {
        bdata = readOneRowData2(rdata, runLen, vinfo.xt);
      }
      // copy into odata
      System.arraycopy(bdata, 0, pdata, vinfo.xt * row, vinfo.xt);
    }   //end of for loop

    return pdata;

  }

  /**
   * Read data from encoded values and run len into regular data array
   *
   * @param ddata is encoded data values
   * @return the data array of row data
   */
  public short[] readOneRowData1(byte[] ddata, int rLen, int xt) throws IOException, InvalidRangeException {
    int run;
    short[] bdata = new short[xt];

    int nbin = 0;
    int total = 0;
    for (run = 0; run < rLen-1; run++) {
      int drun = DataType.unsignedByteToShort(ddata[run]);
      run++;
      short dcode1 = (DataType.unsignedByteToShort(ddata[run]));
      for (int i = 0; i < drun; i++) {
        bdata[nbin++] = dcode1;
        total++;
      }
    }

    if (total < xt) {
      for (run = total; run < xt; run++) {
        bdata[run] = 0;
      }
    }

    return bdata;
  }
    /**
     * Read data from encoded values and run len into regular data array
     *
     * @param ddata is encoded data values
     * @return the data array of row data
     */
    public short[] readOneRowData2(byte[] ddata, int rLen, int xt) throws IOException, InvalidRangeException {
      int run;
      short[] bdata = new short[xt];

      int nbin = 0;
      int total = 0;
      for (run = 0; run < rLen; run++) {
        int drun = DataType.unsignedByteToShort(ddata[run]) >> 4;
        short dcode1 = (short)(DataType.unsignedByteToShort(ddata[run]) & 0Xf);
        for (int i = 0; i < drun; i++) {
          bdata[nbin++] = dcode1;
          total++;
        }
      }

      if (total < xt) {
        for (run = total; run < xt; run++) {
          bdata[run] = 0;
        }
      }

      return bdata;
    }

  /**
   * Read data from encoded values and run len into regular data array
   *
   * @param ddata is encoded data values
   * @return the data array of row data
   */
  public byte[] readOneRowData(byte[] ddata, int rLen, int xt) throws IOException, InvalidRangeException {
    int run;
    byte[] bdata = new byte[xt];

    int nbin = 0;
    int total = 0;
    for (run = 0; run < rLen; run++) {
      int drun = DataType.unsignedByteToShort(ddata[run]) >> 4;
      byte dcode1 = (byte) (DataType.unsignedByteToShort(ddata[run]) & 0Xf);
      for (int i = 0; i < drun; i++) {
        bdata[nbin++] = dcode1;
        total++;
      }
    }

    if (total < xt) {
      for (run = total; run < xt; run++) {
        bdata[run] = 0;
      }
    }

    return bdata;
  }

  /**
   * read radail elevation array
   *
   * @param bos
   * @param vinfo
   * @return data elevation array
   * @throws IOException
   * @throws InvalidRangeException
   */
  public Object readRadialDataEle(ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {

    float[] elvdata = new float[vinfo.yt];
    float elvAngle = vinfo.y0 * 0.1f;
    //Float ra = new Float(elvAngle);

    for (int radial = 0; radial < vinfo.yt; radial++) {
      elvdata[radial] = elvAngle;
    }   //end of for loop

    return elvdata;

  }

  /**
   * read radial data
   *
   * @param t
   * @param vinfo
   * @return data output
   * @throws IOException
   * @throws InvalidRangeException
   */
  public Object readRadialDataLatLonAlt(double t, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {

    float[] vdata = new float[vinfo.yt];

    for (int radial = 0; radial < vinfo.yt; radial++) {
      vdata[radial] = (float) t;
    }   //end of for loop

    return vdata;

  }

  public Object readRadialDataAzi(ByteBuffer bos, Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    int doff = 0;
    float[] azidata = new float[vinfo.yt];

    for (int radial = 0; radial < vinfo.yt; radial++) {

      int runLen = bos.getShort();   //   getInt(vdata, doff, 2 );
      doff += 2;
      float radialAngle = (float) bos.getShort() / 10.0f;
      doff += 2;
      int radialAngleD = bos.getShort();
      doff += 2;
      if (vinfo.xt != runLen)
        doff += runLen * 2;
      else
        doff += runLen;
      bos.position(doff);
      Float ra = new Float(radialAngle);
      azidata[radial] = ra.floatValue();

    }   //end of for loop

    return azidata;

  }

  public Object readDistance(Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    //int doff = 0;
    int[] data = new int[vinfo.yt * vinfo.xt];

    for (int row = 0; row < vinfo.yt; row++) {
      for (int col = 0; col < vinfo.xt; col++) {
        int i = row * vinfo.yt + col;
        data[i] = col + vinfo.x0;
        //data[i] = val;
      }
    }   //end of for loop

    return data;

  }

  public Object readRadialDataGate(Nidsheader.Vinfo vinfo) throws IOException, InvalidRangeException {
    //int doff = 0;
    float[] gatedata = new float[vinfo.xt];
    double ddg = Nidsheader.code_reslookup(pcode);
    if(pcode == 169 || pcode == 170 || pcode == 171 || pcode == 175
            || pcode == 172  || pcode == 173 || pcode == 174 )
        ddg = 1.0f;
    float sc = vinfo.y0 * 1.0f;
    for (int rad = 0; rad < vinfo.xt; rad++) {
      gatedata[rad] = (vinfo.x0) + rad * sc * (float) ddg;

    }   //end of for loop

    return gatedata;

  }

  // for the compressed data read all out into a array and then parse into requested
  // This routine reads compressed image data for Level III formatted file.
  // We referenced McIDAS GetNexrLine function
  public byte[] readCompData1(byte[] uncomp, long hoff, long doff) throws IOException {
    int off;
    off = 2 * (((uncomp[0] & 0x3F) << 8) | (uncomp[1] & 0xFF));
    /* eat WMO and PIL */
    for (int i = 0; i < 2; i++) {
      while ((off < uncomp.length) && (uncomp[off] != '\n')) off++;
      off++;
    }

    byte[] data = new byte[(int) (uncomp.length - off - doff)];

    //byte[] hedata = new byte[(int)doff];

    // System.arraycopy(uncomp, off, hedata, 0, (int)doff);
    System.arraycopy(uncomp, off + (int) doff, data, 0, uncomp.length - off - (int) doff);

    return data;
  }

  /**
   * Read compressed data
   *
   * @param hoff header offset
   * @param doff data offset
   * @return the array of data
   */
  public byte[] readCompData(long hoff, long doff) throws IOException {
    int numin;                /* # input bytes processed       */
    long pos = 0;
    long len = raf.length();
    raf.seek(pos);
    numin = (int) (len - hoff);
    // Read in the contents of the NEXRAD Level III product header

    // nids header process
    byte[] b = new byte[(int) len];
    raf.readFully(b);

    /* a new copy of buff with only compressed bytes */

    // byte[] comp = new byte[numin - 4];
    // System.arraycopy( b, (int)hoff, comp, 0, numin -4 );

    // decompress the bytes
    Inflater inf = new Inflater(false);

    int resultLength;
    int result = 0;
    //byte[] inflateData = null;
    byte[] tmp;
    int uncompLen = 24500;        /* length of decompress space    */
    byte[] uncomp = new byte[uncompLen];

    inf.setInput(b, (int) hoff, numin - 4);
    int limit = 20000;

    while (inf.getRemaining() > 0) {
      try {
        resultLength = inf.inflate(uncomp, result, 4000);
      }
      catch (DataFormatException ex) {
        System.out.println("ERROR on inflation " + ex.getMessage());
        ex.printStackTrace();
        throw new IOException(ex.getMessage());
      }

      result = result + resultLength;
      if (result > limit) {
        // when uncomp data larger then limit, the uncomp need to increase size
        tmp = new byte[result];
        System.arraycopy(uncomp, 0, tmp, 0, result);
        uncompLen = uncompLen + 10000;
        uncomp = new byte[uncompLen];
        System.arraycopy(tmp, 0, uncomp, 0, result);
      }
      if (resultLength == 0) {
        int tt = inf.getRemaining();
        byte[] b2 = new byte[2];
        System.arraycopy(b, (int) hoff + numin - 4 - tt, b2, 0, 2);
        if (headerParser.isZlibHed(b2) == 0) {
          System.arraycopy(b, (int) hoff + numin - 4 - tt, uncomp, result, tt);
          result = result + tt;
          break;
        }
        inf.reset();
        inf.setInput(b, (int) hoff + numin - 4 - tt, tt);
      }

    }
    /*
  while ( inf.getRemaining() > 0) {
   try{
     resultLength = inf.inflate(uncomp);
   }
   catch (DataFormatException ex) {
    System.out.println("ERROR on inflation");
    ex.printStackTrace();
  }
   if(resultLength > 0 ) {
       result = result + resultLength;
       inflateData = new byte[result];
       if(tmp != null) {
          System.arraycopy(tmp, 0, inflateData, 0, tmp.length);
          System.arraycopy(uncomp, 0, inflateData, tmp.length, resultLength);
       } else {
          System.arraycopy(uncomp, 0, inflateData, 0, resultLength);
       }
       tmp = new byte[result];
       System.arraycopy(inflateData, 0, tmp, 0, result);
       uncomp = new byte[(int)uncompLen];
   } else {
       int tt = inf.getRemaining();
       byte [] b2 = new byte[2];
       System.arraycopy(b,(int)hoff+numin-4-tt, b2, 0, 2);
       if( headerParser.isZlibHed( b2 ) == 0 ) {
          result = result + tt;
          inflateData = new byte[result];
          System.arraycopy(tmp, 0, inflateData, 0, tmp.length);
          System.arraycopy(b, (int)hoff+numin-4-tt, inflateData, tmp.length, tt);
          break;
      }
       inf.reset();
       inf.setInput(b, (int)hoff+numin-4-tt, tt);

   }
  }  */
    inf.end();

    int off;
    off = 2 * (((uncomp[0] & 0x3F) << 8) | (uncomp[1] & 0xFF));
    /* eat WMO and PIL */
    for (int i = 0; i < 2; i++) {
      while ((off < result) && (uncomp[off] != '\n')) off++;
      off++;
    }

    byte[] data = new byte[(int) (result - off - doff)];

    //byte[] hedata = new byte[(int)doff];

    // System.arraycopy(uncomp, off, hedata, 0, (int)doff);
    System.arraycopy(uncomp, off + (int) doff, data, 0, result - off - (int) doff);

    return data;

  }

  /**
   * Read uncompressed data
   *
   * @param hoff header offset
   * @param doff data offset
   * @return the array  of data
   */
  public byte[] readUCompData(long hoff, long doff) throws IOException {
    int numin;
    long pos = 0;
    long len = raf.length();
    raf.seek(pos);

    numin = (int) (len - hoff);
    // Read in the contents of the NEXRAD Level III product header

    // nids header process
    byte[] b = new byte[(int) len];
    raf.readFully(b);
    /* a new copy of buff with only compressed bytes */

    byte[] ucomp = new byte[numin - 4];
    System.arraycopy(b, (int) hoff, ucomp, 0, numin - 4);

    byte[] data = new byte[(int) (ucomp.length - doff)];

    System.arraycopy(ucomp, (int) doff, data, 0, ucomp.length - (int) doff);

    return data;

  }

  int getUInt(byte[] b, int offset, int num) {
    int base = 1;
    int i;
    int word = 0;

    int bv[] = new int[num];

    for (i = 0; i < num; i++) {
      bv[i] = DataType.unsignedByteToShort(b[offset + i]);
    }

    /*
    ** Calculate the integer value of the byte sequence
    */

    for (i = num - 1; i >= 0; i--) {
      word += base * bv[i];
      base *= 256;
    }

    return word;

  }

  int getInt(byte[] b, int offset, int num) {
    int base = 1;
    int i;
    int word = 0;

    int bv[] = new int[num];

    for (i = 0; i < num; i++) {
      bv[i] = DataType.unsignedByteToShort(b[offset + i]);
    }

    if (bv[0] > 127) {
      bv[0] -= 128;
      base = -1;
    }
    /*
    ** Calculate the integer value of the byte sequence
    */

    for (i = num - 1; i >= 0; i--) {
      word += base * bv[i];
      base *= 256;
    }

    return word;

  }

  @Override
  public void reacquire() throws IOException {
    super.reacquire();
    headerParser.raf = this.raf;
  }

}