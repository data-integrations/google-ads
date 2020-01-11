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

import io.cdap.plugin.googleads.common.GoogleAdsReportSplit;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.util.Collections;
import java.util.List;

/**
 * InputFormat for mapreduce job, which provides a single split of data.
 */
public class GoogleAdsInputFormat extends InputFormat {
  @Override
  public List<InputSplit> getSplits(JobContext jobContext) {
    return Collections.singletonList(new GoogleAdsReportSplit("noop"));
  }

  @Override
  public RecordReader createRecordReader(InputSplit inputSplit, TaskAttemptContext taskAttemptContext) {
    return new GoogleAdsRecordReader();
  }
}
