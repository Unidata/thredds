package ucar.ui.prefs;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestComboBox {
  private static long lastEvent;

  @Test
  public void test() throws IOException {
    final CC vv = new CC();

    ComboBox<String> cb = new ComboBox<>( null);
    cb.addChangeListener(e -> {
      System.out.printf(" event %s %s %d [", e.getActionCommand(), cb.getSelectedItem(), cb.getSelectedIndex());
      for (int i=0; i<cb.getItemCount(); i++) {
        System.out.printf("%s,", cb.getItemAt(i));
      }
      System.out.printf("]%n");
      cb.addItem( (String) cb.getSelectedItem());
      System.out.printf(" -> addItem %s %d [", cb.getSelectedItem(), cb.getSelectedIndex());
      for (int i=0; i<cb.getItemCount(); i++) {
        System.out.printf("%s,", cb.getItemAt(i));
      }
      System.out.printf("]%n");
      vv.wascalled++;
    });

    assertThat(vv.wascalled).isEqualTo(0);
    cb.addItem("first");
    assertThat(cb.getSelectedItem()).isEqualTo("first");
    cb.addItem("second");
    assertThat(cb.getSelectedItem()).isEqualTo("second");
    assertThat(vv.wascalled).isEqualTo(2);
    assertThat(cb.getSelectedIndex()).isEqualTo(0);
    System.out.printf("-----setSelectedItem(first)%n");
    cb.setSelectedItem("first");
    assertThat(cb.getSelectedItem()).isEqualTo("first");
    assertThat(vv.wascalled).isEqualTo(3);
    System.out.printf("-----addItem(third)%n");
    cb.addItem("third");
    assertThat(vv.wascalled).isEqualTo(4);
  }

  private class CC {
    int wascalled = 0;
  }

}
