package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;

public class TestCreateAggDatasets  {

  private static void makeType3( String filename, int start, int ntimes, String timeUnits) throws Exception {
    NetcdfFileWriteable ncfile = new NetcdfFileWriteable();
    ncfile.setName(filename);

        // define dimensions
    Dimension latDim = ncfile.addDimension("lat", 3);
    Dimension lonDim = ncfile.addDimension("lon", 4);
    Dimension timeDim = ncfile.addDimension("time", ntimes);

    // define Variables
    Dimension[] dim3 = new Dimension[3];
    dim3[0] = timeDim;
    dim3[1] = latDim;
    dim3[2] = lonDim;

    ncfile.addVariable("P", double.class, dim3);
    ncfile.addVariable("T", double.class, dim3);
    ncfile.addVariableAttribute("T", "long_name", "surface temperature");
    ncfile.addVariableAttribute("T", "units", "degC");

    ncfile.addVariable("lat", float.class, new Dimension[] {latDim});
    ncfile.addVariableAttribute("lat", "units", "degrees_north");

    ncfile.addVariable("lon", float.class, new Dimension[] {lonDim});
    ncfile.addVariableAttribute("lon", "units", "degrees_east");

    boolean addTimeCoord = timeUnits != null;
    if (addTimeCoord) {
      ncfile.addVariable("time", int.class, new Dimension[] {timeDim});
      ncfile.addVariableAttribute("time", "units", timeUnits);
    }

    //  :title = "Example Data" ;
    ncfile.addGlobalAttribute("title", "Example Data");

    // create the file
    ncfile.create();

    // write data
    ncfile.write("lat", Array.factory(new float[] {41, 40, 39}));
    ncfile.write("lon", Array.factory(new float[] {-109, -107, -105, -103}));

    if (addTimeCoord) {
      Array timeValues = Array.factory( int.class, new int[] {ntimes});
      IndexIterator tvIter = timeValues.getIndexIterator();
      int time = start;
      while (tvIter.hasNext()) {
        tvIter.setIntNext( time++);
      }
      ncfile.write("time", timeValues);
    }

    Variable t = ncfile.findVariable("T");
    int shape[] = t.getShape();
    Array vals = Array.factory(t.getDataType().getPrimitiveClassType(), shape);
    Index tIndex = vals.getIndex();
    for (int i=0; i<shape[0]; i++)
     for (int j=0; j<shape[1]; j++)
      for (int k=0; k<shape[2]; k++)
        vals.setDouble( tIndex.set(i, j, k), 100*(i+start) + 10*j + k);
    ncfile.write("T", vals);

    for (int i=0; i<shape[0]; i++)
     for (int j=0; j<shape[1]; j++)
      for (int k=0; k<shape[2]; k++)
        vals.setDouble( tIndex.set(i, j, k), 200*(i+start) + 20*j + 2*k);
    ncfile.write("P", vals);

    // all done
    ncfile.close();
    System.out.println( "*****************Create Netcdf == "+filename);
  }

  private static void makeType1( String filename, int start)  throws Exception {
    NetcdfFileWriteable ncfile = new NetcdfFileWriteable();
    ncfile.setName(filename);

        // define dimensions
    Dimension latDim = ncfile.addDimension("lat", 3);
    Dimension lonDim = ncfile.addDimension("lon", 4);

    // define Variables
    Dimension[] dims = new Dimension[2];
    dims[0] = latDim;
    dims[1] = lonDim;

    ncfile.addVariable("T", double.class, dims);
    ncfile.addVariableAttribute("T", "long_name", "surface temperature");
    ncfile.addVariableAttribute("T", "units", "degC");

    ncfile.addVariable("lat", float.class, new Dimension[] {latDim});
    ncfile.addVariableAttribute("lat", "units", "degrees_north");

    ncfile.addVariable("lon", float.class, new Dimension[] {lonDim});
    ncfile.addVariableAttribute("lon", "units", "degrees_east");

    //  :title = "Example Data" ;
    ncfile.addGlobalAttribute("title", "Example Data - Type 1 aggregation");

    // create the file
    ncfile.create();

    // write data
    ncfile.write("lat", Array.factory(new float[] {41, 40, 39}));
    ncfile.write("lon", Array.factory(new float[] {-109, -107, -105, -103}));

    Variable t = ncfile.findVariable("T");
    int shape[] = t.getShape();
    Array vals = Array.factory(t.getDataType().getPrimitiveClassType(), shape);
    Index tIndex = vals.getIndex();
    for (int i=0; i<shape[0]; i++)
     for (int j=0; j<shape[1]; j++)
        vals.setDouble( tIndex.set(i, j), 10*i + j + start);
    ncfile.write("T", vals);

    // all done
    ncfile.close();
    System.out.println( "*****************Create Netcdf == "+filename);
  }

  static public void main(String[] args)  throws Exception {
    String topDir = "test/data/";
    makeType3(topDir+ "jan.nc", 0, 31, "days since Jan 1, 2000");
    makeType3(topDir+  "feb.nc", 31, 28, "days since Jan 1, 2000");

    makeType3(topDir+ "janN.nc", 0, 31, null);
    makeType3(topDir+  "febN.nc", 31, 28, null);

    makeType1(topDir+  "time0.nc", 0);
    makeType1(topDir+  "time1.nc", 100);
    makeType1(topDir+  "time2.nc", 200);
  }

}
