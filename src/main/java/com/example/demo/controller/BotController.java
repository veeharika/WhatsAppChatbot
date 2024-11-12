package com.example.demo.controller;

import com.example.demo.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class BotController {

    @Autowired
    private BotService botService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        botService.processWebhook(payload);
        return ResponseEntity.ok("Webhook processed");
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if (botService.verifyWebhook(mode, token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.badRequest().body("Verification failed");
    }
}