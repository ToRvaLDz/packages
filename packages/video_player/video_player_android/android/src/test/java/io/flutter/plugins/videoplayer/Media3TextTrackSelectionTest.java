// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.test.core.app.ApplicationProvider;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class Media3TextTrackSelectionTest {
  private DefaultTrackSelector trackSelector;
  private TrackGroup textGroup;
  private Tracks tracks;

  private static Tracks.Group group(TrackGroup trackGroup) {
    int[] support = new int[trackGroup.length];
    Arrays.fill(support, C.FORMAT_HANDLED);
    return new Tracks.Group(trackGroup, false, support, new boolean[trackGroup.length]);
  }

  @Before
  public void setUp() {
    Context context = ApplicationProvider.getApplicationContext();
    trackSelector = new DefaultTrackSelector(context);
    TrackGroup audioGroup =
        new TrackGroup(new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AAC).build());
    textGroup =
        new TrackGroup(
            new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_MEDIA3_CUES).build(),
            new Format.Builder().setSampleMimeType(MimeTypes.TEXT_SSA).build());
    tracks = new Tracks(new ArrayList<>(Arrays.asList(group(audioGroup), group(textGroup))));
  }

  @Test
  public void selectAppliesOverrideAndEnablesText() {
    Media3TextTrackSelection.disable(trackSelector);

    Media3TextTrackSelection.select(trackSelector, tracks, 1, 1);

    assertFalse(Media3TextTrackSelection.isDisabled(trackSelector));
    TrackSelectionOverride override = trackSelector.getParameters().overrides.get(textGroup);
    assertEquals(textGroup, override.mediaTrackGroup);
    assertEquals(Arrays.asList(1), override.trackIndices);
  }

  @Test
  public void selectTwiceIsIdempotent() {
    Media3TextTrackSelection.select(trackSelector, tracks, 1, 0);
    Media3TextTrackSelection.select(trackSelector, tracks, 1, 0);

    assertEquals(1, trackSelector.getParameters().overrides.size());
    assertEquals(
        Arrays.asList(0), trackSelector.getParameters().overrides.get(textGroup).trackIndices);
  }

  @Test
  public void selectOnNonTextGroupThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Media3TextTrackSelection.select(trackSelector, tracks, 0, 0));
  }

  @Test
  public void selectOutOfRangeThrows() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Media3TextTrackSelection.select(trackSelector, tracks, 2, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> Media3TextTrackSelection.select(trackSelector, tracks, 1, 2));
  }

  @Test
  public void negativeIndicesThrow() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Media3TextTrackSelection.select(trackSelector, tracks, -1, 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> Media3TextTrackSelection.select(trackSelector, tracks, 1, -1));
  }

  @Test
  public void disableClearsOverrideAndDisablesText() {
    Media3TextTrackSelection.select(trackSelector, tracks, 1, 0);

    Media3TextTrackSelection.disable(trackSelector);

    assertTrue(Media3TextTrackSelection.isDisabled(trackSelector));
    assertTrue(trackSelector.getParameters().overrides.isEmpty());
  }

  @Test
  public void disableTwiceIsIdempotent() {
    Media3TextTrackSelection.disable(trackSelector);
    Media3TextTrackSelection.disable(trackSelector);

    assertTrue(Media3TextTrackSelection.isDisabled(trackSelector));
  }

  @Test
  public void textIsEnabledByDefault() {
    assertFalse(Media3TextTrackSelection.isDisabled(trackSelector));
  }

  @Test
  public void nullTrackSelectorThrowsIllegalState() {
    assertThrows(
        IllegalStateException.class, () -> Media3TextTrackSelection.select(null, tracks, 1, 0));
    assertThrows(IllegalStateException.class, () -> Media3TextTrackSelection.disable(null));
    assertFalse(Media3TextTrackSelection.isDisabled(null));
  }
}
