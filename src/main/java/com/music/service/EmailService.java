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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.music.model.persistent.User;

@Service
public class EmailService {

    /**
     * Sends an email. No exception is thrown even if the message sending fails.
     * (This service may decide to reattempt sending after a while)
     * @param details
     */
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${smtp.host}")
    private String smtpHost;

    @Value("${smtp.user}")
    private String smtpUser;

    @Value("${smtp.password}")
    private String smtpPassword;

    @Value("${smtp.bounce.email}")
    private String smtpBounceEmail;

    @Value("${base.url}")
    private String baseUrl;

    @Inject
    private VelocityEngine engine;

    private Email createEmail(boolean html) {
        Email e = null;
        if (html) {
            e = new HtmlEmail();
        } else {
            e = new SimpleEmail();
        }
        e.setHostName(smtpHost);
        if (!StringUtils.isEmpty(smtpUser)) {
            e.setAuthentication(smtpUser, smtpPassword);
        }

        if (!StringUtils.isEmpty(smtpBounceEmail)) {
            e.setBounceAddress(smtpBounceEmail);
        }

        e.setTLS(true);
        e.setSmtpPort(587); //tls port
        e.setCharset("UTF8");
        //e.setDebug(true);

        return e;
    }

    @Async
    public void send(EmailDetails details) {
        if (details.getSubject() == null || !BooleanUtils.xor(ArrayUtils.toArray(details.getMessage() != null, details.getMessageTemplate() != null))) {
            throw new IllegalStateException("Either subject or subjectKey / either template/message/messageKey should be specified");
        }
        Validate.notBlank(details.getFrom());

        Email email = createEmail(details.isHtml());
        String subject = constructSubject(details);
        email.setSubject(subject);

        String emailMessage = constructEmailMessages(details);

        try {
            if (details.isHtml()) {
                ((HtmlEmail) email).setHtmlMsg(emailMessage);
            } else {
                email.setMsg(emailMessage);
            }

            for (String to : details.getTo()) {
                email.addTo(to);
            }
            email.setFrom(details.getFrom());

            email.send();
        } catch (EmailException ex) {
            logger.error("Exception occurred when sending email to " + details.getTo(), ex);
        }
    }

    private String constructSubject(EmailDetails details) {
        String subject = "";

        if (details.getSubject() != null) {
            subject = details.getSubject();
        }
        return subject;
    }

    private String constructEmailMessages(EmailDetails details) {
        String emailMessage = null;

        if (details.getMessageTemplate() != null) {
            Template template = engine.getTemplate(details.getMessageTemplate(), "UTF-8");
            StringWriter writer = new StringWriter();
            Context ctx = new VelocityContext();
            for (Map.Entry<String, Object> entry : details.getMessageTemplateModel().entrySet()) {
                ctx.put(entry.getKey(), entry.getValue());
            }
            ctx.put("baseUrl", baseUrl);
            ctx.put("currentUser", details.getCurrentUser());

            template.merge(ctx, writer);
            return writer.toString();
        }

        if (details.getMessage() != null) {
            emailMessage = details.getMessage();
        }

        return emailMessage;
    }


    public static class EmailDetails implements Serializable {
        private static final long serialVersionUID = 4456840962920327178L;

        private String from;
        private List<String> to = Lists.newArrayList();
        private String messageTemplate;
        private Map<String, Object> messageTemplateModel = Maps.newHashMap();
        private String subject;
        private String message;
        private String[] subjectParams = new String[0];
        private String[] messageParams = new String[0];
        private String[] extraMessageParams = new String[0];
        private boolean html;
        private Locale locale = Locale.ENGLISH;
        private User currentUser;

        public String[] getSubjectParams() {
            return subjectParams;
        }
        public EmailDetails setSubjectParams(String[] sibjectParams) {
            this.subjectParams = sibjectParams;
            return this;
        }
        public String[] getMessageParams() {
            return messageParams;
        }
        public EmailDetails setMessageParams(String[] messageParams) {
            this.messageParams = messageParams;
            return this;
        }
        public boolean isHtml() {
            return html;
        }
        public EmailDetails setHtml(boolean html) {
            this.html = html;
            return this;
        }
        public Locale getLocale() {
            return locale;
        }
        public EmailDetails setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }
        public String getFrom() {
            return from;
        }
        public EmailDetails setFrom(String from) {
            this.from = from;
            return this;
        }
        public List<String> getTo() {
            return to;
        }
        public EmailDetails setTo(String to) {
            this.to.clear();
            return addTo(to);
        }

        public EmailDetails addTo(String to) {
            this.to.add(to);
            return this;
        }
        public String[] getExtraMessageParams() {
            return extraMessageParams;
        }
        public EmailDetails setExtraMessageParams(String[] extraMessageParams) {
            this.extraMessageParams = extraMessageParams;
            return this;
        }
        public String getSubject() {
            return subject;
        }
        public EmailDetails setSubject(String subject) {
            this.subject = subject;
            return this;
        }
        public String getMessage() {
            return message;
        }
        public EmailDetails setMessage(String message) {
            this.message = message;
            return this;
        }
        public String getMessageTemplate() {
            return messageTemplate;
        }
        public EmailDetails setMessageTemplate(String messageTemplate) {
            this.messageTemplate = messageTemplate;
            return this;
        }
        public Map<String, Object> getMessageTemplateModel() {
            return messageTemplateModel;
        }
        public EmailDetails setMessageTemplateModel(Map<String, Object> messageTemplateModel) {
            this.messageTemplateModel = messageTemplateModel;
            return this;
        }
        public EmailDetails addToMessageTemplateModel(String key, Object value) {
            messageTemplateModel.put(key, value);
            return this;
        }
        public User getCurrentUser() {
            return currentUser;
        }
        public EmailDetails setCurrentUser(User currentUser) {
            this.currentUser = currentUser;
            return this;
        }
    }
}
