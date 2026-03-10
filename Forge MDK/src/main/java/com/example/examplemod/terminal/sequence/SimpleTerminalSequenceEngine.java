package com.example.examplemod.terminal.sequence;

import com.example.examplemod.terminal.ai.AiHackResult;

import java.util.ArrayList;
import java.util.List;

public final class SimpleTerminalSequenceEngine implements TerminalSequenceEngine {
    @Override
    public List<String> toTerminalLines(AiHackResult result, String command) {
        return new ArrayList<>(result.outputLines());
    }
}
