// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// ziptv2: tipi pubblici per i metadati `Format` di Media3 e per i cue dei
// sottotitoli testuali, esposti da `AndroidVideoPlayer` (non fanno parte del
// platform interface).

import 'package:flutter/foundation.dart';

import 'messages.g.dart';

/// The kind of a [Media3TrackInfo].
enum Media3TrackType {
  /// A video track.
  video,

  /// An audio track.
  audio,

  /// A text (subtitle) track.
  text,
}

/// Format metadata of a single Media3 track.
///
/// Values that Media3 reports as unset (`Format.NO_VALUE`) are null.
///
/// For text tracks parsed at extraction time (the default since Media3 1.4)
/// [sampleMimeType] is `application/x-media3-cues` and the original subtitle
/// mime type (for example `application/x-subrip`) is in [codecs].
///
/// Media3's `Format` has no channel mask: only [channelCount] and
/// [pcmEncoding] are available for audio.
@immutable
class Media3TrackInfo {
  /// Creates a track description.
  const Media3TrackInfo({
    required this.type,
    required this.groupIndex,
    required this.trackIndex,
    required this.isSelected,
    required this.isSupported,
    this.selectionFlags = 0,
    this.roleFlags = 0,
    this.formatId,
    this.label,
    this.language,
    this.sampleMimeType,
    this.containerMimeType,
    this.codecs,
    this.bitrate,
    this.width,
    this.height,
    this.frameRate,
    this.channelCount,
    this.sampleRate,
    this.pcmEncoding,
    this.colorSpace,
    this.colorRange,
    this.colorTransfer,
    this.hasHdrStaticInfo,
    this.bitstreamColorTransfer,
  });

  /// `C.SELECTION_FLAG_DEFAULT`.
  static const int selectionFlagDefault = 1;

  /// `C.SELECTION_FLAG_FORCED`.
  static const int selectionFlagForced = 2;

  /// `C.COLOR_TRANSFER_ST2084` (PQ, used by HDR10 and Dolby Vision).
  static const int colorTransferPq = 6;

  /// `C.COLOR_TRANSFER_HLG`.
  static const int colorTransferHlg = 7;

  /// The Dolby Vision sample mime type.
  static const String dolbyVisionMimeType = 'video/dolby-vision';

  /// The kind of track.
  final Media3TrackType type;

  /// The index of the Media3 track group among all the groups.
  final int groupIndex;

  /// The index of the track inside its group.
  final int trackIndex;

  /// Whether the track is currently selected.
  final bool isSelected;

  /// Whether the device can play the track.
  final bool isSupported;

  /// The Media3 `C.SelectionFlags` bitmask.
  final int selectionFlags;

  /// The Media3 `C.RoleFlags` bitmask.
  final int roleFlags;

  /// The `Format.id` reported by Media3.
  final String? formatId;

  /// The human readable label.
  final String? label;

  /// The BCP 47 language code.
  final String? language;

  /// The sample mime type.
  final String? sampleMimeType;

  /// The container mime type.
  final String? containerMimeType;

  /// The codecs string.
  final String? codecs;

  /// The bitrate, in bits per second.
  final int? bitrate;

  /// The video width, in pixels.
  final int? width;

  /// The video height, in pixels.
  final int? height;

  /// The video frame rate.
  final double? frameRate;

  /// The number of audio channels.
  final int? channelCount;

  /// The audio sample rate, in Hz.
  final int? sampleRate;

  /// The Media3 `C.PcmEncoding` of the audio.
  final int? pcmEncoding;

  /// The Media3 `C.ColorSpace`; null when the format has no color info.
  final int? colorSpace;

  /// The Media3 `C.ColorRange`; null when the format has no color info.
  final int? colorRange;

  /// The Media3 `C.ColorTransfer`; null when the format has no color info.
  final int? colorTransfer;

  /// Whether the color info carries HDR static metadata; null when the format
  /// has no color info.
  final bool? hasHdrStaticInfo;

  /// The `C.ColorTransfer` signaled in the VUI of the HEVC SPS of the
  /// bitstream; null when the track is not HEVC (or Dolby Vision on HEVC), has
  /// no SPS or no colour description.
  ///
  /// Unlike [colorTransfer], it is available when Media3 leaves the format
  /// color info null (e.g. Dolby Vision profile 8 in Matroska). It is not used
  /// by [isHdr].
  final int? bitstreamColorTransfer;

  /// The track id, `'<groupIndex>_<trackIndex>'`, in the same format used by
  /// the audio and video track selection.
  String get id => '${groupIndex}_$trackIndex';

  /// Whether the track has the `SELECTION_FLAG_DEFAULT` flag.
  bool get isDefault => selectionFlags & selectionFlagDefault != 0;

  /// Whether the track has the `SELECTION_FLAG_FORCED` flag.
  bool get isForced => selectionFlags & selectionFlagForced != 0;

  /// Whether the color transfer is PQ (HDR10, Dolby Vision) or HLG.
  bool get isHdr => colorTransfer == colorTransferPq || colorTransfer == colorTransferHlg;

  /// Whether the sample mime type is Dolby Vision.
  bool get isDolbyVision => sampleMimeType == dolbyVisionMimeType;

  @override
  String toString() =>
      'Media3TrackInfo($type $id selected=$isSelected supported=$isSupported '
      'mime=$sampleMimeType codecs=$codecs lang=$language label=$label '
      'flags=$selectionFlags ${width}x$height colorTransfer=$colorTransfer '
      'bitstreamColorTransfer=$bitstreamColorTransfer)';
}

/// The Media3 tracks of a player and the text rendering state.
@immutable
class Media3TrackInfoSnapshot {
  /// Creates a snapshot.
  const Media3TrackInfoSnapshot({required this.tracks, required this.textDisabled});

  /// The video, audio and text tracks.
  final List<Media3TrackInfo> tracks;

  /// Whether text rendering is disabled in the track selector.
  final bool textDisabled;
}

/// The text cues to display at a given moment.
///
/// Only the text is carried over: style and position are dropped. Bitmap cues
/// (PGS, DVB) are not converted, they are only reported by [hasBitmapCues].
@immutable
class Media3Cues {
  /// Creates a cue group.
  const Media3Cues({
    required this.texts,
    required this.presentationTime,
    required this.hasBitmapCues,
  });

  /// The text of the cues. Empty when the subtitle must be cleared.
  final List<String> texts;

  /// The presentation time of the cues.
  final Duration presentationTime;

  /// Whether the group contains bitmap cues, which are not converted.
  final bool hasBitmapCues;

  @override
  String toString() =>
      'Media3Cues($texts presentationTime=$presentationTime bitmap=$hasBitmapCues)';
}

/// Converts a pigeon message to a [Media3TrackInfo].
Media3TrackInfo media3TrackInfoFromMessage(Media3TrackFormatMessage message) {
  return Media3TrackInfo(
    type: switch (message.type) {
      PlatformMedia3TrackType.video => Media3TrackType.video,
      PlatformMedia3TrackType.audio => Media3TrackType.audio,
      PlatformMedia3TrackType.text => Media3TrackType.text,
    },
    groupIndex: message.groupIndex,
    trackIndex: message.trackIndex,
    isSelected: message.isSelected,
    isSupported: message.isSupported,
    selectionFlags: message.selectionFlags,
    roleFlags: message.roleFlags,
    formatId: message.id,
    label: message.label,
    language: message.language,
    sampleMimeType: message.sampleMimeType,
    containerMimeType: message.containerMimeType,
    codecs: message.codecs,
    bitrate: message.bitrate,
    width: message.width,
    height: message.height,
    frameRate: message.frameRate,
    channelCount: message.channelCount,
    sampleRate: message.sampleRate,
    pcmEncoding: message.pcmEncoding,
    colorSpace: message.colorSpace,
    colorRange: message.colorRange,
    colorTransfer: message.colorTransfer,
    hasHdrStaticInfo: message.hasHdrStaticInfo,
    bitstreamColorTransfer: message.bitstreamColorTransfer,
  );
}

/// Converts a pigeon track data to a [Media3TrackInfoSnapshot].
Media3TrackInfoSnapshot media3SnapshotFromData(NativeMedia3TrackData data) {
  return Media3TrackInfoSnapshot(
    tracks: List<Media3TrackInfo>.unmodifiable(data.tracks.map(media3TrackInfoFromMessage)),
    textDisabled: data.textDisabled,
  );
}

/// Converts a pigeon cues event to a [Media3Cues].
Media3Cues media3CuesFromEvent(CuesEvent event) {
  return Media3Cues(
    texts: List<String>.unmodifiable(event.texts),
    presentationTime: Duration(microseconds: event.presentationTimeUs),
    hasBitmapCues: event.hasBitmapCues,
  );
}

/// Parses a `'<groupIndex>_<trackIndex>'` track id.
///
/// Throws an [ArgumentError] if the id is malformed or has a negative index.
(int, int) parseMedia3TrackId(String trackId) {
  final List<String> parts = trackId.split('_');
  final int? groupIndex = parts.length == 2 ? int.tryParse(parts[0]) : null;
  final int? trackIndex = parts.length == 2 ? int.tryParse(parts[1]) : null;
  if (groupIndex == null || trackIndex == null || groupIndex < 0 || trackIndex < 0) {
    throw ArgumentError(
      'Invalid track id format: "$trackId". Expected non-negative numeric '
      'indices in "groupIndex_trackIndex".',
    );
  }
  return (groupIndex, trackIndex);
}
