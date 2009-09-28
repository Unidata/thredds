package thredds.catalog2.builder.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import thredds.catalog2.builder.ThreddsMetadataBuilder;
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.simpleImpl.ThreddsBuilderFactoryImpl;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsMetadataBuilderUtilsTest
{
  @Test
  public void checkCopyThreddsMetadataBuilderVariables()
  {
    ThreddsBuilderFactory fac = new ThreddsBuilderFactoryImpl();

    ThreddsMetadataBuilder srcTmBldr = fac.newThreddsMetadataBuilder();
    ThreddsMetadataBuilder.VariableGroupBuilder srcVarGrpBldr = srcTmBldr.addVariableGroupBuilder();

    String vocabAuthId = "vocabAuthId";
    srcVarGrpBldr.setVocabularyAuthorityId( vocabAuthId );
    String name = "name";
    srcVarGrpBldr.addVariableBuilder( name, "descrip", "units", "vocabId", "vocabName" );

    ThreddsMetadataBuilder recipientTmBldr = fac.newThreddsMetadataBuilder();
    assertNotNull( recipientTmBldr);
    assertTrue( recipientTmBldr.getVariableGroupBuilders().isEmpty());

    ThreddsMetadataBuilderUtils.copyThreddsMetadataBuilder( srcTmBldr, recipientTmBldr );

    List<ThreddsMetadataBuilder.VariableGroupBuilder> recipientVarGrpBldrs = recipientTmBldr.getVariableGroupBuilders();
    assertFalse( recipientVarGrpBldrs.isEmpty() );
    assertEquals( 1, recipientVarGrpBldrs.size());

    ThreddsMetadataBuilder.VariableGroupBuilder recipientVarGrpBldr = recipientVarGrpBldrs.get( 0);
    assertEquals( vocabAuthId, recipientVarGrpBldr.getVocabularyAuthorityId());

    List<ThreddsMetadataBuilder.VariableBuilder> variableBuilderList = recipientVarGrpBldr.getVariableBuilders();
    assertEquals( 1, variableBuilderList.size());
    ThreddsMetadataBuilder.VariableBuilder varBldr = variableBuilderList.get( 0);
    assertEquals( name, varBldr.getName());
  }
}
