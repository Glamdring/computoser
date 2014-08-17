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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.music.util.SharedData;

@Controller
@RequestMapping("/mgmt/m/")
public class ManagementController {

    @Inject
    private SharedData sharedData;

    @RequestMapping("/setGenerateMusic")
    @ResponseBody
    public String setGenerateMusic(@RequestParam boolean generate) {
        sharedData.setGenerateMusic(generate);
        return "successfully set";
    }

    @RequestMapping("/setAdaptGenerationQuantity")
    @ResponseBody
    public String setAdaptGenerationQuantity(@RequestParam boolean adapt) {
        sharedData.setAdaptGenerationQuantity(adapt);
        return "successfully set";
    }
}
