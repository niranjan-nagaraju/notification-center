package com.vinithepooh.notifier;

/**
 * Created by vinithepooh on 14/02/19.
 */

public class NtfcnsDataModel {
    String pkg_name;
    String app_name;
    String ntfcn_contents;
    String placeholder;


    public NtfcnsDataModel(String pkg_name, String app_name, String ntfcn_contents,
                           String placeholder) {
        this.pkg_name = pkg_name;
        this.app_name = app_name;
        this.ntfcn_contents = ntfcn_contents;
        this.placeholder = placeholder;
    }


    public String getPkg_name() {
        return pkg_name;
    }

    public String getApp_name() {
        return app_name;
    }

    public String getNtfcn_contents() {
        return ntfcn_contents;
    }

    public String getPlaceholder() {
        return placeholder;
    }
}
