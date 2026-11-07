package com.zola.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdaterTest {
    @Test
    public void testIsNewer() {
        assertTrue(Updater.isNewer("v1.1", "1.0"));
        assertTrue(Updater.isNewer("v1.0.1", "1.0"));
        assertTrue(Updater.isNewer("2.0.0", "1.9.9"));
        assertFalse(Updater.isNewer("v1.0", "1.0"));
        assertFalse(Updater.isNewer("1.0.0", "1.0.0"));
        assertFalse(Updater.isNewer("v0.9", "1.0"));
    }
}
