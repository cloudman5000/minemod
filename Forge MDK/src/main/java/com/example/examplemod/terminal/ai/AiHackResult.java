package com.example.examplemod.terminal.ai;

import com.example.examplemod.terminal.domain.AiAvailabilityState;

import java.util.List;

public record AiHackResult(
        List<String> outputLines,
        AiAvailabilityState availabilityState
) {
}
