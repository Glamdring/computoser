<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="currentPage" value="stockmusic" />
<c:set var="head">
<title>Free stock music packs - Computoser</title>
</c:set>
<%@ include file="header.jsp" %>
<%@ include file="playerScripts.jsp" %>
Below is a list of <span style="font-weight: bold;">free stock audio packs</span>, curated for specific use-cases. <br />
If you use them, you are welcome to <a href="mailto:bozhidar.bozhanov@gmail.com">contact me</a> with your story of how they worked for you.<br /><br />

<div style="float: right;">
Feel free to donate
<form action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
<input type="hidden" name="cmd" value="_s-xclick">
<input type="hidden" name="encrypted" value="-----BEGIN PKCS7-----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-----END PKCS7-----
">
<input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
<img alt="" border="0" src="https://www.paypalobjects.com/en_US/i/scr/pixel.gif" width="1" height="1">
</form>
</div>


<c:forEach items="${packs}" var="pack">
    <div style="margin-bottom: 20px;">
        <h4>${pack.name}</h4>
        <p>${pack.description}</p>
        <a href="javascript:void(0);" onclick="shouldAutoPlay = false; ids = []; <c:forEach items="${pack.pieces}" var="piece">ids.push(${piece.id});</c:forEach>$('.pieceTable').hide(); $('#pack-${pack.id}').toggle(); $('#playerContainer').show();">Listen</a> | <a href="${root}/pack/download?id=${pack.id}" rel="nofollow">Download</a>
        <c:set var="pieces" value="${pack.pieces}" />
        <div id="pack-${pack.id}" class="pieceTable" style="display: none;">
        <%@ include file="tracksTable.jsp" %>
        </div>
    </div>
</c:forEach>

<div id="playerContainer" style="display: none;">
    <%@ include file="player.jsp" %>
</div>

<%@ include file="footer.jsp" %>