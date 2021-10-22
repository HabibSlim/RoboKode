package com.robokode.model;


/**
 * Classe représentant une tuile de la carte
 */
public class Tuile {
    // Coordonnées de la tuile
    private int ligne;
    private int colonne;

    // Propriétés de la tuile
    private Robot robot;
    private boolean obstacle;

    public Tuile(int l, int c) {
        this.ligne = l;
        this.colonne = c;
        robot = null;
    }

    /* Méthodes fonctionnelles
    * =================================== */

    public boolean estLibre() { //renvoie true si aucun robot n'est présent sur la tuile
        return (this.robot == null);
    }
    
    public boolean aucunObstacle() { //renvoie true si aucun robot n'est présent sur la tuile et qu'elle ne contient pas d'obstacle
        return (estLibre() && !obstacle);
    }

    /* Méthodes d'affichage
    * =================================== */

    // Affiche les coordonnées de la tuile et 0 ou 1 selon si la tuile contient un robot
    public void affiche() {
        System.out.print("(" + getLigne() + ";" + getColonne() + ") : ");
        if (estLibre())
            System.out.print(" ");
        else
            System.out.print("X");
    }

    // Affiche les coordonnées de la tuile et le robot s'y trouvant
    public void afficheComplet() {
        affiche();
        System.out.print(" : ");
        robot.affiche();
    }

    /* Getters
    * =================================== */

    public int getLigne() {
        return ligne;
    }

    public int getColonne() {
        return colonne;
    }

    public int[] getCoord() { return new int[]{ligne, colonne}; }
    
    public Robot getRobot() {
        return robot;
    }

    /* Setters
    * =================================== */

    public void setRobot(Robot joueur) { //place le robot sur la tuile
        this.robot = joueur;
    }

    public void setObstacle(boolean obstacle) { //définit la présence ou non d'un obstacle
        this.obstacle = obstacle;
    }

    public void rmvRobot() { //retire le robot de la tuile
        this.robot = null;
    }


    @Override
    public String toString() { //renvoie le même texte affiché par affiche()
        if (estLibre()) {
            return "(" + getLigne() + ";" + getColonne() + ") : 0";
        }
        else {
            return "(" + getLigne() + ";" + getColonne() + ") : 1";
        }
    }

    @Override
    public boolean equals(Object obj) {
        Tuile otherT = (Tuile) obj;
        return ((otherT.getLigne() == this.ligne) && (otherT.getColonne() == this.colonne));
    }
}
