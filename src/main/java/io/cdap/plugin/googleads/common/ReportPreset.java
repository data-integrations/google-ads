package io.cdap.plugin.googleads.common;

import java.util.List;

/**
 *  Report preset definition
 */
public class ReportPreset {

  private String type;
  private List<String> fields;

  public ReportPreset(String type, List<String> fields) {
    this.type = type;
    this.fields = fields;
  }

  public String getType() {
    return type;
  }

  public List<String> getFields() {
    return fields;
  }
}
