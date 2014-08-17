<%@ page pageEncoding="UTF-8"%>
<%@ include file="includes.jsp"%>
<c:set var="currentPage" value="game" />
<c:set var="head">
    <title>Computoser game</title>
    <%@ include file="playerScripts.jsp"%>
    <script type="text/javascript"
        src="${staticRoot}/scripts/sockjs.min.js"></script>
    <!-- script type="text/javascript" src="${staticRoot}/scripts/jquery.zclip.min.js"></script-->
    <script type="text/javascript" src="${staticRoot}/scripts/ZeroClipboard.min.js"></script>
</c:set>
<%@ include file="header.jsp"%>

<script type="text/javascript">
//TODO disable leaving the page (back/refresh)

$(document).ready(function(){
    var clip = new ZeroClipboard(document.getElementById("copyJoinLinkButton"), {
          moviePath: "${staticRoot}/scripts/ZeroClipboard.swf"
    });
    clip.on( "load", function(client) {
          client.on( "complete", function(client, args) {
              //this.style.display = "none";
            alert("Copied text to clipboard: " + args.text );
        });
    });
});

lazyLoad = true;
autoAdvance = false;
var currentPieceIdx = 0;
var maxPieces = 0;
var gameId = '${param.gameId}';
var currentPieceId;
var players = [];
var socket = new SockJS("${root}/game/websocket/");
var timeRemaining;
var playerName;
socket.onmessage = function (message) {
    var response = $.parseJSON(message.data);
    switch (response.type) {
        case 'GAME_INITIALIZED':
            gameId = response.gameId;
            $("#joinLinkPanel").show();
            $("#joinLinkInput").val(window.location.href + "?gameId=" + gameId);
            $("#playersList").show();
            refreshPlayerList();
            break;
        case 'GAME_STARTED':
            maxPieces = response.pieceCount;
            $("#waitingLabel").text("");
            $("#joinLinkPanel, #gameButtonsPanel").hide();
            shouldAutoPlay = true;
            $("#nextButton, #previousButton").hide();
            $("#answerPanel").show();
            $("#playerWrapper").css("visibility", "visible");
            break;
        case 'PLAYER_NAME_TAKEN':
            alert("The player name is already in use in this game. Please choose a different one.");
            $("#joinPanel, #playerNamePanel").show();
            $("#waitingLabel").text("");
            $("#playersList").hide();
            break;
        case 'NEW_PIECE':
            currentPieceIdx++;
            $("#progressLabel").text("Piece " + currentPieceIdx + " of " + maxPieces);
            currentPieceId = response.pieceId;
            startTimer(response.seconds);
            $("#waitingLabel").text("");
            $("#answerPanel input").prop("disabled", true)
            $("#correctTempo, #correctMetre, #correctInstrument").hide();
            visualizeAnswers(response.possibleAnswers);
            ids = [];
            ids.push(currentPieceId);
            playByIdx(0, currentPieceId);
            $("#answerPanel input").removeAttr("disabled")
            break;
        case 'PLAYER_JOINED':
            players.push({id: response.playerId, name: response.playerName});
            refreshPlayerList();
            break;
        case 'GAME_FINISHED':
            var rank = response.currentPlayerResult.rank;
            var rankText = rank;
            if (rank == 1) {
                rankText += 'st';
            } else if (rank == 2) {
                rankText += 'nd';
            } else if (rank == 3) {
                rankText += 'rd';
            } else {
                rankText += 'th';
            }
            $("#playerWrapper").css("visibility", "hidden");
            $("#jplayer").jPlayer("stop");
            var msg = "You rank " + rankText + " with score " + response.currentPlayerResult.score;
            msg += "\n\nFull rankings: \n";
            for (var i = 0; i < response.playerResults.length; i ++) {
                msg += (i + 1) + ". " + response.playerResults[i].name + " (score " + response.playerResults[i].score + ")\n";
            }
            alert(msg);
            document.location.href = '${root}/game';
            break;
        case 'ANSWER_ACCEPTED':
            $("#correctTempo").text("Correct answer: " + response.correctAnswer.tempo);
            $("#correctMetre").text("Correct answer: " + response.correctAnswer.metreNumerator + '/' + response.correctAnswer.metreDenominator);
            $("#correctInstrument").text("Correct answer: " + $("#instrumentLabel" + response.correctAnswer.mainInstrument).text());
            $("#correctTempo, #correctMetre, #correctInstrument").show();
            break;
        case 'PLAYER_LEFT':
            for (var i = 0; i < players.length; i ++) {
                if (players[i].id == response.playerId) {
                    players.splice(i, 1);
                    break;
                }
            }
            refreshPlayerList();
            break;
        case 'NO_GAME_AVAILABLE':
            alert("No available game for joining. You can create a new game.");
            break;
        case 'RANDOM_GAME_JOINED':
            $("#gameButtonsPanel, #playerNamePanel").hide();
            $("#waitingLabel").text("Waiting for the host to start the game...");
            $("#playersList").show();
            gameId = response.gameId;
            break;
    }
};

function visualizeAnswers(answers) {
    $("#instrumentAnswers").empty();
    for (var i = 0; i < answers.instruments.length; i++) {
        var id = answers.instruments[i].id;
        var instrumentOption = '<input type="radio" name="instrument" value="' + id +'" id="instrumentOption' + id + '"/>';
        instrumentOption += '<label for="instrumentOption' + id + '" id="instrumentLabel' + id + '">' + answers.instruments[i].name +'</label><br />';
        $("#instrumentAnswers").append(instrumentOption);
    }

    $("#metreAnswers").empty();
    for (var i = 0; i < answers.metres.length; i++) {
        var metreOption = '<input type="radio" name="metre" value="' + answers.metres[i].numerator + '/' + answers.metres[i].denominator + '" id="metreOption' + i + '"/>';
        metreOption += '<label for="metreOption' + i + '">' + answers.metres[i].numerator + '/' + answers.metres[i].denominator +'</label><br />';
        $("#metreAnswers").append(metreOption);
    }
}
function refreshPlayerList() {
    $("#playersList").empty();
    $("#playersList").append('<strong>' + playerName + '</strong><br />'); //self ontop
    for (var i = 0; i < players.length; i ++) {
        $("#playersList").append(players[i].name + "<br />");
    }
}

function createGame() {
    var request = {
        action: 'INITIALIZE',
        playerName: $('#playerName').val()
    }
    playerName = request.playerName;
    socket.send(JSON.stringify(request));
    $("#gameButtonsPanel, #playerNamePanel").hide();
    $("#startGameButton").show();
}

function startGame() {
    var request = {
        action: 'START',
        gameId: gameId
    }
    socket.send(JSON.stringify(request));
    $("#startGameButton").hide();
}

function joinGame() {
    var request = {
        action: 'JOIN',
        gameId: gameId,
        playerName: $('#playerName').val()
    }
    playerName = request.playerName;
    socket.send(JSON.stringify(request));
    $("#joinPanel, #playerNamePanel").hide();
    $("#waitingLabel").text("Waiting for the host to start the game...");
    $("#playersList").show();
}

function joinRandomGame() {
    var request = {
        action: 'JOIN_RANDOM',
        playerName: $('#playerName').val()
    }
    playerName = request.playerName;
    socket.send(JSON.stringify(request));
}
function answer() {
    var metre = $("input[name=metre]:checked").val();
    if (metre == '' || metre === undefined) { //nothing selected
        metre = "0/0";
    }
    var mainInstrument = $("input[name=instrument]:checked").val();
    if (mainInstrument == '' || mainInstrument === undefined) {
        mainInstrument = -1;
    }
    var tempo = $('#tempo').val();
    if (tempo == '' || tempo === undefined) {
        tempo = -1;
    }
    var metreArray = metre.split("/")
    var request = {
        action: 'ANSWER',
        gameId: gameId,
        answer: {
            tempo: tempo,
            mainInstrument: mainInstrument,
            metreNumerator: metreArray[0],
            metreDenominator: metreArray[1]
        }
    }
    socket.send(JSON.stringify(request));
    $("#answerButton").prop("disabled", true);
    $("#waitingLabel").text("Waiting for other players to answer...");
}
var seconds;
var intervalId;
function startTimer(seconds) {
    //TODO circular ui
    if (intervalId != null) {
        clearInterval(intervalId);
    }
    intervalId = setInterval(function() {
        if (seconds == 0) {
            return;
        }
        var minutes = Math.floor(seconds / 60);
        var secs = seconds % 60;
        var text = "";
        if (minutes > 0) {
            text = minutes + " min. "
        }
        text += secs + "sec.";
        $("#countdownTimer").text(text);
        seconds--;
    }, 1000);
}
</script>

<div id="playerNamePanel">
    In this game, you will have to guess the tempo and main instrument of computer-generated pieces. You can play it by yourself or with other players. <br /><br />
    Player name: <input type="text" name="playerName" id="playerName" />
</div>
<div id="waitingLabel"></div>
<div id="progressLabel"></div>

<c:if test="${!empty param.gameId}">
<div id="joinPanel">
    <input type="button" onclick="joinGame()" value="Join game" class="btn btn-success" />
</div>
</c:if>
<div id="gameButtonsPanel">
<c:if test="${empty param.gameId}">
    <input type="button" onclick="createGame()" id="createGameButton" value="Create new game" class="btn btn-success" />
    or
    <input type="button" onclick="joinRandomGame()" id="joinRandomGameButton" value="Join random game" class="btn btn-success" />
</c:if>
</div>
<input type="button" onclick="startGame()" id="startGameButton" value="Start game" style="display: none;" class="btn btn-success" />
<div id="joinLinkPanel" style="display: none;">
    Send this link to your friends with whom you'd like to play:
    <input type="text" id="joinLinkInput" style="width: 200px; margin-bottom: 0px;" />
    <input type="button" id="copyJoinLinkButton" data-clipboard-target="joinLinkInput" value="copy" class="btn" style="margin-top: 0px;"/>
    (Note: you can play alone)
</div>

<div class="row">
    <div class="span3"></div>
    <div id="answerPanel" class="span6" style="display: none; margin-bottom: 5px;">
        Beats-per-minute (45 to 185): <br />
        <input type="text" name="tempo" id="tempo" style="width: 60px;"/>
        <span id="correctTempo" style="font-weight: bold;"></span>
        <div style="margin-bottom: 5px; margin-left: auto; margin-right: auto; width: 380px;">
            <div style="float: left;">
                Main instrument: <br />
                <span id="correctInstrument" style="font-weight: bold;"></span>
                <div id="instrumentAnswers"></div>
            </div>
            <div style="float: right">
                Metre: <br />
                <span id="correctMetre"  style="font-weight: bold;"></span>
                <div id="metreAnswers"></div>
            </div>
        </div>
        <div style="clear:both; float: right;">
            <input type="button" onclick="answer();" value="Send" id="answerButton" class="btn btn-success"/>
        </div>
    </div>

    <div id="sidePanel" class="span3">
        <div id="playersList" style="display: none; float: left; border-style: solid; border-width: 1px; padding: 5px;">
        </div>
        <div id="countdownTimer" style="float: left; margin-left: 30px; color: red; font-weight: bold;">
        </div>
    </div>
</div>

<div id="playerWrapper" style="visibility: hidden;">
    <%@ include file="player.jsp"%>
</div>

<%@ include file="footer.jsp"%>