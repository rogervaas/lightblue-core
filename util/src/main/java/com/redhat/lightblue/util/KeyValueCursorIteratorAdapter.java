/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of lightblue.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.redhat.lightblue.util;

import java.util.Iterator;
import java.util.Map;

/**
 * Adapter to plug in a Map.Entry<K,V> iterator into KeyValueCursor
 */
public class KeyValueCursorIteratorAdapter<K,V> implements KeyValueCursor<K,V> {

    private final Iterator<Map.Entry<K,V>> itr;

    private K key;
    private V value;

    public KeyValueCursorIteratorAdapter(Iterator<Map.Entry<K,V>> itr) {
        this.itr=itr;
    }

    public boolean hasNext() {
        return itr.hasNext();
    }

    public void next() {
        Map.Entry<K,V> v=itr.next();
        key=v.getKey();
        value=v.getValue();
    }
        
    public K getCurrentKey() {
        return key;
    }

    public V getCurrentValue() {
        return value;
    }
}