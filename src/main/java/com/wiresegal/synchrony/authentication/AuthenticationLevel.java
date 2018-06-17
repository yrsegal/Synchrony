package com.wiresegal.synchrony.authentication;

/**
 * The level that a given session is allowed to use.
 *
 * @author Wire Segal
 */
public enum AuthenticationLevel {
    /**
     * Unauthenticated. For example, creating an account would require NONE privileges.
     */
    NONE,
    /**
     * Authenticated as a user. For example, account operations would require USER privileges.
     */
    USER,
    /**
     * Authenticated as a moderator. Moderators may edit user data, but may not edit admin-level data.
     */
    MODERATOR,
    /**
     * Authenticated as an admin. The super-user privilege state.
     */
    ADMIN
}
