package net.progval.android.andquote.utils;

import org.msgpack.type.ValueFactory;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;

public class MsgPackUtils {
    public static Value get(MapValue map, String key) {
        // http://cdn.memegenerator.net/instances/400x/35280488.jpg
        return map.get(ValueFactory.createRawValue(key));
    }
    public static boolean in(MapValue map, String key) {
        // http://cdn.memegenerator.net/instances/400x/35280488.jpg
        return map.containsKey(ValueFactory.createRawValue(key));
    }
}
