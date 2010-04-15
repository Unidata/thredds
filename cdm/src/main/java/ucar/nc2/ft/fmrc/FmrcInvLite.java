/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ft.fmrc;

import ucar.nc2.util.Misc;

import java.util.*;

/**
 * A lightweight, serializable version of FmrcInv
 *
 * @author caron
 * @since Apr 14, 2010
 */
public class FmrcInvLite implements java.io.Serializable {
  String name;
  Date base;
  int nruns;
  double[] runOffset; // run time in offset hours since base
  List<String> locationList = new ArrayList<String>();
  List<Gridset> gridSets = new ArrayList<Gridset>();
  List<GridInventory> invList = new ArrayList<GridInventory>(); // share these, they are expensive!

  public FmrcInvLite(FmrcInv fmrcInv) {
    this.name = fmrcInv.getName();
    this.base = fmrcInv.getBaseDate();

    List<FmrInv> fmrList = fmrcInv.getFmrInv();
    nruns = fmrList.size();
    runOffset = new double[nruns];

    for (int run = 0; run < nruns; run++) {
      FmrInv fmr = fmrList.get(run);
      runOffset[run] = FmrcInv.getOffsetInHours(base, fmr.getRunDate());

      for (GridDatasetInv inv : fmr.getInventoryList()) {
        locationList.add(inv.getLocation());
      }
    }

    for (FmrcInv.RunSeq runseq : fmrcInv.getRunSeqs()) {
      gridSets.add(new Gridset(runseq));
    }

  }

  // group of Grids with the same time coordinate
  class Gridset implements java.io.Serializable {
    String name;
    List<Grid> grids = new ArrayList<Grid>();
    int noffsets;
    double[] timeOffset;  // timeOffset(nruns,noffsets) in offset hours since base. this is the twoD time coordinate for this Gridset

    Map<String, List<Blob>> timeCoordMap = new HashMap<String, List<Blob>>();

    Gridset(FmrcInv.RunSeq runseq) {
      this.name = runseq.getName();
      List<TimeCoord> timeList = runseq.getTimes();
      assert nruns == timeList.size();
      double[] unionOffsetHours = runseq.getUnionOffsetHours();
      noffsets = unionOffsetHours.length;

      // this is the twoD time coordinate for this Gridset
      timeOffset = new double[nruns * noffsets];
      for (int i=0; i<timeOffset.length; i++) timeOffset[i] = Double.NaN;

      // fill twoD time coordinate from the sequence of time coordinates
      for (int run=0; run<nruns; run++) {
        TimeCoord tc = timeList.get(run);
        double run_offset = FmrcInv.getOffsetInHours(base, tc.getRunDate());
        double[] offsets = tc.getOffsetHours();
        int ntimes = offsets.length;
        for (int time=0; time<ntimes; time++)
          timeOffset[run*noffsets + time] = run_offset + offsets[time];
      }

      for (FmrcInv.UberGrid ugrid : runseq.getUberGrids()) {
        grids.add( new Grid(ugrid.getName(), getInventory(ugrid))); // LOOK could we defer making the Inventory ??
      }
    }

    // create GridInventory, see if it matches other Grids
    private GridInventory getInventory(FmrcInv.UberGrid ugrid) {
      GridInventory result = null;
      GridInventory need = new GridInventory(ugrid, timeOffset, noffsets);

      // see if we already have it
      for (GridInventory got : invList) {
        if (got.equalData(need)) {
          result = got;
          break;
        }
      }
      if (result == null) {
        invList.add(need);
        result = need;
      }
      return result;
    }

    ////////////////////////////////////////////
    // create time coordinate variants

    double[] getBestTimeOffsets() {
      List<Blob> best = timeCoordMap.get("best");
      if (best == null) best = makeBest();

      double[] result = new double[ best.size()];
      for (int i=0; i< best.size(); i++) {
        Blob b = best.get(i);
        result[i] =  b.offset;
      }
      return result;
    }

    double[] getBestRunOffsets() {
      List<Blob> best = timeCoordMap.get("best");
      if (best == null)
        best = makeBest();
      double[] result = new double[ best.size()];
      for (int i=0; i<best.size(); i++) {
        Blob b = best.get(i);
        result[i] =  timeOffset[b.runIdx * noffsets];  // the first one for the run given by runIdx
      }
      return result;
    }

    private List<Blob> makeBest() {
      Map<Double, Blob> map = new HashMap<Double, Blob>();
      for (int run=0; run<nruns; run++) {
        for (int time=0; time<noffsets; time++) {
          double baseOffset = timeOffset[run*noffsets + time];
          map.put(baseOffset, new Blob(run, time, baseOffset)); // later ones override
        }
      }

      Collection<Blob> values = map.values();
      int n = values.size();
      List<Blob> best = Arrays.asList((Blob[]) values.toArray(new Blob[n]));
      Collections.sort(best);
      timeCoordMap.put("best", best);
      return best;
    }

    double[] getConstantOffsets(int col) {
      List<Blob> coords = timeCoordMap.get("offset"+col);
      if (coords == null)
        coords = makeConstantOffset(col);

      double[] result = new double[ coords.size()];
      for (int i=0; i< coords.size(); i++) {
        Blob b = coords.get(i);
        result[i] = b.offset;
      }
      return result;
    }

    private List<Blob> makeConstantOffset(int col) {
      List<Blob> result = new ArrayList<Blob>(nruns);
      for (int run=0; run< nruns; run++) {
        double offset = timeOffset[run*noffsets + col];
         if (!Double.isNaN(offset))
           result.add( new Blob(run, col, offset));
      }
      timeCoordMap.put("offset"+col, result);
      return result;
    }

    double[] getConstantForecast(double offset) {
      List<Blob> coords = timeCoordMap.get("forecast"+offset);
      if (coords == null)
        coords = makeConstantForecast(offset);

      double[] result = new double[ coords.size()];
      for (int i=0; i< coords.size(); i++) {
        Blob b = coords.get(i);
        result[i] = b.offset;
      }
      return result;
    }

    private List<Blob> makeConstantForecast(double offset) {
      List<Blob> result = new ArrayList<Blob>(noffsets);
      for (int run=0; run< nruns; run++) {
        for (int time=0; time< noffsets; time++) {
          if (Misc.closeEnough(timeOffset[run*noffsets + time], offset))
            result.add( new Blob(run, time, offset - timeOffset[run*noffsets])); // use offset from start of run
        }
      }
      timeCoordMap.put("forecast"+offset, result);
      return result;
    }

    double[] getRun(int runIdx) {
      List<Blob> coords = timeCoordMap.get("run"+runIdx);
      if (coords == null)
        coords = makeRun(runIdx);

      double[] result = new double[ coords.size()];
      for (int i=0; i< coords.size(); i++) {
        Blob b = coords.get(i);
        result[i] = b.offset;
      }
      return result;
    }

    private List<Blob> makeRun(int runIdx) {
      List<Blob> result = new ArrayList<Blob>(noffsets);
      for (int time=0; time< noffsets; time++) {
        double offset = timeOffset[runIdx*noffsets + time];
         if (!Double.isNaN(offset))
           result.add( new Blob(runIdx, time, offset));
      }
      timeCoordMap.put("run"+runIdx, result);
      return result;
    }

     class Grid implements java.io.Serializable {
      String name;
      GridInventory inv;

      Grid(String name, GridInventory inv) {
        this.name = name;
        this.inv = inv;
      }

      FmrcDataset.TimeInstance findInventory(int runIdx, int timeIdx) {
        int locIdx = inv.location[runIdx * inv.noffsets + timeIdx];
        if (locIdx == 0) return null;

        int invIndex = inv.invIndex[runIdx * inv.noffsets + timeIdx];
        return new FmrcDataset.TimeInstance(locationList.get(locIdx-1), invIndex);
      }

      FmrcDataset.TimeInstance findInventoryBest(int bestIdx) {
        List<Blob> best = timeCoordMap.get("best");
        if (best == null)
          best = makeBest();

        Blob b = best.get(bestIdx);
        int locIdx = inv.location[b.runIdx * inv.noffsets + b.timeIdx];
        if (locIdx == 0) return null;

        int invIndex = inv.invIndex[b.runIdx * inv.noffsets + b.timeIdx];
        return new FmrcDataset.TimeInstance(locationList.get(locIdx-1), invIndex);
      }

    }
  }

  // represents 1 element in the 2d time matrix
  private class Blob implements Comparable<Blob> {
    int runIdx, timeIdx;
    double offset;

    Blob(int runIdx, int timeIdx, double offset) {
      this.runIdx = runIdx;
      this.timeIdx = timeIdx;
      this.offset = offset;
    }

    @Override
    public int compareTo(Blob o) {
      return (int) (offset - o.offset);
    }
  }

  class GridInventory implements java.io.Serializable {
    int noffsets;
    int[] location;  // (run,time) file location (index+1 into locationList, 0 = missing)
    int[] invIndex;  // (run,time) time index in file

    GridInventory(FmrcInv.UberGrid ugrid, double[] timeOffset, int noffsets) {
      this.noffsets = noffsets;
      this.location = new int[nruns * noffsets];
      this.invIndex = new int[nruns * noffsets];

      // loop over runDates
      int gridIdx = 0;
      List<FmrInv.GridVariable> grids = ugrid.getRuns(); // must be sorted by rundate

      for (int runIdx=0; runIdx<nruns; runIdx++ ) {
        Date runDate =  FmrcInv.makeOffsetDate(base, runOffset[runIdx]);

        // do we have a grid for this runDate?
        FmrInv.GridVariable grid = grids.get(gridIdx);
        if (!grid.getRunDate().equals(runDate)) continue;
        gridIdx++; // for next loop

        // loop over actual inventory
        for (GridDatasetInv.Grid inv : grid.getInventory()) {
          double invOffset = FmrcInv.getOffsetInHours(base, inv.tc.getRunDate());
          double[] offsets = inv.tc.getOffsetHours();
          for (int i = 0; i < offsets.length; i++) {
            int timeIdx = findIndex(timeOffset, runIdx, invOffset + offsets[i]);
            if (timeIdx >= 0) {
              location[runIdx*noffsets + timeIdx] = findLocation(inv.getLocation()) + 1;
              invIndex[runIdx*noffsets + timeIdx] = i;
            }
          }
        }
      }
    }

    boolean equalData(Object oo) {
      GridInventory o = (GridInventory) oo;
      if (o.location.length != location.length) return false;
      if (o.invIndex.length != invIndex.length) return false;
      for (int i=0; i<location.length; i++)
        if (location[i] != o.location[i]) return false;
      for (int i=0; i<invIndex.length; i++)
        if (invIndex[i] != o.invIndex[i]) return false;
      return true;
    }

    // LOOK linear search!
    private int findIndex(double[] coords, int runIdx, double want) {
      for (int j=0; j<noffsets; j++)
        if (Misc.closeEnough(coords[runIdx*noffsets + j], want)) return j;
      return -1;
    }

    // LOOK linear search!
    private int findLocation(String location) {
      return locationList.indexOf(location);
    }

  }


}
