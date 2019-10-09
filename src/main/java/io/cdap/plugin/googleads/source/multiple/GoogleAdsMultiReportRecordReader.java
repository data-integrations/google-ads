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

import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.plugin.googleads.common.GoogleAdsHelper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RecordReader implementation, which reads report instances from Google adWords using
 * googleads-java-lib.
 */
public class GoogleAdsMultiReportRecordReader extends RecordReader<NullWritable, StructuredRecord> {

  private static final Gson gson = new GsonBuilder().create();
  private final GoogleAdsHelper googleAdsHelper = new GoogleAdsHelper();
  private List<StructuredRecord> reportStructure = new ArrayList<>();
  private Iterator<StructuredRecord> iterator;
  private StructuredRecord currentValue;


  @Override
  public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext)
    throws IOException, InterruptedException {
    GoogleAdsReportSplit googleAdsReportSplit = (GoogleAdsReportSplit) inputSplit;
    Configuration conf = taskAttemptContext.getConfiguration();
    String configJson = conf.get(GoogleAdsMultiReportInputFormatProvider.PROPERTY_CONFIG_JSON);
    GoogleAdsMultiReportBatchSourceConfig config = gson.fromJson(
      configJson,
      GoogleAdsMultiReportBatchSourceConfig.class);
    String report;
    try {
      report = googleAdsHelper.downloadReport(config, googleAdsReportSplit.getReportName());
    } catch (OAuthException | ValidationException e) {
      throw new IOException(e);
    }
    StructuredRecord.Builder builder = StructuredRecord.builder(config.getSchema());
    builder.set("ReportName" , googleAdsReportSplit.getReportName());
    builder.set("Report" , report);
    reportStructure.add(builder.build());
    iterator = reportStructure.iterator();
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
