<script type="text/javascript">
var predefinedId = '${pieceId}';
var lazyLoad = false;
var autoAdvance = ${empty pieceId ? 'true' : 'false'};
var ids = [];
var currentPlayedIdx = -1;
var previousPlayedId = -1;
var shouldAutoPlay = false;
var android = ${request.getHeader('User-Agent').contains('Android') ? 'true' : 'false'};
var error = false;
var jPlayerOptions = {swfPath: "${staticRoot}/scripts/jplayer.swf", supplied: "mp3", solution: "html,flash", ready: function() {
    var player = $(this);
    if (predefinedId == '' && !lazyLoad) {
        loadTrack();
    } else if (!lazyLoad){
        currentPlayedIdx = 0;
        ids.push(predefinedId);
        play(predefinedId);
        setTitle(predefinedId);
    }
}, ended:function(e) {
    if (autoAdvance) {
        // wait 4 seconds before starting to load the next track (effectively ~6 seconds pause between plays)
        setTimeout(function() {
            next();
        }, 4000);
    }
}, play: function(e){
    shouldAutoPlay = true;
}, error: function(e){
    //storeJsError(); TODO send error to server and store
    console.log(e);
    newAlert("warning", "Sorry, a problem occured. Please click 'next' or refresh the page");
}};

$(document).ready(function() {
    $("#jplayer").jPlayer(jPlayerOptions);
});

function loadTrack() {
    $("#trackTitle").text("Loading track...");
    $.get("${root}/music/get", $("#preferences").serialize(), function(id) {
        playById(id);
    });
}

function playById(id) {
    ids.push(id);
    previousPlayed = ids[currentPlayedIdx];
    currentPlayedIdx = ids.length - 1;
    play(id);
    $("#trackTitle").text("");
    setTitle(id);
}

function setTitle(id) {
     $.get("${root}/music/getTitle/" + id, function(title) {
         $("#trackTitle").text(title);
     });
}
function play(id) {
    var player = $("#jplayer");
    player.jPlayer("setMedia", {mp3:"${root}/music/get/" + id});
    if (shouldAutoPlay) {
        player.jPlayer("play");
    }
    $("#twitterLink").attr("href", "https://twitter.com/intent/tweet?text=I%20liked%20a%20computer-generated%20musical%20piece%20http://computoser.com/track/" + id + "&via=computosermusic");
    $("#facebookLink").attr("href", "http://www.facebook.com/sharer.php?u=http://computoser.com/track/" + id);
    $("#permalink").attr("href", "http://computoser.com/track/" + id);
    $("#downloadLink").attr("href", "${root}/music/getMidi/" + id);
    $("#downloadMp3Link").attr("href", "${root}/music/get/" + id + "?download=true");
    $("#downloadXmlLink").attr("href", "${root}/music/getXml/" + id);
    $("#addToCartButton").attr("onclick", "addToCart(" + id + ")");
}

function previous() {
    if (currentPlayedIdx - 1 >= 0) {
        var id = ids[--currentPlayedIdx];
        setTitle(id);
        play(id);
    }
}

function next() {
    if (currentPlayedIdx + 1 < ids.length) {
        var id = ids[++currentPlayedIdx];
        setTitle(id);
        play(id);
    } else {
        loadTrack();
    }
}

function evaluateCurrent(positive) {
    if (!positive) {
        $("#jplayer").jPlayer("stop");
    }
    evaluate(ids[currentPlayedIdx], positive);
    if (!positive) {
        next();
    }
}
function evaluatePrevious(positive) {
    evaluate(previousPlayedId, positive);
}

function evaluate(id, positive) {
    $.post("${root}/music/evaluate/" + id, {positive: positive}, function() {
        newAlert("success", "Thanks for your feedback!");
    });
}

function playByIdx(idx, id) {
    currentPlayedIdx = idx;
    setTitle(id);
    shouldAutoPlay = true;
    play(id);
}

function newAlert(type, message) {
    $("body").append($('<div class="alert-message">' + message + '</div>'));
    $(".alert-message").delay(3000).fadeOut("slow", function () { $(this).remove(); });
}

function addToCart(id) {
    $.post("${root}/cart/add", {pieceId: id}, function() {
        newAlert("success", "Successfully added to cart. You can check your cart from the menu on the top right");
    });
}
</script>