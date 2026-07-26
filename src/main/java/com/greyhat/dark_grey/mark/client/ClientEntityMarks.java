package com.greyhat.dark_grey.mark.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientEntityMarks {

    private final Map<String, ClientMarkInstance> marks = new LinkedHashMap<>();

    public ClientMarkInstance getMark(String markId) {
        if (markId == null) return null;
        return marks.get(markId.toLowerCase());
    }

    public void putMark(ClientMarkInstance instance) {
        if (instance != null && instance.markId != null) {
            marks.put(instance.markId.toLowerCase(), instance);
        }
    }

    public void removeMark(String markId) {
        if (markId != null) {
            marks.remove(markId.toLowerCase());
        }
    }

    public void clear() {
        marks.clear();
    }

    public Collection<ClientMarkInstance> getAll() {
        return marks.values();
    }

    public boolean isEmpty() {
        return marks.isEmpty();
    }
}
