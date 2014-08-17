<table class="table table-bordered table-striped">
    <thead>
        <tr>
            <th style="width: 70px;">Track ID</th>
            <th>Title</th>
            <th>Likes</th>
            <th>Scale</th>
            <th></th>
        </tr>
    </thead>
    <tbody>
        <c:forEach items="${pieces}" var="piece" varStatus="stats">
            <tr>
                <td>${piece.id}</td>
                <td><a href="${root}/track/${piece.id}">${piece.title}</a></td>
                <td>${piece.likes}</td>
                <td>${piece.scale.displayName}</td>
                <td><a href="javascript:playByIdx(${stats.index}, ${piece.id})">Play</a></td>
            </tr>
        </c:forEach>
    </tbody>
</table>