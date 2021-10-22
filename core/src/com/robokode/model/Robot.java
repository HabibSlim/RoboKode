package com.robokode.model;

/**
 * Classe modélisant un robot du jeu
 */
public class Robot {
    // Constantes de classe
    public static final int PVMAX = 100;
    public static final int DEGATSCAC = 25; //fixe les dégâts au corps à corps
    public static final int DEGATSDIST = 15; //fixe les dégâts à distance

    // Constantes pour les tourelles
    public static final int PVMAX_T = 50;

    // Propriétés du robot
    private int pv;
    private String nom;

    // Coordonnées du robot
    private Tuile positionAct;
    private Tuile positionPrec;

    public Robot(String nom, Tuile posI) {
        pv = PVMAX;
        setPosition(posI);
        this.nom = nom;
    }

    /* Setters
    * =================================== */

    public void setPv(int pv) {
        this.pv = pv;
    }

    public void takeDamage(int amount) {
        this.pv -= amount;

        if (this.pv < 0)    this.pv = 0;
    }

    // Remplace la position précédente du robot par sa position actuelle et sa position actuelle par celle en paramètre
    public void setPosition(Tuile newPos) {
        // On enregistre la position précédente
        this.positionPrec = this.positionAct;

        // On enlève le robot de la position précédente
        if (this.positionPrec != null)  this.positionPrec.rmvRobot();

        // On définit la nouvelle position localement, et dans la grille
        this.positionAct = newPos;      newPos.setRobot(this);
    }

    /* Getters
    * =================================== */

    public int getPv() {
        return pv;
    }

    public String getNom() {
        return nom;
    }

    public int getLigne() {
        return positionAct.getLigne();
    }
    
    public int getColonne() {
        return positionAct.getColonne();
    }

    public Tuile getPositionAct() {
        return positionAct;
    }

    public int[] getPositionInt() {
        return new int[] {positionAct.getLigne(), positionAct.getColonne()};
    }

    public Tuile getPositionPrec() {
        return positionPrec;
    }

    /* Autres méthodes
    * =================================== */

    public void affiche() {
        System.out.print(nom + " - " + pv + "/" + PVMAX + "pv");
    }

    // Retourne true si les pv du robot sont supérieurs à 0
    public boolean vivant() {
        return pv > 0;
    }
}
