<div id="alertArea"></div>

<div id="jplayer"></div>
    <div id="jp_container_1" class="jp-audio centered-player">
        <div class="jp-type-single">
            <div class="jp-gui jp-interface">
                <ul class="jp-controls">
                    <li><a href="javascript:;" class="jp-play" tabindex="1" rel="nofollow">play</a></li>
                    <li><a href="javascript:;" class="jp-pause" tabindex="1">pause</a></li>
                    <li style="visibility: hidden;"><a href="javascript:;" class="jp-stop" tabindex="1">stop</a></li>
                    <li><a href="javascript:;" class="jp-mute" tabindex="1" title="mute">mute</a></li>
                    <li><a href="javascript:;" class="jp-unmute" tabindex="1" title="unmute">unmute</a></li>
                    <li><a href="javascript:;" class="jp-volume-max" tabindex="1" title="max volume">max volume</a></li>
                </ul>
                <div class="jp-progress">
                    <div class="jp-seek-bar" style="width: 100%;">
                        <div class="jp-play-bar"></div>
                    </div>
                </div>
                <div class="jp-volume-bar">
                    <div class="jp-volume-bar-value"></div>
                </div>
                <div class="jp-time-holder">
                    <div class="jp-current-time"></div>
                    <div class="jp-duration"></div>
                    <ul class="jp-toggles">
                        <li><a href="javascript:;" class="jp-repeat" tabindex="1" title="repeat">repeat</a></li>
                        <li><a href="javascript:;" class="jp-repeat-off" tabindex="1" title="repeat off">repeat off</a></li>
                    </ul>
                </div>
            </div>
            <div class="jp-title">
                <ul>
                    <li id="trackTitle"></li>
                </ul>
                <input type="button" class="btn" style="float: left;" value="< Previous" onclick="previous();" id="previousButton" />
                <input type="button" class="btn" style="float: right;" value="Next >" onclick="next();" id="nextButton" />
                <span>&nbsp;</span>
                <div style="margin-right: auto; margin-left: auto; padding-bottom: 5px;">
                    <input type="button" style="margin-right: 10px;" class="btn btn-success" value="Like!" onclick="evaluateCurrent(true);"/>
                    <!-- input type="button" class="btn btn-success" value="Like the previous track" onclick="evaluatePrevious(true);"/-->
                    <input type="button" class="btn" value="Dislike" onclick="evaluateCurrent(false);"/>
                    <!-- input type="button" class="btn btn-danger" value="Dislike the previous track" onclick="evaluateCurrent(false);"/-->
                </div>
            </div>
            <div class="jp-no-solution">
                <span>Update Required</span> To play the media you will need to
                either update your browser to a recent version or update your <a
                    href="http://get.adobe.com/flashplayer/" target="_blank">Flash
                    plugin</a>.
            </div>
        </div>
    </div>
    <br />
    <div style="text-align: center;">
        <c:if test="${(paymentEnabled || bitcoinEnabled) && currentPage != 'cart'}">
            <div class="centered">
                Need this track for commercial purposes? (2 tracks for $0.99) <input type="button" id="addToCartButton" class="btn btn-info" value="Add to cart" onclick="addToCart(${pieceId})"/>
            </div>
        </c:if>
        <div class="centered">
            Download: <a rel="nofollow" href="${root}/music/getMidi/${pieceId}" id="downloadLink">Original .midi</a> | <a rel="nofollow" href="${root}/music/get/${pieceId}?download=true" id="downloadMp3Link">.mp3</a> | <a rel="nofollow" href="${root}/music/getXml/${pieceId}" id="downloadXmlLink">MusicXML</a> (licensed under Creative Commons)
        </div>
        <div class="centered" style="margin-top: 10px;" id="sharePanel">
            <a href="http://computoser.com/track/${pieceId}" id="permalink">Permanent link to this track</a>. Share this track to:
            <a id="twitterLink" href="https://twitter.com/intent/tweet?text=I%20liked%20a%20computer-generated%20musical%20piece%20http://computoser.com/track/${pieceId}&via=computosermusic" target="_blank">[twitter]</a>
            <a id="facebookLink" href="http://www.facebook.com/sharer.php?u=http://computoser.com/track/${pieceId}" target="_blank">[facebook]</a>
        </div>
    </div>