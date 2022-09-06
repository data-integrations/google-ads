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

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v11.services.GoogleAdsRow;
import com.google.ads.googleads.v11.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v11.services.SearchGoogleAdsRequest;
import com.google.auth.Credentials;
import com.google.auth.oauth2.UserCredentials;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.plugin.googleads.source.multiple.MultiReportBatchSourceGoogleAdsConfig;
import io.cdap.plugin.googleads.source.single.BatchSourceGoogleAdsConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class to incorporate GoogleAds api interaction
 */
public class GoogleAdsHelper {

  public GoogleAdsClient getGoogleAdsClient(BaseGoogleAdsConfig baseGoogleAdsConfig) {
    GoogleAdsClient googleAdsClient;

    Credentials credentials =
            UserCredentials.newBuilder()
                    .setClientId(baseGoogleAdsConfig.clientId)
                    .setClientSecret(baseGoogleAdsConfig.clientSecret)
                    .setRefreshToken(baseGoogleAdsConfig.refreshToken)
                    .build();

    googleAdsClient = GoogleAdsClient.newBuilder()
            .setCredentials(credentials)
            .setDeveloperToken(baseGoogleAdsConfig.developerToken)
            .setLoginCustomerId(Long.parseLong(baseGoogleAdsConfig.clientCustomerId))
            .build();

    return googleAdsClient;
  }

  public List<StructuredRecord> buildReportStructure(BatchSourceGoogleAdsConfig config)
    throws IOException {
    String report = downloadReport(config, null);
    Reader reader = new StringReader(report);
    CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

    List<StructuredRecord> reportStructure = new ArrayList<>();
    List<String> reportFields = config.getReportFields();

    for (CSVRecord csvRecord : csvParser) {
      Iterator<String> reportFieldsIterator = reportFields.iterator();
      Iterator<String> csvRecordIterator = csvRecord.iterator();
      StructuredRecord.Builder builder = StructuredRecord.builder(config.getSchema());
      while (reportFieldsIterator.hasNext() && csvRecordIterator.hasNext()) {
        builder.set(reportFieldsIterator.next(), csvRecordIterator.next());
      }
      reportStructure.add(builder.build());
    }
    return reportStructure;
  }

  public String downloadReport(BaseGoogleAdsConfig config, String reportName) throws IOException {
    GoogleAdsClient googleAdsClient = getGoogleAdsClient(config);
    GoogleAdsServiceClient googleAdsServiceClient = googleAdsClient.getLatestVersion().createGoogleAdsServiceClient();
    SearchGoogleAdsRequest searchGoogleAdsRequest = getSearchGoogleAdsRequest(config, reportName);

    int maxTries = 3;
    int count = 0;
    while (true) {
      try {
        GoogleAdsServiceClient.SearchPagedResponse response = googleAdsServiceClient.search(searchGoogleAdsRequest);

        //for (GoogleAdsRow row : response.iterateAll()) {
          //
        //}

        return response.toString();
      } catch (Exception e) {
        if (++count == maxTries) {
          throw e;
        }
      }
    }
  }

  private SearchGoogleAdsRequest getSearchGoogleAdsRequest(BaseGoogleAdsConfig config,
                                               String reportPreset)
    throws IOException {
    MultiReportBatchSourceGoogleAdsConfig multiReportConfig = null;
    BatchSourceGoogleAdsConfig singleReportConfig = null;
    ReportPreset preset = null;
    if (config instanceof MultiReportBatchSourceGoogleAdsConfig) {
      multiReportConfig = (MultiReportBatchSourceGoogleAdsConfig) config;
      ReportPresetHelper presetHelper = new ReportPresetHelper();
      preset = presetHelper.getReportPreset(reportPreset);
    } else if (config instanceof BatchSourceGoogleAdsConfig) {
      singleReportConfig = (BatchSourceGoogleAdsConfig) config;
    }

    String query = "SELECT *" +
            "FROM " +
            reportPreset;
    SearchGoogleAdsRequest searchGoogleAdsRequest = SearchGoogleAdsRequest.newBuilder()
            .setCustomerId(config.clientCustomerId)
            .setPageSize(5000)
            .setQuery(query)
            .build();

    return searchGoogleAdsRequest;
  }
}
