// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Format;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.container.NalUnitUtil;
import java.util.List;

/**
 * Reads the color transfer signaled in the VUI of the HEVC SPS (ziptv2 fork).
 *
 * <p>Media3 does not always propagate it to {@code Format.colorInfo}: for Dolby Vision profile 8 in
 * Matroska {@code colorInfo} stays null, while {@code initializationData} still holds the HEVC
 * VPS/SPS/PPS in Annex-B. This class parses the first SPS of that buffer.
 *
 * <p>Pure function: any malformed or truncated input yields null, never an exception.
 */
final class HevcSpsColorTransfer {
  private HevcSpsColorTransfer() {}

  /**
   * Returns the {@code C.COLOR_TRANSFER_*} value of the first SPS found in {@code
   * initializationData}, or null if there is no SPS, no colour description in its VUI or the data
   * cannot be parsed.
   */
  // TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
  @UnstableApi
  @Nullable
  static Long read(@NonNull List<byte[]> initializationData) {
    for (byte[] data : initializationData) {
      Long transfer = readFromAnnexB(data);
      if (transfer != null) {
        return transfer;
      }
    }
    return null;
  }

  @Nullable
  private static Long readFromAnnexB(@NonNull byte[] data) {
    try {
      int start = NalUnitUtil.findNalUnit(data, 0, data.length, new boolean[3]);
      while (start < data.length) {
        int nalOffset = start + 3;
        int next = NalUnitUtil.findNalUnit(data, nalOffset, data.length, new boolean[3]);
        if (nalOffset < data.length
            && NalUnitUtil.getH265NalUnitType(data, start)
                == NalUnitUtil.H265_NAL_UNIT_TYPE_SPS) {
          // Only the first SPS is considered.
          int transfer = NalUnitUtil.parseH265SpsNalUnit(data, nalOffset, next, null).colorTransfer;
          return transfer != Format.NO_VALUE ? (long) transfer : null;
        }
        start = next;
      }
    } catch (RuntimeException e) {
      // Malformed or truncated parameter sets.
    }
    return null;
  }
}
