package ucar.ui.prefs;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import javax.swing.JFrame;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

/**
 * Test {@link BeanTable}
 */
@RunWith(JUnit4.class)
public class TestBeanTable {

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  static {
    System
        .setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  private static XMLStore xstore;
  private static PreferencesExt store = null;

  @BeforeClass
  public static void setUp() {
    try {
      xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    } catch (java.io.IOException e) {
    }
    store = xstore.getPreferences();
  }

  @Test
  public void testIntrospection() throws IOException {
    TestBean testBean = new TestBean();
    Class<? extends TestBean> beanClass = testBean.getClass();

    try {
      BeanInfo info = Introspector.getBeanInfo(beanClass, Object.class);
      System.out.println("Bean " + beanClass.getName());

      System.out.println("Properties:");
      PropertyDescriptor[] pd = info.getPropertyDescriptors();
      for (PropertyDescriptor propertyDescriptor : pd) {
        System.out.format(" %s %s%n", propertyDescriptor.getName(), propertyDescriptor.getPropertyType().getName());
        String propName = propertyDescriptor.getName();
        char first = Character.toUpperCase(propName.charAt(0));
        String method_name = "get" + first + propName.substring(1);
        try {
          Method method = beanClass.getMethod(method_name, (Class[]) null);
          System.out.println("   method = " + method);
        } catch (NoSuchMethodException e) {
          System.out.println("  ***NoSuchMethodException " + method_name);
          assert false;
        }
      }

    } catch (HeadlessException e) {
      // ok to fail if there is no display
    } catch (IntrospectionException e) {
      assert false;
    }
  }

  @Test
  public void testBuildAndShow() throws IOException {
    try {
      BeanTable<TestBean> bt = new BeanTable<>(TestBean.class, store, true, "header",
          "header\ntooltip", null);

      JFrame frame = new JFrame("Test BeanTable");
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          try {
            bt.saveState(true);
            xstore.save();
            System.exit(0);
          } catch (java.io.IOException ee) {
            ee.printStackTrace();
          }
        }
      });

      frame.getContentPane().add(bt);
      bt.setPreferredSize(new Dimension(500, 200));

      frame.pack();
      frame.setLocation(300, 300);
      frame.setVisible(true);
    } catch (
        HeadlessException e) {
      // ok to fail if there is no display
    }
  }
}
