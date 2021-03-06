/*
 * #%L
 * BroadleafCommerce PayPal
 * %%
 * Copyright (C) 2009 - 2014 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package org.broadleafcommerce.payment.service.gateway;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentTransactionType;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.payment.dto.LineItemDTO;
import org.broadleafcommerce.common.payment.dto.PaymentRequestDTO;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.AbstractExternalPaymentGatewayCall;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.vendor.paypal.service.payment.MessageConstants;
import org.broadleafcommerce.vendor.paypal.service.payment.PayPalExpressPaymentGatewayType;
import org.broadleafcommerce.vendor.paypal.service.payment.PayPalRequestGenerator;
import org.broadleafcommerce.vendor.paypal.service.payment.PayPalResponseGenerator;
import org.broadleafcommerce.vendor.paypal.service.payment.message.PayPalRequest;
import org.broadleafcommerce.vendor.paypal.service.payment.message.PayPalResponse;
import org.broadleafcommerce.vendor.paypal.service.payment.message.details.PayPalDetailsResponse;
import org.broadleafcommerce.vendor.paypal.service.payment.message.details.PayPalPayerAddress;
import org.broadleafcommerce.vendor.paypal.service.payment.message.payment.PayPalItemRequest;
import org.broadleafcommerce.vendor.paypal.service.payment.message.payment.PayPalPaymentRequest;
import org.broadleafcommerce.vendor.paypal.service.payment.message.payment.PayPalPaymentResponse;
import org.broadleafcommerce.vendor.paypal.service.payment.message.payment.PayPalShippingRequest;
import org.broadleafcommerce.vendor.paypal.service.payment.message.payment.PayPalSummaryRequest;
import org.broadleafcommerce.vendor.paypal.service.payment.type.PayPalMethodType;
import org.broadleafcommerce.vendor.paypal.service.payment.type.PayPalTransactionType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import javax.annotation.Resource;
import javax.net.ssl.SSLContext;

/**
 * @author Elbert Bautista (elbertbautista)
 */
@Service("blExternalCallPayPalExpressService")
public class ExternalCallPayPalExpressServiceImpl extends AbstractExternalPaymentGatewayCall<PayPalRequest, PayPalResponse> implements ExternalCallPayPalExpressService {

    private static final Log LOG = LogFactory.getLog(ExternalCallPayPalExpressServiceImpl.class);

    @Resource(name = "blPayPalExpressConfiguration")
    protected PayPalExpressConfiguration configuration;

    @Resource(name = "blPayPalExpressRequestGenerator")
    protected PayPalRequestGenerator requestGenerator;

    @Resource(name = "blPayPalExpressResponseGenerator")
    protected PayPalResponseGenerator responseGenerator;

    @Override
    public PayPalExpressConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Integer getFailureReportingThreshold() {
        return configuration.getFailureReportingThreshold();
    }

    @Override
    public PayPalResponse call(PayPalRequest paymentRequest) throws PaymentException {
        return super.process(paymentRequest);
    }

    @Override
    public PayPalResponse communicateWithVendor(PayPalRequest paymentRequest) throws Exception {
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                SSLContext.getDefault(),
                new String[] {"TLSv1.2"},
                null,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", socketFactory)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();

        try {
            HttpPost postMethod = new HttpPost(getServerUrl());
            List<NameValuePair> nvps = requestGenerator.buildRequest(paymentRequest);
            postMethod.setEntity(new UrlEncodedFormEntity(nvps));
            CloseableHttpResponse response = httpClient.execute(postMethod);
            String responseString = new BasicResponseHandler().handleResponse(response);
            return responseGenerator.buildResponse(responseString, paymentRequest);
        } finally {
            httpClient.close();
        }

    }

    public String getServerUrl() {
        return configuration.getServerUrl();
    }

    @Override
    public PayPalPaymentRequest buildBasicRequest(PaymentRequestDTO requestDTO, PayPalTransactionType transactionType) {
        Assert.isTrue(requestDTO.getOrderId() != null, "The Order ID for the paypal request cannot be null");
        Assert.isTrue(requestDTO.getTransactionTotal() != null, "The Transaction Total for the paypal request cannot be null");

        if (requestDTO.getOrderId() != null) {
            Assert.isTrue(requestDTO.getOrderId().length() <= 127, "The reference number for the paypal request cannot be greater than 127 characters");
        }

        PayPalPaymentRequest request = new PayPalPaymentRequest();
        request.setTransactionType(transactionType);
        request.setOrderId(requestDTO.getOrderId());
        request.setCurrency(requestDTO.getOrderCurrencyCode());
        request.setCompleteCheckoutOnCallback(requestDTO.isCompleteCheckoutOnCallback());

        PayPalSummaryRequest summaryRequest = new PayPalSummaryRequest();
        summaryRequest.setGrandTotal(new Money(requestDTO.getTransactionTotal(), requestDTO.getOrderCurrencyCode()));
        request.setSummaryRequest(summaryRequest);

        return request;
    }

    @Override
    public PaymentResponseDTO commonAuthorizeOrSale(PaymentRequestDTO requestDTO, PayPalTransactionType transactionType,
                                                       String token, String payerId) throws PaymentException {

        PayPalPaymentRequest request = buildBasicRequest(requestDTO, transactionType);

        Assert.isTrue(requestDTO.getOrderSubtotal() != null, "Must specify an Order Subtotal value on the PaymentRequestDTO");
        Assert.isTrue(requestDTO.getShippingTotal() != null, "Must specify a Shipping Total value on the PaymentRequestDTO");
        Assert.isTrue(requestDTO.getTaxTotal() != null, "Must specify a Tax Total value on the PaymentRequestDTO");

        request.getSummaryRequest().setSubTotal(new Money(requestDTO.getOrderSubtotal(), requestDTO.getOrderCurrencyCode()));
        request.getSummaryRequest().setTotalShipping(new Money(requestDTO.getShippingTotal(), requestDTO.getOrderCurrencyCode()));
        request.getSummaryRequest().setTotalTax(new Money(requestDTO.getTaxTotal(), requestDTO.getOrderCurrencyCode()));

        if (token == null && payerId == null) {
            if (PayPalTransactionType.AUTHORIZE.equals(transactionType)) {
                request.setMethodType(PayPalMethodType.AUTHORIZATION);
            } else {
                request.setMethodType(PayPalMethodType.CHECKOUT);
            }
        } else {
            request.setMethodType(PayPalMethodType.PROCESS);
            if (PayPalTransactionType.AUTHORIZE.equals(transactionType)) {
                request.setSecondaryMethodType(PayPalMethodType.AUTHORIZATION);
            } else {
                request.setSecondaryMethodType(PayPalMethodType.CHECKOUT);
            }

            request.setPayerID(payerId);
            request.setToken(token);
        }

        for(LineItemDTO lineItem : requestDTO.getLineItems()) {
            PayPalItemRequest itemRequest = new PayPalItemRequest();
            itemRequest.setDescription(lineItem.getDescription());
            itemRequest.setShortDescription(lineItem.getShortDescription());
            itemRequest.setQuantity(Long.parseLong(lineItem.getQuantity()));
            itemRequest.setUnitPrice(new Money(lineItem.getAmount(), requestDTO.getOrderCurrencyCode()));
            itemRequest.setSystemId(lineItem.getSystemId());
            request.getItemRequests().add(itemRequest);
        }

        if (requestDTO.shipToPopulated()) {
            PayPalShippingRequest shippingRequest = new PayPalShippingRequest();
            shippingRequest.setShipToName(requestDTO.getShipTo().getAddressFirstName() + " " +
                    requestDTO.getShipTo().getAddressLastName());
            shippingRequest.setShipToStreet(requestDTO.getShipTo().getAddressLine1());
            shippingRequest.setShipToStreet2(requestDTO.getShipTo().getAddressLine2());
            shippingRequest.setShipToCity(requestDTO.getShipTo().getAddressCityLocality());
            shippingRequest.setShipToState(requestDTO.getShipTo().getAddressStateRegion());
            shippingRequest.setShipToZip(requestDTO.getShipTo().getAddressPostalCode());
            shippingRequest.setShipToCountryCode(requestDTO.getShipTo().getAddressCountryCode());
            shippingRequest.setShipToPhoneNum(requestDTO.getShipTo().getAddressPhone());
            request.getShippingRequests().add(shippingRequest);
        }

        PayPalPaymentResponse response = (PayPalPaymentResponse) process(request);
        PaymentResponseDTO responseDTO = new PaymentResponseDTO(PaymentType.THIRD_PARTY_ACCOUNT,
                PayPalExpressPaymentGatewayType.PAYPAL_EXPRESS);

        setCommonPaymentResponse(response, responseDTO);
        responseDTO.successful(response.isSuccessful());

        if (PayPalTransactionType.AUTHORIZE.equals(transactionType)) {
            responseDTO.paymentTransactionType(PaymentTransactionType.AUTHORIZE);
        } else if (PayPalTransactionType.AUTHORIZEANDCAPTURE.equals(transactionType)) {
            responseDTO.paymentTransactionType(PaymentTransactionType.AUTHORIZE_AND_CAPTURE);
        }

        if(PayPalMethodType.PROCESS.equals(request.getMethodType())){
            setDecisionInformation(response, responseDTO);
        } else if (PayPalMethodType.CHECKOUT.equals(request.getMethodType()) || PayPalMethodType.AUTHORIZATION.equals(request.getMethodType())) {
            responseDTO.responseMap(MessageConstants.REDIRECTURL, response.getUserRedirectUrl());
        }

        return responseDTO;
    }

    @Override
    public void setCommonPaymentResponse(PayPalPaymentResponse response, PaymentResponseDTO responseDTO) {

        try {
            responseDTO.rawResponse(URLDecoder.decode(response.getRawResponse(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        responseDTO.responseMap(MessageConstants.TOKEN, response.getResponseToken());
        responseDTO.responseMap(MessageConstants.CORRELATIONID, response.getCorrelationId());
    }

    @Override
    public void setCommonDetailsResponse(PayPalDetailsResponse response, PaymentResponseDTO responseDTO) {

        try {
            responseDTO.rawResponse(URLDecoder.decode(response.getRawResponse(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if (response.getAddresses() != null && !response.getAddresses().isEmpty()) {
            PayPalPayerAddress payerAddress = response.getAddresses().get(0);
            responseDTO.shipTo()
                    .addressFirstName(payerAddress.getName())
                    .addressLine1(payerAddress.getStreet())
                    .addressLine2(payerAddress.getStreet2())
                    .addressCityLocality(payerAddress.getCity())
                    .addressStateRegion(payerAddress.getState())
                    .addressPostalCode(payerAddress.getZip())
                    .addressCountryCode(payerAddress.getCountryCode())
                    .addressPhone(payerAddress.getPhoneNumber())
                    .done();

            if (payerAddress.getAddressStatus()!= null) {
                    responseDTO.getShipTo().additionalFields(MessageConstants.ADDRESSSTATUS, payerAddress.getAddressStatus().getType());
            }
        }

        if (response.getPaymentDetails()!= null) {

            String itemTotal =  (response.getPaymentDetails().getItemTotal() != null)? response.getPaymentDetails().getItemTotal().toString() : "";
            String shippingDiscount = (response.getPaymentDetails().getShippingDiscount() != null)? response.getPaymentDetails().getShippingDiscount().toString() : "";
            String shippingTotal = (response.getPaymentDetails().getShippingTotal() != null)? response.getPaymentDetails().getShippingTotal().toString() : "";
            String totalTax = (response.getPaymentDetails().getTotalTax() != null)? response.getPaymentDetails().getTotalTax().toString() : "";

            responseDTO.amount(response.getPaymentDetails().getAmount())
                    .orderId(response.getPaymentDetails().getOrderId())
                    .completeCheckoutOnCallback(response.getPaymentDetails().isCompleteCheckoutOnCallback())
                    .responseMap(MessageConstants.DETAILSPAYMENTALLOWEDMETHOD, response.getPaymentDetails().getPaymentMethod())
                    .responseMap(MessageConstants.DETAILSPAYMENTREQUESTID, response.getPaymentDetails().getPaymentRequestId())
                    .responseMap(MessageConstants.DETAILSPAYMENTTRANSACTIONID, response.getPaymentDetails().getTransactionId())
                    .responseMap(MessageConstants.DETAILSPAYMENTITEMTOTAL, itemTotal)
                    .responseMap(MessageConstants.DETAILSPAYMENTSHIPPINGDISCOUNT, shippingDiscount)
                    .responseMap(MessageConstants.DETAILSPAYMENTSHIPPINGTOTAL,shippingTotal)
                    .responseMap(MessageConstants.DETAILSPAYMENTTOTALTAX, totalTax);
        }

        if (response.getCheckoutStatusType()!=null) {
            responseDTO.responseMap(MessageConstants.CHECKOUTSTATUS, response.getCheckoutStatusType().getType());
        }

        String paypalAdjustment = (response.getPayPalAdjustment() != null)? response.getPayPalAdjustment().toString() : "";
        String payerStatus = (response.getPayerStatus() != null)? response.getPayerStatus().toString() : "";

        responseDTO.customer()
            .firstName(response.getPayerFirstName())
            .lastName(response.getPayerLastName())
            .companyName(response.getBusiness())
            .phone(response.getPhoneNumber())
            .email(response.getEmailAddress())
            .done()
        .responseMap(MessageConstants.TOKEN, response.getResponseToken())
        .responseMap(MessageConstants.PAYERID, response.getPayerId())
        .responseMap(MessageConstants.NOTE, response.getNote())
        .responseMap(MessageConstants.PAYPALADJUSTMENT, paypalAdjustment)
        .responseMap(MessageConstants.PAYERSTATUS, payerStatus);

    }

    @Override
    public void setDecisionInformation(PayPalPaymentResponse response, PaymentResponseDTO responseDTO) {
        responseDTO.responseMap(MessageConstants.TRANSACTIONID, response.getPaymentInfo().getTransactionId());

        if (response.getPaymentInfo().getTotalAmount() != null) {
            responseDTO.amount(response.getPaymentInfo().getTotalAmount());
        }

        if (response.getPaymentInfo().getParentTransactionId() != null) {
            responseDTO.responseMap(MessageConstants.PARENTTRANSACTIONID, response.getPaymentInfo().getParentTransactionId());
        }
        if (response.getPaymentInfo().getReceiptId() != null) {
            responseDTO.responseMap(MessageConstants.RECEIPTID, response.getPaymentInfo().getReceiptId());
        }
        if (response.getPaymentInfo().getExchangeRate() != null) {
            responseDTO.responseMap(MessageConstants.EXCHANGERATE, response.getPaymentInfo().getExchangeRate().toString());
        }
        if (response.getPaymentInfo().getPaymentStatusType() != null) {
            responseDTO.responseMap(MessageConstants.PAYMENTSTATUS, response.getPaymentInfo().getPaymentStatusType().getType());
        }
        if (response.getPaymentInfo().getPendingReasonType() != null) {
            responseDTO.responseMap(MessageConstants.PENDINGREASON, response.getPaymentInfo().getPendingReasonType().getType());
        }
        if (response.getPaymentInfo().getReasonCodeType() != null) {
            responseDTO.responseMap(MessageConstants.REASONCODE, response.getPaymentInfo().getReasonCodeType().getType());
        }
        if (response.getPaymentInfo().getHoldDecisionType() != null) {
            responseDTO.responseMap(MessageConstants.HOLDDECISION, response.getPaymentInfo().getHoldDecisionType().getType());
        }
        if (response.getPaymentInfo().getFeeAmount() != null) {
            responseDTO.responseMap(MessageConstants.FEEAMOUNT, response.getPaymentInfo().getFeeAmount().toString());
        }
        if (response.getPaymentInfo().getSettleAmount() != null) {
            responseDTO.responseMap(MessageConstants.SETTLEAMOUNT, response.getPaymentInfo().getSettleAmount().toString());
        }
        if (response.getPaymentInfo().getTaxAmount() != null) {
            responseDTO.responseMap(MessageConstants.TAXAMOUNT, response.getPaymentInfo().getTaxAmount().toString());
        }
    }

    @Override
    public void setRefundInformation(PayPalPaymentResponse response, PaymentResponseDTO responseDTO) {
        if (response.getRefundInfo().getRefundTransactionId() != null) {
            responseDTO.responseMap(MessageConstants.REFUNDTRANSACTIONID, response.getRefundInfo().getRefundTransactionId());
        }

        if (response.getRefundInfo().getGrossRefundAmount() != null) {
            responseDTO.amount(new Money(response.getRefundInfo().getGrossRefundAmount().toString()));
            responseDTO.responseMap(MessageConstants.GROSSREFUNDAMT, response.getRefundInfo().getGrossRefundAmount().toString());
        }

        if (response.getRefundInfo().getFeeRefundAmount() != null) {
            responseDTO.responseMap(MessageConstants.FEEREFUNDAMT, response.getRefundInfo().getFeeRefundAmount().toString());
        }

        if (response.getRefundInfo().getNetRefundAmount() != null) {
            responseDTO.responseMap(MessageConstants.NETREFUNDAMT, response.getRefundInfo().getNetRefundAmount().toString());
        }

        if (response.getRefundInfo().getTotalRefundAmount() != null) {
            responseDTO.responseMap(MessageConstants.TOTALREFUNDEDAMT, response.getRefundInfo().getTotalRefundAmount().toString());
        }

        if (response.getRefundInfo().getRefundInfo() != null) {
            responseDTO.responseMap(MessageConstants.REFUNDINFO, response.getRefundInfo().getRefundInfo());
        }

        if (response.getRefundInfo().getRefundStatusType() != null) {
            responseDTO.responseMap(MessageConstants.REFUNDSTATUS, response.getRefundInfo().getRefundStatusType().getType());
        }

        if (response.getRefundInfo().getPendingReasonType() != null) {
            responseDTO.responseMap(MessageConstants.PENDINGREASON, response.getRefundInfo().getPendingReasonType().getType());
        }

    }

    @Override
    public String getServiceName() {
        return getClass().getName();
    }
}
