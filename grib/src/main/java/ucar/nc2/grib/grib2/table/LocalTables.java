/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
