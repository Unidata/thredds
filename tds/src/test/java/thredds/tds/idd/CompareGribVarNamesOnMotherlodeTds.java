package thredds.tds.idd;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.test.util.ThreddsServer;

import java.util.Arrays;
import java.util.Collection;

/**
 * Compare variable names in  "FMRC Run" and "FMRC Raw File" datasets with matching timestamps.
 *
 * @author edavis
 * @since 4.0
 */
@RunWith(Parameterized.class)
public class CompareGribVarNamesOnMotherlodeTds
{
  private String modelId;
  private String tdsUrl = "http://thredds.ucar.edu/thredds/";

  public CompareGribVarNamesOnMotherlodeTds( String modelId )
  {
    super();
    this.modelId = modelId;
  }

  @Parameterized.Parameters(name="{0}")
  public static Collection<Object[]> getModelIds()
  {
    return Arrays.asList( IddModelDatasetsUtils.getModelIds());
    // return Arrays.asList( IddModelDatasetsUtils.getGfsModelIds());
  }

  @Before
  public void setUp() {
    ThreddsServer.LIVE.assumeIsAvailable();
  }

  /**
   * For the given model ID, get the fifth dataset in the "FMRC Run" catalog and compare it to the
   * dataset with matching run time from the "FMRC Raw File" catalog.
   */
  @Test
  public void compareFifthFmrcRunDsAndMatchingScanDsVariableNames() {
      CompareGribVarNamesUtils.assertEqualityOfFmrcRunDsAndMatchingFmrcRawFileDsVariableNames( this.tdsUrl, this.modelId, 4 );
  }

  /**
   * For the given model ID, get the tenth dataset in the "FMRC Run" catalog and compare it to
   * the dataset with matching run time from the "FMRC Raw File" catalog.
   */
  @Test
  public void compareTenthFmrcRunDsAndMatchingScanDsVariableNames() {
      CompareGribVarNamesUtils.assertEqualityOfFmrcRunDsAndMatchingFmrcRawFileDsVariableNames( this.tdsUrl, this.modelId, 9 );
  }

}
