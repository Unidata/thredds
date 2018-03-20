/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * map a native size_t with JNA.
 *
 * @see "https://github.com/twall/jna/issues/191"
 * @since 5/30/14
 */
public class SizeT extends IntegerType {
  public SizeT() { this(0); }
  public SizeT(long value) { super(Native.SIZE_T_SIZE, value, true); }
  public String toString() { return String.format("%d",super.longValue());}
}
