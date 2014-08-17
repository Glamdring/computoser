<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="head">
<title>Music based on your tweets - Computoser</title>

<%@ include file="playerScripts.jsp" %>
<script type="text/javascript">
function triggerTwitterAnalysis() {
    $("#waitLabel").show();
    $("#triggerButton").attr('disabled','disabled');
    $.post("${root}/twitterMusic/trigger");
}
</script>
</c:set>
<%@ include file="header.jsp" %>

<h3>Music based on your tweets</h3>
<c:if test="${!hasTwitter}">
    <a href="${root}/signin/twitter?home=true&redirectUri=/twitterMusic"><img src="${staticRoot}/img/twitter-connect.png" /></a>
</c:if>
<c:if test="${hasTwitter}">
    <input type="button" id="triggerButton" value="Give me my twitter music" class="btn btn-info" onclick="triggerTwitterAnalysis();"/>
</c:if>
<div id="waitLabel" style="display: none; font-size: 1.2em; margin-top: 10px;">You will receive your twitter music on your email soon.</div>


<%@ include file="footer.jsp" %>