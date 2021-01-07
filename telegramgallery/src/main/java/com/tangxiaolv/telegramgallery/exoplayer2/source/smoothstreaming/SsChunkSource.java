/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.tangxiaolv.telegramgallery.exoplayer2.source.smoothstreaming;

import com.tangxiaolv.telegramgallery.exoplayer2.extractor.mp4.TrackEncryptionBox;
import com.tangxiaolv.telegramgallery.exoplayer2.source.chunk.ChunkSource;
import com.tangxiaolv.telegramgallery.exoplayer2.source.smoothstreaming.manifest.SsManifest;
import com.tangxiaolv.telegramgallery.exoplayer2.trackselection.TrackSelection;
import com.tangxiaolv.telegramgallery.exoplayer2.upstream.LoaderErrorThrower;

/**
 * A {@link ChunkSource} for SmoothStreaming.
 */
public interface SsChunkSource extends ChunkSource {

  interface Factory {

    SsChunkSource createChunkSource(LoaderErrorThrower manifestLoaderErrorThrower,
        SsManifest manifest, int elementIndex, TrackSelection trackSelection,
        TrackEncryptionBox[] trackEncryptionBoxes);

  }

  void updateManifest(SsManifest newManifest);

}
