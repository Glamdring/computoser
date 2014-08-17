<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set value="${pageContext.request.contextPath}/static" var="staticRoot" />
<c:set value="${pageContext.request.contextPath}" var="root" />

<c:if test="${root == '//'}">
    <c:set value="" var="root" />
    <c:set value="/static" var="staticRoot" />
</c:if>

<c:set var="userLoggedIn" value="${userContext.user != null}" />