package thredds.tds.idd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.*;

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
  private String tdsUrl = "http://motherlode.ucar.edu:8080/thredds/";

  public CompareGribVarNamesOnMotherlodeTds( String modelId )
  {
    super();
    this.modelId = modelId;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> getModelIds()
  {
    return Arrays.asList( IddModelDatasetsUtils.getModelIds());
    // return Arrays.asList( IddModelDatasetsUtils.getGfsModelIds());
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
