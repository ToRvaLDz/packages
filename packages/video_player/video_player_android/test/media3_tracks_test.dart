// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:video_player_android/src/messages.g.dart';
import 'package:video_player_android/video_player_android.dart';
import 'package:video_player_platform_interface/video_player_platform_interface.dart';

import 'android_video_player_test.mocks.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  provideDummy<Future<void>>(Future<void>.value());

  const playerId = 1;

  (AndroidVideoPlayer, MockVideoPlayerInstanceApi, StreamController<PlatformVideoEvent>) setUp1() {
    final instanceApi = MockVideoPlayerInstanceApi();
    final streamController = StreamController<PlatformVideoEvent>();
    final player = AndroidVideoPlayer(
      pluginApi: MockAndroidVideoPlayerApi(),
      playerApiProvider: (_) => instanceApi,
      videoEventStreamProvider: (_) => streamController.stream.asBroadcastStream(),
    );
    player.ensurePlayerInitialized(playerId, const VideoPlayerPlatformViewState());
    return (player, instanceApi, streamController);
  }

  Media3TrackFormatMessage message({
    PlatformMedia3TrackType type = PlatformMedia3TrackType.text,
    int groupIndex = 2,
    int trackIndex = 1,
    int selectionFlags = 0,
    String? sampleMimeType,
    int? colorTransfer,
    int? bitstreamColorTransfer,
  }) {
    return Media3TrackFormatMessage(
      type: type,
      groupIndex: groupIndex,
      trackIndex: trackIndex,
      isSelected: true,
      isSupported: false,
      selectionFlags: selectionFlags,
      roleFlags: 8,
      id: 'fid',
      label: 'Italiano',
      language: 'it',
      sampleMimeType: sampleMimeType,
      codecs: 'application/x-subrip',
      bitrate: 1000,
      width: 3840,
      height: 2160,
      frameRate: 23.976,
      channelCount: 6,
      sampleRate: 48000,
      pcmEncoding: 2,
      colorSpace: 6,
      colorRange: 2,
      colorTransfer: colorTransfer,
      hasHdrStaticInfo: true,
      bitstreamColorTransfer: bitstreamColorTransfer,
    );
  }

  group('getMedia3Tracks', () {
    test('converts messages to public types', () async {
      final (AndroidVideoPlayer player, MockVideoPlayerInstanceApi api, _) = setUp1();
      when(api.getMedia3Tracks()).thenAnswer(
        (_) async => NativeMedia3TrackData(
          tracks: <Media3TrackFormatMessage>[
            message(selectionFlags: 3, sampleMimeType: 'application/x-media3-cues'),
          ],
          textDisabled: true,
        ),
      );

      final Media3TrackInfoSnapshot snapshot = await player.getMedia3Tracks(playerId);

      expect(snapshot.textDisabled, isTrue);
      expect(snapshot.tracks, hasLength(1));
      final Media3TrackInfo track = snapshot.tracks.single;
      expect(track.type, Media3TrackType.text);
      expect(track.id, '2_1');
      expect(track.formatId, 'fid');
      expect(track.label, 'Italiano');
      expect(track.language, 'it');
      expect(track.sampleMimeType, 'application/x-media3-cues');
      expect(track.codecs, 'application/x-subrip');
      expect(track.isSelected, isTrue);
      expect(track.isSupported, isFalse);
      expect(track.isDefault, isTrue);
      expect(track.isForced, isTrue);
      expect(track.roleFlags, 8);
      expect(track.width, 3840);
      expect(track.frameRate, 23.976);
      expect(track.channelCount, 6);
      expect(track.pcmEncoding, 2);
      expect(track.hasHdrStaticInfo, isTrue);
    });

    test('maps bitstreamColorTransfer separately from colorTransfer', () async {
      final (AndroidVideoPlayer player, MockVideoPlayerInstanceApi api, _) = setUp1();
      when(api.getMedia3Tracks()).thenAnswer(
        (_) async => NativeMedia3TrackData(
          tracks: <Media3TrackFormatMessage>[
            message(type: PlatformMedia3TrackType.video, bitstreamColorTransfer: 6),
            message(type: PlatformMedia3TrackType.video, trackIndex: 2),
          ],
          textDisabled: false,
        ),
      );

      final Media3TrackInfoSnapshot snapshot = await player.getMedia3Tracks(playerId);

      expect(snapshot.tracks[0].bitstreamColorTransfer, 6);
      expect(snapshot.tracks[0].colorTransfer, isNull);
      expect(snapshot.tracks[0].isHdr, isFalse);
      expect(snapshot.tracks[1].bitstreamColorTransfer, isNull);
    });

    test('maps every track type', () async {
      final (AndroidVideoPlayer player, MockVideoPlayerInstanceApi api, _) = setUp1();
      when(api.getMedia3Tracks()).thenAnswer(
        (_) async => NativeMedia3TrackData(
          tracks: <Media3TrackFormatMessage>[
            message(type: PlatformMedia3TrackType.video),
            message(type: PlatformMedia3TrackType.audio),
            message(),
          ],
          textDisabled: false,
        ),
      );

      final Media3TrackInfoSnapshot snapshot = await player.getMedia3Tracks(playerId);

      expect(snapshot.tracks.map((Media3TrackInfo t) => t.type), <Media3TrackType>[
        Media3TrackType.video,
        Media3TrackType.audio,
        Media3TrackType.text,
      ]);
      expect(snapshot.textDisabled, isFalse);
    });

    test('flags getters', () {
      const plain = Media3TrackInfo(
        type: Media3TrackType.video,
        groupIndex: 0,
        trackIndex: 0,
        isSelected: false,
        isSupported: true,
      );
      expect(plain.isDefault, isFalse);
      expect(plain.isForced, isFalse);
      expect(plain.isHdr, isFalse);
      expect(plain.isDolbyVision, isFalse);

      const hdr = Media3TrackInfo(
        type: Media3TrackType.video,
        groupIndex: 0,
        trackIndex: 0,
        isSelected: false,
        isSupported: true,
        colorTransfer: 6,
        sampleMimeType: 'video/dolby-vision',
      );
      expect(hdr.isHdr, isTrue);
      expect(hdr.isDolbyVision, isTrue);

      const hlg = Media3TrackInfo(
        type: Media3TrackType.video,
        groupIndex: 0,
        trackIndex: 0,
        isSelected: false,
        isSupported: true,
        colorTransfer: 7,
      );
      expect(hlg.isHdr, isTrue);
    });
  });

  group('selectTextTrack', () {
    test('selects by parsed group and track index', () async {
      final (AndroidVideoPlayer player, MockVideoPlayerInstanceApi api, _) = setUp1();

      await player.selectTextTrack(playerId, '3_4');

      verify(api.selectTextTrack(3, 4));
      verifyNever(api.disableTextTracks());
    });

    test('null disables the text tracks', () async {
      final (AndroidVideoPlayer player, MockVideoPlayerInstanceApi api, _) = setUp1();

      await player.selectTextTrack(playerId, null);

      verify(api.disableTextTracks());
      verifyNever(api.selectTextTrack(any, any));
    });

    for (final badId in <String>['', '1', '1_2_3', 'a_b', '1_x', '-1_0', '0_-2', '_']) {
      test('malformed trackId "$badId" throws ArgumentError', () async {
        final (AndroidVideoPlayer player, MockVideoPlayerInstanceApi api, _) = setUp1();

        await expectLater(player.selectTextTrack(playerId, badId), throwsArgumentError);

        verifyNever(api.selectTextTrack(any, any));
      });
    }
  });

  group('cuesFor', () {
    test('CuesEvent reaches cuesFor and not videoEventsFor', () async {
      final (AndroidVideoPlayer player, _, StreamController<PlatformVideoEvent> events) = setUp1();
      final cues = <Media3Cues>[];
      final videoEvents = <VideoEvent>[];
      final StreamSubscription<Media3Cues> cuesSub = player.cuesFor(playerId).listen(cues.add);
      final StreamSubscription<VideoEvent> videoSub = player
          .videoEventsFor(playerId)
          .listen(videoEvents.add);

      events.add(
        CuesEvent(texts: <String>['a', 'b'], presentationTimeUs: 1500000, hasBitmapCues: true),
      );
      events.add(CuesEvent(texts: <String>[], presentationTimeUs: 0, hasBitmapCues: false));
      await Future<void>.delayed(Duration.zero);

      expect(cues, hasLength(2));
      expect(cues[0].texts, <String>['a', 'b']);
      expect(cues[0].presentationTime, const Duration(milliseconds: 1500));
      expect(cues[0].hasBitmapCues, isTrue);
      expect(cues[1].texts, isEmpty);
      expect(videoEvents, isEmpty);

      await cuesSub.cancel();
      await videoSub.cancel();
    });

    test('stream is closed on dispose', () async {
      final (AndroidVideoPlayer player, _, _) = setUp1();
      final Stream<Media3Cues> stream = player.cuesFor(playerId);
      final done = Completer<void>();
      stream.listen((_) {}, onDone: done.complete);

      await player.dispose(playerId);

      await done.future.timeout(const Duration(seconds: 2));
    });

    test('cuesFor on an unknown player throws StateError', () {
      final (AndroidVideoPlayer player, _, _) = setUp1();

      expect(() => player.cuesFor(99), throwsStateError);
    });
  });
}
