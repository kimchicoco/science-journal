/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
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
package com.google.android.apps.forscience.whistlepunk.api.scalarinput;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.google.android.apps.forscience.whistlepunk.ImageViewSensorAnimationBehavior;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.SensorAnimationBehavior;
import com.google.android.apps.forscience.whistlepunk.SensorAppearance;
import com.google.android.apps.forscience.whistlepunk.data.nano.GoosciScalarInput;
import com.google.android.apps.forscience.whistlepunk.metadata.ExternalSensorSpec;
import com.google.common.base.Preconditions;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;

public class ScalarInputSpec extends ExternalSensorSpec {
  public static final String TYPE = "ScalarInput";
  private static final String TAG = "ScalarInputSpec";

  private String name;
  private GoosciScalarInput.ScalarInputConfig config;

  public ScalarInputSpec(
      String sensorName,
      String serviceId,
      String address,
      SensorBehavior behavior,
      SensorAppearanceResources ids,
      String deviceId,
      int orderInExperimentApiSensors) {
    name = sensorName;
    config = new GoosciScalarInput.ScalarInputConfig();
    config.serviceId = Preconditions.checkNotNull(serviceId);
    config.address = address;
    config.orderInExperimentApiSensors = orderInExperimentApiSensors;
    config.deviceId = deviceId;

    if (behavior != null) {
      if (behavior.loggingId != null) {
        config.loggingId = behavior.loggingId;
      }
      config.shouldShowOptionsOnConnect = behavior.shouldShowSettingsOnConnect;
      config.expectedSamplesPerSecond = behavior.expectedSamplesPerSecond;
    }

    writeResourceIds(config, ids);
  }

  public ScalarInputSpec(
      String sensorName,
      String serviceId,
      String address,
      SensorBehavior behavior,
      SensorAppearanceResources ids,
      String deviceId) {
    // TODO: inline?
    this(sensorName, serviceId, address, behavior, ids, deviceId, 0);
  }

  private void writeResourceIds(
      GoosciScalarInput.ScalarInputConfig config, SensorAppearanceResources ids) {
    if (ids != null) {
      config.iconId = ids.iconId;
      config.units = emptyIfNull(ids.units);
      config.shortDescription = emptyIfNull(ids.shortDescription);
    }
  }

  private String emptyIfNull(String s) {
    return s == null ? "" : s;
  }

  public ScalarInputSpec(String sensorName, byte[] config) {
    name = sensorName;
    this.config = parse(config);
  }

  @Nullable
  private GoosciScalarInput.ScalarInputConfig parse(byte[] config) {
    try {
      return GoosciScalarInput.ScalarInputConfig.parseFrom(config);
    } catch (InvalidProtocolBufferNanoException e) {
      if (Log.isLoggable(TAG, Log.ERROR)) {
        Log.e(TAG, "error parsing config", e);
      }
    }
    return null;
  }

  @Override
  public SensorAppearance getSensorAppearance() {
    // TODO: better icon?
    return new EmptySensorAppearance() {
      @Override
      public String getName(Context context) {
        return name;
      }

      @Override
      public Drawable getIconDrawable(Context context) {
        if (config.iconId <= 0) {
          return getDefaultIcon(context);
        }
        try {
          // TODO: test this?
          return getApiAppResources(context).getDrawable(config.iconId);
        } catch (PackageManager.NameNotFoundException e) {
          if (Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, "Package has gone missing: " + getPackageId());
          }
          return getDefaultIcon(context);
        } catch (Resources.NotFoundException e) {
          if (Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, "Didn't find icon", e);
          }
          return getDefaultIcon(context);
        }
      }

      private Drawable getDefaultIcon(Context context) {
        return context.getResources().getDrawable(getDefaultIconId());
      }

      @Override
      public String getUnits(Context context) {
        return config.units;
      }

      @Override
      public String getShortDescription(Context context) {
        return config.shortDescription;
      }

      private Resources getApiAppResources(Context context)
          throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getResourcesForApplication(getPackageId());
      }

      @Override
      public SensorAnimationBehavior getSensorAnimationBehavior() {
        return new ImageViewSensorAnimationBehavior(
            R.drawable.api_level_drawable, ImageViewSensorAnimationBehavior.TYPE_RELATIVE_SCALE);
      }
    };
  }

  private String getPackageId() {
    return getPackageId(ScalarInputSpec.this.getServiceId());
  }

  public static Drawable getServiceDrawable(String serviceId, Context context) {
    try {
      return context.getPackageManager().getApplicationIcon(getPackageId(serviceId));
    } catch (PackageManager.NameNotFoundException e) {
      return context.getResources().getDrawable(R.drawable.ic_api_01_white_24dp);
    }
  }

  public int getDefaultIconId() {
    switch (config.orderInExperimentApiSensors % 4) {
      case 0:
        return R.drawable.ic_api_01_white_24dp;
      case 1:
        return R.drawable.ic_api_02_white_24dp;
      case 2:
        return R.drawable.ic_api_03_white_24dp;
      case 3:
        return R.drawable.ic_api_04_white_24dp;
      default:
        // Should never happen, if math works.
        return R.drawable.ic_api_01_white_24dp;
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getAddress() {
    return InputDeviceSpec.joinAddresses(getServiceId(), getSensorAddressInService());
  }

  public String getSensorAddressInService() {
    return config.address;
  }

  @Override
  public byte[] getConfig() {
    return MessageNano.toByteArray(config);
  }

  @Override
  public boolean shouldShowOptionsOnConnect() {
    return config.shouldShowOptionsOnConnect;
  }

  @Override
  public String getDeviceAddress() {
    return makeApiDeviceAddress(getServiceId(), getDeviceId());
  }

  @NonNull
  public static String makeApiDeviceAddress(String serviceId, String deviceId) {
    return InputDeviceSpec.joinAddresses(serviceId, deviceId);
  }

  public String getDeviceId() {
    return config.deviceId;
  }

  public String getServiceId() {
    return config.serviceId;
  }

  private static String getPackageId(String serviceId) {
    // TODO: write test!
    return serviceId.split("/")[0];
  }

  @Override
  public String getLoggingId() {
    return InputDeviceSpec.joinAddresses(getServiceId(), config.loggingId);
  }

  public float getExpectedSamplesPerSecond() {
    return config.expectedSamplesPerSecond;
  }

  @Override
  public ExternalSensorSpec maybeAdjustBeforePairing(int numPairedBeforeAdded) {
    if (numPairedBeforeAdded == config.orderInExperimentApiSensors) {
      return this;
    }
    GoosciScalarInput.ScalarInputConfig copyConfig = parse(MessageNano.toByteArray(config));
    copyConfig.orderInExperimentApiSensors = numPairedBeforeAdded;
    return new ScalarInputSpec(name, MessageNano.toByteArray(copyConfig));
  }
}
