package it.pgp.grimaldo.enums;

import android.Manifest;

/**
 * Adapted from https://github.com/pgp/XFiles
 */

public enum Permissions {
    WRITE_EXTERNAL_STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    String value;

    Permissions(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}