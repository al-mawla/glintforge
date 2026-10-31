package com.glintforge;

/** Duck interface mixed into ItemStackRenderState to remember which item it renders. */
public interface GlintKeyHolder {
    String glintforge$getItem();
    void glintforge$setItem(String id);
}
