# Release builds drop debug/verbose/info logging (also keeps notification keys out of logcat).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
