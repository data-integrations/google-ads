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
package io.cdap.plugin.doubleclick.source.reporting.common;

import com.google.api.services.dfareporting.model.File;
import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.plugin.doubleclick.source.reporting.batch.DoubleClickReportingBatchSourceConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is helper class for transforming CSV report to {@link StructuredRecord}.
 */
public class ReportTransformer {

  /**
   * Transforms report in CSV format to {@link StructuredRecord} instance accordingly to given schema.
   */
  public static List<StructuredRecord> transformReportFromCsv(DoubleClickReportingBatchSourceConfig config)
    throws IOException, InterruptedException {
    Long reportId = (config.getReportId() == null) ? ReportHelper.createReport(config) : config.getReportId();
    File report = ReportHelper.runReport(config, reportId);
    InputStream reportAsInputStream = ReportHelper.downloadReport(config, report);
    CSVParser csvParser = CSVParser.parse(reportAsInputStream, Charset.defaultCharset(), CSVFormat.DEFAULT);

    List<StructuredRecord> reportStructure = new ArrayList<>();
    List<String> reportFields = config.getSchema()
      .getFields()
      .stream()
      .map(Schema.Field::getName)
      .collect(Collectors.toList());

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
}
