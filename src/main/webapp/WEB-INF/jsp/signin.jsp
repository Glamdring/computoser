<%@ page pageEncoding="UTF-8" %>

<c:if test="${!userLoggedIn}">
    <a href="${root}/signin/twitter?home=true"><img src="${staticRoot}/img/twitter-connect.png" /></a>
    <a href="${root}/signin/facebook?home=true&scope=email"><img src="${staticRoot}/img/facebook-connect.png" /></a>
</c:if>