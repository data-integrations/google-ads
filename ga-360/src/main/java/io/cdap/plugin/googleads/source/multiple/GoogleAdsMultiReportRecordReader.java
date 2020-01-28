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
package io.cdap.plugin.googleads.source.multiple;

import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.plugin.googleads.common.GoogleAdsHelper;
import io.cdap.plugin.googleads.common.GoogleAdsReportSplit;
import io.cdap.plugin.googleads.source.single.GoogleAdsRecordReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RecordReader implementation, which reads report instances from Google adWords using
 * googleads-java-lib.
 */
public class GoogleAdsMultiReportRecordReader extends GoogleAdsRecordReader {

  @Override
  public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext)
    throws IOException, InterruptedException {
    GoogleAdsReportSplit googleAdsReportSplit = (GoogleAdsReportSplit) inputSplit;
    Configuration conf = taskAttemptContext.getConfiguration();
    String configJson = conf.get(GoogleAdsMultiReportInputFormatProvider.PROPERTY_CONFIG_JSON);
    MultiReportBatchSourceGoogleAdsConfig config = GSON.fromJson(
      configJson,
      MultiReportBatchSourceGoogleAdsConfig.class);
    String report;
    try {
      report = new GoogleAdsHelper().downloadReport(config, googleAdsReportSplit.getReportName());
    } catch (OAuthException | ValidationException | ReportException | ReportDownloadResponseException e) {
      throw new RuntimeException("download report failed", e);
    }
    StructuredRecord.Builder builder = StructuredRecord.builder(config.getSchema());
    builder.set("report_name" , googleAdsReportSplit.getReportName());
    builder.set("report" , report);
    List<StructuredRecord> reportStructure = new ArrayList<>();
    reportStructure.add(builder.build());
    iterator = reportStructure.iterator();
  }
}
