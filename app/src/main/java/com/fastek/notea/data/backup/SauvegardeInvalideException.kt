package com.fastek.notea.data.backup

/** Erreur lisible renvoyée quand le fichier choisi n'est pas une sauvegarde Notea valide. */
class SauvegardeInvalideException(message: String) : Exception(message)
