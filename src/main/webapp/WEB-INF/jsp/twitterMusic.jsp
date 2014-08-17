<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="head">
<title>Music based on your tweets - Computoser</title>
<c:set var="pieceId" value="${music.piece.id}" />
<script type="text/javascript">
$(document).ready(function() {
    $("#sharePanel").hide(); //hide the original share panel
});
</script>
<style type="text/css">
#explanation li {
    line-height: 1.6em;
}
</style>
<%@ include file="playerScripts.jsp" %>
</c:set>
<%@ include file="header.jsp" %>

<h3>${title}</h3>
<%@ include file="player.jsp" %>

<div class="centered" style="text-align: center; margin-bottom: 15px;">
    <a href="http://computoser.com/twitterMusic/${music.id}" id="permalink">Link</a>. Share this track to:
    <a id="twitterLink" href="https://twitter.com/intent/tweet?text=I%20liked%20a%20computer-generated%20musical%20piece%20http://computoser.com/twitterMusic/${music.id}&via=computosermusic" target="_blank">[twitter]</a>
    <a id="facebookLink" href="http://www.facebook.com/sharer.php?u=http://computoser.com/twitterMusic/${music.id}" target="_blank">[facebook]</a>
</div>

This generated piece is influenced by the tweets of <strong>@${music.twitterHandle}</strong>:
<ul id="explanation">
    <li>The scale is <strong>${music.scale.toString().toLowerCase().replaceAll("_", " ")}</strong> because:
        <ul>
        <li>the overall sentiment of the tweets is <strong>${music.sentiment.toString().toLowerCase()}</strong></li>
        <li>the average tweet length is <strong><fmt:formatNumber value="${music.averageLength}" maxFractionDigits="1" /></strong> characters (that influences 5-tone vs 7-tone scales)</li>
        </ul>
    </li>

    <li>The tempo of the piece is <strong>${music.tempo.toString().toLowerCase().replaceAll("_", " ")}</strong> because a tweet is posted every <fmt:formatNumber value="${music.averageSpacing / (1000 * 60)}" maxFractionDigits="1" /> minutes.</li>
    <li>The piece is <strong>${music.variation.toString().toLowerCase().replaceAll("_", " ")}</strong> because the tweets have ${music.topKeywords.size()} main keywords ${music.topKeywords}</li>
</ul>

<div class="centered" style="text-align: center; margin-top: 20px; font-size: 1.3em;">
    <a href="${root}/twitterMusic">Want to hear your twitter music?</a>
</div>
<%@ include file="footer.jsp" %>