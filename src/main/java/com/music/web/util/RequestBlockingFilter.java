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

package com.music.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.io.CharStreams;

/**
 * Filter that blocks bad user agents. It also stops requests that are not valid, but spring-mvc tries to handle them, which results
 * in unwanted warnings/exceptions
 *
 * TODO allow API calls through (when an API is released)
 *
 * @author bozho
 *
 */
@WebFilter(servletNames="dispatcher", asyncSupported=true)
public class RequestBlockingFilter implements Filter {

    private static final List<String> IGNORED_RESOURCES = Arrays.asList("favicon.ico", "phpmyadmin/");
    // storing in a (hash)set for O(1) lookup
    private Set<String> blockedAgents;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        for (String ignored : IGNORED_RESOURCES) {
            if (uri.contains(ignored)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        String userAgent = request.getHeader("User-Agent");
        // allow no user agent set, for restful calls
        if (!StringUtils.isEmpty(userAgent) && blockedAgents.contains(userAgent)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        InputStream is = filterConfig.getServletContext().getResourceAsStream("/WEB-INF/classes/bots/agents.txt");
        InputStreamReader reader = new InputStreamReader(is);
        try {
            blockedAgents = new HashSet<String>(CharStreams.readLines(reader));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load blocked user agents list", ex);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    public void destroy() {
        // no cleanup
    }
}
