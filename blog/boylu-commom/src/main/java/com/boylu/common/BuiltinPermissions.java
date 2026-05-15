package com.boylu.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BuiltinPermissions {

    private static final List<String> ADMIN_PERMISSIONS = Arrays.asList(
            "sys:resource:add",
            "sys:resource:update",
            "sys:resource:delete"
    );

    private BuiltinPermissions() {
    }

    public static List<String> mergeAdminPermissions(List<String> permissions) {
        List<String> result = permissions == null ? new ArrayList<String>() : new ArrayList<String>(permissions);
        for (String permission : ADMIN_PERMISSIONS) {
            if (!result.contains(permission)) {
                result.add(permission);
            }
        }
        return result;
    }
}
