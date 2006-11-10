// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.util;

  import org.jdom.*;
  import org.jdom.input.*;

  import java.io.IOException;
  import java.io.InputStream;
  import java.util.List;

  import thredds.catalog.DataType;

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
 * @version $Revision$ $Date$
 */
public class RuntimeConfigParser {

    public static void read(InputStream is, StringBuffer errlog) throws IOException {

      Document doc;
      SAXBuilder saxBuilder = new SAXBuilder();
      try {
        doc = saxBuilder.build(is);
      } catch (JDOMException e) {
        throw new IOException(e.getMessage());
      }

      read( doc.getRootElement(), errlog);
    }

    public static void read(org.jdom.Element root, StringBuffer errlog) {

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
          DataType datatype = DataType.getType(typeName);
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
        }
      }
    }

  }

