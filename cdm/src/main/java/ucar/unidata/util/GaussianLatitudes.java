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

package ucar.unidata.util;

/**
 * Calculate Gaussian Latitudes by finding the roots of the ordinary Legendre polynomial of degree
 * NLAT using Newton's iteration method.
 *
 * @author caron port to Java
 * @author Amy Solomon, 28 Jan 1991, via http://dss.ucar.edu/libraries/gridinterps/gaus-lats.f
 */
public class GaussianLatitudes {
  private static final double XLIM  = 1.0E-7;

  // all these have size nlat
  public double[] cosc; // cos(colatitude) or sin(latitude)
  public double[] colat; // the colatitudes in radians
  public double[] gaussw; // the Gaussian weights
  public double[] latd; // the latitudes in degrees
  public double[] sinc; // sin(colatitude) or cos(latitude)
  public double[] wos2; // Gaussian weight over sin**2(colatitude)

  /**
   * Constructor
   * @param nlat the total number of latitudes from pole to pole (degree of the polynomial)
   */
  public GaussianLatitudes(int nlat) {
    if (nlat == 0) throw new IllegalArgumentException("nlats may not be zero");

    // the number of latitudes between pole and equator
    int  nzero = nlat/2;
    cosc = new double[nlat];
    colat = new double[nlat];
    gaussw = new double[nlat];
    latd = new double[nlat];
    sinc = new double[nlat];
    wos2 = new double[nlat];

    /* set first guess for cos(colat)
      PI = 3.141592653589793
      DO 10 I=1,NZERO
         COSC(I) = SIN( (I-0.5)*PI/NLAT + PI*0.5 )
   10 CONTINUE */
    for (int i=0; i<nzero; i++)
      cosc[i] = Math.sin( (i+0.5) * Math.PI/nlat + Math.PI/2 );

    /* constants for determining the derivative of the polynomial
      FI  = NLAT
      FI1 = FI+1.0
      A   = FI*FI1 / SQRT(4.0*FI1*FI1-1.0)
      B   = FI1*FI / SQRT(4.0*FI*FI-1.0)
     */
    double FI = (double) nlat;
    double FI1 = nlat + 1.0;
    double A = FI*FI1 / Math.sqrt(4.0*FI1*FI1-1.0);
    double B = FI*FI1 / Math.sqrt(4.0*FI*FI-1.0);

    // loop over latitudes, iterating the search for each root
    for (int i=0; i<nzero; i++) {
      int countIterations = 0;
      while(true) {
        // -determine the value of the ordinary Legendre polynomial for the current guess root
   /* 30    CALL LGORD( G, COSC(I), NLAT ) */

      double G = lgord( cosc[i], nlat);

     /* -determine the derivative of the polynomial at this point
         CALL LGORD( GM, COSC(I), NLAT-1 )
         CALL LGORD( GP, COSC(I), NLAT+1 )
         GT = (COSC(I)*COSC(I)-1.0) / (A*GP-B*GM) */

      double GM = lgord( cosc[i], nlat-1);
      double GP = lgord( cosc[i], nlat+1);
      double GT = (cosc[i]*cosc[i]-1.0) / (A*GP-B*GM);

      /* -update the estimate of the root
         DELTA   = G*GT
         COSC(I) = COSC(I) - DELTA */
      double delta = G*GT;
      cosc[i] -= delta;

      /*  -if convergence criterion has not been met, keep trying
         J = J+1
         IF( ABS(DELTA).GT.XLIM ) GO TO 30 */
      countIterations++;
      if (Math.abs(delta) <= XLIM) break;
    }

     /* PRINT*,' LAT NO.',I,J,' ITERATIONS' */
      //System.out.println("Lat "+i+" has "+countIterations+" iterations");

      /* determine the Gaussian weights
         C      = 2.0 *( 1.0-COSC(I)*COSC(I) )
         CALL LGORD( D, COSC(I), NLAT-1 )
         D      = D*D*FI*FI
         GWT(I) = C *( FI-0.5 ) / D */
      double C = 2.0 *( 1.0-cosc[i]*cosc[i] );
      double D = lgord( cosc[i], nlat-1 );
      D = D * D * FI * FI;
      gaussw[i] = C *( FI-0.5 ) / D;
    }

    /* C    -determine the colatitudes and sin(colat) and weights over sin**2
      DO 50 I=1,NZERO
         COLAT(I)= ACOS(COSC(I))
         SINC(I) = SIN(COLAT(I))
         WOS2(I) = GWT(I) /( SINC(I)*SINC(I) )
   50 CONTINUE */

    for (int i=0; i<nzero; i++) {
      colat[i] = Math.acos( cosc[i]);
      sinc[i] = Math.sin( colat[i]);
      wos2[i] = gaussw[i] / (sinc[i] * sinc[i]);
    }

    /* if NLAT is odd, set values at the equator
      IF( MOD(NLAT,2) .NE. 0 ) THEN
         I       = NZERO+1
         COSC(I) = 0.0
         C       = 2.0
         CALL LGORD( D, COSC(I), NLAT-1 )
         D       = D*D*FI*FI
         GWT(I)  = C *( FI-0.5 ) / D
         COLAT(I)= PI*0.5
         SINC(I) = 1.0
         WOS2(I) = GWT(I)
      END IF */

    int next = nzero;
    if (nlat % 2 != 0) {
      cosc[next] = 0.0;
      double C = 2.0;
      double D = lgord(cosc[next], nlat-1);
      D       = D*D*FI*FI;
      gaussw[next] = C *( FI-0.5 ) / D;
      colat[next] = Math.PI*0.5;
      sinc[next] = 1.0;
      wos2[next] = gaussw[next];
      next++;
    }

     /*determine the southern hemisphere values by symmetry
      DO 60 I=NLAT-NZERO+1,NLAT
         COSC(I) =-COSC(NLAT+1-I)
         GWT(I)  = GWT(NLAT+1-I)
         COLAT(I)= PI-COLAT(NLAT+1-I)
         SINC(I) = SINC(NLAT+1-I)
         WOS2(I) = WOS2(NLAT+1-I)
   60 CONTINUE  */
    for (int i=next; i<nlat; i++) {
         cosc[i] =-cosc[nlat-i-1];
         gaussw[i] = gaussw[nlat-i-1];
         colat[i] = Math.PI-colat[nlat-i-1];
         sinc[i] = sinc[nlat-i-1];
         wos2[i] = wos2[nlat-i-1];
    }

    // the lats in degrees
    for (int i=0; i<nlat; i++)
      latd[i] = Math.toDegrees(Math.PI/2 - colat[i]);
  }

  private double lgord( double cosc, int nlat) {
    /*     COLAT = ACOS(COSC)
          C1 = SQRT(2.0)
    DO 20 K=1,N
       C1 = C1 * SQRT( 1.0 - 1.0/(4*K*K) )
 20 CONTINUE */

    double colat = Math.acos( cosc);
    double c = Math.sqrt( 2.0);
    for (int k=1; k<=nlat; k++)
      c *= Math.sqrt( 1.0 - 1.0/(4*k*k));

  /* FN = N
    ANG= FN * COLAT
    S1 = 0.0
    C4 = 1.0
    A  =-1.0
    B  = 0.0
    DO 30 K=0,N,2
       IF (K.EQ.N) C4 = 0.5 * C4
       S1 = S1 + C4 * COS(ANG)
       A  = A + 2.0
       B  = B + 1.0
       FK = K
       ANG= COLAT * (FN-FK-2.0)
       C4 = ( A * (FN-B+1.0) / ( B * (FN+FN-A) ) ) * C4
 30 CONTINUE
    F = S1 * C1
    */

    double FN = (double) nlat;
    double ANG = FN * colat;
    double S1 = 0.0;
    double C4 = 1.0;
    double A  =-1.0;
    double B  = 0.0;
    for (int k=0; k<=nlat; k+=2) {
       if (k == nlat) C4 = 0.5 * C4;
       S1 = S1 + C4 * Math.cos(ANG);
       A  = A + 2.0;
       B  = B + 1.0;
       double FK = (double) k;
       ANG = colat * (FN-FK-2.0);
       C4 = ( A * (FN-B+1.0) / ( B * (FN+FN-A) ) ) * C4;
    }
    return S1 * c;
  }

  // should match http://dss.ucar.edu/datasets/common/ecmwf/ERA40/docs/std-transformations/dss_code_glwp.html
  public static void main(String args[]) {
    int nlats = 94;
    GaussianLatitudes glat = new GaussianLatitudes(nlats);
    for (int i=0; i<nlats; i++) {
      System.out.print(" lat "+i+" = "+ Format.dfrac( glat.latd[i], 4));
      if (i < nlats - 1)
        System.out.print(" diff = " + (glat.latd[i + 1] - glat.latd[i]));
      System.out.println(" weight= "+ Format.dfrac( glat.gaussw[i], 6));
    }
  }


}

/*

C ********** WARNING *************
C This routine may not converge using 32-bit arithmetic
C
C routines from Amy Solomon, 28 Jan 1991.
C
C  LGGAUS finds the Gaussian latitudes by finding the roots of the
C  ordinary Legendre polynomial of degree NLAT using Newton's
C  iteration method.
C
C  On entry:
C     NLAT - the number of latitudes (degree of the polynomial)
C
C  On exit: for each Gaussian latitude
C     COSC   - cos(colatitude) or sin(latitude)
C     GWT    - the Gaussian weights
C     SINC   - sin(colatitude) or cos(latitude)
C     COLAT  - the colatitudes in radians
C     WOS2   - Gaussian weight over sin**2(colatitude)
C
      DIMENSION COSC(180), GWT(180), SINC(180), COLAT(180)
     +        , WOS2(180)
C
C-----------------------------------------------------------------------
C
C    -convergence criterion for iteration of cos latitude
      XLIM  = 1.0E-7
C
C    -the number of zeros between pole and equator
      NZERO = NLAT/2
C
C    -set first guess for cos(colat)
      PI = 3.141592653589793
      DO 10 I=1,NZERO
         COSC(I) = SIN( (I-0.5)*PI/NLAT + PI*0.5 )
   10 CONTINUE
C
C    -constants for determining the derivative of the polynomial
      FI  = NLAT
      FI1 = FI+1.0
      A   = FI*FI1 / SQRT(4.0*FI1*FI1-1.0)
      B   = FI1*FI / SQRT(4.0*FI*FI-1.0)
C
C    -loop over latitudes, iterating the search for each root
      DO 40 I=1,NZERO
         J=0
C
C       -determine the value of the ordinary Legendre polynomial for
C       -the current guess root
   30    CALL LGORD( G, COSC(I), NLAT )
C
C       -determine the derivative of the polynomial at this point
         CALL LGORD( GM, COSC(I), NLAT-1 )
         CALL LGORD( GP, COSC(I), NLAT+1 )
         GT = (COSC(I)*COSC(I)-1.0) / (A*GP-B*GM)
C
C       -update the estimate of the root
         DELTA   = G*GT
         COSC(I) = COSC(I) - DELTA
C
C       -if convergence criterion has not been met, keep trying
         J = J+1
         IF( ABS(DELTA).GT.XLIM ) GO TO 30
C        PRINT*,' LAT NO.',I,J,' ITERATIONS'
C
C       -determine the Gaussian weights
         C      = 2.0 *( 1.0-COSC(I)*COSC(I) )
         CALL LGORD( D, COSC(I), NLAT-1 )
         D      = D*D*FI*FI
         GWT(I) = C *( FI-0.5 ) / D
   40 CONTINUE
C
C    -determine the colatitudes and sin(colat) and weights over sin**2
      DO 50 I=1,NZERO
         COLAT(I)= ACOS(COSC(I))
         SINC(I) = SIN(COLAT(I))
         WOS2(I) = GWT(I) /( SINC(I)*SINC(I) )
   50 CONTINUE
C
C    -if NLAT is odd, set values at the equator
      IF( MOD(NLAT,2) .NE. 0 ) THEN
         I       = NZERO+1
         COSC(I) = 0.0
         C       = 2.0
         CALL LGORD( D, COSC(I), NLAT-1 )
         D       = D*D*FI*FI
         GWT(I)  = C *( FI-0.5 ) / D
         COLAT(I)= PI*0.5
         SINC(I) = 1.0
         WOS2(I) = GWT(I)
      END IF
C
C    -determine the southern hemisphere values by symmetry
      DO 60 I=NLAT-NZERO+1,NLAT
         COSC(I) =-COSC(NLAT+1-I)
         GWT(I)  = GWT(NLAT+1-I)
         COLAT(I)= PI-COLAT(NLAT+1-I)
         SINC(I) = SINC(NLAT+1-I)
         WOS2(I) = WOS2(NLAT+1-I)
   60 CONTINUE
c     PRINT*,'NLAT=',NLAT
c     PRINT*,'COLATS'
c     PRINT 101,(I,COLAT(I),COLAT(I)*180./PI,I=1,NLAT)
  101 FORMAT(1X,I3,F16.12,2X,F8.2)
  102 FORMAT(1X,I3,F16.12,2X,F16.12)
c     PRINT*,'COS(COLAT), SIN(COLAT)'
c     PRINT 102,(I,COSC(I),SINC(I),I=1,NLAT)
c     PRINT*,'WEIGHT, GWT/COS**2'
c     PRINT 102,(I,GWT(I),WOS2(I),I=1,NLAT)
C
      RETURN
      END
      SUBROUTINE LGORD( F, COSC, N )
C
C  LGORD calculates the value of an ordinary Legendre polynomial at a
C  latitude.
C
C  On entry:
C     COSC - cos(colatitude)
C     N      - the degree of the polynomial
C
C  On exit:
C     F      - the value of the Legendre polynomial of degree N at
C              latitude asin(COSC)
C
C------------------------------------------------------------------------
C
C    -determine the colatitude
      COLAT = ACOS(COSC)
C
      C1 = SQRT(2.0)
      DO 20 K=1,N
         C1 = C1 * SQRT( 1.0 - 1.0/(4*K*K) )
   20 CONTINUE
C
      FN = N
      ANG= FN * COLAT
      S1 = 0.0
      C4 = 1.0
      A  =-1.0
      B  = 0.0
      DO 30 K=0,N,2
         IF (K.EQ.N) C4 = 0.5 * C4
         S1 = S1 + C4 * COS(ANG)
         A  = A + 2.0
         B  = B + 1.0
         FK = K
         ANG= COLAT * (FN-FK-2.0)
         C4 = ( A * (FN-B+1.0) / ( B * (FN+FN-A) ) ) * C4
   30 CONTINUE
      F = S1 * C1
C
      RETURN
      END
 */
