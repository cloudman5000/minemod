package com.example.examplemod.terminal.command;

import com.example.examplemod.terminal.domain.TerminalMessage;
import com.example.examplemod.terminal.network.TerminalMessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TerminalCommandDispatcherTest {
    @Mock
    private TerminalMessageBus messageBus;

    private TerminalCommandDispatcher dispatcher;
    private TerminalCommandContext context;

    @BeforeEach
    void setUp() {
        dispatcher = new TerminalCommandDispatcher();
        context = new TerminalCommandContext(null, "term-a", messageBus, List.of("[msg] hello"));
    }

    @Test
    void sendCommandPublishesDirectMessage() {
        TerminalCommandResult result = dispatcher.dispatch("send term-b hello world", context);
        assertTrue(result.handled());

        verify(messageBus).send(eq(null), argThat(matchesDirectMessage("term-a", "term-b", "hello world")));
    }

    @Test
    void broadcastCommandPublishesToBus() {
        TerminalCommandResult result = dispatcher.dispatch("broadcast panic all_terminals", context);
        assertTrue(result.handled());

        verify(messageBus).broadcast(null, "term-a", "panic", "all_terminals");
    }

    @Test
    void unknownCommandIsNotHandled() {
        TerminalCommandResult result = dispatcher.dispatch("totally_unknown_cmd", context);
        assertFalse(result.handled());
    }

    private static ArgumentMatcher<TerminalMessage> matchesDirectMessage(String from, String to, String body) {
        return message -> message != null
                && from.equals(message.fromTerminalId())
                && to.equals(message.toTerminalId())
                && body.equals(message.body())
                && "direct".equals(message.channel());
    }
}
