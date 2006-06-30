// $Id: N3ver1.java,v 1.18 2006/06/06 16:07:12 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package ucar.nc2;

import ucar.ma2.*;
import java.io.IOException;
import java.util.*;

/**
 * IOServiceProvider implementation concrete class to read/write netcdf
 *  (file format version 1) files.
 * This class uses the java-netcdf (version 1) classes. this is provided so we can test performance
 * differences between old and new versions.
 * @deprecated - use N3raf.
 */

class N3ver1 implements ucar.nc2.IOServiceProvider {

  private ucar.nc2.NetcdfFile ncfile;
  private ucar.netcdf.NetcdfFile netcdf;

  public void setProperties( List iospProperties) { }

  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException {
    return N3header.isValidFile( raf);
  }  

  //////////////////////////////////////////////////////////////////////////////////////
  // read existing file

  public void open(ucar.unidata.io.RandomAccessFile raf, ucar.nc2.NetcdfFile ncfile,
                   ucar.nc2.util.CancelTask cancelTask) throws IOException {
    this.filename = ncfile.getLocation();
    this.ncfile = ncfile;

    netcdf = new ucar.netcdf.NetcdfFile( filename, true);
    //System.out.println("N3ver1 open "+filename);

    // create dimensions
    ucar.netcdf.DimensionIterator diter = netcdf.getDimensions().iterator();
    while (diter.hasNext()) {
      ucar.netcdf.Dimension d = (ucar.netcdf.Dimension) diter.next();
      ucar.nc2.Dimension d2 = new ucar.nc2.Dimension( d.getName(), d.getLength(), true);
      d2.setUnlimited( d instanceof ucar.netcdf.UnlimitedDimension);

      ncfile.addDimension(null, d2);
    }

    // create global attributes
    ucar.netcdf.AttributeIterator aiter = netcdf.getAttributes().iterator();
    while (aiter.hasNext()) {
      ucar.netcdf.Attribute att = (ucar.netcdf.Attribute) aiter.next();
      ucar.nc2.Attribute a2 = new ucar.nc2.Attribute( att.getName());
      a2.setValueOld( att.getValue());

      ncfile.addAttribute(null, a2);
    }

    // create variables
    ucar.netcdf.VariableIterator viter = netcdf.iterator();
    while (viter.hasNext()) {
      ucar.netcdf.Variable v = (ucar.netcdf.Variable) viter.next();
      ucar.nc2.Variable v2 = new ucar.nc2.Variable( ncfile, ncfile.rootGroup, null, v.getName());
      initVariable( v2, v);

      ncfile.addVariable( null, v2);
    }

    ncfile.finish();
  }

  private void initVariable(ucar.nc2.Variable v2, ucar.netcdf.Variable v) {

    Group rootGroup = ncfile.getRootGroup();

    // map the dimensions
    ArrayList dims = new ArrayList();
    ucar.netcdf.DimensionIterator diter = v.getDimensionIterator();
    while (diter.hasNext()) {
      ucar.netcdf.Dimension d = (ucar.netcdf.Dimension) diter.next();
      dims.add( rootGroup.findDimension( d.getName()));
    }
    v2.setDimensions(dims);

    // create variable attributes
    ucar.netcdf.AttributeIterator aiter = v.getAttributes().iterator();
    while (aiter.hasNext()) {
      ucar.netcdf.Attribute att = (ucar.netcdf.Attribute) aiter.next();
      ucar.nc2.Attribute a2 = new ucar.nc2.Attribute( att.getName());
      a2.setValueOld( att.getValue());

      v2.addAttribute(a2);
    }

    v2.setDataType( DataType.getType( v.getComponentType()));
    v2.calcIsCoordinateVariable();
    v2.setSPobject( v);
  }

  /**
   * Read data from a top level Variable and return a memory resident Array.
   * This Array has the same element type as the Variable, and the requested shape.
   * Note that this does not do rank reduction, so the returned Array has the same rank
   *  as the Variable. Use Array.reduce() for rank reduction.
   *
   * @param v2 a top-level Variable
   * @param sectionList list of Range objects specifying the section of data to read. If Range[] is null, assume all data.
   *   Each Range corresponds to a Dimension. If the Range object is null, it means use the entire dimension.
   * @return the requested data in a memory-resident Array
   */
  public Array readData(ucar.nc2.Variable v2, java.util.List sectionList) throws IOException, InvalidRangeException  {
    ucar.netcdf.Variable v = (ucar.netcdf.Variable) v2.getSPobject();
    Range[] section = Range.toArray( sectionList);

    // construct subset
    int [] varShape = v2.getShape();
    boolean wantsAll = true;
    int[] origin = new int[v.getRank()];
    int[] shape = new int[v.getRank()];
    for (int i=0; i<section.length; i++ ) {
      origin[i] = section[i].first();
      shape[i] = section[i].length();
      if ( shape[i] != varShape[i]) wantsAll = false;
      if ( section[i].stride() != 1)
        throw new UnsupportedOperationException("N3ver1 doesnt support strides");
    }

    if (wantsAll) { // read all
      Object storage = v.toArray();
      Class type = DataType.getType(v.getComponentType()).getPrimitiveClassType();
      return Array.factory(type, v.getLengths(), storage);
    }

    // subset
    ucar.multiarray.MultiArray ma = v.copyout(origin, shape);
    Object storage = ma.getStorage();
    Array aa = Array.factory( DataType.getType(ma.getComponentType()).getPrimitiveClassType(), ma.getLengths(), storage);
    return aa;
  }

  public ucar.ma2.Array readNestedData(ucar.nc2.Variable v2, java.util.List section)
         throws java.io.IOException, ucar.ma2.InvalidRangeException {
       throw new UnsupportedOperationException("version 1 does not support nested variables");
     }

  //////////////////////////////////////////////////////////////////////////////////////
  // create new file

  private ucar.netcdf.Schema schema;
  private String filename;
  private boolean fill;
  private HashMap dimHash = new HashMap(50);

  public void create(String filename, ucar.nc2.NetcdfFile ncfile, boolean fill) throws IOException {
    this.filename = filename;
    this.ncfile = ncfile;
    this.fill = fill;

    ncfile.finish();

    schema = new ucar.netcdf.Schema();

    for (Iterator i = ncfile.getDimensions().iterator(); i.hasNext(); ) {
      Dimension myd = (Dimension) i.next();
      addDimension( myd);
    }

    for (Iterator i = ncfile.getGlobalAttributes().iterator(); i.hasNext(); ) {
      Attribute att = (Attribute) i.next();
      schema.putAttribute(makeAttribute( att));
    }

    for (Iterator i = ncfile.getVariables().iterator(); i.hasNext(); ) {
      Variable v = (Variable) i.next();
      addVariable( v);
    }

    netcdf = new ucar.netcdf.NetcdfFile( filename, true, fill, schema);

    // assign netcdf variable to nc2 variable "SPobject"
    ucar.netcdf.VariableIterator viter = netcdf.iterator();
    while (viter.hasNext()) {
      ucar.netcdf.Variable v = (ucar.netcdf.Variable) viter.next();
      ucar.nc2.Variable v2 = ncfile.findVariable( v.getName());
      v2.setSPobject( v);
    }

    //System.out.println("createDone= "+ncfile);
  }

  private void addDimension( ucar.nc2.Dimension d2) {
    ucar.netcdf.Dimension dim;
    if (d2.isUnlimited())
      dim = new ucar.netcdf.UnlimitedDimension( d2.getName());
    else
      dim = new ucar.netcdf.Dimension( d2.getName(), d2.getLength());
    dimHash.put( d2.getName(), dim);
  }

  private void addVariable( ucar.nc2.Variable v2) {
    // make dimensions
    int rank = v2.getRank();
    ucar.netcdf.Dimension[] dims = new ucar.netcdf.Dimension[ rank];
    for (int i=0; i<dims.length; i++) {
      ucar.nc2.Dimension d2 = v2.getDimension(i);
      dims[i] = (ucar.netcdf.Dimension) dimHash.get( d2.getName());
    }
    // add "proto" variable
    Class classType = v2.getDataType().getPrimitiveClassType();
    ucar.netcdf.ProtoVariable proto = new ucar.netcdf.ProtoVariable( v2.getName(), classType, dims);
    schema.put(proto);

    // add variable attributes
    Iterator attsIter = v2.getAttributes().iterator();
    while (attsIter.hasNext()) {
      ucar.nc2.Attribute att = (ucar.nc2.Attribute)  attsIter.next();
      proto.putAttribute(makeAttribute( att));
    }
  }

  private ucar.netcdf.Attribute makeAttribute( ucar.nc2.Attribute att) {
    ucar.netcdf.Attribute attr = att.isString()
      ? new ucar.netcdf.Attribute( att.getName(), att.getStringValue())
      : new ucar.netcdf.Attribute( att.getName(), att.getValues().getStorage());
    return attr;
  }

  //////////////////////////////////////////////////////////////////////////////////////
  // write

  public void writeData(ucar.nc2.Variable v2, java.util.List sectionList, Array values) throws java.io.IOException {
    ucar.netcdf.Variable ncvar = (ucar.netcdf.Variable) v2.getSPobject();
    ArrayAdapter aa = new ArrayAdapter( values);
    ncvar.copyin(Range.getOrigin(sectionList), aa);
  }

  public void flush() throws IOException {
    netcdf.flush();
  }

  public void close() throws IOException {
    netcdf.close();
  }

  public boolean syncExtend() { return false; }
  public boolean sync() { return false; }
  
  private class ArrayAdapter extends ucar.multiarray.MultiArrayImpl {
    public ArrayAdapter( ucar.ma2.Array arr) {
      super(arr.getShape(), arr.getStorage());
    }
  }

  /** Debug info for this object. */
  public String toStringDebug(Object o) { return null; }
  public String getDetailInfo() { return ""; }

}

/* Change History:
   $Log: N3ver1.java,v $
   Revision 1.18  2006/06/06 16:07:12  caron
   *** empty log message ***

   Revision 1.17  2006/04/03 22:59:46  caron
   IOSP.readNestedData() remove flatten, handle flatten=false in NetcdfFile.readMemberData(); this allows IOSPs to be simpler
   add metar decoder from Robb's thredds.servlet.ldm package

   Revision 1.16  2006/01/17 23:07:12  caron
   *** empty log message ***

   Revision 1.15  2006/01/04 00:02:40  caron
   dods src under our CVS
   forecastModelRun aggregation
   substitute M3IOVGGrid for M3IO coordSysBuilder
   iosp setProperties uses list.
   use jdom 1.0

   Revision 1.14  2005/12/15 00:29:10  caron
   *** empty log message ***

   Revision 1.13  2005/12/09 04:24:41  caron
   Aggregation
   caching
   sync

   Revision 1.12  2005/10/11 19:36:54  caron
   NcML add Records bug fixes
   iosp.isValidFile( ) throws IOException
   release 2.2.11

   Revision 1.11  2005/07/25 22:20:11  caron
   add iosp.synch()

   Revision 1.10  2005/05/23 21:52:57  caron
   add getDetailInfo() to IOSP for error/debug info

   Revision 1.9  2004/10/12 02:57:06  caron
   refactor for grib1/grib2: move common functionality up to ucar.grib
   split GribServiceProvider

   Revision 1.8  2004/09/22 18:44:32  caron
   move common to ucar.unidata

   Revision 1.7  2004/09/22 13:46:35  caron
   *** empty log message ***

   Revision 1.6  2004/08/26 17:55:10  caron
   no message

   Revision 1.5  2004/08/17 19:20:04  caron
   2.2 alpha (2)

   Revision 1.4  2004/08/16 20:53:45  caron
   2.2 alpha (2)

   Revision 1.3  2004/07/12 23:40:17  caron
   2.2 alpha 1.0 checkin

   Revision 1.2  2004/07/06 19:28:10  caron
   pre-alpha checkin

   Revision 1.1.1.1  2003/12/04 21:05:27  caron
   checkin 2.2

 */