package com.example.examplemod.terminal.ai;

import java.util.List;

public interface AiResponseValidator {
    boolean isValid(List<String> lines);
}
