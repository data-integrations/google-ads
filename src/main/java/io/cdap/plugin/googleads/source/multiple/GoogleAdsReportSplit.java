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

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * GoogleAds split by report preset.
 */
public class GoogleAdsReportSplit extends InputSplit implements Writable {

  private String reportName;

  public GoogleAdsReportSplit() {
  }

  public GoogleAdsReportSplit(String reportName) {
    this.reportName = reportName;
  }

  @Override
  public void readFields(DataInput dataInput) throws IOException {
    int byteLength = dataInput.readInt();
    byte[] readArray = new byte[byteLength];
    dataInput.readFully(readArray);
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(readArray);
         ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
      reportName = (String) objectInputStream.readObject();
    } catch (ClassNotFoundException cfe) {
      throw new IOException("Exception while trying to deserialize object ", cfe);
    }
  }

  @Override
  public void write(DataOutput dataOutput) throws IOException {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(reportName);
      objectOutputStream.flush();
      byte[] objectBytes = byteArrayOutputStream.toByteArray();
      // we write the byte array length, to help initialize byte array during deserialization to read from DataInput
      dataOutput.writeInt(objectBytes.length);
      dataOutput.write(objectBytes);
    }
  }

  @Override
  public long getLength() {
    return 0;
  }

  @Override
  public String[] getLocations() {
    return new String[0];
  }

  public String getReportName() {
    return reportName;
  }
}
