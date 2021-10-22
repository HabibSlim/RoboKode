package com.robokode.utils;

/**
 * Classe utilitaire pour le débug
 */
public class ConsoleLog {
    private static int count = 0;

    /** Masque les sorties console si on est en production **/
    private static final boolean MUTE_MODE = false;

    public static void put(String str) {
        if (!MUTE_MODE)
            System.out.println("["+count+"]. "+str);
        count++;
    }

    // Sortie console et exit en cas d'erreur
    public static void put(String str, boolean error) {
        System.out.println("["+count+"]. "+str);
        System.exit(-1);

        // => Si un thread est encore ouvert :
        Runtime.getRuntime().halt(-1);
    }
}
