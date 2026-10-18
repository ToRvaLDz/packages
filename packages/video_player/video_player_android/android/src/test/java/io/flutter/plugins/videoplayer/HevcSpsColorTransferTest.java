// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.media3.common.C;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class HevcSpsColorTransferTest {
  // VPS + SPS + PPS in Annex-B (4-byte start codes), as in Format.initializationData of an HEVC
  // track. Generated once with x265 (64x64, one frame, 4:2:0 8-bit):
  //   x265 --colorprim <p> --transfer <t> --colormatrix <m> ...
  // The only difference among the buffers is the VUI colour description.

  /** transfer=smpte2084 (PQ), colorprim=bt2020, colormatrix=bt2020nc. */
  static final String PQ_HEX =
      "0000000140010c01ffff03700000030090000003000003001e9598090000000142010103700000030090000003000003001ea020810596566924cae6a1220120800001f400000301f404000000014401c172b42240";

  /** transfer=arib-std-b67 (HLG), colorprim=bt2020, colormatrix=bt2020nc. */
  static final String HLG_HEX =
      "0000000140010c01ffff03700000030090000003000003001e9598090000000142010103700000030090000003000003001ea020810596566924cae6a1224120800001f400000301f404000000014401c172b42240";

  /** transfer=bt709, colorprim=bt709, colormatrix=bt709. */
  static final String BT709_HEX =
      "0000000140010c01ffff03700000030090000003000003001e9598090000000142010103700000030090000003000003001ea020810596566924cae6a0202020800001f400000301f404000000014401c172b42240";

  /** No colour description in the VUI. */
  static final String NO_DESCRIPTION_HEX =
      "0000000140010c01ffff03700000030090000003000003001e9598090000000142010103700000030090000003000003001ea020810596566924cae01000003e8000003e8080000000014401c172b42240";

  private static byte[] bytes(String hex) {
    byte[] out = new byte[hex.length() / 2];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return out;
  }

  static List<byte[]> initData(String hex) {
    return Collections.singletonList(bytes(hex));
  }

  @Test
  public void pqIsSt2084() {
    assertEquals(
        Long.valueOf(C.COLOR_TRANSFER_ST2084), HevcSpsColorTransfer.read(initData(PQ_HEX)));
  }

  @Test
  public void hlgIsHlg() {
    assertEquals(
        Long.valueOf(C.COLOR_TRANSFER_HLG), HevcSpsColorTransfer.read(initData(HLG_HEX)));
  }

  @Test
  public void bt709IsSdr() {
    assertEquals(
        Long.valueOf(C.COLOR_TRANSFER_SDR), HevcSpsColorTransfer.read(initData(BT709_HEX)));
  }

  @Test
  public void noColourDescriptionIsNull() {
    assertNull(HevcSpsColorTransfer.read(initData(NO_DESCRIPTION_HEX)));
  }

  @Test
  public void emptyInputsAreNull() {
    assertNull(HevcSpsColorTransfer.read(Collections.<byte[]>emptyList()));
    assertNull(HevcSpsColorTransfer.read(Collections.singletonList(new byte[0])));
  }

  @Test
  public void truncatedBufferIsNullWithoutException() {
    byte[] full = bytes(PQ_HEX);
    // Cut in the middle of the SPS (VPS is 28 bytes, the SPS starts right after).
    for (int length : new int[] {1, 4, 5, 30, 40, 50}) {
      byte[] truncated = Arrays.copyOf(full, length);
      assertNull(
          "length " + length, HevcSpsColorTransfer.read(Collections.singletonList(truncated)));
    }
  }

  @Test
  public void garbageBufferIsNull() {
    byte[] garbage = new byte[64];
    Arrays.fill(garbage, (byte) 0xFF);
    assertNull(HevcSpsColorTransfer.read(Collections.singletonList(garbage)));
  }

  @Test
  public void bufferWithoutSpsIsNull() {
    // VPS only (first 28 bytes of the buffer).
    byte[] vpsOnly = Arrays.copyOf(bytes(PQ_HEX), 28);
    assertNull(HevcSpsColorTransfer.read(Collections.singletonList(vpsOnly)));
  }
}
