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

package com.music;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.music.model.prefs.UserPreferences;
import com.music.util.music.Chance;

import jm.music.data.Score;

public class TitleGenerator implements ScoreManipulator {

    private static final Logger logger = LoggerFactory.getLogger(TitleGenerator.class);

    private Random random = new Random();

    private List<String> nouns;
    private List<String> abstractNouns;
    private List<String> pronouns;
    private List<String> adjectives;
    private List<String> minorAdjectives;
    private List<String> transitiveVerbs;
    private List<KeyValue> intransitiveVerbs = Lists.newArrayList();
    private List<String> adverbs;
    private List<String> linkingWords;
    private List<String> structures = Lists.newArrayList();

    public TitleGenerator() throws IOException {
        Splitter splitter = Splitter.on(',');
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(TitleGenerator.class.getResourceAsStream("/titleComponents.properties"))) {
            properties.load(reader);
            nouns = Lists.newArrayList(splitter.split(properties.getProperty("nouns")));
            abstractNouns = Lists.newArrayList(splitter.split(properties.getProperty("abstract.nouns")));
            pronouns = Lists.newArrayList(splitter.split(properties.getProperty("pronouns")));
            adjectives = Lists.newArrayList(splitter.split(properties.getProperty("adjectives")));
            minorAdjectives = Lists.newArrayList(splitter.split(properties.getProperty("minor.adjectives")));
            linkingWords = Lists.newArrayList(splitter.split(properties.getProperty("linking.words")));
            transitiveVerbs = Lists.newArrayList(splitter.split(properties.getProperty("transitive.verbs")));
            adverbs = Lists.newArrayList(splitter.split(properties.getProperty("adverbs")));
            List<String> intransitiveVerbsList = Lists.newArrayList(splitter.split(properties.getProperty("intransitive.verbs")));
            for (String verb : intransitiveVerbsList) {
                String[] parts = verb.split("\\|");
                intransitiveVerbs.add(new KeyValue(parts[0], parts[1]));
            }
        }

        // define the possible structures of the piece title in terms of its components
        String[] nouns = new String[] {"CN", "AN"};
        String[] verbs = new String[] {"TV", "IV"};
        for (String noun : nouns) {
            for (String verb : verbs) {
                for (String secondNoun : nouns) {
                   structures.add(noun + "-" + verb + "-" + secondNoun);
                   structures.add("PRN" + "-" + verb + "-" + secondNoun);
                   structures.add("ADJ-" + noun + "-" + verb + "-" + secondNoun);
                   structures.add(noun + "-" + verb + "-ADJ-" + secondNoun);
                   structures.add(noun + "-LNK-" + secondNoun);
                   structures.add(noun + "-LNK-ADJ-" + secondNoun);
                   structures.add("ADJ-" + noun + "-LNK-" + secondNoun);
                }
                if (verb.equals("IV")) {
                    structures.add(noun + "-" + verb);
                    structures.add("ADJ-" + noun + "-" + verb);
                    structures.add(noun + "-" + verb + "-ADV");
                    structures.add("ADJ-" + noun + "-" + verb + "-ADV");
                }
                structures.add(verb + "-ADV");
                structures.add(verb + "-" + noun);
                structures.add(verb + "-ADJ-" + noun);
            }
        }
    }

    @Override
    public void handleScore(Score score, ScoreContext ctx, UserPreferences prefs) {
        String structure = structures.get(random.nextInt(structures.size()));
        logger.debug(structure + "= ");
        // getting all of them, rather than checking if they are needed - no difference in speed
        String noun1 = nouns.get(random.nextInt(nouns.size()));
        String noun2 = nouns.get(random.nextInt(nouns.size()));
        String abstractNoun1 = abstractNouns.get(random.nextInt(abstractNouns.size()));
        String abstractNoun2 = abstractNouns.get(random.nextInt(abstractNouns.size()));
        String transitiveVerb = transitiveVerbs.get(random.nextInt(transitiveVerbs.size()));
        String pronoun = pronouns.get(random.nextInt(pronouns.size()));
        String linkingWord = linkingWords.get(random.nextInt(linkingWords.size()));
        KeyValue intransitiveVerb = intransitiveVerbs.get(random.nextInt(intransitiveVerbs.size()));
        String adjective;
        if (ctx.getScale().name().contains("MINOR")) {
            adjective = minorAdjectives.get(random.nextInt(minorAdjectives.size()));
        } else {
            adjective = adjectives.get(random.nextInt(adjectives.size()));
        }
        String adverb = adverbs.get(random.nextInt(adverbs.size()));

        boolean thirdPerson = !(structure.startsWith("TV") || structure.startsWith("IV")) && !(structure.contains("PRN-TV") || structure.contains("PRN-IV"));
        if (structure.startsWith("AN") || structure.startsWith("ADJ-AN")) {
            DeclansionResult declinedFirstNoun = decline(abstractNoun1, false, false);
            if (declinedFirstNoun.isPluralized()) {
                thirdPerson = false;
            }
            structure = structure.replaceFirst("AN", declinedFirstNoun.getResult());
        } else {
            DeclansionResult declinedFirstNoun = decline(noun1, true, false);
            if (declinedFirstNoun.isPluralized()) {
                thirdPerson = false;
            }
            structure = structure.replaceFirst("CN", declinedFirstNoun.getResult());
        }
        structure = structure.replaceFirst("AN", decline(abstractNoun2, false, true).getResult());
        structure = structure.replaceFirst("CN", decline(noun2, true, true).getResult());

        structure = structure.replace("ADJ-the-", "the-ADJ-");
        structure = structure.replace("ADJ-a-", "a-ADJ-");
        structure = structure.replace("PRN", pronoun);
        structure = structure.replaceFirst("ADJ", adjective);

        structure = structure.replaceFirst("TV", conjugate(transitiveVerb, thirdPerson, structure.startsWith("TV")));
        String conjucatedIntrasitiveVerb = conjugate(intransitiveVerb.getKey(), thirdPerson, structure.startsWith("IV"));
        if (structure.endsWith("IV") || structure.endsWith("IV-ADV")) {
            structure = structure.replaceFirst("IV", conjucatedIntrasitiveVerb);
        } else{
            structure = structure.replaceFirst("IV", conjucatedIntrasitiveVerb + " " + intransitiveVerb.getValue());
        }

        structure = structure.replaceFirst("ADV", adverb);
        structure = structure.replaceFirst("LNK", linkingWord);

        structure = structure.replace('-', ' ');
        structure = structure.replaceAll(" a ([aouei])", " an $1");
        structure = structure.replaceAll("^a ([aouei])", "an $1");

        logger.debug(structure);

        score.setTitle(WordUtils.capitalize(structure));
    }

    private String conjugate(String verb, boolean thirdPerson, boolean allowProgressiveTense) {
       if (Chance.test(20) && allowProgressiveTense) { //progressive tesnse
           if (verb.endsWith("e")) {
               verb = verb.substring(0, verb.length() - 1) + "ing";
           } else {
               verb = verb + "ing";
           }
       } else if (thirdPerson){
           if (verb.endsWith("o")) {
               verb += "es";
           } else if (verb.endsWith("y")) {
               if (StringUtils.endsWithAny(verb, "ay", "oy", "ey", "uy")) {
                   verb += "s";
               } else {
                   verb = verb.substring(0, verb.length() - 1) + "ies";
               }
           } else if (verb.endsWith("s") || verb.endsWith("ch") || verb.endsWith("sh")){
               verb += "es";
           } else {
               verb += "s";
           }
       }
       return verb;
    }

    private DeclansionResult decline(String noun, boolean concrete, boolean mandatoryArticle) {
        boolean pluralize = concrete && Chance.test(20);
        if (pluralize) {
            noun = pluralize(noun);
        }
        if (!pluralize && concrete && (mandatoryArticle || Chance.test(30))) {
            noun = "a-" + noun;
        } else if (concrete && (mandatoryArticle || Chance.test(30))) {
            noun = "the-" + noun;
        }
        DeclansionResult result = new DeclansionResult();
        result.setResult(noun);
        result.setPluralized(pluralize);
        return result;
    }

    private String pluralize(String noun) {
        if (noun.endsWith("man")) {
            return noun.replace("man", "men");
        }
        if (noun.endsWith("s") || noun.endsWith("ch") || noun.endsWith("sh")) {
            return noun + "es";
        } else if (noun.endsWith("y")) {
            return noun.substring(0, noun.length() - 1) + "ies";
        } else {
            return noun + "s";
        }
    }

    static class KeyValue {
        private String key;
        private String value;

        public KeyValue(String key, String value) {
            super();
            this.key = key;
            this.value = value;
        }
        public String getKey() {
            return key;
        }
        public void setKey(String key) {
            this.key = key;
        }
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
    }

    static class DeclansionResult {
        private boolean pluralized;
        private String result;
        public boolean isPluralized() {
            return pluralized;
        }
        public void setPluralized(boolean pluralized) {
            this.pluralized = pluralized;
        }
        public String getResult() {
            return result;
        }
        public void setResult(String result) {
            this.result = result;
        }
    }
}
