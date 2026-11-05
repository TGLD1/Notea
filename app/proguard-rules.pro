# Paiement FedaPay : le formulaire de paiement appelle ces méthodes depuis JavaScript ;
# sans cette règle la réduction du code les renomme et le résultat du paiement n'arrive plus.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.fastek.notea.enseignant.ui.PontFedaPay { *; }

-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Fichier de correspondance lisible pour les rapports de plantage.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
