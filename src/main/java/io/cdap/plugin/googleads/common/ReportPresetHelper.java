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

package io.cdap.plugin.googleads.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Helper class to incorporate Report presets definition
 */
public class ReportPresetHelper {

  private static final Gson gson = new GsonBuilder().create();

  private static Map<String, ReportPreset> reportPresets = null;

  public Map<String, ReportPreset> getReportPresets() throws IOException {
    if (reportPresets == null) {
      initializePresets();
    }
    return reportPresets;
  }

  public ReportPreset getReportPreset(String name) throws IOException {
    if (reportPresets == null) {
      initializePresets();
    }
    return reportPresets.get(name);
  }

  private void initializePresets() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    Type type = new TypeToken<Map<String, ReportPreset>>() {
    }.getType();
    try (InputStream inputStream = classLoader.getResourceAsStream("presets.json")) {
      reportPresets = gson.fromJson(new InputStreamReader(inputStream), type);
    }
  }
}
