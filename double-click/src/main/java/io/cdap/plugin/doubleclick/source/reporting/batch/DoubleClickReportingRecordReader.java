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
package io.cdap.plugin.doubleclick.source.reporting.batch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.plugin.doubleclick.source.reporting.common.ReportTransformer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;


/**
 * RecordReader implementation, which reads report instances from DFA Reporting and Trafficking API using
 * google-api-services-dfareporting.
 */
public class DoubleClickReportingRecordReader extends RecordReader<NullWritable, StructuredRecord> {

  private static final Gson gson = new GsonBuilder().create();

  private Iterator<StructuredRecord> iterator;
  private StructuredRecord currentValue;

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
    Configuration conf = context.getConfiguration();
    String configJson = conf.get(DoubleClickReportingFormatProvider.PROPERTY_CONFIG_JSON);
    DoubleClickReportingBatchSourceConfig config = gson.fromJson(configJson,
                                                                 DoubleClickReportingBatchSourceConfig.class);
    List<StructuredRecord> reportStructure = ReportTransformer.transformReportFromCsv(config);
    iterator = reportStructure.listIterator();
  }

  @Override
  public boolean nextKeyValue() {
    if (iterator.hasNext()) {
      currentValue = iterator.next();
      return true;
    }
    return false;
  }

  @Override
  public NullWritable getCurrentKey() {
    return null;
  }

  @Override
  public StructuredRecord getCurrentValue() {
    return currentValue;
  }

  @Override
  public float getProgress() {
    return 0;
  }

  @Override
  public void close() {

  }
}
