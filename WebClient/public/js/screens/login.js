import * as api from '../api.js';
import { ServerError } from '../api.js';
import { onSubmit } from '../util/views.js';

// The login screen (like LoginController in the JavaFX client): the user types a name only - no password,
// by the exercise's definition. It also checks right away whether the server can be reached,
// so a stopped server is reported before the user tries to log in.
export class LoginScreen {
    #context;
    #screen = document.getElementById('login-screen');
    #serverStatus = document.getElementById('server-status');
    #checkAgainButton = document.getElementById('check-again');
    #form = document.getElementById('login-form');
    #userNameField = document.getElementById('user-name');
    #loginButton = document.getElementById('login-button');
    #errorText = document.getElementById('login-error');

    constructor(context) {
        this.#context = context;
        this.#checkAgainButton.addEventListener('click', () => this.#checkServer());
        // A form: pressing Enter in the name field submits it, exactly like clicking "Log in".
        onSubmit(this.#form, () => this.#login());
    }

    // message: why the user is here (e.g. "Your session has ended..."); null for none.
    // The screen is reused (not rebuilt like an FXML screen), so it is reset first: no name left from the last login.
    show(message) {
        this.#userNameField.value = '';
        this.#showError(message);
        this.#setBusy(false);
        this.#screen.hidden = false;
        this.#userNameField.focus();
        this.#checkServer();
    }

    hide() {
        this.#screen.hidden = true;
    }

    async #login() {
        const userName = this.#userNameField.value.trim();
        if (!userName) {
            this.#showError('Please enter your name.');
            return;
        }
        this.#setBusy(true);
        this.#showError(null);
        try {
            this.#context.showMain(await api.login(userName));
        } catch (error) {
            await this.#onLoginFailed(ServerError.from(error));
        }
    }

    async #onLoginFailed(error) {
        if (error.isConflict()) {
            // Either the name is taken, or this browser is already logged in: all the tabs of a browser share one
            // session, so another tab may have logged in meanwhile. In that case simply continue as that user.
            try {
                this.#context.showMain(await api.getMe());
                return;
            } catch {
                // not logged in: the name is taken - the server's message below says so
            }
        }
        this.#setBusy(false);
        this.#showError(error.message);
        if (error.isConnectionProblem()) {
            this.#setServerStatus(false, 'The server is not reachable.');
        }
    }

    async #checkServer() {
        this.#checkAgainButton.disabled = true;
        this.#serverStatus.textContent = 'Checking the server...';
        this.#serverStatus.classList.remove('status-ok', 'status-error');
        try {
            await api.ping();
            this.#setServerStatus(true, 'Connected to the server.');
        } catch (error) {
            this.#setServerStatus(false, ServerError.from(error).message);
        } finally {
            this.#checkAgainButton.disabled = false;
        }
    }

    #setServerStatus(reachable, text) {
        this.#serverStatus.textContent = text;
        this.#serverStatus.classList.toggle('status-ok', reachable);
        this.#serverStatus.classList.toggle('status-error', !reachable);
    }

    #setBusy(busy) {
        this.#loginButton.disabled = busy;
        this.#userNameField.disabled = busy;
    }

    // null hides the message
    #showError(message) {
        this.#errorText.textContent = message ?? '';
        this.#errorText.hidden = message === null;
    }
}
