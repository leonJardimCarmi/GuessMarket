import * as api from './api.js';
import { ServerError } from './api.js';
import { Poller } from './util/poller.js';
import { LoginScreen } from './screens/login.js';
import { MainScreen } from './screens/main.js';

const SESSION_ENDED = 'Your session has ended. Please log in again.';

// What every screen shares (like ClientContext in the JavaFX client): the logged-in user, moving between screens,
// the automatic refresh, and the common reaction to errors.
// The page is never reloaded: both screens are in index.html, and switching screens only shows one and hides the other.
export class ClientContext {
    #userName = null; // null while nobody is logged in
    #poller = null;   // the automatic refresh; runs only while the main screen is shown
    #loginScreen;
    #mainScreen;

    constructor() {
        // Each screen gets this context, like init(context) in the JavaFX controllers.
        this.#loginScreen = new LoginScreen(this);
        this.#mainScreen = new MainScreen(this);
    }

    isLoggedIn() {
        return this.#userName !== null;
    }

    // Is this the logged-in user? Names are compared ignoring upper / lower case, like the server does
    // ("bob" and "Bob" are the same user).
    isMe(userName) {
        return this.isLoggedIn() && userName.toLowerCase() === this.#userName.toLowerCase();
    }

    // The first screen: when this browser is still logged in (e.g. the page was refreshed), straight back to the
    // main screen; otherwise the login screen.
    async start() {
        try {
            this.showMain(await api.getMe());
        } catch {
            this.showLogin(null); // usually 401 (not logged in); a stopped server shows in the login screen's status
        }
    }

    // message: shown on the login screen (e.g. why the user was sent back to it); null for none.
    showLogin(message) {
        this.#stopPolling();
        this.#userName = null;
        document.title = 'Guess Market';
        this.#mainScreen.hide();
        this.#loginScreen.show(message);
    }

    showMain(user) {
        this.#userName = user.name;
        document.title = `${user.name} - Guess Market`; // tells apart browser windows of different users
        this.#loginScreen.hide();
        this.#mainScreen.show(user);
    }

    // Called by the main screen. pull: fetches the data and returns the screen update (see Poller).
    startPolling(pull, onError) {
        this.#stopPolling();
        this.#poller = new Poller(pull, onError);
        this.#poller.start();
    }

    #stopPolling() {
        if (this.#poller !== null) {
            this.#poller.stop();
            this.#poller = null;
        }
    }

    // Fetches and shows a tab's data right away (the Refresh button, switching tabs, after an action) instead of
    // waiting for the next round of the automatic refresh. pull: fetches the data and returns the screen update.
    async refreshNow(pull, showMessage) {
        try {
            const showUpdate = await pull();
            showUpdate();
        } catch (error) {
            this.handleError(error, showMessage);
        }
    }

    // Logs out on the server; whatever the answer, this client returns to the login screen.
    async logout() {
        this.#stopPolling(); // no refresh may run during the logout (it would get "not logged in")
        try {
            await api.logout();
        } catch {
            // e.g. the session had already ended - the user is logged out either way
        }
        this.showLogin(null);
    }

    // The common reaction to a failed request: an ended session returns to the login screen;
    // any other error is shown by showMessage (e.g. in the status bar, or next to the button that failed).
    handleError(error, showMessage) {
        const serverError = ServerError.from(error); // a bug in the client becomes a readable message too
        if (serverError.isUnauthorized() && this.isLoggedIn()) {
            this.showLogin(SESSION_ENDED);
        } else {
            showMessage(serverError.message);
        }
    }
}
