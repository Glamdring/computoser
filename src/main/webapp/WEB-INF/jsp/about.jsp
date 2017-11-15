<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="currentPage" value="about" />
<c:set var="head">
<title>About - Computoser</title>
</c:set>
<%@ include file="header.jsp" %>

<div class="lead">Computoser uses an algorithm to generate music. Each generated track is a unique combination of tones, rhythm and instruments.<br /><br />
The algorithm is currently experimental - it may generate both good and bad pieces. Feel free to mark the ones you like and the ones you dislike, so that I know how to improve the algorithm.<br /></br />
The performance may sound a bit artificial - that's because it's synthesized music - a computer can hardly have the performance of a human musician.<br /><br />
All the tracks are licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.<br /><br />
<c:if test="${paymentEnabled || bitcoinEnabled}">If they are needed for commercial purposes (e.g. as stock audio/elevator music), a very options are available - you can get two pieces for 0.99 USD or a pre-curated pack of 15 pieces for 5 USD</c:if></div>

Created by <a href="http://stackoverflow.com/users/203907/bozho">Bozhidar Bozhanov</a> (<a href="mailto:bozhidar.bozhanov@gmail.com">email</a>)
<%@ include file="footer.jsp" %>
