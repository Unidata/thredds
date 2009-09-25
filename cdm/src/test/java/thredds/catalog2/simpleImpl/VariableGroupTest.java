package thredds.catalog2.simpleImpl;

import org.junit.Test;
import static org.junit.Assert.*;

import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog2.builder.ThreddsMetadataBuilder;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class VariableGroupTest
{
  @Test
  public void checkVariableGroupSetAndGet()
  {
    String vocabAuthId = "vocabAuthId";
    String vocabAuthUrl = "vocabAuthUrl";

    ThreddsMetadataBuilder.VariableGroupBuilder varGrpBldr = new ThreddsMetadataImpl.VariableGroupImpl();
    varGrpBldr.setVocabularyAuthorityId( vocabAuthId );
    varGrpBldr.setVocabularyAuthorityUrl( vocabAuthUrl );

    assertTrue( varGrpBldr.isEmpty());
    assertNotNull( varGrpBldr.getVariableBuilders());
    assertTrue( varGrpBldr.getVariableBuilders().isEmpty());

    String name = "name";
    String desc = "descrip";
    String units = "unit";
    String vocabId = "vocabId";
    String vocabName = "vocabName";
    varGrpBldr.addVariableBuilder( name, desc, units, vocabId, vocabName );

    String name2 = "name2";
    String desc2 = "descrip2";
    String units2 = "unit2";
    String vocabId2 = "vocabId2";
    String vocabName2 = "vocabName2";
    varGrpBldr.addVariableBuilder( name2, desc2, units2, vocabId2, vocabName2 );


    assertEquals( vocabAuthId, varGrpBldr.getVocabularyAuthorityId());
    assertEquals( vocabAuthUrl, varGrpBldr.getVocabularyAuthorityUrl());
    assertNull( varGrpBldr.getVariableMapUrl());
    assertFalse( varGrpBldr.isEmpty());

    List<ThreddsMetadataBuilder.VariableBuilder> varBldrs = varGrpBldr.getVariableBuilders();

    assertEquals( 2, varBldrs.size());
    assertEquals( name, varBldrs.get( 0).getName());
    assertEquals( name2, varBldrs.get( 1).getName());
  }

  @Test
  public void checkVariableGroupSetAndGetVarMap()
  {
    String vocabAuthId = "vocabAuthId";
    String vocabAuthUrl = "vocabAuthUrl";
    String varMapUrl = "varMapUrl";

    ThreddsMetadataBuilder.VariableGroupBuilder varGrpBldr = new ThreddsMetadataImpl.VariableGroupImpl();
    varGrpBldr.setVocabularyAuthorityId( vocabAuthId );
    varGrpBldr.setVocabularyAuthorityUrl( vocabAuthUrl );
    varGrpBldr.setVariableMapUrl( varMapUrl );

    assertEquals( vocabAuthId, varGrpBldr.getVocabularyAuthorityId());
    assertEquals( vocabAuthUrl, varGrpBldr.getVocabularyAuthorityUrl());
    assertEquals( varMapUrl, varGrpBldr.getVariableMapUrl());

    assertFalse( varGrpBldr.isEmpty());
    assertNotNull( varGrpBldr.getVariableBuilders());
    assertTrue( varGrpBldr.getVariableBuilders().isEmpty());
  }

  @Test
  public void checkVariableGroupNoBuildIssuesWithVars()
  {
    ThreddsMetadataBuilder.VariableGroupBuilder varGrpBldr = new ThreddsMetadataImpl.VariableGroupImpl();

    varGrpBldr.addVariableBuilder( "name", "desc", "units", "vocabId", "vocabName" );

    BuilderIssues bldrIssues = varGrpBldr.getIssues();
    assertTrue( bldrIssues.isEmpty());
  }

  @Test
  public void checkVariableGroupNoBuildIssuesWithVarMap()
  {
    ThreddsMetadataBuilder.VariableGroupBuilder varGrpBldr = new ThreddsMetadataImpl.VariableGroupImpl();
    varGrpBldr.setVariableMapUrl( "varMapUrl" );

    BuilderIssues bldrIssues = varGrpBldr.getIssues();
    assertTrue( bldrIssues.isEmpty());
    assertTrue( bldrIssues.isValid());
  }
}
