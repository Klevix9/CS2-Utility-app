# kotlinx.serialization ships its own R8 rules; keep our serializable model classes' companions just in case.
-keepclassmembers class app.cs2lineups.core.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.cs2lineups.core.**$$serializer { *; }
