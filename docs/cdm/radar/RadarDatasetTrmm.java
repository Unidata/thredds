/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
package ucar.nc2.dt.radar;

import java.util.Date;

/* Radar design from http://trmm-fc.gsfc.nasa.gov/trmm_gv/software/rsl/RSL_structures.html */

public class RadarDatasetTrmm {

  class Azimuth_hash {
    Ray ray;
    Azimuth_hash next, ray_high, ray_low;
  }

  // Constant Altitude PPI
  class Cappi {
    Date beginTime;

    float height;      /* Height for this Cappi. */
    float lat;
    float lon;         /* Lat/lon of lower left corner of Carpi. */
    int field_type;
    String radar_type; /* Value of Constant radar->h.radar_type */
    int interp_method; /* ??? string describing interpolation method. */

    Er_loc[] loc;       /* elevation and range coordinate array */
    Sweep[] sweep;      /* Data is stored in Sweeps */
  }

  // Constant Altitude Rectangular from Polar Image
  class Carpi {
    Date beginTime;

    float dx, dy;           /* Size of cell in km. */
    int radar_x, radar_y; /* Location of center of radar. */
    float height;           /* Height of this Carpi. */
    float lat, lon;         /* Lat/lon of lower left corner of Carpi. */
    String radar_type;   /* Radar types. */
    int field_type;       /* Same as for Radar. */
    int interp_method;    /* ??? string describing interpolation method. */

    //float (*f)(Carpi_value x);    /* Data conversion function. f(x). */
    //Carpi_value (*invf)(float x); /* Data conversion function. invf(x). */

    int nx, ny;           /* Number of cells. */
    byte[] data;          /* data[ny][nx]. could be another data type == "Carpi_value" */
  }

  // cube cartesean coordinates
  class Cube {
    float lat, lon;
    float dx, dy, dz;
    int nx, ny, nz;
    String data_type;
    Carpi[][] carpi;
  }

  class Er_loc {
    float elev; /* elevation angle */
    float srange; /* slant range !!! */
  }

  class Hash_table {
    Azimuth_hash[][] indexes;
    int nindexes;
  }

  class Histogram {
    int nbins;
    int low;
    int hi;
    int ucount;
    int ccount;
    int[] data;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  class Ray {
    Date time;
    float unam_rng;  /* Unambiguous range. (KM). */
    float azimuth;   /* Azimuth angle. (degrees). Must be positive
                                          * 0=North, 90=east, -90/270=west.
                      * This angle is the mean azimuth for the whole ray.
                                          * Eg. for NSIG the beginning and end azimuths are
                                          *     averaged.
                                          */
    int ray_num;   /* Ray no. within elevation scan. */
    float elev;       /* Elevation angle. (degrees). */
    int elev_num;   /* Elevation no. within volume scan. */

    int range_bin1; /* Range to first gate.(meters) */
    int gate_size;  /* Data gate size (meters)*/

    float vel_res;    /* Doppler velocity resolution */
    float sweep_rate;   /* Sweep rate. Full sweeps/min. */

    int prf;          /* Pulse repitition frequency, in Hz. */
    float azim_rate;
    float fix_angle;
    float pitch;      /* Pitch angle. */
    float roll;       /* Roll  angle. */
    float heading;    /* Heading. */
    float pitch_rate; /* (angle/sec) */
    float roll_rate;  /* (angle/sec) */
    float heading_rate; /* (angle/sec) */

    float lat;          /* Latitude (degrees) */
    float lon;          /* Longitude (degrees) */
    int alt;          /* Altitude (m) */

    float rvc;          /* Radial velocity correction (m/sec) */
    float vel_east;     /* Platform velocity to the east  (m/sec) */
    float vel_north;    /* Platform velocity to the north (m/sec) */
    float vel_up;       /* Platform velocity toward up    (m/sec) */
    float pulse_count;
    float pulse_width; /* Pulse width (micro-sec). */
    float beam_width;  /* Beamwidth in degrees. */
    float frequency;   /* Bandwidth MHz. */
    float wavelength;  /* Wavelength. Meters. */
    float nyq_vel;    /* Nyquist velocity. m/s */

    // float (*f)(Range x);       /* Data conversion function. f(x). */
    // Range (*invf)(float x);    /* Data conversion function. invf(x). */
    int nbins;
    short[] range; /* range[0..nbins-1] may be a byte */
    /* For wsr88d file:
     * 0..460 for reflectivity, 0..920 for velocity and
     * spectrum width. You must allocate this space.
     */
  }

  class Sweep {
    int sweep_num;      /* Integer sweep number. */
    float elev;         /* Elevation angle (mean) for the sweep. */
    float beam_width;   /* This is in the ray header too. */
    float vert_half_bw; /* Vertical beam width divided by 2 */
    float horz_half_bw; /* Horizontal beam width divided by 2 */

    // float (*f)(Range x); /* Data conversion function. f(x). */
    // Range (*invf)(float x); /* Data conversion function. invf(x). */

    int nrays;
    Ray[] ray; /* ray[0..nrays-1]. */
  }

  class Volume {
    String type_str;            /* One of:'Reflectivity', 'Velocity' or 'Spectrum width' */
    float calibr_const;        /* Calibration constant. */
    // float (*f)(Range x);       /* Data conversion function. f(x). */
    // Range (*invf)(float x);    /* Data conversion function. invf(x). */

    int nsweeps;
    Sweep[] sweep;   /* sweep[0..nsweeps-1]. */
  }

  class Radar {
    Date baseTime;
    String radar_type; /* Type of radar. Use for QC-ing the data.
                         * Supported types are:
                         * "wsr88d", "lassen", "uf",
                         * "nsig", "nsig2", "mcgill",
                         * "kwajalein", "rsl", "toga".
                         * Set by appropriate ingest routine.
                         */
    int number;        /* arbitrary number of this radar site */
    String name;      /* Nexrad site name */
    String radar_name; /* Radar name. */
    String project;   /* Project assocated with data. */
    String city;     /* nearest city to radaar site */
    String state;     /* state of radar site */

    double lat, lon;
    double height; /* height of site in meters above sea level*/

    int spulse; /* length of short pulse (ns)*/
    int lpulse; /* length of long pulse (ns) */

    int nvolumes;
    Volume[] v;   /* Array 0..nvolumes-1 of pointers to Volumes.
                        * 0 = DZ_INDEX = reflectivity.
                        * 1 = VR_INDEX = velocity.
                        * 2 = SW_INDEX = spectrum_width.
                        * 3 = CZ_INDEX = corrected reflectivity.
                        * 4 = ZT_INDEX = total reflectivity.
                        * 5 = DR_INDEX = differential refl.
                        * 6 = LR_INDEX = another differential refl.
                        * 7 = ZD_INDEX = another refl form.
                        * 8 = DM_INDEX = recieved power.
                        * 9 = RH_INDEX = Rho coefficient.
                        *10 = PH_INDEX = Phi (MCTEX parameter).
                        *11 = XZ_INDEX = X-band reflectivity.
                        *12 = CR_INDEX = Corrected DR.
                        *13 = MZ_INDEX = DZ mask for 1C-51 HDF.
                        *14 = MR_INDEX = DR mask for 1C-51 HDF.
                        *15 = ZE_INDEX = Edited reflectivity.
                        *16 = VE_INDEX = Edited velocity.
                        *17 = KD_INDEX = KDP (unknown)  for MCTEX data.
                        *18 = TI_INDEX = TIME (unknown)  for MCTEX data.
                  */
  }

/*
 * DZ     Reflectivity (dBZ), may contain some   DZ_INDEX
 *        signal-processor level QC and/or      
 *        filters. This field would contain 
 *        Darwin's CZ, or WSR88D's standard 
 *        reflectivity. In other words, unless
 *        the field is described otherwise, it
 *        should always go here. In essence, this
 *        is the "cleanest" reflectivity field
 *        for a radar.
 *
 * VR     Radial Velocity (m/s)                  VR_INDEX
 *
 * SW     Spectral Width (m2/s2)                 SW_INDEX
 *
 * CZ     QC Reflectivity (dBZ), contains
 *        post-processed QC'd data               CZ_INDEX
 *
 * ZT     Total Reflectivity (dBZ)               ZT_INDEX
 *        May be uncommon, but important
 *        This is UZ in UF files.
 *
 * DR     Differential reflectivity              DR_INDEX
 *        DR and LR are for dual-polarization
 *        radars only. Unitless or in dB.
 *
 * LR     Another form of differential ref       LR_INDEX
 *        called LDR, not sure of units
 *
 * ZD     ZDR: Reflectivity Depolarization Ratio ZD_INDEX
 *        ZDR = 10log(ZH/ZV)  (dB)
 *
 * DM     Received power measured by the radar.  DM_INDEX
 *        Units are dBm.
 *
 * RH     Rho : Correlation coefficient (MCTEX)  RH_INDEX
 *
 * PH     Phi (MCTEX parameter)                  PH_INDEX
 *
 * XZ     X-band reflectivity                    XZ_INDEX
 *
 * CD     Corrected ZD reflectivity (differential) CD_INDEX
 *        contains QC'ed data
 *
 * MZ     DZ mask volume for HDF 1C-51 product.  MZ_INDEX
 *
 * MD     ZD mask volume for HDF 1C-51 product.  MD_INDEX
 *
 * ZE     Edited Reflectivity.                   ZE_INDEX
 *
 * VE     Edited Velocity.                       VE_INDEX
 *
 * KD     KDP (unknown)  for MCTEX data.         KD_INDEX
 *
 * TI     TIME (unknown)  for MCTEX data.        TI_INDEX
 */
}
