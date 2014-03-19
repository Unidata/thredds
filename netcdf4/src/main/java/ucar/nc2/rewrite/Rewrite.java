/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.rewrite;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.jni.netcdf.Nc4Iosp;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class Description.
 *
 * @author caron
 * @since 4/8/13
 */
public class Rewrite {

  NetcdfFile ncIn;
  NetcdfFileWriter ncOut;
  NetcdfFileWriter.Version version;
  boolean isRadial = false;

  public Rewrite(NetcdfFile ncIn, NetcdfFileWriter ncOut) {
    this.ncIn = ncIn;
    this.ncOut = ncOut;
    this.version = ncOut.getVersion();
  }

  public void rewrite() throws IOException, InvalidRangeException {
    Attribute attr = ncIn.getRootGroup().findAttribute("featureType");
    if(attr.getStringValue().contains("RADIAL"))
        isRadial = true;
    createGroup(null, ncIn.getRootGroup());

    ncOut.create();

    transferData(ncIn.getRootGroup());

    ncOut.close();
  }

  private int anon = 0;
  void createGroup(Group newParent, Group oldGroup) throws IOException, InvalidRangeException {
    Group newGroup = ncOut.addGroup(newParent, oldGroup.getShortName());

    for (Attribute att : oldGroup.getAttributes())
      newGroup.addAttribute(att);

    for (Dimension dim : oldGroup.getDimensions()) {
      ncOut.addDimension(newGroup, dim.getShortName(), dim.getLength(), true, dim.isUnlimited(), dim.isVariableLength());
    }

    for (Variable v : oldGroup.getVariables()) {
      List<Dimension> dims = v.getDimensions();

      // all dimensions must be shared (!)
      for (Dimension dim : dims) {
        if (!dim.isShared()) {
          dim.setName("anon"+anon);
          dim.setShared(true);
          anon++;
          ncOut.addDimension(newGroup, dim.getShortName(), dim.getLength(), true, dim.isUnlimited(), dim.isVariableLength());
        }
      }

      Variable nv;
      if (!isRadial && v.getRank() >= 3) {  // make first dimension last
        StringBuilder sb = new StringBuilder();
        for (int i=1; i<dims.size(); i++)
          sb.append(dims.get(i).getShortName()).append(" ");
        sb.append(dims.get(0).getShortName());
        nv = ncOut.addVariable(null, v.getShortName(), v.getDataType(), sb.toString());

      } else {
        nv = ncOut.addVariable(null, v.getShortName(), v.getDataType(), v.getDimensionsString());
      }

      for (Attribute att : v.getAttributes())
        ncOut.addVariableAttribute(nv, att);
    }

    // recurse
    for (Group g : oldGroup.getGroups())
      createGroup(newGroup, g);
  }

  void transferData(Group oldGroup) throws IOException, InvalidRangeException {

    for (Variable v : oldGroup.getVariables()) {
      if (!isRadial && v.getRank() >= 3) {
        invertOneVar(v);

      } else {
        System.out.printf("write %s%n",v.getNameAndDimensions());
        Array data = v.read();
        Variable nv = ncOut.findVariable(v.getFullName());
        ncOut.write(nv,  data);
      }
    }

    // recurse
    for (Group g : oldGroup.getGroups())
      transferData( g);
  }

  // turn var(nt, any..) into newvar(any.., nt)
  void invertOneVar(Variable oldVar) throws IOException, InvalidRangeException {
    System.out.printf("invertOneVar %s%n",oldVar.getNameAndDimensions());
    int rank = oldVar.getRank();
    int[] origin = new int[rank];

    int[] shape = oldVar.getShape(); // old Shape

    Variable nv = ncOut.findVariable(oldVar.getFullName());
    Cache cache = new Cache(shape, nv.getShape(), oldVar.getDataType());

    int nt = shape[0];
    for (int k=0; k<nt; k++)  { // loop over outermost dimension
      shape[0] = 1;
      origin[0] = k;

      Array data = oldVar.read(origin, shape); // read inner
      cache.transfer(data.reduce(), k);
    }

    cache.write(nv);
  }

  private class Cache {
    int[] shape, newshape;
    int nt, chunksize;

    Array result, work;
    int counter = 0;

    Cache(int[] shape, int[] newshape, DataType dataType)  {
      this.shape = shape;
      this.newshape = newshape;
      this.result = Array.factory(dataType, newshape);

      nt = shape[0];
      Section s = new Section(shape);
      chunksize = (int)(s.computeSize() / nt);

      // get view of result as a 2d array (any..., nt);
      int[] reshape = new int[] {chunksize, nt};
      this.work = this.result.reshapeNoCopy(reshape);
    }

    // transfer the kth slice, where k is index on outer dimension of old var
    void transfer(Array slice, int k) {
      Index ima = work.getIndex();
      ima.set1(k); // this one stays fixed

      int count = 0;
      IndexIterator ii = slice.getIndexIterator();
      while (ii.hasNext()) {
        work.setDouble(ima.set0(count), ii.getDoubleNext());
        count++;
      }
    }

    void write(Variable newVar) throws IOException, InvalidRangeException {
      ncOut.write(newVar, result);
    }
  }

  public static void main(String arg[]) throws IOException, InvalidRangeException {
    /* String usage = "usage: ucar.nc2.rewrite.Rewrite -in <fileIn> -out <fileOut> [-isLargeFile] [-netcdf4]";
    if (arg.length < 4) {
      System.out.println(usage);
      System.exit(0);
    }

    boolean isLargeFile = false;
    boolean netcdf4 = false;
    String datasetIn = null, datasetOut = null;
    for (int i = 0; i < arg.length; i++) {
      String s = arg[i];
      if (s.equalsIgnoreCase("-in")) datasetIn = arg[i + 1];
      if (s.equalsIgnoreCase("-out")) datasetOut = arg[i + 1];
      if (s.equalsIgnoreCase("-isLargeFile")) isLargeFile = true;
      if (s.equalsIgnoreCase("-netcdf4")) netcdf4 = true;
    }
    if ((datasetIn == null) || (datasetOut == null)) {
      System.out.println(usage);
      System.exit(0);
    } */

    Nc4Iosp.setLibraryAndPath("C:\\netcdfc\\netCDF 4.3.0-rc4\\bin", "netcdf");

    long start = System.nanoTime();
    boolean netcdf4 = false;
    String datasetIn = "E:/data/nomads/problem/soilt1.gdas.200603.grb2";
    String datasetOut = "C:/temp/soilt1.gdas.200603.nc3";

    NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, null);
    System.out.printf("Read from %s write to %s%n", datasetIn, datasetOut);

    NetcdfFileWriter.Version version = netcdf4 ? NetcdfFileWriter.Version.netcdf4 : NetcdfFileWriter.Version.netcdf3;

    NetcdfFileWriter ncOut = NetcdfFileWriter.createNew(version, datasetOut);
    Rewrite rewrite = new Rewrite(ncfileIn, ncOut);
    rewrite.rewrite();
    ncfileIn.close();

    File oldFile = new File(datasetIn);
    File newFile = new File(datasetOut);
    double r =  (double) newFile.length() / oldFile.length();

    double took = (double) (System.nanoTime() - start) / 1000 / 1000 / 1000;
    System.out.printf("that took %f secs %n", took);

    System.out.printf("%nRewrite from %s (%d) to %s (%d) version = %s ratio = %f %n",
            datasetIn, oldFile.length(), datasetOut, newFile.length(), version, r);

  }

}
