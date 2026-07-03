package com.easyimmo.payment.controller;

import com.easyimmo.payment.model.TransactionStatus;
import com.easyimmo.payment.service.MobileMoneyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/momo/callback")
@RequiredArgsConstructor
@Slf4j
public class MobileMoneyCallbackController {

    private final MobileMoneyService momoService;

    /**
     * Webhook pour MTN Mobile Money
     */
    @PostMapping("/mtn")
    public ResponseEntity<Void> mtnCallback(
            @RequestHeader(value = "X-Reference-Id", required = false) String referenceId,
            @RequestBody Map<String, Object> body) {
        
        log.info("Callback MTN reçu pour reference: {}", referenceId);
        
        // Structure standard callback MTN MoMo :
        // { "financialTransactionId": "...", "status": "SUCCESSFUL" / "FAILED", ... }
        String statusStr = body.getOrDefault("status", "FAILED").toString();
        TransactionStatus status = statusStr.equalsIgnoreCase("SUCCESSFUL") 
            ? TransactionStatus.SUCCESS 
            : TransactionStatus.FAILED;

        momoService.processCallback(referenceId, status, body.toString());
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook pour Orange Money WebPay
     */
    @PostMapping("/orange")
    public ResponseEntity<Void> orangeCallback(@RequestBody Map<String, Object> body) {
        log.info("Callback Orange Money reçu : {}", body);
        
        // Structure Orange OM WebPay :
        // { "status": "SUCCESS" / "FAIL", "notif_token": "externalRef", ... }
        String externalRef = body.getOrDefault("notif_token", "").toString();
        String statusStr = body.getOrDefault("status", "FAIL").toString();
        
        TransactionStatus status = statusStr.equalsIgnoreCase("SUCCESS") 
            ? TransactionStatus.SUCCESS 
            : TransactionStatus.FAILED;

        momoService.processCallback(externalRef, status, body.toString());
        return ResponseEntity.ok().build();
    }
}
