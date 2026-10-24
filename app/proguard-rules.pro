# Règles ProGuard/R8 spécifiques à Notea.

# SDK de paiement KkiaPay (réflexion et classes sérialisées)
-keep class co.opensi.kkiapay.** { *; }
-dontwarn co.opensi.kkiapay.**

# Dépendances du SDK KkiaPay (réseau, sérialisation) : pas d'avertissement bloquant à la réduction.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
-dontwarn javax.annotation.**
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Fichier de correspondance lisible pour les rapports de plantage.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
