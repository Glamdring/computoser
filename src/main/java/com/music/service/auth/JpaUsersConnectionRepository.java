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
import java.util.Set;

import javax.inject.Inject;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.music.dao.UserDao;
import com.music.model.persistent.SocialAuthentication;
import com.music.service.UserService;

@Service
public class JpaUsersConnectionRepository implements UsersConnectionRepository {
    @Inject
    private UserDao userDao;

    @Inject
    private UserService userService;

    @Inject
    private ConnectionFactoryLocator locator;
    @Override
    public List<String> findUserIdsWithConnection(Connection<?> connection) {
        List<SocialAuthentication> auths = userDao.getSocialAuthentications(connection.getKey()
                .getProviderId(), connection.getKey().getProviderUserId());
        List<String> userIds = Lists.newArrayList();
        for (SocialAuthentication auth : auths) {
            userIds.add(String.valueOf(auth.getUser().getId()));
        }
        return userIds;
    }

    @Override
    public Set<String> findUserIdsConnectedTo(String providerId, Set<String> providerUserIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConnectionRepository createConnectionRepository(String userId) {
        return new JpaConnectionRepository(Long.parseLong(userId), userService, userDao, locator);
    }
}
