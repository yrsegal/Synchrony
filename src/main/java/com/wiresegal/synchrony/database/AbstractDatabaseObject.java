package com.wiresegal.synchrony.database;

/**
 * A simple implementation of an ID-holder, for when your
 * database object doesn't have a superclass it needs.
 *
 * @author Wire Segal
 */
public abstract class AbstractDatabaseObject implements DatabaseObject {

    @Save
    private long id;

    @Override
    public long id() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }
}
