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
/**
 * User: rkambic
 * Date: Aug 17, 2009
 * Time: 1:11:43 PM
 */

package ucar.nc2.iosp.grid;

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.grid.GridTableLookup;
import ucar.grid.GridRecord;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.grib.GribGridRecord;
import ucar.grib.GribNumbers;
import ucar.grib.grib2.Grib2Tables;

import java.util.*;

/**
 * Handles the Ensemble coordinate dimension
 */
public class GridEnsembleCoord {
  /**
   * logger
   */
  static private org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(GridEnsembleCoord.class);

  /**
   * name
   */
  private String name;

  /**
   * lookup table
   */
  private GridTableLookup lookup;

  /**
   * number of ensembles
   */
  private int ensembles;

  /** types for the ensembles diemensions */
  private int[] ensTypes;

  /**
   * product definition  #
   */
  private int pdn = -1;

  /**
   * sequence #
   */
  private int seq = 0;

  /**
   * Create a new GridEnsembleCoord from a record
   *
   * @param record record to use
   * @param lookup lookup table
   */
  GridEnsembleCoord(GridRecord record, GridTableLookup lookup) {
    this.lookup = lookup;
    //ensembles = calEnsembles(records);
    if (record instanceof GribGridRecord) { // check for ensemble
      GribGridRecord ggr = (GribGridRecord) record;
      if (ggr.getEnsembleNumber() == GribNumbers.UNDEFINED) {
        ensembles = -1;
        return;
      }
      ensembles = ggr.getNumberForecasts();
    }
    ensembles = -1;
  }

  /**
   * Create a new GridEnsembleCoord with the list of records
   *
   * @param records records to use
   * @param lookup  lookup table
   */
  GridEnsembleCoord(List<GridRecord> records, GridTableLookup lookup) {
    this.lookup = lookup;
    calEnsembles(records);
  }

  /**
   * add Ensemble dimension
   */
  void calEnsembles(List<GridRecord> records) {

    int[] enstypes = new int[100];

    ensembles = -1;
    for( GridRecord gr : records ) {
      GribGridRecord ggr = (GribGridRecord) gr;
      pdn = ggr.productType;
      int ensNumber = ggr.getEnsembleNumber();
      if (ensNumber == GribNumbers.UNDEFINED) {
        ensembles = -1;
        return;
      }
      if ( ensembles < ensNumber )
        ensembles = ensNumber;
      enstypes[ ensNumber ] = ggr.getEnsembleType();
    }
    ensembles++;

    ensTypes = new int[ ensembles ];
    System.arraycopy( enstypes, 0, ensTypes, 0, ensembles);


    /*
    GridRecord first = records.get(0);
    if (first instanceof GribGridRecord) { // check for ensemble
      GribGridRecord ggr = (GribGridRecord) first;
      if (ggr.getEnsembleNumber() == GribNumbers.UNDEFINED)
        return -1;

      return ggr.getNumberForecasts();
    }
    return -1;
    */


    /*
    int key = ggr.getRecordKey(); // levelType1, levelValue1, levelType2, levelValue2,
    //double key = ggr.getRecordKey() +
    //    ggr.levelType1 + ggr.levelValue1 + ggr.levelType2 +ggr.levelValue2;
    ensembles = 1;
    for( int i = 1; i < records.size(); i++) {
      ggr = (GribGridRecord) records.get( i );
      //double key1 = ggr.getRecordKey() +
      //  ggr.levelType1 + ggr.levelValue1 + ggr.levelType2 +ggr.levelValue2;
      if (key == ggr.getRecordKey() ) {
        ensembles++;
      }

    }
    */
    // get the Ensemble keys
    //System.out.println( "Ensembles ="+ ensembles );
    /*
    enskey = new int[ ensembles ];
    ArrayList<Integer> ek = new ArrayList<Integer>();
    int ikey;
    for( int i = 0; i < records.size(); i++) {
      ggr = (GribGridRecord) records.get( i );
      Integer ii = new Integer( ggr.getRecordKey() );
      if( ! ek.contains( ii ))
          ek.add( ii );
      //enskey[ ggr.forecastTime ] = ggr.getRecordKey();
      if ( ggr.forecastTime == 3)
        System.out.println(  ggr.getRecordKey()+" "+ ggr.productType +" "+ ggr. discipline
          +" "+ ggr.category +" "+ ggr.paramNumber +" "+ ggr.typeGenProcess+" "+
          ggr.levelType1 +" "+ ggr.levelValue1 +" "+ ggr.levelType2 +" "+ ggr.levelValue2
          +" "+  ggr.refTime.hashCode() +" "+  ggr.forecastTime);
    }
    System.out.println( ek );
    System.out.println( ek.size() );
    */
    /*
        int ensemble = 0;
        GridRecord first =  recordList.get( 0 );
        if ( first instanceof GribGridRecord ) { // check for ensemble
          GribGridRecord ggr = (GribGridRecord) first;
          int key = ggr.getRecordKey();
          for( int i = 1; i < recordList.size(); i++) {
            ggr = (GribGridRecord) recordList.get( i );
            if (key == ggr.getRecordKey() ) {
              ensemble++;
            }
          }
          ensembleDimension.add( new Integer( ensemble ));
        }

    }
    return ensembles;
    */
  }

  /**
   * Set the sequence number
   *
   * @param seq the sequence number
   */
  void setSequence(int seq) {
    this.seq = seq;
  }

  /**
   * Get the name
   *
   * @return the name
   */
  String getName() {
    if (name != null) {
      return name;
    }
    return (seq == 0)
        ? "ens"
        : "ens" + seq;
  }

  /**
   * Add this as a dimension to a netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
    ncfile.addDimension(g, new Dimension(getName(), getNEnsembles(), true));
  }

  /**
   * Add this as a variable to the netCDF file
   *
   * @param ncfile the netCDF file
   * @param g      the group in the file
   */
  void addToNetcdfFile(NetcdfFile ncfile, Group g) {
    Variable v = new Variable(ncfile, g, null, getName());
   // v.setDataType(DataType.INT);
    v.setDataType(DataType.STRING);
    v.addAttribute(new Attribute("long_name", "ensemble"));
    /*
    int[] data = new int[ensembles];

    for (int i = 0; i < ensembles; i++) {
      data[i] = i;
    }
    Array dataArray = Array.factory(DataType.INT,
        new int[]{ensembles}, data);
    */
    String[] data = new String[ensembles];

    for (int i = 0; i < ensembles; i++) {
      data[i] = Grib2Tables.getEnsembleType( pdn, ensTypes[ i ]) +" "+ Integer.toString( i );
    }
    Array dataArray = Array.factory(DataType.STRING, new int[]{ensembles}, data);

    v.setDimensions(v.getShortName());
    v.setCachedData(dataArray, false);

    /*   // TODO: code clean up and above
    if ( lookup instanceof Grib2GridTableLookup) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      //v.addAttribute( new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO( d )));
      //v.addAttribute( new Attribute("GRIB2_significanceOfRTName",
       //   g2lookup.getFirstSignificanceOfRTName()));
    } else if ( lookup instanceof Grib1GridTableLookup) {
      Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      //v.addAttribute( new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO( d )));
      //v.addAttribute( new Attribute("GRIB2_significanceOfRTName",
      //    g1lookup.getFirstSignificanceOfRTName()));
    }
    */

    v.addAttribute(new Attribute(_Coordinate.AxisType,
        AxisType.Ensemble.toString()));

    ncfile.addVariable(g, v);
  }

  /**
   * Get the index of a GridRecord
   *
   * @param record the record
   * @return the index or -1 if not found
   */
  int getIndex(GridRecord record) {

    if (record instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) record;
      int en = ggr.getEnsembleNumber();
      if (en == GribNumbers.UNDEFINED)
        return 0;

      return en;
    }
    return -1;
  }

  /**
   * Get the number of Ensembles
   *
   * @return the number of Ensembles
   */
  int getNEnsembles() {
    return ensembles;
  }
}
