package org.example.integrations;

import org.example.exceptions.PaymentRefusedException;
import org.example.models.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class PayPalPaymentProviderClient implements PaymentProviderClient {

    private final RestTemplate restTemplate;

    @Value("${paypal.base-url}")
    private String baseUrl;

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.return-url:}")
    private String returnUrl;

    @Value("${paypal.cancel-url:}")
    private String cancelUrl;

    public PayPalPaymentProviderClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getAccessToken() {
        String url = baseUrl + "/v1/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new PaymentRefusedException("Failed to obtain PayPal access token");
        }

        return (String) response.getBody().get("access_token");
    }

    @Override
    public String createPaymentOrder(Payment payment) {
        String accessToken = getAccessToken();

        String url = baseUrl + "/v2/checkout/orders";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // PayPal order body
        Map<String, Object> body = new HashMap<>();
        body.put("intent", "CAPTURE");

        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("reference_id", payment.getOrderId().toString());

        Map<String, String> amount = new HashMap<>();
        amount.put("currency_code", "EUR");
        amount.put("value", String.valueOf(payment.getAmount()));
        purchaseUnit.put("amount", amount);

        body.put("purchase_units", Collections.singletonList(purchaseUnit));

        Map<String, String> appContext = new HashMap<>();

        String newReturnUrl = "";

        if (returnUrl.equals("no-url")) {
            newReturnUrl = resolveUrl(returnUrl) + "payments/paypal-callback";
        } else {
            newReturnUrl = returnUrl;
        }

        String newCancelUrl = "";
        if (cancelUrl.equals("no-url")) {
            newCancelUrl += resolveUrl(cancelUrl) + "payments/paypal-cancel";
        }else {
            newCancelUrl = cancelUrl;
        }

        appContext.put("return_url", newReturnUrl);
        appContext.put("cancel_url", newCancelUrl);
        body.put("application_context", appContext);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new PaymentRefusedException("Failed to create PayPal order");
        }

        String orderId = (String) response.getBody().get("id");

        if (orderId == null) {
            throw new PaymentRefusedException("PayPal did not return an order id");
        }

        payment.setProviderRef(orderId);

        return orderId;
    }

    @Override
    public void capturePayment(String providerRef) {
        String accessToken = getAccessToken();

        String url = baseUrl + "/v2/checkout/orders/" + providerRef + "/capture";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new PaymentRefusedException("PayPal capture failed with status: " +
                    response.getStatusCode());
        }
    }


    private String resolveUrl(String urlTemplate) {
        if (urlTemplate != null && !urlTemplate.isEmpty() && !urlTemplate.equals("no-url")) {
            return urlTemplate;
        }

        String template = System.getenv("RETURN_URL_TEMPLATE");
        String nodeIp = System.getenv("NODE_IP");

        if (template == null || nodeIp == null) {
            throw new IllegalStateException("RETURN_URL_TEMPLATE or NODE_IP not defined");
        }

        return template.replace("${NODE_IP}", nodeIp);
    }

}