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

import java.io.PrintStream;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.music.model.persistent.TimelineMusic;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

@Service
public class SentimentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalyzer.class);

    private StanfordCoreNLP pipeline = new StanfordCoreNLP(getProperties());

    @PostConstruct
    public void init() {
        // because scientists can't code, and write debug messages to System.err
        PrintStream err = new PrintStream(System.err) {
            @Override
            public void println(String x) {
                if (!x.startsWith("Adding annotator")) {
                    super.println(x);
                }
            }
        };
        System.setErr(err);
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        return props;
    }

    /**
     * Synchronized method to obtain the sentiment of the set of documents.
     * Synchronization is fine, because the method is invoked via a scheduled job
     * and only one execution at a time is permitted.
     * That allows to optimize the loading of the model as well
     * @param documents
     * @return
     */
    public synchronized SentimentResult getSentiment(Set<String> documents, TimelineMusic meta) {

        double sentimentSum = 0;
        for (String document: documents) {
            int mainSentiment = 0;
            if (document != null && document.length() > 0) {
                int longest = 0;
                try {
                    Annotation annotation = pipeline.process(document);
                    // mainSentiment is the sentiment of the whole document. We find
                    // the whole document by comparing the length of individual
                    // annotated "fragments"
                    for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                        Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
                        int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
                        String partText = sentence.toString();
                        if (partText.length() > longest) {
                            mainSentiment = sentiment;
                            longest = partText.length();
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Problem analyzing document sentiment. " + document, ex);
                    continue;
                }
            }
            sentimentSum += mainSentiment;
        }

        double average = sentimentSum / documents.size();
        meta.setAverageSentiment(average);

        if (average >= 2.25) {
            return SentimentResult.POSITIVE;
        } else if (average <= 1.75) {
            return SentimentResult.NEGATIVE;
        }
        return SentimentResult.NEUTRAL;
    }

    public static enum SentimentResult {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}
