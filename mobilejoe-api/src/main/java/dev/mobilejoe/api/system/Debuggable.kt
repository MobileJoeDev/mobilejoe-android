package dev.mobilejoe.api.system

import android.content.Context
import android.content.pm.ApplicationInfo

/** Whether this [Context]'s app has `android:debuggable="true"` in its manifest. */
internal val Context.isDebuggable: Boolean
    get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
