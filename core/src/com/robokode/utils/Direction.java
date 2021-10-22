package com.robokode.utils;

public enum Direction {
    HG,
    HD,
    BG,
    BD;

    @Override
    public String toString() { //renvoie la direction en string
        switch(this)
        {
            case HG:
                return "haut-gauche";
            case HD:
                return "haut-droit";
            case BG:
                return "bas-gauche";
            case BD:
            default:
                return "bas-droit";
        }
    }

    /** Renvoie l'énuméré correspondant à la direction en chaîne de caractères **/
    public static Direction strToEnum(String strEnum) {
        if (strEnum.equals("HAUTGAUCHE")) {
            return HG;
        } else if (strEnum.equals("HAUTDROIT")) {
            return HD;
        } else if (strEnum.equals("BASGAUCHE")) {
            return BG;
        } else if (strEnum.equals("BASDROIT")) {
            return BD;
        } else {
            return null;
        }
    }
}