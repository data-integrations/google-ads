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
import io.cdap.plugin.googleads.source.batch.GoogleAdsBatchSourceConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class to incorporate GoogleAds api interaction
 */
public class GoogleAdsHelper {

  String downloadReport(GoogleAdsBatchSourceConfig googleAdsBatchSourceConfig)
    throws OAuthException, ValidationException, IOException {

    AdWordsSession session = getAdWordsSession(googleAdsBatchSourceConfig);
    session.setReportingConfiguration(getReportingConfiguration(googleAdsBatchSourceConfig));

    AdWordsServicesInterface adWordsServices = AdWordsServices.getInstance();

    ReportDownloaderInterface reportDownloader =
      adWordsServices.getUtility(session, ReportDownloaderInterface.class);

    try {
      ReportDownloadResponse response = reportDownloader.downloadReport(
        getReportDefinition(googleAdsBatchSourceConfig));
      return response.getAsString();
    } catch (ReportException | ReportDownloadResponseException e) {
      throw new IOException(e);
    }
  }

  private ReportDefinition getReportDefinition(GoogleAdsBatchSourceConfig googleAdsBatchSourceConfig) {
    Selector selector = new Selector();
    selector.getFields().addAll(googleAdsBatchSourceConfig.getReportFields());
    DateRange dateRange = new DateRange();
    dateRange.setMax(googleAdsBatchSourceConfig.getEndDate());
    dateRange.setMin(googleAdsBatchSourceConfig.getStartDate());
    selector.setDateRange(dateRange);

    // Create report definition.
    ReportDefinition reportDefinition = new ReportDefinition();
    reportDefinition.setReportName("Criteria performance report #" + System.currentTimeMillis());
    reportDefinition.setDateRangeType(ReportDefinitionDateRangeType.CUSTOM_DATE);
    reportDefinition.setReportType(googleAdsBatchSourceConfig.getReportType());
    reportDefinition.setDownloadFormat(DownloadFormat.CSV);
    reportDefinition.setSelector(selector);
    return reportDefinition;
  }

  private ReportingConfiguration getReportingConfiguration(GoogleAdsBatchSourceConfig googleAdsBatchSourceConfig) {
    return new ReportingConfiguration.Builder()
      .skipReportHeader(true)
      .skipColumnHeader(true)
      .skipReportSummary(!googleAdsBatchSourceConfig.includeReportSummary)
      .useRawEnumValues(googleAdsBatchSourceConfig.useRawEnumValues)
      .includeZeroImpressions(googleAdsBatchSourceConfig.includeZeroImpressions)
      .build();
  }

  public AdWordsSession getAdWordsSession(GoogleAdsBatchSourceConfig googleAdsBatchSourceConfig)
    throws OAuthException, ValidationException {
    AdWordsSession session;
    Credential credential = new OfflineCredentials.Builder()
      .forApi(OfflineCredentials.Api.ADWORDS)
      .withClientSecrets(googleAdsBatchSourceConfig.clientId, googleAdsBatchSourceConfig.clientSecret)
      .withRefreshToken(googleAdsBatchSourceConfig.refreshToken)
      .build()
      .generateCredential();

    session = new AdWordsSession.Builder()
      .withClientCustomerId(googleAdsBatchSourceConfig.clientCustomerId)
      .withDeveloperToken(googleAdsBatchSourceConfig.developerToken)
      .withOAuth2Credential(credential)
      .build();
    return session;
  }

  public List<StructuredRecord> buildReportStructure(GoogleAdsBatchSourceConfig config)
    throws IOException, OAuthException, ValidationException {
    String report = downloadReport(config);
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

  public ReportDefinitionField[] getReportDefinitionFields(GoogleAdsBatchSourceConfig googleAdsBatchSourceConfig)
    throws OAuthException, ValidationException, RemoteException {
    AdWordsSession session = getAdWordsSession(googleAdsBatchSourceConfig);
    AdWordsServicesInterface adWordsServices = AdWordsServices.getInstance();
    // Get the ReportDefinitionService.
    ReportDefinitionServiceInterface reportDefinitionService =
      adWordsServices.get(session, ReportDefinitionServiceInterface.class);

    // Get report fields.
    return reportDefinitionService
      .getReportFields(ReportDefinitionReportType.fromValue(googleAdsBatchSourceConfig.getReportType().value()));
  }
}
