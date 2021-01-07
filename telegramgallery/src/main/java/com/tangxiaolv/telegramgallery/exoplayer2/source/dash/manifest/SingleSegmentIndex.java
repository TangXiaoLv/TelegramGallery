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
package com.tangxiaolv.telegramgallery.exoplayer2.source.dash.manifest;

import com.tangxiaolv.telegramgallery.exoplayer2.source.dash.DashSegmentIndex;

/**
 * A {@link DashSegmentIndex} that defines a single segment.
 */
/* package */ final class SingleSegmentIndex implements DashSegmentIndex {

  private final RangedUri uri;

  /**
   * @param uri A {@link RangedUri} defining the location of the segment data.
   */
  public SingleSegmentIndex(RangedUri uri) {
    this.uri = uri;
  }

  @Override
  public int getSegmentNum(long timeUs, long periodDurationUs) {
    return 0;
  }

  @Override
  public long getTimeUs(int segmentNum) {
    return 0;
  }

  @Override
  public long getDurationUs(int segmentNum, long periodDurationUs) {
    return periodDurationUs;
  }

  @Override
  public RangedUri getSegmentUrl(int segmentNum) {
    return uri;
  }

  @Override
  public int getFirstSegmentNum() {
    return 0;
  }

  @Override
  public int getSegmentCount(long periodDurationUs) {
    return 1;
  }

  @Override
  public boolean isExplicit() {
    return true;
  }

}
