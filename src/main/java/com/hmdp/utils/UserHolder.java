package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

/**
 * @author FanJian
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> local = new ThreadLocal<>();

    public static void setUser(UserDTO user) {
        local.set(user);
    }

    public static UserDTO getUser() {
        UserDTO userDTO = local.get();
        return userDTO;
    }

    public static void removeUser() {
        local.remove();
    }


}
