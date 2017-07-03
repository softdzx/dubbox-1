package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.governance.service.UserService;
import com.alibaba.dubbo.registry.common.domain.User;
import com.alibaba.dubbo.registry.common.util.Coder;

import java.util.List;
import java.util.Map;

public class UserServiceImpl extends AbstractService implements UserService {

    private String rootPassword;

    public void setRootPassword(String password) {
        this.rootPassword = (password == null ? "" : password);
    }

    private String guestPassword;

    public void setGuestPassword(String password) {
        this.guestPassword = (password == null ? "" : password);
    }

    public User findUser(String username) {
        if ("guest".equals(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(Coder.encodeMd5(username + ":" + User.REALM + ":" + guestPassword));
            user.setName(username);
            user.setRole(User.GUEST);
            user.setEnabled(true);
            user.setLocale("zh");
            user.setServicePrivilege("");
            return user;
        } else if ("root".equals(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(Coder.encodeMd5(username + ":" + User.REALM + ":" + rootPassword));
            user.setName(username);
            user.setRole(User.ROOT);
            user.setEnabled(true);
            user.setLocale("zh");
            user.setServicePrivilege("*");
            return user;
        }
        return null;
    }

    public List<User> findAllUsers() {
        return null;
    }

    public Map<String, User> findAllUsersMap() {
        return null;
    }

    public User findById(Long id) {
        return null;
    }

    public void createUser(User user) {

    }

    public void updateUser(User user) {

    }

    public void modifyUser(User user) {

    }

    public boolean updatePassword(User user, String oldPassword) {
        return false;
    }

    public void resetPassword(User user) {

    }

    public void enableUser(User user) {

    }

    public void disableUser(User user) {

    }

    public void deleteUser(User user) {

    }

    public List<User> findUsersByServiceName(String serviceName) {
        return null;
    }

}
