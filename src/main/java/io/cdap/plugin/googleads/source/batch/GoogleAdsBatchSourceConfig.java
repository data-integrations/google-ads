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

import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.plugin.common.ReferencePluginConfig;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Provides all required configuration for reading Google AdWords reports
 */
public class GoogleAdsBatchSourceConfig extends ReferencePluginConfig {

  public GoogleAdsBatchSourceConfig(String referenceName) {
    super(referenceName);
  }

  public String getOauth2_access_token() {
    return oauth2_access_token;
  }

  public String getDeveloper_token() {
    return developer_token;
  }

  public String getClient_customer_id() {
    return client_customer_id;
  }

  public String getStart_date() {
    return start_date;
  }

  public String getEnd_date() {
    return end_date;
  }

  public String getReport_format() {
    return report_format;
  }

  @Nullable
  public Boolean getIncludeReportHeader() {
    return includeReportHeader;
  }

  @Nullable
  public Boolean getIncludeColumnHeader() {
    return includeColumnHeader;
  }

  @Nullable
  public Boolean getIncludeReportSummary() {
    return includeReportSummary;
  }

  @Nullable
  public Boolean getUseRawEnumValues() {
    return useRawEnumValues;
  }

  @Nullable
  public Boolean getIncludeZeroImpressions() {
    return includeZeroImpressions;
  }

  public String getReport_type() {
    return report_type;
  }

  public String[] getReport_fields() {
    return report_fields.split(",");
  }

  @Name("oauth2_access_token")
  @Description("Authorization token")
  @Macro
  protected String oauth2_access_token;

  @Name("developer_token")
  @Description("Developer token")
  @Macro
  protected String developer_token;

  @Name("client_customer_id")
  @Description("Customer ID")
  @Macro
  protected String client_customer_id;

  @Name("start_date")
  @Description("Start Date")
  @Macro
  protected String start_date;

  @Name("end_date")
  @Description("End Date")
  @Macro
  protected String end_date;

  @Name("report_format")
  @Description("Format")
  @Macro
  protected String report_format;

  @Name("includeReportHeader")
  @Description("Include Report Header")
  @Nullable
  @Macro
  protected Boolean includeReportHeader;

  @Name("includeColumnHeader")
  @Description("Include Column Header")
  @Nullable
  @Macro
  protected Boolean includeColumnHeader;

  @Name("includeReportSummary")
  @Description("Include Report Summary")
  @Nullable
  @Macro
  protected Boolean includeReportSummary;

  @Name("useRawEnumValues")
  @Description("Use Raw Enum Values")
  @Nullable
  @Macro
  protected Boolean useRawEnumValues;

  @Name("includeZeroImpressions")
  @Description("Include Zero Impressions")
  @Nullable
  @Macro
  protected Boolean includeZeroImpressions;

  @Name("report_type")
  @Description("Report type")
  @Macro
  protected String report_type;

  @Name("report_fields")
  @Description("Fields")
  @Macro
  protected String report_fields;


  public void validate(FailureCollector failureCollector) {
    if (Strings.isNullOrEmpty(oauth2_access_token)) {
      failureCollector
        .addFailure("Authorization token must be not empty.", "Enter valid value.")
        .withConfigProperty(oauth2_access_token);
    }
    if (Strings.isNullOrEmpty(developer_token)) {
      failureCollector
        .addFailure("Developer token must be not empty.", "Enter valid value.")
        .withConfigProperty(developer_token);
    }
    if (Strings.isNullOrEmpty(client_customer_id)) {
      failureCollector
        .addFailure("Customer ID must be not empty.", "Enter valid value.")
        .withConfigProperty(client_customer_id);
    }
    if (Strings.isNullOrEmpty(start_date)) {
      failureCollector
        .addFailure("Start Date must be not empty.", "Enter valid value.")
        .withConfigProperty(start_date);
    }
    if (Strings.isNullOrEmpty(end_date)) {
      failureCollector
        .addFailure("End Date must be not empty.", "Enter valid value.")
        .withConfigProperty(end_date);
    }
    if (includeReportHeader == null) {
      failureCollector
        .addFailure("Include Report Header must be not empty.", "Enter valid value.");
    }
    if (includeColumnHeader == null) {
      failureCollector
        .addFailure("Include Column Header must be not empty.", "Enter valid value.");
    }
    if (includeReportSummary == null) {
      failureCollector
        .addFailure("Include Report Summary must be not empty.", "Enter valid value.");
    }
    if (useRawEnumValues == null) {
      failureCollector
        .addFailure("Use Raw Enum Values must be not empty.", "Enter valid value.");
    }
    if (includeZeroImpressions == null) {
      failureCollector
        .addFailure("Include Zero Impressions must be not empty.", "Enter valid value.");
    }
    if (Strings.isNullOrEmpty(report_fields)) {
      failureCollector
        .addFailure("Fields must be not empty.", "Enter valid value.")
        .withConfigProperty(report_fields);
    }
  }

  public Schema getSchema() {
    Set<Schema.Field> schemaFields = new HashSet<Schema.Field>();
    for (String name : report_fields.split(",")) {
      schemaFields.add(Schema.Field.of(name, Schema.nullableOf(Schema.of(Schema.Type.STRING))));
    }

    return Schema.recordOf(
      "GoogleAdsRecords",
      schemaFields);
  }
}
