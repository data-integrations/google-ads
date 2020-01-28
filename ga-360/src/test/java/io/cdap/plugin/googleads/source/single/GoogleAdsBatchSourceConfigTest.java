/*
 * Copyright © 2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.plugin.googleads.source.single;

import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionField;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.mock.validation.MockFailureCollector;
import io.cdap.plugin.googleads.common.GoogleAdsHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class GoogleAdsBatchSourceConfigTest {


  @Test
  public void testValidateReportTypeAndFields() throws OAuthException, IOException, ValidationException {
    //setup mocks
    BatchSourceGoogleAdsConfig config = spy(new BatchSourceGoogleAdsConfig("test"));
    config.reportType = "KEYWORDS_PERFORMANCE_REPORT";
    config.reportFields = "test1,test2";
    GoogleAdsHelper googleAdsHelper = spy(GoogleAdsHelper.class);

    ReportDefinitionField reportDefinitionField1 = new ReportDefinitionField();
    reportDefinitionField1.setFieldName("test1");
    ReportDefinitionField reportDefinitionField2 = new ReportDefinitionField();
    reportDefinitionField2.setFieldName("test2");
    ReportDefinitionField reportDefinitionField3 = new ReportDefinitionField();
    reportDefinitionField3.setFieldName("test3");
    reportDefinitionField3.setExclusiveFields(new String[]{"test4"});
    ReportDefinitionField reportDefinitionField4 = new ReportDefinitionField();
    reportDefinitionField4.setFieldName("test4");
    reportDefinitionField4.setExclusiveFields(new String[]{"test3"});
    ReportDefinitionField[] fields = new ReportDefinitionField[]{reportDefinitionField1,
      reportDefinitionField2,
      reportDefinitionField3,
      reportDefinitionField4};

    doReturn(fields).when(googleAdsHelper).getReportDefinitionFields(config, "KEYWORDS_PERFORMANCE_REPORT");
    MockFailureCollector failureCollector = new MockFailureCollector();
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertTrue(failureCollector.getValidationFailures().isEmpty());

    //setup mocks failure
    config.reportFields = "test1,test5";
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());
    Assert.assertEquals("Invalid Field 'test5'", failureCollector.getValidationFailures().get(0).getMessage());
    failureCollector.getValidationFailures().clear();


    //setup mocks failure
    config.reportFields = "test3,test4,test4,invalid"; //valid
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(3, failureCollector.getValidationFailures().size());

    Assert.assertEquals("reportFields contains duplicates",
                        failureCollector.getValidationFailures().get(0).getMessage());
    Assert.assertEquals("Field 'test4' conflicts with field 'test3'",
                        failureCollector.getValidationFailures().get(1).getMessage());
    Assert.assertEquals("Invalid Field 'invalid'",
                        failureCollector.getValidationFailures().get(2).getMessage());
    failureCollector.getValidationFailures().clear();

    //setup mocks failure
    config.reportType = "invalid";
    config.reportFields = "test1,test2"; //valid
    //test
    config.validateReportTypeAndFields(failureCollector, googleAdsHelper);
    //assert
    Assert.assertEquals(1, failureCollector.getValidationFailures().size());

    Assert.assertEquals("reportType 'invalid' is not a valid report type",
                        failureCollector.getValidationFailures().get(0).getMessage());
    failureCollector.getValidationFailures().clear();
  }

  @Test
  public void testGetSchema() throws OAuthException, IOException, ValidationException {
    //setup mocks
    BatchSourceGoogleAdsConfig config = new BatchSourceGoogleAdsConfig("test");
    config.reportFields = "test1,test2";
    //test
    Schema schema = config.getSchema();
    //assert
    Schema expectedSchema = Schema.recordOf("TestSchema",
      Schema.Field.of("test1", Schema.nullableOf(Schema.of(Schema.Type.STRING))),
      Schema.Field.of("test2", Schema.nullableOf(Schema.of(Schema.Type.STRING))));
    Assert.assertTrue(schema.isCompatible(expectedSchema));
  }
}
