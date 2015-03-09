/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.write;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.util.CancelTaskImpl;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;

/**
 * Utility to implement nccopy
 *
 * @author caron
 * @since 3/9/2015
 */
public class Nccopy {
    ///////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class CommandLine {
    @Parameter(names = {"-i", "--input"}, description = "Input dataset.", required = true)
    public File inputFile;

    @Parameter(names = {"-o", "--output"}, description = "Output file.", required = true)
    public File outputFile;

    @Parameter(names = {"-f", "--format"}, description = "Output file format. Allowed values = " +
                    "[netcdf3, netcdf4, netcdf4_classic, netcdf3c, netcdf3c64, ncstream]")
    public NetcdfFileWriter.Version format = NetcdfFileWriter.Version.netcdf3;

    @Parameter(names = {"-st", "--strategy"}, description = "Chunking strategy. Only used in NetCDF 4. " +
            "Allowed values = [standard, grib, none]")
    public Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.standard;

    @Parameter(names = {"-isLargeFile", "--isLargeFile"}, description = "Write to large file format. Only used in NetCDF 3. " +
            "Allowed values = [standard, grib, none]")
    public boolean isLargeFile = false;

   @Parameter(names = {"-d", "--deflateLevel"}, description = "Compression level. Only used in NetCDF 4. " +
            "Allowed values = 0 (no compression, fast) to 9 (max compression, slow)")
    public int deflateLevel = 5;

    @Parameter(names = {"-sh", "--shuffle"}, description = "Enable the shuffle filter, which may improve compression. " +
            "Only used in NetCDF 4. This option is ignored unless a non-zero deflate level is specified.")
    public boolean shuffle = true;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help = false;


    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {
      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames = Arrays.asList(
              "--input", "--output", "--format", "--isLargeFile", "--strategy", "--deflateLevel", "--shuffle", "--help");

      @Override
      public int compare(ParameterDescription p0, ParameterDescription p1) {
        int index0 = orderedParamNames.indexOf(p0.getLongestName());
        int index1 = orderedParamNames.indexOf(p1.getLongestName());
        assert index0 >= 0 : "Unexpected parameter name: " + p0.getLongestName();
        assert index1 >= 0 : "Unexpected parameter name: " + p1.getLongestName();

        return Integer.compare(index0, index1);
      }
    }

    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args);  // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName);           // Displayed in the usage information.

      // Set the ordering of of parameters in the usage information.
      jc.setParameterDescriptionComparator(new ParameterDescriptionComparator());
    }

    public void printUsage() {
      jc.usage();
    }

    public Nc4Chunking getNc4Chunking() {
      return Nc4ChunkingStrategy.factory(strategy, deflateLevel, shuffle);
    }

  }

  public static void main(String[] args) throws Exception {
    String progName = Nccopy.class.getName();

    try {
      CommandLine cmdLine = new CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }

      String datasetIn = cmdLine.inputFile.getAbsolutePath();
      String datasetOut = cmdLine.outputFile.getAbsolutePath();
      CancelTaskImpl cancel = new CancelTaskImpl();
      Formatter errlog = new Formatter();
      System.out.printf("NetcdfDatataset read from %s write %s to %s ", datasetIn, cmdLine.format, datasetOut);

      try ( NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, cancel)){

        FileWriter2 writer = new ucar.nc2.FileWriter2(ncfileIn, datasetOut, cmdLine.format, cmdLine.getNc4Chunking());
        writer.getNetcdfFileWriter().setLargeFile(cmdLine.isLargeFile);
        NetcdfFile ncfileOut = writer.write(cancel);

        if (ncfileOut != null) ncfileOut.close();
        cancel.setDone(true);
        System.out.printf("%s%n", cancel);

      } catch (Exception ex) {
        System.out.printf("%s = %s %n", ex.getClass().getName(), ex.getMessage());
        String err = errlog.toString();
        if (err.length() > 0)
          System.out.printf(" errlog=%s%n", err);
        // e.printStackTrace();
      }

    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }
}
