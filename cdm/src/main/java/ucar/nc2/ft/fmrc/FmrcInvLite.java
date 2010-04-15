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

    List<Blob> best;

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

    double[] makeBestTimeOffsets() {
      if (best == null) makeBest();
      double[] result = new double[ best.size()];
      for (int i=0; i< best.size(); i++) {
        Blob b = best.get(i);
        result[i] =  b.offset;
      }
      return result;
    }

    double[] makeBestRunOffsets() {
      if (best == null) makeBest();
      double[] result = new double[ best.size()];
      for (int i=0; i<best.size(); i++) {
        Blob b = best.get(i);
        result[i] =  timeOffset[b.runIdx * noffsets];  // the first one for the run given by runIdx
      }
      return result;
    }

    private void makeBest() {
      Map<Double, Blob> map = new HashMap<Double, Blob>();
      for (int run=0; run<nruns; run++) {
        for (int time=0; time<noffsets; time++) {
          double baseOffset = timeOffset[run*noffsets + time];
          map.put(baseOffset, new Blob(run, time, baseOffset)); // later ones override
        }
      }

      Collection<Blob> values = map.values();
      int n = values.size();
      best = Arrays.asList((Blob[]) values.toArray(new Blob[n]));
      Collections.sort(best);
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
        if (best == null) makeBest();

        Blob b = best.get(bestIdx);
        int locIdx = inv.location[b.runIdx * inv.noffsets + b.timeIdx];
        if (locIdx == 0) return null;

        int invIndex = inv.invIndex[b.runIdx * inv.noffsets + b.timeIdx];
        return new FmrcDataset.TimeInstance(locationList.get(locIdx-1), invIndex);
      }

    }
  }


  class Blob implements Comparable<Blob> {
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
