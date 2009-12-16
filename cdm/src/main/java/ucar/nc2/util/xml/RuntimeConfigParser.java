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

package ucar.nc2.util.xml;

import org.jdom.*;
import org.jdom.input.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ucar.nc2.constants.FeatureType;

/**
 * Read Runtime Configuration
 *
 * <pre>
   <runtimeConfig>
     <ioServiceProvider  class="edu.univ.ny.stuff.FooFiles"/>
     <coordSysBuilder convention="foo" class="test.Foo"/>
     <coordTransBuilder name="atmos_ln_sigma_coordinates" type="vertical" class="my.stuff.atmosSigmaLog"/>
     <typedDatasetFactory datatype="Point" class="gov.noaa.obscure.file.Flabulate"/>
   </runtimeConfig>
 * </pre>
 *
 * @author caron
 */
public class RuntimeConfigParser {

    public static void read(InputStream is, StringBuilder errlog) throws IOException {

      Document doc;
      SAXBuilder saxBuilder = new SAXBuilder();
      try {
        doc = saxBuilder.build(is);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }

      read( doc.getRootElement(), errlog);
    }

    public static void read(org.jdom.Element root, StringBuilder errlog) {

      List children = root.getChildren();
      for (int i = 0; i < children.size(); i++) {
        Element elem =  (Element) children.get(i);
        if (elem.getName().equals("ioServiceProvider")) {
          String className = elem.getAttributeValue("class");

          try {
            ucar.nc2.NetcdfFile.registerIOProvider( className);
          } catch (ClassNotFoundException e) {
            errlog.append("CoordSysBuilder class not found= "+className+"; check your classpath\n");
          } catch (Exception e) {
            errlog.append("IOServiceProvider "+className+" failed= "+e.getMessage()+"\n");
          }

        } else if (elem.getName().equals("coordSysBuilder")) {
          String conventionName = elem.getAttributeValue("convention");
          String className = elem.getAttributeValue("class");

          try {
            ucar.nc2.dataset.CoordSysBuilder.registerConvention( conventionName, className);
          } catch (ClassNotFoundException e) {
            errlog.append("CoordSysBuilder class not found= "+className+"; check your classpath\n");
          } catch (Exception e) {
            errlog.append("CoordSysBuilder "+className+" failed= "+e.getMessage()+"\n");
          }

        } else if (elem.getName().equals("coordTransBuilder")) {
          String transformName = elem.getAttributeValue("name");
          String className = elem.getAttributeValue("class");

          try {
            ucar.nc2.dataset.CoordTransBuilder.registerTransform( transformName, className);
          } catch (ClassNotFoundException e) {
            errlog.append("CoordSysBuilder class not found= "+className+"; check your classpath\n");
          } catch (Exception e) {
            errlog.append("CoordTransBuilder "+className+" failed= "+e.getMessage()+"\n");
          }

        } else if (elem.getName().equals("typedDatasetFactory")) {
          String typeName = elem.getAttributeValue("datatype");
          String className = elem.getAttributeValue("class");
          FeatureType datatype = FeatureType.valueOf(typeName.toUpperCase());
          if (null == datatype) {
            errlog.append("TypedDatasetFactory "+className+" unknown datatype= "+typeName+"\n");
            continue;
          }

          try {
            ucar.nc2.dt.TypedDatasetFactory.registerFactory( datatype, className);
          } catch (ClassNotFoundException e) {
            errlog.append("CoordSysBuilder class not found= "+className+"; check your classpath\n");
          } catch (Exception e) {
            errlog.append("TypedDatasetFactory "+className+" failed= "+e.getMessage()+"\n");
          }

        } else if (elem.getName().equals("table")) {
          String type = elem.getAttributeValue("type");
          String filename = elem.getAttributeValue("filename");
          if ((type == null) || (filename == null)) {
            errlog.append("table element must have both type and filename attributes\n");
            continue;
          }

          try {
            if (type.equalsIgnoreCase("GRIB1")) { // LOOK - do this with reflection
              ucar.grib.grib1.GribPDSParamTable.addParameterUserLookup( filename);

            } else if (type.equalsIgnoreCase("GRIB2")) {
              ucar.grib.grib2.ParameterTable.addParametersUser( filename);

            } else {
              errlog.append("Unknown table type "+type+"\n");
              continue;
            }

          } catch (Exception e) {
            errlog.append("table read failed on  "+filename+" = "+e.getMessage()+"\n");
          }

        } else if (elem.getName().equals("bufrtable")) {
          String filename = elem.getAttributeValue("filename");
          try {
            ucar.nc2.iosp.bufr.tables.BufrTables.addLookupFile( filename);
          } catch (Exception e) {
            errlog.append("bufrtable read failed on  "+filename+" = "+e.getMessage()+"\n");
          }
        }

      }
    }

  }

