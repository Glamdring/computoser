
<%@ page pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
<link rel="shortcut icon" href="${staticRoot}/img/favicon.png" />
<link href="${staticRoot}/styles/bootstrap.min.css" rel="stylesheet" type="text/css" media="screen" />
<!-- link href="${staticRoot}/styles/bootstrap-responsive.min.css" rel="stylesheet" type="text/css" media="screen"/-->
<link href="${staticRoot}/styles/main.css" rel="stylesheet" type="text/css" media="screen"/>
<link href="${staticRoot}/playerSkins/jplayer.blue.monday.css" rel="stylesheet" type="text/css" media="screen"/>
<link href="${baseUrl}/${root}/rss" title="Computoser - computer-generated tracks feed" type="application/rss+xml" rel="alternate" />
<meta name="viewport" content="width=device-width, initial-scale=1">

<script type="text/javascript" src="${staticRoot}/scripts/jquery-1.10.2.min.js"></script>
<script type="text/javascript" src="${staticRoot}/scripts/jquery.jplayer.min.js"></script>
<script type="text/javascript" src="${staticRoot}/scripts/bootstrap.min.js"></script>
${head}
<meta name="description" content="Algorithmic computer-generated, artificial-intelligence music that can be used for anything - listening to, building upon or commercial purposes (free stock audio/elevator music/on hold music)." />
<meta name="keywords" content="stock,audio,algorithm,music,algorithmic,computer-generated,elevator,supermarket,hold,free,artificial,intelligence" />
</head>
<body>
<c:if test="${currentPage =='home'}">
    <div id="fb-root"></div>
    <script>(function(d, s, id) {
      var js, fjs = d.getElementsByTagName(s)[0];
      if (d.getElementById(id)) return;
      js = d.createElement(s); js.id = id;
      js.src = "//connect.facebook.net/en_US/all.js#xfbml=1";
      fjs.parentNode.insertBefore(js, fjs);
    }(document, 'script', 'facebook-jssdk'));</script>
    <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src="//platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>
</c:if>
    <div class="container">
        <div class="masthead">
            <ul class="nav nav-pills pull-right">
                <li <c:if test="${currentPage == 'home'}">class="active"</c:if>><a href="${root}/">Home</a></li>
                <li <c:if test="${currentPage == 'stockmusic'}">class="active"</c:if>><a href="${root}/stockmusic"><img src="${staticRoot}/img/stock-packs.png" style="width: 16px; height: 16px; margin-right: 5px; border-style: none"/>Stock music</a></li>
                <li <c:if test="${currentPage == 'twittermusic'}">class="active"</c:if>><a href="${root}/twitterMusic"><img src="${staticRoot}/img/twitter.png" style="width: 16px; height: 16px; margin-right: 5px; border-style: none"/>Twitter music</a></li>
                <li <c:if test="${currentPage == 'game'}">class="active"</c:if>><a href="${root}/game">Game</a></li>
                <li <c:if test="${currentPage == 'toptracks' || currentPage == 'toprecent'}">class="active"</c:if>><a href="${root}/toptracks">Top Tracks</a></li>
                <li <c:if test="${currentPage == 'search'}">class="active"</c:if>><a href="${root}/search">Search</a></li>
                <c:if test="${userLoggedIn}">
                    <li <c:if test="${currentPage == 'mytracks'}">class="active"</c:if>><a href="${root}/mytracks">My Tracks</a></li>
                </c:if>
                <c:if test="${!userLoggedIn}">
                    <li <c:if test="${currentPage == 'news'}">class="active"</c:if>><a href="${root}/news">News</a></li>
                </c:if>
                <li <c:if test="${currentPage == 'about'}">class="active"</c:if>><a href="${root}/about">About</a></li>
                <c:if test="${userLoggedIn}">
                    <li><a href="${root}/logout" onclick="navigator.id.logout();">Logout</a></li>
                </c:if>
                <c:if test="${paymentEnabled || bitcoinEnabled}">
                    <li <c:if test="${currentPage == 'cart'}">class="active"</c:if>><a href="https://${pageContext.request.serverName}${root}/cart"><img src="${staticRoot}/img/cart.png" style="width: 16px; height: 16px; margin-right: 5px; border-style: none"/>Cart</a></li>
                </c:if>
            </ul>
            <a href="${root}/"><img src="${staticRoot}/img/logo.png" class="logo" /><h3 class="muted">Computoser</h3></a>
        </div>
        <hr style="width: 100%;"/>
        <c:if test="${!empty param.message}">
            <div style="color: green; text-align: center; font-size: 15pt;">${param.message}</div>
        </c:if>
