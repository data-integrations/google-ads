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

import com.google.api.ads.adwords.lib.jaxb.v201809.DownloadFormat;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.googleads.common.BaseGoogleAdsConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides all required configuration for reading Google AdWords report
 */
public class MultiReportBatchSourceGoogleAdsConfig extends BaseGoogleAdsConfig {

  private static final String REPORT_FORMAT = "reportFormat";
  public static final String INCLUDE_REPORT_HEADER = "includeReportHeader";
  public static final String INCLUDE_COLUMN_HEADER = "includeColumnHeader";

  @Name(REPORT_FORMAT)
  @Description("Report format")
  @Macro
  public String reportFormat;
  @Name(INCLUDE_REPORT_HEADER)
  @Description("Specifies whether to include a header row to a report." +
    " This row contains the report name and date range.")
  @Macro
  public Boolean includeReportHeader;
  @Name(INCLUDE_COLUMN_HEADER)
  @Description("Specifies whether to include a header row to a report. This row contains report field names.")
  @Macro
  public Boolean includeColumnHeader;

  public MultiReportBatchSourceGoogleAdsConfig(String referenceName) {
    super(referenceName);
  }

  public DownloadFormat getReportFormat() {
    return DownloadFormat.fromValue(reportFormat);
  }

  @Override
  public void validate(FailureCollector failureCollector) {
    super.validate(failureCollector);
    validateFormat(failureCollector);
  }

  private void validateFormat(FailureCollector failureCollector) {
    if (containsMacro(REPORT_FORMAT)) {
      return;
    }
    try {
      getReportFormat();
    } catch (IllegalArgumentException e) {
      failureCollector.addFailure(String.format("reportFormat '%s' is not a valid report format", reportFormat),
                                  null).withConfigProperty(REPORT_FORMAT);
    }
  }

  public Schema getSchema() {
    Set<Schema.Field> schemaFields = new HashSet<>();
    schemaFields.add(Schema.Field.of("report_name", Schema.nullableOf(Schema.of(Schema.Type.STRING))));
    schemaFields.add(Schema.Field.of("report", Schema.nullableOf(Schema.of(Schema.Type.STRING))));


    return Schema.recordOf(
      "GoogleAdsReports",
      schemaFields);
  }
}
