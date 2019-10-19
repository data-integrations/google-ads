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

import com.google.common.collect.Sets;
import io.cdap.cdap.api.data.schema.Schema;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class to map Double Click Report fields sets to final {@link Schema}.
 */
public class SchemaBuilder {

  public static Schema buildSchema(List<String> metrics, List<String> dimensions) {
    Set<String> schemaFields = Sets.newHashSet(metrics);
    schemaFields.addAll(dimensions);
    return Schema.recordOf("DoubleClickCampaignManagerReports",
                           schemaFields.stream()
                             .map(name -> Schema.Field.of(mapGoogleAnalyticsFieldToAvro(name),
                                                          Schema.nullableOf(Schema.of(Schema.Type.STRING))))
                             .collect(Collectors.toList()));
  }

  public static String mapGoogleAnalyticsFieldToAvro(String fieldName) {
    return fieldName.substring(fieldName.indexOf(":") + 1);
  }
}
