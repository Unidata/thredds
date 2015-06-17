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
package ucar.util.prefs;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

class Bean {

  private Object o; // the wrapped object
  private BeanParser p = null; // the bean parser (shared for all beans of same class)

  // wrap an object in a Bean
  public Bean(Object o) {
    this.o = o;
  }

  // create a bean from an XML element
  public Bean(org.xml.sax.Attributes atts) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    String className = atts.getValue("class");
    Class<?> c = Class.forName(className);
    o = c.newInstance();
    p = BeanParser.getParser( c);
    p.readProperties(o, atts);
  }

  // write XML using the bean properties of the contained object
  public void writeProperties(PrintWriter out) throws IOException {
    if (p == null) p = BeanParser.getParser( o.getClass());
    p.writeProperties(o, out);
  }

  // get the wrapped object
  public Object getObject() { return o; }
  public Class<?> getBeanClass() { return o.getClass(); }

  static class Collection {
    private java.util.Collection<Object> collect; // the underlying collection
    private Class<?> beanClass; // the class of the beans in the collection
    private BeanParser p = null; // the bean parser (shared for all beans of same class)

    // wrap a collection in a bean
    Collection(java.util.Collection<Object> collect) {
      this.collect = collect;
      if (!collect.isEmpty()) {
        Iterator iter = collect.iterator();
        beanClass = iter.next().getClass();
        p = BeanParser.getParser(beanClass);
      }
    }

    // create a bean collection from an XML element
    Collection(org.xml.sax.Attributes atts) throws ClassNotFoundException { // , InstantiationException, IllegalAccessException {
      String className = atts.getValue("class");
      beanClass = Class.forName( className);
      p = BeanParser.getParser( beanClass);
      collect = new ArrayList<>();
    }

    // write XML using the bean properties of the specified object
    public void writeProperties(PrintWriter out, Object thiso) throws IOException {
      p.writeProperties(thiso, out);
    }

    // get the underlying java.util.Collection
    public java.util.Collection getCollection() { return collect; }
    public Class<?> getBeanClass() { return beanClass; }

      // write XML using the bean properties of the contained object
    public Object readProperties(org.xml.sax.Attributes atts) throws InstantiationException, IllegalAccessException {
      Object o = beanClass.newInstance();
      p.readProperties(o, atts);
      collect.add(o);
      return o;
    }

  }

  private static class BeanParser {
    private static boolean debugBean = false;
    private static Map<Class<?>, BeanParser> parsers = new HashMap<>();

    static BeanParser getParser(Class<?> beanClass) {
      BeanParser parser;
      if (null == (parser = parsers.get(beanClass))) {
        parser = new BeanParser(beanClass);
        parsers.put(beanClass, parser);
      }
      return parser;
    }

    private Map<String, PropertyDescriptor> properties = new TreeMap<>();
    private Object[] args = new Object[1];
    BeanParser(Class<?> beanClass) {

      // get bean info
      BeanInfo info;
      try {
        info = Introspector.getBeanInfo(beanClass, Object.class);
      } catch (IntrospectionException e) {
        e.printStackTrace();
        return;
      }

      if (debugBean)
        System.out.println( "Bean "+beanClass.getName());

      // properties must have read and write method
      PropertyDescriptor[] pds = info.getPropertyDescriptors();
      for (PropertyDescriptor pd : pds) {
        if ((pd.getReadMethod() != null) && (pd.getWriteMethod() != null)) {
          properties.put(pd.getName(), pd);
          if (debugBean) {
            System.out.println(" property " + pd.getName());
          }
        }
      }
    }

    void writeProperties(Object bean, PrintWriter out) throws IOException {
      for (PropertyDescriptor pds : properties.values()) {
        Method getter = pds.getReadMethod();
        try {
          Object value = getter.invoke(bean, (Object[]) null);
          if (value == null) {
            continue;
          }
          if (value instanceof String) {
            value = XMLStore.quote((String) value);
          } else if (value instanceof Date) {
            value = ((Date) value).getTime();
          }
          out.print(pds.getName() + "='" + value + "' ");
          if (debugBean) {
            System.out.println(" property get " + pds.getName() + "='" + value + "' ");
          }
        } catch (InvocationTargetException | IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    void readProperties(Object bean, org.xml.sax.Attributes atts) {
      for (Object o1 : properties.values()) {
        PropertyDescriptor pds = (PropertyDescriptor) o1;
        Method setter = pds.getWriteMethod();
        if (setter == null) {
          continue;
        }

        try {
          String sArg = atts.getValue(pds.getName());
          Object arg = getArgument(pds.getPropertyType(), sArg);
          if (debugBean) {
            System.out.println(" property set " + pds.getName() + "=" + sArg + " == " + arg);
          }
          if (arg == null) {
            return;
          }
          args[0] = arg;
          setter.invoke(bean, args);
        } catch (NumberFormatException | InvocationTargetException | IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    private Object getArgument(Class<?> c, String value) {
      if (c == String.class) {
        return value;
      } else if (c == int.class) {
        return Integer.valueOf(value);
      } else if (c == double.class) {
        return Double.valueOf(value);
      } else if (c == boolean.class) {
        if (value == null) {
          // Boolean.valueOf(null) actually returns "false", which we don't want.
          return null;
        } else {
          return Boolean.valueOf(value);
        }
      } else if (c == float.class) {
        return Float.valueOf(value);
      } else if (c == short.class) {
        return Short.valueOf(value);
      } else if (c == long.class) {
        return Long.valueOf(value);
      } else if (c == byte.class) {
        return Byte.valueOf(value);
      } else if (c == Date.class) {
        long time = Long.parseLong(value);
        return new Date(time);
      } else {
        return null;
      }
    }
  }
}
