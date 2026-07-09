# Smart Island Proguard Rules

# Keep Compose Runtime and library classes
-keep class androidx.compose.** { *; }

# Keep DataStore Preferences serializer/keys
-keep class androidx.datastore.preferences.core.** { *; }

# Keep reflection targets used in SmartIslandOverlayService
-keep class android.view.ViewTreeObserver$OnComputeInternalInsetsListener { *; }
-keep class android.view.ViewTreeObserver {
    public void addOnComputeInternalInsetsListener(android.view.ViewTreeObserver$OnComputeInternalInsetsListener);
}
-keep class android.view.View$InternalInsetsInfo {
    public void setTouchableInsets(int);
    *** mTouchableRegion;
    *** touchableRegion;
}

# Keep ActivityOptions and setLaunchWindowingMode
-keep class android.app.ActivityOptions {
    public void setLaunchWindowingMode(int);
    public void setPendingIntentBackgroundActivityStartMode(int);
}

# ── MediaController reflection used in MusicExpanded ──
# Keep repeat mode methods accessed via reflection
-keep class android.media.session.MediaController {
    public int getRepeatMode();
}
-keep class android.media.session.MediaController$TransportControls {
    public void setRepeatMode(int);
}
# Keep Rating.isHearted() accessed via reflection
-keep class android.media.Rating {
    public boolean isHearted();
}
# Keep MediaMetadata.getRating() used to obtain Rating object
-keep class android.media.MediaMetadata {
    public android.media.Rating getRating(java.lang.String);
}
# Keep PlaybackState custom actions and extras
-keep class android.media.session.PlaybackState {
    public java.util.List getCustomActions();
}
-keep class android.media.session.PlaybackState$CustomAction {
    public java.lang.String getAction();
    public java.lang.CharSequence getName();
}
