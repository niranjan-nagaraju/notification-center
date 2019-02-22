package com.vinithepooh.notifier;

/**
 * Created by vinithepooh on 20/02/19.
 */

enum Ntfcns_state {
    ACTIVE, INACTIVE;
}

public class NtfcnsData {
    private String condensed_string;
    private Ntfcns_state state;

    public NtfcnsData(String condensed_string, Ntfcns_state state) {
        this.condensed_string = condensed_string;
        this.state = state;
    }
}
