package com.robokode.game.mvc;

import java.util.List;

/**
 *
 * @author habib
 */
public interface Observateur {
    public void traiterMessage(Message msg);
    public void traiterMessage(List<Message> msg);
}