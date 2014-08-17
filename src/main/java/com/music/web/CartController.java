/*
 * Computoser is a music-composition algorithm and a website to present the results
 * Copyright (C) 2012-2014  Bozhidar Bozhanov
 *
 * Computoser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Computoser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Computoser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.music.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hibernate.validator.constraints.impl.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.model.persistent.Piece;
import com.music.model.persistent.PiecePack;
import com.music.service.PiecePackService;
import com.music.service.PieceService;
import com.music.service.PurchaseService;

@Controller
@RequestMapping("/cart")
@Deprecated // no purchasing available
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Inject
    private Cart cart;
    @Inject
    private UserContext userContext;
    @Inject
    private PurchaseService purchaseService;
    @Inject
    private PieceService pieceService;
    @Inject
    private PiecePackService piecePackService;

    @Value("${payment.publishable.key}")
    public String publishableKey;

    private EmailValidator emailValidator = new EmailValidator();
    private ObjectMapper mapper = new ObjectMapper();

    @ModelAttribute("paymentPublishableKey")
    public String getPublishableKey() {
        return publishableKey;
    }

    @RequestMapping("")
    public String view(Model model) {
        List<Piece> pieces = pieceService.getPieces(new ArrayList<>(cart.getPieceIds()));
        List<PiecePack> piecePacks = piecePackService.getPiecePacks(new ArrayList<>(cart.getPiecePackIds()));
        model.addAttribute("pieces", pieces);
        model.addAttribute("piecePacks", piecePacks);
        model.addAttribute("price", purchaseService.calculatePrice(cart));
        return "cart";
    }

    @RequestMapping("/add")
    @ResponseBody
    public void addToCart(@RequestParam Long pieceId) {
        cart.getPieceIds().add(pieceId);
    }

    @RequestMapping("/addPack")
    @ResponseBody
    public void addPackToCart(@RequestParam Long piecePackId) {
        cart.getPiecePackIds().add(piecePackId);
    }

    @RequestMapping("/remove")
    public String removeFromCart(@RequestParam Long pieceId) {
        cart.getPieceIds().remove(pieceId);
        return "redirect:/cart";
    }

    @RequestMapping("/removePack")
    public String removePackFromCart(@RequestParam Long piecePackId) {
        cart.getPieceIds().remove(piecePackId);
        return "redirect:/cart";
    }

    @RequestMapping("/clear")
    @ResponseBody
    public void clearCart() {
        cart.getPieceIds().clear();
        cart.getPiecePackIds().clear();
    }

    @RequestMapping("/checkout")
    public String checkout(@RequestParam(required=false) String email, @RequestParam("paymentToken") String token) {
        Long userId = null;
        if (userContext.getUser() != null) {
            userId = userContext.getUser().getId();
        } else {
            if (!emailValidator.isValid(email, null)) {
                return "redirect:/cart/checkout?message=Your email was invalid";
            }
        }
        purchaseService.checkout(cart, userId , email, token);
        clearCart();
        return "redirect:/?message=Checkout successful. Check your email";
    }

    @RequestMapping("/bitcoinCheckout")
    @ResponseBody
    public String bitcoinCheckout(@RequestParam(required=false) String email) {
        Long userId = null;
        if (userContext.getUser() != null) {
            userId = userContext.getUser().getId();
        } else {
            if (!emailValidator.isValid(email, null)) {
                throw new IllegalArgumentException("Invalid email");
            }
        }
        long purchaseId = purchaseService.bitcoinCheckout(new ArrayList<>(cart.getPieceIds()), userId , email);

        long amount = cart.getPieceIds().size();
        Map<String, BigDecimal> rates = purchaseService.getConversionRates();
        String code = purchaseService.getButtonCode(rates.get("usd_to_btc").multiply(BigDecimal.valueOf(0.50d)).multiply(BigDecimal.valueOf(amount)), purchaseId);

        return code;
    }

    @RequestMapping("/confirmPurchase")
    @ResponseBody
    public void confirm(@RequestBody String json) throws Exception {
        JsonNode root = mapper.readTree(json).get("event");
        if (root.get("event_type").asText().equals("transaction.succeeded")) {
            purchaseService.paymentComplete(root.get("event_resources").get("transaction").get("id").asText());
        } else {
            logger.warn("Event received from Payment Gateway: " + root.get("event_type").asText());
        }
    }

    @RequestMapping("/confirmBitcoinPurchase")
    @ResponseBody
    public void confirmBitcoin(@RequestBody String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode order = root.get("order");
        if (order.get("status").asText().equals("completed")) {
            String paymentId = order.get("id").asText();
            long purchaseId = Long.parseLong(order.get("custom").asText());
            purchaseService.bitcoinPaymentComplete(paymentId, purchaseId);
        }
    }
}
