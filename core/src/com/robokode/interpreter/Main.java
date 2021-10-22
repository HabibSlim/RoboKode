package com.robokode.interpreter;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Main pour faire des tests séparément sur l'interpréteur
 */
public class Main {
    /**
     * Runs the interpreter as a command-line app. Takes one argument: a path
     * to a script file to load and run. The script should contain one
     * statement per line.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        // Lecture du fichier
        String contents = readFile("scripts/courant.jas");

        // On instancie l'interpréteur
        Interpreter rk_interpret = new Interpreter();

        // On interprète le script ouvert
        InterpreterMessage msg = rk_interpret.interpret(contents);

        // Selon le message reçu par l'interpréteur, on fait des trucs différents
        boolean fini = false;

        while (!fini) {
            switch (msg.type) {
                case END_PRG:
                    System.out.println ("Programme terminé !"); fini = true;
                    break;
                case DEPLACEMENT:
                    System.out.println ("Le joueur se déplace en direction : "+msg.dirInst);

                    // On fait des trucs pour que les déplacements effectués apparaissent sur le contrôleur (..)
                    // ..... => A FAIRE

                    break;
            }

            // Si on a le programme du joueur est pas terminé, on relance l'exécution
            msg = rk_interpret.restart();
        }
    }

    /**
     * Reads the file from the given path and returns its contents as a single
     * string.
     *
     * @param  path  Path to the text file to read.
     * @return       The contents of the file or null if the load failed.
     * @throws IOException
     */
    private static String readFile(String path) {
        try {
            FileInputStream stream = new FileInputStream(path);

            try {
                InputStreamReader input = new InputStreamReader(stream,
                        Charset.defaultCharset());
                Reader reader = new BufferedReader(input);

                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[8192];
                int read;

                while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                    builder.append(buffer, 0, read);
                }

                // HACK: The parser expects every statement to end in a newline,
                // even the very last one, so we'll just tack one on here in
                // case the file doesn't have one.
                builder.append("\n");

                // => On peut faire pareil que ce bout de code, en lisant le champ de texte
                // => Et en ajoutant un \n à la fin.
                return builder.toString();
            } finally {
                stream.close();
            }
        } catch (IOException ex) {
            return null;
        }
    }
}
