<%@ page pageEncoding="UTF-8" %>
<%@ include file="includes.jsp" %>
<c:set var="currentPage" value="cart" />
<c:if test="${!pieces.isEmpty()}">
    <c:set var="pieceId" value="${pieces.get(0).id}" />
</c:if>
<c:set var="head">
<title>Checkout - Computoser</title>
<script src="//code.jquery.com/ui/1.10.4/jquery-ui.js"></script>
<link rel="stylesheet" href="//code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css" />
<style type="text/css">
#payment-form input {
    width: auto;
}
</style>
<%@ include file="playerScripts.jsp" %>
<script type="text/javascript">
  var PAYMILL_PUBLIC_KEY = '${paymentPublishableKey}';
</script>
<script type="text/javascript" src="https://bridge.paymill.com/"></script>
<script type="text/javascript">
<c:forEach items="${pieces}" var="piece">
ids.push(${piece.id});
</c:forEach>
autoAdvance = true;

function paymillResponseHandler(error, result) {
  if (error) {
    // Shows the error above the form
    $(".payment-errors").text(error.apierror);
    $(".submit-button").removeAttr("disabled");
  } else {
    // Output token
    var token = result.token;
    // Insert token into form in order to submit to server
    $("#paymentToken").val(token);
    $("#checkoutForm").submit();
  }
  $(".submit-button").text("Checkout");
}

$(document).ready(function() {
        $('#checkoutButton').click(function(){
            var email = $('#email').val();
            if (${!userLoggedIn} && (!email || email.length == 0 || email.indexOf("@") == -1)) { // simple validation here; actual - on the server
                alert("Please enter a valid email");
            } else {
                $("#creditCardDetailsDialog").dialog();
                $("#payment-form").submit(function(event) {
                    // Deactivate submit button to avoid further clicks
                    $('.submit-button').attr("disabled", "disabled");
                    $('.submit-button').text("Please wait...");

                    paymill.createToken({
                        number: $('.card-number').val().replace("-", "").replace(" ", ""),  // required
                        exp_month: $('.card-expiry-month').val(),   // required
                        exp_year: $('.card-expiry-year').val(),     // required
                        cvc: $('.card-cvc').val(),                  // required
                        amount_int: $('.card-amount-int').val(),    // required
                        currency: $('.card-currency').val(),    // required
                        cardholder: $('.card-holdername').val() // optional
                    }, paymillResponseHandler);

                    return false;
                });
            }
            return false;
      });

  $('#payWithBitcoin').click(function() {
      var email = $('#email').val();
      if (${!userLoggedIn} && (!email || email.length == 0 || email.indexOf("@") == -1)) { // simple validation here; actual - on the server
          alert("Please enter a valid email");
      } else {
          $.post("${root}/cart/bitcoinCheckout", {email: email}, function(data) {
              $("#bitcoinPurchase").attr("data-code", data);
              $(".coinbase-button").each(function (b, d) {
                  var f, g, h, i, j, k;
                  return f = $(d), h = f.data(), h.referrer = document.URL, j = $.param(h), g = f.data("code"), k = f.data("width") || 195, i = f.data("height") || 46, f.data("button-style") !== "none" && f.replaceWith("<iframe src='" + c + "/buttons/" + g + "?" + j + "' id='coinbase_button_iframe_" + g + "' name='coinbase_button_iframe_" + g + "' style='width: " + k + "px; height: " + i + "px; border: none; overflow: hidden;' scrolling='no' allowtransparency='true' frameborder='0'></iframe>"), $("body").append("<iframe src='https://coinbase.com/buttons/" + g + "/widget?" + j + "' id='coinbase_modal_iframe_" + g + "' name='coinbase_modal_iframe_" + g + "' style='background-color: transparent; border: 0px none transparent; overflow: hidden; display: none; position: fixed; visibility: visible; margin: 0px; padding: 0px; left: 0px; top: 0px; width: 100%; height: 100%; z-index: 9999;' scrolling='no' allowtransparency='true' frameborder='0'></iframe>");
              });
              $("#coinbase_modal_iframe_" + data).load(function() {
                  $(document).trigger('coinbase_show_modal', data);
              });
          });
      }
  });

  $(document).on('coinbase_payment_complete', function(event, code){
      $.post("${root}/cart/clear", function() {
          window.location = "/?message=Checkout successful. Check your email.";
      });
  });
});
</script>
</c:set>
<%@ include file="header.jsp" %>

<h3>Cart</h3>
<div class="row">
<div class="span7">
<c:if test="${pieces.isEmpty()}">
    <h4>Your cart is empty. Listen to some tracks and add them to the cart.</h4>
</c:if>
<table class="table table-bordered table-striped">
    <thead>
        <tr>
            <th style="width: 70px;">Track ID</th>
            <th>Title</th>
            <th>Likes</th>
            <th>Scale</th>
            <th></th>
            <th></th>
        </tr>
    </thead>
    <tbody>
        <c:forEach items="${pieces}" var="piece" varStatus="stats">
            <tr id="row-${piece.id}">
                <td>${piece.id}</td>
                <td><a href="${root}/track/${piece.id}">${piece.title}</a></td>
                <td>${piece.likes}</td>
                <td>${piece.scale.displayName}</td>
                <td><a href="javascript:playByIdx(${stats.index}, ${piece.id})">Play</a></td>
                <td><a href="${root}/cart/remove?pieceId=${piece.id})">Remove</a></td>
            </tr>
        </c:forEach>
    </tbody>
</table>
<c:if test="${pieces.size() == 30}">
    <a href="${root}/toptracks?page=${param.page + 1}">Next page ></a>
</c:if>
<br />
Note: After purchasing these tracks, you are free to use them for commercial purposes<br /><br />
<c:if test="${!pieces.isEmpty()}">
<div id="creditCardDetailsDialog" style="display: none;">
    <form id="payment-form" action="#" method="POST">
      <div class="payment-errors"></div>
      <input class="card-amount-int" type="hidden" value="${price}" />
      <input class="card-currency" type="hidden" value="USD" />

      <div class="form-row"><label>Card number</label>
      <input class="card-number" type="text" size="20" /></div>

      <div class="form-row"><label>CVC</label>
      <input class="card-cvc" type="text" size="4" /></div>

      <div class="form-row"><label>Name</label>
      <input class="card-holdername" type="text" size="20" /></div>

      <div class="form-row"><label>Expiry date (MM/YYYY)</label>
      <input class="card-expiry-month" type="text" size="2" />
      <span>/</span>
      <input class="card-expiry-year" type="text" size="4" /></div>

      <button class="submit-button" type="submit">Checkout</button>
    </form>
</div>

    <form action="${root}/cart/checkout" id="checkoutForm">
        <c:if test="${!userLoggedIn}">
            Your email: <input type="text" name="email" id="email" style="margin-top: 9px;" /><br />
        </c:if>
        <c:if test="${paymentEnabled}">
            <strong>Total (credit card): $<fmt:formatNumber value="${price / 100}" minFractionDigits="2" maxFractionDigits="2" /></strong> (each 2 tracks cost $0.99)<br />
            <input type="hidden" name="paymentToken" id="paymentToken" />
            <input type="button" class="btn btn-success" id="checkoutButton" value="Pay with credit card" />
        </c:if>
    </form>

    <c:if test="${bitcoinEnabled}">
        <strong>Total (bitcoin payment): $<fmt:formatNumber value="${pieces.size() * 0.5}" minFractionDigits="2" maxFractionDigits="2" /></strong> (each track costs $0.50)<br />
        <a id="payWithBitcoin" href="javascript:void(0);"><img src="${staticRoot}/img/bitcoin.png" style="border-style: none;"/></a>
        <div class="coinbase-button" id="bitcoinPurchase" data-button-style="none"></div>
        <script src="https://coinbase.com/assets/button.js" type="text/javascript"></script>
    </c:if>
</c:if>
</div>
<div class="span5">
<c:if test="${!pieces.isEmpty()}">
    <%@ include file="player.jsp" %>
</c:if>
</div>
</div>
<%@ include file="footer.jsp" %>