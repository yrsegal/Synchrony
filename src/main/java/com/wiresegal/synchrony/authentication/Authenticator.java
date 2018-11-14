package com.wiresegal.synchrony.authentication;

import com.wiresegal.synchrony.database.AbstractDatabaseObject;
import com.wiresegal.synchrony.database.Save;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mindrot.jbcrypt.BCrypt;

/**
 * A database object that stores a hashed password, a user level, and an email.
 * This is the login data for a single account.
 *
 * @author Wire Segal
 */
public class Authenticator extends AbstractDatabaseObject {
    @Save
    @Nullable
    private String ps;

    @Save
    @NotNull
    private AuthenticationLevel auth = AuthenticationLevel.USER;

    @Save
    @NotNull
    private String email;

    /**
     * Create a new authenticator. This requires the initial password and the email.
     *
     * @param email The account email, used to identify this user.
     * @param pwd   The initial password given by the user.
     */
    public Authenticator(@NotNull String email, @NotNull String pwd) {
        this.email = email;
        changePassword(pwd);
    }

    /**
     * Sets the level this account authenticator holds.
     *
     * @param level The authentication level to give this account.
     */
    public void setAuthenticationLevel(@NotNull AuthenticationLevel level) {
        auth = level;
    }

    /**
     * Gets the level this account authenticator holds.
     *
     * @return The authentication level to give this account.
     */
    @NotNull
    public AuthenticationLevel getAuthenticationLevel() {
        return auth;
    }

    /**
     * Changes the password, and re-hashes and salts it.
     *
     * @param pwd The password to change to.
     */
    public void changePassword(@NotNull String pwd) {
        String salt = BCrypt.gensalt(15);
        ps = BCrypt.hashpw(pwd, salt);
    }

    /**
     * Attempt to authenticate using the given email and password.
     *
     * @param email The email trying to log in.
     * @param pwd   The password you are trying to use.
     * @return Whether authentication was successful.
     */
    public boolean authenticate(@NotNull String email, @NotNull String pwd) {
        return ps != null && this.email.equals(email) && BCrypt.checkpw(pwd, ps);
    }
}
