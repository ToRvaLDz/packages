// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

/** Selection of text (subtitle) tracks through a {@link DefaultTrackSelector} (ziptv2 fork). */
// TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
@UnstableApi
final class Media3TextTrackSelection {
  private Media3TextTrackSelection() {}

  /**
   * Enables text rendering and selects the given track.
   *
   * @throws IllegalStateException if {@code trackSelector} is null.
   * @throws IllegalArgumentException if the indices are out of range or the group is not text.
   */
  static void select(
      @Nullable DefaultTrackSelector trackSelector,
      @NonNull Tracks tracks,
      long groupIndex,
      long trackIndex) {
    if (trackSelector == null) {
      throw new IllegalStateException("Cannot select text track: track selector is null");
    }

    if (groupIndex < 0 || groupIndex >= tracks.getGroups().size()) {
      throw new IllegalArgumentException(
          "Cannot select text track: groupIndex "
              + groupIndex
              + " is out of bounds (available groups: "
              + tracks.getGroups().size()
              + ")");
    }

    Tracks.Group group = tracks.getGroups().get((int) groupIndex);

    if (group.getType() != C.TRACK_TYPE_TEXT) {
      throw new IllegalArgumentException(
          "Cannot select text track: group at index "
              + groupIndex
              + " is not a text track (type: "
              + group.getType()
              + ")");
    }

    if (trackIndex < 0 || trackIndex >= group.length) {
      throw new IllegalArgumentException(
          "Cannot select text track: trackIndex "
              + trackIndex
              + " is out of bounds (available tracks in group: "
              + group.length
              + ")");
    }

    TrackSelectionOverride override =
        new TrackSelectionOverride(group.getMediaTrackGroup(), (int) trackIndex);
    trackSelector.setParameters(
        trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(override)
            .build());
  }

  /**
   * Disables text rendering and drops any text track override.
   *
   * @throws IllegalStateException if {@code trackSelector} is null.
   */
  static void disable(@Nullable DefaultTrackSelector trackSelector) {
    if (trackSelector == null) {
      throw new IllegalStateException("Cannot disable text tracks: track selector is null");
    }
    trackSelector.setParameters(
        trackSelector
            .buildUponParameters()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build());
  }

  /** Whether text rendering is disabled; false when there is no track selector. */
  static boolean isDisabled(@Nullable DefaultTrackSelector trackSelector) {
    return trackSelector != null
        && trackSelector.getParameters().disabledTrackTypes.contains(C.TRACK_TYPE_TEXT);
  }
}
