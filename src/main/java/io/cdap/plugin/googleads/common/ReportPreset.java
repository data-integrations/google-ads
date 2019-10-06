package io.cdap.plugin.googleads.common;

import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;

import java.util.List;

/**
 *  Report preset definition
 */
public class ReportPreset {

  private ReportDefinitionReportType type;
  private List<String> fields;

  public ReportPreset(ReportDefinitionReportType type, List<String> fields) {
    this.type = type;
    this.fields = fields;
  }

  public ReportDefinitionReportType getType() {
    return type;
  }

  public List<String> getFields() {
    return fields;
  }
}
