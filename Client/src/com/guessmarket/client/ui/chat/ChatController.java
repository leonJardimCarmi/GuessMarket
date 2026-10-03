package com.guessmarket.client.ui.chat;

import com.guessmarket.client.ui.ClientContext;
import com.guessmarket.client.util.Async;
import com.guessmarket.client.util.Views;
import com.guessmarket.dto.ChatMessageDto;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bonus: the chat of all the logged-in users. Every message goes through the server, and new ones arrive with the
 * automatic refresh (only the messages after the ones already shown). Messages that arrive while the tab is not
 * on screen are counted, so the tab's title can show them ("Chat (2)").
 */
public class ChatController {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final String MINE_STYLE = "chat-mine";

    @FXML private ListView<ChatMessageDto> messagesList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Label sendError;

    private final IntegerProperty unread = new SimpleIntegerProperty(0);
    private ClientContext context;
    private volatile int messageCount; // messages already shown; pull() reads it on another thread
    private boolean shown;             // is the Chat tab on screen?

    @FXML
    private void initialize() {
        messagesList.setCellFactory(list -> new MessageCell());
        // A TextFormatter sees every change before it happens. Text that would pass the limit is cut to what fits,
        // so pasting a long text keeps its beginning instead of pasting nothing.
        messageField.setTextFormatter(new TextFormatter<String>(change -> {
            int excess = change.getControlNewText().length() - ChatMessageDto.MAX_LENGTH;
            if (excess > 0) {
                String added = change.getText();
                change.setText(added.substring(0, Math.max(0, added.length() - excess)));
            }
            return change;
        }));
        Views.clearMessage(sendError);
    }

    public void init(ClientContext context) {
        this.context = context;
        refresh();
    }

    // The number of messages that arrived while the tab was hidden; the main window shows it in the tab's title.
    public ReadOnlyIntegerProperty unreadProperty() {
        return unread;
    }

    // Called by the main window when the user switches tabs: once the chat is on screen, nothing is unread.
    public void setShown(boolean shown) {
        this.shown = shown;
        if (shown) {
            unread.set(0);
            scrollToNewest();
            // Later, not now: the tab's content becomes visible only after the switch, and a hidden node gets no focus.
            Platform.runLater(messageField::requestFocus);
        }
    }

    public void refresh() {
        Async.run(this::pull, Runnable::run, context::handleError);
    }

    // Fetches the messages that are new to this client. Runs on a background thread (Async or the poller);
    // returns the screen update, which must run on the JavaFX thread.
    public Runnable pull() {
        int from = messageCount;
        List<ChatMessageDto> newMessages = context.api().getChatMessages(from);
        return () -> show(from, newMessages);
    }

    private void show(int from, List<ChatMessageDto> newMessages) {
        // Two refreshes may run at once; only the one that asked from our current count adds its messages.
        if (from != messageCount || newMessages.isEmpty()) {
            return;
        }
        messagesList.getItems().addAll(newMessages);
        messageCount += newMessages.size();
        if (shown) {
            scrollToNewest();
        } else if (from > 0) { // the history that was there before the user logged in is not "new"
            long fromOthers = newMessages.stream().filter(message -> !isMine(message)).count();
            unread.set(unread.get() + (int) fromOthers);
        }
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().strip();
        if (text.isEmpty()) {
            return; // Enter in an empty field: nothing to send
        }
        setSending(true);
        Views.clearMessage(sendError);
        Async.run(() -> context.api().sendChatMessage(text),
                sent -> {
                    setSending(false);
                    messageField.clear();
                    refresh(); // show it right away, without waiting for the next automatic refresh
                },
                error -> {
                    setSending(false);
                    context.handleError(error, message -> Views.showError(sendError, message));
                });
    }

    // Locked while a message is on its way, so Enter twice does not send it twice.
    private void setSending(boolean sending) {
        messageField.setDisable(sending);
        sendButton.setDisable(sending);
        if (!sending) {
            messageField.requestFocus(); // a disabled field loses the focus; give it back for the next message
        }
    }

    private void scrollToNewest() {
        if (!messagesList.getItems().isEmpty()) {
            messagesList.scrollTo(messagesList.getItems().size() - 1);
        }
    }

    private boolean isMine(ChatMessageDto message) {
        return message.getUserName().equalsIgnoreCase(context.userName());
    }

    // One message: "18:05  Alice: hello". A long message wraps to the list's width; the user's own messages stand out.
    private class MessageCell extends ListCell<ChatMessageDto> {
        MessageCell() {
            setWrapText(true);
            setPrefWidth(0); // follow the list's width, not the text's: long lines wrap instead of scrolling sideways
        }

        @Override
        protected void updateItem(ChatMessageDto message, boolean empty) {
            super.updateItem(message, empty);
            getStyleClass().removeAll(MINE_STYLE); // a cell is reused for different messages
            if (empty || message == null) {
                setText(null);
                return;
            }
            setText(TIME.format(Instant.ofEpochMilli(message.getTimestamp())) + "  "
                    + message.getUserName() + ": " + message.getText());
            if (isMine(message)) {
                getStyleClass().add(MINE_STYLE);
            }
        }
    }
}
