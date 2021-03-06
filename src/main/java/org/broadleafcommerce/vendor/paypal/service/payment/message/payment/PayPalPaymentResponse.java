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
package org.broadleafcommerce.vendor.paypal.service.payment.message.payment;

import org.broadleafcommerce.vendor.paypal.service.payment.message.ErrorCheckable;
import org.broadleafcommerce.vendor.paypal.service.payment.message.PayPalErrorResponse;
import org.broadleafcommerce.vendor.paypal.service.payment.message.PayPalResponse;
import org.broadleafcommerce.vendor.paypal.service.payment.type.PayPalMethodType;
import org.broadleafcommerce.vendor.paypal.service.payment.type.PayPalTransactionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author jfischer
 *
 */
public class PayPalPaymentResponse extends PayPalResponse implements ErrorCheckable {
        
    private static final long serialVersionUID = 1L;
    
    protected boolean isErrorDetected = false;
    protected boolean isSuccessful = true;
    protected String orderId;
    protected String errorText;
    protected PayPalTransactionType transactionType;
    protected PayPalMethodType methodType;
    protected List<PayPalErrorResponse> errorResponses = new ArrayList<PayPalErrorResponse>();
    protected Map<String, String> passThroughErrors = new HashMap<String, String>();
    protected String userRedirectUrl;
    protected String correlationId;
    protected String ack;
    protected PayPalPaymentInfo paymentInfo;
    protected PayPalRefundInfo refundInfo;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public PayPalTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(PayPalTransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    public String getErrorCode() {
        throw new RuntimeException("ErrorCode not supported");
    }

    public String getErrorText() {
        return errorText;
    }

    public boolean isErrorDetected() {
        return isErrorDetected;
    }

    public void setErrorCode(String errorCode) {
        throw new RuntimeException("ErrorCode not supported");
    }

    public void setErrorDetected(boolean isErrorDetected) {
        this.isErrorDetected = isErrorDetected;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    public List<PayPalErrorResponse> getErrorResponses() {
        return errorResponses;
    }

    public void setErrorResponses(List<PayPalErrorResponse> errorResponses) {
        this.errorResponses = errorResponses;
    }

    public Map<String, String> getPassThroughErrors() {
        return passThroughErrors;
    }

    public void setPassThroughErrors(Map<String, String> passThroughErrors) {
        this.passThroughErrors = passThroughErrors;
    }

    public PayPalMethodType getMethodType() {
        return methodType;
    }

    public void setMethodType(PayPalMethodType methodType) {
        this.methodType = methodType;
    }

    public boolean isSuccessful() {

        return isSuccessful;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public String getUserRedirectUrl() {
        return userRedirectUrl;
    }

    public void setUserRedirectUrl(String userRedirectUrl) {
        this.userRedirectUrl = userRedirectUrl;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getAck() {
        return ack;
    }

    public void setAck(String ack) {
        this.ack = ack;
    }

    public PayPalPaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public void setPaymentInfo(PayPalPaymentInfo paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    public PayPalRefundInfo getRefundInfo() {
        return refundInfo;
    }

    public void setRefundInfo(PayPalRefundInfo refundInfo) {
        this.refundInfo = refundInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PayPalPaymentResponse)) return false;
        if (!super.equals(o)) return false;

        PayPalPaymentResponse that = (PayPalPaymentResponse) o;

        if (isErrorDetected != that.isErrorDetected) return false;
        if (isSuccessful != that.isSuccessful) return false;
        if (ack != null ? !ack.equals(that.ack) : that.ack != null) return false;
        if (correlationId != null ? !correlationId.equals(that.correlationId) : that.correlationId != null)
            return false;
        if (errorResponses != null ? !errorResponses.equals(that.errorResponses) : that.errorResponses != null)
            return false;
        if (errorText != null ? !errorText.equals(that.errorText) : that.errorText != null) return false;
        if (methodType != null ? !methodType.equals(that.methodType) : that.methodType != null) return false;
        if (passThroughErrors != null ? !passThroughErrors.equals(that.passThroughErrors) : that.passThroughErrors != null)
            return false;
        if (paymentInfo != null ? !paymentInfo.equals(that.paymentInfo) : that.paymentInfo != null) return false;
        if (refundInfo != null ? !refundInfo.equals(that.refundInfo) : that.refundInfo != null) return false;
        if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) return false;
        if (transactionType != null ? !transactionType.equals(that.transactionType) : that.transactionType != null)
            return false;
        if (userRedirectUrl != null ? !userRedirectUrl.equals(that.userRedirectUrl) : that.userRedirectUrl != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (isErrorDetected ? 1 : 0);
        result = 31 * result + (isSuccessful ? 1 : 0);
        result = 31 * result + (errorText != null ? errorText.hashCode() : 0);
        result = 31 * result + (transactionType != null ? transactionType.hashCode() : 0);
        result = 31 * result + (methodType != null ? methodType.hashCode() : 0);
        result = 31 * result + (errorResponses != null ? errorResponses.hashCode() : 0);
        result = 31 * result + (passThroughErrors != null ? passThroughErrors.hashCode() : 0);
        result = 31 * result + (userRedirectUrl != null ? userRedirectUrl.hashCode() : 0);
        result = 31 * result + (correlationId != null ? correlationId.hashCode() : 0);
        result = 31 * result + (ack != null ? ack.hashCode() : 0);
        result = 31 * result + (paymentInfo != null ? paymentInfo.hashCode() : 0);
        result = 31 * result + (refundInfo != null ? refundInfo.hashCode() : 0);
        result = 31 * result + (orderId != null ? orderId.hashCode() : 0);
        return result;
    }
}
