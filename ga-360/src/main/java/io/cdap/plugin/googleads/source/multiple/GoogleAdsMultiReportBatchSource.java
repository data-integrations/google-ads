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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.data.batch.Input;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.api.dataset.lib.KeyValue;
import io.cdap.cdap.etl.api.Emitter;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.PipelineConfigurer;
import io.cdap.cdap.etl.api.batch.BatchSource;
import io.cdap.cdap.etl.api.batch.BatchSourceContext;
import io.cdap.plugin.common.LineageRecorder;
import org.apache.hadoop.io.NullWritable;

import java.util.stream.Collectors;

/**
 * Plugin read Google AdWords report in batch
 */
@Plugin(type = BatchSource.PLUGIN_TYPE)
@Name(GoogleAdsMultiReportBatchSource.NAME)
@Description("Reads Google AdWords report in batch")
public class GoogleAdsMultiReportBatchSource extends BatchSource<NullWritable, StructuredRecord, StructuredRecord> {

  private final MultiReportBatchSourceGoogleAdsConfig config;

  public static final String NAME = "GoogleAdsMultiReportBatchSource";

  public GoogleAdsMultiReportBatchSource(MultiReportBatchSourceGoogleAdsConfig config) {
    this.config = config;
  }

  @Override
  public void configurePipeline(PipelineConfigurer pipelineConfigurer) {
    FailureCollector failureCollector = pipelineConfigurer.getStageConfigurer().getFailureCollector();
    config.validate(failureCollector);
    failureCollector.getOrThrowException();
    pipelineConfigurer.getStageConfigurer().setOutputSchema(config.getSchema());
  }

  public void prepareRun(BatchSourceContext context) throws Exception {
    LineageRecorder lineageRecorder = new LineageRecorder(context, config.referenceName);
    lineageRecorder.createExternalDataset(config.getSchema());
    lineageRecorder.recordRead("Reads", "Reading Google AdWords report",
                               config.getSchema().getFields().stream().map(Schema.Field::getName)
      .collect(Collectors.toList()));
    context.setInput(Input.of(NAME, new GoogleAdsMultiReportInputFormatProvider(config)));
  }

  @Override
  public void transform(KeyValue<NullWritable, StructuredRecord> input, Emitter<StructuredRecord> emitter) {
    emitter.emit(input.getValue());
  }
}
