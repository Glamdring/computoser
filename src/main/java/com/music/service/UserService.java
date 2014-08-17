/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.service;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.hibernate.StaleStateException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.connect.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.music.dao.UserDao;
import com.music.model.persistent.SocialAuthentication;
import com.music.model.persistent.User;
import com.music.service.auth.JpaConnectionRepository;
import com.music.util.SecurityUtils;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Inject
    private UserDao userDao;

    @Value("${hmac.key}")
    private String hmacKey;

    @Transactional
    public void connect(Long userId, SocialAuthentication auth) {
        List<SocialAuthentication> existingAuths = userDao.getSocialAuthentications(auth.getProviderId(), auth.getProviderUserId());

        if (existingAuths.isEmpty()) {
            User user = userDao.getById(User.class, userId);
            auth.setUser(user);
            userDao.persist(auth);
        } else {
            SocialAuthentication existingAuth = existingAuths.get(0);
            existingAuth.setExpirationTime(auth.getExpirationTime());
            existingAuth.setRefreshToken(auth.getRefreshToken());
            existingAuth.setImageUrl(auth.getImageUrl());
            userDao.persist(existingAuth);
        }
    }

    @Transactional
    public void deleteSocialAuthentication(Long userId, String providerId) {
        userDao.deleteSocialAuthentication(userId, providerId);
    }

    @Transactional(readOnly=true)
    public User getUser(long id) {
        return userDao.getById(User.class, id);
    }

    @Transactional(readOnly=true)
    public User getUserByEmail(String email) {
        return userDao.getByPropertyValue(User.class, "email", email);
    }

    @Transactional
    public User completeUserRegistration(String email, String username, String names, Connection<?> connection, boolean loginAutomatically, boolean receiveDailyDigest) {
        User user = new User();
        user.setEmail(email);
        user.setNames(names);
        user.setUsername(username);
        user.setLoginAutomatically(loginAutomatically);
        user.setRegistrationTime(new DateTime());
        user.setReceiveDailyDigest(receiveDailyDigest);
        user = userDao.persist(user);
        if (connection != null) {
            SocialAuthentication auth = JpaConnectionRepository.connectionToAuth(connection);
            auth.setUser(user);
            userDao.persist(auth);
        }
        return user;
    }

    @Transactional
    public void unsubscribe(long id, String hash) {
        User user = userDao.getById(User.class, id);
        if (hash.equals(SecurityUtils.hmac(user.getEmail(), hmacKey))) {
            user.setReceiveDailyDigest(false);
            userDao.persist(user);
        }
    }

    /**
     * http://jaspan.com/improved_persistent_login_cookie_best_practice
     */
    @Transactional(rollbackFor=StaleStateException.class)
    public User rememberMeLogin(String token, String series) {
        User existingLogin = userDao.getLoginFromAuthToken(token, series);
        if (existingLogin == null) {
            User loginBySeries = userDao.getByPropertyValue(User.class, "loginSeries", series);
            // if a login series exists, assume the previous token was stolen, so deleting all persistent logins.
            // An exception is a request made within a few seconds from the last login time
            // which may mean request from the same browser that is not yet aware of the renewed cookie
            if (loginBySeries != null && new Period(loginBySeries.getLastLoginTime(), new DateTime()).getSeconds() < 5) {
                logger.info("Assuming login cookies theft; deleting all sessions for user " + loginBySeries);
                loginBySeries.setLoginSeries(null);
                loginBySeries.setLoginToken(null);
                userDao.persist(loginBySeries);
            } else if (logger.isDebugEnabled()) {
                logger.debug("No existing login found for token=" + token + ", series=" + series);
            }
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Existing login found for token=" + token + " and series=" + series);
        }
        fillUserWithNewTokens(existingLogin, series);
        return existingLogin;
    }

    @Transactional
    public void fillUserWithNewTokens(User user, String series) {
        user.setLoginToken(UUID.randomUUID().toString());
        user.setLoginSeries(series != null ? series : UUID.randomUUID().toString());
        user.setLastLoginTime(new DateTime());

        userDao.persist(user);
    }

    @Transactional(readOnly=true)
    public SocialAuthentication getTwitterAuthentication(User user) {
        if (user == null) {
            return null;
        }
        SocialAuthentication auth = userDao.getTwitterAuthentication(user);
        return auth;
    }
}
