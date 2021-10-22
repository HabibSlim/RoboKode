package com.robokode.model;

/**
 * Classe représentant les barrières séparant certaines tuiles
 */
public class Barriere {
    private Tuile t1;
    private Tuile t2;

    Barriere(Tuile t1, Tuile t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    @Override
    public boolean equals(Object obj) {

        Barriere otherB = (Barriere) obj;

        /* On récupère les tuiles des deux barrières */
        Tuile aT1 = t1, aT2 = t2, bT1 = otherB.getTuiles()[0], bT2 = otherB.getTuiles()[1];

        return (((aT1.equals(bT1)) && (aT2.equals(bT2)))
             || ((aT2.equals(bT1)) && (aT1.equals(bT2))));

    }

    /* Getters
    * =================================== */

    // Renvoie les tuiles définissant la barrière
    public Tuile[] getTuiles() {
        return new Tuile[]{t1, t2};
    }

    @Override
    public String toString() {
        return t1.toString() + " | " + t2.toString();
    }
}
