// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/messages.g.dart',
    kotlinOut: 'android/src/main/kotlin/io/flutter/plugins/videoplayer/Messages.kt',
    kotlinOptions: KotlinOptions(package: 'io.flutter.plugins.videoplayer'),
    copyrightHeader: 'pigeons/copyright.txt',
  ),
)
/// Pigeon equivalent of video_platform_interface's VideoFormat.
enum PlatformVideoFormat { dash, hls, ss }

/// Pigeon equivalent of Player's playback state.
/// https://developer.android.com/media/media3/exoplayer/listening-to-player-events#playback-state
enum PlatformPlaybackState { idle, buffering, ready, ended, unknown }

sealed class PlatformVideoEvent {}

/// Sent when the video is initialized and ready to play.
class InitializationEvent extends PlatformVideoEvent {
  /// The video duration in milliseconds.
  late final int duration;

  /// The width of the video in pixels.
  late final int width;

  /// The height of the video in pixels.
  late final int height;

  /// The rotation that should be applied during playback.
  late final int rotationCorrection;
}

/// Sent when the video state changes.
///
/// Corresponds to ExoPlayer's onPlaybackStateChanged.
class PlaybackStateChangeEvent extends PlatformVideoEvent {
  late final PlatformPlaybackState state;
}

/// Sent when the video starts or stops playing.
///
/// Corresponds to ExoPlayer's onIsPlayingChanged.
class IsPlayingStateEvent extends PlatformVideoEvent {
  late final bool isPlaying;
}

/// Sent when audio tracks change.
///
/// This includes when the selected audio track changes after calling selectAudioTrack.
/// Corresponds to ExoPlayer's onTracksChanged.
class AudioTrackChangedEvent extends PlatformVideoEvent {
  /// The ID of the newly selected audio track, if any.
  late final String? selectedTrackId;
}

/// Sent when video tracks change.
///
/// This includes when the selected video track changes after calling selectVideoTrack.
/// Corresponds to ExoPlayer's onTracksChanged.
class VideoTrackChangedEvent extends PlatformVideoEvent {
  /// The ID of the newly selected video track, if any.
  /// Will be null when auto quality selection is enabled.
  late final String? selectedTrackId;
}

/// Information passed to the platform view creation.
class PlatformVideoViewCreationParams {
  const PlatformVideoViewCreationParams({required this.playerId});

  final int playerId;
}

class CreationOptions {
  CreationOptions({required this.uri, required this.httpHeaders});
  String uri;
  PlatformVideoFormat? formatHint;
  Map<String, String> httpHeaders;
  String? userAgent;
  int? backBufferDurationMs;
}

class TexturePlayerIds {
  TexturePlayerIds({required this.playerId, required this.textureId});

  final int playerId;
  final int textureId;
}

class PlaybackState {
  PlaybackState({required this.playPosition, required this.bufferPosition});

  /// The current playback position, in milliseconds.
  final int playPosition;

  /// The current buffer position, in milliseconds.
  final int bufferPosition;
}

/// Represents an audio track in a video.
class AudioTrackMessage {
  AudioTrackMessage({
    required this.id,
    required this.label,
    required this.language,
    required this.isSelected,
    this.bitrate,
    this.sampleRate,
    this.channelCount,
    this.codec,
  });

  String id;
  String label;
  String language;
  bool isSelected;
  int? bitrate;
  int? sampleRate;
  int? channelCount;
  String? codec;
}

/// Raw audio track data from ExoPlayer Format objects.
class ExoPlayerAudioTrackData {
  ExoPlayerAudioTrackData({
    required this.groupIndex,
    required this.trackIndex,
    this.label,
    this.language,
    required this.isSelected,
    this.bitrate,
    this.sampleRate,
    this.channelCount,
    this.codec,
  });

  int groupIndex;
  int trackIndex;
  String? label;
  String? language;
  bool isSelected;
  int? bitrate;
  int? sampleRate;
  int? channelCount;
  String? codec;
}

/// Container for raw audio track data from Android ExoPlayer.
class NativeAudioTrackData {
  NativeAudioTrackData({this.exoPlayerTracks});

  /// ExoPlayer-based tracks
  List<ExoPlayerAudioTrackData>? exoPlayerTracks;
}

/// Raw video track data from ExoPlayer Format objects.
class ExoPlayerVideoTrackData {
  ExoPlayerVideoTrackData({
    required this.groupIndex,
    required this.trackIndex,
    this.label,
    required this.isSelected,
    this.bitrate,
    this.width,
    this.height,
    this.frameRate,
    this.codec,
  });

  int groupIndex;
  int trackIndex;
  String? label;
  bool isSelected;
  int? bitrate;
  int? width;
  int? height;
  double? frameRate;
  String? codec;
}

/// Container for raw video track data from Android ExoPlayer.
class NativeVideoTrackData {
  NativeVideoTrackData({this.exoPlayerTracks});

  /// ExoPlayer-based tracks
  List<ExoPlayerVideoTrackData>? exoPlayerTracks;
}

@HostApi()
abstract class AndroidVideoPlayerApi {
  void initialize();
  // Creates a new player using a platform view for rendering and returns its
  // ID.
  int createForPlatformView(CreationOptions options);
  // Creates a new player using a texture for rendering and returns its IDs.
  TexturePlayerIds createForTextureView(CreationOptions options);
  void dispose(int playerId);
  void setMixWithOthers(bool mixWithOthers);
  String getLookupKeyForAsset(String asset, String? packageName);
}

@HostApi()
abstract class VideoPlayerInstanceApi {
  /// Sets whether to automatically loop playback of the video.
  void setLooping(bool looping);

  /// Sets the volume, with 0.0 being muted and 1.0 being full volume.
  void setVolume(double volume);

  /// Sets the playback speed as a multiple of normal speed.
  void setPlaybackSpeed(double speed);

  /// Begins playback if the video is not currently playing.
  void play();

  /// Pauses playback if the video is currently playing.
  void pause();

  /// Seeks to the given playback position, in milliseconds.
  void seekTo(int position);

  /// Returns the current playback position, in milliseconds.
  int getCurrentPosition();

  /// Returns the current buffer position, in milliseconds.
  int getBufferedPosition();

  /// Gets the available audio tracks for the video.
  NativeAudioTrackData getAudioTracks();

  /// Selects which audio track is chosen for playback from its [groupIndex] and [trackIndex]
  void selectAudioTrack(int groupIndex, int trackIndex);

  /// Gets the available video tracks for the video.
  NativeVideoTrackData getVideoTracks();

  /// Selects which video track is chosen for playback from its [groupIndex] and [trackIndex].
  void selectVideoTrack(int groupIndex, int trackIndex);

  /// Enables automatic video quality selection, allowing the player to adaptively
  /// switch between available video tracks based on network conditions.
  void enableAutoVideoQuality();

  // ziptv2: metadati Format Media3 e selezione dei sottotitoli testuali.

  /// Gets the Media3 format metadata of the video, audio and text tracks.
  NativeMedia3TrackData getMedia3Tracks();

  /// Selects the text track from its [groupIndex] and [trackIndex].
  void selectTextTrack(int groupIndex, int trackIndex);

  /// Disables text track rendering.
  void disableTextTracks();
}

@EventChannelApi()
abstract class VideoEventChannel {
  PlatformVideoEvent videoEvents();
}

// ziptv2: tipi del fork per i metadati Media3 e la consegna dei cue.

/// Kind of a Media3 track group exposed by [Media3TrackFormatMessage].
enum PlatformMedia3TrackType { video, audio, text }

/// Format metadata of a single Media3 track.
///
/// `Format.NO_VALUE` fields are reported as null.
class Media3TrackFormatMessage {
  Media3TrackFormatMessage({
    required this.type,
    required this.groupIndex,
    required this.trackIndex,
    required this.isSelected,
    required this.isSupported,
    required this.selectionFlags,
    required this.roleFlags,
    this.id,
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

  PlatformMedia3TrackType type;
  int groupIndex;
  int trackIndex;
  bool isSelected;
  bool isSupported;

  /// Media3 `C.SelectionFlags` bitmask.
  int selectionFlags;

  /// Media3 `C.RoleFlags` bitmask.
  int roleFlags;
  String? id;
  String? label;
  String? language;
  String? sampleMimeType;
  String? containerMimeType;
  String? codecs;
  int? bitrate;
  int? width;
  int? height;
  double? frameRate;
  int? channelCount;
  int? sampleRate;
  int? pcmEncoding;
  int? colorSpace;
  int? colorRange;
  int? colorTransfer;
  bool? hasHdrStaticInfo;

  /// Color transfer read from the HEVC SPS VUI of the bitstream (`C.COLOR_TRANSFER_*`),
  /// null when absent or not HEVC.
  int? bitstreamColorTransfer;
}

/// Container for the Media3 track metadata.
class NativeMedia3TrackData {
  NativeMedia3TrackData({required this.tracks, required this.textDisabled});

  List<Media3TrackFormatMessage> tracks;

  /// Whether the text track type is disabled in the track selector.
  bool textDisabled;
}

/// Sent when the text cues to display change.
///
/// Corresponds to ExoPlayer's onCues. An empty [texts] clears the subtitle.
/// Bitmap cues are not converted: only reported through [hasBitmapCues].
class CuesEvent extends PlatformVideoEvent {
  late final List<String> texts;

  /// The presentation time of the cues, in microseconds.
  late final int presentationTimeUs;

  late final bool hasBitmapCues;
}
