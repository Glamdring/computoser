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

package com.music.service.text;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.connect.TwitterServiceProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.music.dao.PieceDao;
import com.music.dao.UserDao;
import com.music.model.Scale;
import com.music.model.persistent.Piece;
import com.music.model.persistent.SocialAuthentication;
import com.music.model.persistent.TimelineMusic;
import com.music.model.persistent.TimelineMusicRequest;
import com.music.model.persistent.User;
import com.music.model.prefs.Tempo;
import com.music.model.prefs.UserPreferences;
import com.music.model.prefs.Variation;
import com.music.service.text.SentimentAnalyzer.SentimentResult;
import com.music.util.music.Chance;

import edu.stanford.nlp.process.Morphology;

/**
 * Service class to transform a twitter timeline to music.
 * Note that only English is supported, and other languages will yield unexpected results (but should still work)
 *
 * @author bozho
 *
 */
@Service
public class TimelineToMusicService {

    private static final Logger logger = LoggerFactory.getLogger(TimelineToMusicService.class);
    private static final String USERNAME_REGEX = "(?:^|\\s|[\\p{Punct}&&[^/]])(@[\\p{L}0-9_\\.]*[\\p{L}0-9_]{1})";
    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);
    private static final String URL_REGEX = "http(s)?://([\\w+?\\.\\w+])+([\\p{L}0-9\\p{Punct}]*)?[\\p{L}0-9/]";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX, Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);;

    @Value("${twitter.app.key}")
    private String appKey;

    @Value("${twitter.app.secret}")
    private String appSecret;

    @Inject
    private SentimentAnalyzer sentimentAnalyzer;

    @Inject
    private UserDao userDao;

    @Inject
    private PieceDao pieceDao;

    private TwitterServiceProvider provider;

    private Set<String> stopwords;

    private Random random = new Random();

    @PostConstruct
    public void init() throws IOException {
        provider = new TwitterServiceProvider(appKey, appSecret);
        stopwords = new HashSet<>(IOUtils.readLines(TimelineToMusicService.class.getResourceAsStream("/stopwords.txt")));
    }

    @Transactional(readOnly=true)
    public TimelineMusic getTwitterMusic(Long id) {
        return pieceDao.getById(TimelineMusic.class, id);
    }

    @Transactional
    public TimelineMusic storeUserTimelinePiece(User user) {
        if (user == null) {
            return null;
        }

        SocialAuthentication auth = userDao.getTwitterAuthentication(user);

        if (auth == null) {
            return null;
        }

        Twitter twitter = provider.getApi(auth.getToken(), auth.getSecret());
        List<Tweet> tweets = twitter.timelineOperations().getUserTimeline(200);

        TimelineMusic meta = getUserTimelinePiece(tweets);
        meta.setTwitterHandle(twitter.userOperations().getScreenName());
        meta.setUser(user);

        meta = pieceDao.persist(meta);
        return meta;
    }

    public TimelineMusic getUserTimelinePiece(List<Tweet> tweets) {
        TimelineMusic meta = new TimelineMusic();
        Scale scale = getScale(tweets, meta);
        meta.setScale(scale);

        Tempo tempo = getTempo(tweets, meta);
        meta.setTempo(tempo);

        Variation variation = getVariation(tweets, meta);
        meta.setVariation(variation);

        UserPreferences prefs = new UserPreferences();
        prefs.setTempo(tempo);
        prefs.setScale(scale);
        prefs.setVariation(variation);

        List<Piece> pieces = pieceDao.getByPreferences(prefs);
        if (pieces.isEmpty()) {
            logger.warn("No piece found for preferences " + prefs + ". Getting relaxing criteria");
            prefs.setVariation(Variation.ANY);
            pieces = pieceDao.getByPreferences(prefs);
            if (pieces.isEmpty()) {
                prefs.setTempo(Tempo.ANY);
                pieces = pieceDao.getByPreferences(prefs);
            }
        }

        Piece piece = pieces.get(random.nextInt(pieces.size()));
        meta.setPiece(piece);
        return meta;
    }

    /**
     * Gets the tempo, depending on the rate of tweeting
     *
     * @param tweets
     * @return tempo
     */
    private Tempo getTempo(List<Tweet> tweets, TimelineMusic meta) {
        long totalSpacingInMillis = 0;
        Tweet previousTweet = null;
        for (Tweet tweet : tweets) {
           if (previousTweet != null) {
               totalSpacingInMillis += Math.abs(previousTweet.getCreatedAt().getTime() - tweet.getCreatedAt().getTime());
           }
           previousTweet = tweet;
        }

        double averageSpacing = totalSpacingInMillis / (tweets.size() - 1);
        meta.setAverageSpacing(averageSpacing);

        if (averageSpacing > 3 * DateTimeConstants.MILLIS_PER_DAY) { //once every three days
            return Tempo.VERY_SLOW;
        } else if (averageSpacing > 1.5 * DateTimeConstants.MILLIS_PER_DAY) { // more than once every 1.5 days
            return Tempo.SLOW;
        } else if (averageSpacing > 16 * DateTimeConstants.MILLIS_PER_HOUR) { // more than once every 16 hours
            return Tempo.MEDIUM;
        } else if (averageSpacing > 4 * DateTimeConstants.MILLIS_PER_HOUR) { // more than once every 4 hours
            return Tempo.FAST;
        } else {
            return Tempo.VERY_FAST;
        }
    }

    /**
     * Sentiment determines major or minor scale (or lydian/dorian ~= neutral)
     * Average length of tweets determines pentatonic or heptatonic
     *
     * @param tweets
     * @return scale
     */
    private Scale getScale(List<Tweet> tweets, TimelineMusic meta) {
        Set<String> documents = new HashSet<>();
        for (Tweet tweet : tweets) {
            documents.add(tweet.getText());
        }
        SentimentResult sentiment = sentimentAnalyzer.getSentiment(documents, meta);

        double totalLength = 0;
        for (String document : documents) {
            totalLength += document.length();
        }
        double averageLength = totalLength / documents.size();

        meta.setSentiment(sentiment);
        meta.setAverageLength(averageLength);

        if (sentiment == SentimentResult.POSITIVE) {
            return averageLength < 40 ? Scale.MAJOR_PENTATONIC : Scale.MAJOR;
        } else if (sentiment == SentimentResult.NEGATIVE) {
            return averageLength < 40 ? Scale.MINOR_PENTATONIC : Scale.MINOR;
        }
        // choose rarer scales for neutral tweets
        return Chance.test(50) ? Scale.LYDIAN : Scale.DORIAN;
    }

    private Variation getVariation(List<Tweet> tweets, TimelineMusic meta) {
        Morphology morphology = new Morphology(new StringReader(""));
        Multiset<String> words = HashMultiset.create();
        for (Tweet tweet : tweets) {
            String tweetText = tweet.getText().toLowerCase();
            List<String> urls = TimelineToMusicService.extractUrls(tweetText);
            for (String url : urls) {
                tweetText = tweetText.replace(url, "");
            }
            List<String> usernames = TimelineToMusicService.extractMentionedUsernames(tweetText);
            for (String username : usernames) {
                tweetText = tweetText.replace(username, "").replace("rt", "");
            }

            String[] wordsInTweet = tweetText.split("[^\\p{L}&&[^']]+");
            for (String word : wordsInTweet) {
                try {
                    words.add(morphology.stem(word));
                } catch (Exception ex) {
                    words.add(word);
                }
            }
        }
        words.removeAll(stopwords);

        // if a word is mentioned more times than is 4% of the tweets, it's considered a topic
        double topicThreshold = tweets.size() * 4 / 100;
        for (Iterator<String> it = words.iterator(); it.hasNext();) {
            String word = it.next();
            // remove stopwords not in the list (e.g. in a different language).
            // We consider all words less than 4 characters to be stop words
            if (word == null || word.length() < 4) {
                it.remove();
            } else  if (words.count(word) < topicThreshold) {
                it.remove();
            }
        }

        meta.setTopKeywords(new HashSet<>(words.elementSet()));

        // the more topics you have, the more variative music
        if (meta.getTopKeywords().size() > 40) {
            return Variation.EXTREMELY_VARIATIVE;
        } else if (meta.getTopKeywords().size() > 30) {
            return Variation.VERY_VARIATIVE;
        } else if (meta.getTopKeywords().size() > 20) {
            return Variation.MOVING;
        } else if (meta.getTopKeywords().size() > 10) {
            return Variation.AVERAGE;
        } else {
            return Variation.MONOTONOUS;
        }
    }

    public static List<String> extractUrls(String text) {
        if (StringUtils.isEmpty(text)) {
            return Collections.emptyList();
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        List<String> list = new ArrayList<String>();
        while (matcher.find()) {
            list.add(matcher.group());
        }

        return list;
    }

    public static List<String> extractMentionedUsernames(String text) {
        List<String> usernames = new ArrayList<String>();
        Matcher m = USERNAME_PATTERN.matcher(text);
        while (m.find()) {
            usernames.add(m.group(1).trim());
        }

        return usernames;
    }

    @Transactional
    public void completeRequest(TimelineMusicRequest request, long start) {
        request.setTimeToProcess(System.currentTimeMillis() - start);
        request.setProcessed(true);
        userDao.persist(request);
    }

    @Transactional
    public TimelineMusicRequest makeTimelineMusicRequest(User user) {
        TimelineMusicRequest request = new TimelineMusicRequest();
        request.setUser(user);
        request.setRequested(new DateTime());
        return userDao.persist(request);
    }
}
