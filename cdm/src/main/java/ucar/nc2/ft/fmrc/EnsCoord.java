/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

import ucar.nc2.dataset.CoordinateAxis1D;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a ensemble coordinate shared among variables
 *
 * @author caron
 * @since Jan 12, 2010
 */
public class EnsCoord implements Comparable {
  private String name; //, units;
  private int id; // unique id for XML

  // ??
  public int ensembles;
  public int pdn;
  public int[] ensTypes;

  EnsCoord() {
  }

  EnsCoord(CoordinateAxis1D axis, int[] einfo) {
    this.name = axis.getFullName();
    this.ensembles = einfo[0];
    this.pdn = einfo[1];
    this.ensTypes = new int[this.ensembles];
    System.arraycopy(einfo, 2, ensTypes, 0, ensembles);
  }

  // copy constructor
  EnsCoord(ucar.nc2.ft.fmrc.EnsCoord ec) {
    this.name = ec.getName();
    this.id = ec.getId();
    this.ensembles = ec.getNEnsembles();
    this.pdn = ec.getPDN();
    this.ensTypes = ec.getEnsTypes().clone();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getNEnsembles() {
    return ensembles;
  }

  public void setNEnsembles(int ensembles) {
    this.ensembles = ensembles;
  }

  public int getPDN() {
    return pdn;
  }

  public void setPDN(int pdn) {
    this.pdn = pdn;
  }

  public int[] getEnsTypes() {
    return ensTypes;
  }

  public void setEnsTypes(int[] ensTypes) {
    this.ensTypes = ensTypes;
  }

  public int getSize() {
    return ensembles;
  }

  // LOOK
  public boolean equalsData(ucar.nc2.ft.fmrc.EnsCoord other) {

    if (ensembles != other.ensembles)
      return false;

    if (pdn != other.pdn)
      return false;

    for (int i = 0; i < ensTypes.length; i++) {
      if (ensTypes[i] != other.ensTypes[i])
        return false;
    }

    return true;
  }

  public int compareTo(Object o) {
    ucar.nc2.ft.fmrc.EnsCoord other = (ucar.nc2.ft.fmrc.EnsCoord) o;
    return name.compareTo(other.name);
  }

  ////////////////
  static public EnsCoord findEnsCoord(List<EnsCoord> ensCoords, EnsCoord want) {
    if (want == null) return null;

    for (EnsCoord ec : ensCoords) {
      if (want.equalsData(ec))
        return ec;
    }

    // make a new one
    EnsCoord result = new EnsCoord(want);
    ensCoords.add(result);
    return result;
  }

   /**
   * Extend result with all the values in the list of EnsCoord
   *
   * @param result extend this coord
   * @param ecList list of EnsCoord, may be empty
   */
  static public void normalize(EnsCoord result, List<EnsCoord> ecList) {
    List<EnsCoord> extra = new ArrayList<>();
    for (EnsCoord ec : ecList) {
      if (!result.equalsData(ec)) {
        // differences can only be greater
        extra.add(ec);
      }
    }
    if (extra.size() == 0)
      return;
    for (EnsCoord ec : extra) {
      if (ec.getNEnsembles() < result.getNEnsembles())
        continue;
      result = ec;
    }

  }
}
