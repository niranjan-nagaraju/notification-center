package com.vinithepooh.notifier;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by vinithepooh on 16/03/19.
 */

public class CurrentNotificationsView {
    @Retention(RetentionPolicy.SOURCE)

    @IntDef({TYPE_ACTIVE, TYPE_ALL, TYPE_APP, TYPE_SEARCH})

    // Create an interface for validating int types
    public @interface CurrentViewMode {}

    // Declare the constants
    public static final int TYPE_ACTIVE = 0;
    public static final int TYPE_ALL = 1;
    public static final int TYPE_APP = 2;
    public static final int TYPE_SEARCH = 3;

    public final int currentViewMode;

    // Mark the argument as restricted to these enumerated types
    public CurrentNotificationsView(@CurrentViewMode int mode) {
        this.currentViewMode = mode;
    }
}
