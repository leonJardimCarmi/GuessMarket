import { ServerError } from '../api.js';

// The automatic refresh ("pull"), like Poller in the JavaFX client: every half a second it runs one round -
// fetch fresh data from the server, then show it on the screen.
// The next round is scheduled only after the previous one has ended (setTimeout after each round, not
// setInterval), so when the server is slow, rounds are skipped instead of piling up.
// A stopped poller is never started again: the context makes a new one for every login.
export class Poller {
    // The exercise allows up to 2 seconds; half a second feels immediate and is still light for a local server.
    static INTERVAL_MS = 500;

    #pull;    // async function: fetches the data, returns a function that shows it on the screen
    #onError; // called with a ServerError when a round fails; the next rounds keep trying
    #timer = null;
    #stopped = false;

    constructor(pull, onError) {
        this.#pull = pull;
        this.#onError = onError;
    }

    start() {
        this.#scheduleRound();
    }

    // A round whose answer is still on its way finds #stopped set and leaves the screen alone
    // (e.g. the user logged out meanwhile).
    stop() {
        this.#stopped = true;
        clearTimeout(this.#timer);
    }

    #scheduleRound() {
        this.#timer = setTimeout(() => this.#runRound(), Poller.INTERVAL_MS);
    }

    async #runRound() {
        try {
            const showUpdate = await this.#pull();
            if (!this.#stopped) {
                showUpdate();
            }
        } catch (error) {
            if (!this.#stopped) {
                this.#onError(ServerError.from(error));
            }
        }
        if (!this.#stopped) { // onError may have stopped the poller (e.g. the session ended)
            this.#scheduleRound();
        }
    }
}
