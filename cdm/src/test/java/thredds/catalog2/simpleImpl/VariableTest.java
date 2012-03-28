/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.catalog2.simpleImpl;

import org.junit.Test;
import static org.junit.Assert.*;

import org.easymock.EasyMock;
import thredds.catalog2.builder.BuilderIssues;
import thredds.catalog2.builder.ThreddsMetadataBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class VariableTest
{
  @Test
  public void checkVariableCtorAndGet()
  {
    String vocabAuthId = "vocabAuthId";
    String vocabAuthUrl = "vocabAuthUrl";
    ThreddsMetadataBuilder.VariableGroupBuilder mockVarGrp = createMockVarGroupBasic( vocabAuthId, vocabAuthUrl);

    String name = "name";
    String desc = "descrip";
    String units = "unit";
    String vocabId = "vocabId";
    String vocabName = "vocabName";
    ThreddsMetadataImpl.VariableImpl var = new ThreddsMetadataImpl.VariableImpl( name, desc, units, vocabId, vocabName, mockVarGrp);

    assertEquals( name, var.getName());
    assertEquals( desc, var.getDescription());
    assertEquals( units, var.getUnits());
    assertEquals( vocabId, var.getVocabularyId());
    assertEquals( vocabName, var.getVocabularyName());

    assertEquals( vocabAuthId, var.getVocabularyAuthorityId());
    assertEquals( vocabAuthUrl, var.getVocabularyAuthorityUrl());
  }

  @Test
  public void checkVariableSetGet()
  {
    String vocabAuthId = "vocabAuthId";
    String vocabAuthUrl = "vocabAuthUrl";
    ThreddsMetadataBuilder.VariableGroupBuilder mockVarGrp = createMockVarGroupBasic( vocabAuthId, vocabAuthUrl );

    ThreddsMetadataImpl.VariableImpl var = new ThreddsMetadataImpl.VariableImpl( "name", "descrip", "unit", "vocabId", "vocabName", mockVarGrp );

    String name2 = "name2";
    String desc2 = "descrip2";
    String units2 = "unit2";
    String vocabId2 = "vocabId2";
    String vocabName2 = "vocabName2";

    var.setName( name2);
    assertEquals( name2, var.getName());
    var.setDescription( desc2 );
    assertEquals( desc2, var.getDescription());
    var.setUnits( units2 );
    assertEquals( units2, var.getUnits());
    var.setVocabularyId( vocabId2 );
    assertEquals( vocabId2, var.getVocabularyId());
    var.setVocabularyName( vocabName2 );
    assertEquals( vocabName2, var.getVocabularyName());

    assertEquals( vocabAuthId, var.getVocabularyAuthorityId());
    assertEquals( vocabAuthUrl, var.getVocabularyAuthorityUrl());
  }

  @Test
  public void checkVariableNoBuildIssues()
  {
    String vocabAuthId = "vocabAuthId";
    String vocabAuthUrl = "vocabAuthUrl";
    ThreddsMetadataBuilder.VariableGroupBuilder mockVarGrp = createMockVarGroupBasic( vocabAuthId, vocabAuthUrl );

    ThreddsMetadataImpl.VariableImpl var = new ThreddsMetadataImpl.VariableImpl( "name", "descrip", "unit", "vocabId", "vocabName", mockVarGrp );

    BuilderIssues bldrIssues = var.getIssues();
    assertTrue( bldrIssues.isEmpty());
  }

  @Test
  public void checkVariableWithNullNameHasBuildIssue()
  {
    String vocabAuthId = "vocabAuthId";
    String vocabAuthUrl = "vocabAuthUrl";
    ThreddsMetadataBuilder.VariableGroupBuilder mockVarGrp = createMockVarGroupBasic( vocabAuthId, vocabAuthUrl );

    ThreddsMetadataImpl.VariableImpl var = new ThreddsMetadataImpl.VariableImpl( null, "descrip", "unit", "vocabId", "vocabName", mockVarGrp );

    BuilderIssues bldrIssues = var.getIssues();
    assertFalse( bldrIssues.isEmpty());
    assertEquals( 1, bldrIssues.size());
  }

  @Test
  public void checkVariableWhereNameIsOnlyNonNullHasNoBuildIssue()
  {
    String vocabAuthId = "vocabAuthId";
    String vocabAuthUrl = "vocabAuthUrl";
    ThreddsMetadataBuilder.VariableGroupBuilder mockVarGrp = createMockVarGroupBasic( vocabAuthId, vocabAuthUrl );

    ThreddsMetadataImpl.VariableImpl var = new ThreddsMetadataImpl.VariableImpl( "name", null, null, null, null, mockVarGrp );

    BuilderIssues bldrIssues = var.getIssues();
    assertTrue( bldrIssues.isEmpty());
  }

  private ThreddsMetadataBuilder.VariableGroupBuilder createMockVarGroupBasic( String vocabAuthId, String vocabAuthUrl )
  {
    ThreddsMetadataBuilder.VariableGroupBuilder mockVarGrp = EasyMock.createMock( ThreddsMetadataBuilder.VariableGroupBuilder.class );
    EasyMock.expect( mockVarGrp.getVocabularyAuthorityId()).andReturn( vocabAuthId );
    EasyMock.expect( mockVarGrp.getVocabularyAuthorityUrl()).andReturn( vocabAuthUrl );

    EasyMock.replay( mockVarGrp );

    return mockVarGrp;
  }
}