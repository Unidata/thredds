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
package ucar.nc2.iosp.hdf4;

import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;

/**
 * @author caron
 * @since Dec 20, 2007
 */
public class H4type {

  // p 110 table 9a : probably the "class" of the number type
  static String getNumberType(byte type) {
    switch (type) {
      case 0:
        return "NONE";
      case 1:
        return "IEEE";
      case 2:
        return "VAX";
      case 3:
        return "CRAY";
      case 4:
        return "PC";
      case 5:
        return "CONVEX";
      default:
        throw new IllegalStateException("unknown type= " + type);
    }
  }

  /* type info codes from hntdefs.h
    #define DFNT_UCHAR8      3
    #define DFNT_CHAR8       4
    #define DFNT_FLOAT32     5
    #define DFNT_FLOAT64     6

    #define DFNT_INT8       20
    #define DFNT_UINT8      21
    #define DFNT_INT16      22
    #define DFNT_UINT16     23
    #define DFNT_INT32      24
    #define DFNT_UINT32     25
    #define DFNT_INT64      26
    #define DFNT_UINT64     27
  */
  static DataType setDataType(short type, Variable v) {
    DataType dt;
    boolean unsigned = false;
    switch (type) {
      case 3:
        dt = DataType.BYTE;
        unsigned = true;
        break;
      case 4:
        dt =  DataType.CHAR;
        break;
      case 5:
        dt =  DataType.FLOAT;
        break;
      case 6:
        dt =  DataType.DOUBLE;
        break;
      case 21:
        unsigned = true;
      case 20:
        dt =  DataType.BYTE;
        break;
      case 23:
        unsigned = true;
      case 22:
        dt =  DataType.SHORT;
        break;
      case 25:
        unsigned = true;
      case 24:
        dt =  DataType.INT;
        break;
      case 27:
        unsigned = true;
      case 26:
        dt =  DataType.LONG;
        break;
      default:
        throw new IllegalStateException("unknown type= " + type);
    }

    if (v != null) {
      v.setDataType(dt);
      if (unsigned)
         v.addAttribute(new Attribute("_Unsigned", "true"));
    }

    return dt;
  }

}

/*


/* $Id: hntdefs.h,v 1.5 2000/05/23 18:03:30 acheng Exp $

/*+ hnt.h
   *** This file contains all the number-type definitions for HDF


#ifndef _HNT_H
#define _HNT_H

/* masks for types
#define DFNT_HDF      0x00000000    /* standard HDF format
#define DFNT_NATIVE   0x00001000    /* native format
#define DFNT_CUSTOM   0x00002000    /* custom format
#define DFNT_LITEND   0x00004000    /* Little Endian format
#define DFNT_MASK     0x00000fff    /* format mask

/* type info codes

#define DFNT_NONE        0  /* indicates that number type not set
#define DFNT_QUERY       0  /* use this code to find the current type
#define DFNT_VERSION     1  /* current version of NT info

#define DFNT_FLOAT32     5
#define DFNT_FLOAT       5  /* For backward compat; don't use
#define DFNT_FLOAT64     6
#define DFNT_DOUBLE      6  /* For backward compat; don't use
#define DFNT_FLOAT128    7  /* No current plans for support

#define DFNT_INT8       20
#define DFNT_UINT8      21

#define DFNT_INT16      22
#define DFNT_UINT16     23
#define DFNT_INT32      24
#define DFNT_UINT32     25
#define DFNT_INT64      26
#define DFNT_UINT64     27
#define DFNT_INT128     28  /* No current plans for support
#define DFNT_UINT128    30  /* No current plans for support

#define DFNT_UCHAR8      3  /* 3 chosen for backward compatibility
#define DFNT_UCHAR       3  /* uchar=uchar8 for backward combatibility
#define DFNT_CHAR8       4  /* 4 chosen for backward compatibility
#define DFNT_CHAR        4  /* uchar=uchar8 for backward combatibility
#define DFNT_CHAR16     42  /* No current plans for support
#define DFNT_UCHAR16    43  /* No current plans for support

/* Type info codes for Native Mode datasets
#define DFNT_NFLOAT32   (DFNT_NATIVE | DFNT_FLOAT32)
#define DFNT_NFLOAT64   (DFNT_NATIVE | DFNT_FLOAT64)
#define DFNT_NFLOAT128  (DFNT_NATIVE | DFNT_FLOAT128)   /* Unsupported

#define DFNT_NINT8      (DFNT_NATIVE | DFNT_INT8)
#define DFNT_NUINT8     (DFNT_NATIVE | DFNT_UINT8)
#define DFNT_NINT16     (DFNT_NATIVE | DFNT_INT16)
#define DFNT_NUINT16    (DFNT_NATIVE | DFNT_UINT16)
#define DFNT_NINT32     (DFNT_NATIVE | DFNT_INT32)
#define DFNT_NUINT32    (DFNT_NATIVE | DFNT_UINT32)
#define DFNT_NINT64     (DFNT_NATIVE | DFNT_INT64)
#define DFNT_NUINT64    (DFNT_NATIVE | DFNT_UINT64)
#define DFNT_NINT128    (DFNT_NATIVE | DFNT_INT128)     /* Unsupported
#define DFNT_NUINT128   (DFNT_NATIVE | DFNT_UINT128)    /* Unsupported

#define DFNT_NCHAR8     (DFNT_NATIVE | DFNT_CHAR8)
#define DFNT_NCHAR      (DFNT_NATIVE | DFNT_CHAR8)  /* backward compat
#define DFNT_NUCHAR8    (DFNT_NATIVE | DFNT_UCHAR8)
#define DFNT_NUCHAR     (DFNT_NATIVE | DFNT_UCHAR8)     /* backward compat
#define DFNT_NCHAR16    (DFNT_NATIVE | DFNT_CHAR16)     /* Unsupported
#define DFNT_NUCHAR16   (DFNT_NATIVE | DFNT_UCHAR16)    /* Unsupported

/* Type info codes for Little Endian data
#define DFNT_LFLOAT32   (DFNT_LITEND | DFNT_FLOAT32)
#define DFNT_LFLOAT64   (DFNT_LITEND | DFNT_FLOAT64)
#define DFNT_LFLOAT128  (DFNT_LITEND | DFNT_FLOAT128)   /* Unsupported

#define DFNT_LINT8      (DFNT_LITEND | DFNT_INT8)
#define DFNT_LUINT8     (DFNT_LITEND | DFNT_UINT8)
#define DFNT_LINT16     (DFNT_LITEND | DFNT_INT16)
#define DFNT_LUINT16    (DFNT_LITEND | DFNT_UINT16)
#define DFNT_LINT32     (DFNT_LITEND | DFNT_INT32)
#define DFNT_LUINT32    (DFNT_LITEND | DFNT_UINT32)
#define DFNT_LINT64     (DFNT_LITEND | DFNT_INT64)
#define DFNT_LUINT64    (DFNT_LITEND | DFNT_UINT64)
#define DFNT_LINT128    (DFNT_LITEND | DFNT_INT128)     /* Unsupported
#define DFNT_LUINT128   (DFNT_LITEND | DFNT_UINT128)    /* Unsupported

#define DFNT_LCHAR8     (DFNT_LITEND | DFNT_CHAR8)
#define DFNT_LCHAR      (DFNT_LITEND | DFNT_CHAR8)  /* backward compat
#define DFNT_LUCHAR8    (DFNT_LITEND | DFNT_UCHAR8)
#define DFNT_LUCHAR     (DFNT_LITEND | DFNT_UCHAR8)     /* backward compat
#define DFNT_LCHAR16    (DFNT_LITEND | DFNT_CHAR16)     /* Unsupported
#define DFNT_LUCHAR16   (DFNT_LITEND | DFNT_UCHAR16)    /* Unsupported

/* class info codes for int
#define        DFNTI_MBO       1    /* Motorola byte order 2's compl
#define        DFNTI_VBO       2    /* Vax byte order 2's compl
#define        DFNTI_IBO       4    /* Intel byte order 2's compl

/* class info codes for float
#define        DFNTF_NONE      0    /* indicates subclass is not set
#define        DFNTF_HDFDEFAULT 1   /* hdf default float format is ieee
#define        DFNTF_IEEE      1    /* IEEE format
#define        DFNTF_VAX       2    /* Vax format
#define        DFNTF_CRAY      3    /* Cray forma
#define        DFNTF_PC        4    /* PC floats - flipped IEEE
#define        DFNTF_CONVEX    5    /* CONVEX native format
#define        DFNTF_VP        6    /* Fujitsu VP native format
#define        DFNTF_CRAYMPP   7    /* Cray MPP format

/* class info codes for char
#define        DFNTC_BYTE      0    /* bitwise/numeric field
#define        DFNTC_ASCII     1    /* ASCII
#define        DFNTC_EBCDIC    5    /* EBCDIC

/* array order
#define        DFO_FORTRAN     1    /* column major order
#define        DFO_C           2    /* row major order

/******************************************************************
/* Sizes of number types
/******************************************************************

/* first the standard sizes of number types

#    define SIZE_FLOAT32    4
#    define SIZE_FLOAT64    8
#    define SIZE_FLOAT128  16   /* No current plans for support

#    define SIZE_INT8       1
#    define SIZE_UINT8      1
#    define SIZE_INT16      2
#    define SIZE_UINT16     2
#    define SIZE_INT32      4
#    define SIZE_UINT32     4
#    define SIZE_INT64      8
#    define SIZE_UINT64     8
#    define SIZE_INT128    16   /* No current plans for support
#    define SIZE_UINT128   16   /* No current plans for support

#    define SIZE_CHAR8      1
#    define SIZE_CHAR       1   /* For backward compat char8 == char
#    define SIZE_UCHAR8     1
#    define SIZE_UCHAR      1   /* For backward compat uchar8 == uchar
#    define SIZE_CHAR16     2   /* No current plans for support
#    define SIZE_UCHAR16    2   /* No current plans for support

/* then the native sizes of number types

/* Unusual number sizes
/* Cray (UNICOS) native number sizes:
	Char = 8 bits, unsigned
	Short=64 int=64 long=64 float=64 double=64 bits
	Long double=128 bits
	Char pointers = 64 bits
	Int pointers = 64 bits
/* T3D/T3E (CRAYMPP) native number sizes:
	Char = 8 bits, unsigned
	Short=32 int=64 long=64 float=32 double=64 bits
	Long double=64 bits
	Char pointers = 64 bits
	Int pointers = 64 bits
	Big endian, IEEE floating point
/* IA64 (IA64) native number sizes:
	Char = 8 bits, signed
	Short=16 int=32 long=64 float=32 double=64 bits
	Long double=64 bits
	Char pointers = 64 bits
	Int pointers = 64 bits
	Little endian, IEEE floating point

#if !defined(UNICOS)
#    define SIZE_NFLOAT32    4
#    define SIZE_NFLOAT64    8
#    define SIZE_NFLOAT128  16  /* No current plans for support

#    define SIZE_NINT8       1
#    define SIZE_NUINT8      1
#if defined(CRAYMPP)
#    define SIZE_NINT16      4
#    define SIZE_NUINT16     4
#else
#    define SIZE_NINT16      2
#    define SIZE_NUINT16     2
#endif
#    define SIZE_NINT32      4
#    define SIZE_NUINT32     4
#    define SIZE_NINT64      8
#    define SIZE_NUINT64     8
#    define SIZE_NINT128    16  /* No current plans for support
#    define SIZE_NUINT128   16  /* No current plans for support

#    define SIZE_NCHAR8      1
#    define SIZE_NCHAR       1  /* For backward compat char8 == char
#    define SIZE_NUCHAR8     1
#    define SIZE_NUCHAR      1  /* For backward compat uchar8 == uchar
#if defined(CRAYMPP)
#    define SIZE_NCHAR16     4  /* No current plans for support
#    define SIZE_NUCHAR16    4  /* No current plans for support
#else
#    define SIZE_NCHAR16     2  /* No current plans for support
#    define SIZE_NUCHAR16    2  /* No current plans for support
#endif
#else  /* !!!!!! SOMEBODY NEEDS TO CHECK THESE !!!!!
#    define SIZE_NFLOAT32    8
#    define SIZE_NFLOAT64    8
#    define SIZE_NFLOAT128  16  /* No current plans for support

#    define SIZE_NINT8       1
#    define SIZE_NUINT8      1
#    define SIZE_NINT16      8
#    define SIZE_NUINT16     8
#    define SIZE_NINT32      8
#    define SIZE_NUINT32     8
#    define SIZE_NINT64      8
#    define SIZE_NUINT64     8
#    define SIZE_NINT128    16  /* No current plans for support
#    define SIZE_NUINT128   16  /* No current plans for support
#    define SIZE_NCHAR8      1
#    define SIZE_NCHAR       1
#    define SIZE_NCHAR       1  /* For backward compat char8 == char
#    define SIZE_NUCHAR8     1
#    define SIZE_NUCHAR      1  /* For backward compat uchar8 == uchar
#    define SIZE_NCHAR16     2  /* No current plans for support
#    define SIZE_NUCHAR16    2  /* No current plans for support
#endif /* UNICOS */

/* then the sizes of little-endian number types
#    define SIZE_LFLOAT32    4
#    define SIZE_LFLOAT64    8
#    define SIZE_LFLOAT128  16  /* No current plans for support

#    define SIZE_LINT8       1
#    define SIZE_LUINT8      1
#    define SIZE_LINT16      2
#    define SIZE_LUINT16     2
#    define SIZE_LINT32      4
#    define SIZE_LUINT32     4
#    define SIZE_LINT64      8
#    define SIZE_LUINT64     8
#    define SIZE_LINT128    16  /* No current plans for support
#    define SIZE_LUINT128   16  /* No current plans for support

#    define SIZE_LCHAR8      1
#    define SIZE_LCHAR       1  /* For backward compat char8 == char
#    define SIZE_LUCHAR8     1
#    define SIZE_LUCHAR      1  /* For backward compat uchar8 == uchar
#    define SIZE_LCHAR16     2  /* No current plans for support
#    define SIZE_LUCHAR16    2  /* No current plans for support

    /* sizes of different number types
#       define MACHINE_I8_SIZE     1
#       define MACHINE_I16_SIZE    2
#       define MACHINE_I32_SIZE    4
#       define MACHINE_F32_SIZE    4
#       define MACHINE_F64_SIZE    8

    /* maximum size of the atomic data types
#       define MAX_NT_SIZE      16
#endif /* _HNT_H */

/* -----------  Reserved classes and names for vdatas/vgroups -----

/* The names of the Vgroups created by the GR interface, from mfgr.h
#define GR_NAME "RIG0.0"          /* name of the Vgroup containing all the images
#define RI_NAME "RI0.0"           /* name of a Vgroup containing information about one image
#define RIGATTRNAME  "RIATTR0.0N" /* name of a Vdata containing an   attribute
#define RIGATTRCLASS "RIATTR0.0C" /* class of a Vdata containing an   attribute

/* Vdata and Vgroup attributes use the same class as that of SD attr,
 *  _HDF_ATTRIBUTE  "Attr0.0"  8/1/96

/* classes of the Vdatas/Vgroups created by the SD interface, from local_nc.h
#define _HDF_ATTRIBUTE         "Attr0.0"  /* class of a Vdata containing SD interface attribute
#define _HDF_VARIABLE          "Var0.0"   /* class of a Vgroup representing an SD NDG
#define _HDF_DIMENSION         "Dim0.0"   /* class of a Vgroup representing an SD dimension
#define _HDF_UDIMENSION        "UDim0.0"  /* class of a Vgroup representing an SD UNLIMITED dimension
#define DIM_VALS          "DimVal0.0"     /* class of a Vdata containing an SD dimension size and fake values
#define DIM_VALS01        "DimVal0.1"     /* class of a Vdata containing an SD dimension size

#define _HDF_CDF               "CDF0.0"

/* DATA is defined in DTM. Change DATA to DATA0
#define DATA              "Data0.0"
#define DATA0             "Data0.0"
#define ATTR_FIELD_NAME   "VALUES"

/* The following vdata class name is reserved by the Chunking interface.
   originally defined in 'hchunks.h'. The full class name
   currently is "_HDF_CHK_TBL_0". -GV 9/25/97
#ifdef   _HCHUNKS_MAIN /* Private to 'hchunks.c'
#define _HDF_CHK_TBL_CLASS "_HDF_CHK_TBL_" /* 13 bytes
#define _HDF_CHK_TBL_CLASS_VER  0          /* zero version number for class
#endif /* _HCHUNKS_MAIN_


/* ------------  pre-defined attribute names ----------------
/* For MFGR interface
#define FILL_ATTR    "FillValue"   /* name of an attribute containing the fill value

/* For SD interface
#define _FillValue      "_FillValue" /* name of an attribute to set fill value for an SDS
#define _HDF_LongName "long_name" /* data/dimension label string
#define _HDF_Units    "units"     /* data/dimension unit string
#define _HDF_Format   "format"    /* data/dimension format string
#define _HDF_CoordSys "coordsys"  /* data coordsys string
#define _HDF_ValidRange     "valid_range" /* valid range of data values
#define _HDF_ScaleFactor    "scale_factor" /* data calibration factor
#define _HDF_ScaleFactorErr "scale_factor_err" /* data calibration factor error
#define _HDF_AddOffset      "add_offset" /* calibration offset
#define _HDF_AddOffsetErr   "add_offset_err" /*  calibration offset error
#define _HDF_CalibratedNt   "calibrated_nt"  /* data type of uncalibrated data
#define _HDF_ValidMax       "valid_max"
#define _HDF_ValidMin       "valid_min"
#define _HDF_Remarks        "remarks"        /* annotation, by DFAN
#define _HDF_AnnoLabel      "anno_label"     /* annotation label, by DFAN */


