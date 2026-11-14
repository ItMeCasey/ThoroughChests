package com.itmecasey.thoroughchests;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** A whitelist or blacklist of item ids. An empty list allows everything, so a fresh chest filters nothing. */
public final class ItemFilter {
    private boolean whitelist;
    private final Set<String> ids = new LinkedHashSet<>();

    public boolean whitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public void toggleMode() {
        whitelist = !whitelist;
    }

    public void setIds(Collection<String> items) {
        ids.clear();
        ids.addAll(items);
    }

    public Set<String> ids() {
        return ids;
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    public boolean allows(String itemId) {
        if (ids.isEmpty()) {
            return true;
        }
        return whitelist == ids.contains(itemId);
    }
}
