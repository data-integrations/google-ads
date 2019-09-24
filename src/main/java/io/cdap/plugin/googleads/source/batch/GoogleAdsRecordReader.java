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
package io.cdap.plugin.googleads.source.batch;

import com.google.api.ads.adwords.axis.factory.AdWordsServices;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.client.reporting.ReportingConfiguration;
import com.google.api.ads.adwords.lib.factory.AdWordsServicesInterface;
import com.google.api.ads.adwords.lib.jaxb.v201809.DownloadFormat;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinition;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionDateRangeType;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.jaxb.v201809.Selector;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.adwords.lib.utils.v201809.ReportDownloaderInterface;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * RecordReader implementation, which reads report instances from Google adWords using
 * googleads-java-lib.
 */
public class GoogleAdsRecordReader extends RecordReader<NullWritable, StructuredRecord> {

  private static final Gson gson = new GsonBuilder().create();
  private List<StructuredRecord> reportStructure;
  private String report;
  private String[] heads;
  private Iterator<StructuredRecord> iterator;
  private StructuredRecord currentValue;


  @Override
  public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {

    Configuration conf = taskAttemptContext.getConfiguration();
    String configJson = conf.get(GoogleAdsInputFormatProvider.PROPERTY_CONFIG_JSON);
    GoogleAdsBatchSourceConfig googleAdsBatchSourceConfig = gson.fromJson(configJson, GoogleAdsBatchSourceConfig.class);

    downloadReport(googleAdsBatchSourceConfig);
    heads = googleAdsBatchSourceConfig.getReport_fields();
    buildReportStructure(googleAdsBatchSourceConfig.getSchema());

  }

  private void downloadReport(GoogleAdsBatchSourceConfig googleAdsBatchSourceConfig) throws IOException {
    AdWordsSession session = null;
    Credential credential = null;
    try {
      credential = new OfflineCredentials.Builder()
        .forApi(OfflineCredentials.Api.ADWORDS)
        .withClientSecrets("", "")//todo remove hardcod
        .withRefreshToken("")//todo remove hardcod
        .build()
        .generateCredential();
    } catch (OAuthException e) {
      e.printStackTrace();
    } catch (ValidationException e) {
      e.printStackTrace();
    }

    try {
      session =
        new AdWordsSession.Builder()
          .withClientCustomerId(googleAdsBatchSourceConfig.getClient_customer_id())
          .withDeveloperToken(googleAdsBatchSourceConfig.getDeveloper_token())
          .withOAuth2Credential(credential)
          .build();
    } catch (ValidationException e) {
      e.printStackTrace();
    }

    AdWordsServicesInterface adWordsServices = AdWordsServices.getInstance();


    Selector selector = new Selector();
    selector.getFields().addAll(Arrays.asList(googleAdsBatchSourceConfig.getReport_fields()));


    // Create report definition.
    ReportDefinition reportDefinition = new ReportDefinition();
    reportDefinition.setReportName("Criteria performance report #" + System.currentTimeMillis());
    reportDefinition.setDateRangeType(ReportDefinitionDateRangeType.LAST_30_DAYS);//todo CUSTOM_DATE selection
    reportDefinition.setReportType(ReportDefinitionReportType.fromValue(googleAdsBatchSourceConfig.getReport_type()));
    reportDefinition.setDownloadFormat(DownloadFormat.CSV);

    ReportingConfiguration reportingConfiguration =
      new ReportingConfiguration.Builder()
        .skipReportHeader(!googleAdsBatchSourceConfig.getIncludeReportHeader())
        .skipColumnHeader(!googleAdsBatchSourceConfig.getIncludeColumnHeader())
        .skipReportSummary(!googleAdsBatchSourceConfig.getIncludeReportSummary())
        .useRawEnumValues(googleAdsBatchSourceConfig.getUseRawEnumValues())
        .includeZeroImpressions(googleAdsBatchSourceConfig.getIncludeZeroImpressions())
        .build();
    session.setReportingConfiguration(reportingConfiguration);

    reportDefinition.setSelector(selector);

    ReportDownloaderInterface reportDownloader =
      adWordsServices.getUtility(session, ReportDownloaderInterface.class);


    try {
      ReportDownloadResponse response = reportDownloader.downloadReport(reportDefinition);
      report = response.getAsString();
    } catch (ReportException e) {
      e.printStackTrace();
    } catch (ReportDownloadResponseException e) {
      e.printStackTrace();
    }
  }

  private void buildReportStructure(Schema schema) {
    reportStructure = new ArrayList<>();
    String[] lines = report.split("\n");
    for (int i = 0; i < lines.length; i++) {
      StructuredRecord.Builder builder = StructuredRecord.builder(schema);
      String[] values = lines[i].split(",");
      for (int j = 0; j < values.length; j++) {
        builder.set(heads[j], values[j]);
      }
      reportStructure.add(builder.build());
    }
    iterator = reportStructure.listIterator();
  }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    if (iterator.hasNext()) {
      currentValue = iterator.next();
      return true;
    }
    return false;
  }

  @Override
  public NullWritable getCurrentKey() throws IOException, InterruptedException {
    return null;
  }

  @Override
  public StructuredRecord getCurrentValue() throws IOException, InterruptedException {
    return currentValue;
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {
    return 0;
  }

  @Override
  public void close() throws IOException {

  }
}
