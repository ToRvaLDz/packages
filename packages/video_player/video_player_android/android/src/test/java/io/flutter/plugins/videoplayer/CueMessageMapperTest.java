// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.text.SpannableString;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CueMessageMapperTest {
  @Test
  public void textCuesBecomeStrings() {
    CueGroup group =
        new CueGroup(
            ImmutableList.of(
                new Cue.Builder().setText("Ciao").build(),
                new Cue.Builder().setText(new SpannableString("mondo")).build()),
            1_500_000L);

    CuesEvent event = CueMessageMapper.toEvent(group);

    assertEquals(Arrays.asList("Ciao", "mondo"), event.getTexts());
    assertEquals(1_500_000L, event.getPresentationTimeUs());
    assertFalse(event.getHasBitmapCues());
  }

  @Test
  public void bitmapCuesAreOnlyReported() {
    Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
    CueGroup group =
        new CueGroup(ImmutableList.of(new Cue.Builder().setBitmap(bitmap).build()), 10L);

    CuesEvent event = CueMessageMapper.toEvent(group);

    assertTrue(event.getTexts().isEmpty());
    assertTrue(event.getHasBitmapCues());
  }

  @Test
  public void mixedGroupKeepsTextAndFlagsBitmap() {
    Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
    CueGroup group =
        new CueGroup(
            ImmutableList.of(
                new Cue.Builder().setText("a").build(),
                new Cue.Builder().setBitmap(bitmap).build()),
            0L);

    CuesEvent event = CueMessageMapper.toEvent(group);

    assertEquals(Arrays.asList("a"), event.getTexts());
    assertTrue(event.getHasBitmapCues());
  }

  @Test
  public void emptyGroupProducesEmptyEvent() {
    CuesEvent event = CueMessageMapper.toEvent(CueGroup.EMPTY_TIME_ZERO);

    assertTrue(event.getTexts().isEmpty());
    assertEquals(0L, event.getPresentationTimeUs());
    assertFalse(event.getHasBitmapCues());
  }
}
