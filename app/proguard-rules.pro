# --- FIREBASE / GSON / MODELS ---
# Keeps your model classes from being renamed, ensuring Firebase can parse them properly
-keep class org.shda.** { *; }

# --- MPANDROIDCHART ---
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# --- GENERAL KEEP RULES ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
