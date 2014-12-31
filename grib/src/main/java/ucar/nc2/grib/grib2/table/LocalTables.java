/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib.grib2.table;

import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.grib2.Grib2Parameter;

import java.util.*;

/**
 * superclass for local table implementations
 *
 * @author John
 * @since 6/22/11
 */
public abstract class LocalTables extends Grib2Customizer {

  //////////////////////////////////////////////////////////////////////
  protected Map<Integer, Grib2Parameter> local = new HashMap<>(100);  // subclass must set

  LocalTables(Grib2Table grib2Table) {
    super(grib2Table);
  }

  @Override
  public String getTablePath(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getTablePath(discipline, category, number);
    return grib2Table.getPath();
  }

  @Override
  public List<GribTables.Parameter> getParameters() {
    List<GribTables.Parameter> result = new ArrayList<>();
    for (Grib2Parameter p : local.values()) result.add(p);
    Collections.sort(result, new ParameterSort());
    return result;
  }

  protected static class ParameterSort implements Comparator<Parameter> {
    public int compare(Parameter p1, Parameter p2) {
      int c = p1.getDiscipline() - p2.getDiscipline();
      if (c != 0) return c;
      c = p1.getCategory() - p2.getCategory();
      if (c != 0) return c;
      return p1.getNumber() - p2.getNumber() ;
    }
  }

  @Override
  public String getVariableName(int discipline, int category, int parameter) {
    if ((category <= 191) && (parameter <= 191))
      return super.getVariableName(discipline, category, parameter);

    GribTables.Parameter te = getParameter(discipline, category, parameter);
    if (te == null)
      return super.getVariableName(discipline, category, parameter);
    else
      return te.getName();
  }

  @Override
  public GribTables.Parameter getParameter(int discipline, int category, int number) {
    Grib2Parameter plocal = local.get(makeParamId(discipline, category, number));

    if ((category <= 191) && (number <= 191))  {
      GribTables.Parameter pwmo = WmoCodeTable.getParameterEntry(discipline, category, number);
      if (plocal == null) return pwmo;
      if (pwmo == null) return plocal;

      // allow local table to override all but name, units
      return new Grib2Parameter(plocal, pwmo.getName(), pwmo.getUnit());
    }

    return plocal;
  }

  @Override
  public GribTables.Parameter getParameterRaw(int discipline, int category, int number) {
    return local.get(makeParamId(discipline, category, number));
   }

 }
