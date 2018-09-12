/*
 * Copyright 1998-2017 University Corporation for Atmospheric Research/Unidata
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

import ucar.ma2.ArrayObject;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.ArrayChar;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Formatter;

import org.jdom2.Element;

/**
 * Parse structural metadata from HDF-EOS.
 * This allows us to use shared dimensions, identify Coordinate Axes, and the FeatureType.
 *
 * <p>from HDF-EOS.status.ppt:
   <pre>
 HDF-EOS is format for EOS  Standard Products
   <ul>
   <li>Landsat 7 (ETM+)
   <li>Terra (CERES, MISR, MODIS, ASTER, MOPITT)
   <li>Meteor-3M (SAGE III)
   <li>Aqua (AIRS, AMSU-A, AMSR-E, CERES, MODIS)
   <li>Aura(MLS, TES, HIRDLS, OMI
 </ul>
 HDF is used by other EOS missions
   <ul>
   <li>OrbView 2 (SeaWIFS)
   <li>TRMM (CERES, VIRS, TMI, PR)
   <li>Quickscat (SeaWinds)
   <li>EO-1 (Hyperion, ALI)
   <li>ICESat (GLAS)
   <li>Calypso
   </ul>
 * </pre>
 * </p>
 *
 * @author caron
 * @since Jul 23, 2007
 */
public class HdfEos {
  static public final String HDF5_GROUP = "HDFEOS_INFORMATION";
  static public final String HDFEOS_CRS = "_HDFEOS_CRS";
  static public final String HDFEOS_CRS_Projection = "Projection";
  static public final String HDFEOS_CRS_UpperLeft = "UpperLeftPointMtrs";
  static public final String HDFEOS_CRS_LowerRight = "LowerRightMtrs";
  static public final String HDFEOS_CRS_ProjParams = "ProjParams";
  static public final String HDFEOS_CRS_SphereCode = "SphereCode";

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HdfEos.class);
  static boolean showWork = false; // set in debug
  static private final String GEOLOC_FIELDS = "Geolocation Fields";
  static private final String GEOLOC_FIELDS2 = "Geolocation_Fields";
  static private final String DATA_FIELDS = "Data Fields";
  static private final String DATA_FIELDS2 = "Data_Fields";

  /**
   * Amend the given NetcdfFile with metadata from HDF-EOS structMetadata.
   * All Variables named StructMetadata.n, where n= 1, 2, 3 ... are read in and their contents concatenated
   * to make the structMetadata String.
   *
   * @param ncfile Amend this file
   * @param eosGroup the group containing variables named StructMetadata.*
   * @throws IOException on read error
   * @return true if HDF-EOS info was found
   */
  static public boolean amendFromODL(NetcdfFile ncfile, Group eosGroup) throws IOException {
    String smeta = getStructMetadata(eosGroup);
    if (smeta == null) { return false; }

    HdfEos fixer = new HdfEos();
    fixer.fixAttributes(ncfile.getRootGroup());
    fixer.amendFromODL(ncfile, smeta);
    return true;
  }

    /**
     *
     */
  static public boolean getEosInfo(NetcdfFile ncfile, Group eosGroup, Formatter f) throws IOException {
    String smeta = getStructMetadata(eosGroup);
    if (smeta == null) {
      f.format("No StructMetadata variables in group %s %n", eosGroup.getFullName());
      return false;
    }
    f.format("raw = %n%s%n", smeta);
    ODLparser parser = new ODLparser();
    parser.parseFromString(smeta); // now we have the ODL in JDOM elements
    StringWriter sw = new StringWriter(5000);
    parser.showDoc(new PrintWriter(sw));
    f.format("parsed = %n%s%n", sw.toString());
    return true;
  }

    /**
     *
     */
  static private String getStructMetadata(Group eosGroup) throws IOException {
    StringBuilder sbuff = null;
    String structMetadata = null;

    int n = 0;
    while (true) {
      Variable structMetadataVar = eosGroup.findVariable("StructMetadata." + n);
      if (structMetadataVar == null) { break; }
      if ((structMetadata != null) && (sbuff == null)) { // more than 1 StructMetadata
        sbuff = new StringBuilder(64000);
        sbuff.append(structMetadata);
      }

      // read and parse the ODL
      Array A = structMetadataVar.read();
      if (A instanceof ArrayChar.D1) {
        ArrayChar ca = (ArrayChar) A;
        structMetadata = ca.getString(); // common case only StructMetadata.0, avoid extra copy
      } else if (A instanceof ArrayObject.D0) {
        ArrayObject ao = (ArrayObject) A;
        structMetadata = (String) ao.getObject(0);
      } else {
        log.error("Unsupported array type {} for StructMetadata", A.getElementType());
      }

      if (sbuff != null) {
        sbuff.append(structMetadata);
      }
      n++;
    }
    return (sbuff != null) ? sbuff.toString() : structMetadata;
  }

    /**
     * Amend the given NetcdfFile with metadata from HDF-EOS structMetadata
     *
     * @param ncfile Amend this file
     * @param structMetadata  structMetadata as String
     * @throws IOException on read error
     */
    private void amendFromODL(NetcdfFile ncfile, String structMetadata) throws IOException {
        Group rootg = ncfile.getRootGroup();

        ODLparser parser = new ODLparser();
        Element root = parser.parseFromString(structMetadata); // now we have the ODL in JDOM elements
        FeatureType featureType = null;

        // SWATH
        Element swathStructure = root.getChild("SwathStructure");
        if (swathStructure != null) {
            List<Element> swaths = swathStructure.getChildren();
            for (Element elemSwath : swaths) {
                Element swathNameElem = elemSwath.getChild("SwathName");
                if (swathNameElem == null) {
                    log.warn("No SwathName element in {} {} ", elemSwath.getName(), ncfile.getLocation());
                    continue;
                }
                String swathName = NetcdfFile.makeValidCdmObjectName(swathNameElem.getText().trim());
                Group swathGroup = findGroupNested(rootg, swathName);
                //if (swathGroup == null)
                //  swathGroup = findGroupNested(rootg, H4header.createValidObjectName(swathName));

                if (swathGroup != null) {
                    featureType = amendSwath(ncfile, elemSwath, swathGroup);
                } else {
                    log.warn("Cant find swath group {} {}", swathName, ncfile.getLocation());
                }
            }
        }

        // GRID
        Element gridStructure = root.getChild("GridStructure");
        if (gridStructure != null) {
            List<Element> grids = gridStructure.getChildren();
            for (Element elemGrid : grids) {
                Element gridNameElem = elemGrid.getChild("GridName");
                if (gridNameElem == null) {
                    log.warn("No GridName element in {} {} ", elemGrid.getName(), ncfile.getLocation());
                    continue;
                }
                String gridName = NetcdfFile.makeValidCdmObjectName(gridNameElem.getText().trim());
                Group gridGroup = findGroupNested(rootg, gridName);
                //if (gridGroup == null)
                //  gridGroup = findGroupNested(rootg, H4header.createValidObjectName(gridName));
                if (gridGroup != null) {
                    featureType = amendGrid(elemGrid, ncfile, gridGroup, ncfile.getLocation());
                } else {
                    log.warn("Cant find Grid group {} {}", gridName, ncfile.getLocation());
                }
            }
        }

        // POINT - NOT DONE YET
        Element pointStructure = root.getChild("PointStructure");
        if (pointStructure != null) {
            List<Element> pts = pointStructure.getChildren();
            for (Element elem : pts) {
                Element nameElem = elem.getChild("PointName");
                if (nameElem == null) {
                    log.warn("No PointName element in {} {}", elem.getName(), ncfile.getLocation());
                    continue;
                }
                String name = nameElem.getText().trim();
                Group ptGroup = findGroupNested(rootg, name);
                //if (ptGroup == null)
                //  ptGroup = findGroupNested(rootg, H4header.createValidObjectName(name));
                if (ptGroup != null) {
                    featureType = FeatureType.POINT;
                } else {
                    log.warn("Cant find Point group {} {}", name, ncfile.getLocation());
                }
            }
        }

        if (featureType != null) {
            if (showWork) { log.debug("***EOS featureType= {}", featureType.toString()); }
            rootg.addAttribute(new Attribute(CF.FEATURE_TYPE, featureType.toString()));
            // rootg.addAttribute(new Attribute(CDM.CONVENTIONS, "HDFEOS"));
        }
    }

    /**
     *
     */
    private FeatureType amendSwath(NetcdfFile ncfile, Element swathElem, Group parent) {
        FeatureType featureType = FeatureType.SWATH;
        List<Dimension> unknownDims = new ArrayList<>();

        // Dimensions
        Element d = swathElem.getChild("Dimension");
        List<Element> dims = d.getChildren();
        for (Element elem : dims) {
            String name = elem.getChild("DimensionName").getText().trim();
            name = NetcdfFile.makeValidCdmObjectName(name);

            if (name.equalsIgnoreCase("scalar")) {
                continue;
            }
            String sizeS = elem.getChild("Size").getText().trim();
            int length = Integer.parseInt(sizeS);
            if (length > 0) {
                Dimension dim = parent.findDimensionLocal(name);
                if (dim != null) {                 // already added - may be dimension scale ?
                    if (dim.getLength() != length) { // ok as long as it matches
                        log.error("Conflicting Dimensions = {} {}", dim, ncfile.getLocation());
                        throw new IllegalStateException("Conflicting Dimensions = "+name);
                    }
                } else {
                    dim = new Dimension(name, length);
                    if (parent.addDimensionIfNotExists(dim) && showWork) {
                        log.debug(" Add dimension {}",dim);
                    }
                }
            } else {
                log.warn("Dimension {} has size {} {}", name, sizeS, ncfile.getLocation());
                Dimension udim = new Dimension(name, 1);
                udim.setGroup(parent);
                unknownDims.add( udim);
                if (showWork) {
                    log.debug(" Add dimension {}", udim);
                }
            }
        }

        // Dimension Maps
        Element dmap = swathElem.getChild("DimensionMap");
        List<Element> dimMaps = dmap.getChildren();
        for (Element elem : dimMaps) {
            String geoDimName = elem.getChild("GeoDimension").getText().trim();
            geoDimName = NetcdfFile.makeValidCdmObjectName(geoDimName);
            String dataDimName = elem.getChild("DataDimension").getText().trim();
            dataDimName = NetcdfFile.makeValidCdmObjectName(dataDimName);

            String offsetS = elem.getChild("Offset").getText().trim();
            String incrS = elem.getChild("Increment").getText().trim();
            int offset = Integer.parseInt(offsetS);
            int incr = Integer.parseInt(incrS);

            // make new variable for this dimension map
            Variable v = new Variable(ncfile, parent, null, dataDimName);
            v.setDimensions(geoDimName);
            v.setDataType(DataType.INT);
            int npts = (int) v.getSize();
            Array data = Array.makeArray(v.getDataType(), npts, offset, incr);
            v.setCachedData(data, true);
            v.addAttribute(new Attribute("_DimensionMap", ""));
            parent.addVariable(v);
            if (showWork) {
                log.debug(" Add dimensionMap {}", v);
            }
        }

        // Geolocation Variables
        Group geoFieldsG = parent.findGroup(GEOLOC_FIELDS);
        if (geoFieldsG == null) {
            geoFieldsG = parent.findGroup(GEOLOC_FIELDS2);
        }
        if (geoFieldsG != null) {
            Variable latAxis = null, lonAxis = null, timeAxis = null;
            Element floc = swathElem.getChild("GeoField");
            List<Element> varsLoc = floc.getChildren();
            for (Element elem : varsLoc) {
                String varname = elem.getChild("GeoFieldName").getText().trim();
                Variable v = geoFieldsG.findVariable(varname);
                //if (v == null)
                //  v = geoFieldsG.findVariable( H4header.createValidObjectName(varname));
                assert v != null : varname;
                AxisType axis = addAxisType(ncfile, v);
                if (axis == AxisType.Lat)  { latAxis = v; }
                if (axis == AxisType.Lon)  { lonAxis = v; }
                if (axis == AxisType.Time) { timeAxis = v; }

                Element dimList = elem.getChild("DimList");
                List<Element> values = dimList.getChildren("value");
                setSharedDimensions( v, values, unknownDims, ncfile.getLocation());
                if (showWork) {
                    log.debug(" set coordinate {}", v);
                }
            }

            // Treat possibility that this is a discrete geometry featureType.
            // We check if lat and lon axes are 2D and if not (1) see if it looks like a
            // trajectory, or (2) otherwise tag it as a profile.
            // This could/should be expanded to consider other FTs.
            if ((latAxis != null) && (lonAxis != null)) {
                log.debug ("found lonAxis and latAxis -- testing XY domain");
                List<Dimension> xyDomain = CoordinateSystem.makeDomain(new Variable[] {latAxis, lonAxis});
                log.debug ("xyDomain size {}", xyDomain.size());
                if (xyDomain.size() < 2) {
                    if (timeAxis != null)
                    {
                        log.debug ("found timeAxis -- testing if trajectory");
                        Dimension dd1 = timeAxis.getDimension(0);
                        Dimension dd2 = latAxis.getDimension(0);
                        Dimension dd3 = lonAxis.getDimension(0);
                   
                        if (dd1.equals (dd2) && dd1.equals(dd3)) {
                            featureType = FeatureType.TRAJECTORY;
                        } else {
                            featureType = FeatureType.PROFILE;  // ??
                        }
                    } else {
                        featureType = FeatureType.PROFILE;  // ??
                    }
                }
            }
        }

        // Data Variables
        Group dataG = parent.findGroup(DATA_FIELDS);
        if (dataG == null) {
            dataG = parent.findGroup(DATA_FIELDS2);
        }
        if (dataG != null) {
            Element f = swathElem.getChild("DataField");
            List<Element> vars = f.getChildren();
            for (Element elem : vars) {
                Element dataFieldNameElem = elem.getChild("DataFieldName");
                if (dataFieldNameElem == null) {
                    continue;
                }
                String varname = NetcdfFile.makeValidCdmObjectName(dataFieldNameElem.getText().trim());
                Variable v = dataG.findVariable(varname);
                //if (v == null)
                //  v = dataG.findVariable( H4header.createValidObjectName(varname));
                if (v == null) {
                    log.error("Cant find variable {} {}", varname, ncfile.getLocation());
                    continue;
                }

                Element dimList = elem.getChild("DimList");
                List<Element> values = dimList.getChildren("value");
                setSharedDimensions( v, values, unknownDims, ncfile.getLocation());
            }
        }

        return featureType;
    }

    /**
     *
     */
  private AxisType addAxisType(NetcdfFile ncfile, Variable v) {
    String name = v.getShortName();
    if (name.equalsIgnoreCase("Latitude") || name.equalsIgnoreCase("GeodeticLatitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
      v.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      return AxisType.Lat;

    } else if (name.equalsIgnoreCase("Longitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
      v.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      return AxisType.Lon;

    } else if (name.equalsIgnoreCase("Time")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
      if (v.findAttribute(CDM.UNITS) == null) {
        /*
        from http://newsroom.gsfc.nasa.gov/sdptoolkit/hdfeosfaq.html
        HDF-EOS uses the TAI93 (International Atomic Time) format. This means that time is stored as the number of
        elapsed seconds since January 1, 1993 (negative values represent times prior to this date).
        An 8 byte floating point number is used, producing microsecond accuracy from 1963 (when leap second records
        became available electronically) to 2100. The SDP Toolkit provides conversions from other date formats to and
        from TAI93. Other representations of time can be entered as ancillary data, if desired.
        For lists and descriptions of other supported time formats, consult the Toolkit documentation or write to
        landover_PGSTLKIT@raytheon.com.
         */
        v.addAttribute(new Attribute(CDM.UNITS, "seconds since 1993-01-01T00:00:00Z"));
        v.addAttribute(new Attribute(CF.CALENDAR, "TAI"));
        /* String tit = ncfile.findAttValueIgnoreCase(v, "Title", null);
        if (tit != null && tit.contains("TAI93")) {
          // Time is given in the TAI-93 format, i.e. the number of seconds passed since 01-01-1993, 00:00 UTC.
          v.addAttribute(new Attribute(CDM.UNITS, "seconds since 1993-01-01T00:00:00Z"));
          v.addAttribute(new Attribute(CF.CALENDAR, "TAI"));
        } else { // who the hell knows ??
          v.addAttribute(new Attribute(CDM.UNITS, "seconds since 1970-01-01T00:00:00Z"));
        }  */
      }
      return AxisType.Time;

    } else if (name.equalsIgnoreCase("Pressure")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Pressure.toString()));
      return AxisType.Pressure;

    } else if (name.equalsIgnoreCase("Altitude")) {
      v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
      v.addAttribute(new Attribute(CF.POSITIVE, CF.POSITIVE_UP)); // probably
      return AxisType.Height;
     }

    return null;
  }


  private FeatureType amendGrid(Element gridElem, NetcdfFile ncfile, Group parent, String location) {
    List<Dimension> unknownDims = new ArrayList<>();

    // always has x and y dimension
    String xdimSizeS = gridElem.getChild("XDim").getText().trim();
    String ydimSizeS = gridElem.getChild("YDim").getText().trim();
    int xdimSize = Integer.parseInt(xdimSizeS);
    int ydimSize = Integer.parseInt(ydimSizeS);
    parent.addDimensionIfNotExists(new Dimension("XDim", xdimSize));
    parent.addDimensionIfNotExists(new Dimension("YDim", ydimSize));

    /* see HdfEosModisConvention
    UpperLeftPointMtrs=(-20015109.354000,1111950.519667)
    		LowerRightMtrs=(-18903158.834333,-0.000000)
    		Projection=GCTP_SNSOID
    		ProjParams=(6371007.181000,0,0,0,0,0,0,0,0,0,0,0,0)
    		SphereCode=-1
     */
    Element proj = gridElem.getChild("Projection");
    if (proj != null) {
      Variable crs = new Variable(ncfile, parent, null, HDFEOS_CRS);
      crs.setDataType(DataType.SHORT);
      crs.setDimensions(""); // scalar
      crs.setCachedData(Array.makeArray(DataType.SHORT, 1, 0, 0)); // fake data
      parent.addVariable(crs);

      addAttributeIfExists(gridElem, HDFEOS_CRS_Projection, crs, false);
      addAttributeIfExists(gridElem, HDFEOS_CRS_UpperLeft, crs, true);
      addAttributeIfExists(gridElem, HDFEOS_CRS_LowerRight, crs, true);
      addAttributeIfExists(gridElem, HDFEOS_CRS_ProjParams, crs, true);
      addAttributeIfExists(gridElem, HDFEOS_CRS_SphereCode, crs, false);
    }

    // global Dimensions
    Element d = gridElem.getChild("Dimension");
    List<Element> dims = d.getChildren();
    for (Element elem : dims) {
      String name = elem.getChild("DimensionName").getText().trim();
      name = NetcdfFile.makeValidCdmObjectName(name);
      if (name.equalsIgnoreCase("scalar")) {
        continue;
      }

      String sizeS = elem.getChild("Size").getText().trim();
      int length = Integer.parseInt(sizeS);
      Dimension old = parent.findDimension(name);
      if ((old == null) || (old.getLength() != length)) {
        if (length > 0) {
          Dimension dim = new Dimension(name, length);
          if (parent.addDimensionIfNotExists(dim) && showWork) {
            log.debug(" Add dimension {}", dim);
          }
        } else {
          log.warn("Dimension {} has size {} {} ", sizeS, name, location);
          Dimension udim = new Dimension(name, 1);
          udim.setGroup(parent);
          unknownDims.add( udim);
          if (showWork) { log.debug(" Add dimension {}", udim); }
        }
      }
    }

    // Geolocation Variables
    Group geoFieldsG = parent.findGroup(GEOLOC_FIELDS);
    if (geoFieldsG == null) {
        geoFieldsG = parent.findGroup(GEOLOC_FIELDS2);
    }
    if (geoFieldsG != null) {
      Element floc = gridElem.getChild("GeoField");
      List<Element> varsLoc = floc.getChildren();
      for (Element elem : varsLoc) {
        String varname = elem.getChild("GeoFieldName").getText().trim();
        Variable v = geoFieldsG.findVariable(varname);
        //if (v == null)
        //  v = geoFieldsG.findVariable( H4header.createValidObjectName(varname));
        assert v != null : varname;

        Element dimList = elem.getChild("DimList");
        List<Element> values = dimList.getChildren("value");
        setSharedDimensions( v, values, unknownDims, location);
      }
    }

    // Data Variables
    Group dataG = parent.findGroup(DATA_FIELDS);
    if (dataG == null) {
        dataG = parent.findGroup(DATA_FIELDS2);  // eg C:\data\formats\hdf4\eos\mopitt\MOP03M-200501-L3V81.0.1.hdf
    }
    if (dataG != null) {
      Element f = gridElem.getChild("DataField");
      List<Element> vars = f.getChildren();
      for (Element elem : vars) {
        String varname = elem.getChild("DataFieldName").getText().trim();
        varname = NetcdfFile.makeValidCdmObjectName( varname);
        Variable v = dataG.findVariable(varname);
        //if (v == null)
        //  v = dataG.findVariable( H4header.createValidObjectName(varname));
        assert v != null : varname;

        Element dimList = elem.getChild("DimList");
        List<Element> values = dimList.getChildren("value");
        setSharedDimensions( v, values, unknownDims, location);
      }

      // get projection
      String projS = null;
      Element projElem = gridElem.getChild("Projection");
      if (projElem != null) {
        projS = projElem.getText().trim();
      }
      boolean isLatLon = "GCTP_GEO".equals(projS);

      // look for XDim, YDim coordinate variables
      if (isLatLon) {
        for (Variable v : dataG.getVariables()) {
          if (v.isCoordinateVariable()) {
            if (v.getShortName().equals("YDim")) {
              v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
              v.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
            }
            if (v.getShortName().equals("XDim")) {
              v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
            }
          }
        }
      }
  }
    return FeatureType.GRID;
  }

  private void addAttributeIfExists(Element elem, String name, Variable v, boolean isDoubleArray) {
    Element child = elem.getChild(name);
    if (child == null) { return; }
    if (isDoubleArray) {
      List<Element> vElems = child.getChildren();
      List<Double> values = new ArrayList<>();
      for (Element ve : vElems) {
        String valueS = ve.getText().trim();
        try {
          values.add(Double.parseDouble(valueS));
        } catch (NumberFormatException e) {  }
        }
      Attribute att = new Attribute(name, values);
      v.addAttribute(att);
    } else {
      String value = child.getText().trim();
      Attribute att = new Attribute(name, value);
      v.addAttribute(att);
    }
  }

  // convert to shared dimensions
  private void setSharedDimensions(Variable v, List<Element> values, List<Dimension> unknownDims, String location) {
    if (values.size() == 0) {
      return;
    }

    // remove the "scalar" dumbension
    Iterator<Element> iter = values.iterator();
    while (iter.hasNext()) {
      Element value = iter.next();
      String dimName = value.getText().trim();
      if (dimName.equalsIgnoreCase("scalar")) {
        iter.remove();
      }
    }

    // gotta have same number of dimensions
    List<Dimension> oldDims = v.getDimensions();
    if (oldDims.size() != values.size()) {
      log.error("Different number of dimensions for {} {}", v, location);
      return;
    }

    List<Dimension> newDims = new ArrayList<>();
    Group group = v.getParentGroup();

    for (int i=0; i<values.size(); i++) {
      Element value = values.get(i);
      String dimName = value.getText().trim();
      dimName = NetcdfFile.makeValidCdmObjectName( dimName);

      Dimension dim = group.findDimension(dimName);
      Dimension oldDim = oldDims.get(i);
      if (dim == null) {
        dim = checkUnknownDims(dimName, unknownDims, oldDim, location);
      }

      if (dim == null) {
        log.error("Unknown Dimension= {} for variable = {} {} ", dimName, v.getFullName(), location);
        return;
      }
      if (dim.getLength() != oldDim.getLength()) {
        log.error("Shared dimension ({}) has different length than data dimension ({}) shared={} org={} for {} {}",
                    dim.getShortName(), oldDim.getShortName(),
                    dim.getLength(), oldDim.getLength(), v, location);
        return;
      }
      newDims.add(dim);
    }
    v.setDimensions(newDims);
    if (showWork) { log.debug(" set shared dimensions for {}", v.getNameAndDimensions()); }
  }

  // look if the wanted dimension is in the  unknownDims list.
  private Dimension checkUnknownDims(String wantDim, List<Dimension> unknownDims, Dimension oldDim, String location) {
    for (Dimension dim : unknownDims) {
      if (dim.getShortName().equals(wantDim)) {
        int len = oldDim.getLength();
        if (len == 0) {
          dim.setUnlimited( true); // allow zero length dimension !!
        }
        dim.setLength(len); // use existing (anon) dimension
        Group parent = dim.getGroup();
        parent.addDimensionIfNotExists(dim);  // add to the parent
        unknownDims.remove(dim); // remove from list LOOK is this ok?
        log.warn("unknownDim {} length set to {}{}", wantDim, oldDim.getLength(), location);
        return dim;
      }
    }
    return null;
  }

  // look for a group with the given name. recurse into subgroups if needed. breadth first
  private Group findGroupNested(Group parent, String name) {

    for (Group g : parent.getGroups()) {
      if (g.getShortName().equals(name)) { return g; }
    }
    for (Group g : parent.getGroups()) {
      Group result = findGroupNested(g, name);
      if (result != null) { return result; }
    }
    return null;
  }

  private void fixAttributes(Group g) {
    for (Variable v : g.getVariables()) {
      for (Attribute a : v.getAttributes()) {
        if (a.getShortName().equalsIgnoreCase("UNIT") || a.getShortName().equalsIgnoreCase("UNITS")) {
          a.setShortName(CDM.UNITS);
        }
        if (a.getShortName().equalsIgnoreCase("SCALE_FACTOR")) {
          a.setShortName(CDM.SCALE_FACTOR);
        }
        if (a.getShortName().equalsIgnoreCase("OFFSET")) {
          a.setShortName(CDM.ADD_OFFSET);
        }
      }
    }

    for (Group ng : g.getGroups()) {
      fixAttributes(ng);
    }

  }

}
