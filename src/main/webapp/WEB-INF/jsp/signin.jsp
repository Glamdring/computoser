<%@ page pageEncoding="UTF-8" %>

<c:if test="${!userLoggedIn}">
    <script type="text/javascript">
        $(document).ready(function() {
            var signinLink = $("#personaSignin");
            signinLink.click(function() {
                userRequestedAuthentication = true;
                navigator.id.request({siteName: 'Computoser'});
            });
        });
    </script>
    <a href="javascript:void(0);" id="personaSignin"><img src="${staticRoot}/img/persona-connect.png" /></a>
    <a href="${root}/signin/twitter?home=true"><img src="${staticRoot}/img/twitter-connect.png" /></a>
    <a href="${root}/signin/facebook?home=true&scope=email"><img src="${staticRoot}/img/facebook-connect.png" /></a>
</c:if>