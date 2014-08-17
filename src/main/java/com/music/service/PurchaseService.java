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

package com.music.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.music.dao.PieceDao;
import com.music.model.persistent.PaymentType;
import com.music.model.persistent.Piece;
import com.music.model.persistent.PiecePack;
import com.music.model.persistent.Purchase;
import com.music.model.persistent.User;
import com.music.service.EmailService.EmailDetails;
import com.music.util.SecurityUtils;
import com.music.web.Cart;

@Service
@Deprecated //purchasing not available anymore
public class PurchaseService {

    @Inject PieceDao dao;

    @Inject
    private EmailService emailService;
    @Inject
    private PieceService pieceService;

    @Value("${payment.secret.key}")
    private String secret;
    @Value("${base.url}")
    private String baseUrl;
    @Value("${hmac.key}")
    private String hmacKey;
    @Value("${coinbase.key}")
    private String coinbaseKey;
    @Value("${information.email.sender}")
    private String emailSender;

    private RestTemplate template = new RestTemplate();
    private ObjectMapper jsonMapper = new ObjectMapper();

    //private PaymillContext paymentContext;

    @PostConstruct
    public void init() {
        jsonMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        template.getInterceptors().add(new ClientHttpRequestInterceptor() {

            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                    throws IOException {
                request.getHeaders().add("Accept", "application/json");
                request.getHeaders().add("Content-Type", "application/json");
                request.getHeaders().add("User-Agent", "");
                return execution.execute(request, body);
            }
        });
        //paymentContext = new PaymillContext(secret);
    }

    @Transactional
    public void checkout(Cart cart, Long userId, String email, String purchaseToken) {
        Purchase purchase = new Purchase();
        purchase.setPurchaseTime(new DateTime());
        purchase.setPaymentType(PaymentType.CREDITCARD);
        if (userId != null) {
            purchase.setUser(dao.getById(User.class, userId));
            purchase.setEmail(purchase.getUser().getEmail());
        } else {
            purchase.setEmail(email);
        }
        if (!cart.getPieceIds().isEmpty()) {
            purchase.setPieces(new HashSet<>(dao.getByIds(Piece.class, cart.getPieceIds())));
        }
        if (!cart.getPiecePackIds().isEmpty()) {
            purchase.setPacks(new HashSet<>(dao.getByIds(PiecePack.class, cart.getPiecePackIds())));
        }

        dao.persist(purchase);

//        Payment payment = paymentContext.getPaymentService().createWithToken(purchaseToken);
//        Transaction transaction = paymentContext.getTransactionService().createWithPayment(payment, calculatePrice(cart), "USD");
//        try {
//            purchase.setPaymentProviderId(transaction.getId());
//            purchase = dao.persist(purchase);
//            // if the transaction is synchronously confirmed as closed
//            if (transaction.getStatus() == Status.CLOSED) {
//                purchase.setComplete(true);
//                dao.persist(purchase);
//                sendPurchaseEmail(purchase);
//            } else {
//                throw new IllegalStateException("Transaction status is unexpected: " + transaction.getStatus());
//            }
//        } catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
    }

    public Integer calculatePrice(Cart cart) {
        return (int) ((int) Math.ceil(cart.getPieceIds().size() / 2d) * 100 * 0.99
                + cart.getPiecePackIds().size() * 100 * 5);
    }

    @Transactional
    public void paymentComplete(String paymentProviderId) {
        Purchase purchase = dao.getByPropertyValue(Purchase.class, "paymentProviderId", paymentProviderId);
        // if no purchase found, or purchase already complete, ignore
        if (purchase == null || purchase.isComplete()) {
            return;
        }
        purchase.setComplete(true);
        dao.persist(purchase);
        sendPurchaseEmail(purchase);
    }

    private void sendPurchaseEmail(Purchase purchase) {
        Map<String, Object> model = new HashMap<>();
        String paddedString = StringUtils.leftPad(String.valueOf(purchase.getId()), 10, '0');
        model.put("zipUrl", baseUrl + "/purchase/download/" + purchase.getId() + "/"
                        + SecurityUtils.hmac(paddedString, hmacKey));

        EmailDetails emailDetails = new EmailDetails().addTo(purchase.getEmail())
                .setFrom(emailSender)
                .setHtml(true)
                .setSubject("Your Computoser purchase")
                .setMessageTemplate("purchase.vm")
                .setMessageTemplateModel(model);

        emailService.send(emailDetails);
    }

    @Transactional(readOnly=true)
    public void download(long id, OutputStream out) throws IOException {
        Purchase purchase = dao.getById(Purchase.class, id);
        if (!purchase.isComplete()) {
            return;
        }

        pieceService.downloadPieces(out, purchase.getPieces());
    }

    @Cacheable("bitcoinRatesCache")
    public Map<String, BigDecimal> getConversionRates() {
        ResponseEntity<String> entity = template.getForEntity("https://coinbase.com/api/v1/currencies/exchange_rates?api_key=" + coinbaseKey, String.class);
        String json = entity.getBody();

        try {
            Map<String, BigDecimal> result = new HashMap<>();
            JsonNode node = jsonMapper.readTree(json);
            result.put("btc_to_usd", new BigDecimal(node.get("btc_to_usd").asText()));
            result.put("usd_to_btc", new BigDecimal(node.get("usd_to_btc").asText()));
            result.put("btc_to_eur", new BigDecimal(node.get("btc_to_eur").asText()));
            result.put("eur_to_btc", new BigDecimal(node.get("eur_to_btc").asText()));

            return result;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getButtonCode(BigDecimal price, long purchaseId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ButtonRequest buttonRequest = new ButtonRequest();
        buttonRequest.setCurrency("btc");
        buttonRequest.setCustom(String.valueOf(purchaseId));
        buttonRequest.setPrice(price.toPlainString());
        buttonRequest.setType("buy_now");
        buttonRequest.setName("Computer-generated tracks");
        ResponseEntity<String> entity = template.postForEntity("https://coinbase.com/api/v1/buttons?api_key=" + coinbaseKey, buttonRequest, String.class);
        String json = entity.getBody();

        try {
            JsonNode node = jsonMapper.readTree(json);

            return node.get("button").get("code").asText();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public long bitcoinCheckout(ArrayList<Long> ids, Long userId, String email) {
        Purchase purchase = new Purchase();
        purchase.setPaymentType(PaymentType.BITCOIN);
        purchase.setPurchaseTime(new DateTime());
        if (userId != null) {
            purchase.setUser(dao.getById(User.class, userId));
            purchase.setEmail(purchase.getUser().getEmail());
        } else {
            purchase.setEmail(email);
        }
        purchase.setPieces(new HashSet<>(dao.getByIds(Piece.class, ids)));

        return dao.persist(purchase).getId();
    }

    @Transactional
    public void bitcoinPaymentComplete(String paymentId, Long purchaseId) {
        Purchase purchase = dao.getById(Purchase.class, purchaseId);
        // if no purchase found, or purchase already complete, ignore
        if (purchase == null || purchase.isComplete()) {
            return;
        }
        purchase.setPaymentProviderId(paymentId);
        purchase.setComplete(true);
        dao.persist(purchase);

        sendPurchaseEmail(purchase);
    }

    @JsonRootName("button")
    public static class ButtonRequest {
        private String name;
        @JsonProperty("price_string")
        private String price;
        @JsonProperty("price_currency_iso")
        private String currency;
        private String description;
        private String type;
        private String custom;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getPrice() {
            return price;
        }
        public void setPrice(String priceString) {
            this.price = priceString;
        }
        public String getCurrency() {
            return currency;
        }
        public void setCurrency(String priceCurrency) {
            this.currency = priceCurrency;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public String getCustom() {
            return custom;
        }
        public void setCustom(String custom) {
            this.custom = custom;
        }
    }

    public void resentPurchaseEmail(long purchaseId) {
        sendPurchaseEmail(dao.getById(Purchase.class, purchaseId));
    }

    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();
        String url = "https://coinbase.com/api/v1/currencies/exchange_rates?api_key=226d24b176455557d2928e54321b5ec35fd6e054d9d682ad0927f4e7ea6ed7b1";
        GetMethod get = new GetMethod(url);
        get.setRequestHeader("Content-Type", "application/json");
        get.setRequestHeader("Accept", "application/json");
        get.setRequestHeader("User-Agent", "");
        client.executeMethod(get);
        System.out.println(new String(get.getResponseBody()));
    }
}
