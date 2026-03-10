package com.example.examplemod.terminal.runtime;

import com.example.examplemod.terminal.domain.TerminalState;

public interface TerminalStateStore {
    TerminalState load();

    void save(TerminalState state);
}
