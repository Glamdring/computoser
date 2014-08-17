<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="currentPage" value="${currentPage}" />
<c:if test="${!pieces.isEmpty()}">
    <c:set var="pieceId" value="${pieces.get(0).id}" />
</c:if>
<c:set var="head">
<title>${title} - Computoser</title>
<c:if test="${!pieces.isEmpty()}">
    <%@ include file="playerScripts.jsp" %>
</c:if>
<script type="text/javascript">
<c:forEach items="${pieces}" var="piece">
ids.push(${piece.id});
</c:forEach>
autoAdvance = true;
</script>
</c:set>
<%@ include file="header.jsp" %>

<h3>${title}</h3>
<div class="row">
<div class="span7">
<c:if test="${searchEnabled}">
    <form id="searchCriteria" action="${root}/search" class="searchForm">
        <c:set var="customizationsSpanWidth" value="3" />
        <%@ include file="customizations.jsp" %>
        <div style="clear: both;"></div>
        <input type="submit" value="Search" style="float: right; margin-bottom: 5px;" class="btn btn-success" />
    </form>
</c:if>
<c:if test="${currentPage.startsWith('top')}">
    <a href="${root}/toptracks">Top tracks all time</a> | <a href="${root}/toprecent">Top recent tracks</a><br />
</c:if>
<%@ include file="tracksTable.jsp" %>
<c:if test="${pieces.size() == 30}">
    <a href="?page=${param.page + 1}">Next page ></a>
</c:if>
</div>
<div class="span5">
<c:if test="${!pieces.isEmpty()}">
    <%@ include file="player.jsp" %>
</c:if>
</div>
</div>
<%@ include file="footer.jsp" %>