package ucar.nc2;

import ucar.ma2.*;

import java.io.IOException;


import static ucar.nc2.NetcdfFileWriteable.createNew;

public class TestCreateManyObjects {

    private static void nccreate(String filename, int nvars) {

        NetcdfFileWriteable ncfile = null;
        try {
            ncfile = createNew(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Dimension x = ncfile.addDimension("x", 2);
        Dimension y = ncfile.addDimension("y", 2);
        Dimension z = ncfile.addDimension("z", 2);

        Dimension[] dim3 = new Dimension[3];
        dim3[0] = x;
        dim3[1] = y;
        dim3[2] = z;

        int i;
        for (i=0; i<nvars+1; i++) {
            ncfile.addVariable("var" + i, DataType.FLOAT, dim3);
        }

        try {
            ncfile.create();
            ncfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            long start = System.nanoTime();
            NetcdfFileWriteable nc = NetcdfFileWriteable.openExisting(filename);
            double diff = (System.nanoTime() - start)/1000000000.0;
            nc.close();
            double ratio = diff/nvars;
            System.out.println(String.format("%8d vars: %7.4f sec, %6.6f sec/var", nvars, diff, ratio));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        String workdir = "C:/temp/";
        nccreate(workdir + "testWrite10.nc", 10);
        nccreate(workdir + "testWrite1000.nc", 1000);
        nccreate(workdir + "testWrite10000.nc", 10000);
        nccreate(workdir + "testWrite50000.nc", 50000);
    }


}
