package ucar.ui.prefs;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ucar.util.prefs.PreferencesExt;

@RunWith(JUnit4.class)
public class TestComboBox {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  PreferencesExt prefs;

  private static long lastEvent;

  @Before
  public void setup() {
    ImmutableList<String> choices = ImmutableList.of("1", "2", "3");
    when(prefs.getList(any(), any())).thenReturn(choices);
  }

  @Test
  public void testSelection() throws IOException {
    final CC vv = new CC();

    ComboBox<String> cb = new ComboBox<>( prefs);
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

    assertThat(cb.getItemList()).isEqualTo(ImmutableList.of("1", "2", "3"));
    assertThat(cb.getSelectedObjects()).isEmpty();
    cb.setSelectedItem("2");
    assertThat(vv.wascalled).isEqualTo(1);
    assertThat(cb.getSelectedItem()).isEqualTo("2");
    assertThat(cb.getItemList()).isEqualTo(ImmutableList.of("2", "1", "3"));
  }

  @Test
  public void testAddChangeListener() throws IOException {
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
