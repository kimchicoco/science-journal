/*
 *  Copyright 2017 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.google.android.apps.forscience.whistlepunk.filemetadata;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.android.apps.forscience.whistlepunk.PictureUtils;
import com.google.android.apps.forscience.whistlepunk.accounts.AppAccount;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciCaption;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciCaption.Caption;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciLabel.Label.ValueType;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciPictureLabelValue;
import com.google.android.apps.forscience.whistlepunk.metadata.GoosciTextLabelValue;
import com.google.android.apps.forscience.whistlepunk.metadata.nano.GoosciLabel;
import com.google.android.apps.forscience.whistlepunk.metadata.nano.GoosciSensorTriggerLabelValue;
import com.google.android.apps.forscience.whistlepunk.metadata.nano.GoosciSnapshotValue;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.migration.nano2lite.runtime.MigrateAs;
import com.google.protobuf.migration.nano2lite.runtime.MigrateAs.Destination;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import java.io.File;
import java.util.Comparator;

/**
 * A label, which is user- or app-generated metadata tagged with a particular timestamp. All changes
 * should be made using the getters and setters provided, rather than by getting the underlying
 * protocol buffer and making changes to that directly.
 */
public class Label implements Parcelable {
  public static final Comparator<Label> COMPARATOR_BY_TIMESTAMP =
      (first, second) -> Long.compare(first.getTimeStamp(), second.getTimeStamp());

  private static final String TAG = "label";
  private GoosciLabel.Label label;

  /** Loads an existing label from a proto. */
  public static Label fromLabel(GoosciLabel.Label goosciLabel) {
    return new Label(goosciLabel);
  }

  /** Creates a new label with no content. */
  public static Label newLabel(long creationTimeMs, ValueType valueType) {
    return new Label(creationTimeMs, java.util.UUID.randomUUID().toString(), valueType);
  }

  // TODO(b/110156716): remove this method.
  /** Creates a new label with the specified label value. */
  public static Label newLabelWithValue(
      long creationTimeMs, ValueType type, MessageNano data, GoosciCaption.Caption caption) {
    Label result = new Label(creationTimeMs, java.util.UUID.randomUUID().toString(), type);
    result.setLabelProtoData(data);
    result.setCaption(caption);
    return result;
  }

  /** Creates a new label with the specified label value. */
  public static Label newLabelWithValue(
      long creationTimeMs, ValueType type, MessageLite data, GoosciCaption.Caption caption) {
    Label result = new Label(creationTimeMs, java.util.UUID.randomUUID().toString(), type);
    result.setLabelProtoData(data);
    result.setCaption(caption);
    return result;
  }

  // TODO(b/110156716): remove this method.
  public static Label fromUuidAndValue(
      long creationTimeMs, String uuid, ValueType type, MessageNano data) {
    Label result = new Label(creationTimeMs, uuid, type);
    result.setLabelProtoData(data);
    return result;
  }

  public static Label fromUuidAndValue(
      long creationTimeMs, String uuid, ValueType type, MessageLite data) {
    Label result = new Label(creationTimeMs, uuid, type);
    result.setLabelProtoData(data);
    return result;
  }

  /** Creates a deep copy of an existing label. The creation time and label ID will be different. */
  public static Label copyOf(Label label) {
    Parcel parcel = Parcel.obtain();
    label.writeToParcel(parcel, 0);
    parcel.setDataPosition(0);
    Label result = Label.CREATOR.createFromParcel(parcel);
    result.getLabelProto().creationTimeMs = System.currentTimeMillis();
    result.getLabelProto().labelId = java.util.UUID.randomUUID().toString();
    if (result.getLabelProto().caption != null) {
      Caption newCaption =
          result.getLabelProto().caption.toBuilder()
              .setLastEditedTimestamp(System.currentTimeMillis())
              .build();
      result.getLabelProto().caption = newCaption;
    }
    return result;
  }

  private Label(GoosciLabel.Label goosciLabel) {
    label = goosciLabel;
  }

  private Label(long creationTimeMs, String labelId, ValueType valueType) {
    @MigrateAs(Destination.BUILDER)
    GoosciLabel.Label label1 = new GoosciLabel.Label();
    label1.timestampMs = creationTimeMs;
    label1.creationTimeMs = creationTimeMs;
    label1.labelId = labelId;
    label1.type = valueType;
    label = label1;
  }

  protected Label(Parcel in) {
    int serializedSize = in.readInt();
    // readByteArray(byte[]) appears to be broken in robolectric currently
    // createByteArray() is an alternative
    // byte[] serialized = new byte[serializedSize];
    // in.readByteArray(serialized);
    byte[] serialized = in.createByteArray();
    try {
      label = GoosciLabel.Label.parseFrom(serialized);
    } catch (InvalidProtocolBufferNanoException ex) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "Couldn't parse label storage");
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int i) {
    parcel.writeInt(label.getSerializedSize());
    parcel.writeByteArray(MessageNano.toByteArray(label));
  }

  public static final Parcelable.Creator<Label> CREATOR =
      new Parcelable.Creator<Label>() {
        public Label createFromParcel(Parcel in) {
          return new Label(in);
        }

        @Override
        public Label[] newArray(int size) {
          return new Label[size];
        }
      };

  public GoosciLabel.Label getLabelProto() {
    return label;
  }

  public String getLabelId() {
    return label.labelId;
  }

  public long getTimeStamp() {
    return label.timestampMs;
  }

  public void setTimestamp(long timestampMs) {
    label.timestampMs = timestampMs;
  }

  public long getCreationTimeMs() {
    return label.creationTimeMs;
  }

  // You cannot edit the timestamp of some labels, like Snapshot and Trigger labels.
  public boolean canEditTimestamp() {
    return (label.type != ValueType.SNAPSHOT && label.type != ValueType.SENSOR_TRIGGER);
  }

  public String getCaptionText() {
    if (label.caption == null) {
      return "";
    }
    return label.caption.getText();
  }

  public void setCaption(GoosciCaption.Caption caption) {
    label.caption = caption;
  }

  public ValueType getType() {
    return label.type;
  }

  /**
   * Gets the GoosciTextLabelValue.TextLabelValue for this label. If changes are made, this needs to
   * be re-set on the Label for them to be saved.
   */
  public GoosciTextLabelValue.TextLabelValue getTextLabelValue() {
    if (label.type == ValueType.TEXT) {
      try {
        return GoosciTextLabelValue.TextLabelValue.parseFrom(
            label.protoData, ExtensionRegistryLite.getGeneratedRegistry());
      } catch (InvalidProtocolBufferException e) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
          Log.e(TAG, e.getMessage());
        }
      }
    } else {
      throwLabelValueException("TextLabelValue", label.type);
    }
    return null;
  }

  /**
   * Gets the PictureLabelValue for this label. If changes are made, this needs to be re-set on the
   * Label for them to be saved.
   */
  public GoosciPictureLabelValue.PictureLabelValue getPictureLabelValue() {
    if (label.type == ValueType.PICTURE) {
      try {
        return GoosciPictureLabelValue.PictureLabelValue.parseFrom(
            label.protoData, ExtensionRegistryLite.getGeneratedRegistry());
      } catch (InvalidProtocolBufferException e) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
          Log.e(TAG, e.getMessage());
        }
      }
    } else {
      throwLabelValueException("PictureLabelValue", label.type);
    }
    return null;
  }

  /**
   * Gets the SensorTriggerLabelValue for this label. If changes are made, this needs to be re-set
   * on the Label for them to be saved.
   */
  public GoosciSensorTriggerLabelValue.SensorTriggerLabelValue getSensorTriggerLabelValue() {
    if (label.type == ValueType.SENSOR_TRIGGER) {
      try {
        return GoosciSensorTriggerLabelValue.SensorTriggerLabelValue.parseFrom(label.protoData);
      } catch (InvalidProtocolBufferNanoException e) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
          Log.e(TAG, e.getMessage());
        }
      }
    } else {
      throwLabelValueException("SensorTriggerLabelValue", label.type);
    }
    return null;
  }

  /**
   * Gets the SnapshotLabelValue for this label. If changes are made, this needs to be re-set on the
   * Label for them to be saved.
   */
  @MigrateAs(Destination.EITHER)
  public GoosciSnapshotValue.SnapshotLabelValue getSnapshotLabelValue() {
    if (label.type == ValueType.SNAPSHOT) {
      try {
        return GoosciSnapshotValue.SnapshotLabelValue.parseFrom(label.protoData);
      } catch (InvalidProtocolBufferNanoException e) {
        if (Log.isLoggable(TAG, Log.ERROR)) {
          Log.e(TAG, e.getMessage());
        }
      }
    } else {
      throwLabelValueException("SnapshotLabelValue", label.type);
    }
    return null;
  }

  // TODO(b/110156716): remove this method.
  /**
   * Sets the proto data and type on this label. This must be done in order to save changes back to
   * the label that occur on the protoData field.
   */
  public void setLabelProtoData(MessageNano data) {
    label.protoData = MessageNano.toByteArray(data);
  }

  /**
   * Sets the proto data and type on this label. This must be done in order to save changes back to
   * the label that occur on the protoData field.
   */
  public void setLabelProtoData(MessageLite data) {
    label.protoData = data.toByteArray();
  }

  /** Deletes any assets associated with this label */
  public void deleteAssets(Context context, AppAccount appAccount, String experimentId) {
    if (label.type == ValueType.PICTURE) {
      File file =
          new File(
              PictureUtils.getExperimentImagePath(
                  context, appAccount, experimentId, getPictureLabelValue().getFilePath()));
      boolean deleted = file.delete();
      if (!deleted && Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Could not delete " + file.toString());
      }
    }
  }

  @Override
  public String toString() {
    return label.labelId
        + ": time: "
        + label.timestampMs
        + ", type:"
        + getDebugTypeString()
        + ", data: "
        + getDebugLabelValue();
  }

  private String getDebugTypeString() {
    switch (label.type) {
      case TEXT:
        return "TEXT";
      case PICTURE:
        return "PICTURE";
      case SENSOR_TRIGGER:
        return "TRIGGER";
      case SNAPSHOT:
        return "SNAPSHOT";
      default:
        return "???";
    }
  }

  private Object getDebugLabelValue() {
    switch (label.type) {
      case TEXT:
        return getTextLabelValue();
      case PICTURE:
        return getPictureLabelValue();
      case SENSOR_TRIGGER:
        return getSensorTriggerLabelValue();
      case SNAPSHOT:
        return getSnapshotLabelValue();
      default:
        return "unknown type";
    }
  }

  private static void throwLabelValueException(String protoToCreate, ValueType actualType) {
    throw new IllegalStateException(
        String.format(
            "Cannot get %s from label of type %s", protoToCreate, actualType.getNumber()));
  }
}
