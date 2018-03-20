/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.gempak;


import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import java.io.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Read a Gempak sounding file
 *
 * @author IDV Development Team
 */
public class GempakSoundingFileReader extends AbstractGempakStationFileReader {

  /**
   * Surface Text identifier
   */
  public static final String SNDT = "SNDT";

  /**
   * date key identifier
   */
  public static final String DATE = "DATE";

  /**
   * time key identifier
   */
  public static final String TIME = "TIME";

  /**
   * PRES vertical coordinate
   */
  public static final int PRES_COORD = 1;

  /**
   * THTA vertical coordinate
   */
  public static final int THTA_COORD = 2;

  /**
   * HGHT vertical coordinate
   */
  public static final int HGHT_COORD = 3;

  /**
   * standard surface file id
   */
  public static final String MERGED = "merged";

  /**
   * climate surface file id
   */
  public static final String UNMERGED = "unmerged";

  /**
   * vertical coordinate
   */
  private int ivert = -1;

  /**
   * list of unmerged parts
   */
  private List<String> unmergedParts;

  /**
   * array of manadatory parameters
   */
  private final String[] mandpp = {"PRES", "TEMP", "DWPT", "DRCT", "SPED", "HGHT"};

  /**
   * array of significant temperature parameters
   */
  private final String[] sigtpp = {"PRES", "TEMP", "DWPT"};

  /**
   * array of significant wind parameters
   */
  private final String[] sigwpp = {"HGHT", "DRCT", "SPED"};

  /**
   * array of tropopause parameters
   */
  private final String[] troppp = {"PRES", "TEMP", "DWPT", "DRCT", "SPED"};

  /**
   * array of maximum wind parameters
   */
  private final String[] maxwpp = {"PRES", "DRCT", "SPED"};

  /**
   * array of groups below 100 mb
   */
  private final String[] belowGroups = {"TTAA", "TRPA", "MXWA", "PPAA", "TTBB", "PPBB"};

  /**
   * array of groups above 100 mb
   */
  private final String[] aboveGroups = {"TTCC", "TRPC", "MXWC", "PPCC", "TTDD", "PPDD"};

  /**
   * list of valid params for each group
   */
  private final String[][] parmLists = {mandpp, troppp, maxwpp, maxwpp, sigtpp, sigwpp};

  /**
   * Default ctor
   */
  GempakSoundingFileReader() {
  }

  /**
   * Initialize the file, read in all the metadata (ala DM_OPEN)
   *
   * @param raf       RandomAccessFile to read.
   * @param fullCheck if true, check entire structure
   * @return A GempakSoundingFileReader
   * @throws IOException problem reading file
   */
  public static GempakSoundingFileReader getInstance(RandomAccessFile raf, boolean fullCheck) throws IOException {
    GempakSoundingFileReader gsfr = new GempakSoundingFileReader();
    gsfr.init(raf, fullCheck);
    return gsfr;
  }

  /**
   * Initialize this reader.  Read all the metadata
   *
   * @return true if successful
   * @throws IOException problem reading the data
   */
  protected boolean init() throws IOException {
    return init(true);
  }

  /**
   * Initialize this reader.  Get the Grid specific info
   *
   * @param fullCheck check to make sure there are grids we can handle
   * @return true if successful
   * @throws IOException problem reading the data
   */
  protected boolean init(boolean fullCheck) throws IOException {

    if (!super.init(fullCheck)) {
      return false;
    }

    // Modeled after SN_OFIL
    if (dmLabel.kftype != MFSN) {
      logError("not a sounding data file ");
      return false;
    }

    DMPart part = getPart(SNDT);

    if (part != null) {  // merged file
      subType = MERGED;
      String vertName = part.params.get(0).kprmnm;
      switch (vertName) {
        case "PRES":
          ivert = PRES_COORD;
          break;
        case "THTA":
          ivert = THTA_COORD;
          break;
        case "HGHT":
        case "MHGT":
        case "DHGT":
          ivert = HGHT_COORD;
          break;
        default:
          logError("unknown vertical coordinate in merged file");
          return false;
      }

    } else {
      unmergedParts = SN_CKUA();
      boolean haveUnMerged = !unmergedParts.isEmpty();
      if (!haveUnMerged) {
        logError("unknown sounding file type - not merged/unmerged");
        return false;
      } else {
        ivert = PRES_COORD;
        subType = UNMERGED;
      }
    }
    if (!readStationsAndTimes(true)) {
      logError("Unable to read stations and times");
      return false;
    }

    return true;

  }

  /**
   * Get the vertical coordinate
   *
   * @return the vertical coordinate (PRES_COORD, THTA_COORD, HGHT_COORD)
   */
  public int getVerticalCoordinate() {
    return ivert;
  }

  /**
   * Get the list of merged parts in this file
   *
   * @return a list of the unmerged parts (only SNDT)
   */
  public List<String> getMergedParts() {
    List<String> list = new ArrayList<>(1);
    list.add(SNDT);
    return list;
  }

  /**
   * Get the list of unmerged parts in this file
   *
   * @return a list of the unmerged parts
   */
  public List<String> getUnmergedParts() {
    return new ArrayList<>(unmergedParts);
  }

  /**
   * Make the file subtype
   */
  protected void makeFileSubType() {
  }  // already set in init()

  /**
   * Print the list of dates in the file
   *
   * @param row ob row
   * @param col ob column
   */
  public void printOb(int row, int col) {
    GempakStation station = getStations().get(col - 1);
    String time = getDateString(row - 1);
    StringBuilder builder = new StringBuilder("\n");
    builder.append(makeHeader(station, time));
    builder.append("\n");
    boolean merge = getFileSubType().equals(MERGED);
    List<String> parts;
    if (merge) {
      parts = new ArrayList<>();
      parts.add(SNDT);
    } else {
      parts = unmergedParts;
    }
    for (String part : parts) {
      RData rd;
      try {
        rd = DM_RDTR(row, col, part);
      } catch (IOException ioe) {
        ioe.printStackTrace();
        rd = null;
      }
      if (rd == null) {
        continue;
      }
      if (!merge) {
        builder.append("    ");
        builder.append(part);
        builder.append("    ");
        builder.append(time.substring(time.indexOf("/") + 1));
      }
      builder.append("\n");
      if (!merge) {
        builder.append("\t");
      }
      List<GempakParameter> params = getParameters(part);
      for (GempakParameter parm : params) {
        builder.append(StringUtil2.padLeft(parm.getName(), 7));
        builder.append("\t");
      }
      builder.append("\n");
      if (!merge) {
        builder.append("\t");
      }
      float[] data = rd.data;
      int numParams = params.size();
      int numLevels = data.length / numParams;
      for (int j = 0; j < numLevels; j++) {
        for (int i = 0; i < numParams; i++) {
          builder.append(
                  StringUtil2.padLeft(
                          Format.formatDouble(
                                  data[j * numParams + i], 7, 1), 7));
          builder.append("\t");
        }
        builder.append("\n");
        if (!merge) {
          builder.append("\t");
        }
      }
      builder.append("\n");
    }
    builder.append("\n");
    System.out.println(builder.toString());
  }

  /**
   * Make the header for the text report
   *
   * @param stn  the station
   * @param date the date
   * @return the header
   */
  private String makeHeader(GempakStation stn, String date) {
    StringBuilder builder = new StringBuilder();
    builder.append("STID = ");
    builder.append(StringUtil2.padRight((stn.getSTID().trim()
            + stn.getSTD2().trim()), 8));
    builder.append("\t");
    builder.append("STNM = ");
    builder.append(Format.i(stn.getSTNM(), 6));
    builder.append("\t");
    builder.append("TIME = ");
    builder.append(date);
    builder.append("\n");
    builder.append("SLAT = ");
    builder.append(Format.d(stn.getLatitude(), 5));
    builder.append("\t");
    builder.append("SLON = ");
    builder.append(Format.d(stn.getLongitude(), 5));
    builder.append("\t");
    builder.append("SELV = ");
    builder.append(Format.d(stn.getAltitude(), 5));
    builder.append("\n");
    return builder.toString();
  }

  /**
   * This subroutine checks the parts in a sounding data set for the
   * unmerged data types.
   *
   * @return list of part names
   */
  private List<String> SN_CKUA() {
    List<String> types = new ArrayList<>();
    boolean above = false;
    boolean done = false;
    String partToCheck;
    while (!done) {
      // check for mandatory groups
      for (int group = 0; group < belowGroups.length; group++) {
        if (above) {
          partToCheck = aboveGroups[group];
        } else {
          partToCheck = belowGroups[group];
        }
        if (checkForValidGroup(partToCheck, parmLists[group])) {
          types.add(partToCheck);
        }
      }
      if (!above) {
        above = true;
      } else {
        done = true;
      }
    }
    return types;
  }

  /**
   * Check for valid groups
   *
   * @param partToCheck the part name
   * @param params      the parameters that are supposed to be there
   * @return true if the part is there and has the right params
   */
  private boolean checkForValidGroup(String partToCheck, String[] params) {
    DMPart part = getPart(partToCheck);
    if (part == null) {
      return false;
    }
    int i = 0;
    for (DMParam parm : part.params) {
      if (!(parm.kprmnm.equals(params[i++]))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Run the program
   *
   * @param args [0] filename (required),
   *             [1] variable name (X for default),
   *             [2] X to not list grids
   * @throws IOException problem reading the file
   */
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.out.println("need to supply a GEMPAK sounding file name");
      System.exit(1);
    }
    try {
      GempakParameters.addParameters(
              "resources/nj22/tables/gempak/params.tbl");
    } catch (Exception e) {
      System.out.println("unable to init param tables");
    }

    GempakSoundingFileReader gsfr = getInstance(getFile(args[0]), true);
    System.out.println("Type = " + gsfr.getFileType());
    gsfr.printFileLabel();
    gsfr.printKeys();
    gsfr.printHeaders();
    gsfr.printParts();
    gsfr.printDates();
    gsfr.printStations(false);
    int row = 1;
    int col = 1;
    if (args.length > 1) {
      row = Integer.parseInt(args[1]);
    }
    if (args.length > 2) {
      try {
        col = Integer.parseInt(args[2]);
      } catch (Exception npe) {
        col = gsfr.findStationIndex(args[2]);
        if (col == -1) {
          System.out.println("couldn't find station " + args[2]);
          System.exit(1);
        }
        System.out.println("found station at column " + col);
      }
    }
    gsfr.printOb(row, col);
  }


}

