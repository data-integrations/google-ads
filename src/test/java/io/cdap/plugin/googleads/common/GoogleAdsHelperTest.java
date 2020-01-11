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
package io.cdap.plugin.googleads.common;

import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.common.base.Strings;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.plugin.googleads.source.batch.GoogleAdsBatchSourceConfig;
import io.cdap.plugin.googleads.source.batch.GoogleAdsBatchSourceConfigTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class GoogleAdsHelperTest {

  private static String refreshToken;
  private static String clientId;
  private static String clientSecret;
  private static String developerToken;
  private static String clientCustomerId;

  @BeforeClass
  public static void setupTestClass() throws Exception {
    // initialize api
    refreshToken = System.getProperty("gads.refresh.token");
    if (Strings.isNullOrEmpty(refreshToken)) {
      throw new IllegalArgumentException("gads.refresh.token system property must not be empty.");
    }
    clientId = System.getProperty("gads.client.id");
    if (Strings.isNullOrEmpty(clientId)) {
      throw new IllegalArgumentException("gads.client.id system property must not be empty.");
    }
    clientSecret = System.getProperty("gads.client.secret");
    if (Strings.isNullOrEmpty(clientSecret)) {
      throw new IllegalArgumentException("gads.client.secret system property must not be empty.");
    }
    developerToken = System.getProperty("gads.developer.token");
    if (Strings.isNullOrEmpty(developerToken)) {
      throw new IllegalArgumentException("gads.developer.token system property must not be empty.");
    }
    clientCustomerId = System.getProperty("gads.customer.id");
    if (Strings.isNullOrEmpty(clientCustomerId)) {
      throw new IllegalArgumentException("gads.customer.id system property must not be empty.");
    }
  }


  @Test
  public void testBuildReportStructure() throws OAuthException, IOException, ValidationException {
    //setup mocks
    GoogleAdsBatchSourceConfig config = spy(new GoogleAdsBatchSourceConfig("test"));
    List<String> fields = new ArrayList<>();
    fields.add("test1");
    fields.add("test2");
    fields.add("test3");
    doReturn(ReportDefinitionReportType.KEYWORDS_PERFORMANCE_REPORT).when(config).getReportType();
    doReturn(fields).when(config).getReportFields();
    String report = "1,2,3\n6,7,8";
    GoogleAdsHelper googleAdsHelper = spy(GoogleAdsHelper.class);
    doReturn(report).when(googleAdsHelper).downloadReport(config);
    //test
    List<StructuredRecord> records = googleAdsHelper.buildReportStructure(config);
    //assert
    Iterator<StructuredRecord> iterator = records.iterator();
    Assert.assertTrue(iterator.hasNext());
    StructuredRecord record = iterator.next();
    Assert.assertEquals("1", record.get("test1"));
    Assert.assertEquals("2", record.get("test2"));
    Assert.assertEquals("3", record.get("test3"));
    Assert.assertTrue(iterator.hasNext());
    record = iterator.next();
    Assert.assertEquals("6", record.get("test1"));
    Assert.assertEquals("7", record.get("test2"));
    Assert.assertEquals("8", record.get("test3"));
    Assert.assertFalse(iterator.hasNext());
  }

  @Test
  public void testBuildReportStructureUsingApi() throws Exception {
    //setup mocks
    GoogleAdsBatchSourceConfig config = GoogleAdsBatchSourceConfigTest.getTestConfig(refreshToken, clientId,
                                                                                     clientSecret, developerToken,
                                                                                     clientCustomerId);
    //test
    GoogleAdsHelper googleAdsHelper = new GoogleAdsHelper();
    List<StructuredRecord> records = googleAdsHelper.buildReportStructure(config);
    //assert
    Iterator<StructuredRecord> iterator = records.iterator();
    Assert.assertTrue(iterator.hasNext());
    StructuredRecord record = iterator.next();
    Assert.assertEquals("Total", record.get("AccountCurrencyCode"));
    Assert.assertEquals(" --", record.get("AccountDescriptiveName"));
    Assert.assertEquals(" --", record.get("AccountTimeZone"));
    Assert.assertFalse(iterator.hasNext());
  }

}
