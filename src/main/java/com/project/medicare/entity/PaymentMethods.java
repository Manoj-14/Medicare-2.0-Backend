package com.project.medicare.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public enum PaymentMethods {
    CASH_ON_DELIVERY,CREDIT_CARD,DEBIT_CARD,UPI ;

}
