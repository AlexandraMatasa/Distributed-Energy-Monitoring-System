package com.energy.communicationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ChatRuleService {

    private static final Logger log = LoggerFactory.getLogger(ChatRuleService.class);
    private final Map<Pattern, String> rules = new LinkedHashMap<>();

    public ChatRuleService() {
        initializeRules();
    }

    private void initializeRules() {
        rules.put(
                Pattern.compile(".*\\b(hello|hi|hey|good morning|good afternoon|good evening|greetings)\\b.*", Pattern.CASE_INSENSITIVE),
                "Hello! Welcome to Energy Management System support. How can I assist you today?"
        );

        rules.put(
                Pattern.compile(".*\\b(device|devices).*(not showing|missing|can't see|don't see|disappeared|not visible)\\b.*", Pattern.CASE_INSENSITIVE),
                "If your devices aren't showing up, please try:\n1. Refresh the page (F5)\n2. Verify you're logged in with the correct account\n3. Contact an administrator to check device assignments"
        );

        rules.put(
                Pattern.compile(".*\\b(how|can i).*(add|create|register|setup).*(device)\\b.*", Pattern.CASE_INSENSITIVE),
                "Only administrators have permission to add new devices to the system. If you're an admin, use the 'Devices' section. Otherwise, please contact your system administrator."
        );

        rules.put(
                Pattern.compile(".*\\b(overconsumption|over consumption|alert|notification|exceeded|too much energy|high consumption)\\b.*", Pattern.CASE_INSENSITIVE),
                "Overconsumption alerts occur when a device exceeds its maximum hourly consumption limit. You can:\n• Check the device's max consumption setting in Devices page\n• View hourly consumption data in the Monitoring tab\n• Contact support if you believe the alert is incorrect"
        );

        rules.put(
                Pattern.compile(".*\\b(password|forgot password|reset password|can't login|cannot login|lost password)\\b.*", Pattern.CASE_INSENSITIVE),
                "For security reasons, password resets must be handled by your system administrator. Please contact them directly to reset your password."
        );

        rules.put(
                Pattern.compile(".*\\b(chart|graph|monitoring|consumption data).*(not loading|empty|blank|no data|not working)\\b.*", Pattern.CASE_INSENSITIVE),
                "If your energy consumption chart isn't loading:\n1. Ensure you have devices assigned to your account\n2. Verify there's data available for the selected date\n3. Try selecting a different date range\n4. Refresh the page or clear browser cache"
        );

        rules.put(
                Pattern.compile(".*\\b(what is|difference between|explain).*(admin|client|role|roles|permissions)\\b.*", Pattern.CASE_INSENSITIVE),
                "The system has two user roles:\n• ADMIN: Can manage users, devices, and device assignments\n• CLIENT: Can view assigned devices and monitor their energy consumption\n\nContact an administrator to request a role change."
        );

        rules.put(
                Pattern.compile(".*\\b(kwh|kilowatt|watt|what unit|energy unit|measurement unit|how is energy measured)\\b.*", Pattern.CASE_INSENSITIVE),
                "All energy consumption in our system is measured in kWh (kilowatt-hours). The monitoring charts display hourly aggregated data showing total consumption per hour."
        );

        rules.put(
                Pattern.compile(".*\\b(assign|assignment|assign device|how to assign|link device)\\b.*", Pattern.CASE_INSENSITIVE),
                "Device assignment is performed by administrators through the 'Assignments' page. If you need a device assigned to your account, please contact your system administrator."
        );

        rules.put(
                Pattern.compile(".*\\b(how often|refresh|update|real.?time|realtime|live data|frequency)\\b.*", Pattern.CASE_INSENSITIVE),
                "Energy data collection works as follows:\n• Device data collected every 10 minutes\n• Aggregated into hourly consumption totals\n• Charts update automatically when new hourly data arrives\n• Overconsumption alerts sent in real-time via WebSocket"
        );

        rules.put(
                Pattern.compile(".*\\b(create account|register|sign up|new user|new account|join)\\b.*", Pattern.CASE_INSENSITIVE),
                "New user accounts can only be created by system administrators. Please contact your organization's admin to request an account."
        );


        rules.put(
                Pattern.compile(".*\\b(contact|reach|talk to|speak with|email).*(admin|administrator|support|help desk)\\b.*", Pattern.CASE_INSENSITIVE),
                "You're currently chatting with our automated support system. For direct admin assistance, your message will be forwarded to an available administrator who will respond shortly."
        );

        rules.put(
                Pattern.compile(".*\\b(export|download|save).*(data|chart|report|consumption)\\b.*", Pattern.CASE_INSENSITIVE),
                "Currently, data export features are being developed. For now, you can take screenshots of the charts or contact an administrator for detailed consumption reports."
        );

        rules.put(
                Pattern.compile(".*\\b(thank you|thanks|thx|appreciate|grateful)\\b.*", Pattern.CASE_INSENSITIVE),
                "You're very welcome! Is there anything else I can help you with?"
        );

        rules.put(
                Pattern.compile(".*\\b(bye|goodbye|see you|exit|close chat|end chat)\\b.*", Pattern.CASE_INSENSITIVE),
                "Thank you for using our support system! Have a great day! Feel free to return if you need more help."
        );

        log.info("Initialized {} chat rules", rules.size());
    }

    public Optional<String> matchRule(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = userMessage.trim().toLowerCase();
        for (Map.Entry<Pattern, String> entry : rules.entrySet()) {
            if (entry.getKey().matcher(normalized).matches()) {
                log.info(" Rule matched for message: {}", userMessage);
                return Optional.of(entry.getValue());
            }
        }

        log.info("No rule matched for message: {}", userMessage);
        return Optional.empty();
    }

    public int getRuleCount() {
        return rules.size();
    }
}