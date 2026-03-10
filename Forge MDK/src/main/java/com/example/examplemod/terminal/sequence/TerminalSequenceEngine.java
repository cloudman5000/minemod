package com.example.examplemod.terminal.sequence;

import com.example.examplemod.terminal.ai.AiHackResult;

import java.util.List;

public interface TerminalSequenceEngine {
    List<String> toTerminalLines(AiHackResult result, String command);
}
