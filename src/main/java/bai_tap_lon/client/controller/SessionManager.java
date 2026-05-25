package bai_tap_lon.client.controller;

import bai_tap_lon.common.model.user.User;

// SessionManager.java
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void setCurrentUser(User user) { this.currentUser = user; }
    public User getCurrentUser() { return currentUser; }
    public void clear() { currentUser = null; }
}