package utb.fai;

import java.util.*;

public class ActiveHandlers {
    private static final long serialVersionUID = 1L;
    private HashSet<SocketHandler> activeHandlersSet = new HashSet<SocketHandler>();

    /**
     * Send message to yourself.
     * 
     * @param sender  - sender reference
     * @param message - message
     */
    void sendMessageToSelf(SocketHandler sender, String message) {
        if (!sender.messages.offer(message)) // zkus přidat zprávu do fronty jeho zpráv
            System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
    }

    /**
     * Send message to everyone except yourself.
     * 
     * @param sender  - sender reference
     * @param message - message
     */
    synchronized void sendMessageToAll(SocketHandler sender, String message) {
        for (SocketHandler handler : activeHandlersSet) // pro všechny aktivní handlery
            if (handler != sender) {
                if (!handler.messages.offer(message)) // zkus přidat zprávu do fronty jeho zpráv
                    System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);
            }
    }

    /**
     * add přidá do množiny aktivních handlerů nový handler.
     * Metoda je sychronizovaná, protože HashSet neumí multithreading.
     * 
     * @param handler - reference na handler, který se má přidat.
     * @return true if the set did not already contain the specified element.
     */
    synchronized boolean add(SocketHandler handler) {
        return activeHandlersSet.add(handler);
    }

    /**
     * remove odebere z množiny aktivních handlerů nový handler.
     * Metoda je sychronizovaná, protože HashSet neumí multithreading.
     * 
     * @param handler - reference na handler, který se má odstranit
     * @return true if the set did not already contain the specified element.
     */
    synchronized boolean remove(SocketHandler handler) {
        return activeHandlersSet.remove(handler);
    }
}
