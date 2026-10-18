// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the Media3 {@link Format} metadata of video, audio and text tracks (ziptv2 fork).
 *
 * <p>All the methods are pure functions of a {@link Tracks} instance. {@link Format#NO_VALUE}
 * fields are reported as null.
 *
 * <p>{@code colorTransfer} is the one of {@link Format#colorInfo}; {@code bitstreamColorTransfer} is
 * the one signaled in the HEVC SPS VUI, read from {@code initializationData} (see {@link
 * HevcSpsColorTransfer}). The two are kept separate: Media3 leaves {@code colorInfo} null e.g. for
 * Dolby Vision profile 8 in Matroska.
 *
 * <p>Notes on Media3 behavior:
 *
 * <ul>
 *   <li>{@link Format} has no channel mask: only {@code channelCount} and {@code pcmEncoding}.
 *   <li>For text tracks parsed at extraction time (default since Media3 1.4) {@code sampleMimeType}
 *       is {@code application/x-media3-cues} and the original subtitle mime type is in {@code
 *       codecs}. The values are exposed as they are.
 * </ul>
 */
final class Media3TrackReader {
  private Media3TrackReader() {}

  /**
   * Returns the formats of all the video, audio and text tracks. Other group types (metadata,
   * image, ...) are ignored.
   *
   * <p>{@code groupIndex} is the index of the group among all the groups of {@code tracks}, the
   * same convention of the audio and video track selection.
   */
  // TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
  @UnstableApi
  @NonNull
  static List<Media3TrackFormatMessage> read(@NonNull Tracks tracks) {
    List<Media3TrackFormatMessage> result = new ArrayList<>();
    List<Tracks.Group> groups = tracks.getGroups();
    for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
      Tracks.Group group = groups.get(groupIndex);
      PlatformMedia3TrackType type = toPlatformType(group.getType());
      if (type == null) {
        continue;
      }
      for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
        result.add(
            toMessage(
                type,
                groupIndex,
                trackIndex,
                group.getTrackFormat(trackIndex),
                group.isTrackSelected(trackIndex),
                group.isTrackSupported(trackIndex)));
      }
    }
    return result;
  }

  @Nullable
  private static PlatformMedia3TrackType toPlatformType(int trackType) {
    switch (trackType) {
      case C.TRACK_TYPE_VIDEO:
        return PlatformMedia3TrackType.VIDEO;
      case C.TRACK_TYPE_AUDIO:
        return PlatformMedia3TrackType.AUDIO;
      case C.TRACK_TYPE_TEXT:
        return PlatformMedia3TrackType.TEXT;
      default:
        return null;
    }
  }

  @NonNull
  private static Media3TrackFormatMessage toMessage(
      @NonNull PlatformMedia3TrackType type,
      int groupIndex,
      int trackIndex,
      @NonNull Format format,
      boolean isSelected,
      boolean isSupported) {
    ColorInfo colorInfo = format.colorInfo;
    return new Media3TrackFormatMessage(
        type,
        (long) groupIndex,
        (long) trackIndex,
        isSelected,
        isSupported,
        (long) format.selectionFlags,
        (long) format.roleFlags,
        format.id,
        format.label,
        format.language,
        format.sampleMimeType,
        format.containerMimeType,
        format.codecs,
        intOrNull(format.bitrate),
        intOrNull(format.width),
        intOrNull(format.height),
        format.frameRate != Format.NO_VALUE ? (double) format.frameRate : null,
        intOrNull(format.channelCount),
        intOrNull(format.sampleRate),
        intOrNull(format.pcmEncoding),
        colorInfo != null ? intOrNull(colorInfo.colorSpace) : null,
        colorInfo != null ? intOrNull(colorInfo.colorRange) : null,
        colorInfo != null ? intOrNull(colorInfo.colorTransfer) : null,
        colorInfo != null ? colorInfo.hdrStaticInfo != null : null,
        bitstreamColorTransfer(format));
  }

  /**
   * Color transfer from the HEVC SPS VUI, for HEVC and Dolby Vision profile 5/8/10 (dvhe/dvh1,
   * HEVC-based) tracks that carry the parameter sets in {@code initializationData}.
   */
  // TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
  @UnstableApi
  @Nullable
  private static Long bitstreamColorTransfer(@NonNull Format format) {
    if (format.initializationData.isEmpty()) {
      return null;
    }
    boolean hevc = MimeTypes.VIDEO_H265.equals(format.sampleMimeType);
    boolean hevcBasedDolbyVision =
        MimeTypes.VIDEO_DOLBY_VISION.equals(format.sampleMimeType)
            && format.codecs != null
            && (format.codecs.startsWith("dvhe") || format.codecs.startsWith("dvh1"));
    return hevc || hevcBasedDolbyVision
        ? HevcSpsColorTransfer.read(format.initializationData)
        : null;
  }

  @Nullable
  private static Long intOrNull(int value) {
    return value != Format.NO_VALUE ? (long) value : null;
  }
}
