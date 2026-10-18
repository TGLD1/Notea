# Règles ProGuard/R8 spécifiques à Notea.

# SDK de paiement KkiaPay (réflexion et classes sérialisées)
-keep class co.opensi.kkiapay.** { *; }
-dontwarn co.opensi.kkiapay.**
