package com.wiresegal.synchrony.database;

/**
 * TODO
 *
 * @author Wire Segal
 */
public interface DatabaseObject {
    /**
     * This should be stored in a field on the database object or otherwise serialized to Json.
     *
     * @return The ID of this object in the associated database.
     */
    long id();

    /**
     * Sets the ID of this object in the database. This should only be called by databases directly.
     *
     * @param id The ID to assign.
     */
    void setId(long id);
}
