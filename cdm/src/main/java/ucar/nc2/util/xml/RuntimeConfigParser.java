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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Read Runtime Configuration
 * <p/>
 * <pre>
 * <runtimeConfig>
 * <ioServiceProvider  class="edu.univ.ny.stuff.FooFiles"/>
 * <coordSysBuilder convention="foo" class="test.Foo"/>
 * <coordTransBuilder name="atmos_ln_sigma_coordinates" type="vertical" class="my.stuff.atmosSigmaLog"/>
 * <typedDatasetFactory datatype="Point" class="gov.noaa.obscure.file.Flabulate"/>
 * </runtimeConfig>
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

    read(doc.getRootElement(), errlog);
  }

  public static void read(org.jdom2.Element root, StringBuilder errlog) {

    List children = root.getChildren();
    for (int i = 0; i < children.size(); i++) {
      Element elem = (Element) children.get(i);
      if (elem.getName().equals("ioServiceProvider")) {
        String className = elem.getAttributeValue("class");

        try {
          ucar.nc2.NetcdfFile.registerIOProvider(className);
        } catch (ClassNotFoundException e) {
          errlog.append("CoordSysBuilder class not found= " + className + "; check your classpath\n");
        } catch (Exception e) {
          errlog.append("IOServiceProvider " + className + " failed= " + e.getMessage() + "\n");
        }

      } else if (elem.getName().equals("coordSysBuilder")) {
        String conventionName = elem.getAttributeValue("convention");
        String className = elem.getAttributeValue("class");

        try {
          ucar.nc2.dataset.CoordSysBuilder.registerConvention(conventionName, className);
        } catch (ClassNotFoundException e) {
          errlog.append("CoordSysBuilder class not found= " + className + "; check your classpath\n");
        } catch (Exception e) {
          errlog.append("CoordSysBuilder " + className + " failed= " + e.getMessage() + "\n");
        }

      } else if (elem.getName().equals("coordTransBuilder")) {
        String transformName = elem.getAttributeValue("name");
        String className = elem.getAttributeValue("class");

        try {
          ucar.nc2.dataset.CoordTransBuilder.registerTransform(transformName, className);
        } catch (ClassNotFoundException e) {
          errlog.append("CoordSysBuilder class not found= " + className + "; check your classpath\n");
        } catch (Exception e) {
          errlog.append("CoordTransBuilder " + className + " failed= " + e.getMessage() + "\n");
        }

      } else if (elem.getName().equals("typedDatasetFactory")) {
        String typeName = elem.getAttributeValue("datatype");
        String className = elem.getAttributeValue("class");
        FeatureType datatype = FeatureType.valueOf(typeName.toUpperCase());
        if (null == datatype) {
          errlog.append("TypedDatasetFactory " + className + " unknown datatype= " + typeName + "\n");
          continue;
        }

        try {
          ucar.nc2.dt.TypedDatasetFactory.registerFactory(datatype, className);
        } catch (ClassNotFoundException e) {
          errlog.append("CoordSysBuilder class not found= " + className + "; check your classpath\n");
        } catch (Exception e) {
          errlog.append("TypedDatasetFactory " + className + " failed= " + e.getMessage() + "\n");
        }

      } else if (elem.getName().equals("gribParameterTable")) {
        String editionS = elem.getAttributeValue("edition");
        String centerS = elem.getAttributeValue("center");
        String subcenterS = elem.getAttributeValue("subcenter");
        String versionS = elem.getAttributeValue("version");
        String filename = elem.getText();
        if ((centerS == null) || (versionS == null) || (filename == null)) {
          errlog.append("table element must center, version and filename attributes\n");
          continue;
        }

        // Grib1ParamTables.addParameterTable(int center, int subcenter, int tableVersion, String filename) {
        // use reflection to decouple from the grib package
        try {
          int center = Integer.parseInt(centerS);
          int subcenter = (subcenterS == null) ? -1 : Integer.parseInt(subcenterS);
          int version = Integer.parseInt(versionS);

          // ucar.nc2.grib.grib1.tables.Grib1ParamTables.addParameterTable(int center, int subcenter, int tableVersion, String tableFilename)
          Class c = RuntimeConfigParser.class.getClassLoader().loadClass("ucar.nc2.grib.grib1.tables.Grib1ParamTables");
          Method m = c.getMethod("addParameterTable", int.class, int.class, int.class, String.class);
          m.invoke(null, center, subcenter, version, filename);

        } catch (Exception e) {
          e.printStackTrace();
        }

        String strictS = elem.getAttributeValue("strict");
        if (strictS != null) {
          boolean notStrict = strictS.equalsIgnoreCase("false");
          try {
            Class c = RuntimeConfigParser.class.getClassLoader().loadClass("ucar.grib.grib1.tables.Grib1ParamTables");
            Method m = c.getMethod("setStrict", boolean.class);
            m.invoke(null, !notStrict);

          } catch (Exception e) {
            e.printStackTrace();
          }

          continue;
        }

      } else if (elem.getName().equals("gribParameterTableLookup")) {

        String editionS = elem.getAttributeValue("edition");
        String filename = elem.getText();

        // ucar.nc2.grib.grib1.tables.Grib1ParamTables.addParameterTableLookup(String lookupFilename)
        try {
          Class c = RuntimeConfigParser.class.getClassLoader().loadClass("ucar.nc2.grib.grib1.tables.Grib1ParamTables");
          Method m = c.getMethod("addParameterTableLookup", String.class);
          m.invoke(null, filename);

        } catch (Exception e) {
          e.printStackTrace();
        }


      } else if (elem.getName().equals("table")) {
        String type = elem.getAttributeValue("type");
        String filename = elem.getAttributeValue("filename");
        if ((type == null) || (filename == null)) {
          errlog.append("table element must have both type and filename attributes\n");
          continue;
        }

        try {
          if (type.equalsIgnoreCase("GRIB1")) {
            // ucar.grib.grib1.GribPDSParamTable.addParameterUserLookup( filename);
            // use reflection instead to decouple from the grib package
            try {
              Class c = RuntimeConfigParser.class.getClassLoader().loadClass("ucar.grib.grib1.GribPDSParamTable");
              Method m = c.getMethod("addParameterUserLookup", String.class);
              m.invoke(null, filename);

            } catch (ClassNotFoundException e) {
              e.printStackTrace();
            } catch (NoSuchMethodException e) {
              e.printStackTrace();
            } catch (InvocationTargetException e) {
              e.printStackTrace();
            } catch (IllegalAccessException e) {
              e.printStackTrace();
            }

          } else if (type.equalsIgnoreCase("GRIB2")) {
            // ucar.grib.grib2.ParameterTable.addParametersUser( filename);
            try {
              Class c = RuntimeConfigParser.class.getClassLoader().loadClass(" ucar.grib.grib2.ParameterTable");
              Method m = c.getMethod("addParametersUser", String.class);
              m.invoke(null, filename);

            } catch (Exception e) {
              e.printStackTrace();
            }

          } else {
            errlog.append("Unknown table type " + type + "\n");
            continue;
          }

        } catch (Exception e) {
          errlog.append("table read failed on  " + filename + " = " + e.getMessage() + "\n");
        }

      } else if (elem.getName().equals("bufrtable")) {

        String filename = elem.getAttributeValue("filename");
        if (filename == null) {
          errlog.append("bufrtable must have filename attribute\n");
          continue;
        }

        // reflection is used to decouple optional jars
        Class bufrTablesClass;
        try {
          bufrTablesClass = RuntimeConfigParser.class.getClassLoader().loadClass("ucar.nc2.iosp.bufr.tables.BufrTables"); // only load if bufr.jar is present
          java.lang.Class[] params = new Class[1];
          params[0] = String.class;
          Method method = bufrTablesClass.getMethod("addLookupFile", params);
          Object[] args = new Object[1];
          args[0] = filename;
          method.invoke(null, args); // static method has null for object

        } catch (Throwable e) {
          if (e instanceof FileNotFoundException)
            errlog.append("bufrtable read failed on  " + filename + " = " + e.getMessage() + "\n");
          else
            errlog.append("bufr.jar is not on classpath\n");
        }

      } else if (elem.getName().equals("Netcdf4Clibrary")) {
        // cdm does not have a dependency on netcdf4 (and we don't want to introduce one),
        // so we cannot refer to the Nc4Iosp.class object.
        String nc4IospClassName = "ucar.nc2.jni.netcdf.Nc4Iosp";

        /*
          <Netcdf4Clibrary>
            <libraryPath>/usr/local/lib</libraryPath>
            <libraryName>netcdf</libraryName>
            <useForReading>false</useForReading>
          </Netcdf4Clibrary>
        */
        String path = elem.getChildText("libraryPath");
        String name = elem.getChildText("libraryName");

        if (path != null && name != null) {
          // reflection is used to decouple optional jars
          try {
            Class nc4IospClass = RuntimeConfigParser.class.getClassLoader().loadClass(nc4IospClassName);
            Method method = nc4IospClass.getMethod("setLibraryAndPath", new Class[] {String.class, String.class});
            method.invoke(null, path, name); // static method has null for object
          } catch (Throwable e) {
              errlog.append(nc4IospClassName + " is not on classpath\n");
          }
        }

        boolean useForReading = Boolean.parseBoolean(elem.getChildText("useForReading"));
        if (useForReading) {
          try {
            // Registers Nc4Iosp in front of all the other IOSPs already registered in NetcdfFile.<clinit>().
            // Crucially, this means that we'll try to open a file with Nc4Iosp before we try it with H5iosp.
            NetcdfFile.registerIOProvider(nc4IospClassName);
          } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            errlog.append(String.format("Could not register IOSP '%s': %s%n", nc4IospClassName, e.getMessage()));
          }
        }
      }
    }
  }
}
