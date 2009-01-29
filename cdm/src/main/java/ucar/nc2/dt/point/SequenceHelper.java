// $Id: SequenceHelper.java 51 2006-07-12 17:13:13Z caron $
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

package ucar.nc2.dt.point;

import ucar.nc2.*;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dt.*;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.ma2.*;

import java.io.*;
import java.util.*;

/**
 * Helper class for dods sequence datasets.
 *
 * @author caron
 * @version $Revision: 51 $ $Date: 2006-07-12 17:13:13Z $
 */

public class SequenceHelper {
  protected NetcdfFile ncfile;
  protected DODSNetcdfFile dodsFile;

  protected StructureDS sequenceOuter, sequenceInner;
  protected Variable latVar, lonVar, altVar, timeVar;
  protected boolean isProfile;

  protected DateUnit timeUnit;
  protected DateFormatter formatter;

  /**
   * Constructor.
   * @param ncfile the netccdf file
   * @param typedDataVariables list of data variables; all record variables will be added to this list, except . You
   *    can remove extra
   * @throws IllegalArgumentException if ncfile has no unlimited dimension and recDimName is null.
   */
  public SequenceHelper(NetcdfDataset ncfile, boolean isProfile, StructureDS sequenceOuter, StructureDS sequenceInner, Variable latVar, Variable lonVar, Variable altVar,
          Variable timeVar, List typedDataVariables, StringBuffer errBuffer) {

    this.ncfile = ncfile;
    this.isProfile = isProfile;
    this.sequenceOuter = sequenceOuter;
    this.sequenceInner = sequenceInner;
    this.latVar = latVar;
    this.lonVar = lonVar;
    this.altVar = altVar;
    this.timeVar = timeVar;

    // Need the underlying DODSNetcdfFile
    NetcdfFile refFile = ncfile.getReferencedFile();
    while (dodsFile == null) {
      if (refFile instanceof DODSNetcdfFile)
        dodsFile = (DODSNetcdfFile) refFile;
      else if (refFile instanceof NetcdfDataset)
        refFile = ((NetcdfDataset)refFile).getReferencedFile();
      else
        throw new IllegalArgumentException("Must be a DODSNetcdfFile");
    }


    // create member variables
    List recordMembers = sequenceOuter.getVariables();
    for (int i = 0; i < recordMembers.size(); i++) {
      Variable v = (Variable) recordMembers.get(i);
      typedDataVariables.add( v);
    }

    recordMembers = sequenceInner.getVariables();
    for (int i = 0; i < recordMembers.size(); i++) {
      Variable v = (Variable) recordMembers.get(i);
      typedDataVariables.add( v);
    }

    typedDataVariables.remove(latVar);
    typedDataVariables.remove(lonVar);
    typedDataVariables.remove(altVar);
    typedDataVariables.remove(timeVar);
    typedDataVariables.remove(sequenceInner);
  }

  public void setTimeUnit( DateUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  public DateUnit getTimeUnit() {
    return( this.timeUnit );
  }

  /* private Variable getDODSVariable( Variable v) {
    while (true) {
      if ((v instanceof DODSVariable) || (v instanceof DODSStructure))
        return v;
      else {
        if (v == v.getIOVar())
         throw new IllegalStateException("Circular reference");
        v = v.getIOVar();
      }
    }
  } */

  public List getData(CancelTask cancel) throws IOException {
    String CE = sequenceOuter.getName();
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(sequenceOuter, CE);
    extractMembers(as);
    int n = (int) as.getSize();
    ArrayList dataList = new ArrayList(n);
    for (int i=0; i<n; i++)
      dataList.add( new SeqPointObs( i, as.getStructureData(i)));
    return dataList;
  }

  public List getData(LatLonRect boundingBox, CancelTask cancel) throws IOException {
    String CE = sequenceOuter.getName() + "&" + makeBB( boundingBox);
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(sequenceOuter, CE);
    extractMembers(as);
    int n = (int) as.getSize();
    ArrayList dataList = new ArrayList(n);
    for (int i=0; i<n; i++)
      dataList.add( new SeqPointObs( i, as.getStructureData(i)));
    return dataList;

  }

  public List getData(LatLonRect boundingBox, Date start, Date end, CancelTask cancel) throws IOException {
    String CE = sequenceOuter.getName() + "&" + makeBB( boundingBox) + "&"+ makeTimeRange( start, end);
    ArrayStructure as = (ArrayStructure) dodsFile.readWithCE(sequenceOuter, CE);
    extractMembers(as);

    int n = (int) as.getSize();
    ArrayList dataList = new ArrayList(n);
    for (int i=0; i<n; i++)
      dataList.add( new SeqPointObs( i, as.getStructureData(i)));
    return dataList;
  }

  private String makeBB( LatLonRect bb) {
    return latVar.getName()+">="+bb.getLowerLeftPoint().getLatitude()+"&"+
           latVar.getName()+"<="+bb.getUpperRightPoint().getLatitude()+"&"+
           lonVar.getName()+">="+bb.getLowerLeftPoint().getLongitude()+"&"+
           lonVar.getName()+"<="+bb.getUpperRightPoint().getLongitude();
  }

  private String makeTimeRange( Date start, Date end) {
    double startValue = timeUnit.makeValue(start);
    double endValue = timeUnit.makeValue(end);
    return timeVar.getName()+">="+startValue+"&"+   // LOOK
           timeVar.getName()+"<="+endValue;
  }

  private StructureMembers.Member latMember, lonMember, innerMember, altMember, timeMember;
  private void extractMembers( ArrayStructure as) {
    StructureMembers members = as.getStructureMembers();
    latMember = members.findMember(latVar.getShortName());
    lonMember = members.findMember(lonVar.getShortName());
    innerMember = members.findMember(sequenceInner.getShortName());

    StructureData first = as.getStructureData(0);
    StructureData innerFirst = first.getScalarStructure(innerMember);
    StructureMembers innerMembers = innerFirst.getStructureMembers();

    if (isProfile) {
      timeMember = members.findMember(timeVar.getShortName());
      altMember = innerMembers.findMember(altVar.getShortName());
    } else {
      timeMember = innerMembers.findMember(timeVar.getShortName());
      altMember = members.findMember(altVar.getShortName());
    }
  }


  /* private class SeqDatatypeIterator extends DatatypeIterator {
    protected Object makeDatatypeWithData(int recnum, StructureData sdata) {
      return new SeqPointObs( recnum, sdata);
    }

    SeqDatatypeIterator(Structure struct, int bufferSize) {
      super( struct, bufferSize);
    }
  } */

  ////////////////////////////////////////////////////////////
  public class SeqPointObs extends PointObsDatatypeImpl {
    protected int recno;
    protected LatLonPointImpl llpt = null;
    protected StructureData sdata;


    /**
     * Constructor for the case where you keep track of the location, time of each record, but not the data.
     */
    protected SeqPointObs( ucar.unidata.geoloc.EarthLocation location, double obsTime, double nomTime, int recno) {
      super( location, obsTime, nomTime);
      this.recno = recno;
    }

    /**
     * Constructor for when you already have the StructureData and want to wrap it in a StationObsDatatype
     * @param recno record number LOOK why do we need ??
     * @param sdata the structure data
     */
    public SeqPointObs(int recno, StructureData sdata) {
      this.recno = recno;
      this.sdata = sdata;

      double lat = sdata.convertScalarDouble( latMember);
      double lon = sdata.convertScalarDouble( lonMember);

      // double lat = sdata.convertScalarDouble(latMember);
      // double lon = sdata.convertScalarDouble(lonMember);

      StructureData inner = sdata.getScalarStructure(innerMember);
      double alt = 0.0;

      if (isProfile) {
        obsTime = sdata.convertScalarDouble( timeMember); // sdata.convertScalarDouble(timeMember);
        alt = inner.convertScalarDouble( altMember); // inner.convertScalarDouble(altMember);
      } else {
        obsTime = inner.convertScalarDouble( timeMember); // inner.convertScalarDouble(timeMember);
        alt = sdata.convertScalarDouble( altMember); // sdata.convertScalarDouble(altMember);
      }

      nomTime = obsTime;
      location = new ucar.unidata.geoloc.EarthLocationImpl( lat, lon, alt);
    }

    public LatLonPoint getLatLon() {
      if (llpt == null)
         llpt = new LatLonPointImpl( location.getLatitude(), location.getLongitude());
      return llpt;
    }

    public Date getNominalTimeAsDate() {
      return timeUnit.makeDate( getNominalTime());
    }

    public Date getObservationTimeAsDate() {
      return timeUnit.makeDate( getObservationTime());
    }

    public StructureData getData() throws IOException {
      return sdata;
    }
  }


  /////////////////////////

  public DataIterator getDataIterator(int bufferSize) throws IOException {
    return new DataIteratorAdapter( getData(null).iterator()); // LOOK
  }

}
