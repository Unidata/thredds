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
/*

http://www.javaspecialists.eu/archive/Issue142.html

    java -javaagent:lib/memoryagent.jar <Our main class>
    
    <?xml version="1.0"?>
    <project name="memoryagent" default="compile">
      <target name="init">
        <tstamp/>
        <mkdir dir="build"/>
      </target>

      <target name="compile" depends="init">
        <javac srcdir="src" source="1.5" target="1.5"
               destdir="build"/>
        <copy todir="build/META-INF">
          <fileset dir="src/META-INF"/>
        </copy>
        <jar jarfile="memoryagent.jar" basedir="build"
             filesetmanifest="merge"/>

      </target>

      <target name="clean">
        <delete dir="build"/>
        <delete file="memoryagent.jar"/>
      </target>
    </project>  
 */

package ucar.nc2.util.memory;

import java.lang.instrument.Instrumentation;

import java.lang.reflect.*;
import java.util.*;

/**
 * Use the Java instrumentation API to predict the amount of memory taken up by an object.
 * This is taken from the Java Specialist newsletter #142 "Instrumentation Memory Counter",
 * http://www.javaspecialists.eu/archive/Issue142.html.
 *
 */
public class MemoryCounterAgent {
  private static Instrumentation instrumentation;

  /**
   * Initializes agent
   */
  public static void premain(String agentArgs, Instrumentation instrumentation) {
    MemoryCounterAgent.instrumentation = instrumentation;
  }

  /**
   * Returns object size.
   */
  public static long sizeOf(Object obj) {
    if (instrumentation == null) {
      throw new IllegalStateException( "Instrumentation environment not initialised.");
    }
    long result;
    if (isSharedFlyweight(obj))
      result = 0;
    else
     result =  instrumentation.getObjectSize(obj);

    return result;
  }

  /**
   * Returns true if this is a well-known shared flyweight.
   * For example, interned Strings, Booleans and Number objects.
   */

  private static boolean isSharedFlyweight(Object obj) {
    // optimization - all of our flyweights are Comparable
    if (obj instanceof Comparable) {
      if (obj instanceof Enum) {
        return true;
      } else if (obj instanceof String) {
        return (obj == ((String) obj).intern());
      } else if (obj instanceof Boolean) {
        return (obj == Boolean.TRUE || obj == Boolean.FALSE);
      } else if (obj instanceof Integer) {
        return (obj == Integer.valueOf((Integer) obj));
      } else if (obj instanceof Short) {
        return (obj == Short.valueOf((Short) obj));
      } else if (obj instanceof Byte) {
        return (obj == Byte.valueOf((Byte) obj));
      } else if (obj instanceof Long) {
        return (obj == Long.valueOf((Long) obj));
      } else if (obj instanceof Character) {
        return (obj == Character.valueOf((Character) obj));
      }
    }
    return false;
  }

  /**
   * Returns deep size of object, recursively iterating over its
   * fields and superclasses.
   */
  public static long deepSizeOf(Object obj) {
    Map visited = new IdentityHashMap();
    Stack stack = new Stack();
    stack.push(obj);

    long result = 0;
    do {
      result += internalSizeOf(stack.pop(), stack, visited);
    } while (!stack.isEmpty());

    return result;
  }

  private static boolean skipObject(Object obj, Map visited) {
    return obj == null || visited.containsKey(obj) || isSharedFlyweight(obj);
  }

  private static long internalSizeOf( Object obj, Stack stack, Map visited) {

    if (skipObject(obj, visited)) {
      return 0;
    }

    Class clazz = obj.getClass();
    if (clazz.isArray()) {
      addArrayElementsToStack(clazz, obj, stack);

    } else {
      // add all non-primitive fields to the stack
      while (clazz != null) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
          if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
            field.setAccessible(true);
            try {
              stack.add(field.get(obj));
            } catch (IllegalAccessException ex) {
              throw new RuntimeException(ex);
            }
          }
        }
        clazz = clazz.getSuperclass();
      }
    }

    visited.put(obj, null);
    return sizeOf(obj);
  }

  private static void addArrayElementsToStack(  Class clazz, Object obj, Stack stack) {
    if (!clazz.getComponentType().isPrimitive()) {
      int length = Array.getLength(obj);
      for (int i = 0; i < length; i++) {
        stack.add(Array.get(obj, i));
      }
    }
  }

  ///////////////////////////////

  public static long deepSizeOf2(String name, Object obj, Class skipClass, boolean show) {
    if (show) System.out.printf("%s %s\n", obj.getClass().getName(), name);
    Map visited = new IdentityHashMap();
    Stack<NamedObject> stack = new Stack<NamedObject>();
    stack.push( new NamedObject(name,obj));

    long result = 0;
    do {
      result += internalSizeOf2(stack.pop(), stack, visited, skipClass, show);
    } while (!stack.isEmpty());
    return result;
  }

  private static long internalSizeOf2( NamedObject nobj, Stack<NamedObject> stack, Map visited, Class skipClass, boolean show) {
    Object obj = nobj.obj;
    if (skipObject(obj, visited)) {
      return 0;
    }

    Class clazz = obj.getClass();
    if (clazz.isArray()) {
      addArrayElementsToStack2(clazz, nobj, stack);

    } else {
      // add all non-primitive fields to the stack
      while (clazz != null) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
          if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
            field.setAccessible(true);
            try {
              Object val = field.get(obj);
              if ((skipClass != null) && skipClass.isInstance(val)) continue;
              if (!(val instanceof java.lang.Class) && !(val instanceof ucar.nc2.NetcdfFile))
                stack.add( new NamedObject(nobj.name+"-"+field.getName(), val));
              //if (show) System.out.printf("    add %s for %s\n", field.getName(), obj.getClass().getName());
            } catch (IllegalAccessException ex) {
              throw new RuntimeException(ex);
            }
          }
        }
        clazz = clazz.getSuperclass();
      }
    }

    visited.put(obj, null);
    long result = sizeOf(obj);
    if (show) System.out.printf("  %5d %s (%s)%n", result, nobj.name, obj.getClass().getName());
    return result;
  }

  private static void addArrayElementsToStack2(  Class clazz, NamedObject nobj, Stack<NamedObject> stack) {
    if (!clazz.getComponentType().isPrimitive()) {
      int length = Array.getLength(nobj.obj);
      for (int i = 0; i < length; i++) {
        stack.add( new NamedObject(nobj.name, Array.get(nobj.obj, i)));
      }
    }
  }

  static private class NamedObject {
    String name;
    Object obj;
    NamedObject(String name, Object obj) {
      this.name = name;
      this.obj = obj;
    }
  }

  ///////////////////////////////

  public static long deepSizeOf3(String name, Object obj, Class skipClass, boolean show) {
    if (show) System.out.printf("%s %s\n", obj.getClass().getName(), name);
    Map visited = new IdentityHashMap();

    long result = deepSizeOf3(name, obj, visited, skipClass, show, 0);
    return result;
  }

  private static long deepSizeOf3(String name, Object obj, Map visited, Class skipClass, boolean show, int indent) {
    //if (name.endsWith("firstRecord-this$0-index-elementData"))
    //  System.out.println("HEY");
    long result = internalSizeOf3(name, obj, visited, skipClass, show, indent);
    if (show) {
      for (int i=0; i<indent; i++) System.out.print(" ");
      System.out.printf("%6d %s (%s)\n", result, name, obj.getClass().getName());
    }
    return result;
  }

  private static long internalSizeOf3( String name, Object obj, Map visited, Class skipClass, boolean show, int indent) {
    if (skipObject(obj, visited))
      return 0;

    visited.put(obj, null);
    long result = sizeOf(obj);

    Class clazz = obj.getClass();
    if (clazz.isArray() && !clazz.getComponentType().isPrimitive()) {
      int length = Array.getLength(obj);
      for (int i = 0; i < length; i++) {
        Object val = Array.get(obj, i);
        if (val != null)
          result += deepSizeOf3( name, val, visited, skipClass, show, indent+2);
      }
    } else {
      // add all non-primitive fields to the stack
      while (clazz != null) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
          if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
            field.setAccessible(true);
            try {
              Object val = field.get(obj);
              if (val == null) continue;
              if ((skipClass != null) && skipClass.isInstance(val)) continue;
              if (!(val instanceof java.lang.Class) && !(val instanceof ucar.nc2.NetcdfFile))
                result += deepSizeOf3(name+"-"+field.getName(), val, visited, skipClass, show, indent+2);
            } catch (IllegalAccessException ex) {
              throw new RuntimeException(ex);
            }
          }
        }
        clazz = clazz.getSuperclass();
      }
    }

    return result;
  }


}
