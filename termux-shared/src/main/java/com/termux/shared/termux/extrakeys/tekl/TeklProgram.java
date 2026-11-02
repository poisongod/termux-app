package com.termux.shared.termux.extrakeys.tekl;

import java.util.List;

/** Result of parsing a tekl document: a matrix of rows, each row a list of key expressions. */
public final class TeklProgram {

    /** Rows of the extra-key matrix. Never {@code null}; may be empty. */
    public final List<List<TeklNode>> rows;

    public TeklProgram(List<List<TeklNode>> rows) {
        this.rows = rows;
    }
}
