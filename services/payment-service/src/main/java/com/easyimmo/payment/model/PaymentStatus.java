package com.easyimmo.payment.model;

public enum PaymentStatus {
    PENDING,  // En attente totale de paiement
    PARTIAL,  // Payé partiellement
    PAID,     // Entièrement payé
    LATE,     // En retard de paiement
    FAILED    // Échec de traitement du paiement
}
