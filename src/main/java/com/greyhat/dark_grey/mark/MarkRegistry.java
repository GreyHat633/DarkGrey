package com.greyhat.dark_grey.mark;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.mark.api.IMarkType;

public final class MarkRegistry {

    private static final Map<String, IMarkType> TYPES = new HashMap<>();

    private MarkRegistry() {}

    public static void register(IMarkType type) {
        String id = type.getId();
        if (id == null || id.isEmpty()) {
            DarkGrey.LOG.error("Failed to register mark type: id is null or empty");
            return;
        }
        id = id.toLowerCase();
        if (TYPES.containsKey(id)) {
            DarkGrey.LOG.error("Failed to register mark type: duplicate id " + id);
            return;
        }
        TYPES.put(id, type);
        DarkGrey.LOG.info("Registered mark type: " + id);
    }

    public static IMarkType get(String markId) {
        if (markId == null) return null;
        return TYPES.get(markId.toLowerCase());
    }

    public static boolean contains(String markId) {
        if (markId == null) return false;
        return TYPES.containsKey(markId.toLowerCase());
    }

    public static Collection<IMarkType> getAll() {
        return Collections.unmodifiableCollection(TYPES.values());
    }
}
