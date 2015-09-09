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
 */
package ucar.nc2.ft2.coverage.writer;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.util.*;

/**
 * Helper class to create logical subsets.
 * Used by CFGridCoverageWriter2
 *
 * @author caron
 * @since 7/12/2015
 */
public class CoverageSubsetter2 {

  public static ucar.nc2.util.Optional<CoverageDataset> makeCoverageDatasetSubset(CoverageDataset org, List<String> gridsWanted, SubsetParams params) throws InvalidRangeException {

    // Get subset of original objects that are needed by the requested grids
    List<Coverage> orgCoverages = new ArrayList<>();
    Map<String, CoverageCoordSys> orgCoordSys = new HashMap<>();  // eliminate duplicates
    Set<String> coordTransformSet = new HashSet<>();              // eliminate duplicates

    for (String gridName : gridsWanted) {
      Coverage orgGrid =  org.findCoverage(gridName);
      if (orgGrid == null) continue;
      orgCoverages.add(orgGrid);
      CoverageCoordSys cs = orgGrid.getCoordSys();
      orgCoordSys.put(cs.getName(), cs);
      for (String tname : cs.getTransformNames())
        coordTransformSet.add(tname);
    }

    // LOOK bail out if any fail, make more robust
    // subset all coordSys, and eliminate duplicate axes.
    Map<String, CoverageCoordAxis> subsetCoordAxes = new HashMap<>();
    Map<String, CoverageCoordSys> subsetCFCoordSys = new HashMap<>();
    for (CoverageCoordSys orgCs : orgCoordSys.values()) {
      ucar.nc2.util.Optional<CoverageCoordSysSubset> coordSysSubseto = orgCs.subset(params, true); // subsetCF make do some CF tweaks, not needed in regular subset
      if (!coordSysSubseto.isPresent())
        return ucar.nc2.util.Optional.empty(coordSysSubseto.getErrorMessage());

      CoverageCoordSysSubset coordSysSubset = coordSysSubseto.get();
      subsetCFCoordSys.put(orgCs.getName(), coordSysSubset.coordSys);
      for (CoverageCoordAxis axis : coordSysSubset.coordSys.getAxes()) {
        subsetCoordAxes.put(axis.getName(), axis);  // eliminate duplicates
      }
    }

    // here are the objects we need to make the subsetted dataset
    List<CoverageCoordSys> coordSys = new ArrayList<>();
    List<CoverageCoordAxis> coordAxes = new ArrayList<>();
    List<Coverage> coverages = new ArrayList<>();
    List<CoverageTransform> coordTransforms = new ArrayList<>();

    for (CoverageCoordSys subsetCs : subsetCFCoordSys.values()) {
      coordSys.add( subsetCs);
    }

    for (CoverageCoordAxis subsetAxis : subsetCoordAxes.values())
      coordAxes.add( subsetAxis);               // must use a copy, because of setDataset()

    for (Coverage orgCov : orgCoverages) {
      // must substitute subsetCS
      CoverageCoordSys subsetCs = subsetCFCoordSys.get(orgCov.getCoordSysName());
      coverages.add( new Coverage(orgCov, subsetCs)); // must use a copy, because of setCoordSys()
    }

    for (String tname : coordTransformSet) {
      CoverageTransform t = org.findCoordTransform(tname); // these are truly immutable, so can use originals
      if (t != null)
        coordTransforms.add(t);
    }

    // LOOK TODO
    LatLonRect latLonBoundingBox = null;
    ProjectionRect projBoundingBox = null;
    CalendarDateRange dateRange = null;

    // put it all together
    return ucar.nc2.util.Optional.of(new CoverageDataset(org.getName(), org.getCoverageType(), new AttributeContainerHelper(org.getName(), org.getGlobalAttributes()),
            latLonBoundingBox, projBoundingBox, dateRange,
            coordSys, coordTransforms, coordAxes, coverages, org.getReader()));  // use org.reader -> subset always in coord space !
  }

  CoverageCoordAxis1D findIndependentAxis(String want, List<CoverageCoordAxis> axes) {
    String name = want == null ? null : want.trim();
    for (CoverageCoordAxis axis : axes)
      if (axis instanceof CoverageCoordAxis1D && axis.getName().equalsIgnoreCase(name))
        return (CoverageCoordAxis1D) axis;
    return null;
  }

}
