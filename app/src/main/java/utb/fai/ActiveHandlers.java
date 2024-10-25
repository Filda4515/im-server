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
        if (!sender.messages.offer(message))
            System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
    }

    /**
     * Send message to everyone except yourself.
     * 
     * @param sender  - sender reference
     * @param message - message
     */
    synchronized void sendMessageToAll(SocketHandler sender, String message) {
        for (SocketHandler handler : activeHandlersSet)
            if (handler != sender) {
                if (!handler.messages.offer(message))
                    System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);
            }
    }

    /**
     * Send message to client by name.
     * 
     * @param message  - message
     * @param name - name
     * @return false if name doesn't exist; true otherwise
     */
    synchronized boolean sendMessageToName(String message, String name) {
        for (SocketHandler handler : activeHandlersSet) {
            if (handler.name.equals(name)) {
                if (!handler.messages.offer(message))
                    System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a name is already taken by another client.
     *
     * @param name - name
     * @return true if the name is taken by another client; false otherwise
     */
    synchronized boolean isNameTaken(String name) {
        for (SocketHandler handler : activeHandlersSet) {
            if (name.equals(handler.name)) {
                return true;
            }
        }
        return false;
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
