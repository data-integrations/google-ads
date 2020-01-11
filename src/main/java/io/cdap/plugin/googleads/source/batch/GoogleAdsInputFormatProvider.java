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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cdap.cdap.api.data.batch.InputFormatProvider;

import java.util.Map;

/**
 * InputFormatProvider used by cdap to provide configurations to mapreduce job
 */
public class GoogleAdsInputFormatProvider implements InputFormatProvider {

  private static final Gson gson = new GsonBuilder().create();
  private final Map<String, String> conf;
  public static final String PROPERTY_CONFIG_JSON = "cdap.google.ads.config";

  public GoogleAdsInputFormatProvider(GoogleAdsBatchSourceConfig config) {
    this.conf = new ImmutableMap.Builder<String, String>()
      .put(PROPERTY_CONFIG_JSON, gson.toJson(config))
      .build();
  }

  public String getInputFormatClassName() {
    return GoogleAdsInputFormat.class.getName();
  }

  public Map<String, String> getInputFormatConfiguration() {
    return conf;
  }
}
