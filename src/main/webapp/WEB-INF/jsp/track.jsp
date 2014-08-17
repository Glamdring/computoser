<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="head">
<title>${title} - track #${id} - Computoser</title>
<%@ include file="playerScripts.jsp" %>
</c:set>
<%@ include file="header.jsp" %>

<h3>${title} (computer-generated track #${id})</h3>
<c:if test="${likes > 0}">
    Likes: ${likes}
</c:if>
<%@ include file="player.jsp" %>

<%@ include file="footer.jsp" %>