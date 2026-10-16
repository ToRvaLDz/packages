// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import androidx.annotation.NonNull;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a Media3 {@link CueGroup} to a {@link CuesEvent} (ziptv2 fork).
 *
 * <p>Only the text of the cues is carried over (style and position are dropped). Bitmap cues (for
 * example PGS or DVB subtitles) are not converted, they are only reported through {@code
 * hasBitmapCues}.
 */
final class CueMessageMapper {
  private CueMessageMapper() {}

  @NonNull
  static CuesEvent toEvent(@NonNull CueGroup cueGroup) {
    List<String> texts = new ArrayList<>();
    boolean hasBitmapCues = false;
    for (Cue cue : cueGroup.cues) {
      if (cue.bitmap != null) {
        hasBitmapCues = true;
      }
      if (cue.text != null) {
        texts.add(cue.text.toString());
      }
    }
    return new CuesEvent(texts, cueGroup.presentationTimeUs, hasBitmapCues);
  }
}
