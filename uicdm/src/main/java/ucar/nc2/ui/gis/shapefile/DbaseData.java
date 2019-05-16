/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
/*
   Class to contain a single field of data from a dbase file
  @author Russ Rew
 */
package ucar.nc2.ui.gis.shapefile;

import ucar.nc2.constants.CDM;

import java.io.DataInputStream;

public class DbaseData {
  DbaseFieldDesc desc;
  int nrec;  /* number of records */

  /**
   * Character type data (String[]).
   */
  public static final int TYPE_CHAR = 0;
  /**
   * Data is an array of doubles (double[]).
   */
  public static final int TYPE_NUMERIC = 1;
  /**
   * Data is an array of booleans (boolean[]).
   */
  public static final int TYPE_BOOLEAN = 2;

  /* the various possible types */
  String[] character;
  double[] numeric;
  boolean[] logical;
  byte[] field;
  int type;


  DbaseData(DbaseFieldDesc desc, int nrec) {
    this.desc = desc;
    this.nrec = nrec;
    field = new byte[desc.FieldLength];
    switch (desc.Type) {
      case 'C':
      case 'D':
        character = new String[nrec];
        type = TYPE_CHAR;
        break;
      case 'N':
      case 'F':
        numeric = new double[nrec];
        type = TYPE_NUMERIC;
        break;
      case 'L':
        logical = new boolean[nrec];
        type = TYPE_BOOLEAN;
        break;
    }

  }

  /**
   * Method to return the type of data for the field
   *
   * @return One of TYPE_CHAR, TYPE_BOOLEAN, or TYPE_NUMERIC
   */
  public int getType() {
    return type;
  }

  /**
   * Method to read an entry from the data stream.  The stream is assumed to be
   * in the right spot for reading.  This method should be called from something
   * controlling the reading of the entire file.
   */
  int readRowN(DataInputStream ds, int n) {
    if (n > nrec) return -1;
    /* the assumption here is that the DataInputStream (ds)
    * is already pointing at the right spot!
    */
    try {
      ds.readFully(field, 0, desc.FieldLength);
    } catch (java.io.IOException e) {
      return -1;
    }
    switch (desc.Type) {
      case 'C':
      case 'D':
        character[n] = new String(field, CDM.utf8Charset);
        break;
      case 'N':
        numeric[n] = Double.valueOf(new String(field, CDM.utf8Charset));
        break;
      case 'F':  /* binary floating point */
        if (desc.FieldLength == 4) {
          numeric[n] = (double) Swap.swapFloat(field, 0);
        } else {
          numeric[n] = Swap.swapDouble(field, 0);
        }
        break;
      case 'L':
        switch (field[0]) {
          case 't':
          case 'T':
          case 'Y':
          case 'y':
            logical[n] = true;
            break;
          default:
            logical[n] = false;
            break;
        }
      default:
        return -1;
    }

    return 0;
  }

  /**
   * Method to retrieve the double array for this field
   *
   * @return An array of doubles with the data
   */
  public double[] getDoubles() {
    return numeric;
  }

  /**
   * Method to retrieve a double for this field
   *
   * @param i index of desired double, assumes 0 < i < getNumRec()
   * @return A double with the data
   */
  public double getDouble(int i) {
    return numeric[i];
  }

  /**
   * Method to retrieve a booleans array for this field
   *
   * @return An array of boolean values
   */
  public boolean[] getBooleans() {
    return logical;
  }

  /**
   * Method to retrieve a boolean for this field
   *
   * @param i index of desired boolean, assumes 0 < i < getNumRec()
   * @return A boolean with the data
   */
  public boolean getBoolean(int i) {
    return logical[i];
  }

  /**
   * Method to retrieve an array of Strings for this field
   *
   * @return An array of Strings
   */
  public String[] getStrings() {
    return character;
  }

  /**
   * Method to retrieve a String for this field
   *
   * @param i index of desired String, assumes 0 < i < getNumRec()
   * @return A String with the data
   */
  public String getString(int i) {
    return character[i];
  }

  /**
   * Method to retrieve data for this field
   *
   * @param i index of desired String, assumes 0 < i < getNumRec()
   * @return either a Double, Boolean, or String with the data
   */
  public Object getData(int i) {
    switch (type) {
      case TYPE_CHAR:
        return character[i];
      case TYPE_NUMERIC:
        return numeric[i];
      case TYPE_BOOLEAN:
        return logical[i];
    }
    return null;
  }

  /**
   * @return The number of records in the field.
   */
  public int getNumRec() {
    return nrec;
  }
}

