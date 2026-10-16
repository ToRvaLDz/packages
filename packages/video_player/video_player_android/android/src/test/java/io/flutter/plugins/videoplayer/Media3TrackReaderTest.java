// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class Media3TrackReaderTest {
  private static Tracks.Group group(boolean supported, boolean selected, Format... formats) {
    int[] support = new int[formats.length];
    boolean[] sel = new boolean[formats.length];
    Arrays.fill(support, supported ? C.FORMAT_HANDLED : C.FORMAT_UNSUPPORTED_TYPE);
    Arrays.fill(sel, selected);
    return new Tracks.Group(new TrackGroup(formats), false, support, sel);
  }

  private static Tracks tracksOf(Tracks.Group... groups) {
    return new Tracks(new ArrayList<>(Arrays.asList(groups)));
  }

  private static Media3TrackFormatMessage only(Tracks tracks) {
    List<Media3TrackFormatMessage> read = Media3TrackReader.read(tracks);
    assertEquals(1, read.size());
    return read.get(0);
  }

  @Test
  public void hdr10VideoExposesColorInfo() {
    ColorInfo colorInfo =
        new ColorInfo.Builder()
            .setColorSpace(C.COLOR_SPACE_BT2020)
            .setColorRange(C.COLOR_RANGE_LIMITED)
            .setColorTransfer(C.COLOR_TRANSFER_ST2084)
            .setHdrStaticInfo(new byte[] {1, 2, 3})
            .build();
    Format format =
        new Format.Builder()
            .setId("v1")
            .setSampleMimeType(MimeTypes.VIDEO_H265)
            .setCodecs("hvc1.2.4.L153.B0")
            .setWidth(3840)
            .setHeight(2160)
            .setFrameRate(23.976f)
            .setAverageBitrate(15_000_000)
            .setColorInfo(colorInfo)
            .build();

    Media3TrackFormatMessage m = only(tracksOf(group(true, true, format)));

    assertEquals(PlatformMedia3TrackType.VIDEO, m.getType());
    assertEquals(Long.valueOf(0), (Long) m.getGroupIndex());
    assertTrue(m.isSelected());
    assertTrue(m.isSupported());
    assertEquals("v1", m.getId());
    assertEquals(MimeTypes.VIDEO_H265, m.getSampleMimeType());
    assertEquals(Long.valueOf(3840), m.getWidth());
    assertEquals(Long.valueOf(2160), m.getHeight());
    assertEquals(23.976f, m.getFrameRate().floatValue(), 0.0001f);
    assertEquals(Long.valueOf(15_000_000), m.getBitrate());
    assertEquals(Long.valueOf(C.COLOR_TRANSFER_ST2084), m.getColorTransfer());
    assertEquals(Long.valueOf(C.COLOR_SPACE_BT2020), m.getColorSpace());
    assertEquals(Long.valueOf(C.COLOR_RANGE_LIMITED), m.getColorRange());
    assertEquals(Boolean.TRUE, m.getHasHdrStaticInfo());
  }

  @Test
  public void hlgVideoExposesTransferWithoutStaticInfo() {
    ColorInfo colorInfo =
        new ColorInfo.Builder()
            .setColorSpace(C.COLOR_SPACE_BT2020)
            .setColorRange(C.COLOR_RANGE_LIMITED)
            .setColorTransfer(C.COLOR_TRANSFER_HLG)
            .build();
    Format format =
        new Format.Builder()
            .setSampleMimeType(MimeTypes.VIDEO_H265)
            .setColorInfo(colorInfo)
            .build();

    Media3TrackFormatMessage m = only(tracksOf(group(true, false, format)));

    assertEquals(Long.valueOf(C.COLOR_TRANSFER_HLG), m.getColorTransfer());
    assertEquals(Boolean.FALSE, m.getHasHdrStaticInfo());
    assertFalse(m.isSelected());
  }

  @Test
  public void sdrVideoWithoutColorInfoHasNullColorFields() {
    Format format =
        new Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).setWidth(1280).build();

    Media3TrackFormatMessage m = only(tracksOf(group(true, true, format)));

    assertNull(m.getColorSpace());
    assertNull(m.getColorRange());
    assertNull(m.getColorTransfer());
    assertNull(m.getHasHdrStaticInfo());
  }

  @Test
  public void dolbyVisionKeepsMimeAndUnsupportedFlag() {
    Format format =
        new Format.Builder()
            .setSampleMimeType(MimeTypes.VIDEO_DOLBY_VISION)
            .setCodecs("dvhe.08.06")
            .build();

    Media3TrackFormatMessage m = only(tracksOf(group(false, false, format)));

    assertEquals("video/dolby-vision", m.getSampleMimeType());
    assertEquals("dvhe.08.06", m.getCodecs());
    assertFalse(m.isSupported());
  }

  @Test
  public void eac3JocAudioExposesChannelsAndLanguage() {
    Format format =
        new Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_E_AC3_JOC)
            .setCodecs("ec+3")
            .setChannelCount(6)
            .setSampleRate(48000)
            .setLanguage("ita")
            .setLabel("Italiano")
            .build();

    Media3TrackFormatMessage m = only(tracksOf(group(true, true, format)));

    assertEquals(PlatformMedia3TrackType.AUDIO, m.getType());
    assertEquals(MimeTypes.AUDIO_E_AC3_JOC, m.getSampleMimeType());
    assertEquals(Long.valueOf(6), m.getChannelCount());
    assertEquals(Long.valueOf(48000), m.getSampleRate());
    // Media3 normalizza il codice lingua (ISO 639-2 -> 639-1).
    assertEquals("it", m.getLanguage());
    assertEquals("Italiano", m.getLabel());
    assertNull(m.getPcmEncoding());
  }

  @Test
  public void textTracksExposeFlagsAndLanguage() {
    int flags = C.SELECTION_FLAG_DEFAULT | C.SELECTION_FLAG_FORCED;
    Format srt =
        new Format.Builder()
            .setSampleMimeType(MimeTypes.APPLICATION_MEDIA3_CUES)
            .setCodecs(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("ita")
            .setSelectionFlags(flags)
            .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
            .build();
    Format ass =
        new Format.Builder().setSampleMimeType(MimeTypes.TEXT_SSA).setLanguage("eng").build();

    List<Media3TrackFormatMessage> read =
        Media3TrackReader.read(tracksOf(group(true, false, srt, ass)));

    assertEquals(2, read.size());
    Media3TrackFormatMessage first = read.get(0);
    assertEquals(PlatformMedia3TrackType.TEXT, first.getType());
    assertEquals(Long.valueOf(0), (Long) first.getTrackIndex());
    assertEquals("application/x-media3-cues", first.getSampleMimeType());
    assertEquals("application/x-subrip", first.getCodecs());
    assertEquals("it", first.getLanguage());
    assertEquals(flags, first.getSelectionFlags());
    assertEquals(C.ROLE_FLAG_SUBTITLE, first.getRoleFlags());
    Media3TrackFormatMessage second = read.get(1);
    assertEquals(1L, second.getTrackIndex());
    assertEquals(MimeTypes.TEXT_SSA, second.getSampleMimeType());
    assertEquals("en", second.getLanguage());
    assertEquals(0L, second.getSelectionFlags());
  }

  @Test
  public void metadataAndImageGroupsAreIgnoredAndGroupIndexCountsAllGroups() {
    Format metadata = new Format.Builder().setSampleMimeType(MimeTypes.APPLICATION_ID3).build();
    Format image = new Format.Builder().setSampleMimeType(MimeTypes.IMAGE_JPEG).build();
    Format audio = new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AAC).build();

    List<Media3TrackFormatMessage> read =
        Media3TrackReader.read(
            tracksOf(
                group(true, false, metadata), group(true, false, image), group(true, true, audio)));

    assertEquals(1, read.size());
    assertNotNull(read.get(0));
    assertEquals(PlatformMedia3TrackType.AUDIO, read.get(0).getType());
    assertEquals(2L, read.get(0).getGroupIndex());
  }

  @Test
  public void noValueFieldsBecomeNull() {
    Format format = new Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H264).build();

    Media3TrackFormatMessage m = only(tracksOf(group(true, true, format)));

    assertNull(m.getBitrate());
    assertNull(m.getWidth());
    assertNull(m.getHeight());
    assertNull(m.getFrameRate());
    assertNull(m.getChannelCount());
    assertNull(m.getSampleRate());
    assertNull(m.getPcmEncoding());
    assertNull(m.getId());
    assertNull(m.getLabel());
    assertNull(m.getLanguage());
  }

  @Test
  public void emptyTracksYieldEmptyList() {
    assertTrue(Media3TrackReader.read(Tracks.EMPTY).isEmpty());
  }
}
