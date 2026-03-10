package com.example.examplemod.terminal.ai;

import java.util.List;

public final class SimpleAiResponseValidator implements AiResponseValidator {
    @Override
    public boolean isValid(List<String> lines) {
        return lines != null && !lines.isEmpty() && lines.size() <= 20;
    }
}
