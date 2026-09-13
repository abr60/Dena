# Safety net for Room database classes (entities, DAOs, converters)
-keep class com.dena.data.** { *; }

# Safety net for BuildConfig to prevent stripping of fields shown in the UI
-keep class com.dena.BuildConfig { *; }
