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

import com.google.api.ads.adwords.axis.factory.AdWordsServices;
import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionField;
import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionReportType;
import com.google.api.ads.adwords.axis.v201809.cm.ReportDefinitionServiceInterface;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.client.reporting.ReportingConfiguration;
import com.google.api.ads.adwords.lib.factory.AdWordsServicesInterface;
import com.google.api.ads.adwords.lib.jaxb.v201809.DateRange;
import com.google.api.ads.adwords.lib.jaxb.v201809.DownloadFormat;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinition;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionDateRangeType;
import com.google.api.ads.adwords.lib.jaxb.v201809.Selector;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.adwords.lib.utils.v201809.ReportDownloaderInterface;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.api.client.auth.oauth2.Credential;
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

  private ReportingConfiguration getReportingConfiguration(BaseGoogleAdsConfig baseGoogleAdsConfig) {
    ReportingConfiguration.Builder builder = new ReportingConfiguration.Builder();
    if (baseGoogleAdsConfig instanceof MultiReportBatchSourceGoogleAdsConfig) {
      MultiReportBatchSourceGoogleAdsConfig config = (MultiReportBatchSourceGoogleAdsConfig) baseGoogleAdsConfig;
      builder = builder.skipReportHeader(!config.includeReportHeader)
        .skipColumnHeader(!config.includeColumnHeader);
    } else {
      builder = builder.skipReportHeader(true)
        .skipColumnHeader(true);
    }
    return builder.skipReportSummary(!baseGoogleAdsConfig.includeReportSummary)
      .useRawEnumValues(baseGoogleAdsConfig.useRawEnumValues)
      .includeZeroImpressions(baseGoogleAdsConfig.includeZeroImpressions).build();
  }

  public AdWordsSession getAdWordsSession(BaseGoogleAdsConfig baseGoogleAdsConfig)
    throws OAuthException, ValidationException {
    AdWordsSession session;
    Credential credential = new OfflineCredentials.Builder()
      .forApi(OfflineCredentials.Api.ADWORDS)
      .withClientSecrets(baseGoogleAdsConfig.clientId, baseGoogleAdsConfig.clientSecret)
      .withRefreshToken(baseGoogleAdsConfig.refreshToken)
      .build()
      .generateCredential();

    session = new AdWordsSession.Builder()
      .withClientCustomerId(baseGoogleAdsConfig.clientCustomerId)
      .withDeveloperToken(baseGoogleAdsConfig.developerToken)
      .withOAuth2Credential(credential)
      .build();
    return session;
  }

  public List<StructuredRecord> buildReportStructure(BatchSourceGoogleAdsConfig config)
    throws IOException, OAuthException, ValidationException, ReportDownloadResponseException, ReportException {
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

  public ReportDefinitionField[] getReportDefinitionFields(BaseGoogleAdsConfig config, String reportType)
    throws OAuthException, ValidationException, IOException {
    AdWordsSession session = getAdWordsSession(config);
    AdWordsServicesInterface adWordsServices = AdWordsServices.getInstance();
    // Get the ReportDefinitionService.
    ReportDefinitionServiceInterface reportDefinitionService =
      adWordsServices.get(session, ReportDefinitionServiceInterface.class);

    // Get report fields.
    return reportDefinitionService
      .getReportFields(ReportDefinitionReportType.fromValue(reportType));
  }

  public String downloadReport(BaseGoogleAdsConfig config, String reportName)
    throws OAuthException, ValidationException, IOException, ReportException, ReportDownloadResponseException {
    ReportDownloaderInterface reportDownloader = getReportDownloaderInterface(config);
    ReportDefinition reportDefinition = getReportDefinition(config, reportName);

    int maxTries = 3;
    int count = 0;
    while (true) {
      try {
        ReportDownloadResponse response = reportDownloader.downloadReport(
          reportDefinition);
        return response.getAsString();
      } catch (ReportException e) {
        if (++count == maxTries) {
          throw e;
        }
      }
    }
  }

  protected ReportDownloaderInterface getReportDownloaderInterface(BaseGoogleAdsConfig config)
    throws OAuthException, ValidationException {
    AdWordsSession session = getAdWordsSession(config);
    session.setReportingConfiguration(getReportingConfiguration(config));
    AdWordsServicesInterface adWordsServices = AdWordsServices.getInstance();
    return adWordsServices.getUtility(session, ReportDownloaderInterface.class);
  }

  private ReportDefinition getReportDefinition(BaseGoogleAdsConfig config,
                                               String reportPreset)
    throws IOException {
    MultiReportBatchSourceGoogleAdsConfig multiReportConfig = null;
    BatchSourceGoogleAdsConfig singleReportConfig = null;
    ReportPreset preset = null;
    if (config instanceof MultiReportBatchSourceGoogleAdsConfig) {
      multiReportConfig = (MultiReportBatchSourceGoogleAdsConfig) config;
      ReportPresetHelper presetHelper = new ReportPresetHelper();
      preset = presetHelper.getReportPreset(reportPreset);
    }
    if (config instanceof BatchSourceGoogleAdsConfig) {
      singleReportConfig = (BatchSourceGoogleAdsConfig) config;
    }
    Selector selector = new Selector();
    if (preset != null) {
      selector.getFields().addAll(preset.getFields());
    } else if (singleReportConfig != null) {
      selector.getFields().addAll(singleReportConfig.getReportFields());
    }
    DateRange dateRange = new DateRange();
    dateRange.setMax(config.getEndDate());
    dateRange.setMin(config.getStartDate());
    selector.setDateRange(dateRange);

    // Create report definition.
    ReportDefinition reportDefinition = new ReportDefinition();
    reportDefinition.setDateRangeType(ReportDefinitionDateRangeType.CUSTOM_DATE);
    if (preset != null && multiReportConfig != null) {
      reportDefinition.setReportName(reportPreset);
      reportDefinition.setReportType(preset.getType());
      reportDefinition.setDownloadFormat(multiReportConfig.getReportFormat());
    } else if (singleReportConfig != null) {
      reportDefinition.setReportName(singleReportConfig.getReportType().value());
      reportDefinition.setReportType(singleReportConfig.getReportType());
      reportDefinition.setDownloadFormat(DownloadFormat.CSV);
    }
    reportDefinition.setSelector(selector);
    return reportDefinition;
  }
}
