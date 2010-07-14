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

// $Id: ParameterTable.java,v 1.17 2005/12/13 22:58:47 rkambic Exp $


package ucar.grib.grib2;


import org.w3c.dom.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ucar.grib.Parameter;
import ucar.grid.GridParameter;

/**
 * Performs operations related to loading parameter tables stored in files.
 * Through a lookup table (see readParameterTableLookup) all of the supported
 * Parameter Tables are known.
 * A parameter consists of a discipline( ie Meteorological_products),
 * a Category( ie Temperature ) and a number that refers to a name( ie Temperature)
 * <p/>
 * see <a href="../../Parameters.txt">Parameters.txt</a>
 *
 * @author Robb Kambic /10/10/03
 */


public final class ParameterTable {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParameterTable.class);

  static private final Pattern valid = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_@:\\.\\-\\+]*$");
  static private final Pattern numberFirst = Pattern.compile("^[0-9]");
  static private final boolean debug = false;
  static private final ArrayList<Discipline> discipline;

  static {

    discipline = new ArrayList<Discipline>();
    DocumentBuilder parser;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);

    try {
      parser = factory.newDocumentBuilder();
      ClassLoader cl = ParameterTable.class.getClassLoader();
      InputStream is = cl.getResourceAsStream( "resources/grib/tables/grib2StdQuantities.xml");

      Document doc = parser.parse(is);
      NodeList d = doc.getElementsByTagName("discipline");
      for (int i = 0; i < d.getLength(); i++) {  // Discipline for
        Discipline dis = new Discipline();
        NamedNodeMap da = d.item(i).getAttributes();  // Dis attributes
        for (int j = 0; j < da.getLength(); j++) {
          if (da.item(j).getNodeName().equals("id")) {
            dis.setName(da.item(j).getNodeValue());
          } else if (da.item(j).getNodeName().equals("number")) {
            dis.setNumber( Integer.parseInt(da.item(j).getNodeValue()));
          }

        }
        if (debug)
          System.out.println( "discipline = " + dis.getName() +" "+ dis.getNumber());

        NodeList c = d.item(i).getChildNodes();  //Children of discipline
        for (int j = 0; j < c.getLength(); j++) {  // categories
          if (!c.item(j).getNodeName().equals("category")) {
            continue;  //kludge for #text nodes
          }
          Category cat = new Category();
          NamedNodeMap ca = c.item(j).getAttributes();
          for (int k = 0; k < ca.getLength(); k++) {
            if (ca.item(k).getNodeName().equals("id")) {
              cat.setName(ca.item(k).getNodeValue());
            } else if (ca.item(k).getNodeName().equals(
                "number")) {
              cat.setNumber(
                  Integer.parseInt(ca.item(k).getNodeValue()));
            }
          }
          if (debug)
            System.out.println( "\ncatalog = " + cat.getName() +" "+ cat.getNumber() );

          NodeList p = c.item(j).getChildNodes();  //Children of cat
          for (int m = 0; m < p.getLength(); m++) {
            if (!p.item(m).getNodeName().equals("quantity")) {
              continue;  //kludge for #text nodes
            }
            //System.out.println( "node=" + p.item(m).getNodeName());

            Parameter par = null;
            String pName = "";
            int pNumber = -1;
            String pUnit = "";
            String pDesc = "";
            NamedNodeMap pa = p.item(m).getAttributes();
            for (int n = 0; n < pa.getLength(); n++) {
              Node attr = pa.item(n);
              if (attr.getNodeName().equals("id")) {
                pName = attr.getNodeValue();
              } else if (attr.getNodeName().equals("number")) {
                pNumber = Integer.parseInt(attr.getNodeValue());
              } else if (attr.getNodeName().equals("unit")) {
                pUnit = attr.getNodeValue();
              } else if (attr.getNodeName().equals(
                  "description")) {
                pDesc = makeValidDesc(attr.getNodeValue());
              }
            }
            if (!(pName.startsWith("Reserved") ||  // add new par
                pName.startsWith("Missing"))) {
              par = new Parameter();
              par.setName(pName);
              par.setNumber(pNumber);
              par.setUnit(pUnit);
              par.setDescription(pDesc);
              cat.setParameter(par);  // set P into category
              if (debug ) {
                //System.out.print( "parameter= "+ dis.getName() +" "+ cat.getName()
                System.out.print( "parameter= "+ dis.getNumber() +" "+ cat.getNumber()
                    +" " + par.getNumber());
                System.out.print( "\t\t\t" + par.getName());
                System.out.print( "\tUnit: " + par.getUnit());
                System.out.println( "\tDesc: " + par.getDescription());
              }
            }
          }  // end for parameter
          dis.setCategory(cat);
        }      // end for category
        discipline.add(dis);
      }
      is.close();
      addLocalParameters("resources/grib/tables/grib2local.tab");
    } catch (Throwable e) {
      logger.error("grib2 table reading failed", e);
    }
  }                  // end static

  /**
   * converts description to a netcdf 3 type variable name
   *
   * @param description table raw description
   * @return Valid Description of netcdf 3 type variable name
   */
  private static String makeValidDesc(String description) {
    description = description.replaceAll("\\s+", "_");
    if (valid.matcher(description).find())
      return description;
    if (numberFirst.matcher(description).find())
      description = "N" + description;
    // else check for special characters
    return description.replaceAll("\\)|\\(|=|,|;|\\[|\\]", "");

  }

  /**
   * Get a name for the Discipline with id.
   *
   * @param aDis Discipline as a int
   * @return Name of the Discipline
   */
  public static String getDisciplineName(int aDis) {
    Discipline dis = getDiscipline(aDis);
    if (dis != null) {
      return dis.getName();
    } else {
      logger.debug("ParameterTable: UnknownDiscipline " + Integer.toString(aDis));
      return "UnknownDiscipline_" + Integer.toString(aDis);
    }
  }

  /**
   * Get the Discipline with id <tt>id</tt>.
   *
   * @param aDis Discipline as a int
   * @return the Discipline
   */
  private static Discipline getDiscipline(int aDis) {
    for (Discipline dis : discipline) {
      if (dis.getNumber() == aDis) {
        return dis;
      }
    }
    logger.debug("ParameterTable: UnknownDiscipline " + Integer.toString(aDis));
    return null;
  }

  /**
   * Get a description for the Category with ids <tt>id</tt>.
   *
   * @param d Discipline
   * @param c Category
   * @return Name for the Category
   */
  public static String getCategoryName(int d, int c) {
    Discipline discipline = getDiscipline(d);
    if (discipline == null)
      return "UnknownDiscipline_" + Integer.toString(d) +"_Category_" + Integer.toString(c);
    Category category = discipline.getCategory(c);
    if (category == null) {
      logger.debug("ParameterTable: UnknownCategory " + Integer.toString(c));
      return "UnknownCategory_" + Integer.toString(c);
    }
    return category.getName();
  }

  /**
   * Get a Name for the Parameter with ids <tt>id</tt>.
   *
   * @param d Discipline
   * @param c Category
   * @param p Parameter
   * @return Name for the Parameter
   */
  public static String getParameterName(int d, int c, int p) {
    Discipline discipline = getDiscipline(d);
    if (discipline == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return unknown;
    }

    Category category = discipline.getCategory(c);
    if (category == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return unknown;
    }

    GridParameter parameter = category.getParameter(p);
    if (parameter == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return unknown;
    }

    return parameter.getName();
  }

  /**
   * Get a unit for the Parameter with ids <tt>id</tt>.
   *
   * @param d Discipline
   * @param c Category
   * @param p Parameter
   * @return unit for the Parameter
   */
  public static String getParameterUnit(int d, int c, int p) {
    Discipline discipline = getDiscipline(d);
    if (discipline == null) {
      return "Unknown";
    }

    Category category = discipline.getCategory(c);
    if (category == null) {
      return "Unknown";
    }

    GridParameter parameter = category.getParameter(p);
    if (parameter == null) {
      return "Unknown";
    }

    return parameter.getUnit();
  }

  /**
   * Get a description for the Parameter with ids <tt>id</tt>.
   *
   * @param d Discipline
   * @param c Category
   * @param p Parameter
   * @return Name for the Parameter
   */
  public static String getParameterDescription(int d, int c, int p) {
    Discipline discipline = getDiscipline(d);
    if (discipline == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return unknown;
    }

    Category category = discipline.getCategory(c);
    if (category == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return unknown;
    }

    GridParameter parameter = category.getParameter(p);
    if (parameter == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return unknown;
    }

    return parameter.getDescription();
  }

  /**
   * Get a Parameter obj for the Parameter with ids <tt>id</tt>.
   *
   * @param d Discipline
   * @param c Category
   * @param p Parameter
   * @return Parameter
   */
  public static GridParameter getParameter(int d, int c, int p) {
    Discipline discipline = getDiscipline(d);
    if (discipline == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return new GridParameter(p, unknown, unknown, "Unknown");
    }

    Category category = discipline.getCategory(c);
    if (category == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return new GridParameter(p, unknown, unknown, "Unknown");
    }

    GridParameter parameter = category.getParameter(p);
    if (parameter == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return new GridParameter(p, unknown, unknown, "Unknown");
    }

    return parameter;
  }

  /**
   * Get a Local Parameter obj for the Parameter with ids <tt>id</tt>.
   *
   * @param d Discipline
   * @param c Category
   * @param p Parameter
   * @param center
   * @return Parameter
   */
  public static GridParameter getParameter(int d, int c, int p, int center) {
    // local parameters disciplines are offset by center and size of discipline
    Discipline discipline = getDiscipline( center * 255 + d);
    if (discipline == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return new GridParameter(p, unknown, unknown, "Unknown");
    }

    Category category = discipline.getCategory(c);
    if (category == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return new GridParameter(p, unknown, unknown, "Unknown");
    }

    GridParameter parameter = category.getParameter(p);
    if (parameter == null) {
      String unknown = "UnknownParameter_D"+ Integer.toString(d) +"_C"+
          Integer.toString(c) +"_"+ Integer.toString(p);
      return new GridParameter(p, unknown, unknown, "Unknown");
    }

    return parameter;
  }

  /**
   * Reads in the list of parameters and stores them.
   * Parameters are listed by row, fields are separated by tabs:
   * Discipline, Catagory, Parmeter Number, Name, Units, & Description.
   * For more information, look at IndexFormat.txt in the root of the
   * distribution.
   *
   * @param UserGribTable Name
   * @throws IOException  on reading list user parameters
   */

  public static void addParametersUser(String UserGribTable)
      throws IOException {
    addParametersUser(getInputStream(UserGribTable));
  }

  /**
   * Let the user add parameters
   *
   * @param is InputStream
   * @throws IOException on read
   */
  public static void addParametersUser(InputStream is) throws IOException {

    if (is == null) {
      return;
    }

    Pattern p_comment = Pattern.compile("^#");
    Pattern p_parameter =
        Pattern.compile("(.*)\t(.*)\t(.*)\t(.*)\t(.*)\t(.*)");
    Matcher m;

    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    String line;
    while ((line = br.readLine()) != null) {
      if (p_comment.matcher(line).find()) {
        continue;
      }
      m = p_parameter.matcher(line);
      if (m.find() && (m.groupCount() == 6)) {
        Parameter p = new Parameter(Integer.parseInt(m.group(3)),
            m.group(4), m.group(6),
            m.group(5));
        getDiscipline(Integer.parseInt(m.group(1))).getCategory(
            Integer.parseInt(m.group(2))).setParameter(p);
      }

    }
    is.close();
  }

  /**
   * Read in local parameters
   *
   * @param LocalParameters
   * @throws IOException on read
   */
  public static void addLocalParameters( String LocalParameters ) throws IOException {

    InputStream is = getInputStream( LocalParameters );
    if (is == null) {
      return;
    }
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);

    String line;
    int center = 0;
    String centerStr = null;
    while ((line = br.readLine()) != null) {

      if (line.startsWith( "#"))
        continue;
      line.trim();
      // fields seperated by at least double spaces
      String[] field = line.split("\\s[\\s]+");
      if (field.length == 1) {
        centerStr = "center_"+ field[ 0 ];
        center = Integer.parseInt( field[ 0 ]);
        continue;
      } else if( field.length == 6 ) {
        // 0   1   192  Weather  non-dim  Weather
        // remove non-dim unit
        if( field[ 4 ].compareToIgnoreCase( "non-dim") == 0)
          field[ 4 ] = "";
        Parameter p = new Parameter(Integer.parseInt(field[ 2 ]),
            field[ 3 ], field[ 5 ], field[ 4 ]);
        // unique the discipline number so no conflicts from different centers
        // first time disciplines will not exist
        int d = center * 255 + Integer.parseInt(field[ 0 ]);
        Discipline dis = getDiscipline( d );
        if (dis == null) {
          dis = new Discipline();
          dis.setNumber( d );
          dis.setName( centerStr );
          discipline.add(dis);
        }
        int c = Integer.parseInt(field[ 1 ]);
        // first time category will not exist
        Category cat = dis.getCategory( c );
        if ( cat == null ) {
          cat = new Category();
          cat.setNumber( c );
          cat.setName( field[ 1 ] );
          dis.setCategory( cat );
        }
        cat.setParameter( p );
      } else {
        // wrong number of fields
        logger.error("Grib2 local table reading failed");
      }
    }
    is.close();
  }

  /**
   * Get an input stream for the filename.
   *
   * @param filename name of file
   * @return corresponding input stream
   */
  private static InputStream getInputStream(String filename) {
    return ParameterTable.getInputStream(filename, null);
  }

  /**
   * Get an input stream for the filename.
   *
   * @param filename name of file
   * @param origin   relative origin point for file location
   * @return corresponding input stream
   */
  private static InputStream getInputStream(String filename, Class origin) {
    return ucar.grib.GribResourceReader.getInputStream(filename, origin);
  }

  public static void main(String[] args) {
    GridParameter p = ParameterTable.getParameter( 0, 0, 193, 8 );
    System.out.println( "Parameter = "+ p.getName() );

    System.out.println( "Parameter = "+ ParameterTable.getParameterName( 0, 0, 193 ) );
  }
}

