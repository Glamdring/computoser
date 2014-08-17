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

package com.music.web;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.joda.time.DateTimeConstants;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import com.music.model.persistent.User;
import com.music.service.UserService;

@Component
public class SocialSignInAdapter implements SignInAdapter {

    private static final int COOKIE_AGE = DateTimeConstants.SECONDS_PER_WEEK;
    public static final String AUTH_TOKEN_COOKIE_NAME = "authToken";
    public static final String AUTH_TOKEN_SERIES_COOKIE_NAME = "authSeries";

    @Inject
    private UserContext context;
    @Inject
    private UserService userService;

    @Override
    public String signIn(String userId, Connection<?> connection, NativeWebRequest request) {
        User user = userService.getUser(Long.parseLong(userId));
        signIn(user, (HttpServletResponse) request.getNativeResponse(), true);
        HttpSession session = ((HttpServletRequest) request.getNativeRequest()).getSession();
        String redirectUri = (String) session.getAttribute(AuthenticationController.REDIRECT_AFTER_LOGIN);
        if (redirectUri != null) {
            return redirectUri;
        }
        return "/";
    }

    public void signIn(User user, HttpServletResponse response, boolean resetTokens) {
        context.setUser(user);
        if (resetTokens) {
            userService.fillUserWithNewTokens(user, null);
        }
        addPermanentCookies(user, response);
    }

    public void addPermanentCookies(User user, HttpServletResponse response) {
        Cookie authTokenCookie = new Cookie(AUTH_TOKEN_COOKIE_NAME, user.getLoginToken());
        authTokenCookie.setMaxAge(COOKIE_AGE);
        authTokenCookie.setPath("/");
        authTokenCookie.setDomain(".computoser.com");
        response.addCookie(authTokenCookie);

        Cookie seriesCookie = new Cookie(AUTH_TOKEN_SERIES_COOKIE_NAME, user.getLoginSeries());
        seriesCookie.setMaxAge(COOKIE_AGE);
        seriesCookie.setPath("/");
        seriesCookie.setDomain(".computoser.com");
        response.addCookie(seriesCookie);
    }
}
