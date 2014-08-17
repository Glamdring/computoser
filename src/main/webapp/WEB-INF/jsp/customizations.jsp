<%@ page pageEncoding="UTF-8" %>
<div class="span${customizationsSpanWidth}">
    <h4>Mood</h4>
    <p class="preference-panel">
          <input type="radio" name="mood" value="ANY" id="moodAny" checked="checked" /><label for="moodAny">Any</label>
          <input type="radio" name="mood" value="MAJOR" id="moodMajor"/><label for="moodMajor">Major (happy)</label>
          <input type="radio" name="mood" value="MINOR" id="moodMinor"/><label for="moodMinor">Minor (sad)</label>
    </p>

    <h4>Tempo</h4>
    <p class="preference-panel">
          <input type="radio" name="tempo" value="ANY" id="tempoAny" checked="checked" /><label for="tempoAny">Any</label>
          <input type="radio" name="tempo" value="VERY_SLOW" id="tempoVerySlow"/><label for="tempoVerySlow">Very slow</label>
          <input type="radio" name="tempo" value="SLOW" id="tempoSlow"/><label for="tempoSlow">Slow</label>
          <input type="radio" name="tempo" value="MEDIUM" id="tempoMedium"/><label for="tempoMedium">Medium</label>
          <input type="radio" name="tempo" value="FAST" id="tempoFast"/><label for="tempoFast">Fast</label>
          <input type="radio" name="tempo" value="VERY_FAST" id="tempoVeryFast"/><label for="tempoVeryFast">Very fast</label>
    </p>

    <h4>Accompaniment (chords)</h4>
    <p class="preference-panel">
          <input type="radio" name="accompaniment" value="OPTIONAL" id="accompanimentOptional" checked="checked" /><label for="accompanimentOptional">Optional</label>
          <input type="radio" name="accompaniment" value="YES" id="accompanimentYes"/><label for="accompanimentYes">Yes</label>
          <input type="radio" name="accompaniment" value="NO" id="accompanimentNo"/><label for="accompanimentNo">No chords</label>
    </p>

    <h4>Instrument</h4>
    <p class="preference-panel">
        <select name="instrument">
          <option selected="true" value="-1">Any</option>
          <option value="0">Piano</option>
          <option value="24">Guitar</option>
          <option value="25">Steel guitar</option>
          <option value="60">French horn</option>
          <option value="71">Clarinet</option>
          <option value="48">String ensemble</option>
          <option value="68">Oboe</option>
        </select>
    </p>

    <h4>Scale</h4>
    <p class="preference-panel">
        <select name="scale">
          <option selected="true" value="">Any</option>
          <option value="MAJOR">Major</option>
          <option value="MINOR">Minor</option>
          <option value="HARMONIC_MINOR">Harmonic Minor</option>
          <option value="MELODIC_MINOR">Melodic Minor</option>
          <option value="NATURAL_MINOR">Natural Minor</option>
          <option value="DORIAN">Dorian</option>
          <option value="LYDIAN">Lydian</option>
          <option value="MIXOLYDIAN">Mixolydian</option>
          <option value="TURKISH">Turkish</option>
          <option value="INDIAN">Indian</option>
          <option value="BLUES">Blues</option>
          <option value="MAJOR_PENTATONIC">Major Pentatonic</option>
          <option value="MINOR_PENTATONIC">Minor Pentatonic</option>
        </select>
    </p>
  </div>

  <div class="span${customizationsSpanWidth}">
      <h4>Classical</h4>
      <p class="preference-panel">
          <input type="radio" name="classical" value="false" id="classicalOptional" checked="checked" /><label for="classicalOptional">Optional</label>
          <input type="radio" name="classical" value="true" id="classicalYes"/><label for="classicalYes">Yes</label>
      </p>

      <h4>Electronic-like</h4>
      <p class="preference-panel">
          <input type="radio" name="electronic" value="OPTIONAL" id="electronicOptional" checked="checked" /><label for="electronicOptional">Optional</label>
          <input type="radio" name="electronic" value="YES" id="electronicYes"/><label for="electronicYes">Yes</label>
          <input type="radio" name="electronic" value="NO" id="electronicNo"/><label for="electronicNo">No</label>
      </p>

      <h4>Drums</h4>
      <p class="preference-panel">
          <input type="radio" name="drums" value="OPTIONAL" id="drumsOptional" checked="checked" /><label for="drumsOptional">Optional</label>
          <input type="radio" name="drums" value="YES" id="drumsYes"/><label for="drumsYes">Yes</label>
          <input type="radio" name="drums" value="NO" id="drumsNo"/><label for="drumsNo">No drums</label>
      </p>

      <h4>More dissonant</h4>
      <p class="preference-panel">
          <input type="radio" name="preferDissonance" value="false" id="preferDissonanceOptional" checked="checked" /><label for="preferDissonanceOptional">Optional</label>
          <input type="radio" name="preferDissonance" value="true" id="preferDissonanceYes"/><label for="preferDissonanceYes">Yes</label>
      </p>
  </div>