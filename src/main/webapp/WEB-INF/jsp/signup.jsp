<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="currentPage" value="signup" />
<c:set var="head">
<title>Signup - Computoser</title>
</c:set>
<%@ include file="header.jsp" %>

<div class="main">
   <div class="lead">Currently authentication works via Facebook, Twitter and Mozilla Persona:</div>
   <%@ include file="signin.jsp" %>
</div>
<%@ include file="footer.jsp" %>