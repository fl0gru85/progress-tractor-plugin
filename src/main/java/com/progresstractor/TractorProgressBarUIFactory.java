package com.progresstractor;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Swing looks up a static {@code createUI(JComponent)} on the registered UI class. Kotlin can't declare
 * one that hides {@code BasicProgressBarUI.createUI} ("accidental override"), so this Java shim does it.
 */
public final class TractorProgressBarUIFactory extends TractorProgressBarUI {

    private TractorProgressBarUIFactory() {
    }

    @SuppressWarnings({"unused", "MethodOverridesStaticMethodOfSuperclass"})
    public static ComponentUI createUI(JComponent c) {
        return new TractorProgressBarUI();
    }
}
