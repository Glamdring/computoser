<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="currentPage" value="news" />
<c:set var="head">
<title>News - Computoser</title>
<style type="text/css">
li {
    line-height: 30px;
}
</style>
</c:set>
<%@ include file="header.jsp" %>
<c:set var="newLine" value="\n" />

<c:forEach items="${news}" var="entry">
<h3><a href="news?id=${entry.id}">${entry.title}</a></h3>
<h6><fmt:formatDate value="${entry.publishTime.toDate()}" pattern="dd.MM.yyyy"/></h6>
<div class="lead"><pre style="word-break: normal;">${entry.content}</pre></div>

</c:forEach>

<%@ include file="footer.jsp" %>