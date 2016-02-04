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

package ucar.nc2.grib;

/**
 * Converts a QuasiRegular grid into a regular rectanglar (lat/lon) grid.
 *
 * @author John, from rkambic, probably from gempak
 * @author jkaehler@meteomatics.com
 * @see "http://lost-contact.mit.edu/afs/eos.ncsu.edu/service/pams/meteorology/nawips/unidata/ldmbridge/dcgrib2/qlin.c"
 * @since 9/10/11
 */
public class QuasiRegular {
  /**
   * @param quasi   input data
   * @param linePts npts in each line
   * @param nx      num parellels or undefined
   * @param ny      num parellels or undefined
   * @return regular grid
   */
  public static float[] convertQuasiGrid(float[] quasi, int[] linePts, int nx, int ny, GribData.InterpolationMethod interpolationMethod) {

    if (interpolationMethod == GribData.InterpolationMethod.none) return quasi;


    //int nrows;   /* number of rows in input */
    // int ix[]; /* row i starts at idat[ix[i]], and ix[nrows] is 1 after
    // last elem of idat */
    //float *idat; /* input quasi-regular data */
    //int ni;      /* constant length of each output row */
    //int nj;      /* number of output rows */
    //float *odat; /* where to put ni*nj outputs, already allocated */

    int max = getMax(linePts);
    if (nx < 0) {
      assert ny == linePts.length;
      nx = max;
    } else {
      assert ny < 0;
      assert nx == linePts.length;
      ny = max;
    }

    double x1_der = 1.0e30;  /* derivative of the first end point */
    double xn_der = 1.0e30;  /* derivative of the nth end point */
    double[] second_d;         /* second derivatives of input row */
    int inputIdx = 0;    /* current index position in input */
    int outputIdx = 0;    /* current index position in output */

    float[] data = new float[nx * ny];

    if (interpolationMethod == GribData.InterpolationMethod.cubic) {

      for (int j = 0; j < ny; j++) { // LOOK - assumes varies by x
        //int inrow;            /* input row to use */
        int npoints;  /* number input points in current parrallel */

        //inrow = j * (nrows - 1) / (nj - 1);  /* set the row number */
        //npoints = ix[inrow+1] - ix[inrow];   /* set number of input points */
        npoints = linePts[j];  /* number of input points in this parellel */

        // skip the processing if npoints = number of points in output parrallel
        if (npoints == nx) {
          for (int i = 0; i < nx; i++) {
            data[outputIdx++] = quasi[inputIdx++];
          }
          continue;
        }
        //second_d = (float *)emalloc(npoints * sizeof(double));
        second_d = new double[npoints];

      /* calculate the second derivatives of the input row */
        secondDerivative(
                //&idat[ix[inrow]], /* input row */
                quasi,                /* input data */
                inputIdx,             /* current index position in input */
                npoints,              /*  number of points in input row */
                x1_der,               /* first derivative of first element */
                xn_der,               /* first derivative of nth element */
                second_d);            /* output second derivative */

      /* interpolate the output row */
        for (int i = 0; i < nx; i++) {
          double mapped_i;  /* i mapped to input space */
          mapped_i = (float) i / ((float) nx) * ((float) npoints);

        /* map output point to input space */
          cubicSpline(  /* interpolate the value */
                  quasi,         /* input data */
                  inputIdx,      /* current index position in input */
                  second_d,      /* calculated second derivatives */
                  mapped_i,      /* element to be interpolated */
                  data,          /* output data */
                  outputIdx++);  /* where to put the interpolated value */
        }
        inputIdx += npoints;
      }

    } else if (interpolationMethod == GribData.InterpolationMethod.linear) { // =>
      // USE_LINEAR

      for (int j = 0; j < ny; j++) { // LOOK - assumes varies by x
        // int inrow; /* input row to use */
        // inrow = j * (nrows - 1) / (nj - 1); /* set the row number */
        // npoints = ix[inrow+1] - ix[inrow]; /* set number of input points */
        int npoints = linePts[j]; /* number of input points in this parallel */

        // skip the processing if npoints = number of points in output
        // parallel
        if (npoints == nx) {
          for (int i = 0; i < nx; i++)
            data[outputIdx++] = quasi[inputIdx++];
          continue;
        }

        for (int i = 0; i < nx; i++) {
          double mapped_i = (float) i / ((float) nx) * ((float) npoints); // i mapped to input space

          // interpolate this point
          linear( quasi, /* input data */
                  inputIdx, /* current index position in input */
                  mapped_i, /* element to be interpolated */
                  data,     /* output data */
                  outputIdx++, /* where to put the interpolated value */
                  npoints);
        }

        inputIdx += npoints;
      }

    } else {
      throw new RuntimeException("unsupported interpolation method");
    }

    return data;
  }

  public static int getMax(int[] vals) {
    int max = 0;
    for (int v : vals) if (v > max) max = v;
    return max;
  }

  public static void secondDerivative(float[] inpt, int idx, int n, double x1d, double xnd, double[] y2d) {
    //    float *inpt;        /* input data */
    //    float idx;          /* input data index*/
    //    int n;              /* number of points in input row */
    //    float x1d;          /* first derivative of the first end point */
    //    float xnd;          /* first derivative of the nth end point */
    //    float *y2d;         /* output row of 2nd derivatives */

    double p;
    double qn;
    double sig;
    double un;
    //float *scratch = (float *)emalloc((n - 1) * sizeof(float));
    double[] scratch = new double[n - 1];  /* scratch vector */

    if (x1d > 0.99e30) {  /* lower boundary is natural */
      y2d[0] = scratch[0] = 0;

    } else {              /* calculate the lower boundary value */
      y2d[0] = 0.5;
      //scratch[0] = 3.0 * ((inpt[1] - inpt[0]) / (1 - x1d));
      scratch[0] = 3.0 * ((inpt[idx + 1] - inpt[idx]) / (1 - x1d));
    }

    for (int i = 1; i < n - 1; i++) {  /* decomposition loop */
      sig = 0.5;
      p = sig * y2d[i - 1] + 2.0;
      y2d[i] = (sig - 1.0) / p;
      //scratch[i] = (inpt[i+1] - inpt[i]) - (inpt[i] - inpt[i-1]);
      scratch[i] = (inpt[idx + i + 1] - inpt[idx + i]) - (inpt[idx + i] - inpt[idx + i - 1]);
      scratch[i] = (6.0 * scratch[i] / 2.0 - sig * scratch[i - 1]) / p;
    }


    if (xnd > 0.99e30) {  /* upper boundary is natural */
      qn = un = 0;

    } else {              /* calculate the upper boundary value */
      qn = 0.5;
      //un = 3.0 * (xnd - (inpt[n-1] - inpt[n-2]));
      un = 3.0 * (xnd - (inpt[idx + n - 1] - inpt[idx + n - 2]));
    }

    y2d[n - 1] = (un - qn * scratch[n - 2]) / (qn * y2d[n - 2] + 1.0);

    for (int i = n - 2; i >= 0; i--) {  /* back substitution loop */
      y2d[i] = y2d[i] * y2d[i + 1] + scratch[i];
    }

  }


  public static void cubicSpline(float[] inpt, int iIdx, double[] y2d, double x,
                                 float[] outpt, int oIdx) {
    //    float *inpt;        /* input row */
    //    int iIdx;           /* input data index*/
    //    float *y2d;         /* second derivatives of input row */
    //    float x;            /* output point */
    //    float outpt;        /* where to put the interpolated data */
    //    int oIdx;           /* index in output, the interpolated data */

    int hi;
    int low;
    double a;
    double b;

    if (java.lang.Math.floor(x) == x) {  /* existing data point */
      //*outpt = inpt[(int)x];
      outpt[oIdx] = inpt[iIdx + (int) x];
      return;
    }

    /* set the input bracket */
    hi = (int) (java.lang.Math.ceil(x));
    low = (int) (java.lang.Math.floor(x));

    a = hi - x;
    b = x - low;

    hi = hi > (y2d.length - 1) ? 0 : hi;

		/* evaluate the polynomial */

    // *outpt = a * inpt[low] + b * inpt[hi] + ((a * a * a - a) * y2d[low] +
    // (b * b * b - b) * y2d[hi]) / 6.0;
    outpt[oIdx] = (float) (a * inpt[iIdx + low] + b * inpt[iIdx + hi] + ((a
            * a * a - a)
            * y2d[low] + (b * b * b - b) * y2d[hi]) / 6.0);

  }

  public static void linear(float[] inpt, int iIdx, double x, float[] outpt,
                            int oIdx, int npoints) {

    // float *inpt; /* input row */
    // int iIdx; /* input data index*/
    // float x; /* output point */
    // float outpt; /* where to put the interpolated data */
    // int oIdx; /* index in output, the interpolated data */

    if (java.lang.Math.floor(x) == x) { /* existing data point */
      outpt[oIdx] = inpt[iIdx + (int) x];
      return;
    }

		/* set the input bracket */
    int hi = (int) (java.lang.Math.ceil(x));
    int low = (int) (java.lang.Math.floor(x));

    double a = hi - x;
    double b = x - low;

    int hiIdx = hi > (npoints - 1) ? iIdx : iIdx + hi;
    int lowIdx = iIdx + low;

    if (lowIdx >= inpt.length || hiIdx >= inpt.length)
      System.out.printf("HEY%n");

    outpt[oIdx] = (float) (a * inpt[lowIdx] + b * inpt[hiIdx]);

  }

}
