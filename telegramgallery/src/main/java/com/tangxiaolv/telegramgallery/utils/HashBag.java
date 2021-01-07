/*
 * Copyright 2013 Michael Evans <michaelcevans10@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tangxiaolv.telegramgallery.utils;

import java.util.HashMap;
import java.util.Iterator;

public class HashBag<K> extends HashMap<K, Integer> {

    public HashBag() {
        super();
    }

    public int getCount(K value) {
        if (get(value) == null) {
            return 0;
        } else {
            return get(value);
        }
    }

    public void add(K value) {
        if (get(value) == null) {
            put(value, 1);
        } else {
            put(value, get(value) + 1);
        }
    }

    public Iterator<K> iterator() {
        return keySet().iterator();
    }
}
