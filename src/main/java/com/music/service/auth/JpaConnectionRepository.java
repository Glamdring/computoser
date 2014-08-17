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

package com.music.service.auth;

import java.util.List;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.NoSuchConnectionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import com.music.dao.UserDao;
import com.music.model.persistent.SocialAuthentication;
import com.music.service.UserService;

public class JpaConnectionRepository implements ConnectionRepository {

    private UserService userService;
    private UserDao userDao;
    private ConnectionFactoryLocator locator;
    private Long userId;

    public JpaConnectionRepository(Long userId, UserService userService, UserDao userDao, ConnectionFactoryLocator locator) {
        this.userId = userId;
        this.userService = userService;
        this.userDao = userDao;
        this.locator = locator;
    }

    @Override
    public MultiValueMap<String, Connection<?>> findAllConnections() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Connection<?>> findConnections(String providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <A> List<Connection<A>> findConnections(Class<A> apiType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultiValueMap<String, Connection<?>> findConnectionsToUsers(
            MultiValueMap<String, String> providerUserIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Connection<?> getConnection(ConnectionKey connectionKey) {
        return getConnection(connectionKey.getProviderId(), connectionKey.getProviderUserId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> Connection<A> getConnection(Class<A> apiType, String providerUserId) {
        String providerId = locator.getConnectionFactory(apiType).getProviderId();
        return (Connection<A>) getConnection(providerId, providerUserId);
    }

    private Connection<?> getConnection(String providerId, String providerUserId) {
        List<SocialAuthentication> socialAuthentications = userDao.getSocialAuthentications(providerId, providerUserId);
        if (socialAuthentications.isEmpty()) {
            throw new NoSuchConnectionException(new ConnectionKey(providerId, providerUserId));
        }
        return authToConnection(socialAuthentications.get(0));
    }

    @Override
    public <A> Connection<A> getPrimaryConnection(Class<A> apiType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <A> Connection<A> findPrimaryConnection(Class<A> apiType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional
    public void addConnection(Connection<?> connection) {
        SocialAuthentication auth = connectionToAuth(connection);
        userService.connect(userId, auth);
    }

    @Override
    public void updateConnection(Connection<?> connection) {
        SocialAuthentication auth = connectionToAuth(connection);
        userService.connect(userId, auth);
    }

    public static SocialAuthentication connectionToAuth(Connection<?> connection) {
        SocialAuthentication auth = new SocialAuthentication();
        ConnectionData data = connection.createData();
        auth.setProviderId(data.getProviderId());
        auth.setToken(data.getAccessToken());
        auth.setRefreshToken(data.getRefreshToken());
        auth.setSecret(data.getSecret());
        auth.setProviderUserId(data.getProviderUserId());
        return auth;
    }

    private Connection<?> authToConnection(SocialAuthentication auth) {
        ConnectionFactory<?> connectionFactory = locator.getConnectionFactory(auth.getProviderId());
        ConnectionData data = new ConnectionData(auth.getProviderId(), auth.getProviderUserId(), null, null,
                auth.getImageUrl(), auth.getToken(), auth.getSecret(), auth.getRefreshToken(),
                auth.getExpirationTime());
        return connectionFactory.createConnection(data);
    }

    @Override
    public void removeConnections(String providerId) {
        userService.deleteSocialAuthentication(userId, providerId);
    }

    @Override
    public void removeConnection(ConnectionKey connectionKey) {
        userService.deleteSocialAuthentication(userId, connectionKey.getProviderId());
    }

}
