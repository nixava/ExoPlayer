/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.metadata.emsg;

import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataDecoder;
import com.google.android.exoplayer2.metadata.MetadataInputBuffer;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Decodes Event Message (emsg) atoms, as defined in ISO/IEC 23009-1:2014, Section 5.10.3.3.
 *
 * <p>Atom data should be provided to the decoder without the full atom header (i.e. starting from
 * the first byte of the scheme_id_uri field). It is expected that the presentation_time_delta field
 * should be 0, having already been accounted for by adjusting the sample timestamp.
 */
public final class EventMessageDecoder implements MetadataDecoder {

  private static final String TAG = "EventMessageDecoder";

  @SuppressWarnings("ByteBufferBackingArray")
  @Override
  public Metadata decode(MetadataInputBuffer inputBuffer) {
    ByteBuffer buffer = inputBuffer.data;
    byte[] data = buffer.array();
    int size = buffer.limit();
    ParsableByteArray emsgData = new ParsableByteArray(data, size);
    String schemeIdUri = Assertions.checkNotNull(emsgData.readNullTerminatedString());
    String value = Assertions.checkNotNull(emsgData.readNullTerminatedString());
    long timescale = emsgData.readUnsignedInt();
    long presentationTimeDelta = emsgData.readUnsignedInt();
    if (presentationTimeDelta != 0) {
      // We expect the source to have accounted for presentation_time_delta by adjusting the sample
      // timestamp and zeroing the field in the sample data. Log a warning if the field is non-zero.
      Log.w(TAG, "Ignoring non-zero presentation_time_delta: " + presentationTimeDelta);
    }
    long durationMs = Util.scaleLargeTimestamp(emsgData.readUnsignedInt(), 1000, timescale);
    long id = emsgData.readUnsignedInt();
    byte[] messageData = Arrays.copyOfRange(data, emsgData.getPosition(), size);
    return new Metadata(new EventMessage(schemeIdUri, value, durationMs, id, messageData));
  }

}
