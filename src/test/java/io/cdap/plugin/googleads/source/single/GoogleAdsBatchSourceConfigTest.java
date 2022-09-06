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
  public void testValidateReportTypeAndFields() throws IOException {
    //setup mocks
    BatchSourceGoogleAdsConfig config = spy(new BatchSourceGoogleAdsConfig("test"));
    config.reportType = "KEYWORDS_PERFORMANCE_REPORT";
    config.reportFields = "test1,test2";
    GoogleAdsHelper googleAdsHelper = spy(GoogleAdsHelper.class);


  }

  @Test
  public void testGetSchema() throws IOException {
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
