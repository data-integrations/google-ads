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

import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
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
import java.util.Iterator;
import java.util.List;

/**
 * RecordReader implementation, which reads report instance from Google adWords using
 * googleads-java-lib.
 */
public class GoogleAdsRecordReader extends RecordReader<NullWritable, StructuredRecord> {

  protected static final Gson GSON = new GsonBuilder().create();

  protected Iterator<StructuredRecord> iterator;
  private StructuredRecord currentValue;

  @Override
  public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext)
    throws IOException, InterruptedException {
    Configuration conf = taskAttemptContext.getConfiguration();
    String configJson = conf.get(GoogleAdsInputFormatProvider.PROPERTY_CONFIG_JSON);
    BatchSourceGoogleAdsConfig googleAdsBatchSourceConfig = GSON.fromJson(configJson, BatchSourceGoogleAdsConfig.class);
    List<StructuredRecord> reportStructure;
    try {
      reportStructure = new GoogleAdsHelper().buildReportStructure(googleAdsBatchSourceConfig);
    } catch (OAuthException | ValidationException | ReportDownloadResponseException | ReportException e) {
      throw new RuntimeException("download report failed", e);
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
