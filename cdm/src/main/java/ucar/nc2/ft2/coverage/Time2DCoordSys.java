/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.ft2.coverage;

import net.jcip.annotations.Immutable;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.util.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Time2DCoordSys with run and 2D time coordinate axes. LOOK not complete
 * A CoordSys has 0 or 1 Time2DCoordSys, but there may be several in a Dataset.
 *
 * @author John
 * @since 8/14/2015
 */
@Immutable
public class Time2DCoordSys {
  public final CoverageCoordAxis1D runAxis;
  public final TimeAxis2DFmrc timeAxis2D;

  public Time2DCoordSys(CoverageCoordAxis1D runAxis, TimeAxis2DFmrc timeAxis) {
    this.runAxis = runAxis;
    this.timeAxis2D = timeAxis;
  }

  public List<CoverageCoordAxis> getCoordAxes() throws InvalidRangeException {
    List<CoverageCoordAxis> result = new ArrayList<>();
    result.add(runAxis);
    result.add(timeAxis2D);
    return result;
  }

  public int[] getShape() {
    return timeAxis2D.getShape();
  }

  /*
    (from CdmrfParams.adoc) :

    2D Time subsetting
    A 2D time dataset will have CoverageType set to FMRC.
    You may specify a runtime with a date, latest or all; specify a timeOffset with a numeric value, first, or all.
      If only one is set, use the default for the other. If neither is set, then return all times for latest runtime.
    Time parameters are only used if explicitly set and timeOffset is not set. There are only 2 cases where time can be used:
    Set runtime to a specific value or latest (not all). Time parameters (point or range) can be used.
    Set runtime to all. Time point (date, or present) can be used.
    Special cases:
      set specific runtime = constant runtime dataset
      set specific timeOffset, set runTime to all = constant offset dataset
      set specific time, set runTime to all = constant forecast dataset
   */
  /*
  1) single runtime
     1a timeOffset
     1b time or timeRange
     1c none = constant runtime dataset
  2) multiple runtimes
     2a timeOffset       = constant offset dataset
     2b time (not range) = constant forecast dataset
   */
  public Optional<List<CoverageCoordAxis>> subset(SubsetParams params, AtomicBoolean isConstantForcast, boolean makeCFcompliant) {
    return Optional.empty("not implemented by "+getClass().getName());

    /* List<CoverageCoordAxis> result = new ArrayList<>();

    Optional<CoverageCoordAxis> axiso = runAxis.subset(params);
    if (!axiso.isPresent())
      return Optional.empty(axiso.getErrorMessage());
    CoverageCoordAxis1D runAxisSubset = (CoverageCoordAxis1D) axiso.get();
    result.add(runAxisSubset);

    // subset on timeOffset (1a, 1c, 2a)
    if (params.hasTimeOffsetParam() || !params.hasTimeParam()) {
      axiso = timeAxis2D.subset(params);
      if (!axiso.isPresent())
        return Optional.empty(axiso.getErrorMessage());
      CoverageCoordAxis timeOffsetSubset = axiso.get();
      result.add(timeOffsetSubset);

      if (makeCFcompliant)
        result.add(makeCFTimeCoord(runAxisSubset, (CoverageCoordAxis1D) timeOffsetSubset)); // possible the twoD time case, if nruns > 1
      return Optional.of(result);
    }

    // subset on time, # runtimes = 1 (1b)
    if (runAxisSubset.getNcoords() == 1) {
      double val = runAxisSubset.getCoord(0);   // not sure runAxis is needed. maybe use runtimeSubset
      CalendarDate runDate = runAxisSubset.makeDate(val);
      Optional<TimeOffsetAxis> too = timeAxis2D.subsetFromTime(params, runDate);
      if (!too.isPresent())
        return Optional.empty(too.getErrorMessage());
      TimeOffsetAxis timeOffsetSubset =  too.get();
      result.add(timeOffsetSubset);

      if (makeCFcompliant)
        result.add(makeCFTimeCoord(runAxisSubset, timeOffsetSubset));
      return Optional.of(result);
    }

    // tricky case 2b time (point only not range) = constant forecast dataset
    // data reader has to skip around the 2D times
    // 1) the runtimes may be subset by whats available
    // 2) timeOffset could become an aux coordinate
    // 3) time coordinate becomes a scalar,
    isConstantForcast.set(true);

    CalendarDate dateWanted;
    if (params.isTrue(SubsetParams.timePresent))
      dateWanted = CalendarDate.present();
    else
      dateWanted = (CalendarDate) params.get(SubsetParams.time);
    if (dateWanted == null)
      throw new IllegalStateException("Must have time parameter");

    double wantOffset = runAxisSubset.convert(dateWanted); // forecastDate offset from refdate
    double start = timeAxis.getStartValue();
    double end = timeAxis.getEndValue();
    CoordAxisHelper helper = new CoordAxisHelper(timeOffset);

    // brute force search LOOK specialize for regular ?
    List<Integer> runtimeIdx = new ArrayList<>();  // list of runtime indexes that have this forecast
    // List<Integer> offsetIdx = new ArrayList<>();  // list of offset indexes that have this forecast
    List<Double> offset = new ArrayList<>();      // corresponding offset from start of run
    for (int i=0; i<runAxisSubset.getNcoords(); i++) {
      // public double getOffsetInTimeUnits(CalendarDate convertFrom, CalendarDate convertTo);
      double runOffset = runAxisSubset.getCoord(i);
      if (end + runOffset < wantOffset) continue;
      if (wantOffset < start + runOffset) break;
      int idx = helper.search(wantOffset - runOffset);
      if (idx >= 0) {
        runtimeIdx.add(i);  // the ith runtime
        // offsetIdx.add(idx);   // the idx time offset
        offset.add(wantOffset - runOffset);   // the offset from the runtime
      }
    }

    // here are the runtimes
    int ncoords = runtimeIdx.size();
    double[] runValues = new double[ncoords];
    double[] offsetValues = new double[ncoords];
    int count = 0;
    for (int k=0; k<ncoords; k++) {
      offsetValues[count] = offset.get(k);
      runValues[count++] = runAxisSubset.getCoord( runtimeIdx.get(k));
    }

    CoverageCoordAxisBuilder runbuilder = new CoverageCoordAxisBuilder(runAxisSubset)
            .subset(null, CoverageCoordAxis.Spacing.irregularPoint, ncoords, runValues); // LOOK check for regular (in CovCoordAxis ?)
    CoverageCoordAxis1D runAxisSubset2 = new CoverageCoordAxis1D(runbuilder);

    CoverageCoordAxisBuilder timebuilder = new CoverageCoordAxisBuilder(timeOffset)
            .subset(runAxisSubset2.getName(), CoverageCoordAxis.Spacing.irregularPoint, ncoords, offsetValues); // aux coord (LOOK interval) ??
    CoverageCoordAxis1D timeOffsetSubset = new TimeOffsetAxis(timebuilder);

    CoverageCoordAxis scalarTimeCoord = makeScalarTimeCoord(wantOffset, runAxisSubset);

    // nothing needed for CF, the run coordinate acts as the CF time independent coord. timeOffset is aux, forecastTime is scalar
    return Optional.of(Lists.newArrayList(runAxisSubset2, timeOffsetSubset, scalarTimeCoord)); // */
  }

  private CoverageCoordAxis makeScalarTimeCoord(double val, CoverageCoordAxis1D runAxisSubset) {
    String name = "constantForecastTime";
    String desc = "forecast time";
    AttributeContainerHelper atts = new AttributeContainerHelper(name);
    atts.addAttribute(new Attribute(CDM.UNITS, runAxisSubset.getUnits()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, desc));
    atts.addAttribute(new Attribute(CF.CALENDAR, runAxisSubset.getCalendar().toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(name, runAxisSubset.getUnits(), desc, DataType.DOUBLE, AxisType.Time, atts,
            CoverageCoordAxis.DependenceType.scalar, null, CoverageCoordAxis.Spacing.regularPoint, 1, val, val, 0.0, null, null);
    builder.setIsSubset(true);

    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCFTimeCoord(CoverageCoordAxis1D runAxisSubset, CoverageCoordAxis1D timeAxisSubset) {
    String name = timeAxisSubset.getName()+"Forecast";
    String desc = "forecast time";
    AttributeContainerHelper atts = new AttributeContainerHelper(name);
    atts.addAttribute(new Attribute(CDM.UNITS, runAxisSubset.getUnits()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, desc));
    atts.addAttribute(new Attribute(CF.CALENDAR, runAxisSubset.getCalendar().toString()));

    if (runAxisSubset.getNcoords() == 1) {
      CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder();
      builder.name = name;
      builder.units = runAxisSubset.getUnits();
      builder.description = desc;
      builder.dataType = DataType.DOUBLE;
      builder.axisType = AxisType.Time;
      builder.attributes = atts;
      builder.dependenceType = CoverageCoordAxis.DependenceType.dependent;
      builder.setDependsOn(timeAxisSubset.getName());

      builder.spacing = timeAxisSubset.getSpacing();
      builder.ncoords = timeAxisSubset.ncoords;
      builder.isSubset = true;

      // conversion from timeOffset to runtime units
      double offset = timeAxisSubset.getOffsetInTimeUnits(runAxis.getRefDate(), timeAxisSubset.getRefDate());

      switch (timeAxisSubset.getSpacing()) {
        case regularInterval:
        case regularPoint:
          builder.startValue = timeAxisSubset.getStartValue() + offset;
          builder.endValue = timeAxisSubset.getEndValue() + offset;
          break;

        case contiguousInterval:
        case irregularPoint:
        case discontiguousInterval:
          builder.values = timeAxisSubset.getValues(); // this is a copy
          for (int i=0; i<builder.values.length; i++)
            builder.values[i] += offset;
          break;
      }

      return new CoverageCoordAxis1D(builder);
    }

    return null;
  }


}
